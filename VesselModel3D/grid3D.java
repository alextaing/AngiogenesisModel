package VesselModel3D;

import HAL.GridsAndAgents.AgentGrid3D;
import HAL.GridsAndAgents.AgentPT3D;
import HAL.GridsAndAgents.PDEGrid3D;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;

public class grid3D extends AgentGrid3D<agent> {
    ////////////////
    // PARAMETERS //
    ////////////////

    // AGENT PARAMETERS
    public static final double divProb = 0.3;

    // GRID PROPERTIES
    public static final int x = 200;
    public static final int y = 200;
    public static final int z = 200;
    public static final int SCALE_FACTOR = 2;
    public final static int TICK_PAUSE = 1;
    public final static int TIMESTEPS = 2000; // how long will the simulation run?
    Rand rng = new Rand();

    /////////////////
    // MAIN METHOD //
    /////////////////
    public static void main(String[] args) {
        GridWindow window = new GridWindow("Angiogenesis", x, y, SCALE_FACTOR);
        grid3D woundGrid = new grid3D(x, y, z);

        woundGrid.DrawGrid(window);

        woundGrid.NewAgentPT(x/2.0, y/2.0, z/2.0);

        for (int step = 0; step < TIMESTEPS; step++) {
            woundGrid.StepCells(divProb);
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
        super(100, 100, 100, agent.class);
        VEGF = new PDEGrid3D(x, y, z);
    }

    ////////////////////
    // INITIALIZATION //
    ////////////////////


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

    public void DrawGrid (GridWindow window){
        for (int i=0; i < length; i++) {
            int color = Util.BLACK;
            if (GetAgent(i) != null){
                agent cell = GetAgent(i);
                color = cell.color;
            }
        }
    }

    /////////////////
    // DATA EXPORT //
    /////////////////

}