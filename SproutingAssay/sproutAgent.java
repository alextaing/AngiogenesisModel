/*
GRIFFIN LAB
ALEX TAING, UNDERGRADUATE
FALL 2020
*/

package SproutingAssay;

import HAL.GridsAndAgents.AgentSQ2D;
import HAL.Util;

import java.util.ArrayList;

import static SproutingAssay.sproutGrid.PERSISTENCY_TIME;

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

    public static double VESSEL_VEGF_INTAKE = sproutGrid.VESSEL_VEGF_INTAKE;
    public final static int VESSEL_GROWTH_DELAY = sproutGrid.VESSEL_GROWTH_DELAY;
    public final static double VEGF_SENSITIVITY = sproutGrid.VEGF_SENSITIVITY;
    //public final static int MAX_ELONGATION_LENGTH = sproutGrid.MAX_ELONGATION_LENGTH;
    public static final double HEP_MAP_VEGF_RELEASE = sproutGrid.HEP_MAP_VEGF_RELEASE;

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
    int ticks_since_directionchange;
    public static boolean start_vessel_growth = true; // is set to true when vessels begin to invade
    boolean heparin_particle_release_VEGF = true;


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
                    break;
                }
            }
            if (MAPcount == 0) { // If there were no occlusions with MAP particles, then check to see if it is close to the target point
                int[] hoodPoint = {G.ItoX(G.divHood[i]), G.ItoY(G.divHood[i])}; // This is the location of the point in question (neighborhood point
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
            this.heparin_particle_release_VEGF = true;
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
    public void InitVesselMigrationRate(int type, int length, int target, int elongationLength, int since_last_elongation) {
        this.type = type;
        this.length = length;
        this.target = target;
        this.elongationLength = elongationLength;
        this.since_last_elongation = since_last_elongation;
        this.color = HEAD_CELL_COLOR;
    }

    /**
     * Calls cell to consume appropriate amount of VEGF
     */
    public void ConsumeVEGF() {
        assert G != null;
        if ((G.VEGF.Get(Isq()) >= 0) && ((type == HEAD_CELL) || (type == BODY_CELL))) { // Head cells and body cells consume VEGF
            G.VEGF.Add(Isq(), -(G.VEGF.Get(Isq()))* VESSEL_VEGF_INTAKE);
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
        if (type == HEPARIN_MAP && heparin_particle_release_VEGF) {

            assert G != null;
            int adjacent = G.MapHood(G.divHood, Isq()); // open areas around heparin particle

            for (int i = 0; i < adjacent; i++) { // iterate thorough the adjacent areas
                for (sproutAgent agent : G.IterAgents(G.divHood[i])) { // iterate through all the units around the coordinate
                    if ((!agent.heparin_particle_release_VEGF) || (agent.type == HEAD_CELL)|| (agent.type == BODY_CELL)) { // if there is MAP GEL there
                        this.heparin_particle_release_VEGF = false; // then keep track that there was a particle there.
                        return;
                    }
                }
            }

            double toAdd = (1 - G.VEGF.Get(Isq())) * HEP_MAP_VEGF_RELEASE; // the more VEGF present, the less VEGF released-- with a max of 1
            // HEP_MAP_VEGF_RELEASE is the percent of (1-currentVEGF) to add.
            // i.e. if 0.2 VEGF present, and HEP_MAP_VEGF_RELEASE = 0.5, then 0.4 VEGF would be added (1-0.2)*0.5 = 0.4
            // CAN BE CHANGED. only reason this was changed was because VEGF intake was changed to be a percentage of
            // VEGF present, so I wanted to be consistent and make it such that VEGF initialization around HEP_MAP particles
            // operated similarly (as a function of VEGF already present).

            G.VEGF.Add(Isq(), (toAdd));
            if ((G.VEGF.Get(Isq())) > 1) {
                G.VEGF.Set(Isq(), 1);
            }
        }
    }

    // Edit for changing elongation length to persistency time
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * Performs vessel elongation more analogous to that specified in Mehdizadeh et al. Functions by growth rate
     */
    public void VesselGrowthByRate() {
        // checks of G is null (just for safely)
        assert G != null;

        // only continue for head cells
        if (type != HEAD_CELL){
            return;
        }

        // only continue if there is enough VEGF around, if not then stay quiescent
        if (G.VEGF.Get(Isq()) < VEGF_SENSITIVITY) {
            return;
        }

        // Makes the vessel grow at a certain rate (i.e. 30 microns/hr) (this is NOT elongation length!!!)
        if (since_last_elongation < migration_rate){ // if it's not yet time to migrate, then don't migrate yet.
            since_last_elongation += 1;
            ticks_since_directionchange +=1;
            return;
        }


        if (start_vessel_growth) {
            int cellDivLocation; // stores the int version of the location where the cell will grow to

            // TODO: ELONGATION LENGTH modifications
                // some implementation ideas:
                // 1) store of the time tick of the time it's changed direction, and change
                // direction once time elapsed > persistency time, then find a new direction
                // 2) keep a running tally of time since last change direction, and find a
                // new direction once that tally > persistency time
           // if ((elongationLength >= MAX_ELONGATION_LENGTH) || (Isq() == target) || (target == 0)) {
            if (( ticks_since_directionchange >= PERSISTENCY_TIME) || (Isq() == target) || (target == 0)){

                int highestConcentrationCoord = HighestConcentrationVEGF(); // find new target location: the location in the sight radius with the highest VEGF
                highestConcentrationCoord = CalculateTargetTwiceAsFar(highestConcentrationCoord); // find a point in the same direction, but very far away, so you won't reach it: want to persist in that direction
                cellDivLocation = HoodClosestToTarget(highestConcentrationCoord); // and find the closest adjacent coordinate in the direction towards the highestConcentrationCoord

                if ((highestConcentrationCoord > 0) && (cellDivLocation > 0)){ // If there is a location of highest concentration and there is an open adjacent spot... (the values will be 0 if none were found)
                    G.NewAgentSQ(cellDivLocation).InitVesselMigrationRate(HEAD_CELL, this.length + 1, highestConcentrationCoord, 0, since_last_elongation); // make a new head cell at cellDivLocation
                    InitVessel(BODY_CELL, this.length); // and make the old cell a body cell

                }

                // branching

                // TODO: (for alex) check if branching probability is being properly modified.  I think it is, but I think it's worth a double check
                if(G.rng.Double() < branching_probability){ // if the cell happens to branch (branching probability is a parameter of the cell, and is modified by a function CalculateBranchingProbability)

                    // the options for branching locations around the cell
                    int options = MapEmptyHood(G.divHood);
                    if (options >= 1) { // if there is an open nearby location, then branch there
                        G.NewAgentSQ(G.divHood[G.rng.Int(options)]).InitVesselMigrationRate(HEAD_CELL, this.length + 1, 0, 0, 0); // make a new head cell at the branching location
                        InitVessel(BODY_CELL, this.length); // make the old cell a body cell
                    }
                }
            } else {
                // if not at max length and not at it's target location, then it has a target that it needs to get to.
                cellDivLocation = HoodClosestToTarget(target); // find an open adjacent location closest to the target
                G.NewAgentSQ(cellDivLocation).InitVesselMigrationRate(HEAD_CELL, this.length + 1, target, ticks_since_directionchange, since_last_elongation); // make a new cell there LP QUestion: how do I make ticks_since_direction change= 0
                InitVessel(BODY_CELL, this.length); // make the old cell a body cell
            }


        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public int CalculateTargetTwiceAsFar(int targetCoord){
        assert G != null;
        int target_x = G.ItoX(targetCoord);
        int target_y = G.ItoY(targetCoord);
        int current_x = Xsq();
        int current_y = Ysq();
        int scaled_vector_x = 4*(target_x-current_x);
        int scaled_vector_y = 4*(target_y-current_y);
        int[] new_target = {(scaled_vector_x+current_x),(scaled_vector_y+current_y)};
        return G.I(new_target[0], new_target[1]);
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
        VesselGrowthByRate();
    }
}
