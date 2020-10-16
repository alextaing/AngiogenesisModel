package MAP;

import HAL.GridsAndAgents.*;
import HAL.Gui.GifMaker;
import HAL.Gui.OpenGL3DWindow;
import HAL.Interfaces.DoubleToInt;
import HAL.Rand;
import HAL.Util;

import java.util.ArrayList;
import java.util.List;

import static HAL.Util.*;

class MapGel3D extends AgentSQ3D<System3D> {
    int type;
    int lives;
    int t_delay;
    //set up
    double rho=6400.0;
    double init_x;
    double init_y;
    double init_z;
    int kcat = 100; //100-1000
    int km=1;
    int rho_crit=1;
    double diameter=2.0;
    //setting up types of things in the simulation (gel, cells, etc.)
    void InitGel(){ type= System3D.GEL;lives=1;t_delay=0; }
    void InitASAT(){type=System3D.A_SAT_CELL;lives=1;t_delay=0;}
    //void InitASAT(int time){type=System3D.A_SAT_CELL;lives=1;t_delay=time;}
    //void InitPSAT(){type=System3D.P_SAT_CELL;lives=1;t_delay=0;}
   // void InitMYO(){type=System3D.MYOBLAST;lives=1;t_delay=0;}
    //void InitMYO(int time){type=System3D.MYOBLAST;lives=1;t_delay=time;}
    //void InitMAC(){type=System3D.MACROP;lives=7;t_delay=0; }
    //void InitMAC(int life){type=System3D.MACROP;lives=7;t_delay=life; }
    //void InitMYOC(){type=System3D.MYOCYTE;lives=1;t_delay=0;}
    void InitWound(){type=System3D.WOUND;lives=1;t_delay=0;}
    void InitASAT(double x, double y,double z){type=System3D.A_SAT_CELL;lives=1;t_delay=0;init_x=x;init_y=y;init_z=z;}
    void InitASAT(int time,double x, double y,double z){type=System3D.A_SAT_CELL;lives=1;t_delay=time;init_x=x;init_y=y;init_z=z;}
    void InitPSAT(double x, double y,double z){type=System3D.P_SAT_CELL;lives=1;t_delay=0;init_x=x;init_y=y;init_z=z;}
    void InitMYO(double x, double y,double z){type=System3D.MYOBLAST;lives=1;t_delay=0;init_x=x;init_y=y;init_z=z;}
    void InitMYO(int time,double x, double y,double z){type=System3D.MYOBLAST;lives=1;t_delay=time;init_x=x;init_y=y;init_z=z;}
    void InitMAC(double x, double y,double z){type=System3D.MACROP;lives=7;t_delay=0;init_x=x;init_y=y;init_z=z; }
    void InitMAC(int life,double x, double y,double z){type=System3D.MACROP;lives=life;t_delay=life; init_x=x;init_y=y;init_z=z;}
    void InitMYOC(double x, double y,double z){type=System3D.MYOCYTE;lives=1;t_delay=0;init_x=x;init_y=y;init_z=z;}



    //still could get wrap around with migration/proliferation but i am not sure if anything can be done
    //also an issue with getting enough HGF so that satellite cells are active
    public void StepCell() {
        //myoblasts
        if (type == System3D.MYOBLAST) {
            G.depth_myo.add(Math.sqrt(Math.pow((Xsq()-init_x),2)+Math.pow((Ysq()-init_y),2)+Math.pow((Zsq()-init_z),2)));
            //calc proliferation prob
            double pro_prob=1.0/(10.0-(G.HGF.Get(Isq())+0.5*G.IL10.Get(Isq())));
            if (pro_prob>1.0/5.0){pro_prob=1.0/5.0;}
            //calc migration prob
            double mig_prob=1.0/(5.0-G.HGF.Get(Isq()));
            if (mig_prob>2.0/5.0){pro_prob=2.0/5.0;}
            //calc differentiation prob
            double diff_prob=1.0/(100.0-(G.IL10.Get(Isq())-G.HGF.Get(Isq())));
            if (diff_prob>1.0/50.0){diff_prob=1.0/50.0;}
            //migrate
                if (G.rn.Double() < mig_prob) {
                    //cell migrates toward greater HGF conc and take up HGF and IL10 and secrete MMP and ECM(?)
                    int options = this.MapHood(G.migHood);
                    if (options > 0) {
                        int max_inc = 0;
                        for (int i = 0; i < options; i++) {
                            if ((G.HGF.Get(G.migHood[i]) > G.HGF.Get(G.migHood[max_inc]))) {//trying to prevent wrap around but now this limits the number of directions it can move in
                                //if (G.migHood[i] - this.Isq() >= -1300 && G.migHood[i] - this.Isq() <= 1300) {
                                    max_inc = i;
                                //}
                            }
                        }
                        if (G.PopAt(G.migHood[max_inc]) < 1) {
                            Dispose();
                            G.NewAgentSQ(G.migHood[max_inc]).InitMYO(this.t_delay+1,this.init_x,this.init_y,this.init_z);
                            G.HGF.Mul(Isq(),-.1);
                            G.HGF.Update();
                            G.IL10.Mul(Isq(),-.1);
                            G.IL10.Update();
                            G.MMP.Add(Isq(),0.1);//not sure about this, is in decision tree but not 2d code
                            G.MMP.Update();
                        }
                    }
                    return;
                }
            //proliferate and take up HGF and IL10
            if (G.PopAt(Xsq(), Ysq(), Zsq()) < 2 && G.rn.Double() < (pro_prob) && this.t_delay>=40){
                int options=MapEmptyHood(G.divHood);
                if (options>2) {
                    int spot = G.rn.Int(options);
                    if (G.PopAt(G.migHood[spot]) < 1) {
                        G.NewAgentSQ(G.divHood[spot]).InitMYO(0,this.init_x,this.init_y,this.init_z);
                        G.countMYOB++;
                        G.HGF.Mul(Isq(), -.1);
                        G.HGF.Update();
                        G.IL10.Mul(Isq(), -.1);
                        G.IL10.Update();
                        this.t_delay = 0;
                    }
                }
                return;
            }
            //differentiate and take up HGF and IL10
            if(G.rn.Double() < diff_prob && this.t_delay>=20) {
                G.final_depth_myo.add(Math.sqrt(Math.pow((Xsq()-init_x),2)+Math.pow((Ysq()-init_y),2)+Math.pow((Zsq()-init_z),2)));
                this.InitMYOC(this.init_x,this.init_y,this.init_z);
                G.HGF.Mul(Isq(),-.1);
                G.HGF.Update();
                G.IL10.Mul(Isq(),-.1);
                G.IL10.Update();
                this.t_delay=0;
                return;
            }
            //just secrete MMP and ECM(?)
            else{
                G.MMP.Add(Isq(),0.1);
                G.MMP.Update();
                this.t_delay++;
                return;
            }
        }
        //satellite cells

        //activate passive cells
        if (type == System3D.P_SAT_CELL) {
            G.depth_sat.add(Math.sqrt(Math.pow((Xsq()-init_x),2)+Math.pow((Ysq()-init_y),2)+Math.pow((Zsq()-init_z),2)));
            if (G.HGF.Get(Isq())>G.ACT_HGF_CONC){
                this.InitASAT(this.init_x,this.init_y,this.init_z);
                G.HGF.Mul(Isq(),-0.5);
                G.HGF.Update();
            }
            return;
        }
        //active satellite cells
        if (type == System3D.A_SAT_CELL) {
            G.depth_sat.add(Math.sqrt(Math.pow((Xsq()-init_x),2)+Math.pow((Ysq()-init_y),2)+Math.pow((Zsq()-init_z),2)));
            //check if HGF is in correct range, otherwise don't do anything
            if (G.HGF.Get(Isq())>0 && G.HGF.Get(Isq())<10) {
                //calc proliferation prob
                double pro_prob=1.0/(30.0-(G.HGF.Get(Isq())-G.IL10.Get(Isq())));
                if (pro_prob>1.0/20.0){pro_prob=1.0/20.0;}
                //calc mig prob
                double mig_prob=1.0/(5.0-G.HGF.Get(Isq()));
                if (mig_prob>2.0/5.0){pro_prob=2.0/5.0;}
                //calc diff prob
                double diff_prob=1.0/(20.0-(G.IL10.Get(Isq())-G.HGF.Get(Isq())));
                if (diff_prob>1.0/10.0){diff_prob=1.0/10.0;}
                //migrate
                if (G.rn.Double() < mig_prob) {
                    //cell migrates toward greater HGF conc, takes up HGF and IL10, and secretes HGF
                    int options = this.MapHood(G.migHood);
                    if (options > 0) {
                        int max_inc = 0;
                        for (int i = 0; i < options; i++) {
                            if ((G.HGF.Get(G.migHood[i]) > G.HGF.Get(G.migHood[max_inc]))) {//trying to prevent wrap around but now this limits the number of directions it can move in
                                    max_inc = i;
                            }
                        }
                        if (G.PopAt(G.migHood[max_inc]) < 1) {
                            Dispose();
                            G.NewAgentSQ(G.migHood[max_inc]).InitASAT(this.t_delay+1,this.init_x,this.init_y,this.init_z);
                            G.HGF.Mul(max_inc,-.1);
                            G.HGF.Update();
                            G.IL10.Mul(max_inc,-.1);
                            G.IL10.Update();
                            G.HGF.Add(max_inc,G.rn.Double());
                            G.HGF.Update();
                        }
                    }
                    return;
                }
                //proliferate
                if (G.PopAt(Xsq(), Ysq(), Zsq()) < 2 && G.rn.Double() < pro_prob && this.t_delay>=40) {
                    int options = MapEmptyHood(G.divHood);
                    if (options > 0) {
                        int spot = G.rn.Int(options);
                        if (options > 0 && G.PopAt(G.migHood[spot]) < 1) {
                            G.NewAgentSQ(G.divHood[spot]).InitASAT(0, this.init_x, this.init_y, this.init_z);//this makes new cell in a random open position in the hood around the cell, also applies Init to new cell
                            G.countSAT++;
                            G.HGF.Mul(Isq(), -.1);
                            G.HGF.Update();
                            G.IL10.Mul(Isq(), -.1);
                            G.IL10.Update();
                            this.t_delay = 0;
                        }
                        return;
                    }
                }
                //differentiate into a myoblast and take up HGF and IL10
                if(G.rn.Double() < diff_prob && G.countSAT-1>G.critSAT && this.t_delay>=20) {
                    G.final_depth_sat.add(Math.sqrt(Math.pow((Xsq()-init_x),2)+Math.pow((Ysq()-init_y),2)+Math.pow((Zsq()-init_z),2)));
                    this.InitMYO(this.init_x,this.init_y,this.init_z);
                    G.countSAT--;
                    if (G.countSAT<0){G.countSAT=0;}
                    G.countMYOB++;
                    this.t_delay=0;
                    G.HGF.Mul(Isq(),-.1);
                    G.HGF.Update();
                    G.IL10.Mul(Isq(),-.1);
                    G.IL10.Update();
                    return;
                }
                //just secrete HGF
                else{
                    G.HGF.Add(Isq(),G.rn.Double());
                    G.HGF.Update();
                    this.t_delay++;
                    return;
                }
            }
            else{
                this.InitPSAT(this.init_x,this.init_y,this.init_z);
                return;
            }
        }

        //macrophages
        if (type == System3D.MACROP){
            G.depth_mac.add(Math.sqrt(Math.pow((Xsq()-init_x),2)+Math.pow((Ysq()-init_y),2)+Math.pow((Zsq()-init_z),2)));
            //secrete MMP,HGF and IL10
            G.HGF.Add(Isq(),1);
            G.HGF.Diffusion(G.DIFF_RATE);
            G.HGF.Update();
            G.IL10.Add(Isq(),.5);
            G.IL10.Diffusion(G.DIFF_RATE);
            G.IL10.Update();
            G.MMP.Add(Isq(),.5);
            G.MMP.Diffusion(G.DIFF_RATE);
            G.MMP.Update();
            this.t_delay++;
            //proliferate if open space
            if (G.PopAt(Xsq(), Ysq(), Zsq()) < 4 && this.t_delay>=120) {
                int options = MapEmptyHood(G.divHood);
                if (options > 3) {
                    int spot = G.rn.Int(options);
                    if (G.PopAt(G.migHood[spot]) < 1) {//if more than 3 empty spots
                        G.NewAgentSQ(G.divHood[spot]).InitMAC(this.t_delay+1,this.init_x,this.init_y,this.init_z);
                        G.countMACR++;
                        this.lives--;
                        this.t_delay = 0;
                    }
                }
            }
            //die
            if (this.lives==0) {
                //cell dies, delete agent that was representing cell
                G.countMACR--;
                Dispose();
                return;
            }
            //migrate toward not itself with some randomness (from wound into gel)
            if (this.lives>0 && t_delay>=25) {
                //cell migrates toward not itself
                int options = this.MapHood(G.migHood);
                if (options > 0) {
                    int max_inc = 0;
                    int num_places=1;
                    List<Integer> poss = new ArrayList<Integer>();
                    poss.add(G.migHood[max_inc]);
                    for (int i = 0; i < options; i++) {
                        if ((G.notmacr.Get(G.migHood[i]) > G.notmacr.Get(G.migHood[max_inc]))) {
                            max_inc = i;
                            num_places++;
                            poss.add(G.migHood[max_inc]);
                        }
                    }
                    int spot =G.rn.Int(num_places);
                    if (G.PopAt(G.migHood[spot]) < 1) {
                        Dispose();
                        G.NewAgentSQ(poss.get(spot)).InitMAC(this.lives,this.init_x,this.init_y,this.init_z);
                        //add deposit ECM?
                    }
                }
                return;
            }
        }
        if (type== System3D.GEL){
            int options = this.MapHood(G.migHood);
            if (options > 0) {
                double MMP_tot=0.0;
                for (int i = 0; i < options; i++) {
                    MMP_tot+=G.MMP.Get(G.migHood[i]);
                }
                if (MMP_tot>2.5) { //idk what value to do for this
                    this.rho = (rho - (kcat * MMP_tot * (rho / (rho + km))));
                }
            }
            if (rho <= rho_crit) {
                if (this.diameter-.2<0) {
                    this.diameter = 0;
                    for (int i = 0; i < options; i++) {
                        G.MMP.Mul(Isq(), -0.15);
                    }
                }
                else{
                    this.diameter=this.diameter-.2;
                    for (int i = 0; i < options; i++) {
                        G.MMP.Mul(Isq(), -0.15);
                    }
                }
            }
        }
    }
}

public class System3D extends AgentGrid3D<MapGel3D> {
    //set up gel
    final static int GEL_COLOR =RGB256(0,153,153);//light blue
    final static int GEL=0;
    Rand rn=new Rand();
    //set up cells
    static int countSAT=0;
    static int countMYOB=0;
    static int countMACR=0;
    static int critSAT;
    static List<Double> depth_sat = new ArrayList<Double>();
    static List<Double> final_depth_sat = new ArrayList<Double>();
    static List<Double> depth_myo = new ArrayList<Double>();
    static List<Double> final_depth_myo = new ArrayList<Double>();
    static List<Double> depth_mac = new ArrayList<Double>();
    //set up proliferation hood
    int[] divHood = Util.VonNeumannHood3D(false);
    //set up migration hood in response to gradient
    int[] migHood = Util.SphereHood(false,2);
    //set up cell types
    final static int A_SAT_CELL=2;
    final static int A_SAT_COLOR=RGB256(204,0,0);//red
    final static int P_SAT_CELL=3;
    final static int P_SAT_COLOR=RGB256(255,128,0);//orange
    final static int MYOBLAST=4;
    final static int MYOB_COLOR=RGB256(102,0,102);//dark purple
    final static int MACROP=5;
    final static int MACR_COLOR=RGB256(255,255,51);//yellow
    final static int MYOCYTE=6;
    final static int MYOC_COLOR=RGB256(204,153,255);//light purple

    //set up wound interface
    final static int WOUND=7;
    final static int WOUND_Color=RGB(0,153,0);


    //max probabilities and degredation/diffusion rates
    double DIFF_RATE=0.5/6;//maximum stable diffusion rate
    double HGF_DEG=-(1-0.983);
    double IL10_DEG=-(1-0.962);
    double MMP_DEG=-(1-0.914);
    double ACT_HGF_CONC=.25; //2.5 ng/ml


    int time_delay=0;

    //Setting up gradients
    PDEGrid3D notmacr;
    PDEGrid3D HGF;
    PDEGrid3D IL10;
    PDEGrid3D MMP;

    public System3D(int x, int y, int z) {
        super(x, y, z, MapGel3D.class);
        notmacr=new PDEGrid3D(x,y,z);
        HGF=new PDEGrid3D(x,y,z);
        IL10=new PDEGrid3D(x,y,z);
        MMP=new PDEGrid3D(x,y,z);
    }

    public void DiffStep(){
        //Set up Not-macrophage gradient
        notmacr.Set(0,10,80,25.0);
        notmacr.Set(20,10,80,25.0);
        notmacr.Set(40,10,80,25.0);
        notmacr.Set(55,10,80,25.0);
        notmacr.Set(75,10,80,25.0);
        notmacr.Diffusion(DIFF_RATE);
        notmacr.Update();
        //Set up HGF gradient
        int[] num= new int[1];
        for (int i = 0; i < 5; i++) {
            rn.RandomIS(num,65,80);
            HGF.Set(rn.Int(79),rn.Int(20),num[0],10.0);//6.25 used in literature as supplement and has similar effects as control without HGF
        }
        HGF.Diffusion(DIFF_RATE);
        HGF.Update();
        //Set up IL10 gradient (is there any in the gel to start?)
        for (int i = 0; i < 5; i++) {
            rn.RandomIS(num,65,80);
            IL10.Set(rn.Int(79),rn.Int(20),num[0],10.0);
        }
        IL10.Diffusion(DIFF_RATE);
        IL10.Update();
    }


    public int GenGel(double GelSpacingMin){
        //create a Grid to store the locations that are too close for placing another gel particle
        Grid3Ddouble openSpots=new Grid3Ddouble(xDim,yDim,zDim);
        //create a neighborhood that defines all indices that are too close
        int[]vesselSpacingHood=SphereHood(false, GelSpacingMin);
        int[]indicesToTry=GenIndicesArray(openSpots.length);
        rn.Shuffle(indicesToTry);
        int partCt=0;
        //creates particle at position x,y,z if it is far enough from another particle (so don't overlap)
        for (int i : indicesToTry) {
            if(openSpots.Get(i)==0){
                int x=openSpots.ItoX(i);
                int y=openSpots.ItoY(i);
                int z=openSpots.ItoZ(i);
                GenGel(x,y,z);
                partCt++;
                int nSpots=openSpots.MapHood(vesselSpacingHood,x,y,z);
                for (int j = 0; j < nSpots; j++) {
                    //mark spot as too close for another vessel
                    openSpots.Set(vesselSpacingHood[j],-1);
                }
            }
        }
        return partCt;
    }


    public void GenGel(int x,int y,int z) {
        //checks to see if param locations are within the dimensions of the gel, makes a particle if they are
        if (y<=yDim && x<=xDim && z<=zDim){
            MapGel3D occupant = GetAgent(x,y,z);
            NewAgentSQ(x, y, z).InitGel();//makes gel at open location that is not too close to other particles
        }
    }

    public void GenCells(int initPopSize, String type) {
        int numCells=0;
        if (type.equals("Macrophage")) {
            countMACR+=initPopSize;
            while (numCells<= initPopSize) {
                int first = rn.Int(79);
                int sec=rn.Int(20);
                if (PopAt(first,sec,79)<1){
                    NewAgentSQ(first,sec, 79).InitMAC(first,sec,79);
                    numCells++;
                }
            }
        }
        if (type.equals("Myoblast")) {
            countMYOB+=initPopSize;
            while (numCells<= initPopSize) {
                int first = rn.Int(79);
                int sec=rn.Int(20);
                if (PopAt(first,sec,79)<1){
                    NewAgentSQ(first,sec, 79).InitMYO(first,sec,79);
                    numCells++;
                }
            }
        }
        if (type.equals("ASat")) {
            countSAT+=initPopSize;
            critSAT+=initPopSize;
            while (numCells<= initPopSize) {
                int first = rn.Int(79);
                int sec=rn.Int(20);
                if (PopAt(first,sec,79)<1){
                    NewAgentSQ(first,sec, 79).InitASAT(first,sec,79);
                    numCells++;
                }
            }
        }
        if (type.equals("PSat")) {
            countSAT+=initPopSize;
            critSAT+=initPopSize;
            while (numCells<= initPopSize) {
                int first = rn.Int(79);
                int sec=rn.Int(20);
                if (PopAt(first,sec,79)<1){
                    NewAgentSQ(first,sec, 79).InitPSAT(first,sec,79);
                    numCells++;
                }
            }
        }
    }

    public void GenWound(int x,int y,int z){
        NewAgentSQ(x, y, z).InitWound();
    }

    public void StepCells (){
        for(MapGel3D cell:this){
            cell.StepCell();
        }
        time_delay++;
        if (time_delay==50){
            //GF and enzyme degredation
            HGF.MulAll(HGF_DEG);
            IL10.MulAll(IL10_DEG);
            MMP.MulAll(MMP_DEG);
            HGF.Update();
            IL10.Update();
            MMP.Update();
            time_delay=0;
        }
    }

    //this draws whatever is in simulation in window (dimensions/outline of gel, gel , cells,gradients,etc.)
    //can draw multiple gradients at once, but cannot distinguish between them at the moment
    public void DrawCells(OpenGL3DWindow vis, DoubleToInt DrawConcs){
        vis.ClearBox(Util.BLACK, WHITE);
        for (MapGel3D cellOrVessel : this) {
            switch (cellOrVessel.type) {
                case GEL:
                    vis.Circle(cellOrVessel.Xpt(), cellOrVessel.Ypt(), cellOrVessel.Zpt(), cellOrVessel.diameter, GEL_COLOR);//sets radius for gel
                    break;
                case A_SAT_CELL:
                    vis.Circle(cellOrVessel.Xpt(),cellOrVessel.Ypt(),cellOrVessel.Zpt(),1,A_SAT_COLOR);
                    break;
                case P_SAT_CELL:
                    vis.Circle(cellOrVessel.Xpt(),cellOrVessel.Ypt(),cellOrVessel.Zpt(),1,P_SAT_COLOR);
                    break;
                case MYOBLAST:
                    vis.Circle(cellOrVessel.Xpt(),cellOrVessel.Ypt(),cellOrVessel.Zpt(),1,MYOB_COLOR);
                    break;
                case MYOCYTE:
                    vis.Circle(cellOrVessel.Xpt(),cellOrVessel.Ypt(),cellOrVessel.Zpt(),1,MYOC_COLOR);
                    break;
                case MACROP:
                    vis.Circle(cellOrVessel.Xpt(),cellOrVessel.Ypt(),cellOrVessel.Zpt(),1,MACR_COLOR);
                    break;
                case WOUND:
                    vis.RectangleXY(0,0,80,20,80,WOUND_Color);
            }
        }
        if(DrawConcs!=null){
            for (int x = 0; x < HGF.xDim; x++) {
                for (int z = 0; z < HGF.zDim; z++) {
                    double HGFSum=0;
                    //add column to avgConcs
                    for (int y = 0; y < HGF.yDim; y++) {
                        HGFSum+=HGF.Get(x,y,z);
                    }
                    HGFSum/=HGF.yDim;
                    vis.SetPixXZ(x,z,DrawConcs.DoubleToInt(HGFSum));
                }
            }
            /*for (int x = 0; x < IL10.xDim; x++) {
                    for (int z = 0; z < IL10.zDim; z++) {
                        double IL10Sum=0;
                        //add column to avgConcs
                        for (int y = 0; y < IL10.yDim; y++) {
                            IL10Sum+=IL10.Get(x,y,z);
                        }
                        IL10Sum/=IL10.yDim;
                        vis.SetPixXZ(x,z,DrawConcs.DoubleToInt(IL10Sum));
                    }
                }*/
            /*for (int x = 0; x < MMP.xDim; x++) {
                    for (int z = 0; z < MMP.zDim; z++) {
                        double MMPSum=0;
                        //add column to avgConcs
                        for (int y = 0; y < MMP.yDim; y++) {
                            MMPSum+=MMP.Get(x,y,z);
                        }
                        MMPSum/=MMP.yDim;
                        vis.SetPixXZ(x,z,DrawConcs.DoubleToInt(MMPSum));
                    }
                }*/
            /*for (int x = 0; x < notmacr.xDim; x++) {
                for (int z = 0; z < notmacr.zDim; z++) {
                    double notmacrSum=0;
                    //add column to avgConcs
                    for (int y = 0; y < notmacr.yDim; y++) {
                        notmacrSum+=notmacr.Get(x,y,z);
                    }
                    notmacrSum/=notmacr.yDim;
                    vis.SetPixXZ(x,z,DrawConcs.DoubleToInt(notmacrSum));
                }
            }*/
        }
        vis.Update();
    }

    public static void main(String[] args) {
        int x = 80, y = 80, z = 20;
        System3D ex = new System3D(x, z, y);
        ex.GenWound(75,80,0);
        ex.GenGel(4);//this affects how far apart the circles are (currently set to 2*radius of gel particle)
        for (int i = 0; i < 1000; i++) {
            ex.DiffStep();
        }
        double min=0;
        ex.GenCells(40,"Macrophage");
        ex.GenCells(20,"Myoblast");
        ex.GenCells(15,"PSat");
        ex.GenCells(10,"ASat");
        critSAT= (int) (critSAT*.1);
        OpenGL3DWindow vis=new OpenGL3DWindow("GelVis", 1000,1000,x,z,y);//sets window size
        int j=0;
        vis.ToGIF(j+"MAP GIF.gif");
        while (!vis.IsClosed()){
            j++;
            depth_sat.clear();
            depth_myo.clear();
            depth_mac.clear();
            min=min+15.0;
            ex.StepCells();
            ex.DrawCells(vis,Util::HeatMapRGB);
            ex.CleanAgents();//Equivalent to calling CleanAgents, ShuffleAgents, and IncTick grid functions
            ex.ShuffleAgents(ex.rn);
            //map_model.AddFrame(vis);
            vis.TickPause(10);
            vis.ToGIF(j+"MAP GIF.gif");

        }
        vis.Close();
        //map_model.Close();
        System.out.println("Days: "+(min/60.0)/24.0);
        double sat_avg=0.0;
        for (int i = 0; i < depth_sat.size() ; i++) {
            sat_avg+=depth_sat.get(i);
        }
        for (int i = 0; i < final_depth_sat.size() ; i++) {
            sat_avg+=final_depth_sat.get(i);
        }
        sat_avg=sat_avg/(depth_sat.size()+final_depth_sat.size());
        double myo_avg=0.0;
        double myo_max=0.0;
        for (int i = 0; i < depth_myo.size() ; i++) {
            myo_avg+=depth_myo.get(i);
            if (depth_myo.get(i)>myo_max){ myo_max=depth_myo.get(i);}
        }
        for (int i = 0; i < final_depth_myo.size() ; i++) {
            myo_avg+=final_depth_myo.get(i);
            if (final_depth_myo.get(i)>myo_max){ myo_max=final_depth_myo.get(i);}
        }
        myo_avg=myo_avg/(depth_myo.size()+final_depth_myo.size());

        double mac_avg=0.0;
        for (int i = 0; i < depth_mac.size() ; i++) {
            mac_avg+=depth_mac.get(i);
        }
        mac_avg=mac_avg/countMACR;
        System.out.println("Satellite Cells: "+countSAT);
        System.out.println("Myoblasts:"+countMYOB);
        System.out.println("Macrophages:"+ countMACR);
        System.out.println("Satellite Cells Mean Depth: "+sat_avg);
        System.out.println("Myoblasts Mean Depth:"+myo_avg);
        System.out.println("Myoblasts Max Depth:"+myo_max);
        System.out.println("Macrophages Mean Depth:"+ mac_avg);
    }
}

