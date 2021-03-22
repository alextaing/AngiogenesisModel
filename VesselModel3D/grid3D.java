package VesselModel3D;

import HAL.GridsAndAgents.AgentGrid3D;
import HAL.GridsAndAgents.PDEGrid3D;
import HAL.Gui.OpenGL3DWindow;
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
    public static final double HEPARIN_PERCENTAGE = 0.1; // enter as a decimal between 0 and 1
    public static final double MAP_DIAMETER = 16;//80 * (SCALE_FACTOR);
    public static final double MAP_GAP =  1; //16 * (SCALE_FACTOR);

    // VESSELS

    // AGENT PARAMETERS
    public static final double divProb = 0.3;

    // GRID PROPERTIES
    public static final int x = 200 ;//* (SCALE_FACTOR);
    public static final int y = 100 ;//* (SCALE_FACTOR);
    public static final int z = 500 ;//* (SCALE_FACTOR);
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


    /////////////////
    // MAIN METHOD //
    /////////////////

    public static void main(String[] args) {
        OpenGL3DWindow window = new OpenGL3DWindow("Angiogenesis", 900, 900, x, y, z);
        grid3D woundGrid = new grid3D(x, y, z);

        Init_MAP_Particles(woundGrid);

        agent3D cell = woundGrid.NewAgentPT(x * Math.random(), y * Math.random(), z * Math.random());
        cell.Init(HEAD_CELL, 0.5);

        for (int step = 0; step < TIMESTEPS; step++) {
            if (step % 20 == 0){
                cell.Divide(1, woundGrid.rng).Init(HEAD_CELL, 0.5);
            }
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
            }
            if ((cell.type == HEPARIN_ISLAND) && (!VIEW_HEP_ISLANDS)){
                continue;
            }
            if ((cell.type == MACROPHAGE) && (!VIEW_MACROPHAGES)){
                continue;
            }
            if (((cell.type == HEAD_CELL) || (cell.type == BODY_CELL)) && (!VIEW_VESSELS)){
                continue;
            }
            window.CelSphere(cell.Xpt(),cell.Ypt(),cell.Zpt(),cell.radius, cell.color);
        }
        window.Update();
    }

    /////////////////
    // DATA EXPORT //
    /////////////////

}