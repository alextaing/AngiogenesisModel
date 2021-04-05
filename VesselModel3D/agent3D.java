package VesselModel3D;

import HAL.GridsAndAgents.SphericalAgent3D;
import HAL.Util;

/**
 * NOTES:  IN ORDER FOR PROGRAM TO WORK, MUST MODIFY SphericalAgent3D class.
 * See Required_additions for specifics.
 */

public class agent3D extends SphericalAgent3D<agent3D, grid3D> {
    ////////////////
    // PROPERTIES //
    ////////////////

    int color;
    int length = 0;
    int origin;
    boolean arrived;
    double[] pastLocation;
    boolean heparinOn = true;
    public static boolean start_endo = false; // determines start time of endo growth after macrophages


    ////////////////////
    // PROPERTY TYPES //
    ////////////////////

    // CELL TYPES
    public static final int HEAD_CELL = 1;
    public static final int BODY_CELL = 2;
    public static final int MAP_PARTICLE = 3;
    public static final int HEPARIN_ISLAND = 4;
    public static final int MACROPHAGE = 5;

    // COLORS
    public static int HEAD_CELL_COLOR = Util.RED;
    public static int BODY_CELL_COLOR = Util.RED;
    public static int MAP_PARTICLE_COLOR = Util.RGB(23.0/255, 28.0/255, 173.0/255); // normal MAP;
    public static int HEPARIN_MAP_COLOR = Util.RGB(48.0/255, 191.0/255, 217.0/255); // Heparin MAP;
    public static int MACROPHAGE_COLOR = Util.WHITE;

    ///////////////////////
    // UTILITY VARIABLES //
    ///////////////////////

    int[] posneg = {1, -1};
    public static double MAP_RAD = grid3D.MAP_RAD;
    public static final double HEPARIN_ISLAND_PERCENTAGE = grid3D.HEPARIN_ISLAND_PERCENTAGE;
    public static final double VESSEL_RADIUS = grid3D.VESSEL_RADIUS;

    ////////////////////
    // INITIALIZATION //
    ////////////////////

    public void Init(int type, double radius){

        this.radius = radius;
        this.type = type;

        if (type == HEAD_CELL) {
            this.color = HEAD_CELL_COLOR; // Growing endothelial cells
        } else if (type == BODY_CELL){
            this.color = BODY_CELL_COLOR;
        } else if (type == MAP_PARTICLE){
            this.color = MAP_PARTICLE_COLOR; // normal MAP
        } else if (type == HEPARIN_ISLAND) { // Inactive Endothelial cells
            this.color = HEPARIN_MAP_COLOR; // Heparin MAP
        } else if (type == MACROPHAGE) {
            this.color = MACROPHAGE_COLOR;
        }
    }

    public void Recursive_MAP_Generator(){
        assert G != null;
        // recursive function, makes 12 surrounding MAP particles if they are in bounds and unoccupied

        double x_distance = grid3D.MAP_GAP_CENTERS * Math.cos(Math.toRadians(60));
        double y_distance = grid3D.MAP_GAP_CENTERS * Math.sin(Math.toRadians(60));

        // Make 6 surrounding MAP_seed on same plane
        for (int i : posneg) {
            for (int j : posneg) {
                // calculate new coordinate
                double new_agent_x = Xpt() + (i * x_distance);
                double new_agent_y = Ypt() + (j * y_distance);

                // make sure that the new coordinate is inside bounds
                boolean x_in_range = (0 < new_agent_x) && (new_agent_x < grid3D.x);
                boolean y_in_range = (0 < new_agent_y) && (new_agent_y < grid3D.y);

                // make sure that there is nothing there
                boolean open_for_MAP = false;
                if (x_in_range && y_in_range) {
                    int occlusions = G.PopAt((int) new_agent_x, (int) new_agent_y, (int) Zpt());
                    if (occlusions == 0) {
                        open_for_MAP = true;
                    }
                }

                if (x_in_range && y_in_range && open_for_MAP) {
                    agent3D new_agent;
                    if(G.rng.Double() < HEPARIN_ISLAND_PERCENTAGE){
                        new_agent = G.NewAgentPT(new_agent_x, new_agent_y, Zpt());
                        new_agent.Init(HEPARIN_ISLAND, MAP_RAD);
                    } else {
                        new_agent = G.NewAgentPT(new_agent_x, new_agent_y, Zpt());
                        new_agent.Init(MAP_PARTICLE, MAP_RAD);
                    }
                    new_agent.Recursive_MAP_Generator();
                }
            }
        }

        // 6 surrounding on either side of xy place.  The 3 "above" and 3 "below" in a triangular formation
        // NOTE: np = next plane
        // NOTE: sc = side cut
        // see notes for details on calculations

        double Zsc_distance = grid3D.MAP_GAP_CENTERS * Math.cos(Math.toRadians(30)); // distance above/below xy plane
        double Ysc_distance = grid3D.MAP_GAP_CENTERS * Math.sin(Math.toRadians(30)); // distance above x coordinate

        // calculate position of one that is straight forward (.'.) i.e. quotation mark

        // calculate new coordinate
        double new_agent_y = Ypt() + (Ysc_distance);
        double new_agent_z = Zpt() + (Zsc_distance);

        // make sure that the new coordinate is inside bounds
        boolean y_in_range = (0 < new_agent_y) && (new_agent_y < grid3D.y);
        boolean z_in_range = (0 < new_agent_z) && (Zpt() < grid3D.z);


        // make sure that there is nothing there
        boolean open_for_MAP = false;
        if (y_in_range && z_in_range) {
            int occlusions = G.PopAt((int) Xpt(), (int) new_agent_y, (int) new_agent_z);
            if (occlusions == 0) {
                open_for_MAP = true;
            }
        }

        if (y_in_range && z_in_range && open_for_MAP){
            agent3D new_agent = G.NewAgentPT(Xpt(), new_agent_y, new_agent_z);
            new_agent.Init(MAP_PARTICLE, MAP_RAD);
            new_agent.Recursive_MAP_Generator();
        }
    }

    /////////////
    // METHODS //
    /////////////


    public void StepCell(double divProb) {
        assert G != null;

        // HEAD CELLS
        if(type == HEAD_CELL) {
            chemotaxis();
            // BRANCH SOMETIMES
            if (G.rng.Double() < 0.001) {
                agent3D cell = G.NewAgentPT(Xpt()-0.02, Ypt()-0.02, Zpt()-0.02);
                cell.Init(HEAD_CELL, radius);
                cell.pastLocation = new double[] {Xpt()-1.5, Ypt()-1.5, Zpt()-1.5};
            }

//            G.VEGF.Add(Isq(), -0.02);

        } else if (type == BODY_CELL){
//            G.VEGF.Add(Isq(), -0.00001);
        } else if (type == HEPARIN_ISLAND){
            stepHeparinIslands();
        }
    }

    public void chemotaxis() {
        // MAX VELOCITY

        // RATE OF GROWTH
        double CHEMOTAX_RATE = 1;
        double FORCE_SCALER = 1;

        // GRADIENTS
        double gradX=G.VEGF.GradientX(Xsq(),Ypt(), Zpt());
        double gradY=G.VEGF.GradientY(Xsq(),Ypt(), Zpt());
        double gradZ=G.VEGF.GradientZ(Xpt(), Ypt(), Zpt());

        // CALCULATE MOVEMENT
        double norm= Util.Norm(gradX,gradY,gradZ);
        if(gradX!=0) {
            xVel += gradX / norm * CHEMOTAX_RATE;
        }
        if(gradY!=0) {
            yVel += gradY / norm * CHEMOTAX_RATE;
        }
        if(gradZ!=0) {
            zVel += gradZ / norm * CHEMOTAX_RATE;
        }

        // SUM FORCES AND MOVE
        SumForcesMAP(radius+MAP_RAD,(overlap,other)-> overlap*FORCE_SCALER, new int[] {MAP_PARTICLE, HEPARIN_ISLAND});
        CapVelocity(0.3);
        ForceMove();

        // UPDATE PAST LOCATION
//        if(!(Dist(pastLocation[0], pastLocation[1], pastLocation[2]) < radius/2)) {
            G.NewAgentPT(Xpt()-.01, Ypt()-.01, Zpt()-.01).Init(BODY_CELL, radius);
            pastLocation[0] = Xpt();
            pastLocation[1] = Ypt();
            pastLocation[2] = Zpt();
//        }
    }

    public void stepHeparinIslands() {
        if (heparinOn){
            G.VEGF.Set(Isq(), 0.5);
            for (agent3D agent : G.IterAgentsRad(Xpt(), Ypt(), Zpt(), MAP_RAD + VESSEL_RADIUS)) {
                if (agent.type == HEAD_CELL || agent.type == BODY_CELL) {
                    heparinOn = false;
                    break;
                }
            }
        } else {
            G.VEGF.Set(Isq(), 0);
        }
    }
}


