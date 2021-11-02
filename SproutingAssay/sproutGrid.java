/*
GRIFFIN LAB
ALEX TAING, UNDERGRADUATE
FALL 2020
*/

package SproutingAssay;

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

public class sproutGrid extends AgentGrid2D<sproutAgent> {


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////      PARAMETERS      /////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    // BATCH RUNS
    public final static boolean BATCH_RUN = true;
    public final static boolean EXPORT_DATA = true;
    public final static boolean EXPORT_TIME_DATA = true; // (note that EXPORT_DATA must also be true as well to export time data)
    public final static boolean EXPORT_HEAD_CELL_DISTANCE_DATA = true; // (note that EXPORT_DATA must also be true as well to export distance data)
    public final static int TRIALS = 2;
    public final static double[] HEPARIN_PERCENTAGES = new double[]{0.1,0.15, 0.2};
    public final static double FOLD_CHANGE_SAMPLE_TIME = 0.25 ; // every ___ hours
    //public final static double[] DIFFUSION_COEFFICIENT = new double[]{0.01, 0.04, 0.07, 0.10, 0.13};

    // VESSEL PARAMETERS FIXED!
    public static final int CULTURE_RADIUS_MICRONS = 140; // radius of the initial spheroid in microns
    public final static int SIGHT_RADIUS_MICRONS = 20; // how far a EC can sense in microns
    public final static int PERSISTENCY_TIME_PER_HOUR = 3; // time between a EC changes direction in hours
    public static final int MAX_ELONGATION_LENGTH_MICRONS = 40; // microns
    public final static int MIGRATION_RATE_MICRONS_PER_HOUR = 30; // microns/hr

    // VESSEL PARAMETERS NEEDING PARAMETERIZED AND SENSITIVITY ANALYSIS
    public final static double VEGF_SENSITIVITY = 0.005; // minimum VEGF to attract cell growth
    public static double VESSEL_VEGF_INTAKE = 0.1; // percent of how much of the present VEGF is consumed when a blood vessel is nearby
    public final static double INITIAL_PERCENT_HEAD_CELLS = 0.1; // probability of initializing an off branch from wound site
    public final static double VEGF_DEGRADATION_RATE = 0;//
    // BRANCHING PROBABILITY AND THRESHOLDS_ PROBABILITIES NEED PARAMETERIZED BUT COULD STAY FIXED
    public final static double LOW_BRANCHING_PROBABILITY= 0.4; // probability of branching while VEGF is under LOW_MED_VEGF_THRESHOLD
    public final static double LOW_MED_VEGF_THRESHOLD = 0.05;
    public final static double MED_BRANCHING_PROBABILITY= 0.6; // probability of branching while VEGF is between LOW_MED_VEGF_THRESHOLD and MED_HIGH_VEGF_THRESHOLD
    public final static double MED_HIGH_VEGF_THRESHOLD = 0.66;
    public final static double HIGH_BRANCHING_PROBABILITY= 0.9; // probability of branching while VEGF is above MED_HIGH_VEGF_THRESHOLD

    // MAP GEL PARAMETERS - FIXED
    public final static int MAP_RADIUS_MICRONS = 40; // microns
    public final static int MAP_SPACING_MICRONS = 15; // microns
    public final static double HEP_MAP_VEGF_RELEASE = 1; // how much VEGF to add per media exchange
    public final static double MEDIA_EXCHANGE_SCHEDULE_HOURS = 24; // exchange media to refresh VEGF every __ hours
    //public final static double HEPARIN_PERCENTAGES = 0.1; // percentage of heparin particles

    // MAIN METHOD PARAMETERS - FIXED
    public final static int x_MICRONS = 2000; // microns
    public final static int y_MICRONS = 2000; // microns
    public final static int SCALE_FACTOR = 2;
    public final static int TICK_PAUSE = 1;
    public final static int RUNTIME_HOURS = 48; // how long will the simulation run?
    public final static double VESSEL_GROWTH_DELAY_HOURS = 2;

    // DIFFUSION - NEEDS PARAMETERIZED
    public final static double DIFFUSION_COEFFICIENT = 0.07; // diffusion coefficient



/////////////////////////////////////////////      CONVERSIONS      ////////////////////////////////////////////////////

    // FACTORS
    public final static int MICRONS_PER_PIXEL = 10; // 1 pixel represents 10 microns
    public final static int TICKS_PER_HOUR = 60; // 1 tick represents 1 minute

    // vessels
    public static final int CULTURE_RADIUS = CULTURE_RADIUS_MICRONS/ MICRONS_PER_PIXEL;
    public final static int SIGHT_RADIUS = SIGHT_RADIUS_MICRONS/ MICRONS_PER_PIXEL; // radius to detect VEGF
    public static final int MAX_ELONGATION_LENGTH = MAX_ELONGATION_LENGTH_MICRONS/ MICRONS_PER_PIXEL;
    public final static double MIGRATION_RATE = 1/((MIGRATION_RATE_MICRONS_PER_HOUR/(double) MICRONS_PER_PIXEL)*(1/(double)TICKS_PER_HOUR)); // convert to "elongate every ___ ticks"
    public final static double PERSISTENCY_TIME = PERSISTENCY_TIME_PER_HOUR * TICKS_PER_HOUR;

    // particles
    public final static int MAP_RADIUS = MAP_RADIUS_MICRONS/ MICRONS_PER_PIXEL; // radius of MAP particle
    public final static int MAP_SPACING = MAP_RADIUS_MICRONS/ MICRONS_PER_PIXEL + MAP_SPACING_MICRONS/ MICRONS_PER_PIXEL; // spacing radius between MAP gel centers
    public final static double MEDIA_EXCHANGE_SCHEDULE_TICKS = MEDIA_EXCHANGE_SCHEDULE_HOURS*TICKS_PER_HOUR;

    // grid
    public final static int x = x_MICRONS/ MICRONS_PER_PIXEL; // microns
    public final static int y = y_MICRONS/ MICRONS_PER_PIXEL; // microns

    // runtime
    public final static int TIMESTEPS = RUNTIME_HOURS*TICKS_PER_HOUR; // how long will the simulation run?
    public final static int VESSEL_GROWTH_DELAY = (int)(VESSEL_GROWTH_DELAY_HOURS*TICKS_PER_HOUR);
    public final static int FOLD_CHANGE_SAMPLE_TICKS = (int)(FOLD_CHANGE_SAMPLE_TIME*TICKS_PER_HOUR); // take a sample every ____ ticks



//////////////////////////////////////////////      MISC VARIABLES      ////////////////////////////////////////////////

    public static int HEAD_CELL = sproutAgent.HEAD_CELL;
    public static int BODY_CELL = sproutAgent.BODY_CELL;

    Rand rng = new Rand();
    int[] divHood = Util.VonNeumannHood(false); // neighborhood for division
    int[] VEGFHood = Util.CircleHood(false, SIGHT_RADIUS); // how far can you see VEGF
    int[] MAP_rad = Util.CircleHood(true, MAP_RADIUS); // radius of MAP particles
    int[] MAP_space = Util.CircleHood(true, MAP_SPACING); // "cushion" between MAP particles

    PDEGrid2D VEGF; // Initialize PDE Grid



////////////////////////////////////////////////      DATA EXPORT      /////////////////////////////////////////////////

    public static StringBuilder CSV = new StringBuilder();
    public static StringBuilder TIME_CSV = new StringBuilder();
    public static StringBuilder HEAD_CSV = new StringBuilder();
    public int initialCultureSize;
    public ArrayList<Double> foldChangeOverTime = new ArrayList<>();
    public ArrayList<Double> FCTime = new ArrayList<>();



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////      METHODS      ///////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    /**
     * Heparin Grid constructor
     * @param x x dimension of grid
     * @param y y dimension of grid
     */
    public sproutGrid(int x, int y) { // Constructor for the agent grid
        super(x, y, sproutAgent.class);
        VEGF = new PDEGrid2D(x, y);
    }


    /**
     * Steps all cells
     */
    public void StepCells(){ // steps all the cells
        for (sproutAgent endoCell : this) {
            endoCell.StepCell();
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
                sproutAgent cell = GetAgent(i);
                color = cell.color;
            }
            win.SetPix(i, color);
        }
    }


    /**
     * Replaces Dist function, which DOES NOT WORK with initVesselsCircleCulture
     * @param x1 first x coord
     * @param y1 first y coord
     * @param x2 second x coord
     * @param y2 second y coord
     * @return the integer distance between them
     */
    public int distance(double x1, double y1, double x2, double y2){
        double dist = Math.sqrt((Math.pow((x1-x2), 2)+Math.pow((y1-y2), 2)));
        return (int)dist;
    }


    /**
     * Initializes the vessels at both side of the wound
     * @param model the model to draw the vessels in
     */
    public void initVesselsCircleCulture(sproutGrid model) {

        int center = I((Xdim()/2), (Ydim()/2));
        for (int i = 0; i < (model.Xdim()*model.Ydim()); i++) {
            double dist = distance(ItoX(i), ItoY(i), ItoX(center), ItoY(center));
            if (dist < (CULTURE_RADIUS)) {
                if (dist > (CULTURE_RADIUS - 2)){
                    if (Math.random() < INITIAL_PERCENT_HEAD_CELLS) { // may be head cells or body cells
                        model.NewAgentSQ(i).InitVessel(sproutAgent.HEAD_CELL, 0);
                    }
                } else {
                    model.NewAgentSQ(i).InitVessel(sproutAgent.BODY_CELL, 0);
                }
            }
        }
    }


    /**
     * Initializes MAP particles as full sized MAP particles
     * @param model model to draw the particles in
     */
    public void initMAPParticles(sproutGrid model, double Heparin_Percent){

        // Iterate through every coordinate in the grid
        for (int i = 0; i < x*y; i++) {
            int cellType = sproutAgent.MAP_PARTICLE; // assume that it will be a MAP particle
            double chance = Math.random();
            if (chance < Heparin_Percent){ // if chosen probability dictates that it will he a heparin microIsland
                cellType = sproutAgent.HEPARIN_MAP;// then its type will be changed to heparin microIsland
            }

            int occlusions = MapOccupiedHood(MAP_space, i); // Check a radius around the chosen coordinate equal to the radius of the MAP particle with proper spacing
            int open = MapEmptyHood(MAP_rad, i);
            if (occlusions == 0) { // if there are no occlusions
                for (int j = 0; j < open; j++){ // then make the MAP particle (or Heparin microIsland)
                    if (0 < MAP_rad[j] && MAP_rad[j] < x*y){
                        model.NewAgentSQ(MAP_rad[j]).Init(cellType);
                    }
                }
            }
        }
    }

    
    public int countVessels() {
        int vessel_unit_counter = 0; // total vessel length calculated by taking the number of vessel units and multiplying by conversion factor
        for (int x_coord = 0; x_coord < x; x_coord++) {
            for (int y_coord = 0; y_coord < y; y_coord++) {
                Iterable<sproutAgent> agents = IterAgents(x_coord, y_coord);
                for (sproutAgent agent : agents) {
                    if (agent.type == HEAD_CELL || agent.type == BODY_CELL){
                        vessel_unit_counter ++;
                        break;
                    }
                }
            }
        }
        return vessel_unit_counter;
    }
    
    
    /**
     * Collects the data for export
     */
    public void CollectData(double heparinPercentage){
        // vessel cells, MAP Particle, and Heparin MAP Data
        CSV.append("\n");
        
        // Note percentage
        CSV.append((int)(heparinPercentage*100)).append("%,");
        
        // Total vessel length
        int numVessels = countVessels();
        CSV.append((numVessels-initialCultureSize)* MICRONS_PER_PIXEL).append(","); // total length of vessels
        
        // Fold change
        double foldChange = numVessels/(double)initialCultureSize;
        CSV.append(foldChange).append(",");

        // Head cell data
        ArrayList<Double> headCellDistances = FinalHeadCellDistance();

        //diffusion coefficient
        CSV.append(DIFFUSION_COEFFICIENT).append(",");

        //vegf sensitivity
        CSV.append(VEGF_SENSITIVITY).append(",");

        //vessel growth delay
        CSV.append(VESSEL_GROWTH_DELAY_HOURS).append(",");

        //initial percent head cells
        CSV.append(INITIAL_PERCENT_HEAD_CELLS).append(",");

        //vessel vegf intake
        CSV.append(VESSEL_VEGF_INTAKE).append(",");

        //hours
        CSV.append(RUNTIME_HOURS).append(",");

        //vegf degradation
        CSV.append(VEGF_DEGRADATION_RATE).append(",");

        //Low-med threshold
        CSV.append(LOW_MED_VEGF_THRESHOLD).append(",");

        //med-high threshold
        CSV.append(MED_HIGH_VEGF_THRESHOLD).append(",");

        //VEGF in each hep particle
        CSV.append(HEP_MAP_VEGF_RELEASE).append(",");

        //How frequently hep-map vegf VEGF is released
        CSV.append(MEDIA_EXCHANGE_SCHEDULE_HOURS);

        // TIME DATA

        if (EXPORT_TIME_DATA){
            if (TIME_CSV.length() == 0){
                TIME_CSV.append("Time (hours), ");
                for (Double time : FCTime) {
                    TIME_CSV.append(time).append(",");
                }
                TIME_CSV.append("\n");
            }
            TIME_CSV.append("[").append((int) (heparinPercentage * 100)).append("%] Fold Change, ");
            for (Double TimeFoldChange : foldChangeOverTime) {
                TIME_CSV.append(TimeFoldChange).append(",");
            }
            TIME_CSV.append("\n");

        }

        // HEAD CELL DISTANCE DATA

        if (EXPORT_HEAD_CELL_DISTANCE_DATA){
            if (HEAD_CSV.length() == 0){
                HEAD_CSV.append("Final Head Cell Distances (microns) ").append("\n");
            }
            HEAD_CSV.append("[").append((int) (heparinPercentage * 100)).append("%],");
            for (Double distance : headCellDistances) {
                HEAD_CSV.append(distance).append(",");
            }
            HEAD_CSV.append("\n");

        }

    }

    public ArrayList<Double> FinalHeadCellDistance(){
        // Head cell distances
        ArrayList<Double> head_cell_distances = new ArrayList<>();
        int center_x = (Xdim()/2);
        int center_y = (Ydim()/2);
        for (int x_coord = 0; x_coord < x; x_coord++) {
            for (int y_coord = 0; y_coord < y; y_coord++) {
                Iterable<sproutAgent> agents = IterAgents(x_coord, y_coord);
                for (sproutAgent agent : agents) {
                    if (agent.type == HEAD_CELL){
                        double dist = (distance(x_coord, y_coord, center_x, center_y))*MICRONS_PER_PIXEL;
                        head_cell_distances.add(dist);
                    }
                }
            }
        }

        return head_cell_distances;
    }


    /**
     * Exports data to SproutingAssayData file: arrival time, endothelial cells coordinates, MAP and HepMAP coordinates
     * @throws IOException error in data export
     */
    public void ExportData() throws IOException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        ArrayList<Integer> percentages = new ArrayList<>();
        if (BATCH_RUN) {
            for (double percentage : HEPARIN_PERCENTAGES) {
                percentage = percentage*100;
                percentages.add((int)percentage);
            }
        } else {
            percentages.add((int)(HEPARIN_PERCENTAGES[0]*100));
        }

        String timestamp_string = ((timestamp.toString().replace(" ","_").replace(".", "-").replace(":", "-")).substring(0, 10) +" " + (percentages) + "%");
        Path fileName= Path.of("SproutingAssay\\SproutingAssayData\\" + timestamp_string + "sensitivitydegradation.csv");
        Path timeDataFileName =  Path.of("SproutingAssay\\SproutingAssayData\\" + timestamp_string + "sensitivity_timeData.csv");
        Path headCellDistanceFileName = Path.of("SproutingAssay\\SproutingAssayData\\" + timestamp_string + "sensitivity_headCellDistance.csv");
        int i = 1;
        while (Files.exists(fileName)){
            fileName= Path.of("SproutingAssay\\SproutingAssayData\\" + timestamp_string + " (" + i + ")" + "sensitivitydegradation.csv");
            timeDataFileName =  Path.of("SproutingAssay\\SproutingAssayData\\" + timestamp_string + " (" + i + ")" + "sensitivity_timeData.csv");
            headCellDistanceFileName = Path.of("SproutingAssay\\SproutingAssayData\\" + timestamp_string + " (" + i + ")" + "sensitivity_headCellDistance.csv");
            i++;
        }
        StringBuilder dataset = CSV;
        Files.writeString(fileName, dataset);

        if (EXPORT_TIME_DATA){
            StringBuilder timeDataset = TIME_CSV;
            Files.writeString(timeDataFileName, timeDataset);
        }

        if (EXPORT_HEAD_CELL_DISTANCE_DATA){
            StringBuilder timeDataset = HEAD_CSV;
            Files.writeString(headCellDistanceFileName, timeDataset);
        }

    }

    
    public void Initialize_CSV(){
        CSV.append("Heparin Percentage (%), Total Vessel Length (microns), Fold Change (%), Diffusion Coefficient, VEGF Sensitivity, Vessel Growth Delay, Initial percent head cells, VEGF intake, hours, vegfdegradation, low-med thres, med-high thres, vegf loaded, media exchange");
    }


    public void CalculateFoldChangeOverTime(){
        if (GetTick()%FOLD_CHANGE_SAMPLE_TICKS == 0){
            FCTime.add(((double)GetTick())/TICKS_PER_HOUR);
            int currentVesselCount = countVessels();
            double foldChange = (double)(currentVesselCount)/initialCultureSize;
            foldChangeOverTime.add(foldChange);
        }
    }

    public void ClearFoldChange() {
        this.foldChangeOverTime.clear();
    }


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////      MAIN METHOD      /////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    public static void main(String[] args) throws IOException {
        GridWindow gridWin = new GridWindow("Endothelial Cells",x, y, SCALE_FACTOR); // window for agents
        GridWindow VEGFWin = new GridWindow("VEGF Diffusion", x, y, SCALE_FACTOR); // window for diffusion

        sproutGrid model = new sproutGrid(x, y); // initialize agent grid

        Path fileName= Path.of("SproutingAssay\\SproutingAssayData");
        File SproutingAssayDatafile = new File(String.valueOf(fileName));

        if (EXPORT_DATA && !SproutingAssayDatafile.exists()) {
            if (!SproutingAssayDatafile.mkdir()) {
                throw new IOException("SproutingAssayData folder not made");
            }
        }

        if (!BATCH_RUN){
            // Initialize
            model.initVesselsCircleCulture(model);
            model.initMAPParticles(model, HEPARIN_PERCENTAGES[0]);

            model.Initialize_CSV();
            model.initialCultureSize= model.countVessels(); // used for the initial vessel count in fold change
            for (int i = 0; i < TIMESTEPS; i++){
                // pause
                gridWin.TickPause(TICK_PAUSE); // how fast the simulation runs
                // model step
                model.StepCells(); // step the cells

                // draw
                model.DrawPDE(VEGFWin); // draw the PDE window
                model.DrawModel(gridWin); // draw the agent window
                model.CalculateFoldChangeOverTime();
            }
            if (EXPORT_DATA){
                model.CollectData(HEPARIN_PERCENTAGES[0]);
                model.ExportData();
            }

        } else {
            model.Initialize_CSV();

            for (double heparinPercentage : HEPARIN_PERCENTAGES) { // For each percentage listen in HEPARIN_PERCENTAGES
                for (int trial = 0; trial < TRIALS; trial++) { // perform the amount of trials specified in TRIALS
                    // initialize
                    model.Reset(); // reset the model
                    model.ResetTick(); // reset the time tick
                    model.ClearFoldChange();
                    model.VEGF = new PDEGrid2D(x, y); // initialize the diffusion grid
                    model.initVesselsCircleCulture(model); // initialize vessels
                    model.initMAPParticles(model, heparinPercentage); // initialize MAP particles

                    model.initialCultureSize= model.countVessels(); // used for the initial vessel count in fold change
                    for (int i = 0; i < TIMESTEPS; i++){
                        // pause
                        gridWin.TickPause(TICK_PAUSE); // how fast the simulation runs
                        // model step
                        model.StepCells(); // step the cells

                        // draw
                        model.DrawPDE(VEGFWin); // draw the PDE window
                        model.DrawModel(gridWin); // draw the agent window
                        model.CalculateFoldChangeOverTime();
                    }
                    if (EXPORT_DATA){
                        model.CollectData(heparinPercentage);
                    }
                }
            }
            if (EXPORT_DATA){
                model.ExportData();
            }
        }
    }
}

