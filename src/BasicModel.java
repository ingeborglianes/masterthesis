import gurobi.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BasicModel {
    private int [][] OperationsForVessel;
    private int [][] TimeWindowsForOperations;
    private int [][][] Edges;
    private int [][][][] SailingTimes;
    private int [][][] TimeVesselUseOnOperation;
    private int [][][] TimeVesselUseOnOperationGurobi;
    private int [] EarliestStartingTimeForVessel;
    private int [] SailingCostForVessel;
    private int [][][] OperationGain;
    private int [][] Precedence;
    private int [][] Simultaneous;
    private int [] BigTasks;
    private Map<Integer, List<Integer>> ConsolidatedTasks;
    private int nVessels;
    private int nOperations;
    private int nTimePeriods;
    private int[] endNodes;
    private int[] startNodes;
    private double[] endPenaltyforVessel;
    private String logName;

    public BasicModel(int [][] OperationsForVessel, int [][] TimeWindowsForOperations, int [][][] Edges, int [][][][] SailingTimes,
                      int [][][] TimeVesselUseOnOperation, int[][][] TimeVesselUseOnOperationGurobi,int [] EarliestStartingTimeForVessel,
                      int [] SailingCostForVessel, int [][][] OperationGain, int [][] Precedence, int [][] Simultaneous,
                      int [] BigTasks, Map<Integer, List<Integer>> ConsolidatedTasks, int[] endNodes, int[] startNodes,
                      double[] endPenaltyforVessel, String testInstance, String logNameAddition){
        this.OperationsForVessel=OperationsForVessel;
        this.TimeWindowsForOperations=TimeWindowsForOperations;
        this.Edges=Edges;
        this.SailingTimes=SailingTimes;
        this.TimeVesselUseOnOperation=TimeVesselUseOnOperation;
        this.TimeVesselUseOnOperationGurobi = TimeVesselUseOnOperationGurobi;
        this.EarliestStartingTimeForVessel=EarliestStartingTimeForVessel;
        this.SailingCostForVessel=SailingCostForVessel;
        this.OperationGain=OperationGain;
        this.Precedence=Precedence;
        this.Simultaneous=Simultaneous;
        this.BigTasks=BigTasks;
        this.ConsolidatedTasks=ConsolidatedTasks;
        this.nVessels=this.OperationsForVessel.length;
        this.nOperations=TimeWindowsForOperations.length;
        this.nTimePeriods=TimeWindowsForOperations[0].length;
        this.endNodes=endNodes;
        this.startNodes=startNodes;
        this.endPenaltyforVessel=endPenaltyforVessel;
        //System.out.println(nOperations-startNodes.length*2);
        //System.out.println(Arrays.toString(this.endNodes));
        //System.out.println(Arrays.toString(this.startNodes));
        logName = testInstance.substring(0,testInstance.length()-4)+logNameAddition;
    }


    public static Boolean containsElement(int element, int[] list)   {
        Boolean bol = false;
        for (Integer e: list)     {
            if(element==e){
                bol=true;
            }
        }
        return bol;
    }

    public List<String> runModel(String filepath) {
        List<String> routing=new ArrayList<>();
        List<Integer> vesselsNotUsed=new ArrayList<>();
        List<Integer> operationsNotPerformed=new ArrayList<>();
        String[][] vesselPath = new String[nVessels][nTimePeriods*3];

        try {
            // Model
            GRBEnv env = new GRBEnv("logfiles/"+logName+"_log.log");
            GRBModel model = new GRBModel(env);
            //model.getEnv().set(GRB.StringParam.LogFile,1);
            model.set(GRB.StringAttr.ModelName, filepath);
            model.set(GRB.DoubleParam.TimeLimit, 18000);
            //model.get(GRB.params.LogFile=filepath+"_LOG");

            // Sailing decision variable, x[vijt]=1 if vessel v sails from i to j in time period t
            GRBVar[][][][] x = new GRBVar[nVessels][3*nTimePeriods][nOperations][nOperations];
            for (int v = 0; v < nVessels; ++v) {
                for (int t = EarliestStartingTimeForVessel[v]; t < nTimePeriods; ++t) {
                    for (int i = 0; i < nOperations-nVessels; ++i) {
                        for (int j = 0; j < nOperations; ++j) {
                            if(Edges[v][i][j] == 1) {
                                x[v][t][i][j] = model.addVar(0, 1, -SailingCostForVessel[v] * SailingTimes[v][t][i][j],
                                        GRB.BINARY, "x" + v + "." + t + "." + i + "." + j);
                                        //System.out.println("lagt til" + "x" + v + "." + t + "." + i + "." + j);
                                if (j == nOperations - v - 1 && i >= nVessels && t == nTimePeriods - 1) {
                                    for (int k = t + 1; k < t+1 + TimeVesselUseOnOperation[v][i - nVessels][t] + 1; k++) {
                                        x[v][k][i][j] = model.addVar(0, 1, -SailingCostForVessel[v] * SailingTimes[v][t][i][j],
                                                GRB.BINARY, "x" + v + "." + k + "." + i + "." + j);
                                        //System.out.println("lagt til" + "x" + v + "." + k + "." + i + "." + j);
                                    }
                                }
                            }
                            //System.out.println("x" + v + "." + t + "." + i + "." + j);
                        }
                    }
                }
            }

            // Task decision variable: y[vit]=1 if vessel v perform task i in time period t
            GRBVar[][][] y = new GRBVar[nVessels][nOperations][nTimePeriods];
            for (int v = 0; v < nVessels; ++v) {
                for (int i = nVessels; i < nOperations - nVessels; ++i) {
                    for (int t = EarliestStartingTimeForVessel[v]; t < nTimePeriods; ++t) {
                        if (containsElement( t+ 1, TimeWindowsForOperations[i]) && OperationsForVessel[v][i-nVessels]!=0) {
                            y[v][i][t] = model.addVar(0, 1, OperationGain[v][i-nVessels][t], GRB.BINARY,
                                    "y" + v + "." + i + "." + t);
                        }
                    }
                }
            }

            //Waiting decision variable: w[vit]=1 if vessel v wait in task i in time period t
            GRBVar[][][] w = new GRBVar[nVessels][nOperations][nTimePeriods];
            for (int v = 0; v < nVessels; ++v) {
                for (int i = nVessels; i < nOperations-nVessels; ++i) {
                    for (int t = EarliestStartingTimeForVessel[v]; t < nTimePeriods; ++t) {
                        if(OperationsForVessel[v][i-nVessels]!=0) {
                            w[v][i][t] = model.addVar(0, 1, 0, GRB.BINARY,
                                    "w" + v + "." + i + "." + t);
                        }

                    }
                }
            }

            //z[i]=1 if task i is not performed
            GRBVar[] z = new GRBVar[nOperations];
            for (int i = nVessels; i < nOperations - nVessels; ++i) {
                z[i] = model.addVar(0, 1, 0, GRB.BINARY, "z" + i);
            }


            // The objective is to minimize the total fixed and variable costs
            model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);


            // Undone constraints
            for (int i = nVessels; i < nOperations - nVessels; ++i) {
                GRBLinExpr operation_performed = new GRBLinExpr();
                for (int v = 0; v < nVessels; ++v) {
                    if (containsElement(i + 1, OperationsForVessel[v])) {
                        for (int t : TimeWindowsForOperations[i]) {
                            if (t > EarliestStartingTimeForVessel[v]) {
                                operation_performed.addTerm(1.0, y[v][i][t - 1]);
                            }
                        }
                    }
                }
                operation_performed.addTerm(1, z[i]);
                model.addConstr(operation_performed, GRB.LESS_EQUAL, 1, "Undone" + i);
            }


            // One activity constraint
            for (int v = 0; v < nVessels; ++v) {
                for (int t = EarliestStartingTimeForVessel[v]; t < nTimePeriods; ++t) {
                    GRBLinExpr sail = new GRBLinExpr();
                    for (int i = 0; i < nOperations; ++i) {
                        for (int j = 0; j < nOperations; ++j) {
                            if (Edges[v][i][j] == 1 && SailingTimes[v][t][i][j]!=0) {
                                sail.addTerm(1, x[v][t][i][j]);
                            }
                        }
                        if (containsElement(i + 1, OperationsForVessel[v])){
                            if(containsElement(t + 1, TimeWindowsForOperations[i])) {
                                sail.addTerm(1, y[v][i][t]);
                            }
                            sail.addTerm(1, w[v][i][t]);
                        }
                    }
                    model.addConstr(sail, GRB.LESS_EQUAL, 1, "Oneactivity" + v + t);
                }
            }

            // New following operation constraint, updated to allow sailing to the dummy end node after the planning horizon has ended.
            for (int v = 0; v < nVessels; ++v) {
                for (int i = nVessels; i < nOperations; ++i) {
                    if (containsElement(i + 1, OperationsForVessel[v])) {
                        ArrayList<Integer> control = new ArrayList<Integer>();
                        for (int t = EarliestStartingTimeForVessel[v]; t < nTimePeriods + TimeVesselUseOnOperation[v][i-nVessels][Math.min(t,nTimePeriods-1)]; ++t) {
                            GRBLinExpr operation = new GRBLinExpr();
                            GRBLinExpr next_sail = new GRBLinExpr();
                            if (containsElement((t-TimeVesselUseOnOperationGurobi[v][i - nVessels][Math.min(nTimePeriods-1,t)]+1),
                                    TimeWindowsForOperations[i]) & (t-TimeVesselUseOnOperationGurobi[v][i - nVessels][Math.min(nTimePeriods-1,t)])>=EarliestStartingTimeForVessel[v]
                                    && !control.contains(t-TimeVesselUseOnOperationGurobi[v][i - nVessels][Math.min(nTimePeriods-1,t)])) {
                                operation.addTerm(1, y[v][i][t-TimeVesselUseOnOperationGurobi[v][i - nVessels][Math.min(nTimePeriods-1,t)]]);
                                control.add(t-TimeVesselUseOnOperationGurobi[v][i - nVessels][Math.min(nTimePeriods-1,t)]);
                                //System.out.println("add y with v= " + (v+1) + " operation= " + (i+1) + " timeperiod= " +
                                  //      (t-TimeVesselUseOnOperationGurobi[v][i - nVessels][Math.min(nTimePeriods-1,t)]+1));
                            }
                            for (int j = nVessels; j < nOperations; ++j) {
                                if (Edges[v][i][j] == 1) {
                                    if(t<nTimePeriods) {
                                        next_sail.addTerm(1, x[v][t][i][j]);
                                    }
                                    else if(i >= nVessels && t >= nTimePeriods - 1 && j == nOperations-v-1){
                                        //for (int k = t + 1; k < t + TimeVesselUseOnOperation[v][i - nVessels][t] + 1; k++) {
                                            next_sail.addTerm(1, x[v][t][i][j]);
                                            //System.out.println("add x with v= " + (v+1) + " sail from operation= " + (i+1) + " sail to operation=  "
                                            //        +(j+1) + " timeperiod= " + (k+1));
                                        //}
                                    }
                                    //System.out.println("add x with v= " + (v+1) + " sail from operation= " + (i+1) + " sail to operation=  "
                                    //        +(j+1) + " timeperiod= " + (t+1));
                                }
                            }
                            model.addConstr(operation, GRB.EQUAL, next_sail, "followingoperation" + v + i + t);
                        }
                    }
                }
            }


/*
            // New following operation constraint, updated to allow sailing to the dummy end node after the planning horizon has ended.
            for (int v = 0; v < nVessels; ++v) {
                for (int i = nVessels; i < nOperations; ++i) {
                    if (containsElement(i + 1, OperationsForVessel[v])) {
                        ArrayList<Integer> control = new ArrayList<Integer>();
                        for (int t = EarliestStartingTimeForVessel[v]; t < nTimePeriods; ++t) {
                            GRBLinExpr operation = new GRBLinExpr();
                            GRBLinExpr next_sail = new GRBLinExpr();
                            if (containsElement((t-TimeVesselUseOnOperation[v][i - nVessels][t]+1),
                                    TimeWindowsForOperations[i]) & (t-TimeVesselUseOnOperation[v][i - nVessels][t])>EarliestStartingTimeForVessel[v]
                                    && !control.contains(t-TimeVesselUseOnOperation[v][i - nVessels][t])) {
                                operation.addTerm(1, y[v][i][t-TimeVesselUseOnOperation[v][i - nVessels][t]]);
                                control.add(t-TimeVesselUseOnOperation[v][i - nVessels][t]);
                                System.out.println("add y with v= " + (v+1) + " operation= " + (i+1) + " timeperiod= " +
                                        (t-TimeVesselUseOnOperation[v][i - nVessels][t]+1));
                            }
                            for (int j = nVessels; j < nOperations; ++j) {
                                if (Edges[v][i][j] == 1) {
                                    next_sail.addTerm(1, x[v][t][i][j]);
                                    if(i >= nVessels && t == nTimePeriods - 1 && j == nOperations-v-1){
                                        for (int k = t + 1; k < t + TimeVesselUseOnOperation[v][i - nVessels][t] + 1; k++) {
                                            next_sail.addTerm(1, x[v][k][i][j]);
                                            //System.out.println("add x with v= " + (v+1) + " sail from operation= " + (i+1) + " sail to operation=  "
                                            //        +(j+1) + " timeperiod= " + (k+1));
                                        }
                                    }
                                    //System.out.println("add x with v= " + (v+1) + " sail from operation= " + (i+1) + " sail to operation=  "
                                    //        +(j+1) + " timeperiod= " + (t+1));
                                }
                            }
                            model.addConstr(operation, GRB.EQUAL, next_sail, "followingoperation" + v + i + t);
                        }
                    }
                }
            }


            // New following operation constraint, updated to allow sailing to the dummy end node after the planning horizon has ended.
            for (int v = 0; v < nVessels; ++v) {
                for (int i = nVessels; i < nOperations; ++i) {
                    if (containsElement(i + 1, OperationsForVessel[v])) {
                        ArrayList<Integer> control = new ArrayList<Integer>();
                        for (int t = EarliestStartingTimeForVessel[v]; t < nTimePeriods*3; ++t) {
                            GRBLinExpr operation = new GRBLinExpr();
                            GRBLinExpr next_sail = new GRBLinExpr();
                            if(t<nTimePeriods) {
                                if (containsElement((t + 1), TimeWindowsForOperations[i]) & (t) > EarliestStartingTimeForVessel[v]
                                        && !control.contains(t)) {
                                    operation.addTerm(1, y[v][i][t]);
                                    control.add(t);
                                    System.out.println("add y with v= " + (v + 1) + " operation= " + (i + 1) + " timeperiod= " +
                                            (t + 1));
                                }
                                for (int j = nVessels; j < nOperations; ++j) {
                                    if (Edges[v][i][j] == 1 && t+1 + TimeVesselUseOnOperation[v][i - nVessels][Math.min(t+1, nTimePeriods - 1)] < nTimePeriods) {
                                        next_sail.addTerm(1, x[v][t+1 + TimeVesselUseOnOperation[v][i - nVessels][t]][i][j]);
                                        System.out.println("adding x with v= " + (v + 1) + " sail from operation= " + (i + 1) + " sail to operation=  "
                                                + (j + 1) + " timeperiod= " + (t+1 + TimeVesselUseOnOperation[v][i - nVessels][Math.min(t, nTimePeriods - 1)] + 1));

                                    } else if (Edges[v][i][j] == 1 && i >= nVessels && j == nOperations - v - 1) {
                                        next_sail.addTerm(1, x[v][t+1 + TimeVesselUseOnOperation[v][i - nVessels][Math.min(t, nTimePeriods - 1)]][i][j]);
                                        System.out.println("add x with v= " + (v + 1) + " sail from operation= " + (i + 1) + " sail to operation=  "
                                                + (j + 1) + " timeperiod= " + (t+1 + TimeVesselUseOnOperation[v][i - nVessels][Math.min(t, nTimePeriods - 1)] + 1));
                                    }
                                }
                            }
                            model.addConstr(operation, GRB.EQUAL, next_sail, "followingoperation" + v + i + t);
                        }
                    }
                }
            }*/

            // Following sail constraint
            for (int v = 0; v < nVessels; ++v) {
                for (int i = 0; i < nOperations; ++i) {
                    if (containsElement(i + 1, OperationsForVessel[v])) {
                        ArrayList<Integer> control = new ArrayList<Integer>();
                        for (int t = EarliestStartingTimeForVessel[v]; t < nTimePeriods; ++t) {
                            //System.out.println(t);

                            GRBLinExpr sail = new GRBLinExpr();
                            GRBLinExpr following_sail = new GRBLinExpr();
                            for (int j = 0; j < nOperations; ++j) {
                                if (Edges[v][j][i] == 1 & (t - SailingTimes[v][t][j][i] >= EarliestStartingTimeForVessel[v])) {
                                    sail.addTerm(1, x[v][t - SailingTimes[v][t][j][i]][j][i]);
                                    //System.out.println("add x with v= " + (v+1) + " sail from operation= " + (j+1) + " sail to operation=  "
                                    //   +(i+1) + " timeperiod= " + (t - SailingTimes[j][i]+1));
                                }
                            }
                            if (t > EarliestStartingTimeForVessel[v]) {
                                sail.addTerm(1, w[v][i][t - 1]);
                                //System.out.println("add w with v= " + (v+1) + " operation= " + (i+1) + " timeperiod= " + (t));
                            }
                            if (containsElement(t+1, TimeWindowsForOperations[i]) && containsElement(i + 1, OperationsForVessel[v])) {
                                following_sail.addTerm(1, y[v][i][t]);
                                //System.out.println("add y with v= " + (v+1) + " operation= " + (i+1) + " timeperiod= " + (t+1));
                            }
                            following_sail.addTerm(1, w[v][i][t]);
                            model.addConstr(sail, GRB.EQUAL, following_sail, "followingSail" + v + i + t);
                        }
                    }
                }
            }


            //constraint that ensures that each planning horizon starts at the dummy start nodes
            //TEMPORARILY: Venteleddet av restriksjonen er kommentert ut.
            int count = 0;
            for (int v = 0; v < nVessels; ++v) {
                GRBLinExpr start = new GRBLinExpr();
                GRBLinExpr Time = new GRBLinExpr();
                for(int t=EarliestStartingTimeForVessel[v];t<nTimePeriods;t++) {
                    for (int j = 0; j < nOperations; ++j) {
                        if ((containsElement(j + 1, OperationsForVessel[v])||containsElement(j+1,endNodes)) & Edges[v][count][j] == 1) {
                            start.addTerm(1, x[v][t][count][j]);
                            //System.out.println("add x with v= " + (v+1) + " sail from operation= " + (count+1) + " sail to operation=  "
                            //        +(j+1) + " timeperiod= " + (EarliestStartingTimeForVessel[v]+1));
                        }
                    }
                }
                for (int j = 0; j < nOperations; ++j) {
                    if ((containsElement(j + 1, OperationsForVessel[v])||containsElement(j+1,endNodes)) & Edges[v][count][j] == 1) {
                        Time.addTerm(1, x[v][EarliestStartingTimeForVessel[v]][count][j]);
                        //System.out.println("add x with v= " + (v+1) + " sail from operation= " + (count+1) + " sail to operation=  "
                        //        +(j+1) + " timeperiod= " + (EarliestStartingTimeForVessel[v]+1));
                    }
                }
                model.addConstr(start, GRB.EQUAL, 1, "start"
                        + v);

                model.addConstr(Time, GRB.EQUAL, 1, "startTime"
                        + v);
                count += 1;
            }


            //Constraint that ensures that each planning horizon ends at the dummy end nodes
            int count2=1;
            for (int v=0;v<nVessels;++v) {
                GRBLinExpr end = new GRBLinExpr();
                for (int i = 0; i < nOperations; ++i) {
                    for(int t=EarliestStartingTimeForVessel[v];t<nTimePeriods;++t) {
                        if ((containsElement(i+1,startNodes)|| containsElement(i+1, OperationsForVessel[v])) & Edges[v][i][nOperations - (count2)] == 1) {
                            end.addTerm(1, x[v][t][i][nOperations - (count2)]);
                            if( i >= nVessels && t == nTimePeriods - 1){
                                for (int k = t + 1; k < t + TimeVesselUseOnOperation[v][i - nVessels][t] + 1; k++) {
                                    end.addTerm(1, x[v][k][i][nOperations - count2]);
                                    //System.out.println("add x for vessel "+(v+1)+", in timeperiod "+(k+1)
                                    //        +", from operation "+(i+1)+" to operation"+(nOperations - (count2)+1));
                                    //System.out.println("add tv for vessel "+(v+1)+", with value "+(k)
                                    //        +", from operation "+(i+1)+" to operation"+(nOperations - (count2)+1));
                                }
                            }
                        }
                    }
                }
                model.addConstr(end, GRB.EQUAL, 1, "end" + v);
                count2+=1;
            }



            // Precedence constraint
            for (int i = 0; i < nOperations; ++i) {
                for (int j = 0; j < nOperations; ++j) {
                    if (Precedence[i][j] == 1) {
                        for (int t = 0; t < nTimePeriods; ++t) {
                            if (containsElement(t + 1, TimeWindowsForOperations[j])) {
                                //System.out.println(t);
                                GRBLinExpr first = new GRBLinExpr();
                                GRBLinExpr second = new GRBLinExpr();
                                for (int v = 0; v < nVessels; ++v) {
                                    if (containsElement(i+1, OperationsForVessel[v])) {
                                        for (int tau = 0; tau < t-TimeVesselUseOnOperation[v][j-nVessels][tau]+1; ++tau) {
                                            if (containsElement(tau + 1, TimeWindowsForOperations[i])) {
                                                first.addTerm(1, y[v][i][tau]);
                                                //System.out.println("add y1 with v= " + (v + 1) + " perform operation= " + (i + 1) + " in timeperiod= " + (tau + 1));
                                            }
                                        }
                                    }
                                    if (containsElement(j+1, OperationsForVessel[v])) {
                                        second.addTerm(1, y[v][j][t]);
                                        //System.out.println("add y2 with v= " + (v+1) + " perform operation= " + (j+1) + " in timeperiod= " + (t+1));
                                    }

                                }
                                model.addConstr(first, GRB.GREATER_EQUAL, second, "precedence"+i+j);
                            }
                        }
                    }
                }
            }


            // Simultaneous constraint
            for (int i = nVessels; i < nOperations-nVessels; ++i) {
                for (int j = nVessels; j < nOperations-nVessels; ++j) {
                    if (Simultaneous[i][j] == 1) {
                        for (int t = 0; t < nTimePeriods; ++t) {
                            if (containsElement(t+1, TimeWindowsForOperations[i]) &
                                    containsElement(t+1, TimeWindowsForOperations[j])) {
                                GRBLinExpr operations = new GRBLinExpr();
                                //System.out.println(t);
                                for (int v = 0; v < nVessels; ++v) {
                                    if(t>EarliestStartingTimeForVessel[v]) {
                                        if (containsElement(i + 1, OperationsForVessel[v])) {
                                            operations.addTerm(1, y[v][i][t]);
                                            //System.out.println("add y1 with v= " + (v+1) + " perform operation= " + (i+1) + " in timeperiod= " + (t+1));
                                        }
                                        if (containsElement(j + 1, OperationsForVessel[v])) {
                                            operations.addTerm(-1, y[v][j][t]);
                                            //System.out.println("add y2 with v= " + (v + 1) + " perform operation= " + (j + 1) + " in timeperiod= " + (t + 1));
                                        }
                                    }
                                }
                                model.addConstr(operations, GRB.GREATER_EQUAL, 0, "simultaneous"+i+j);
                            }
                            else{
                                //System.out.println("Simultaneous requirement, but no overlapping time windows. Check preprocessing");
                            }
                        }
                    }
                }
            }


            // Constraint connecting consolidated tasks
            for(int k : BigTasks){
                double nk = ConsolidatedTasks.get(k).size();
                GRBLinExpr left_side = new GRBLinExpr();
                for (int v = 0; v < nVessels; ++v) {
                    for(int i : ConsolidatedTasks.get(k)){
                        if (containsElement(i, OperationsForVessel[v])) {
                            for (int t = EarliestStartingTimeForVessel[v]; t < nTimePeriods; ++t) {
                                if (containsElement(t + 1, TimeWindowsForOperations[i - 1])) {
                                    left_side.addTerm(1, y[v][i - 1][t]);
                                    //System.out.println("add yi with v= " + (v + 1) + " perform operation= " + (i) + " in timeperiod= " + (t + 1));
                                }
                            }
                        }
                    }
                    if (containsElement(k, OperationsForVessel[v])) {
                        for (int t = EarliestStartingTimeForVessel[v]; t < nTimePeriods; ++t) {
                            if (containsElement(t + 1, TimeWindowsForOperations[k - 1])) {
                                if(nk == 0.0){
                                    left_side.addTerm(1, y[v][k - 1][t]);
                                    //System.out.println("add yk with coef 1 v= " + (v + 1) + " perform operation= " + (k) + " in timeperiod= " + (t + 1));
                                }
                                else {
                                    left_side.addTerm(nk, y[v][k - 1][t]);
                                    //System.out.println("add yk with coef nk v= " + (v + 1) + " perform operation= " + (k) + " in timeperiod= " + (t + 1));
                                }
                            }
                        }
                    }
                }
                for(int i : ConsolidatedTasks.get(k)) {
                    left_side.addTerm(1, z[i - 1]);
                    //System.out.println("add z with i= " + (i));
                }
                if(nk == 0.0){
                    left_side.addTerm(1, z[k-1]);
                    //System.out.println("add z with coef 1 k= " + (k));
                }
                else {
                    left_side.addTerm(nk, z[k-1]);
                    //System.out.println("add z with coef nk k= " + (k));
                }
                double rs = 0.0;
                if(nk == 0){
                    rs = 1;
                }
                else{
                    rs = nk;
                }

                model.addConstr(left_side, GRB.EQUAL, rs, "consolidatedTasks"+k);
            }




            // Solve
            model.optimize();
            // ---------  Se if model has returned any results ---------
            int optimstatus = model.get(GRB.IntAttr.Status);
            double objval = 0;
            double runtime = model.get(GRB.DoubleAttr.Runtime);
            double gap = model.get(GRB.DoubleAttr.MIPGap);
            double objBound = model.get(GRB.DoubleAttr.ObjBound);
            double objBoundC = model.get(GRB.DoubleAttr.ObjBoundC);
            int isOptimal = GRB.Status.OPTIMAL;
            int numRows = model.get(GRB.IntAttr.NumConstrs);
            int numCol = model.get(GRB.IntAttr.NumVars);
            int continuousVariables =  model.get(GRB.IntAttr.NumIntVars);
            int binaryVariables = model.get(GRB.IntAttr.NumBinVars);
            int nonZeros = model.get(GRB.IntAttr.NumNZs);
            int solCount = model.get(GRB.IntAttr.SolCount);
            double nodeCount = model.get(GRB.DoubleAttr.NodeCount);
            //double rootNodeSolution = model.get(GRB.DoubleAttr.Start);
            String instanceName = model.get(GRB.StringAttr.ModelName);
            //double preProcessTime = model.get(GRB.DoubleAttr.);



            if (optimstatus == GRB.Status.OPTIMAL) {
                objval = model.get(GRB.DoubleAttr.ObjVal);
                System.out.println("Optimal objective: " + objval);
            } else if (optimstatus == GRB.Status.INF_OR_UNBD) {
                System.out.println("Model is infeasible or unbounded");
                //return routing;
            } else if (optimstatus == GRB.Status.INFEASIBLE) {

                // Compute IIS
                System.out.println("The model is infeasible; computing IIS");
                model.computeIIS();
                System.out.println("\nThe following constraint(s) "
                        + "cannot be satisfied:");
                for (GRBConstr c : model.getConstrs()) {
                    if (c.get(GRB.IntAttr.IISConstr) == 1) {
                        System.out.println(c.get(GRB.StringAttr.ConstrName));
                    }
                }
                //return routing;
            } else if (optimstatus == GRB.Status.UNBOUNDED) {
                System.out.println("Model is unbounded");
                //return routing;
            } else {
                objval = model.get(GRB.DoubleAttr.ObjVal);
                System.out.println("Optimization was stopped with status = "
                        + optimstatus);
                //return routing;
            }
            System.out.println("   ");
            System.out.println("   ");

            int [] done= new int[nOperations];
            System.out.println("\nTOTAL COSTS: " + model.get(GRB.DoubleAttr.ObjVal));
            System.out.println("SOLUTION:");
            for (int v=0; v < nVessels; ++v){
                for (int t=EarliestStartingTimeForVessel[v]; t<nTimePeriods;++t){
                    for (int i=0; i<nOperations;++i){
                        for (int j=0; j<nOperations;++j){
                            if(Edges[v][i][j]==1 ) {
                                if (x[v][t][i][j].get(GRB.DoubleAttr.X) == 1) {
                                    routing.add("Vessel " + (v + 1) + " is sailing between " +
                                            "task " + (i + 1) + " and task" + (j + 1) + " in time" +
                                            " period " + (t + 1));
                                    vesselPath[v][t] = "x (" + (i+1) +"-"+ (j+1) +")";
                                }
                                if (j == nOperations - v - 1 && t == nTimePeriods-1 && i >= nVessels){
                                    for (int k = t + 1; k < t + TimeVesselUseOnOperation[v][i - nVessels][t] + 1; k++) {
                                        if (x[v][k][i][j].get(GRB.DoubleAttr.X) == 1) {
                                            routing.add("Vessel " + (v + 1) + " is sailing between " +
                                                    "task " + (i + 1) + " and task" + (j + 1) + " in time" +
                                                    " period " + (k + 1));
                                            vesselPath[v][k] = "x (" + (i + 1) + "-" + (j + 1) + ")";
                                        }
                                    }
                                }
                            }
                        }
                        if (containsElement(i+1,OperationsForVessel[v])){
                            if (containsElement(t + 1, TimeWindowsForOperations[i])) {
                                if (y[v][i][t].get(GRB.DoubleAttr.X) == 1) {
                                    routing.add("Task " + (i + 1) + " is performed by vessel " + (v + 1) +
                                            " in time period " + (t + 1));
                                    vesselPath[v][t] = "y (" + (i+1)+")";
                                }
                            }
                            if( w[v][i][t].get(GRB.DoubleAttr.X) == 1)  {
                                routing.add("Vessel "+ (v+1)+ " wait in task "+(i+1)+
                                        " in time period "+(t+1));
                                vesselPath[v][t]= "w ("+ (i+1)+")";
                            }
                        }
                    }
                }
            }

            for (int i=nVessels;i<nOperations-nVessels;i++) {
                if (done[i] != 1) {
                    if (z[i].get(GRB.DoubleAttr.X) == 1) {
                        operationsNotPerformed.add(i+1);
                        routing.add("Task " + (i + 1) + " is not performed");
                        done[i] = 1;
                    }
                }
            }
            //Save results
            Result result = new Result(runtime, objval, gap, objBound, objBoundC, instanceName, isOptimal, optimstatus,
                    vesselsNotUsed, operationsNotPerformed, (nTimePeriods/12), numRows, numCol, continuousVariables,
                    binaryVariables, nonZeros, solCount, (nOperations-2*nVessels), vesselPath, nVessels, nTimePeriods, nodeCount);
            result.store();

            // Dispose of model and environment
            model.dispose();
            env.dispose();



        } catch (GRBException | IOException e) {
            System.out.println("Error code: " + e.getStackTrace() + ". " +
                    e.getMessage());

        }
        return routing;

    }

    public void writeToFile(List<String> routing, String filename){
        try(FileWriter fw = new FileWriter(filename, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            for(String s : routing) {
                out.println(s);
            }
        } catch (IOException e) {
            //exception handling left as an exercise for the reader

        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        /*int[] vessels=new int[]{1,2,4,5,5,6,2,4};
        int[] locStart = new int[]{1,2,3,4,5,6,7,8};
        int loc = ParameterFile.loc;
        String testInstance=ParameterFile.testInstance;
        String nameResultFile =ParameterFile.nameResultFileGurobi;
        int days = ParameterFile.days;
        String weatherFile = ParameterFile.weatherFile;
        if (loc == 20) {
            vessels = new int[]{3, 4, 5};
            locStart = new int[]{94, 95, 96};
        } else if (loc == 25) {
            vessels = new int[]{3, 4, 5, 6};
            locStart = new int[]{3, 4, 5, 6};
        }
        else if (loc == 30) {
            vessels = new int[]{1, 3, 4, 5, 6};
            locStart = new int[]{1, 3, 4, 5, 6};
        }
        else if (loc == 35) {
            vessels = new int[]{1, 3, 4, 5, 6};
            locStart = new int[]{1, 3, 4, 5, 6};
        }
        else if (loc == 40) {
            vessels = new int[]{1,2,3,4,5,6};
            locStart = new int[]{94,95,96,97,98,99};
        }
        else if (loc == 60) {
            vessels = new int[]{1,2, 3,4, 5,6,2,3,4};
            locStart = new int[]{94,95,96,97,98,99,100,101,102};
        }
        else if (loc == 15) {
            vessels = new int[]{1 , 2 , 4,5};
            locStart = new int[]{1, 2, 3,4};
        }*/
        //dg.printAllData();
/*
        for(int i =1;i<6;i++){
            int[] vessels = new int[]{3, 5,6};
            int []  locStart = new int[]{94, 95, 96};
            //testInstance="test_instances/30" + "_"+i+"_locations_normalOpGenerator.txt";
            String testInstance = "technical_test_instances//20_"+i+"_low_locations(94_113)_.txt";
            String nameResultFile = ParameterFile.nameResultFileGurobi+testInstance;
            DataGenerator dg = new DataGenerator(vessels, ParameterFile.days, locStart, testInstance, nameResultFile, ParameterFile.weatherFile);
            dg.generateData();
            BasicModel m = new BasicModel(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                    dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getTimeVesselUseOnOperationGurobi(),dg.getEarliestStartingTimeForVessel(),
                    dg.getSailingCostForVessel(), dg.getOperationGainGurobi(), dg.getPrecedence(), dg.getSimultaneous(),
                    dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel()
                    ,testInstance,"september");
            List<String> routing = m.runModel(testInstance);
            m.writeToFile(routing, nameResultFile);

        }
        for(int i =1;i<6;i++){
            int[] vessels = new int[]{1,3,4,5,6};
            int [] locStart = new int[]{94,95,96,97,98};
            String testInstance = "technical_test_instances//40_"+i+"_low_locations(94_133)_.txt";
            String nameResultFile = ParameterFile.nameResultFileGurobi+testInstance;
            DataGenerator dg = new DataGenerator(vessels, ParameterFile.days, locStart, testInstance, nameResultFile, ParameterFile.weatherFile);
            dg.generateData();
            BasicModel m = new BasicModel(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                    dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getTimeVesselUseOnOperationGurobi(), dg.getEarliestStartingTimeForVessel(),
                    dg.getSailingCostForVessel(), dg.getOperationGainGurobi(), dg.getPrecedence(), dg.getSimultaneous(),
                    dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(),
                    dg.getEndPenaltyForVessel(),testInstance,"september");
            List<String> routing = m.runModel(testInstance);
            m.writeToFile(routing, nameResultFile);

        }

 */
        for(int i =1;i<4;i++){
            int []vessels = new int[]{1,2,3,4,5,6,3,4,1,2,3,4,5,6,3,4,1,2,3,4,5,6,3,4};
            int []locStart = new int[]{1,9,17,25,33,41,49,57,65,73,81,89,97,105,113,121,129,137,145,153,161,169,177,185};
            String testInstance = "large_test_instances/all_MOWI_locations(1_191)_"+i+".txt";
            String nameResultFile = ParameterFile.nameResultFileGurobi+testInstance;
            DataGenerator dg = new DataGenerator(vessels, ParameterFile.days, locStart, testInstance, nameResultFile, ParameterFile.weatherFile);
            dg.generateData();
            BasicModel m = new BasicModel(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                    dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getTimeVesselUseOnOperationGurobi(), dg.getEarliestStartingTimeForVessel(),
                    dg.getSailingCostForVessel(), dg.getOperationGainGurobi(), dg.getPrecedence(), dg.getSimultaneous(),
                    dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(),
                    dg.getEndPenaltyForVessel(),testInstance,"large_instances");
            List<String> routing = m.runModel(testInstance);
            m.writeToFile(routing, nameResultFile);

        }
        /*
        for(int i =1;i<4;i++){
            int[] vessels = new int[]{3, 5,6};
            int []  locStart = new int[]{94, 95, 96};
            //testInstance="test_instances/30" + "_"+i+"_locations_normalOpGenerator.txt";
            String testInstance = "technical_test_instances//20_"+i+"_low_locations(94_113)_.txt";
            String nameResultFile = ParameterFile.nameResultFileGurobi2+testInstance;
            DataGenerator dg = new DataGenerator(vessels, ParameterFile.days, locStart, testInstance, nameResultFile, ParameterFile.weatherFile2);
            dg.generateData();
            BasicModel m = new BasicModel(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                    dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getTimeVesselUseOnOperationGurobi(), dg.getEarliestStartingTimeForVessel(),
                    dg.getSailingCostForVessel(), dg.getOperationGainGurobi(), dg.getPrecedence(), dg.getSimultaneous(),
                    dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(),
                    dg.getEndPenaltyForVessel(),testInstance,"januar");
            List<String> routing = m.runModel(testInstance);
            m.writeToFile(routing, nameResultFile);

        }
        for(int i =1;i<4;i++){
            int[] vessels = new int[]{1,3,4,5,6};
            int [] locStart = new int[]{94,95,96,97,98};
            String testInstance = "technical_test_instances//40_"+i+"_low_locations(94_133)_.txt";
            String nameResultFile = ParameterFile.nameResultFileGurobi2+testInstance;
            DataGenerator dg = new DataGenerator(vessels, ParameterFile.days, locStart, testInstance, nameResultFile, ParameterFile.weatherFile2);
            dg.generateData();
            BasicModel m = new BasicModel(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                    dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getTimeVesselUseOnOperationGurobi(),dg.getEarliestStartingTimeForVessel(),
                    dg.getSailingCostForVessel(), dg.getOperationGainGurobi(), dg.getPrecedence(), dg.getSimultaneous(),
                    dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(),
                    dg.getEndPenaltyForVessel(),testInstance,"januar");
            List<String> routing = m.runModel(testInstance);
            m.writeToFile(routing, nameResultFile);

        }
        for(int i =1;i<4;i++){
            int []vessels = new int[]{1,2, 3,4, 5,6,3,4};
            int []locStart = new int[]{94,95,96,97,98,99,100,101};
            String testInstance = "technical_test_instances//60_"+i+"_low_locations(81_140)_.txt";
            String nameResultFile = ParameterFile.nameResultFileGurobi2+testInstance;
            DataGenerator dg = new DataGenerator(vessels, ParameterFile.days, locStart, testInstance, nameResultFile, ParameterFile.weatherFile2);
            dg.generateData();
            BasicModel m = new BasicModel(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                    dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getTimeVesselUseOnOperationGurobi(),dg.getEarliestStartingTimeForVessel(),
                    dg.getSailingCostForVessel(), dg.getOperationGainGurobi(), dg.getPrecedence(), dg.getSimultaneous(),
                    dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(),
                    dg.getEndPenaltyForVessel(),testInstance,"januar");
            List<String> routing = m.runModel(testInstance);
            m.writeToFile(routing, nameResultFile);

        }
        */
    }
}

