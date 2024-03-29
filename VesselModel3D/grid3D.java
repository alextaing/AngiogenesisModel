package VesselModel3D;

import HAL.GridsAndAgents.AgentGrid3D;
import HAL.GridsAndAgents.PDEGrid3D;
import HAL.Gui.GridWindow;
import HAL.Gui.OpenGL3DWindow;
import HAL.Interfaces.DoubleToInt;
import HAL.Rand;
import HAL.Util;
import SproutingAssay.sproutAgent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;

public class grid3D extends AgentGrid3D<agent3D> {

    ////////////////
    // PARAMETERS //
    ////////////////


    // DATA EXPORT
    public static final boolean EXPORT_DATA = true;
    public static final boolean EXPORT_HEAD_CELL_DATA = true;
    public static final double HEAD_CELL_SAMPLE_HOURS = 1; // How frequently head cell distances will be collected

    // SCALE FACTORS
    public static final double SCALE_FACTOR = 0.1; // microns to units
    public static final double TIME_SCALE_FACTOR = 10; // hours to ticks (normally 60?) (ticks/hr)

    // GRID PROPERTIES
    public static final int x = (int)(.5 * (SCALE_FACTOR)*1000); // dimension of the wound in mm
    public static final int y = (int)(.5 * (SCALE_FACTOR)*1000); // dimension of the wound in mm
    public static final int z = (int)(.5 * (SCALE_FACTOR)*1000); // dimension of the wound in mm
    public final static int RUNTIME = (int)(168 *TIME_SCALE_FACTOR); // how long will the simulation run?
    public static final double DIFFUSION_COEFFICIENT = 0.1; // diffusion coefficient, ADI
    Rand rng = new Rand();

    // VIEW: what agents to display
    public static final boolean VIEW_MAP = false;
    public static final boolean VIEW_HEP_ISLANDS = true;
    public static final boolean VIEW_VESSELS = true;

    // MAP GEL
    public static final double HEPARIN_ISLAND_PERCENTAGE = 0.1; // enter as a decimal between 0 and 1, heparin microislands
    public static final double MAP_DIAMETER = 80 * (SCALE_FACTOR); //(microns)
    public static final double VESSEL_DIAMETER = 16 * (SCALE_FACTOR); //(microns)
    public static final double MAP_GAP =  18 * (SCALE_FACTOR); //(microns)
    public static final double TOTAL_VEGF_PRESENT = 1.0; 
    public static final double SEQUENTIAL_TURN_ON = 24 * (TIME_SCALE_FACTOR); // (hours)

    // VESSELS
    public static final int NUM_VESSELS_PER_SIDE = 12; // The number of head vessels to start the model per side
    public static final double VESSEL_VEGF_CONSUME = 0.0001; // the amount of VEGF consumed by eligible cells (body cells older than AGE_BEFORE_CONSUME
    public static final int  AGE_BEFORE_CONSUME = 25; // age (in ticks) before a body cell can start consuming VEGF: to keep consumption from interacting with head cell gradient calculation
    public static final double MIGRATION_RATE = 3 * SCALE_FACTOR/TIME_SCALE_FACTOR; // microns per hour
    public static final double VEGF_SENSITIVITY_THRESHOLD = 0.001; // Threshold for VEGF sensitivity
    public static final double MAX_ELONGATION_LENGTH = 40 * (SCALE_FACTOR); // in microns
    public static final double MAX_PERSISTENCY_TIME = 3 * (TIME_SCALE_FACTOR);
    public static final double BRANCH_DELAY = 4 * (TIME_SCALE_FACTOR); // The minimum amount of ticks between ticks (model specific, included in Mehdizadeh et al.)
    // BRANCHING PROBABILITY AND THRESHOLDS_ PROBABILITIES NEED PARAMETERIZED BUT COULD STAY FIXED
    public final static double LOW_BRANCHING_PROBABILITY= 0.4; // probability of branching while VEGF is under LOW_MED_VEGF_THRESHOLD
    public final static double LOW_MED_VEGF_THRESHOLD = 0.05;
    public final static double MED_BRANCHING_PROBABILITY= 0.55; // probability of branching while VEGF is between LOW_MED_VEGF_THRESHOLD and MED_HIGH_VEGF_THRESHOLD
    public final static double MED_HIGH_VEGF_THRESHOLD = 0.25;
    public final static double HIGH_BRANCHING_PROBABILITY= 0.9; // probability of branching while VEGF is above MED_HIGH_VEGF_THRESHOLD

    // DO NOT MODIFY

    public static final int HEAD_CELL = agent3D.HEAD_CELL;
    public static final int BODY_CELL = agent3D.BODY_CELL;
    public static final int MAP_PARTICLE = agent3D.MAP_PARTICLE;
    public static final int HEPARIN_ISLAND = agent3D.HEPARIN_ISLAND;

    public static final double MAP_RAD = (MAP_DIAMETER / 2.0);
    public static final double MAP_GAP_CENTERS = (MAP_GAP + (2 * MAP_RAD));
    public static final double VESSEL_RADIUS = VESSEL_DIAMETER/2.0;

    public static final double HEAD_CELL_SAMPLE_TICKS = (int)(HEAD_CELL_SAMPLE_HOURS*TIME_SCALE_FACTOR); // How frequently head cell distances will be collected


    // DATA EXPORT
    public static StringBuilder CSV = new StringBuilder();
    public static StringBuilder Head_cell_data_over_time = new StringBuilder();
    public static double CenterArrivalTime = -1;
    public static double QuarterArrivalTime = -1;

    /////////////////
    // MAIN METHOD //
    /////////////////

    public static void main(String[] args) throws IOException{

        // INITIALIZE WINDOWS
        OpenGL3DWindow window = new OpenGL3DWindow("Angiogenesis", 900, 900, x, y, z);
        grid3D woundGrid = new grid3D(x, y, z);
        GridWindow VEGF_xz = new GridWindow("VEGF Diffusion X-Z plane", x, z,3);

        // INITIALIZE MAP PARTICLES
        Init_MAP_Particles(woundGrid);
        Init_Vessels(woundGrid);

        // INITIALIZE CSV
        Initialize_CSV();


        // TICK ACTIONS
        for (int step = 0; step < RUNTIME; step++) {  // For each timestep
            woundGrid.StepVEGF(); // Step the gradient
            woundGrid.StepCells(); // Step all the cells
            woundGrid.DrawGrid(window); // Draw the updated window
            woundGrid.DrawGradientWindowed(VEGF_xz, Util::HeatMapBGR); // Draw the updated PDE grid
            woundGrid.DrawAgents(window); // draw the new updated agents
            woundGrid.VEGF.Update(); // Update the VEGF window with the newly drawn PDE grid
            window.Update();// Update the wound grid window with the newly drawn agents
            woundGrid.HeadCellDistOverTime();

            woundGrid.IncTick(); // Increment the time ticks

            if(window.IsClosed()){ // If the window is Xed out
                window.Close(); // close the wound grid window
                VEGF_xz.Close(); // and close the PDE window
                break; // exit the time tick loop
            }
        }

        // COLLECT DATA
        CollectVesselData(woundGrid);

        if (EXPORT_DATA){
            ExportData();
        }

        window.Close(); // close the wound grid window
        VEGF_xz.Close(); // and close the PDE window
    }

    ///////////////
    // VARIABLES //
    ///////////////

    PDEGrid3D VEGF;

    ///////////////////
    // NEIGHBORHOODS //
    ///////////////////



    //////////////////
    // CONSTRUCTORS //
    //////////////////

    /**
     * Constructs the grid
     * @param x The x dimension of the wound
     * @param y The y dimension of the wound
     * @param z The z dimension of the wound
     */
    public grid3D (int x, int y, int z) {
        super(x, y, z, agent3D.class);
        VEGF = new PDEGrid3D(x, y, z);

    }

    ////////////////////
    // INITIALIZATION //
    ////////////////////

    /**
     * Initializes the wound grid with MAP particles by starting with a random seed MAP, and recursively generating MAP
     * around it in perfect HCP packing
     * @param grid
     */
    public static void Init_MAP_Particles(grid3D grid){
        agent3D MAP_seed = grid.NewAgentPTSafe(x * Math.random(), y * Math.random(), 0);  // Generates the "seed" particles
        MAP_seed.Init(MAP_PARTICLE, MAP_RAD); // Initializes the seed as a MAP particle
        MAP_seed.Recursive_MAP_Generator(); // Creates all the particles around the seed, recursively (defined in agent3D class)
    }

    /**
     * Initializes the host vasculature by placing head cells on the wound edge
     * @param grid The wound grid that the vasculature is to be placed in
     */
    public static void Init_Vessels(grid3D grid){

        // first side
        boolean empty = true;  // whether the location is occupied with MAP gel (assume is empty)
        for (int i = 0; i < NUM_VESSELS_PER_SIDE;) {  // for as many vessels you want to start with  ("i" is tally for how many successful head vessels have been initialized)
            empty = true; // assume that the desired location is empty
            double[] location = {(x/2.0)*grid.rng.Double()+(x/4.0), (y/2.0)*grid.rng.Double()+(y/4.0), 1.5*VESSEL_RADIUS}; // starts on the z=1.5*vessel_radius plane (i.e the beginning of the wound, but with some leeway)
            for (agent3D agent : grid.IterAgentsRad(location[0], location[1], location[2], MAP_RAD+VESSEL_RADIUS)) { // Iterate through all locations around the desired point in a radius equal to MAP radius
                if (agent.type == MAP_PARTICLE || agent.type == HEPARIN_ISLAND) { // If there is a MAP particle center in that radius (meaning that it overlaps with the desired location)
                    empty = false; // the desired location is not empty
                    break; // exit the for loop since you know that it is not empty
                }
            }
            if (empty){ // BUT if it is empty, initialize a head vessel there and increment "i" which is a tally for how many vessels have been initialized
                grid.NewAgentPT(location[0], location[1], location[2]).Init_HEAD_CELL("L");
                i++;
            }
        }

        // other side
        for (int i = 0; i < NUM_VESSELS_PER_SIDE;) {  // for as many vessels you want to start with  ("i" is tally for how many successful head vessels have been initialized)
            empty = true; // assume that the desired location is empty
            double[] location = {(x/2.0)*grid.rng.Double()+(x/4.0), (y/2.0)*grid.rng.Double()+(y/4.0), z-(1.5*VESSEL_RADIUS)}; // starts on the z=1.5*vessel_radius plane (i.e the beginning of the wound, but with some leeway)
            for (agent3D agent : grid.IterAgentsRad(location[0], location[1], location[2], MAP_RAD+VESSEL_RADIUS)) { // Iterate through all locations around the desired point in a radius equal to MAP radius
                if (agent.type == MAP_PARTICLE || agent.type == HEPARIN_ISLAND) { // If there is a MAP particle center in that radius (meaning that it overlaps with the desired location)
                    empty = false; // the desired location is not empty
                    break; // exit the for loop since you know that it is not empty
                }
            }
            if (empty){ // BUT if it is empty, initialize a head vessel there and increment "i" which is a tally for how many vessels have been initialized
                grid.NewAgentPT(location[0], location[1], location[2]).Init_HEAD_CELL("R");
                i++;
            }
        }
    }

    //////////////////
    // GRID ACTIONS //
    //////////////////

    /**
     * To be called on all cells inside the grid: calls StepCell on all cells
     */
    public void StepCells() {
        for (agent3D cell: this) { // for each of the cells in the grid
            cell.StepCell(); // call them to take action
        }
    }

    /**
     * Calls the VEGF grid to take action
     */
    public void StepVEGF(){
        VEGF.DiffusionADI(DIFFUSION_COEFFICIENT);
    }

    //////////////////
    // GRID DRAWING //
    //////////////////

    /**
     * Draws the grid inside the given window
     * @param window The window in which to draw the grid
     */
    public void DrawGrid (OpenGL3DWindow window){
        window.ClearBox(Util.RGB(225/245.0,198/245.0,153/245.0), Util.BLUE); // draw the grid with background color and line color
    }

    /**
     * Draw Agents depending on their color as spheres
     * @param window The window to draw the agents in
     */
    public void DrawAgents(OpenGL3DWindow window){
        for (agent3D cell : this) {
            if ((cell.type == MAP_PARTICLE) && (!VIEW_MAP)){
                continue;
            } else if ((cell.type == HEPARIN_ISLAND) && (!VIEW_HEP_ISLANDS)) {
                continue;
            } else if (((cell.type == HEAD_CELL) || (cell.type == BODY_CELL)) && (!VIEW_VESSELS)){
                continue;
            }
            window.CelSphere(cell.Xpt(),cell.Ypt(),cell.Zpt(),cell.radius, cell.color);
        }
    }

    /**
     * Draws the gradient in a separate window from the wound grid (gradient visualized from a top-down view)
     * @param window The window to draw the gradient in
     * @param DrawConcs Use Util.HeatMap___ as the argument to visualize the diffusion
     */
    public void DrawGradientWindowed (GridWindow window, DoubleToInt DrawConcs){
        for (int x = 0; x < VEGF.xDim; x++) {
            for (int z = 0; z < VEGF.zDim; z++) {
                double VEGF_Sum=0;
                //add column to avgConcs
                for (int y = 0; y < VEGF.yDim; y++) { // add all the concentrations in a column (all y coordinates for intersection of the x-z plane)
                    VEGF_Sum+=VEGF.Get(x,y,z);
                }
//                VEGF_Sum/=VEGF.yDim;
                window.SetPix(x,z,DrawConcs.DoubleToInt(VEGF_Sum)); // draw the concentration to the x-y grid
            }
        }
    }

    /**
     * Draws the diffusion gradient on the bottom face of the wound grid visualization
     * @param window The window containing the wound grid
     * @param DrawConcs Use Util.HeatMap___ as the argument to visualize the diffusion
     */
    public void DrawGradientEmbedded (OpenGL3DWindow window, DoubleToInt DrawConcs){
        window.ClearBox(Util.RGB(225/245.0,198/245.0,153/245.0), Util.BLUE);
        for (int x = 0; x < VEGF.xDim; x++) {
            for (int z = 0; z < VEGF.zDim; z++) {
                double VEGF_Sum=0;
                //add column to avgConcs
                for (int y = 0; y < VEGF.yDim; y++) { // add all the concentrations in a column (all y coordinates for intersection of the x-z plane)
                    VEGF_Sum+=VEGF.Get(x,y,z);
                }
                VEGF_Sum/=VEGF.yDim;
                window.SetPixXZ(x,z,DrawConcs.DoubleToInt(VEGF_Sum)); // draw the concentration to the bottom face of the model
            }
        }
    }

    /////////////////
    // DATA EXPORT //
    /////////////////

    public static void CollectVesselData(grid3D G){

        // check their quadrants
        int outerQuadrant = 0; // counter for agents in the outer half of the wound (0 to x/2 since x is wound center)
        int innerQuadrant = 0; // counter for agents in the inner half of the wound (x/2 to x)
        int numHeadCells = 0;

        for (agent3D agent3D : G.IterAgentsRect(0, 0, 0, x, y, z)) {
            if (agent3D.type == HEAD_CELL){
                numHeadCells ++;
            }
            if ((agent3D.type == HEAD_CELL) || (agent3D.type == BODY_CELL)){
                if (agent3D.Zpt() < z/2.0) {
                    outerQuadrant++;
                } else{
                    innerQuadrant ++;
                }
            }
        }
        int totalAgents = outerQuadrant + innerQuadrant;

        double ratioInner = innerQuadrant/(totalAgents*1.0);
        double ratioOuter = outerQuadrant/(totalAgents*1.0);
        double totalVesselLength = ((totalAgents*VESSEL_RADIUS) + (numHeadCells*VESSEL_RADIUS))/SCALE_FACTOR;

        CSV.append("\n" + HEPARIN_ISLAND_PERCENTAGE + ", ").append(totalVesselLength).append(", ").append(ratioInner).append(", ").append(ratioOuter).append(", ").append(QuarterArrivalTime).append(", ").append(CenterArrivalTime);

        // Reset Arrival Times
        QuarterArrivalTime = -1;
        CenterArrivalTime = -1;

    }


    public static void Initialize_CSV(){
        CSV.append("Heparin Percentage (%), Total BVL (microns), Inner Quadrant BV percentage, Outer Quadrant BV percentage, Quarter Arrival Time (h), Center Arrival Time (h)");
        Head_cell_data_over_time.append("Time (h), Head Cell Distances from wound edge (microns)");
    }


    public static void ExportData() throws IOException {
        Path fileName= Path.of("VesselModel3D\\Model3D_Data");
        File Model3D_Datafile = new File(String.valueOf(fileName));
        if (!Model3D_Datafile.exists()) {
            if (!Model3D_Datafile.mkdir()) {
                throw new IOException("Model3D_Data folder not made");
            }
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        String timestamp_string = ((timestamp.toString().replace(" ","_").replace(".", "-").replace(":", "-")).substring(0, 10) +" " + (HEPARIN_ISLAND_PERCENTAGE*100) + "%");
        Path fileName3D= Path.of("VesselModel3D\\Model3D_Data\\" + timestamp_string + " 3DModel.csv");
        int i = 0;
        while (Files.exists(fileName3D)){
            i++;
            fileName3D= Path.of("VesselModel3D\\Model3D_Data\\" + timestamp_string + " (" + i + ")" + "3DModel.csv");
        }
        Files.writeString(fileName3D, CSV);

        if (EXPORT_HEAD_CELL_DATA){
            Path fileNameHead= Path.of("VesselModel3D\\Model3D_Data\\" + timestamp_string + " 3DModel_HeadCellTimeData.csv");
            if (i != 0) {
                fileNameHead = Path.of("VesselModel3D\\Model3D_Data\\" + timestamp_string + " (" + i + ")" + "3DModel_HeadCellTimeData.csv");
            }
            Files.writeString(fileNameHead, Head_cell_data_over_time);
        }
    }

    public void ClearHeadCellTimeData() {
        Head_cell_data_over_time.setLength(0);
    }

    public void HeadCellDistOverTime(){
        if (EXPORT_HEAD_CELL_DATA){
            if (GetTick()%HEAD_CELL_SAMPLE_TICKS == 0){
                Head_cell_data_over_time.append("\n").append(GetTick()/TIME_SCALE_FACTOR);

                for (agent3D agent3D : this.IterAgentsRect(0, 0, 0, x, y, z)) {
                    if (agent3D.type == HEAD_CELL){
                        if (agent3D.side.equals("L")){
                            Head_cell_data_over_time.append(",").append(agent3D.Zpt()/(SCALE_FACTOR));
                        } else if (agent3D.side.equals("R")) {
                            Head_cell_data_over_time.append(",").append((z-agent3D.Zpt())/(SCALE_FACTOR));
                        }
                    }
                }
            }
        }
    }
}