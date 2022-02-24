package VesselModel3D;

import HAL.GridsAndAgents.AgentGrid3D;
import HAL.GridsAndAgents.PDEGrid3D;
import HAL.Gui.GridWindow;
import HAL.Gui.OpenGL3DWindow;
import HAL.Interfaces.DoubleToInt;
import HAL.Rand;
import HAL.Util;

import java.util.concurrent.ForkJoinWorkerThread;

public class grid3D extends AgentGrid3D<agent3D> {

    ////////////////
    // PARAMETERS //
    ////////////////

    public static final double SCALE_FACTOR = 0.1;

    // VIEW: what agents to display
    public static final boolean VIEW_MAP = false;
    public static final boolean VIEW_HEP_ISLANDS = true;
    public static final boolean VIEW_MACROPHAGES = true;
    public static final boolean VIEW_VESSELS = true;

    // MAP GEL
    public static final double HEPARIN_ISLAND_PERCENTAGE = 0.1; // enter as a decimal between 0 and 1, heparin microislands
    public static final double MAP_DIAMETER = 80 * (SCALE_FACTOR); //(microns)
    public static final double VESSEL_DIAMETER = 16 * (SCALE_FACTOR); //(microns)
    public static final double MAP_GAP =  18 * (SCALE_FACTOR); //(microns)

    // VESSELS
    public static final int NUM_VESSELS = 12; // The number of head vessels to start the model
    public static final int BRANCH_DELAY = 50; // The minimum amount of ticks between ticks (model specific, included in Mehdizadeh et al.)
    public static final double BRANCH_PROB = 0.001; // The probability of a head cell to branch given that it has been longer since BRANCH_DELAY since it last branched
    public static final double VESSEL_VEGF_CONSUME = 0.001; // the amount of VEGF consumed by eligible cells (body cells older than AGE_BEFORE_CONSUME
    public static final int  AGE_BEFORE_CONSUME = 25; // age (in ticks) before a body cell can start consuming VEGF: to keep consumption from interacting with head cell gradient calculation
    public static final double MIGRATION_RATE = 1; // microns per hour
    public static final double VEGF_SENSITIVITY_THRESHOLD = 0.001; // Threshold for VEGF sensitivity
    public static final double MAX_ELONGATION_LENGTH = 100;
    public static final double MAX_PERSISTENCY_TIME = 100;

    // GRID PROPERTIES
    public static final int x = (int)(.5 * (SCALE_FACTOR)*1000); // dimension of the wound in mm
    public static final int y = (int)(.5 * (SCALE_FACTOR)*1000); // dimension of the wound in mm
    public static final int z = (int)(1 * (SCALE_FACTOR)*1000); // dimension of the wound in mm
    public final static int TICK_PAUSE = 1; // The time between ticks (not essential to model, just determines running speed)
    public final static int TIMESTEPS = 10000; // how long will the simulation run?
    Rand rng = new Rand();

    // DO NOT MODIFY

    public static final int HEAD_CELL = agent3D.HEAD_CELL;
    public static final int BODY_CELL = agent3D.BODY_CELL;
    public static final int MAP_PARTICLE = agent3D.MAP_PARTICLE;
    public static final int HEPARIN_ISLAND = agent3D.HEPARIN_ISLAND;
    public static final int MACROPHAGE = agent3D.MACROPHAGE;

    public static final double MAP_RAD = (MAP_DIAMETER / 2.0);
    public static final double MAP_GAP_CENTERS = (MAP_GAP + (2 * MAP_RAD));
    public static final double VESSEL_RADIUS = VESSEL_DIAMETER/2.0;

    // CONVERSIONS


    /////////////////
    // MAIN METHOD //
    /////////////////

    public static void main(String[] args) {

        // INITIALIZE WINDOWS
        OpenGL3DWindow window = new OpenGL3DWindow("Angiogenesis", 900, 900, x, y, z);
        grid3D woundGrid = new grid3D(x, y, z);
        GridWindow VEGF_xz = new GridWindow("VEGF Diffusion X-Z plane", x, z,3);

        // INITIALIZE MAP PARTICLES
        Init_MAP_Particles(woundGrid);
        Init_Vessels(woundGrid);

        // TICK ACTIONS
        for (int step = 0; step < TIMESTEPS; step++) {  // For each timestep
            woundGrid.StepVEGF(); // Step the gradient
            woundGrid.StepCells(0.5); // Step all the cells
            woundGrid.DrawGrid(window); // Draw the updated window
            woundGrid.DrawGradientWindowed(VEGF_xz, Util::HeatMapBGR); // Draw the updated PDE grid
            woundGrid.DrawAgents(window); // draw the new updated agents
            woundGrid.VEGF.Update(); // Update the VEGF window with the newly drawn PDE grid
            window.Update();// Update the wound grid window with the newly drawn agents

            woundGrid.IncTick(); // Increment the time ticks

            if(window.IsClosed()){ // If the window is Xed out
                window.Close(); // close the wound grid window
                VEGF_xz.Close(); // and close the PDE window
                break; // exit the time tick loop
            }
        }
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
        boolean empty = true;  // whether the location is occupied with MAP gel (assume is empty)
        for (int i = 0; i < NUM_VESSELS;) {  // for as many vessels you want to start with  ("i" is tally for how many successful head vessels have been initialized)
            empty = true; // assume that the desired location is empty
            double[] location = {x*grid.rng.Double(), y*grid.rng.Double(), 1.5*VESSEL_RADIUS}; // starts on the z=1.5*vessel_radius plane (i.e the beginning of the wound, but with some leeway)
            for (agent3D agent : grid.IterAgentsRad(location[0], location[1], location[2], MAP_RAD+VESSEL_RADIUS)) { // Iterate through all locations around the desired point in a radius equal to MAP radius
                if (agent.type == MAP_PARTICLE || agent.type == HEPARIN_ISLAND) { // If there is a MAP particle center in that radius (meaning that it overlaps with the desired location)
                    empty = false; // the desired location is not empty
                    break; // exit the for loop since you know that it is not empty
                }
            }
            if (empty){ // BUT if it is empty, initialize a head vessel there and increment "i" which is a tally for how many vessels have been initialized
                grid.NewAgentPT(location[0], location[1], location[2]).Init_HEAD_CELL(location);
                i++;
            }
        }
    }

    //////////////////
    // GRID ACTIONS //
    //////////////////

    /**
     * To be called on all cells inside the grid: calls StepCell on all cells
     * @param divProb
     */
    public void StepCells(double divProb) {
        for (agent3D cell: this) { // for each of the cells in the grid
            cell.StepCell(divProb); // call them to take action
        }
    }

    /**
     * Calls the VEGF grid to take action
     */
    public void StepVEGF(){
        VEGF.DiffusionADI(0.1);
    } // TODO: add as parameter (0.733 in 2D)(changed to ADI)

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
            } else if ((cell.type == MACROPHAGE) && (!VIEW_MACROPHAGES)){
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

    // NOT YET IMPLEMENTED

}