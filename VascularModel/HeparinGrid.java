/*
GRIFFIN LAB
ALEX TAING, UNDERGRADUATE
FALL 2020
*/

package VascularModel;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2D;
import HAL.GridsAndAgents.PDEGrid2D;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;

/// CURRENT ISSUES:

/// 9) make age of cell? if cell is old, then less likely to grow/branch!!!! let new cells branch, but old ones not
/// 11) incorporate brush border effect
/// 12) possible fundamental change in how I approach cell division: tip cell migration, stalk cell elongation, tip cell duplication, repeat
/// 15) Vessels are running into each other in the beginning!  It looks like they are struggling to follow VEGF?
/// 16) Rescale agents
/// 17) make particles turn off when vessels arrive
/// 18) fix lag: data leak somewhere?

/// THINGS THAT CAN BE VARIED FOR EXPERIMENTATION:
/// Ratio of MAP to Heparin MAP (HEP_TO_MAP_RATIO)
/// Radius of VEGF sensing (SIGHT_RADIUS)
/// Radius of MAP (MAP_RADIUS)
/// MAP spacing (MAP_SPACING)
/// VEGF sensitivity (VEGF_SENSITIVITY)
/// Division chance, when in, and not in, the presence of VEGF (DIV_PROB)
/// Diffusion rate of VEGF (DIFFUSION_COEFFICIENT)
/// Chance for endothelial cell to branch (SPLIT_PROB)
/// Chance for body cell to begin branching (BODY_CELL_BRANCH_PROB)
/// Percent of spawning branch from injury site (START_VASCULAR_CHANCE)
/// how much VEGF is consumed by an endothelial cell (VASCULAR_VEGF_INTAKE)

class EndothelialCell extends AgentSQ2D<HeparinGrid>{


    // DO NOT MODIFY FOR PARAMETERS
    public static int HEAD_CELL = 0;
    public static int BODY_CELL = 1;
    public static int MAP_PARTICLE = 2;
    public static int HEPARIN_MAP = 3;
    public static int MACROPHAGE = 4;

    public static int HEAD_CELL_COLOR = Util.RED;
    public static int BODY_CELL_COLOR = Util.RED;
    public static int MAP_PARTICLE_COLOR = Util.RGB(23.0/255, 28.0/255, 173.0/255); // normal MAP;
    public static int HEPARIN_MAP_COLOR = Util.RGB(48.0/255, 191.0/255, 217.0/255); // Heparin MAP;
    public static int MACROPHAGE_COLOR = Util.WHITE;

    public static double VASCULAR_VEGF_INTAKE = HeparinGrid.VASCULAR_VEGF_INTAKE;
    public static double VEGF_DIV_PROB = HeparinGrid.VEGF_DIV_PROB;
    public static double BODY_CELL_BRANCH_PROB = HeparinGrid.BODY_CELL_BRANCH_PROB;
    public final static double MACROPHAGE_SPAWN_CHANCE = HeparinGrid.MACROPHAGE_SPAWN_CHANCE;
    public final static int MAX_MACROPHAGE_PER_SPAWN = HeparinGrid.MAX_MACROPHAGE_PER_SPAWN;
    public final static double MACROPHAGE_FORWARD_TENDENCY  = HeparinGrid.MACROPHAGE_FORWARD_TENDENCY;
    public final static int ENDO_CELL_TICK_DELAY = HeparinGrid.ENDO_CELL_TICK_DELAY;
    public final static double VEGF_SENSITIVITY = HeparinGrid.VEGF_SENSITIVITY;

    int color;
    int type;
    int length = 0;
    boolean arrived = false; // true if the vessel has reached the wound edge
    public static boolean start_endo = false; // when the endo cells begin to grow after macrophage start

    /**
     * Gets the location with the highest VEGF concentration within the cell's radius of sight
     * @return the location of the VEGF
     */
    public int HighestConcentrationVEGF(){
        assert G != null;
        int VEGF_options = G.MapEmptyHood(G.VEGFHood, Isq()); // gets the cell's range of detecting VEGF

        double maxConcentration = -1;
        ArrayList<Integer> maxConcentrationLocations = new ArrayList<>();
        for (int i = 0; i < VEGF_options; i++) { // if there's nearby VEGF...
            double test_concentration = G.VEGF.Get(G.VEGFHood[i]);
            if ((test_concentration > maxConcentration) && (test_concentration > VEGF_SENSITIVITY)){
                maxConcentration = test_concentration;
                maxConcentrationLocations.clear();
                maxConcentrationLocations.add(G.VEGFHood[i]);
            } else if (test_concentration == maxConcentration) {
                maxConcentrationLocations.add(G.VEGFHood[i]);
            }
        }
        if (maxConcentrationLocations.size() < 1) {
            return 0;
        } else if (maxConcentration <= 0){
            return 0;
        }

        return maxConcentrationLocations.get((int)(Math.random()*maxConcentrationLocations.size()));
    }

    /**
     * Given an int location of a target, will find best location for next cell duplication
     * @param VEGF_location location of nearest VEGF
     * @return int location to grow closer to target
     */
    public int HoodClosestToVEGF(int VEGF_location){

        int minDistance = Integer.MAX_VALUE; // gets updated with each location check
        ArrayList<Integer> mincoordint = new ArrayList<>(); // closest points that are in the cell neighborhood (as an int)

        assert G != null;
        int options = G.MapHood(G.divHood, Isq()); // open areas around cell

        for (int i = 0; i < options; i++) {
            int MAPcount = 0;
            for (EndothelialCell cell : G.IterAgents(G.divHood[i])) {
                if ((cell.type == MAP_PARTICLE) || (cell.type == HEPARIN_MAP)) {
                    MAPcount++;
                }
            }
            if (MAPcount == 0){
                int[] hoodPoint = {G.ItoX(G.divHood[i]), G.ItoY(G.divHood[i])};

                // gets the distance from neighborhood area to target
                int dist = Math.abs((int)Math.hypot(G.ItoX(VEGF_location)-hoodPoint[0], G.ItoY(VEGF_location)-hoodPoint[1]));

                // keeps a list of the hood points closest to the VEGF
                if (dist < minDistance){ // if the new hood point distance is closer than the one before, then...
                    minDistance = dist; // the minimum distance is updated to the new closest distance
                    mincoordint.clear();// the old list is cleared
                    mincoordint.add(G.I(hoodPoint[0], hoodPoint[1])); // and the new closest point is added to the empty list
                }else if (dist == minDistance){ // But, if the point is just as close as the ones on the list
                    mincoordint.add(G.I(hoodPoint[0], hoodPoint[1])); // it is added to the list of the hood points that are just as close
                }
            }
        }
        if (mincoordint.size() == 0){ // if there are no sufficiently strong VEGF nearby, then...
            return 0; // return 0
        }
        return mincoordint.get((int)(Math.random()*mincoordint.size())); // otherwise, return a random hood point on the list
    }

    /**
     * Initializes macrophages
     */
    public void initMacrophages(){
        assert G != null;
        if(G.rng.Double() < MACROPHAGE_SPAWN_CHANCE){
            for (int i = 1; i < (int)MAX_MACROPHAGE_PER_SPAWN*(G.rng.Double()); i++) {
                G.NewAgentPT((HeparinGrid.x)*Math.random(), 0).Init(MACROPHAGE, false, 0); // make a new macrophage there
            }
        }
    }

    /**
     Initializes a cell with color and type
     * @param type: type of cell/particle
     * @param arrived whether the cell has arrived at the target or not (to be inherited from parent cell)
     */
    public void Init(int type, boolean arrived, int length){

        this.arrived = arrived;
        this.type = type;
        this.length = length;

        if (type == HEAD_CELL) {
            this.color = HEAD_CELL_COLOR; // Growing endothelial cells
        } else if (type == BODY_CELL){
            this.color = BODY_CELL_COLOR;
        } else if (type == MAP_PARTICLE){
            this.color = MAP_PARTICLE_COLOR; // normal MAP
        } else if (type == HEPARIN_MAP) { // Inactive Endothelial cells
            this.color = HEPARIN_MAP_COLOR; // Heparin MAP
        } else if (type == MACROPHAGE) {
            this.color = MACROPHAGE_COLOR;
        }
    }

    /**
     * Divides a cell to a random nearby location, allowing overlap with vessels, but not MAP
     */
    public void randomDivideOverlap() {
        assert G != null;
        int options = G.MapHood(G.divHood, Isq());
        ArrayList<Integer> openAreas = new ArrayList<>();
        for (int i = 0; i < options; i++) {
            int MAPcount = 0;
            for (EndothelialCell cell : G.IterAgents(G.divHood[i])) {
                if ((cell.type == HEPARIN_MAP) || (cell.type == MAP_PARTICLE)) {
                    MAPcount++;
                }
            }
            if (MAPcount == 0){
                openAreas.add(G.divHood[i]);
            }
        }
        int location = openAreas.get((int)(Math.random()*openAreas.size()));
        G.NewAgentSQ(location).Init(HEAD_CELL, this.arrived, this.length+1);
        Init(BODY_CELL, this.arrived, this.length);
    }

    /**
     * Divides a cell to a random nearby location, NOT allowing overlap with vessels or MAP
     */
    public void randomDivideNotOverlap(){
        assert G != null;
        int options = MapEmptyHood(G.divHood);
        if (options >= 1) {
            G.NewAgentSQ(G.divHood[G.rng.Int(options)]).Init(HEAD_CELL, this.arrived, this.length+1);
            Init(BODY_CELL, this.arrived, this.length);
        }
    }

    /**
     * Checks if the current cell has arrive at the other side of the wound
     */
    public void checkIfArrived(){
        if (!arrived){
            if (type == HEAD_CELL){
                if (Ysq() == HeparinGrid.y-1){
                    this.arrived = true;
                    assert G != null;
                    double time = (double)G.GetTick()/6;
                    int length = this.length*16;
                    HeparinGrid.arrivedTime.add(time);
                    HeparinGrid.arrivedLengths.add(length);
                    System.out.println(HeparinGrid.arrivedTime.size()+ ") " + time + " hours, " + length + " microns");
                }
            }
        }
    }

    /**
     * Calls cell to consume appropriate amount of VEGF
     */
    public void ConsumeVEGF(){
        assert G != null;
        if ((G.VEGF.Get(Isq()) != 0) && ((type == HEAD_CELL)||(type == BODY_CELL))){
            G.VEGF.Add(Isq(), -VASCULAR_VEGF_INTAKE);
        }
    }

    /**
     * Determines whether the endothelial cells should begin dividing, according to ENDO_CELL_TICK_DELAY
     */
    public void CheckEndothelialStart(){
        assert G != null;
        if (G.GetTick() == ENDO_CELL_TICK_DELAY){
            start_endo = true;
        }
    }

    /**
     * Initializes VEGF concentrations according to macrophage presence
     */
    public void InitializeVEGF(){
        if (type == HEPARIN_MAP) {
            assert G != null;
            int occupied = MapOccupiedHood(G.Macrophage_sense_hood);
            for (int i = 0; i < occupied; i++) {
                Iterable<EndothelialCell> agents = G.IterAgents(G.Macrophage_sense_hood[i]);
                for (EndothelialCell agent : agents) {
                    if (agent.type == MACROPHAGE) {
                        G.VEGF.Set(Isq(), 1);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Performs body cell actions (e.g. splitting)
     */
    public void BodyCellActions(){
        if (type == BODY_CELL){ // just in case all heads come to a dead end, new head cells can still form
            assert G != null;
            if (G.rng.Double() < BODY_CELL_BRANCH_PROB) {
                int options2 = MapEmptyHood(G.divHood);
                if (options2 > 0) {
                    G.NewAgentSQ(G.divHood[G.rng.Int(options2)]).Init(HEAD_CELL, this.arrived, this.length+1);
                }
            }
        }
    }

    /**
     * Moves Macrophages randomly with a bias towards the opposite wound edge
     */
    public void MoveMacrophages(){
        if (type == MACROPHAGE) {
            assert G != null;

            if ((0 > Xpt()) ||(Xpt() > HeparinGrid.x-1)) {
                Dispose();
                return;
            }
            if ((0 > Ypt()) ||(Ypt() > HeparinGrid.y-1)) {
                Dispose();
                return;
            }

            if (type == MACROPHAGE){
                if (G.rng.Double() < MACROPHAGE_FORWARD_TENDENCY){
                    MoveSQ(G.I((Xpt()),(Ypt()+1)));
                    return;
                }
                int options = MapHood(G.moveHood);
                MoveSQ(G.moveHood[(int)(options*Math.random())]);
            }
        }
    }

    /**
     * Modifies the division probability of a cell depending on the presence of VEGF
     * @param divProb original division probability
     * @return modified divProb
     */
    public double ModifyDivProbNearVEGF(double divProb){
        if (HighestConcentrationVEGF() > VEGF_SENSITIVITY){
            return VEGF_DIV_PROB;
        }
        return divProb;
    }

    /**
     * Divides endothelial cells
     * @param divProb division probability
     * @param splitProb probability that the vessel will split
     */
    public void EndothelialGrowth(double divProb, double splitProb){
        if (type == HEAD_CELL && start_endo && !arrived) {
            assert G != null;
            if (G.rng.Double() < divProb){ // if cell chances to divide
                int TargetLocation = HighestConcentrationVEGF();
                if (TargetLocation != 0){
                    int cellDivLocation = HoodClosestToVEGF(TargetLocation); // take the int position and find the closest neighborhood division spot
                    if (G.PopAt(cellDivLocation) < 5){ // if the area is not too crowded
                        G.NewAgentSQ(cellDivLocation).Init(HEAD_CELL, this.arrived, this.length+1); // make a new cell there
                        Init(BODY_CELL, this.arrived, this.length);
                    }
                } else { // supposed to be random movement if there is no VEGF nearby
                    randomDivideNotOverlap();
                }

                if (G.rng.Double() < splitProb){ // maybe branch off
                    randomDivideNotOverlap();
                }
            }
        }
    }

    /**
     * Steps an agent, can be used on all implemented agents
     * @param divProb: chance of division for endothelial cells
     * @param splitProb: chance of branching for endothelial cells
     */
    public void StepCell(double divProb, double splitProb){
        divProb = ModifyDivProbNearVEGF(divProb);

        // Check if arrived
        checkIfArrived();

        // Eat VEGF
        ConsumeVEGF();

        // Make Macrophages
        initMacrophages();

        // check if endothelial cells will divide yet
        CheckEndothelialStart();

        // initialize VEGF
        InitializeVEGF();
//        if (this.type == HEPARIN_MAP){
//            G.VEGF.Set(Isq(), 1);
//        }

        // Normal MAP Cells: nothing
        BodyCellActions();

        // Move Macrophages
        MoveMacrophages();

        // dividing endothelial cells
        EndothelialGrowth(divProb, splitProb);
    }
}


// ----------------------------------------------- GRID ZONE -----------------------------------------------------------


public class HeparinGrid extends AgentGrid2D<EndothelialCell> {

    /**
     * PARAMETERS!
     */
    // BATCH RUNS
    public final static boolean BATCH_RUN = true;
    public final static boolean EXPORT_DATA = false;
    public final static int TRIALS = 5;
    public final static double[] HEPARIN_PERCENTAGES = new double[]{0.01, 0.05, 0.10, 0.2};

    // ENDOTHELIAL CELL PARAMETERS
    public final static int SIGHT_RADIUS = 3; // radius for VEGF sight
    public final static double VEGF_SENSITIVITY = 0; // minimum VEGF to attract cell growth
    public final static double BODY_CELL_BRANCH_PROB = 1.0/1000000; // opportunity for branching of body cell (multiplied by split_prob)
    public static double VASCULAR_VEGF_INTAKE = 0.1; // how much VEGF is used when a blood vessel is nearby
    public final static double VEGF_DIV_PROB = 1;
    public final static double DIV_PROB = 1; // chance of dividing not in presence of VEGF
    public final static double SPLIT_PROB = 0.01; // how likely is endothelial cell to branch
    public final static double START_VASCULAR_CHANCE = 0.05; // percent of initializing an off branch from wound site
    public final static double MACROPHAGE_SPAWN_CHANCE = 0.00005;
    public final static int MAX_MACROPHAGE_PER_SPAWN = 2;
    public final static double MACROPHAGE_FORWARD_TENDENCY  = 0.3;

    // MAP GEL PARAMETERS
    public final static int MAP_RADIUS = 3; // radius of MAP particle
    public final static int MAP_SPACING = 6; // spacing radius from center of MAP particle
    public final static int MAP_PARTICLES = 800; // number of MAP particles
    public final static double HEP_TO_MAP_RATIO = .15; // percent of MAP that are Heparin MAP
    public final static double DIFFUSION_COEFFICIENT = 0.07; // diffusion coefficient

    // MAIN METHOD PARAMETERS
    public final static int x = 200; // x dimension of the window
    public final static int y = 200; // y dimension of the window 312
    public final static int SCALE_FACTOR = 2;
    public final static int TICK_PAUSE = 1;
    public final static int TIMESTEPS = 2000; // how long will the simulation run?
    public final static int ENDO_CELL_TICK_DELAY = 200;

    //------------------------------------------------------------------------------------------------------------------

    public static int HEAD_CELL = EndothelialCell.HEAD_CELL;
    public static int BODY_CELL = EndothelialCell.BODY_CELL;
    public static int MAP_PARTICLE = EndothelialCell.MAP_PARTICLE;
    public static int HEPARIN_MAP = EndothelialCell.HEPARIN_MAP;
    public static int MACROPHAGE = EndothelialCell.MACROPHAGE;

    public static ArrayList<Double> arrivedTime = new ArrayList<>(); // the time of vessels that have arrived
    public static ArrayList<Integer> arrivedLengths = new ArrayList<>(); // the length of vessels that have arrived
    public static int population;



    Rand rng = new Rand();
    int[] divHood = Util.VonNeumannHood(false); // neighborhood for division
    int[] moveHood = Util.VonNeumannHood(false); // neighborhood for division
    int[] VEGFHood = Util.CircleHood(false, SIGHT_RADIUS); // how far can you see VEGF
    int[] MAP_rad = Util.CircleHood(true, MAP_RADIUS); // radius of MAP particles
    int[] Macrophage_sense_hood = Util.CircleHood(true, MAP_RADIUS+1);
    int[] MAP_space = Util.CircleHood(true, MAP_SPACING); // "cushion" between MAP particles

    PDEGrid2D VEGF; // Initialize PDE Grid

    /**
     * Heparin Grid constructor
     * @param x x dimension of grid
     * @param y y dimension of grid
     */
    public HeparinGrid (int x, int y) { // Constructor for the agent grid
        super(x, y, EndothelialCell.class);
        VEGF = new PDEGrid2D(x, y);
    }

    /**
     * Steps all cells
     * @param divProb probability of division
     * @param splitProb probability of branching
     */
    public void StepCells(double divProb, double splitProb){ // steps all the cells
        for (EndothelialCell endoCell : this) {
            endoCell.StepCell(divProb, splitProb);
        }
        IncTick();
        VEGF.Diffusion(DIFFUSION_COEFFICIENT);
        VEGF.Update();
    }

    /**
     * Draws the PDE window
     * @param windows the window to draw the PDE in
     */
    public void DrawPDE(GridWindow windows){ // draws the PDE window
        for (int i = 0; i < length; i++) {
            windows.SetPix(i,Util.HeatMapBGR(VEGF.Get(i)));
        }
    }

    /**
     * Draws the cell model
     * @param win the window to draw the cell model in
     */
    public void DrawModel(GridWindow win){ // Draws the Agent model
        for (int i = 0; i < length; i++) {
            int color = Util.BLACK;
            if (GetAgent(i) != null) {
                EndothelialCell cell = GetAgent(i);
                color = cell.color;
            }
            win.SetPix(i, color);
        }
    }

    /**
     * Initializes the vessels at wound edge
     * @param model the model to draw the vessels in
     * @param startVascularChance ratio of head to body vessels in wound edge
     */
    public void initVascular(@NotNull HeparinGrid model, double startVascularChance) {
        for (int i = 0; i < model.Xdim(); i++) {
            if (Math.random() < startVascularChance){
                model.NewAgentSQ(i,0).Init(EndothelialCell.HEAD_CELL, false, 0);
            } else {
                model.NewAgentSQ(i, 0).Init(EndothelialCell.BODY_CELL, false, 0);
            }
        }
    }

    /**
     * Initializes MAP particles as point particles
     * @param model model to draw the particles in
     */
    public void initMAPPointParticles(HeparinGrid model, double Heparin_Percent){
        for (int i = 0; i < MAP_PARTICLES; i++) { // creates the MAPs
            int celltype = EndothelialCell.MAP_PARTICLE;
            double chance = Math.random();
            if (chance < Heparin_Percent){
                celltype = EndothelialCell.HEPARIN_MAP;
            }
            int randx =(int)(model.xDim*Math.random());
            int randy =(int)(model.yDim*Math.random());
            model.NewAgentSQ(randx,randy).Init(celltype, false, 0);
        }
    }

    /**
     * Initializes MAP particles as full sized MAP particles
     * @param model model to draw the particles in
     */
    public void initMAPParticles(HeparinGrid model, double Heparin_Percent){
        for (int i = 0; i < x*y; i++) {
            int cellType = EndothelialCell.MAP_PARTICLE;
            double chance = Math.random();
            if (chance < Heparin_Percent){
                cellType = EndothelialCell.HEPARIN_MAP;
            }

            int occlusions = MapOccupiedHood(MAP_space, i);
            int open = MapEmptyHood(MAP_rad, i);
            if (occlusions == 0) {
                for (int j = 0; j < open; j++){
                    if (0 < MAP_rad[j] && MAP_rad[j] < x*y){
                        model.NewAgentSQ(MAP_rad[j]).Init(cellType, false, 0);
                    }
                }
            }
        }
    }

    /**
     * Can freeze simulation when there is no more growth
     * @return returns true when growth stops
     */
    public boolean checkPopForEnd(){
        if (Pop() == population){
            return true;
        }
        population = Pop();
        return false;
    }

    /**
     * Exports data to EndoData file: arrival time, endothelial cells coordinates, MAP and HepMAP coordinates
     * @throws IOException error in data export
     */
    public void ExportData() throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timestamp_string = timestamp.toString().replace(" ","_").replace(".", "-").replace(":", "-");
        Path fileName= Path.of("VascularModel\\EndoData\\" + timestamp_string + ".csv");
        StringBuilder dataset = new StringBuilder();

        // endothelial cell, MAP Particle, and Heparin MAP Data
        StringBuilder endothelial_cell_data = new StringBuilder();

        for (int x_coord = 0; x_coord < x; x_coord++) {
            for (int y_coord = 0; y_coord < y; y_coord++) {
                Iterable<EndothelialCell> agents = IterAgents(x_coord, y_coord);
                for (EndothelialCell agent : agents) {
                    if (agent.type == HEAD_CELL || agent.type == BODY_CELL){
                        if (endothelial_cell_data.length() == 0){
                            endothelial_cell_data.append("Vascular Coordinates (x-y)");
                        }
                        if (agent.type == HEAD_CELL && !agent.arrived){
                            arrivedLengths.add(agent.length);
                            arrivedTime.add(-1.0);
                        }
                        endothelial_cell_data.append(", ").append(agent.Xsq()).append("-").append(agent.Ysq());
                    }
                }
            }
        }

        // time data
        StringBuilder time_data= new StringBuilder();

        for (Double Double : arrivedTime) {
            if (time_data.length() == 0){
                time_data.append("Arrival Time (h)");
            }
            if (Double == -1) {
                time_data.append(", N/A");
            } else {
                time_data.append(", ").append(Double);
            }
        }

        // length data
        StringBuilder length_data= new StringBuilder();

        for (Integer length : arrivedLengths) {
            if (length_data.length() == 0){
                length_data.append("Length (microns)");
            }
            length_data.append(", ").append(length);
        }

        dataset.append(time_data).append("\n").append(length_data).append("\n").append(endothelial_cell_data);
        Files.writeString(fileName, dataset);
    }

    public void BatchExportData(Path datafile, int trial_number, double heparinPercentage) throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timestamp_string = timestamp.toString().replace(" ","_").replace(".", "-").replace(":", "-");
        heparinPercentage = (heparinPercentage*100);
        trial_number = trial_number+1;
        Path fileName= Path.of(datafile + "\\"+ heparinPercentage +"% "+" Trial " + trial_number + ", " + timestamp_string + ".csv");
        StringBuilder dataset = new StringBuilder();

        // endothelial cell, MAP Particle, and Heparin MAP Data
        StringBuilder endothelial_cell_data = new StringBuilder();

        for (int x_coord = 0; x_coord < x; x_coord++) {
            for (int y_coord = 0; y_coord < y; y_coord++) {
                Iterable<EndothelialCell> agents = IterAgents(x_coord, y_coord);
                for (EndothelialCell agent : agents) {
                    if (agent.type == HEAD_CELL || agent.type == BODY_CELL){
                        if (endothelial_cell_data.length() == 0){
                            endothelial_cell_data.append("Vascular Coordinates (x-y)");
                        }
                        if (agent.type == HEAD_CELL && !agent.arrived){
                            arrivedLengths.add(agent.length);
                            arrivedTime.add(-1.0);
                        }
                        endothelial_cell_data.append(", ").append(agent.Xsq()).append("-").append(agent.Ysq());
                    }
                }
            }
        }

        // time data
        StringBuilder time_data= new StringBuilder();

        for (Double Double : arrivedTime) {
            if (time_data.length() == 0){
                time_data.append("Arrival Time (h)");
            }
            if (Double == -1) {
                time_data.append(", N/A");
            } else {
                time_data.append(", ").append(Double);
            }
        }

        // length data
        StringBuilder length_data= new StringBuilder();

        for (Integer length : arrivedLengths) {
            if (length_data.length() == 0){
                length_data.append("Length (microns)");
            }
            length_data.append(", ").append(length);
        }

        dataset.append(time_data).append("\n").append(length_data).append("\n").append(endothelial_cell_data);
        Files.writeString(fileName, dataset);
    }

    public void ClearData() {
        arrivedTime.clear();
        arrivedLengths.clear();

    }

    public Path MakeFolder(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timestamp_string = timestamp.toString().replace(" ","_").replace(".", "-").replace(":", "-");
        StringBuilder HeparinPercentagesString = new StringBuilder();
        HeparinPercentagesString.append(HEPARIN_PERCENTAGES[0]*100);
        if (HEPARIN_PERCENTAGES.length > 1){
            for (int i = 1; i < HEPARIN_PERCENTAGES.length; i++) {
                HeparinPercentagesString.append("- ");
                HeparinPercentagesString.append(HEPARIN_PERCENTAGES[i]*100);
            }
        }
        Path fileName= Path.of("VascularModel\\EndoData\\" + "Batch ["+ HeparinPercentagesString + "]% " + timestamp_string + ".csv");
        File file = new File(String.valueOf(fileName));
        file.mkdirs();

        return fileName;
    }

// ----------------------------------------------- MAIN METHOD ---------------------------------------------------------

    public static void main(String[] args) throws IOException {
        GridWindow gridWin = new GridWindow("Endothelial Cells",x, y, SCALE_FACTOR); // window for agents
        GridWindow VEGFWin = new GridWindow("VEGF Diffusion", x, y, SCALE_FACTOR); // window for diffusion

        HeparinGrid model = new HeparinGrid(x, y); // instantiate agent grid

        if (!BATCH_RUN){

            // initialize
            model.ClearData();

            model.initVascular(model, START_VASCULAR_CHANCE);
            model.initMAPParticles(model, HEP_TO_MAP_RATIO);

            for (int i = 0; i < TIMESTEPS; i++){
                // pause
                gridWin.TickPause(TICK_PAUSE); // how fast the simulation runs
                // model step
                model.StepCells(DIV_PROB, SPLIT_PROB); // step the cells

                // draw
                model.DrawPDE(VEGFWin); // draw the PDE window
                model.DrawModel(gridWin); // draw the agent window
            }
            if (EXPORT_DATA){
                model.ExportData();
            }

        } else {
            Path datafile;
            if (EXPORT_DATA){
                datafile = model.MakeFolder();
            }
            for (double heparinPercentage : HEPARIN_PERCENTAGES) {
                for (int trial = 0; trial < TRIALS; trial++) {
                    // initialize
                    model.ClearData();
                    model.Reset();
                    model.ResetTick();
                    EndothelialCell.start_endo = false;
                    model.VEGF = new PDEGrid2D(x, y);
                    model.initVascular(model, START_VASCULAR_CHANCE);
                    model.initMAPParticles(model, heparinPercentage);

                    for (int i = 0; i < TIMESTEPS; i++){
                        // pause
                        gridWin.TickPause(TICK_PAUSE); // how fast the simulation runs
                        // model step
                        model.StepCells(DIV_PROB, SPLIT_PROB); // step the cells

                        // draw
                        model.DrawPDE(VEGFWin); // draw the PDE window
                        model.DrawModel(gridWin); // draw the agent window
                    }
                    if (EXPORT_DATA){
                        model.BatchExportData(datafile ,trial, heparinPercentage);
                    }
                }
            }
        }
    }
}






