package VesselModel3D;

import HAL.GridsAndAgents.AgentGrid3D;
import HAL.GridsAndAgents.AgentPT3D;
import HAL.GridsAndAgents.PDEGrid3D;
import HAL.Gui.GridWindow;
import HAL.Gui.OpenGL3DWindow;
import HAL.Rand;
import HAL.Util;

public class grid3D extends AgentGrid3D<agent> {

    ////////////////
    // PARAMETERS //
    ////////////////

    // MAP GEL
    public static final double HEPARIN_PERCENTAGE = 0.1; // enter as a decimal between 0 and 1
    public static final double MAP_RAD = 8;
    public static double MAP_GAP = 3;


    // DO NOT MODIFY
    public static final double MAP_GAP_CENTERS = MAP_GAP + (2*MAP_RAD);
    // VESSELS

    // AGENT PARAMETERS
    public static final double divProb = 0.3;

    // GRID PROPERTIES
    public static final int x = 500;
    public static final int y = 500;
    public static final int z = 500;
    public static final int SCALE_FACTOR = 2;
    public final static int TICK_PAUSE = 1;
    public final static int TIMESTEPS = 1000; // how long will the simulation run?
    Rand rng = new Rand();

    /////////////////
    // MAIN METHOD //
    /////////////////

    public static void main(String[] args) {
        OpenGL3DWindow window = new OpenGL3DWindow("Angiogenesis", 1000, 1000, x, y, z);
        grid3D woundGrid = new grid3D(x, y, z);

        woundGrid.NewAgentPT(x/2.0, y/2.0, z/2.0).Init(MAP_RAD);

        for (int step = 0; step < TIMESTEPS; step++) {
            woundGrid.StepCells(divProb);
            for (agent cell : woundGrid) {
                if (woundGrid.rng.Double() < 0.1) {
                    cell.Divide(20, woundGrid.rng);
                }
            }
            woundGrid.DrawAgents(window);
            woundGrid.DrawGrid(window);
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
        super(x, y, z, agent.class);
        VEGF = new PDEGrid3D(x, y, z);
    }

    ////////////////////
    // INITIALIZATION //
    ////////////////////

//    public void Init_MAP_Particles(){
//        for (int i = 0; i < x*y; i++) {
//            int cellType = agent.MAP_PARTICLE;
//            double chance = Math.random();
//            if (chance < HEPARIN_PERCENTAGE){
//                cellType = agent.HEPARIN_ISLAND;
//            }
//            NewAgentPT(50, 50, 50);
//
//            int occlusions = MapOccupiedHood(MAP_space, i);
//            int open = MapEmptyHood(MAP_rad, i);
//            if (occlusions == 0) {
//                for (int j = 0; j < open; j++){
//                    if (0 < MAP_rad[j] && MAP_rad[j] < x*y){
//                        NewAgentSQ(MAP_rad[j]).Init(cellType, false, 0);
//                    }
//                }
//            }
//        }
//    }

    //////////////////
    // GRID ACTIONS //
    //////////////////

    public void StepCells(double divProb) {
        for (agent cell: this) {
            cell.StepCell(divProb);
        }
        IncTick();

    }

    //////////////////
    // GRID DRAWING //
    //////////////////

    public void DrawGrid (OpenGL3DWindow window){
        for (int i=0; i < length; i++) {
//            int color = Util.BLACK;
//            if (GetAgent(i) != null){
//                agent cell = GetAgent(i);
//                color = cell.color;
//            }
        }
    }

    public void DrawAgents(OpenGL3DWindow window){
        window.ClearBox(Util.BLUE, Util.WHITE);
        for (agent cell : this) {
            window.CelSphere(cell.Xpt(),cell.Ypt(),cell.Zpt(),MAP_RAD,Util.WHITE);
        }
        window.Update();
    }

    /////////////////
    // DATA EXPORT //
    /////////////////

}