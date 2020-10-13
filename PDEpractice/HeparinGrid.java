package PDEpractice;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2D;
import HAL.GridsAndAgents.PDEGrid2D;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;
import java.util.ArrayList;
import java.util.Collections;

/// CURRENT ISSUES:
/// 1) Code written to go to closest VEGF, regardless of strength. Need to make it so that stronger VEGF attracts more
// cell growth, because current model does very little since it cannot tell the difference between weak and strong VEGF
/// 2) Endothelial cells tend to move in a straight line unless blocked by MAP, sometimes b/c ^^^
/// 3) Addition of a timer to see how long it takes for vascular cells to arrive at the other side
/// 4) Addition of Phagocytes to "turn on" Heparin and allow it to begin secreting VEGF
/// 5) VEGF continues to diffuse and get more concentrated
/// 6) if a lot of VEGF around, they tend to just head straight up

/// THINGS THAT CAN BE VARIED FOR EXPERIMENTATION:
/// Ratio of MAP to Heparin MAP (HepToMapRatio)
/// Radius of VEGF sensing (rad)
/// Division chance, when in, and not in, the presence of VEGF (divProb)
/// Diffusion rate of VEGF (diffusionCoefficient)
/// VEGF sensitivity (VEGFsensitivity)
/// Chance for endothelial cell to branch (splitProb)
/// Percent of spawning branch from injury site (startVascularChance)
/// how much VEGF is uptaken by an endothelial cell (vascularVEGFIntake)

class EndoCell extends AgentSQ2D<HeparinGrid>{

    /**
     * VEGFsensitivity, VEGFdivProb
     */
    double VEGFsensitivity = 0.05;
    double VEGFdivProb = 0.5;

    int color;
    int type;

    /**
     * Given an int location of a target, will find best location for next cell duplication
     * @param heparinLocation loation of nearest heparin
     * @return int location to grow closer to target
     */
    public int HoodClosestToHep(int heparinLocation){

        double minDistance = Integer.MAX_VALUE; // gets updated with each location check
        int mincoordint = 0; // closest point that is in the cell neighborhood (as an int)

        assert G != null;
        int options = G.MapEmptyHood(G.divHood, Isq()); // open areas around cell

        for (int i = 0; i < options; i++) {
            int[] hoodPoint = {G.ItoX(G.divHood[i]), G.ItoY(G.divHood[i])};

            // gets the distance from neighborhood area to target
            double dist = Math.hypot(G.ItoX(heparinLocation)-hoodPoint[0], G.ItoY(heparinLocation)-hoodPoint[1]);
            if (dist <= minDistance){
                minDistance = dist;
                mincoordint = G.I(hoodPoint[0], hoodPoint[1]);
            }
        }
        if (mincoordint == 0){
            mincoordint = Isq();
        }
        return mincoordint;
    }


    /**
     * Initializes a cell with color and type (1 is endothelial, 2 is HeparinMAP, and 3 is normal MAP)
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

        double vascularVEGFIntake = .8; // how much VEGF is used when a blood vessel is nearby
        if ((G.VEGF.Get(Isq()) != 0) && ((type == 0)||(type == 3))){
            G.VEGF.Add(Isq(), -vascularVEGFIntake);
        }

        if (type == 1) {
            G.VEGF.Set(Isq(), 1);
            return;

        // Normal MAP Cells: nothing
        } else if (type == 2 || type == 3){
            if (type == 3){ // just in case all heads come to a dead end, new head cells can still form
                if (G.rng.Double() < splitProb/200) {
                    int options2 = MapEmptyHood(G.divHood);
                    if (options2 > 0) {
                        G.NewAgentSQ(G.divHood[G.rng.Int(options2)]).Init(0);
                    }
                }
            }
            return;
        }

        // Endothelial Cells
        ArrayList<Double> VEGFdistance = new ArrayList<>(); // list of distances of VEGF from cell
        ArrayList<Integer> nearbyVEGF = new ArrayList<>(); // the location of the close VEGF ^^^

        int VEGFoptions = G.MapEmptyHood(G.VEGFHood, Isq()); // gets the cell's neighborhood

        for (int i = 0; i < VEGFoptions; i++) { // if there's nearby VEGF, add the distance from original cell to
                                                // VEGFdistance, and add its int position to nearby VEGF
            if (G.VEGF.Get(G.VEGFHood[i]) > VEGFsensitivity){
                VEGFdistance.add(Dist(G.ItoX(G.VEGFHood[i]), G.ItoY(G.VEGFHood[i])));
                nearbyVEGF.add(G.VEGFHood[i]);
                divProb = VEGFdivProb;
            }
        }

        if (G.rng.Double() < divProb){ // if cell chances to divide
            if (VEGFdistance.size() != 0){ // and there is VEGF nearby
                double closest = Collections.min(VEGFdistance); // get the the closest VEGF
                int closestIndex = VEGFdistance.indexOf(closest); // and use it to find the index
                int closestVEGFCoord = nearbyVEGF.get(closestIndex); // use the index to find the int position
                int cellDivLocation = HoodClosestToHep(closestVEGFCoord); // take the int position and find the closest neighborhood division spot
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
    int rad = 6; // radius for VEGF sight
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
        double splitProb = 0.01; // how likely is endothelial cell to branch
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
