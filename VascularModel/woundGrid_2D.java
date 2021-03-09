/*
GRIFFIN LAB
ALEX TAING, UNDERGRADUATE
FALL 2020
*/

package VascularModel;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.PDEGrid2D;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;

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
/// Ratio of MAP to Heparin MAP (HEPARIN_PERCENTAGES)
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


// ----------------------------------------------- GRID ZONE -----------------------------------------------------------


public class woundGrid_2D extends AgentGrid2D<agent_2D> {

    /**
     * PARAMETERS!
     */
    // BATCH RUNS
    public final static boolean BATCH_RUN = false;
    public final static boolean EXPORT_DATA = true;
    public final static int TRIALS = 2;
    public final static double[] HEPARIN_PERCENTAGES = new double[]{0.1, 0.05};

    // ENDOTHELIAL CELL PARAMETERS
    public final static int SIGHT_RADIUS = 3; // radius for VEGF sight
    public final static double VEGF_SENSITIVITY = 0; // minimum VEGF to attract cell growth
    public final static double BODY_CELL_BRANCH_PROB = 1.0/1000000; // opportunity for branching of body cell (multiplied by split_prob)
    public static double VASCULAR_VEGF_INTAKE = 0.1; // how much VEGF is used when a blood vessel is nearby
    public final static double VEGF_DIV_PROB = 1;
    public final static double DIV_PROB = 1; // chance of dividing not in presence of VEGF
    public final static double SPLIT_PROB = 0.01; // how likely is endothelial cell to branch
    public final static double START_VASCULAR_CHANCE = 0.05; // percent of initializing an off branch from wound site

    // MACROPHAGE PARAMETERS
    public final static double MACROPHAGE_SPAWN_CHANCE = 0.00005;
    public final static int MAX_MACROPHAGE_PER_SPAWN = 2;
    public final static double MACROPHAGE_FORWARD_TENDENCY  = 0.3;

    // MAP GEL PARAMETERS
    public final static int MAP_RADIUS = 3; // radius of MAP particle
    public final static int MAP_SPACING = 6; // spacing radius from center of MAP particle
    public final static double DIFFUSION_COEFFICIENT = 0.07; // diffusion coefficient
    //    public final static int MAP_PARTICLES = 800; // number of MAP particles

    // MAIN METHOD PARAMETERS
    public final static int x = 150; // x dimension of the window (94)
    public final static int y = 312; // y dimension of the window 312
    public final static int SCALE_FACTOR = 2;
    public final static int TICK_PAUSE = 1;
    public final static int TIMESTEPS = 2000; // how long will the simulation run?
    public final static int ENDO_CELL_TICK_DELAY = 200;

    //------------------------------------------------------------------------------------------------------------------

    public static int HEAD_CELL = agent_2D.HEAD_CELL;
    public static int BODY_CELL = agent_2D.BODY_CELL;
    public static int MAP_PARTICLE = agent_2D.MAP_PARTICLE;
    public static int HEPARIN_MAP = agent_2D.HEPARIN_MAP;
    public static int MACROPHAGE = agent_2D.MACROPHAGE;

    public static ArrayList<Double> arrivedTime = new ArrayList<>(); // the time of vessels that have arrived
    public static ArrayList<Integer> arrivedLengths = new ArrayList<>(); // the length of vessels upon arrival
    public static ArrayList<Integer> finalLengths = new ArrayList<>(); // the length of vessels at end of run
    public static ArrayList<Integer> invasionDepth = new ArrayList<>(); // vessel invasion depth
//    public static int anastomoses;
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
    public woundGrid_2D(int x, int y) { // Constructor for the agent grid
        super(x, y, agent_2D.class);
        VEGF = new PDEGrid2D(x, y);
    }

    /**
     * Steps all cells
     * @param divProb probability of division
     * @param splitProb probability of branching
     */
    public void StepCells(double divProb, double splitProb){ // steps all the cells
        for (agent_2D endoCell : this) {
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
                agent_2D cell = GetAgent(i);
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
    public void initVascular(woundGrid_2D model, double startVascularChance) {
        for (int i = 0; i < model.Xdim(); i++) {
            if (Math.random() < startVascularChance){
                model.NewAgentSQ(i,0).InitVascular(agent_2D.HEAD_CELL, false, 0, true);
            } else {
                model.NewAgentSQ(i, 0).InitVascular(agent_2D.BODY_CELL, false, 0, true);
            }
        }
    }

    /**
     * Initializes the vessels at both side of the wound
     * @param model the model to draw the vessels in
     * @param startVascularChance ratio of head to body vessels in wound edge
     */
    public void initVascularTwoEdges(woundGrid_2D model, double startVascularChance) {
        for (int i = 0; i < model.Xdim(); i++) {
            if (Math.random() < startVascularChance){
                model.NewAgentSQ(i,0).InitVascular(agent_2D.HEAD_CELL, false, 0, true);
            } else {
                model.NewAgentSQ(i, 0).InitVascular(agent_2D.BODY_CELL, false, 0, true);
            }
        }
        for (int i = 0; i < model.Xdim(); i++) {
            if (Math.random() < startVascularChance){
                model.NewAgentSQ(i,model.yDim-1).InitVascular(agent_2D.HEAD_CELL, false, 0, false);
            } else {
                model.NewAgentSQ(i, model.yDim-1).InitVascular(agent_2D.BODY_CELL, false, 0, false);
            }
        }
    }

//    /**
//     * Initializes MAP particles as point particles
//     * @param model model to draw the particles in
//     */
//    public void initMAPPointParticles(HeparinGrid model, double Heparin_Percent){
//        for (int i = 0; i < MAP_PARTICLES; i++) { // creates the MAPs
//            int celltype = agent_2D.MAP_PARTICLE;
//            double chance = Math.random();
//            if (chance < Heparin_Percent){
//                celltype = agent_2D.HEPARIN_MAP;
//            }
//            int randx =(int)(model.xDim*Math.random());
//            int randy =(int)(model.yDim*Math.random());
//            model.NewAgentSQ(randx,randy).Init(celltype, false, 0);
//        }
//    }

    /**
     * Initializes MAP particles as full sized MAP particles
     * @param model model to draw the particles in
     */
    public void initMAPParticles(woundGrid_2D model, double Heparin_Percent){
        for (int i = 0; i < x*y; i++) {
            int cellType = agent_2D.MAP_PARTICLE;
            double chance = Math.random();
            if (chance < Heparin_Percent){
                cellType = agent_2D.HEPARIN_MAP;
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
     * Collects the data for export
     * @return the CSV to export
     */
    public StringBuilder CollectData(){
        StringBuilder dataset = new StringBuilder();

        // endothelial cell, MAP Particle, and Heparin MAP Data
        StringBuilder endothelial_cell_data = new StringBuilder();

        for (int x_coord = 0; x_coord < x; x_coord++) {
            for (int y_coord = 0; y_coord < y; y_coord++) {
                Iterable<agent_2D> agents = IterAgents(x_coord, y_coord);
                for (agent_2D agent : agents) {
                    if (agent.type == HEAD_CELL || agent.type == BODY_CELL){
                        if (endothelial_cell_data.length() == 0){
                            endothelial_cell_data.append("Vessel Coordinates (x-y)");
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

        // arrivalLength data
        StringBuilder arrivalLength_data= new StringBuilder();

        for (Integer length : arrivedLengths) {
            if (arrivalLength_data.length() == 0){
                arrivalLength_data.append("Length at arrival (microns)");
            }
            arrivalLength_data.append(", ").append(length);
        }

        // End length and invasion depth data
        StringBuilder finalLength_data= new StringBuilder();
        StringBuilder invasionDepth_data= new StringBuilder();

        for (agent_2D cell : IterAgentsRect(0,0,x,y)){
            if (cell.type == HEAD_CELL) {
                int invDepth;
                if (cell.vesselBottom){
                    invDepth = cell.Ysq()*16;
                } else{
                    invDepth = (y-cell.Ysq())*16;
                }

                if (invasionDepth_data.length() == 0){
                    invasionDepth_data.append("Invasion depth (microns)");
                }
                invasionDepth.add(invDepth);
                invasionDepth_data.append(", ").append(invDepth);

                int finalLength = cell.length*16;
                if (finalLength_data.length() == 0){
                    finalLength_data.append("Final Length (microns)");
                }
                finalLengths.add(finalLength);
                finalLength_data.append(", ").append(finalLength);
            }
        }

        // anastamoses data
        int anastomoses = 0;

        for (int x_coord = 0; x_coord < x; x_coord++) {
            for (int y_coord = 0; y_coord < y; y_coord++) {
                int vesselCount = 0;
                Iterable<agent_2D> cells = IterAgents(x_coord, y_coord);
                for (agent_2D cell: cells) {
                    if (cell.type == HEAD_CELL || cell.type == BODY_CELL){
                        vesselCount++;
                    }
                }
                if (vesselCount > 1){
                    anastomoses++;
                }
            }
        }
        return dataset.append(time_data).append("\n").append(arrivalLength_data).append("\n").append(invasionDepth_data).append("\n").append(finalLength_data).append("\n").append(endothelial_cell_data).append("\n").append("Total Anastomoses, ").append(anastomoses);
    }

    /**
     * Exports data to EndoData file: arrival time, endothelial cells coordinates, MAP and HepMAP coordinates
     * @throws IOException error in data export
     */
    public void ExportData() throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timestamp_string = timestamp.toString().replace(" ","_").replace(".", "-").replace(":", "-");
        Path fileName= Path.of("VascularModel\\EndoData\\" + timestamp_string + ".csv");
        StringBuilder dataset = CollectData();
        Files.writeString(fileName, dataset);
    }

    /**
     * Exports data to the Batch run file inside EndoData file during batch runs
     * @param datafile the name of the batch run file
     * @param trial_number trial number
     * @param heparinPercentage percentage heparin (to be included in the file name)
     * @throws IOException if the file cannot be found
     */
    public void ExportData(Path datafile, int trial_number, double heparinPercentage) throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String timestamp_string = timestamp.toString().replace(" ","_").replace(".", "-").replace(":", "-");
        heparinPercentage = (heparinPercentage*100);
        trial_number = trial_number+1;
        Path fileName= Path.of(datafile + "\\"+ heparinPercentage +"% "+" Trial " + trial_number + ", " + timestamp_string + ".csv");
        StringBuilder dataset = CollectData();
        Files.writeString(fileName, dataset);
    }

    /**
     * Clears data from arrivedTime and arrivedLengths, just a precaution
     */
    public void ClearData() {
        arrivedTime.clear();
        arrivedLengths.clear();
        finalLengths.clear();
        invasionDepth.clear();
//        anastomoses = 0;
    }

    /**
     * makes the batch run folder to organize the data from a single batch run
     * @return the pathname of the created batch folder
     * @throws IOException if the folder cannot be made
     */
    public Path MakeBatchRunFolder() throws IOException {
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
        Path fileName= Path.of("VascularModel\\EndoData\\" + "Batch ["+ HeparinPercentagesString + "]% " + timestamp_string);
        File file = new File(String.valueOf(fileName));
        if (!file.mkdir()) {
            throw new IOException("Batch Run folder not made");
        }

        return fileName;
    }

// ----------------------------------------------- MAIN METHOD ---------------------------------------------------------
    public static void main(String[] args) throws IOException {
        GridWindow gridWin = new GridWindow("Endothelial Cells",x, y, SCALE_FACTOR); // window for agents
        GridWindow VEGFWin = new GridWindow("VEGF Diffusion", x, y, SCALE_FACTOR); // window for diffusion

        woundGrid_2D model = new woundGrid_2D(x, y); // instantiate agent grid

        Path fileName= Path.of("VascularModel\\EndoData");
        File EndoDatafile = new File(String.valueOf(fileName));

        if (EXPORT_DATA && !EndoDatafile.exists()) {
            if (!EndoDatafile.mkdir()) {
                throw new IOException("EndoData folder not made");
            }
        }

        if (!BATCH_RUN){

            // initialize
            model.ClearData();

            model.initVascularTwoEdges(model, START_VASCULAR_CHANCE);
            model.initMAPParticles(model, HEPARIN_PERCENTAGES[0]);

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
                datafile = model.MakeBatchRunFolder();
            }
            for (double heparinPercentage : HEPARIN_PERCENTAGES) {
                for (int trial = 0; trial < TRIALS; trial++) {
                    // initialize
                    model.ClearData();
                    model.Reset();
                    model.ResetTick();
                    agent_2D.start_endo = false;
                    model.VEGF = new PDEGrid2D(x, y);
                    model.initVascularTwoEdges(model, START_VASCULAR_CHANCE);
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
                        model.ExportData(datafile ,trial, heparinPercentage);
                    }
                }
            }
        }
    }
}

