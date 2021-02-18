package VesselModel3D;

import HAL.GridsAndAgents.AgentPT3D;
import HAL.Util;

public class agent extends AgentPT3D<grid3D> {
    ////////////////
    // PROPERTIES //
    ////////////////

    int color;
    int type;
    int length = 0;
    int origin;
    boolean arrived;
    public static boolean start_endo = false; // determines start time of endo growth after macrophages

    ////////////////////
    // PROPERTY TYPES //
    ////////////////////

    // CELL TYPES

    // COLORS
    public static int HEAD_CELL_COLOR = Util.RED;
    public static int BODY_CELL_COLOR = Util.RED;
    public static int MAP_PARTICLE_COLOR = Util.RGB(23.0/255, 28.0/255, 173.0/255); // normal MAP;
    public static int HEPARIN_MAP_COLOR = Util.RGB(48.0/255, 191.0/255, 217.0/255); // Heparin MAP;
    public static int MACROPHAGE_COLOR = Util.WHITE;


    ////////////////////
    // INITIALIZATION //
    ////////////////////



    /////////////
    // METHODS //
    /////////////

    public void StepCell(double divProb) {
        assert G != null;
        if (G.rng.Double() < divProb) {

        }
    }
}
