/*
GRIFFIN LAB
ALEX TAING, UNDERGRADUATE
FALL 2020
*/


package VascularModel;

import HAL.GridsAndAgents.AgentSQ2D;
import HAL.Util;

import java.util.ArrayList;

public class agent_2D extends AgentSQ2D<woundGrid_2D> {


    // DO NOT MODIFY FOR PARAMETERS
    public static int HEAD_CELL = 0;
    public static int BODY_CELL = 1;
    public static int MAP_PARTICLE = 2;
    public static int HEPARIN_MAP = 3;
    public static int MACROPHAGE = 4;

    public static int HEAD_CELL_COLOR = Util.RED;
    public static int BODY_CELL_COLOR = Util.RED;
    public static int MAP_PARTICLE_COLOR = Util.RGB(23.0 / 255, 28.0 / 255, 173.0 / 255); // normal MAP;
    public static int HEPARIN_MAP_COLOR = Util.RGB(48.0 / 255, 191.0 / 255, 217.0 / 255); // Heparin MAP;
    public static int MACROPHAGE_COLOR = Util.WHITE;

    public static double VASCULAR_VEGF_INTAKE = woundGrid_2D.VASCULAR_VEGF_INTAKE;
    public static double VEGF_DIV_PROB = woundGrid_2D.VEGF_DIV_PROB;
    public static double BODY_CELL_BRANCH_PROB = woundGrid_2D.BODY_CELL_BRANCH_PROB;
    public final static double MACROPHAGE_SPAWN_CHANCE = woundGrid_2D.MACROPHAGE_SPAWN_CHANCE;
    public final static int MAX_MACROPHAGE_PER_SPAWN = woundGrid_2D.MAX_MACROPHAGE_PER_SPAWN;
    public final static double MACROPHAGE_FORWARD_TENDENCY = woundGrid_2D.MACROPHAGE_FORWARD_TENDENCY;
    public final static int ENDO_CELL_TICK_DELAY = woundGrid_2D.ENDO_CELL_TICK_DELAY;
    public final static double VEGF_SENSITIVITY = woundGrid_2D.VEGF_SENSITIVITY;

    int color;
    int type;
    int length = 0;
    boolean macrophageBottom;
    boolean macrophageOff;
    boolean vesselBottom;
    boolean arrived = false; // true if the vessel has reached the wound edge
    public static boolean start_endo = false; // when the endo cells begin to grow after macrophage start

    /**
     * Gets the location with the highest VEGF concentration within the cell's radius of sight
     *
     * @return the location of the VEGF
     */
    public int HighestConcentrationVEGF() {
        assert G != null;
        int VEGF_options = G.MapEmptyHood(G.VEGFHood, Isq()); // gets the cell's range of detecting VEGF

        double maxConcentration = -1;
        ArrayList<Integer> maxConcentrationLocations = new ArrayList<>();
        for (int i = 0; i < VEGF_options; i++) { // if there's nearby VEGF...
            double test_concentration = G.VEGF.Get(G.VEGFHood[i]);
            if ((test_concentration > maxConcentration) && (test_concentration > VEGF_SENSITIVITY)) {
                maxConcentration = test_concentration;
                maxConcentrationLocations.clear();
                maxConcentrationLocations.add(G.VEGFHood[i]);
            } else if (test_concentration == maxConcentration) {
                maxConcentrationLocations.add(G.VEGFHood[i]);
            }
        }
        if (maxConcentrationLocations.size() < 1) {
            return 0;
        } else if (maxConcentration <= 0) {
            return 0;
        }

        return maxConcentrationLocations.get((int) (Math.random() * maxConcentrationLocations.size()));
    }

    /**
     * Given an int location of a target, will find best location for next cell duplication
     *
     * @param VEGF_location location of nearest VEGF
     * @return int location to grow closer to target
     */
    public int HoodClosestToVEGF(int VEGF_location) {

        int minDistance = Integer.MAX_VALUE; // gets updated with each location check
        ArrayList<Integer> mincoordint = new ArrayList<>(); // closest points that are in the cell neighborhood (as an int)

        assert G != null;
        int options = G.MapHood(G.divHood, Isq()); // open areas around cell

        for (int i = 0; i < options; i++) {
            int MAPcount = 0;
            for (agent_2D cell : G.IterAgents(G.divHood[i])) {
                if ((cell.type == MAP_PARTICLE) || (cell.type == HEPARIN_MAP)) {
                    MAPcount++;
                }
            }
            if (MAPcount == 0) {
                int[] hoodPoint = {G.ItoX(G.divHood[i]), G.ItoY(G.divHood[i])};

                // gets the distance from neighborhood area to target
                int dist = Math.abs((int) Math.hypot(G.ItoX(VEGF_location) - hoodPoint[0], G.ItoY(VEGF_location) - hoodPoint[1]));

                // keeps a list of the hood points closest to the VEGF
                if (dist < minDistance) { // if the new hood point distance is closer than the one before, then...
                    minDistance = dist; // the minimum distance is updated to the new closest distance
                    mincoordint.clear();// the old list is cleared
                    mincoordint.add(G.I(hoodPoint[0], hoodPoint[1])); // and the new closest point is added to the empty list
                } else if (dist == minDistance) { // But, if the point is just as close as the ones on the list
                    mincoordint.add(G.I(hoodPoint[0], hoodPoint[1])); // it is added to the list of the hood points that are just as close
                }
            }
        }
        if (mincoordint.size() == 0) { // if there are no sufficiently strong VEGF nearby, then...
            return 0; // return 0
        }
        return mincoordint.get((int) (Math.random() * mincoordint.size())); // otherwise, return a random hood point on the list
    }

//    /**
//     * Initializes macrophages
//     */
//    public void initMacrophages() {
//        assert G != null;
//        if (G.rng.Double() < MACROPHAGE_SPAWN_CHANCE) {
//            for (int i = 1; i < MAX_MACROPHAGE_PER_SPAWN * (G.rng.Double()); i++) {
//                G.NewAgentPT((woundGrid_2D.x) * Math.random(), 1).InitMacrophage(MACROPHAGE, false, 0, true); // make a new macrophage there
//            }
//        }
//        if (G.rng.Double() < MACROPHAGE_SPAWN_CHANCE) {
//            for (int i = 1; i < MAX_MACROPHAGE_PER_SPAWN * (G.rng.Double()); i++) {
//                G.NewAgentPT((woundGrid_2D.x - 1) * Math.random(), woundGrid_2D.y - 2).InitMacrophage(MACROPHAGE, false, 0, false); // make a new macrophage there
//            }
//        }
//    }

    /**
     * Initializes macrophages
     */
    public void startMacrophageInvasion() {
        assert G != null;
        if (G.rng.Double() < MACROPHAGE_SPAWN_CHANCE) {
            for (int i = 1; i < MAX_MACROPHAGE_PER_SPAWN * (G.rng.Double()); i++) {
                G.NewAgentPT((woundGrid_2D.x) * Math.random(), 1).InitMacrophage(MACROPHAGE, false, 0, true); // make a new macrophage there
            }
        }
        if (G.rng.Double() < MACROPHAGE_SPAWN_CHANCE) {
            for (int i = 1; i < MAX_MACROPHAGE_PER_SPAWN * (G.rng.Double()); i++) {
                G.NewAgentPT((woundGrid_2D.x - 1) * Math.random(), woundGrid_2D.y - 2).InitMacrophage(MACROPHAGE, false, 0, false); // make a new macrophage there
            }
        }
    }

    /**
     * Initializes a cell with color and type
     *
     * @param type:   type of cell/particle
     * @param arrived whether the cell has arrived at the target or not (to be inherited from parent cell)
     */
    public void Init(int type, boolean arrived, int length) {

        this.arrived = arrived;
        this.type = type;
        this.length = length;

        if (type == HEAD_CELL) {
            this.color = HEAD_CELL_COLOR; // Growing endothelial cells
        } else if (type == BODY_CELL) {
            this.color = BODY_CELL_COLOR;
        } else if (type == MAP_PARTICLE) {
            this.color = MAP_PARTICLE_COLOR; // normal MAP
        } else if (type == HEPARIN_MAP) { // Inactive Endothelial cells
            this.color = HEPARIN_MAP_COLOR; // Heparin MAP
        } else if (type == MACROPHAGE) {
            this.color = MACROPHAGE_COLOR;
        }
    }

    /**
     * Initializes a cell with color and type
     *
     * @param type:   type of cell/particle
     * @param arrived whether the cell has arrived at the target or not (to be inherited from parent cell)
     */
    public void InitVascular(int type, boolean arrived, int length, boolean vesselBottom) {

        this.arrived = arrived;
        this.type = type;
        this.length = length;
        this.vesselBottom = vesselBottom;

        if (type == HEAD_CELL) {
            this.color = HEAD_CELL_COLOR; // Growing endothelial cells
        } else if (type == BODY_CELL) {
            this.color = BODY_CELL_COLOR;
        } else if (type == MAP_PARTICLE) {
            this.color = MAP_PARTICLE_COLOR; // normal MAP
        } else if (type == HEPARIN_MAP) { // Inactive Endothelial cells
            this.color = HEPARIN_MAP_COLOR; // Heparin MAP
        } else if (type == MACROPHAGE) {
            this.color = MACROPHAGE_COLOR;
        }
    }

    /**
     * Initializes a cell with color type, and macrophage direction
     *
     * @param type:   type of cell/particle
     * @param arrived whether the cell has arrived at the target or not (to be inherited from parent cell)
     */
    public void InitMacrophage(int type, boolean arrived, int length, boolean macrophageBottom) {

        this.arrived = arrived;
        this.type = type;
        this.length = length;
        this.macrophageBottom = macrophageBottom;
        this.macrophageOff = false;

        if (type == HEAD_CELL) {
            this.color = HEAD_CELL_COLOR; // Growing endothelial cells
        } else if (type == BODY_CELL) {
            this.color = BODY_CELL_COLOR;
        } else if (type == MAP_PARTICLE) {
            this.color = MAP_PARTICLE_COLOR; // normal MAP
        } else if (type == HEPARIN_MAP) { // Inactive Endothelial cells
            this.color = HEPARIN_MAP_COLOR; // Heparin MAP
        } else if (type == MACROPHAGE) {
            this.color = MACROPHAGE_COLOR;
        }
    }

    /**
     * Divides a cell to a random nearby location, allowing overlap with vessels, but not MAP
     */
    public void randomDivideOverlap() {
        assert G != null;
        int options = G.MapHood(G.divHood, Isq());
        ArrayList<Integer> openAreas = new ArrayList<>();
        for (int i = 0; i < options; i++) {
            int MAPcount = 0;
            for (agent_2D cell : G.IterAgents(G.divHood[i])) {
                if ((cell.type == HEPARIN_MAP) || (cell.type == MAP_PARTICLE)) {
                    MAPcount++;
                }
            }
            if (MAPcount == 0) {
                openAreas.add(G.divHood[i]);
            }
        }
        int location = openAreas.get((int) (Math.random() * openAreas.size()));
        G.NewAgentSQ(location).InitVascular(HEAD_CELL, this.arrived, this.length + 1, this.vesselBottom);
        InitVascular(BODY_CELL, this.arrived, this.length, this.vesselBottom);
    }

    /**
     * Divides a cell to a random nearby location, NOT allowing overlap with vessels or MAP
     */
    public void randomDivideNotOverlap() {
        assert G != null;
        int options = MapEmptyHood(G.divHood);
        if (options >= 1) {
            G.NewAgentSQ(G.divHood[G.rng.Int(options)]).InitVascular(HEAD_CELL, this.arrived, this.length + 1, this.vesselBottom);
            InitVascular(BODY_CELL, this.arrived, this.length, this.vesselBottom);
        }
    }

    /**
     * Checks if the current cell has arrive at the other side of the wound
     */
    public void checkIfArrived() {
        if (!arrived) {
            if (type == HEAD_CELL) {
                if (Ysq() == woundGrid_2D.y / 2) {
                    this.arrived = true;
                    assert G != null;
                    double time = (double) G.GetTick() / 6;
                    int arrivedLength = this.length * 16;
                    woundGrid_2D.arrivedTime.add(time);
                    woundGrid_2D.arrivedLengths.add(arrivedLength);
                    System.out.println(woundGrid_2D.arrivedTime.size() + ") " + time + " hours, " + arrivedLength + " microns");
                }
            }
        }
    }

    /**
     * Calls cell to consume appropriate amount of VEGF
     */
    public void ConsumeVEGF() {
        assert G != null;
        if ((G.VEGF.Get(Isq()) != 0) && ((type == HEAD_CELL) || (type == BODY_CELL))) {
            G.VEGF.Add(Isq(), -VASCULAR_VEGF_INTAKE);
        }
    }

    /**
     * Determines whether the endothelial cells should begin dividing, according to ENDO_CELL_TICK_DELAY
     */
    public void CheckEndothelialStart() {
        assert G != null;
        if (G.GetTick() == ENDO_CELL_TICK_DELAY) {
            start_endo = true;
        }
    }

    /**
     * Initializes VEGF concentrations according to macrophage presence
     */
    public void InitializeVEGF() {
        if (type == HEPARIN_MAP) {
            assert G != null;
            int occupied = MapOccupiedHood(G.Macrophage_sense_hood);
            for (int i = 0; i < occupied; i++) {
                Iterable<agent_2D> agents = G.IterAgents(G.Macrophage_sense_hood[i]);
                for (agent_2D agent : agents) {
                    if (macrophageOff) {
                        return;
                    }
                    if ((agent.type == HEAD_CELL) || (agent.type == BODY_CELL) || (agent.macrophageOff) ) {
                        macrophageOff = true;
                        return;
                    }
                    if (agent.type == MACROPHAGE) {
                        G.VEGF.Set(Isq(), 1);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Performs body cell actions (e.g. splitting)
     */
    public void BodyCellActions() {
        if (type == BODY_CELL) { // just in case all heads come to a dead end, new head cells can still form
            assert G != null;
            if (G.rng.Double() < BODY_CELL_BRANCH_PROB) {
                int options2 = MapEmptyHood(G.divHood);
                if (options2 > 0) {
                    G.NewAgentSQ(G.divHood[G.rng.Int(options2)]).InitVascular(HEAD_CELL, this.arrived, this.length + 1, this.vesselBottom);
                }
            }
        }
    }

    /**
     * Moves Macrophages randomly with a bias towards the opposite wound edge
     */
    public void MoveMacrophages() {
        if (type == MACROPHAGE) {
            assert G != null;

            if ((1 > Xpt()) || (Xpt() > woundGrid_2D.x - 1)) {
                Dispose();
                return;
            }
            if ((1 > Ypt()) || (Ypt() > woundGrid_2D.y - 1)) {
                Dispose();
                return;
            }

            if (type == MACROPHAGE) {
                if (G.rng.Double() < MACROPHAGE_FORWARD_TENDENCY) {
                    if (macrophageBottom) {
                        MoveSQ(G.I((Xpt()), (Ypt() + 1)));
                        return;
                    } else {
                        MoveSQ(G.I((Xpt()), (Ypt() - 1)));
                    }
                }
                int options = MapHood(G.moveHood);
                MoveSQ(G.moveHood[(int) (options * Math.random())]);
            }
        }
    }

    /**
     * Modifies the division probability of a cell depending on the presence of VEGF
     *
     * @param divProb original division probability
     * @return modified divProb
     */
    public double ModifyDivProbNearVEGF(double divProb) {
        if (HighestConcentrationVEGF() > VEGF_SENSITIVITY) {
            return VEGF_DIV_PROB;
        }
        return divProb;
    }

    /**
     * Divides endothelial cells
     *
     * @param divProb   division probability
     * @param splitProb probability that the vessel will split
     */
    public void EndothelialGrowth(double divProb, double splitProb) {
        if (type == HEAD_CELL && start_endo) {

            // Check anastomosis
//            int vesselsAtLocation = 0;
//            assert G != null;
//            for (EndothelialCell cell : G.IterAgents(Isq())) {
//                if (cell.type == HEAD_CELL || cell.type == BODY_CELL){
//                    vesselsAtLocation++;
//                }
//            }
//            if (vesselsAtLocation >= 2){
//                HeparinGrid.anastomoses++;
//            }

            assert G != null;
            if (G.rng.Double() < divProb) { // if cell chances to divide
                int TargetLocation = HighestConcentrationVEGF();
                if (TargetLocation != 0) {
                    int cellDivLocation = HoodClosestToVEGF(TargetLocation); // take the int position and find the closest neighborhood division spot
                    if (G.PopAt(cellDivLocation) < 5) { // if the area is not too crowded
                        G.NewAgentSQ(cellDivLocation).InitVascular(HEAD_CELL, this.arrived, this.length + 1, this.vesselBottom); // make a new cell there
                        InitVascular(BODY_CELL, this.arrived, this.length, this.vesselBottom);
                    }
                } else { // supposed to be random movement if there is no VEGF nearby
                    randomDivideNotOverlap();
                }

                if (G.rng.Double() < splitProb) { // maybe branch off
                    randomDivideNotOverlap();
                }
            }
        }
    }

    /**
     * Steps an agent, can be used on all implemented agents
     *
     * @param divProb:   chance of division for endothelial cells
     * @param splitProb: chance of branching for endothelial cells
     */
    public void StepCell(double divProb, double splitProb) {
        divProb = ModifyDivProbNearVEGF(divProb);

        // Check if arrived
        checkIfArrived();

        // Eat VEGF
        ConsumeVEGF();

        // Make Macrophages
        startMacrophageInvasion();

        // check if endothelial cells will divide yet
        CheckEndothelialStart();

        // initialize VEGF
        InitializeVEGF();
//        if (this.type == HEPARIN_MAP){
//            G.VEGF.Set(Isq(), 1);
//        }

        // Normal MAP Cells: nothing
        BodyCellActions();

        // Move Macrophages
        MoveMacrophages();

        // dividing endothelial cells
        EndothelialGrowth(divProb, splitProb);
    }
}
