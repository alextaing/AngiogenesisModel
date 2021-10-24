/*
GRIFFIN LAB
ALEX TAING, UNDERGRADUATE
FALL 2020
*/

package SproutingAssay;

import HAL.GridsAndAgents.AgentSQ2D;
import HAL.Util;

import java.util.ArrayList;

import static SproutingAssay.sproutGrid.MAX_ELONGATION_LENGTH;
import static SproutingAssay.sproutGrid.PERSISTENCY_TIME;

public class sproutAgent extends AgentSQ2D<sproutGrid> {


    // DO NOT MODIFY FOR PARAMETERS
    public static int HEAD_CELL = 0;
    public static int BODY_CELL = 1;
    public static int MAP_PARTICLE = 2;
    public static int HEPARIN_MAP = 3;

    public static int HEAD_CELL_COLOR = Util.RED;
    public static int BODY_CELL_COLOR = Util.RED;
    public static int MAP_PARTICLE_COLOR = Util.RGB(128.0 / 255, 128.0 / 255, 128.0 / 255); // normal MAP;
    public static int HEPARIN_MAP_COLOR = Util.RGB(0.0 / 255, 0.0 / 255, 217.0 / 255); // Heparin MAP;

    public static double VESSEL_VEGF_INTAKE = sproutGrid.VESSEL_VEGF_INTAKE;
    public final static int VESSEL_GROWTH_DELAY = sproutGrid.VESSEL_GROWTH_DELAY;
    public final static double VEGF_SENSITIVITY = sproutGrid.VEGF_SENSITIVITY;
    //public final static int MAX_ELONGATION_LENGTH = sproutGrid.MAX_ELONGATION_LENGTH;
    public final static double PERSISTENCY_TIME = sproutGrid.PERSISTENCY_TIME; //IS THIS NECESSARY HERE??
    public static final double HEP_MAP_VEGF_RELEASE = sproutGrid.HEP_MAP_VEGF_RELEASE;
    public static final double MEDIA_EXCHANGE_SCHEDULE_TICKS = sproutGrid.MEDIA_EXCHANGE_SCHEDULE_TICKS;

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
     */
    public void InitVesselMigrationRate(int type, int length, int target, int since_last_elongation, int ticks_since_directionchange) {
        this.type = type;
        this.length = length;
        this.target = target;
        this.since_last_elongation = since_last_elongation;
        this.color = HEAD_CELL_COLOR;
        this.ticks_since_directionchange = ticks_since_directionchange;
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

    public void ExchangeMedia(){
        assert G != null;
        if (G.GetTick()%MEDIA_EXCHANGE_SCHEDULE_TICKS == 0){
            if (type == HEPARIN_MAP){
                G.VEGF.Set(Isq(), HEP_MAP_VEGF_RELEASE);
            }
        }

    }


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

        if (ticks_since_directionchange <= PERSISTENCY_TIME) {
            ticks_since_directionchange += 1;
        }

        // Makes the vessel grow at a certain rate (i.e. 30 microns/hr) (this is NOT elongation length!!!)
        if (since_last_elongation < migration_rate){ // if it's not yet time to migrate, then don't migrate yet.
            since_last_elongation += 1;
            return;
        }

        // only continue if there is enough VEGF around, if not then stay quiescent
        if (G.VEGF.Get(Isq()) < VEGF_SENSITIVITY) {
            return;
        }

        if (start_vessel_growth) {
            int cellDivLocation; // stores the int version of the location where the cell will grow to
            // ALEX - LP question - could you better comment what each of these if statements is doing? Also, I am trying to determine if we also want to add
            // in a branching delay time (typically 8 hrs) or if that is too complex and only rely on the branching probability. The main thing I want to make sure of
            // for this section though is that it only resets the ticker if it changes direction
            if (( ticks_since_directionchange >= PERSISTENCY_TIME) || (target == 0)){ // took out if reached target
                int highestConcentrationCoord = HighestConcentrationVEGF(); // find new target location: the location in the sight radius with the highest VEGF
                highestConcentrationCoord = CalculateTargetScaleTargetCoord(highestConcentrationCoord); // find a point in the same direction, but very far away, so you won't reach it: want to persist in that direction
                cellDivLocation = HoodClosestToTarget(highestConcentrationCoord); // and find the closest adjacent coordinate in the direction towards the highestConcentrationCoord
                if ((highestConcentrationCoord > 0) && (cellDivLocation > 0)){ // If there is a location of highest concentration and there is an open adjacent spot... (the values will be 0 if none were found)
                    G.NewAgentSQ(cellDivLocation).InitVesselMigrationRate(HEAD_CELL, this.length + 1, highestConcentrationCoord, since_last_elongation, 0); // make a new head cell at cellDivLocation
                    InitVessel(BODY_CELL, this.length); // and make the old cell a body cell
                }

                // branching

                if(G.rng.Double() < branching_probability){ // if the cell happens to branch (branching probability is a parameter of the cell, and is modified by a function CalculateBranchingProbability)
                    // the options for branching locations around the cell
                    int options = MapEmptyHood(G.divHood);
                    if (options >= 1) { // if there is an open nearby location, then branch there
                        G.NewAgentSQ(G.divHood[G.rng.Int(options)]).InitVesselMigrationRate(HEAD_CELL, this.length + 1, 0, 0, this.ticks_since_directionchange=0); // make a new head cell at the branching location
                        InitVessel(BODY_CELL, this.length); // make the old cell a body cell
                    }
                }
            } else if (Isq()!= target){ //added else if
                // if not at max length and not at it's target location, then it has a target that it needs to get to.
                cellDivLocation = HoodClosestToTarget(target); // find an open adjacent location closest to the target
                G.NewAgentSQ(cellDivLocation).InitVesselMigrationRate(HEAD_CELL, this.length + 1, target, since_last_elongation, ticks_since_directionchange); // make a new cell there LP QUestion: how do I make ticks_since_direction change= 0
                InitVessel(BODY_CELL, this.length); // make the old cell a body cell
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public int CalculateTargetScaleTargetCoord(int targetCoord){
        assert G != null;
        int scale = MAX_ELONGATION_LENGTH;
        int target_x = G.ItoX(targetCoord);
        int target_y = G.ItoY(targetCoord);
        int current_x = Xsq();
        int current_y = Ysq();
        int scaled_vector_x = scale*(target_x-current_x);
        int scaled_vector_y = scale*(target_y-current_y);
        int[] new_target = {(scaled_vector_x+current_x),(scaled_vector_y+current_y)};
        while (!G.In(new_target[0], new_target[1])){
            scale -= 1;
            scaled_vector_x = scale*(target_x-current_x);
            scaled_vector_y = scale*(target_y-current_y);
            new_target[0] = scaled_vector_x+current_x;
            new_target[1] = (scaled_vector_y+current_y);
        }
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
//        InitializeVEGF();
        ExchangeMedia();

        // calculate branching probability
        CalculateBranchingProbability();

        // check if endothelial cells will divide yet
        CheckStartVesselGrowth();

        // Elongate
        VesselGrowthByRate();
    }
}
