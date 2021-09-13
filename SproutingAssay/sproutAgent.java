/*
GRIFFIN LAB
ALEX TAING, UNDERGRADUATE
FALL 2020
*/

package SproutingAssay;

import HAL.GridsAndAgents.AgentSQ2D;
import HAL.Util;

import java.util.ArrayList;

public class sproutAgent extends AgentSQ2D<sproutGrid> {


    // DO NOT MODIFY FOR PARAMETERS
    public static int HEAD_CELL = 0;
    public static int BODY_CELL = 1;
    public static int MAP_PARTICLE = 2;
    public static int HEPARIN_MAP = 3;

    public static int HEAD_CELL_COLOR = Util.RED;
    public static int BODY_CELL_COLOR = Util.RED;
    public static int MAP_PARTICLE_COLOR = Util.RGB(23.0 / 255, 28.0 / 255, 173.0 / 255); // normal MAP;
    public static int HEPARIN_MAP_COLOR = Util.RGB(48.0 / 255, 191.0 / 255, 217.0 / 255); // Heparin MAP;

    public static double VEGF_INTAKE = sproutGrid.VASCULAR_VEGF_INTAKE;
    public final static int VESSEL_GROWTH_DELAY = sproutGrid.VESSEL_GROWTH_DELAY;
    public final static double VEGF_SENSITIVITY = sproutGrid.VEGF_SENSITIVITY;

    public final static double LOW_BRANCHING_PROBABILITY= sproutGrid.LOW_BRANCHING_PROBABILITY;
    public final static double LOW_MED_VEGF_THRESHOLD = sproutGrid.LOW_MED_VEGF_THRESHOLD;
    public final static double MED_BRANCHING_PROBABILITY= sproutGrid.MED_BRANCHING_PROBABILITY;
    public final static double MED_HIGH_VEGF_THRESHOLD = sproutGrid.MED_HIGH_VEGF_THRESHOLD;
    public final static double HIGH_BRANCHING_PROBABILITY= sproutGrid.HIGH_BRANCHING_PROBABILITY;

    int color;
    int type;
    int length = 0;
    int target;
    double migration_rate = sproutGrid.MIGRATION_RATE;
    double branching_probability;
    int since_last_elongation;
    int elongationLength;
    int MAX_ELONGATION_LENGTH = sproutGrid.MAX_ELONGATION_LENGTH;
    public static boolean start_vessel_growth = false; // is set to true when vessels begin to invade

    /**
     * Gets the location with the highest VEGF concentration within the cell's radius of sight
     *
     * @return returns the location of highest concentration of VEGF (if there are multiple, then it will return a random one)
     */
    public int HighestConcentrationVEGF() {
        assert G != null;
        int VEGF_options = G.MapEmptyHood(G.VEGFHood, Isq()); // gets the cell's range of detecting VEGF

        double maxConcentration = -1; // holds the max concentration so far (initially -1)
        ArrayList<Integer> maxConcentrationLocations = new ArrayList<>(); // holds the coordinates for the locations of highest concentration
        for (int i = 0; i < VEGF_options; i++) { // Iterates through all nearby coordinates
            double test_concentration = G.VEGF.Get(G.VEGFHood[i]); // gets the concentration at a point (test concentration)
            if ((test_concentration > maxConcentration) && (test_concentration > VEGF_SENSITIVITY)) { // if the concentration here is larger than the max so far (maxConcentration)
                maxConcentration = test_concentration; // then set that concentration as the new max
                maxConcentrationLocations.clear(); // clear the old locations of highest concentration
                maxConcentrationLocations.add(G.VEGFHood[i]); // add this location to the list of highest concentrations
            } else if (test_concentration == maxConcentration) { // if the test concentration is equal to the current max concentration
                maxConcentrationLocations.add(G.VEGFHood[i]); // just add the coordinate to the running list of locations of highest concentration
            }
        }
        if (maxConcentrationLocations.size() < 1) { // if there were no locations of highest concentration at all
            return 0;
        } else if (maxConcentration <= 0) { // if max concentration was 0
            return 0;
        }

        return maxConcentrationLocations.get((int) (Math.random() * maxConcentrationLocations.size())); // return a random one of the locations of highest concentration
    }

    /**
     * Given an int location of a target, this function will find best location for next cell duplication to move towards this location
     *
     * @param target location of nearest VEGF
     * @return int location to grow closer to target
     */
    public int HoodClosestToTarget(int target) {

        int minDistance = Integer.MAX_VALUE; // gets updated with each location check (holds the minimum distance of a coordinate to the target)
        ArrayList<Integer> mincoordint = new ArrayList<>(); // closest points that are in the cell neighborhood (as an int)

        assert G != null;
        int options = G.MapHood(G.divHood, Isq()); // open areas around cell

        for (int i = 0; i < options; i++) { // iterate thorough the open areas
            int MAPcount = 0; // tally of MAP present
            for (sproutAgent cell : G.IterAgents(G.divHood[i])) { // iterate through all the cells at that coordinate
                if ((cell.type == MAP_PARTICLE) || (cell.type == HEPARIN_MAP)) { // if there is MAP GEL there
                    MAPcount++; // then keep track that there was a particle there.
                }
            }
            if (MAPcount == 0) { // If there were no occlusions with MAP particles, then check to see if it is close to the target point
                int[] hoodPoint = {G.ItoX(G.divHood[i]), G.ItoY(G.divHood[i])}; // This is the location of the point in question (neighborhood point)

                // gets the distance from neighborhood point to target
                int dist = Math.abs((int) Math.hypot(G.ItoX(target) - hoodPoint[0], G.ItoY(target) - hoodPoint[1]));

                // keeps a list of the neighborhood points closest to the VEGF
                if (dist < minDistance) { // if the neighborhood point distance is closer than the one before, then...
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

    /**
     * Initializes a cell with color and type
     *
     * @param type type of cell/particle
     *
     */
    public void Init(int type) {

        this.type = type;

        if (type == HEAD_CELL) {
            this.color = HEAD_CELL_COLOR; // Growing vessel cells
        } else if (type == BODY_CELL) {
            this.color = BODY_CELL_COLOR; // vessel stalk cells
        } else if (type == MAP_PARTICLE) {
            this.color = MAP_PARTICLE_COLOR; // normal MAP
        } else if (type == HEPARIN_MAP) {
            this.color = HEPARIN_MAP_COLOR; // Heparin MAP
        }
    }

    /**
     * Initializes a vessel cell, but passes on arrival state to next generations.
     *
     * @param type type of cell/particle
     * @param length the current length of the vessel
     */
    public void InitVessel(int type, int length) {
        this.type = type;
        this.length = length;
        this.color = HEAD_CELL_COLOR;
    }

    /**
     * Initializes a cell with color and type,
     *
     * @param type type of cell/particle
     * @param length the current length of the vessel
     * @param target the location where the vessel is growing towards
     * @param elongationLength the current elongation length of the vessel head cell
     */
    public void InitVesselBranching(int type, int length, int target, int elongationLength) {
        this.type = type;
        this.length = length;
        this.target = target;
        this.elongationLength = elongationLength;
        this.color = HEAD_CELL_COLOR;
    }

    /**
     * Initializes a cell with color and type,
     *
     * @param type type of cell/particle
     * @param length the current length of the vessel
     * @param target the location where the vessel is growing towards
     * @param elongationLength the current elongation length of the vessel head cell
     */
    public void InitVesselMigrationRate(int type, int length, int target, int elongationLength, int since_last_elongation) {
        this.type = type;
        this.length = length;
        this.target = target;
        this.elongationLength = elongationLength;
        this.since_last_elongation = since_last_elongation;
        this.color = HEAD_CELL_COLOR;
    }


    /**
     * Divides a cell to a random nearby location, NOT allowing overlap with vessels or MAP
     */
    public void randomDivideNotOverlap() {
        assert G != null;
        int options = MapEmptyHood(G.divHood); // check for empty spots within its division hood
        if (options >= 1) { // if there is an empty spot, divide into a random one of the empty spots
            G.NewAgentSQ(G.divHood[G.rng.Int(options)]).InitVessel(HEAD_CELL, this.length + 1);
            InitVessel(BODY_CELL, this.length);
        }
    }

    /**
     * Calls cell to consume appropriate amount of VEGF
     */
    public void ConsumeVEGF() {
        assert G != null;
        if ((G.VEGF.Get(Isq()) != 0) && ((type == HEAD_CELL) || (type == BODY_CELL))) { // Head cells and body cells consume VEGF
            G.VEGF.Add(Isq(), -(G.VEGF.Get(Isq()))*VEGF_INTAKE);
            if (G.VEGF.Get(Isq()) < 0){
                G.VEGF.Set(Isq(), 0);
            }
        }
    }

    /**
     * Determines whether the endothelial cells should begin dividing, according to VESSEL_GROWTH_DELAY
     */
    public void CheckStartVesselGrowth() {
        assert G != null;
        if (G.GetTick() == VESSEL_GROWTH_DELAY) { // If the VESSEL_GROWTH_DELAY has passed, then vessels can begin to grow
            start_vessel_growth = true;
        }
    }

    /**
     * Initializes VEGF concentrations
     */
    public void InitializeVEGF() {
        if (type == HEPARIN_MAP) {
            double toAdd = (1 - G.VEGF.Get(Isq())) * 0.5;
            G.VEGF.Add(Isq(), (toAdd));
            if ((G.VEGF.Get(Isq())) > 1) {
                G.VEGF.Set(Isq(), 1);
            }
        }
    }

    /**
     * Performs simple vessel growth (functional)
     * @param splitProb probability that the vessel will split
     */
    public void EndothelialGrowth(double splitProb) {
        if (start_vessel_growth){
            if (type == HEAD_CELL) {
                assert G != null;
                int TargetLocation = HighestConcentrationVEGF(); // The target location to grow is the location with the hightest concentration of VEGF
                if (TargetLocation != 0) { // If there is a location of highest VEGF...
                    int cellDivLocation = HoodClosestToTarget(TargetLocation); // take the position of the target and find the closest neighborhood division spot to the target
                    if (G.PopAt(cellDivLocation) < 5) { // if the area is not too crowded
                        G.NewAgentSQ(cellDivLocation).InitVessel(HEAD_CELL, this.length + 1); // make a new head cell there
                        InitVessel(BODY_CELL, this.length); // and turn the old cell into a body cell
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
     * Performs vessel elongation more analogous to that specified in Mehdizadeh et al. (Does not work well)
     * @param elongationLength the current length of elongation
     * @param targetCoord the coordinate that the vessel is attempting to reach (has highest VEGF concentration)
     */
    public void VesselElongationGrowth(int elongationLength, int targetCoord) {
        if (type == HEAD_CELL && start_vessel_growth) {
            assert G != null;
            int cellDivLocation;
            if ((elongationLength >= MAX_ELONGATION_LENGTH) || (Isq() == targetCoord) || (targetCoord == 0)) { // if the vessel
                                                            // needs to find a new target (i.e. it has reached max elongation,
                                                            // it has reached a target, or it doesn't have a target...
                elongationLength = 0; // reset elongation length
                int highestConcentrationCoord = HighestConcentrationVEGF(); // find new target location
                cellDivLocation = HoodClosestToTarget(highestConcentrationCoord); // and find the closest adjacent coordinate to this location
                if ((highestConcentrationCoord != 0) && (cellDivLocation != 0)){ // If there is a location of highest concentration and there is an open adjacent spot...
                    if (G.PopAt(cellDivLocation) < 5) { // and if the area is not too crowded
                        G.NewAgentSQ(cellDivLocation).InitVesselBranching(HEAD_CELL, this.length + 1, highestConcentrationCoord, elongationLength + 1); // make a new cell there
                        InitVessel(BODY_CELL, this.length); // and make the old cell a body cell
                    }
                } else { // if there was not a location of highest concentration VEGF then divide randomly
                    randomDivideNotOverlap();
                }
                // branching
                if ((G.VEGF.Get(Isq()) > 0.7) && (G.rng.Double() < 0.1)) { // if there is enough VEGF
                    int options = MapEmptyHood(G.divHood);
                    if (options >= 1) { // if there is an open nearby location, then branch there
                        G.NewAgentSQ(G.divHood[G.rng.Int(options)]).InitVesselBranching(HEAD_CELL, this.length + 1, 0, 0);
                        InitVessel(BODY_CELL, this.length);
                    }
                }
            } else {
                // if not max length, not at target then it has a target to get to.
                cellDivLocation = HoodClosestToTarget(targetCoord); // take the int position and find the closest neighborhood division spot
                if (G.PopAt(cellDivLocation) < 5) { // if the area is not too crowded
                    G.NewAgentSQ(cellDivLocation).InitVesselBranching(HEAD_CELL, this.length + 1, targetCoord, elongationLength + 1); // make a new cell there
                    InitVessel(BODY_CELL, this.length);
                    // supposed to be random movement if there is no VEGF nearby
                } else {
                    randomDivideNotOverlap();
                }
            }
        }
    }

    /**
     * Performs vessel elongation more analogous to that specified in Mehdizadeh et al. Functions by growth rate
     * @param elongationLength the current length of elongation
     * @param targetCoord the coordinate that the vessel is attempting to reach (has highest VEGF concentration)
     */
    public void VesselGrowthByRate(int elongationLength, int targetCoord) {
        assert G != null;
        if (since_last_elongation < migration_rate){
            since_last_elongation += 1;
            return;
        }
        if (type == HEAD_CELL && start_vessel_growth) {
            int cellDivLocation;
            if ((elongationLength >= MAX_ELONGATION_LENGTH) || (Isq() == targetCoord) || (targetCoord == 0)) { // if the vessel
                // needs to find a new target (i.e. it has reached max elongation,
                // it has reached a target, or it doesn't have a target...
                elongationLength = 0; // reset elongation length
                int highestConcentrationCoord = HighestConcentrationVEGF(); // find new target location
                cellDivLocation = HoodClosestToTarget(highestConcentrationCoord); // and find the closest adjacent coordinate to this location
                if ((highestConcentrationCoord != 0) && (cellDivLocation != 0)){ // If there is a location of highest concentration and there is an open adjacent spot...
                    if (G.PopAt(cellDivLocation) < 5) { // and if the area is not too crowded
                        G.NewAgentSQ(cellDivLocation).InitVesselMigrationRate(HEAD_CELL, this.length + 1, highestConcentrationCoord, elongationLength + 1, since_last_elongation); // make a new cell there
                        InitVessel(BODY_CELL, this.length); // and make the old cell a body cell
                    }
                } else { // if there was not a location of highest concentration VEGF then divide randomly
                    randomDivideNotOverlap();
                }
                // branching
//                if ((G.VEGF.Get(Isq()) > 0.7) && (G.rng.Double() < 0.1)) { // if there is enough VEGF
                if(G.rng.Double() < branching_probability){
                    int options = MapEmptyHood(G.divHood);
                    if (options >= 1) { // if there is an open nearby location, then branch there
                        G.NewAgentSQ(G.divHood[G.rng.Int(options)]).InitVesselMigrationRate(HEAD_CELL, this.length + 1, 0, 0, 0);
                        InitVessel(BODY_CELL, this.length);
                    }
                }
            } else {
                // if not max length, not at target then it has a target to get to.
                cellDivLocation = HoodClosestToTarget(targetCoord); // take the int position and find the closest neighborhood division spot
                if (G.PopAt(cellDivLocation) < 5) { // if the area is not too crowded
                    G.NewAgentSQ(cellDivLocation).InitVesselMigrationRate(HEAD_CELL, this.length + 1, targetCoord, elongationLength + 1, since_last_elongation); // make a new cell there
                    InitVessel(BODY_CELL, this.length);
                    // supposed to be random movement if there is no VEGF nearby
                } else {
                    randomDivideNotOverlap();
                }
            }
        }
    }


    public void CalculateBranchingProbability(){
        if (G.VEGF.Get(Isq()) < LOW_MED_VEGF_THRESHOLD){
            branching_probability = LOW_BRANCHING_PROBABILITY;
        } else if (G.VEGF.Get(Isq()) < MED_HIGH_VEGF_THRESHOLD){
            branching_probability = MED_BRANCHING_PROBABILITY;
        } else if (G.VEGF.Get(Isq()) > HIGH_BRANCHING_PROBABILITY){
            branching_probability = HIGH_BRANCHING_PROBABILITY;
        }
    }

    /**
     * Steps an agent, can be used on all implemented agents
     */
    public void StepCell() {

        // Eat VEGF
        ConsumeVEGF();

        // initialize VEGF
        InitializeVEGF();


        // calculate branching probability
        CalculateBranchingProbability();

        // check if endothelial cells will divide yet
        CheckStartVesselGrowth();

        // Elongate
        VesselGrowthByRate(0, 0);
//        VesselElongationGrowth(elongationLength, 0);
    }
}
