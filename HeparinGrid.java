package PDEpractice;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2D;
import HAL.GridsAndAgents.PDEGrid2D;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

/// CURRENT ISSUES:
/// 1) Code written to go to closest VEGF, regardless of strength. Need to make it so that stronger VEGF attracts more
// cell growth, because current model does very little since it cannot tell the difference between weak and strong VEGF
/// 3) Addition of a timer to see how long it takes for vascular cells to arrive at the other side
/// 4) Addition of Phagocytes to "turn on" Heparin and allow it to begin secreting VEGF
/// 5) VEGF continues to diffuse and get more concentrated
/// 6) if a lot of VEGF around, they tend to just head straight up
/// 7) NOTE that endo will not overlap when there is no VEGF

/// THINGS THAT CAN BE VARIED FOR EXPERIMENTATION:
/// Ratio of MAP to Heparin MAP (HepToMapRatio)
/// Radius of VEGF sensing (rad)
/// Division chance, when in, and not in, the presence of VEGF (divProb)
/// Diffusion rate of VEGF (diffusionCoefficient)
/// Chance for endothelial cell to branch (splitProb)
/// Chance for body cell to begin branching (bodyCellBranchProb)
/// Percent of spawning branch from injury site (startVascularChance)
/// how much VEGF is uptaken by an endothelial cell (vascularVEGFIntake)

class EndoCell extends AgentSQ2D<HeparinGrid>{

    /**
     * VEGFdivProb
     */
    double VEGFdivProb = 0.5;

    int color;
    int type;

    public int HighestConcentrationVEGF(){
        int VEGFoptions = G.MapEmptyHood(G.VEGFHood, Isq()); // gets the cell's range of detecting VEGF

        double maxConcentration = -1;
        ArrayList<Integer> maxConcentrationLocations = new ArrayList<>();
        for (int i = 0; i < VEGFoptions; i++) { // if there's nearby VEGF...
            if (G.VEGF.Get(G.VEGFHood[i]) > maxConcentration){
                maxConcentration = G.VEGF.Get(G.VEGFHood[i]);
                maxConcentrationLocations.clear();
                maxConcentrationLocations.add(G.VEGFHood[i]);
            } else if (G.VEGF.Get(G.VEGFHood[i]) == maxConcentration) {
                maxConcentrationLocations.add(G.VEGFHood[i]);
            }
        }
        if (maxConcentrationLocations.size() == 0) {
            return 0;
        } else if (maxConcentration == -1){
            return 0;
        }
        return maxConcentrationLocations.get((int)(Math.random()*maxConcentrationLocations.size()));
    }

    /**
     * Given an int location of a target, will find best location for next cell duplication
     * @param heparinLocation location of nearest heparin
     * @return int location to grow closer to target
     */

    public int HoodClosestToVEGF(int heparinLocation){

        int minDistance = Integer.MAX_VALUE; // gets updated with each location check
        ArrayList<Integer> mincoordint = new ArrayList<>(); // closest points that are in the cell neighborhood (as an int)

        assert G != null;
        int options = G.MapHood(G.divHood, Isq()); // open areas around cell

        for (int i = 0; i < options; i++) {
            int MAPcount = 0;
            for (EndoCell cell : G.IterAgents(G.divHood[i])) {
                if ((cell.type == 1) || (cell.type == 2)) {
                    MAPcount++;
                }
            }
            if (MAPcount == 0){
                int[] hoodPoint = {G.ItoX(G.divHood[i]), G.ItoY(G.divHood[i])};

                // gets the distance from neighborhood area to target
                int dist = Math.abs((int)Math.hypot(G.ItoX(heparinLocation)-hoodPoint[0], G.ItoY(heparinLocation)-hoodPoint[1]));

                // keeps a list of the hood points closest to the VEGF
                if (dist < minDistance){ // if the new hood point distance is closer than the one before, then...
                    minDistance = dist; // the minimum distance is updated to the new closest distance
                    mincoordint.clear();// the old list is cleared
                    mincoordint.add(G.I(hoodPoint[0], hoodPoint[1])); // and the new closest point is added to the empty list
                }else if (dist == minDistance){ // But, if the point is just as close as the ones on the list
                    mincoordint.add(G.I(hoodPoint[0], hoodPoint[1])); // it is added to the list of the hood points that are just as close
                }
            }
        }
        if (mincoordint.size() == 0){ // if there are no sufficiently strong VEGF nearby, then...
            return 0; // return 0
        }
        return mincoordint.get((int)(Math.random()*mincoordint.size())); // otherwise, return a random hood point on the list
    }


    /**
     * Initializes a cell with color and type (0 are active endothelial cells, 1 are HeparinMAP, 2 are normal MAP, and 3 are inactive Endothelial cells)
     * @param type: type of cell/particle
     */
    public void Init(int type){
        this.type = type;
        if (type == 0) {
            color = Util.RED; // Growing endothelial cells
        } else if (type == 1){
            color = Util.GREEN; // Heparin MAP
        } else if (type == 2){
            color = Util.WHITE; // normal MAP
        } else if (type == 3) { // Inactive Endothelial cells
            color = Util.RED;
        }
    }

    /**
     * cell chemotaxis: attempts to grow towards closest VEGF (issue: dependence on strength is not yet implemented)
     * @param divProb: chance of division
     * @param splitProb: chance of branching endothelial cell
     */
    public void StepCell(double divProb, double splitProb){

        double bodyCellBranchProb = 1.0/100000;

        double vascularVEGFIntake = .8; // how much VEGF is used when a blood vessel is nearby
        if ((G.VEGF.Get(Isq()) != 0) && ((type == 0)||(type == 3))){
            G.VEGF.Add(Isq(), -vascularVEGFIntake);
        }

        // initialize VEGF
        if (type == 1) {
            G.VEGF.Set(Isq(), 1);
            return;

        // Normal MAP Cells: nothing
        } else if (type == 2 || type == 3){
            if (type == 3){ // just in case all heads come to a dead end, new head cells can still form
                if (G.rng.Double() < splitProb*bodyCellBranchProb) {
                    int options2 = MapEmptyHood(G.divHood);
                    if (options2 > 0) {
                        G.NewAgentSQ(G.divHood[G.rng.Int(options2)]).Init(0);
                    }
                }
            }
            return;
        }

        // dividing endo cells
        if (G.rng.Double() < divProb){ // if cell chances to divide
            int HighestConcentrationVEGF = HighestConcentrationVEGF();
            if (HighestConcentrationVEGF > 0){
                int cellDivLocation = HoodClosestToVEGF(HighestConcentrationVEGF); // take the int position and find the closest neighborhood division spot
                if (G.PopAt(cellDivLocation) < 5){ // if the area is not too crowded
                    G.NewAgentSQ(cellDivLocation).Init(0); // make a new cell there
                    this.type = 3;
                }
            } else { // supposed to be random movement if there is no VEGF nearby
                int options = MapEmptyHood(G.divHood);
                if (options > 0) {
                    G.NewAgentSQ(G.divHood[G.rng.Int(options)]).Init(0);
                    this.type = 3;
                }
            }
            if (G.rng.Double() < splitProb){ // maybe branch off
                int options2 = MapEmptyHood(G.divHood);
                if (options2 > 0) {
                    G.NewAgentSQ(G.divHood[G.rng.Int(options2)]).Init(0);
                }
            }
        }
    }
}

public class HeparinGrid extends AgentGrid2D<EndoCell> {

    /**
     * rad and diffusionCoefficient
     */
    int rad = 2; // radius for VEGF sight
    double diffusionCoefficient = 0.07; // diffusion coefficient

    Rand rng = new Rand();
    int[] divHood = Util.VonNeumannHood(false); // neighborhood for division
    int[] VEGFHood = Util.CircleHood(false, rad); // how far can you see VEGF

    PDEGrid2D VEGF; // Initialize PDE Grid

    public HeparinGrid (int x, int y) { // Constructor for the agent grid
        super(x, y, EndoCell.class);
        VEGF = new PDEGrid2D(x, y);
    }

    public void StepCells(double divProb, double splitProb){ // steps all the cells
        for (EndoCell endoCell : this) {
            endoCell.StepCell(divProb, splitProb);
        }
        VEGF.Diffusion(diffusionCoefficient);
        VEGF.Update();
    }

    public void DrawPDE(GridWindow windows){ // draws the PDE window
        for (int i = 0; i < length; i++) {
            windows.SetPix(i,Util.HeatMapRGB(VEGF.Get(i)));
        }
    }

    public void DrawModel(GridWindow win){ // Draws the Agent model
        for (int i = 0; i < length; i++) {
            int color = Util.BLACK;
            if (GetAgent(i) != null) {
                EndoCell cell = GetAgent(i);
                color = cell.color;
            }
            win.SetPix(i, color);
        }
    }

    /**
     * Control Panel!
     */
    public static void main(String[] args) {
        int x = 100; // size of the window
        int y = 100;
        int timesteps = 2000; // how long will the simulation run?
        double divProb = 0.5; // chance of dividing not in presence of VEGF
        int MAP_particles = 800; // number of MAP particles
        double HepToMapRatio = 0.1; // percent of MAP that are Heparin MAP
        double splitProb = 0.001; // how likely is endothelial cell to branch
        double startVascularChance = 0.1; // percent of initializing an off branch from wound site

        GridWindow gridwin = new GridWindow("Endothelial Cells",x, y, 5); // window for agents
        GridWindow VEGFWin = new GridWindow("VEGF Diffusion", x, y, 5); // window for diffusion

        HeparinGrid model = new HeparinGrid(x, y); // instantiate agent grid


        // initialize
        for (int i = 0; i < model.Xdim(); i++) {
            if (Math.random() < startVascularChance){
                model.NewAgentSQ(i,0).Init(0);
            } else {
                model.NewAgentSQ(i, 0).Init(3);
            }
        }

        for (int i = 0; i < MAP_particles; i++) { // creates the MAPs
            int celltype = 2;
            double chance = Math.random();
            if (chance < HepToMapRatio){
                celltype = 1;
            }
            int randx =(int)(model.xDim*Math.random());
            int randy =(int)(model.yDim*Math.random());
            model.NewAgentSQ(randx,randy).Init(celltype);
        }

        for (int i = 0; i < timesteps; i++){
            // pause
            gridwin.TickPause(20); // how fast the simulation runs

            // model step
            model.StepCells(divProb, splitProb); // step the cells

            // draw
            model.DrawPDE(VEGFWin); // draw the PDE window
            model.DrawModel(gridwin); // draw the agent window
        }
    }
}






