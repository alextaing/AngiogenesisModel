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
    public static double MAP_GAP = 5;


    // DO NOT MODIFY
    public static final double MAP_GAP_CENTERS = MAP_GAP + (2*MAP_RAD);
    // VESSELS

    // AGENT PARAMETERS
    public static final double divProb = 0.3;

    // GRID PROPERTIES
    public static final int x = 200;
    public static final int y = 200;
    public static final int z = 200;
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

        agent MAP_seed = woundGrid.NewAgentPTSafe(x * Math.random(), y * Math.random(), 0);
        woundGrid.Init_MAP_Particles(MAP_seed);


        for (int step = 0; step < TIMESTEPS; step++) {
//            woundGrid.StepCells(divProb);
//            for (agent cell : woundGrid) {
//                if (woundGrid.rng.Double() < 0.1) {
//                    cell.Divide(20, woundGrid.rng);
//                }
//            }
            woundGrid.DrawGrid(window);
            woundGrid.DrawAgents(window);
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

    public void Init_MAP_Particles(agent MAP_seed){
        MAP_seed.Recursive_MAP_Generator();
    }

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
        window.ClearBox(Util.BLUE, Util.WHITE);
//        for (int i=0; i < length; i++) {
//            int color = Util.BLACK;
//            if (GetAgent(i) != null){
//                agent cell = GetAgent(i);
//                color = cell.color;
//            }
//        }
    }

    public void DrawAgents(OpenGL3DWindow window){
        for (agent cell : this) {
            window.CelSphere(cell.Xpt(),cell.Ypt(),cell.Zpt(),10.0, cell.color);
        }
        window.Update();
    }

    /////////////////
    // DATA EXPORT //
    /////////////////

}