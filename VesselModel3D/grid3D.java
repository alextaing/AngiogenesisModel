package VesselModel3D;

import HAL.GridsAndAgents.AgentGrid3D;
import HAL.GridsAndAgents.PDEGrid3D;
import HAL.Gui.GridWindow;
import HAL.Gui.OpenGL3DWindow;
import HAL.Interfaces.DoubleToInt;
import HAL.Rand;
import HAL.Util;

public class grid3D extends AgentGrid3D<agent3D> {

    ////////////////
    // PARAMETERS //
    ////////////////

    public static final int SCALE_FACTOR = 2;

    // VIEW: what agents to display
    public static final boolean VIEW_MAP = true;
    public static final boolean VIEW_HEP_ISLANDS = true;
    public static final boolean VIEW_MACROPHAGES = true;
    public static final boolean VIEW_VESSELS = true;

    // MAP GEL
    public static final double HEPARIN_ISLAND_PERCENTAGE = 0.1; // enter as a decimal between 0 and 1, heparin microislands
    public static final double MAP_DIAMETER = 16;//80 * (SCALE_FACTOR);
    public static final double VESSEL_DIAMETER = 2;
    public static final double MAP_GAP =  10; //16 * (SCALE_FACTOR);

    // VESSELS

    // AGENT PARAMETERS
    public static final double divProb = 0.3;

    // GRID PROPERTIES
    public static final int x = 100 ;//* (SCALE_FACTOR);
    public static final int y = 50 ;//* (SCALE_FACTOR);
    public static final int z = 100 ;//* (SCALE_FACTOR);
    public final static int TICK_PAUSE = 1;
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


    /////////////////
    // MAIN METHOD //
    /////////////////

    public static void main(String[] args) {

        // INITIALIZE WINDOWS
        OpenGL3DWindow window = new OpenGL3DWindow("Angiogenesis", 900, 900, x, y, z);
        grid3D woundGrid = new grid3D(x, y, z);
        GridWindow VEGF_xz = new GridWindow("VEGF Diffusion X-Z plane", x, z);

        // INITIALIZE MAP PARTICLES
//        Init_MAP_Particles(woundGrid);


        // TEST INITIALIZATION OF VESSELS ------ WILL BE REPLACED!!
        agent3D cell = woundGrid.NewAgentPT(10, 10, 10);
//        agent3D cell2 = woundGrid.NewAgentPT(40, 50, 30);
//        agent3D cell3 = woundGrid.NewAgentPT(30, 20, 50);

        cell.pastLocation = new double[]{10, 10, 10};
//        cell2.pastLocation = new double[]{40, 50, 30};
//        cell3.pastLocation = new double[]{30, 20, 50};
        cell.Init(HEAD_CELL, VESSEL_RADIUS);
//        cell2.Init(HEAD_CELL, VESSEL_RADIUS);
//        cell3.Init(HEAD_CELL, VESSEL_RADIUS);

        agent3D HEPMAP = woundGrid.NewAgentPT(60, 30 ,55);
        HEPMAP.Init(HEPARIN_ISLAND, MAP_RAD);

        agent3D MAP = woundGrid.NewAgentPT(20, 12 ,18);
        MAP.Init(MAP_PARTICLE, MAP_RAD);
        agent3D MAP2 = woundGrid.NewAgentPT(33, 19 ,33);
        MAP2.Init(MAP_PARTICLE, MAP_RAD);

        // END TESTING


        // TICK ACTIONS
        for (int step = 0; step < TIMESTEPS; step++) {
            woundGrid.StepVEGF();
            woundGrid.StepCells(0.5);
            woundGrid.DrawGrid(window);
            woundGrid.DrawGradientWindowed(VEGF_xz, Util::HeatMapBGR);
            woundGrid.DrawAgents(window);

            if(window.IsClosed()){
                window.Close();
                VEGF_xz.Close();
                break;
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

    public grid3D (int x, int y, int z) {
        super(x, y, z, agent3D.class);
        VEGF = new PDEGrid3D(x, y, z);
    }

    ////////////////////
    // INITIALIZATION //
    ////////////////////

    public static void Init_MAP_Particles(grid3D grid){
        agent3D MAP_seed = grid.NewAgentPTSafe(x * Math.random(), y * Math.random(), 0);
        MAP_seed.Init(MAP_PARTICLE, MAP_RAD);
        MAP_seed.Recursive_MAP_Generator();
    }

    //////////////////
    // GRID ACTIONS //
    //////////////////

    public void StepCells(double divProb) {
        for (agent3D cell: this) {
            cell.StepCell(divProb);
        }
        IncTick();
    }

    public void StepVEGF(){

        // TESTING SET BASE GRADIENT
//        for (int i = 1; i < x; i++){
//            for (int j = 1; j < y; j++){
//                for (int k = 1; k < z; k++){
//                    VEGF.Set(i, j, k, (i+j+k)/9000.0);
//                }
//            }
//        }

        VEGF.Set (x-1, y-1, z-1, 0.5);

        // END TESTING


        VEGF.Diffusion(0.1);
        VEGF.Update();
    }

    //////////////////
    // GRID DRAWING //
    //////////////////

    public void DrawGrid (OpenGL3DWindow window){
        window.ClearBox(Util.RGB(225/245.0,198/245.0,153/245.0), Util.BLUE);
    }

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
        window.Update();
    }

    public void DrawGradientWindowed (GridWindow window, DoubleToInt DrawConcs){
        for (int x = 0; x < VEGF.xDim; x++) {
            for (int z = 0; z < VEGF.zDim; z++) {
                double VEGF_Sum=0;
                //add column to avgConcs
                for (int y = 0; y < VEGF.yDim; y++) {
                    VEGF_Sum+=VEGF.Get(x,y,z);
                }
//                VEGF_Sum/=VEGF.yDim;
                window.SetPix(x,z,DrawConcs.DoubleToInt(VEGF_Sum));
            }
        }
    }

    public void DrawGradientEmbedded (OpenGL3DWindow window, DoubleToInt DrawConcs){
        window.ClearBox(Util.RGB(225/245.0,198/245.0,153/245.0), Util.BLUE);
        for (int x = 0; x < VEGF.xDim; x++) {
            for (int z = 0; z < VEGF.zDim; z++) {
                double VEGF_Sum=0;
                //add column to avgConcs
                for (int y = 0; y < VEGF.yDim; y++) {
                    VEGF_Sum+=VEGF.Get(x,y,z);
                }
                VEGF_Sum/=VEGF.yDim;
                window.SetPixXZ(x,z,DrawConcs.DoubleToInt(VEGF_Sum));
            }
        }
    }

    /////////////////
    // DATA EXPORT //
    /////////////////

}