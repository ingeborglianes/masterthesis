import org.w3c.dom.ls.LSOutput;

import java.io.FileNotFoundException;
import java.sql.SQLOutput;
import java.sql.Time;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.stream.IntStream;

public class ConstructionHeuristic {
    private int [][] OperationsForVessel;
    private int [][] TimeWindowsForOperations;
    private int [][][] Edges;
    private int [][][][] SailingTimes;
    private int [][][] TimeVesselUseOnOperation;
    private int [] EarliestStartingTimeForVessel;
    private int [] SailingCostForVessel;
    private int [][][] operationGain;
    private int [][][] operationGainGurobi;
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
    List<OperationInRoute> unroutedTasks =  new ArrayList<OperationInRoute>();
    List<List<OperationInRoute>> vesselroutes = new ArrayList<List<OperationInRoute>>();
    List<Integer> allOperations =  new ArrayList<Integer>();
    private int[] sortedOperations;
    private int [][] twIntervals;
    private int [][] precedenceALNS;
    private int [][] simALNS;
    private int [][] bigTasksALNS;
    //each entry in routeSailingCost and routeOperationGain correspond to one vessel/route
    private int [] routeSailingCost;
    private int[] routeOperationGain;
    private int[][] timeWindowsForOperations;
    private int[] actionTime;
    private int objValue;
    //map for operations that are connected with precedence. ID= operation number. Value= Precedence value.
    private Map<Integer,PrecedenceValues> precedenceOverOperations=new HashMap<>();
    private Map<Integer,PrecedenceValues> precedenceOfOperations=new HashMap<>();
    //map for operations that are connected as simultaneous operations. ID= operation number. Value= Simultaneous value.
    private Map<Integer, ConnectedValues> simultaneousOp = new HashMap<>();
    private List<Map<Integer, ConnectedValues>> simOpRoutes = new ArrayList<Map<Integer, ConnectedValues>>();
    private List<Map<Integer,PrecedenceValues>> precedenceOfRoutes=new ArrayList<Map<Integer, PrecedenceValues>>();
    private List<Map<Integer,PrecedenceValues>> precedenceOverRoutes=new ArrayList<Map<Integer, PrecedenceValues>>();
    private Map<Integer, ConsolidatedValues> consolidatedOperations = new HashMap<>();

    public ConstructionHeuristic(int [][] OperationsForVessel, int [][] TimeWindowsForOperations, int [][][] Edges, int [][][][] SailingTimes,
                                 int [][][] TimeVesselUseOnOperation, int [] EarliestStartingTimeForVessel,
                                 int [] SailingCostForVessel, int [][][] operationGain, int [][] Precedence, int [][] Simultaneous,
                                 int [] BigTasks, Map<Integer, List<Integer>> ConsolidatedTasks, int[] endNodes, int[] startNodes, double[] endPenaltyforVessel,
                                 int[][] twIntervals, int[][] precedenceALNS, int[][] simALNS, int[][] bigTasksALNS, int[][] timeWindowsForOperations, int[][][] operationgaingurobi){
        this.OperationsForVessel=OperationsForVessel;
        this.TimeWindowsForOperations=TimeWindowsForOperations;
        this.Edges=Edges;
        this.SailingTimes=SailingTimes;
        this.TimeVesselUseOnOperation=TimeVesselUseOnOperation;
        this.EarliestStartingTimeForVessel=EarliestStartingTimeForVessel;
        this.SailingCostForVessel=SailingCostForVessel;
        this.operationGain=operationGain;
        this.Precedence=Precedence;
        this.Simultaneous=Simultaneous;
        this.BigTasks=BigTasks;
        this.ConsolidatedTasks=ConsolidatedTasks;
        this.nVessels=this.OperationsForVessel.length;
        //nOperations is the number of all nodes, including start and end nodes
        this.nOperations=TimeWindowsForOperations.length;
        this.nTimePeriods=TimeWindowsForOperations[0].length;
        this.endNodes=endNodes;
        this.startNodes=startNodes;
        this.endPenaltyforVessel=endPenaltyforVessel;
        this.sortedOperations=new int[nOperations-2*startNodes.length];
        this.twIntervals=twIntervals;
        this.precedenceALNS=precedenceALNS;
        this.simALNS=simALNS;
        this.bigTasksALNS=bigTasksALNS;
        this.routeSailingCost=new int[nVessels];
        this.routeOperationGain=new int[nVessels];
        this.objValue=0;
        for(int o = startNodes.length+1; o<nOperations-endNodes.length+1;o++){
            allOperations.add(o);
        }
        //actionTime is only a heuristic with changing weather, maybe drop it?
        this.actionTime=new int[startNodes.length];
        this.timeWindowsForOperations=timeWindowsForOperations;
        this.operationGainGurobi=operationgaingurobi;
        //System.out.println("Number of operations: "+(nOperations-startNodes.length*2));
        //System.out.println("START NODES: "+Arrays.toString(this.startNodes));
        //System.out.println("END NODES: "+Arrays.toString(this.endNodes));
    }

    public void createSortedOperations(){
        //Use time 0 and vessel 0 kind of randomly, as we need to know the relationship between the operations
        SortedSet<KeyValuePair> penaltiesDict = new TreeSet<KeyValuePair>();
        for (int g=0;g<this.operationGain[0].length;g++){
            //System.out.println((g+1+startNodes.length)+" "+operationGain[0][g][0]);
            //Key value (= operation number) in penaltiesDict is not null indexed
            penaltiesDict.add(new KeyValuePair(g+1+startNodes.length,operationGain[0][g][0]));
        }
        int index=0;
        for (KeyValuePair keyValuePair : penaltiesDict) {
            if(!DataGenerator.containsElement(keyValuePair.key,sortedOperations)){
                sortedOperations[index] = keyValuePair.key;
            }
            if(bigTasksALNS[keyValuePair.key-startNodes.length-1]!= null && bigTasksALNS[keyValuePair.key- startNodes.length-1][0]==keyValuePair.key){
                for (int i=1;i<bigTasksALNS[keyValuePair.key-startNodes.length-1].length;i++){
                    index+=1;
                    sortedOperations[index]=bigTasksALNS[keyValuePair.key-startNodes.length-1][i];
                }
            }
            index+=1;
        }
        //System.out.println("BIG TASK ALNS LIST");
        //PrintData.printBigTasksALNS(bigTasksALNS,nOperations);
        /*
        System.out.println("Sorted by operation gain: ");
        for(Integer op : sortedOperations){
            System.out.println("Operation "+op+" Gain: "+operationGain[0][op-1-startNodes.length][0]);
        }
        System.out.println("Sorted tasks: "+Arrays.toString(sortedOperations));

         */
    }

    public void constructionHeuristic(){
        for (int n = 0; n < nVessels; n++) {
            vesselroutes.add(null);
            precedenceOfRoutes.add(new HashMap<Integer, PrecedenceValues>());
            precedenceOverRoutes.add(new HashMap<Integer, PrecedenceValues>());
            simOpRoutes.add(new HashMap<Integer, ConnectedValues>());
        }
        ArrayList<Integer> removeConsolidatedSmallTasks=new ArrayList<>();
        outer: for (Integer o : sortedOperations){
            if(removeConsolidatedSmallTasks.contains(o)){
                continue;
            }
            //System.out.println("On operation: "+o);
            int benefitIncrease=-100000;
            int indexInRoute=0;
            int routeIndex=0;
            int earliest=0;
            int latest=nTimePeriods-1;
            int timeAdded=0;
            Boolean isActionTime = false;
            for (int v = 0; v < nVessels; v++) {
                //List<ConnectedValues> simOps = checkSimultaneous(v);
                Boolean notThisVessel = checkSimOpInRoute(simOpRoutes.get(v),o,simALNS,startNodes);
                /*
                if(actionTime[v]+TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]>nTimePeriods){
                    System.out.println("Break because of actiontime");
                    isActionTime = true;
                    continue;
                }
                 */
                //List<PrecedenceValues> precedenceOver= checkPrecedence(v,0);
                //List<PrecedenceValues> precedenceOf= checkPrecedence(v,1);
                boolean precedenceOverFeasible=true;
                boolean precedenceOfFeasible=true;
                boolean simultaneousFeasible=true;
                if (DataGenerator.containsElement(o, OperationsForVessel[v]) && notThisVessel) {
                    //System.out.println("Try vessel "+v);
                    if (vesselroutes.get(v) == null) {
                        //System.out.println("Empty route");
                        //insertion into empty route
                        int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                        int timeIncrease=sailingTimeStartNodeToO;
                        int sailingCost=sailingTimeStartNodeToO*SailingCostForVessel[v];
                        int earliestTemp=Math.max(EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO+1,twIntervals[o-startNodes.length-1][0]);
                        int latestTemp=Math.min(nTimePeriods,twIntervals[o-startNodes.length-1][1]);
                        int[] precedenceOfValuesEarliest=checkprecedenceOfEarliest(o,v,earliestTemp,precedenceALNS,startNodes,
                                precedenceOverOperations,TimeVesselUseOnOperation);
                        if (precedenceOfValuesEarliest[1]==1) {
                            //System.out.println("BREAK PRECEDENCE");
                            continue outer;
                        }
                        earliestTemp=precedenceOfValuesEarliest[0];
                        int [] simultaneousTimesValues = checkSimultaneousOfTimes(o,v,earliestTemp,latestTemp,simALNS,simultaneousOp,startNodes);
                        //System.out.println(simultaneousTimesValues[0] + "," + simultaneousTimesValues[1]+ " sim time");
                        if(simultaneousTimesValues[2]==1){
                            //System.out.println("BREAK SIMULTANEOUS");
                            continue outer;
                        }
                        earliestTemp=simultaneousTimesValues[0];
                        latestTemp=simultaneousTimesValues[1];



                        int[] startingTimes = weatherFeasible(TimeVesselUseOnOperation,v,earliestTemp,latestTemp,o,nTimePeriods,startNodes);
                        if(startingTimes != null){
                            earliestTemp = startingTimes[0];
                            latestTemp = startingTimes[1];
                        }


                        if(earliestTemp<=latestTemp && startingTimes != null) {
                            int benefitIncreaseTemp=operationGain[v][o-startNodes.length-1][earliestTemp-1]-sailingCost;
                            if(precedenceALNS[o-1-startNodes.length][0]!=0){
                                benefitIncreaseTemp+=(nTimePeriods-earliestTemp)*ParameterFile.earlyPrecedenceFactor;
                            }
                            if(benefitIncreaseTemp>0 && benefitIncreaseTemp>benefitIncrease) {
                                benefitIncrease = benefitIncreaseTemp;
                                routeIndex = v;
                                indexInRoute = 0;
                                earliest = earliestTemp;
                                latest = latestTemp;
                                timeAdded=timeIncrease;
                            }
                        }
                    }
                    else{
                        for(int n=0;n<vesselroutes.get(v).size();n++){
                            //System.out.println("On index "+n);
                            if(n==0) {
                                //check insertion in first position
                                int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                                int earliestTemp = Math.max(EarliestStartingTimeForVessel[v] + sailingTimeStartNodeToO + 1, twIntervals[o - startNodes.length - 1][0]);
                                int opTime=TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO];
                                int[] precedenceOfValuesEarliest=checkprecedenceOfEarliest(o,v,earliestTemp,precedenceALNS,startNodes,precedenceOverOperations
                                ,TimeVesselUseOnOperation);
                                if (precedenceOfValuesEarliest[1]==1) {
                                    //System.out.println("BREAK PRECEDENCE");
                                    //System.out.println("precedence over task not placed");
                                    continue outer;
                                }
                                earliestTemp=precedenceOfValuesEarliest[0];
                                if(earliestTemp<=nTimePeriods) {
                                    int sailingTimeOToNext = SailingTimes[v][earliestTemp - 1][o - 1][vesselroutes.get(v).get(0).getID() - 1];
                                    int latestTemp = Math.min(vesselroutes.get(v).get(0).getLatestTime() - sailingTimeOToNext - opTime,
                                            twIntervals[o - startNodes.length - 1][1]);

                                    if(latestTemp>0) {
                                        latestTemp = weatherLatestTimeSimPreInsert(latestTemp, earliestTemp, TimeVesselUseOnOperation, v, o, startNodes, SailingTimes, 0, vesselroutes, simultaneousOp,
                                                                            precedenceOfOperations,precedenceOverOperations,-1);
                                    }
                                    int[] simultaneousTimesValues = checkSimultaneousOfTimes(o, v, earliestTemp, latestTemp,simALNS,simultaneousOp,startNodes);
                                    if (simultaneousTimesValues[2] == 1) {
                                        //System.out.println("BREAK SIMULTANEOUS");
                                        //System.out.println("sim task not placed");
                                        continue outer;
                                    }
                                    earliestTemp = simultaneousTimesValues[0];
                                    latestTemp = simultaneousTimesValues[1];

                                    int timeIncrease = sailingTimeStartNodeToO + sailingTimeOToNext
                                            - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselroutes.get(v).get(0).getID() - 1];
                                    int sailingCost = timeIncrease * SailingCostForVessel[v];
                                    Boolean pPlacementFeasible = checkPPlacement(o, n, v,precedenceALNS,startNodes,precedenceOverOperations,simALNS,simultaneousOp);


                                    int[] startingTimes = weatherFeasible(TimeVesselUseOnOperation,v,earliestTemp,latestTemp,o,nTimePeriods,startNodes);
                                    if(startingTimes != null){
                                        earliestTemp = startingTimes[0];
                                        latestTemp = startingTimes[1];
                                    }


                                    if (earliestTemp <= latestTemp && pPlacementFeasible && startingTimes != null) {
                                        OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                                        int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                        int changedTime = checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, 0, v, o,vesselroutes,
                                                TimeVesselUseOnOperation,startNodes,SailingTimes);
                                        int deltaOperationGainLastOperation = 0;
                                        if (changedTime > 0) {
                                            deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                    operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                        }
                                        int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                        if (benefitIncreaseTemp > 0 ) {
                                            if(precedenceALNS[o-1-startNodes.length][0]!=0){
                                                benefitIncreaseTemp+=(nTimePeriods-earliestTemp)*ParameterFile.earlyPrecedenceFactor;
                                            }
                                            if(benefitIncreaseTemp > benefitIncrease) {
                                                //System.out.println("Feil finnes i feasibility check loopen");
                                                precedenceOverFeasible = checkPOverFeasible(precedenceOverRoutes.get(v), o, 0, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods,
                                                        SailingTimes, vesselroutes, precedenceOfOperations, precedenceOverRoutes);
                                                precedenceOfFeasible = checkPOfFeasible(precedenceOfRoutes.get(v), o, 0, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                        vesselroutes, precedenceOverOperations, precedenceOfOperations,precedenceOfRoutes,simultaneousOp,twIntervals);
                                                simultaneousFeasible = checkSimultaneousFeasible(simOpRoutes.get(v), o, v, 0, earliestTemp, latestTemp, simultaneousOp, simALNS,
                                                        startNodes, SailingTimes, TimeVesselUseOnOperation, vesselroutes,precedenceOverOperations,precedenceOfOperations,twIntervals);
                                                //System.out.println("Feasibility check loopen slutter her");
                                                if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                                    benefitIncrease = benefitIncreaseTemp;
                                                    routeIndex = v;
                                                    indexInRoute = 0;
                                                    earliest = earliestTemp;
                                                    latest = latestTemp;
                                                    timeAdded = timeIncrease;
                                                    //System.out.println("Chosen index: " + (0));
                                                    //System.out.println(earliest);
                                                    //System.out.println(latest);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (n==vesselroutes.get(v).size()-1){
                                //check insertion in last position
                                //System.out.println("Checking this position");
                                int earliestN=vesselroutes.get(v).get(n).getEarliestTime();
                                int operationTimeN=TimeVesselUseOnOperation[v][vesselroutes.get(v).get(n).getID()-1-startNodes.length][earliestN-1];
                                int startTimeSailingTimePrevToO=earliestN+operationTimeN;
                                if(startTimeSailingTimePrevToO >= nTimePeriods){
                                    continue;
                                }
                                int sailingTimePrevToO=SailingTimes[v][startTimeSailingTimePrevToO-1]
                                        [vesselroutes.get(v).get(n).getID()-1][o - 1];
                                int earliestTemp=Math.max(earliestN + operationTimeN + sailingTimePrevToO
                                        ,twIntervals[o-startNodes.length-1][0]);
                                int latestTemp=twIntervals[o-startNodes.length-1][1];
                                int[] precedenceOfValuesEarliest=checkprecedenceOfEarliest(o,v,earliestTemp,precedenceALNS,startNodes,precedenceOverOperations
                                        ,TimeVesselUseOnOperation);
                                if (precedenceOfValuesEarliest[1]==1) {
                                    //System.out.println("BREAK PRECEDENCE");
                                    //System.out.println("precedence over task not placed");
                                    continue outer;
                                }
                                earliestTemp=precedenceOfValuesEarliest[0];
                                if(earliestTemp<=nTimePeriods) {
                                    int[] simultaneousTimesValues = checkSimultaneousOfTimes(o, v, earliestTemp, latestTemp,simALNS,simultaneousOp,startNodes);
                                    if (simultaneousTimesValues[2] == 1) {
                                        //System.out.println("BREAK SIMULTANEOUS");
                                        //System.out.println("sim task not placed");
                                        continue outer;
                                    }
                                    earliestTemp = simultaneousTimesValues[0];
                                    latestTemp = simultaneousTimesValues[1];

                                    int timeIncrease = sailingTimePrevToO;
                                    int sailingCost = timeIncrease * SailingCostForVessel[v];


                                    int[] startingTimes = weatherFeasible(TimeVesselUseOnOperation,v,earliestTemp,latestTemp,o,nTimePeriods,startNodes);
                                    if(startingTimes != null){
                                        earliestTemp = startingTimes[0];
                                        latestTemp = startingTimes[1];
                                    }


                                    if (earliestTemp <= latestTemp && startingTimes != null) {
                                        OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                                        int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                        int changedTime = checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, n + 1, v, o,vesselroutes,
                                                TimeVesselUseOnOperation,startNodes,SailingTimes);
                                        int deltaOperationGainLastOperation = 0;
                                        if (changedTime > 0) {
                                            deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                    operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                        }
                                        int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                        if (benefitIncreaseTemp > 0 ) {
                                            if(precedenceALNS[o-1-startNodes.length][0]!=0){
                                                benefitIncreaseTemp+=(nTimePeriods-earliestTemp)*ParameterFile.earlyPrecedenceFactor;
                                            }
                                            if(benefitIncreaseTemp > benefitIncrease) {
                                                //System.out.println("Fesibility check loopen for siste element");
                                                precedenceOverFeasible = checkPOverFeasible(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods, SailingTimes,
                                                        vesselroutes, precedenceOfOperations, precedenceOverRoutes);
                                                precedenceOfFeasible = checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                        vesselroutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, simultaneousOp,twIntervals);
                                                simultaneousFeasible = checkSimultaneousFeasible(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp, simultaneousOp,
                                                        simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation, vesselroutes,precedenceOverOperations,precedenceOfOperations,twIntervals);
                                                //System.out.println("Feasibility check loopen for siste element slutt");
                                                if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                                    //System.out.println("Chosen index: " + (n + 1) + " n==vesselroutes.get(v).size()-1");
                                                    benefitIncrease = benefitIncreaseTemp;
                                                    routeIndex = v;
                                                    indexInRoute = n + 1;
                                                    earliest = earliestTemp;
                                                    latest = latestTemp;
                                                    timeAdded = timeIncrease;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if(n<vesselroutes.get(v).size()-1) {
                                //check insertion for all other positions in the route
                                int earliestN = vesselroutes.get(v).get(n).getEarliestTime();
                                int operationTimeN = TimeVesselUseOnOperation[v][vesselroutes.get(v).get(n).getID() - 1 - startNodes.length][earliestN - 1];
                                int startTimeSailingTimePrevToO = earliestN + operationTimeN;
                                int sailingTimePrevToO = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselroutes.get(v).get(n).getID() - 1][o - 1];
                                int earliestTemp = Math.max(earliestN + sailingTimePrevToO + operationTimeN, twIntervals[o - startNodes.length - 1][0]);
                                int[] precedenceOfValuesEarliest = checkprecedenceOfEarliest(o, v, earliestTemp,precedenceALNS,startNodes,precedenceOverOperations
                                        ,TimeVesselUseOnOperation);
                                if (precedenceOfValuesEarliest[1] == 1) {
                                    //System.out.println("BREAK PRECEDENCE");
                                    //System.out.println("precedence over task not placed");
                                    continue outer;
                                }
                                earliestTemp = precedenceOfValuesEarliest[0];
                                if(earliestTemp<=nTimePeriods) {
                                    if (earliestTemp - 1 < nTimePeriods) {
                                        int opTime = TimeVesselUseOnOperation[v][o - 1 - startNodes.length][earliestTemp - 1];
                                        int sailingTimeOToNext = SailingTimes[v][Math.min(earliestTemp + opTime - 1, nTimePeriods - 1)][o - 1][vesselroutes.get(v).get(n + 1).getID() - 1];

                                        int latestTemp = Math.min(vesselroutes.get(v).get(n + 1).getLatestTime() -
                                                sailingTimeOToNext - opTime, twIntervals[o - startNodes.length - 1][1]);

                                        if(latestTemp>0) {
                                            latestTemp = weatherLatestTimeSimPreInsert(latestTemp, earliestTemp, TimeVesselUseOnOperation, v, o, startNodes, SailingTimes, n+1, vesselroutes,
                                                                            simultaneousOp, precedenceOfOperations,precedenceOverOperations,-1);
                                        }

                                        int[] simultaneousTimesValues = checkSimultaneousOfTimes(o, v, earliestTemp, latestTemp,simALNS,simultaneousOp,startNodes);
                                        if (simultaneousTimesValues[2] == 1) {
                                            //System.out.println("BREAK SIMULTANEOUS");
                                            //System.out.println("sim task not placed");
                                            continue outer;
                                        }
                                        earliestTemp = simultaneousTimesValues[0];
                                        latestTemp = simultaneousTimesValues[1];
                                        int sailingTimePrevToNext = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselroutes.get(v).get(n).getID() - 1][vesselroutes.get(v).get(n + 1).getID() - 1];
                                        int timeIncrease = sailingTimePrevToO + sailingTimeOToNext - sailingTimePrevToNext;
                                        int sailingCost = timeIncrease * SailingCostForVessel[v];
                                        Boolean pPlacementFeasible = checkPPlacement(o, n + 1, v,precedenceALNS,startNodes,precedenceOverOperations,simALNS,simultaneousOp);


                                        int[] startingTimes = weatherFeasible(TimeVesselUseOnOperation,v,earliestTemp,latestTemp,o,nTimePeriods,startNodes);
                                        if(startingTimes != null){
                                            earliestTemp = startingTimes[0];
                                            latestTemp = startingTimes[1];
                                        }


                                        if (earliestTemp <= latestTemp && pPlacementFeasible && startingTimes!=null) {
                                            OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                                            int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                            int changedTime = checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, n + 1, v, o,vesselroutes,
                                                    TimeVesselUseOnOperation,startNodes,SailingTimes);
                                            int deltaOperationGainLastOperation = 0;
                                            if (changedTime > 0) {
                                                deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                        operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                            }
                                            int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;

                                            if (benefitIncreaseTemp > 0){
                                                if(precedenceALNS[o-1-startNodes.length][0]!=0){
                                                    benefitIncreaseTemp+=(nTimePeriods-earliestTemp)*ParameterFile.earlyPrecedenceFactor;
                                                }
                                                 if(benefitIncreaseTemp > benefitIncrease) {
                                                    int currentLatest = vesselroutes.get(v).get(n).getLatestTime();
                                                     //System.out.println("Feasibility check loopen for alle element");
                                                    simultaneousFeasible = checkSOfFeasible(o, v, currentLatest, startNodes, simALNS, simultaneousOp);

                                                    if (simultaneousFeasible) {
                                                        simultaneousFeasible = checkSimultaneousFeasible(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp,
                                                                simultaneousOp, simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation,
                                                                vesselroutes,precedenceOverOperations,precedenceOfOperations,twIntervals);
                                                    }
                                                    precedenceOverFeasible = checkPOverFeasible(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation,
                                                            nTimePeriods, SailingTimes, vesselroutes, precedenceOfOperations, precedenceOverRoutes);
                                                    precedenceOfFeasible = checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                            vesselroutes, precedenceOverOperations, precedenceOfOperations,precedenceOfRoutes,simultaneousOp,twIntervals);
                                                     //System.out.println("Fesibility check loopen for alle element slutt");
                                                    if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                                        //System.out.println("Chosen index: " + (n + 1) + " n<vesselroutes.get(v).size()-1");
                                                        benefitIncrease = benefitIncreaseTemp;
                                                        routeIndex = v;
                                                        indexInRoute = n + 1;
                                                        earliest = earliestTemp;
                                                        latest = latestTemp;
                                                        timeAdded = timeIncrease;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(benefitIncrease==-100000){
                //System.out.println("OPERATION "+o+ " not possible to place");
                if (bigTasksALNS[o - 1 - startNodes.length] != null && bigTasksALNS[o - startNodes.length - 1][2] == o) {
                    consolidatedOperations.replace(bigTasksALNS[o - startNodes.length - 1][0], new ConsolidatedValues(false, false, 0, 0, 0));
                } else if (bigTasksALNS[o - 1 - startNodes.length] != null && bigTasksALNS[o - startNodes.length - 1][1] == o) {
                    consolidatedOperations.put(bigTasksALNS[o - startNodes.length - 1][0], new ConsolidatedValues(false, false, 0, 0, 0));
                }
                if(simALNS[o-startNodes.length-1][1] != 0 ) {
                    ConnectedValues simOp = simultaneousOp.get(simALNS[o - startNodes.length - 1][1]);
                    if(simOp!=null) {
                        removeSimOf(simOp, simultaneousOp, vesselroutes, TimeVesselUseOnOperation, startNodes,
                                SailingTimes, twIntervals, unroutedTasks, simOpRoutes, precedenceOverOperations, precedenceOfOperations,
                                precedenceOverRoutes, precedenceOfRoutes, EarliestStartingTimeForVessel,precedenceALNS,bigTasksALNS);
                        simOpRoutes.get(simOp.getRoute()).remove(simALNS[o-startNodes.length-1][1]);
                        simultaneousOp.remove(simALNS[o-startNodes.length-1][1]);
                        if(precedenceALNS[o-startNodes.length-1][1] != 0){
                            precedenceOverOperations.get(precedenceALNS[o-startNodes.length-1][1]).setConnectedOperationObject(null);
                            int pRoute=precedenceOfOperations.get(simALNS[o-startNodes.length-1][1]).getRoute();
                            precedenceOfRoutes.get(pRoute).remove(simALNS[o-startNodes.length-1][1]);
                            precedenceOfOperations.remove(simALNS[o-startNodes.length-1][1]);
                        }
                    }
                }

            }

            //After iterating through all possible insertion places, we here add the operation at the best insertion place
            if(benefitIncrease!=-100000) {
                removeConsolidatedSmallTasks=updateConsolidatedOperations(o,routeIndex,removeConsolidatedSmallTasks,bigTasksALNS,
                startNodes, consolidatedOperations);
                actionTime[routeIndex]+=timeAdded+TimeVesselUseOnOperation[routeIndex][o-startNodes.length-1][EarliestStartingTimeForVessel[routeIndex]];
                OperationInRoute newOr=new OperationInRoute(o, earliest, latest);
                int presOver=precedenceALNS[o-1-startNodes.length][0];
                int presOf=precedenceALNS[o-1-startNodes.length][1];
                if (presOver!=0){
                    //System.out.println(o+" added in precedence operations dictionary 0 "+presOver);
                    PrecedenceValues pValues= new PrecedenceValues(newOr,null,presOver,indexInRoute,routeIndex,-1);
                    precedenceOverOperations.put(o,pValues);
                    precedenceOverRoutes.get(routeIndex).put(o,pValues);
                }
                if (presOf!=0){
                    //System.out.println("Operation precedence of: "+presOf);
                    PrecedenceValues pValues= precedenceOverOperations.get(presOf);
                    PrecedenceValues pValuesReplace=new PrecedenceValues(pValues.getOperationObject(),
                            newOr,pValues.getConnectedOperationID(),pValues.getIndex(),pValues.getRoute(),routeIndex);
                    //System.out.println("Operation "+pValues.getOperationObject().getID()+" route: "+pValues.getRoute());
                    PrecedenceValues pValuesPut=new PrecedenceValues(newOr,pValues.getOperationObject(),presOf,indexInRoute,routeIndex,pValues.getRoute());
                    precedenceOverOperations.put(presOf,pValuesReplace);
                    precedenceOfOperations.put(o, pValuesPut);
                    precedenceOverRoutes.get(pValues.getRoute()).put(presOf,pValuesReplace);
                    precedenceOfRoutes.get(routeIndex).put(o, pValuesPut);
                }
                //while((o-1-startNodes.length+k) < simALNS.length && simALNS[o-1-startNodes.length+k][0]!=0 ){
                int simA = simALNS[o-1-startNodes.length][1];
                int simB = simALNS[o-1-startNodes.length][0];
                if(simB != 0 && simA == 0) {
                    ConnectedValues sValue = new ConnectedValues(newOr, null,simB,indexInRoute,routeIndex, -1);
                    simultaneousOp.put(o,sValue);
                    simOpRoutes.get(routeIndex).put(o,sValue);
                }
                else if (simA != 0){
                    ConnectedValues sValues = simultaneousOp.get(simA);
                    if(sValues.getConnectedOperationObject() == null){
                        ConnectedValues cValuesReplace=new ConnectedValues(sValues.getOperationObject(), newOr, sValues.getConnectedOperationID(),
                                sValues.getIndex(), sValues.getRoute(), routeIndex);
                        simultaneousOp.replace(simA, sValues, cValuesReplace);
                        simOpRoutes.get(sValues.getRoute()).replace(simA, sValues, cValuesReplace);
                    }
                    else{
                        ConnectedValues cValuesPut1=new ConnectedValues(sValues.getOperationObject(), newOr, sValues.getConnectedOperationID(),
                                sValues.getIndex(), sValues.getRoute(), routeIndex);
                        simultaneousOp.put(simA, cValuesPut1);
                        simOpRoutes.get(sValues.getRoute()).put(simA, cValuesPut1);
                    }
                    ConnectedValues sim2=new ConnectedValues(newOr,sValues.getOperationObject(),simA,indexInRoute,routeIndex,sValues.getRoute());
                    simultaneousOp.put(o, sim2);
                    simOpRoutes.get(routeIndex).put(o, sim2);
                }
                /*System.out.println("NEW ADD: Vessel route "+routeIndex);
                System.out.println("Operation "+o);
                System.out.println("Earliest time "+ earliest);
                System.out.println("Latest time "+ latest);
                System.out.println("Route index "+indexInRoute);
                System.out.println(" ");*/


                if (vesselroutes.get(routeIndex) == null) {
                    int finalIndexInRoute = indexInRoute;
                    vesselroutes.set(routeIndex, new ArrayList<>() {{
                        add(finalIndexInRoute, newOr);
                    }});
                }
                else {
                    vesselroutes.get(routeIndex).add(indexInRoute,newOr);
                }
                allOperations.remove(Integer.valueOf(o));
                updateIndexesInsertion(routeIndex,indexInRoute, vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
                //Update all earliest starting times forward
                updateEarliest(earliest,indexInRoute,routeIndex, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"notLocal");
                updateLatest(latest,indexInRoute,routeIndex, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"notLocal", simultaneousOp, precedenceOfOperations,precedenceOverOperations,twIntervals);
                updatePrecedenceOver(precedenceOverRoutes.get(routeIndex),indexInRoute,simOpRoutes,precedenceOfOperations,precedenceOverOperations,
                        TimeVesselUseOnOperation, startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselroutes,SailingTimes,twIntervals);
                updatePrecedenceOf(precedenceOfRoutes.get(routeIndex),indexInRoute,TimeVesselUseOnOperation,startNodes,simOpRoutes,
                        precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselroutes,simultaneousOp,SailingTimes,twIntervals);
                updateSimultaneous(simOpRoutes,routeIndex,indexInRoute,simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,
                        startNodes,SailingTimes,precedenceOverOperations,precedenceOfOperations,vesselroutes,twIntervals);
                /*
                for(int r=0;r<nVessels;r++) {
                    //System.out.println("VESSEL " + r);
                    if(vesselroutes.get(r) != null) {
                        for (int n = 0; n < vesselroutes.get(r).size(); n++) {
                            //System.out.println("Number in order: " + n);
                            //System.out.println("ID " + vesselroutes.get(r).get(n).getID());
                            //System.out.println("Earliest starting time " + vesselroutes.get(r).get(n).getEarliestTime());
                            //System.out.println("latest starting time " + vesselroutes.get(r).get(n).getLatestTime());
                            //System.out.println(" ");
                        }
                    }
                }

                 */


            }
        }
        //System.out.println("ADD TO UNROUTED");
        for(Integer taskLeft : allOperations){
            if(bigTasksALNS[taskLeft-1-startNodes.length]==null) {
                unroutedTasks.add(new OperationInRoute(taskLeft, 0, nTimePeriods));
                //System.out.println("added to unrouted: "+taskLeft);
            }
        }
        for (Map.Entry<Integer, ConsolidatedValues> entry : consolidatedOperations.entrySet()) {
            int bigTask = entry.getKey();
            int small1= bigTasksALNS[bigTask-1-startNodes.length][1];
            int small2= bigTasksALNS[bigTask-1-startNodes.length][2];
            ConsolidatedValues conVals = entry.getValue();
            if (!conVals.getConsolidated() && !conVals.getSmallTasks()){
                unroutedTasks.add(new OperationInRoute(bigTask,0,0));
                unroutedTasks.add(new OperationInRoute(small1,0,0));
                unroutedTasks.add(new OperationInRoute(small2,0,0));
            }
        }
        //Calculate objective
        calculateObjective(vesselroutes,TimeVesselUseOnOperation,startNodes,SailingTimes,SailingCostForVessel,
                EarliestStartingTimeForVessel,operationGainGurobi,routeSailingCost,routeOperationGain,objValue,simALNS,bigTasksALNS);
    }

    public static ObjectiveValues calculateObjective(List<List<OperationInRoute>> vesselroutes, int [][][] TimeVesselUseOnOperation,
                                          int [] startNodes, int[][][][] SailingTimes, int [] SailingCostForVessel,
                                          int [] EarliestStartingTimeForVessel, int [][][] operationGain, int[] routeSailingCost,
                                          int [] routeOperationGain, int objValue, int [][] simALNS, int [][] bigTasksALNS){
        for (int r=0;r<vesselroutes.size();r++){
            if(vesselroutes.get(r)!= null && !vesselroutes.get(r).isEmpty()) {
                OperationInRoute or = vesselroutes.get(r).get(0);
                //System.out.println(or.getEarliestTime());
                int cost0 = SailingTimes[r][EarliestStartingTimeForVessel[r]][r][or.getID() - 1]*SailingCostForVessel[r];
                int gain0 = operationGain[r][or.getID() - 1 - startNodes.length][or.getEarliestTime()-1];
                routeSailingCost[r] += cost0;
                routeOperationGain[r] += gain0;
                objValue += gain0;
                objValue -= cost0;
                for (int o = 1; o < vesselroutes.get(r).size(); o++) {
                    OperationInRoute orCurrent = vesselroutes.get(r).get(o);
                    OperationInRoute orPrevious = vesselroutes.get(r).get(o - 1);
                    int opPreviousTime = TimeVesselUseOnOperation[r][orPrevious.getID() - 1 - startNodes.length][orPrevious.getEarliestTime()-1];
                    /*
                    for (int i = 0; i < vesselroutes.size(); i++) {
                        System.out.println("VESSELINDEX " + i);
                        if (vesselroutes.get(i) != null) {
                            for (int o2 = 0; o2 < vesselroutes.get(i).size(); o2++) {
                                System.out.println("Operation number: " + vesselroutes.get(i).get(o2).getID() + " Earliest start time: " +
                                        vesselroutes.get(i).get(o2).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o2).getLatestTime());
                            }
                        }
                    }

                     */
                    /*
                    System.out.println("Prev op "+orPrevious.getID()+" earliest time "+orPrevious.getEarliestTime());
                    System.out.println("current op "+orCurrent.getID());
                    System.out.println("Op prev time "+opPreviousTime);

                     */
                    int costN = SailingTimes[r][orPrevious.getEarliestTime() + opPreviousTime-1][orPrevious.getID() - 1][orCurrent.getID() - 1]*SailingCostForVessel[r];
                    int gainN;
                    gainN = operationGain[r][orCurrent.getID() - 1 - startNodes.length][orCurrent.getEarliestTime()-1];
                    routeSailingCost[r] += costN;
                    routeOperationGain[r] += gainN;
                    objValue-=costN;
                    objValue+=gainN;
                }
            }
            //System.out.println("OBJ: "+objValue);
        }
        return new ObjectiveValues(objValue,routeSailingCost,routeOperationGain);
    }

    public static void updatePrecedenceOver(Map<Integer,PrecedenceValues> precedenceOver, int insertIndex,
                                            List<Map<Integer, ConnectedValues>> simOpRoutes,
                                            Map<Integer,PrecedenceValues> precedenceOfOperations,
                                            Map<Integer,PrecedenceValues> precedenceOverOperations,
                                            int[][][] TimeVesselUseOnOperation, int [] startNodes,
                                            List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                                            List<Map<Integer,PrecedenceValues>> precedenceOfRoutes,
                                            Map<Integer,ConnectedValues> simultaneousOp,
                                            List<List<OperationInRoute>> vesselroutes,
                                            int [][][][] SailingTimes, int[][] twIntervals){
        if(precedenceOver!=null){
            for (PrecedenceValues pValues : precedenceOver.values()) {
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                int precedenceIndex =pValues.getIndex();
                if (secondOr != null) {
                    //System.out.println("First or "+firstOr.getID());
                    //System.out.println("second or "+secondOr.getID());
                    PrecedenceValues connectedOpPValues = precedenceOfOperations.get(secondOr.getID());
                    if(connectedOpPValues!=null) {
                        int routeConnectedOp = connectedOpPValues.getRoute();
                        int route = pValues.getRoute();
                        if (routeConnectedOp == pValues.getRoute()) {
                            continue;
                        }
                        int newESecondOr = firstOr.getEarliestTime() + TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                                [firstOr.getEarliestTime() - 1];
                        //System.out.println("first or earliest: "+firstOr.getEarliestTime());
                        //System.out.println("time vessel use on operation: "+TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                        //        [firstOr.getEarliestTime() - 1]);
                        int indexConnected = connectedOpPValues.getIndex();
                        if (insertIndex <= precedenceIndex) {
                            //System.out.println("Index demands update");
                            //System.out.println("Old earliest: " + secondOr.getEarliestTime());
                            //System.out.println("New earliest: " + newESecondOr);
                            //System.out.println(newESecondOr);
                            //System.out.println(secondOr.getEarliestTime());
                            if (secondOr.getEarliestTime() < newESecondOr) {
                                secondOr.setEarliestTime(newESecondOr);
                                updateEarliest(newESecondOr, indexConnected, routeConnectedOp, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"notLocal");
                                updatePrecedenceOver(precedenceOverRoutes.get(routeConnectedOp), connectedOpPValues.getIndex(), simOpRoutes, precedenceOfOperations,
                                        precedenceOverOperations, TimeVesselUseOnOperation, startNodes, precedenceOverRoutes,
                                        precedenceOfRoutes, simultaneousOp, vesselroutes, SailingTimes,twIntervals);
                                updateSimultaneous(simOpRoutes, routeConnectedOp, connectedOpPValues.getIndex(),
                                        simultaneousOp, precedenceOverRoutes, precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations,
                                        precedenceOfOperations, vesselroutes,twIntervals);
                            }
                            //System.out.println("update earliest because of precedence over");
                        }
                    }
                }
            }
        }
    }

    public static void updatePrecedenceOf(Map<Integer,PrecedenceValues> precedenceOf, int insertIndex,
                                          int[][][] TimeVesselUseOnOperation, int [] startNodes,
                                          List<Map<Integer, ConnectedValues>> simOpRoutes,
                                          Map<Integer,PrecedenceValues> precedenceOverOperations,
                                          Map<Integer,PrecedenceValues> precedenceOfOperations,
                                          List<Map<Integer,PrecedenceValues>> precedenceOfRoutes,List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                                          List<List<OperationInRoute>> vesselroutes, Map<Integer, ConnectedValues> simultaneousOp,
                                          int [][][][] SailingTimes,int[][] twIntervals){
        if(precedenceOf!=null){
            for (PrecedenceValues pValues : precedenceOf.values()) {
                int precedenceIndex =pValues.getIndex();
                OperationInRoute firstOr = pValues.getOperationObject();
                //System.out.println("FirstOr: "+firstOr.getID());
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                //System.out.println(" Second or: "+secondOr.getID()+ " with latest time " +secondOr.getLatestTime());
                PrecedenceValues connectedOpPValues = precedenceOverOperations.get(secondOr.getID());
                //System.out.println(connectedOpPValues);
                int routeConnectedOp = connectedOpPValues.getRoute();
                if (routeConnectedOp == pValues.getRoute()) {
                    continue;
                }
                int indexConnected = connectedOpPValues.getIndex();
                /*
                System.out.println(firstOr.getID() + " , " + firstOr.getLatestTime());
                System.out.println("Op time "+TimeVesselUseOnOperation[pValues.getConnectedRoute()][secondOr.getID() - startNodes.length - 1]
                        [secondOr.getLatestTime()-1]);

                 */
                int newLSecondOr = firstOr.getLatestTime() - TimeVesselUseOnOperation[pValues.getConnectedRoute()][secondOr.getID() - startNodes.length - 1]
                        [secondOr.getLatestTime()-1];

                //Oppdatert latest-tid
                if (insertIndex >= precedenceIndex) {
                    /*
                    System.out.println("Within UPDATE PRECEDENCE OF");
                    System.out.println("task "+secondOr.getID());
                    System.out.println("Old latest: " + secondOr.getLatestTime());
                    System.out.println("New latest: " + newLSecondOr);

                     */


                    if (secondOr.getLatestTime() > newLSecondOr) {
                        newLSecondOr = weatherLatestTimeSimPostInsert(newLSecondOr,secondOr.getEarliestTime(),TimeVesselUseOnOperation,routeConnectedOp,secondOr.getID(),startNodes,SailingTimes,indexConnected,
                                                            vesselroutes,simultaneousOp, precedenceOfOperations,precedenceOverOperations,-1,0,twIntervals);
                        //System.out.println("new l second or after weather update "+newLSecondOr);
                        secondOr.setLatestTime(newLSecondOr);
                        //System.out.println("index connected: "+indexConnected);
                        updateLatest(newLSecondOr, indexConnected, pValues.getConnectedRoute(),TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"notLocal",
                                    simultaneousOp, precedenceOfOperations,precedenceOverOperations,twIntervals);
                        updatePrecedenceOf(precedenceOfRoutes.get(routeConnectedOp),connectedOpPValues.getIndex(),TimeVesselUseOnOperation,startNodes,
                                simOpRoutes,precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,
                                vesselroutes,simultaneousOp,SailingTimes,twIntervals);
                        /*
                        System.out.println("within update precedence of ");
                        for (int i = 0; i < vesselroutes.size(); i++) {
                            System.out.println("VESSELINDEX " + i);
                            if (vesselroutes.get(i) != null) {
                                for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                                    System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                            vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                                }
                            }
                        }

                         */
                        updateSimultaneous(simOpRoutes,routeConnectedOp,connectedOpPValues.getIndex(),simultaneousOp, precedenceOverRoutes,
                                precedenceOfRoutes, TimeVesselUseOnOperation,startNodes,SailingTimes, precedenceOverOperations,precedenceOfOperations,vesselroutes,twIntervals);
                        //System.out.println("update latest because of precedence of");
                    }
                }
            }
        }
    }

    public static Boolean checkPOverFeasible(Map<Integer,PrecedenceValues> precedenceOver, int o, int insertIndex,int earliest,
                                             int []startNodes, int[][][] TimeVesselUseOnOperation, int nTimePeriods,
                                             int [][][][] SailingTimes, List<List<OperationInRoute>> vesselroutes,
                                             Map<Integer,PrecedenceValues> precedenceOfOperations,
                                             List<Map<Integer,PrecedenceValues>> precedenceOverRoutes) {
        if(precedenceOver!=null) {
            for (PrecedenceValues pValues : precedenceOver.values()) {
                int route = pValues.getRoute();
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                if (secondOr != null) {
                    //System.out.println("within precedence over");
                    PrecedenceValues connectedOpPValues = precedenceOfOperations.get(secondOr.getID());
                    //
                    // System.out.println("first operation: "+firstOr.getID());
                    int routeConnectedOp = connectedOpPValues.getRoute();
                    if (routeConnectedOp == pValues.getRoute()) {
                        continue;
                    }
                    int precedenceIndex = pValues.getIndex();
                    if (insertIndex <= precedenceIndex) {
                        int change = checkChangeEarliest(earliest, insertIndex, route, precedenceIndex, pValues.getOperationObject().getEarliestTime(), o,
                                                        startNodes,TimeVesselUseOnOperation,SailingTimes, vesselroutes);
                        if (change!=0) {
                            int t= firstOr.getEarliestTime()+change-1;
                            if(t>nTimePeriods-1){
                                t=nTimePeriods-1;
                            }
                            int newESecondOr=firstOr.getEarliestTime() + TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                                    [t] + change;
                            if(newESecondOr>secondOr.getEarliestTime()) {
                                if (newESecondOr > secondOr.getLatestTime()) {
                                    //System.out.println("NOT PRECEDENCE OVER FEASIBLE EARLIEST/LATEST P-OPERATION "+firstOr.getID());
                                    //System.out.println("Precedence over infeasible");
                                    return false;
                                }
                                else{
                                    int secondOrIndex= connectedOpPValues.getIndex();
                                    if (!checkPOverFeasible(precedenceOverRoutes.get(routeConnectedOp),secondOr.getID(),secondOrIndex,newESecondOr,
                                                            startNodes,TimeVesselUseOnOperation,nTimePeriods, SailingTimes, vesselroutes, precedenceOfOperations,precedenceOverRoutes)){
                                        //System.out.println("Precedence over infeasible");
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public static Boolean checkPOfFeasible(Map<Integer,PrecedenceValues> precedenceOf, int o, int insertIndex, int latest,
                                           int []startNodes, int[][][] TimeVesselUseOnOperation, int [][][][] SailingTimes,
                                           List<List<OperationInRoute>> vesselroutes,
                                           Map<Integer,PrecedenceValues> precedenceOverOperations, Map<Integer,PrecedenceValues> precedenceOfOperations,
                                           List<Map<Integer,PrecedenceValues>> precedenceOfRoutes, Map<Integer, ConnectedValues> simultaneousOp, int[][] twIntervals) {
        if(precedenceOf!=null) {
            for (PrecedenceValues pValues : precedenceOf.values()) {
                int route = pValues.getRoute();
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                PrecedenceValues connectedOpPValues = precedenceOverOperations.get(secondOr.getID());
                /*
                System.out.println("Within check p of feasible");
                System.out.println("first or id: "+firstOr.getID() + " , "+ firstOr.getLatestTime());
                System.out.println("second or id: "+secondOr.getID() + " , " + secondOr.getLatestTime());
                System.out.println("connected op p values: "+connectedOpPValues);

                 */

                int routeConnectedOp = connectedOpPValues.getRoute();
                if (routeConnectedOp == pValues.getRoute()) {
                    //System.out.println("inn her");
                    continue;
                }
                int precedenceIndex = pValues.getIndex();
                if (insertIndex > precedenceIndex) {
                    /*
                    System.out.println("ID: "+pValues.getOperationObject().getID());
                    System.out.println(latest + " , " + insertIndex + " , " + pValues.getIndex());

                     */
                    int change = checkChangeLatest(latest, insertIndex, route, pValues.getIndex(), pValues.getOperationObject().getLatestTime(), o,
                                                    startNodes,TimeVesselUseOnOperation,SailingTimes, vesselroutes, simultaneousOp,
                            precedenceOfOperations, precedenceOverOperations,twIntervals);
                    //System.out.println("change "+change);
                    //System.out.println(change);
                    if (change!=0) {
                        //System.out.println("Previous latest time for previous of "+firstOr.getLatestTime());
                        //System.out.println("change in previous of latest time "+change);
                        if(change==-1000){
                            return false;
                        }
                        int t =secondOr.getLatestTime()-change-1;
                        if(t<0 || t>1000){
                            t=0;
                        }
                        // System.out.println("First or "+firstOr.getID());
                        // System.out.println("Second or "+secondOr.getID());
                        int newLSecondOr = firstOr.getLatestTime() - change - TimeVesselUseOnOperation[pValues.getConnectedRoute()][secondOr.getID() - startNodes.length - 1][t];
                        // System.out.println("New L second or in pPOf feasibility check"+newLSecondOr);
                        // System.out.println("Old L second or in pPOf feasibility check"+secondOr.getLatestTime());
                        if (newLSecondOr < secondOr.getLatestTime()) {
                            //int newLSecondOr = secondOr.getLatestTime() - change
                            //System.out.println("earliest second "+secondOr.getEarliestTime());
                            if (newLSecondOr < secondOr.getEarliestTime()) {

                                //System.out.println("NOT PRECEDENCE OF FEASIBLE EARLIEST/LATEST P-OPERATION "+firstOr.getID());
                                //System.out.println("Precedence of infeasible");
                                return false;
                            }
                            else{
                                int secondOrIndex= connectedOpPValues.getIndex();
                                if (!checkPOfFeasible(precedenceOfRoutes.get(routeConnectedOp),secondOr.getID(),secondOrIndex,newLSecondOr,
                                                        startNodes,TimeVesselUseOnOperation,SailingTimes, vesselroutes,
                                        precedenceOverOperations, precedenceOfOperations,precedenceOfRoutes,simultaneousOp,twIntervals)){
                                    //System.out.println("Precedence of infeasible");
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public static int[] checkprecedenceOfEarliest(int o,int v, int earliestTemp, int[][] precedenceALNS, int[] startNodes,
                                                  Map<Integer,PrecedenceValues> precedenceOverOperations,int[][][] TimeVesselUseOnOperation){
        int breakValue=0;
        int precedenceOf=precedenceALNS[o-1-startNodes.length][1];
        if(precedenceOf!=0) {
            //System.out.println("Precedence of operation: "+o);
            PrecedenceValues pValues=precedenceOverOperations.get(precedenceOf);
            if (pValues == null) {
                breakValue=1;
            }
            if(breakValue==0) {
                int earliestPO = pValues.getOperationObject().getEarliestTime();
                //System.out.println("Earliest time before: "+earliestTemp);
                earliestTemp = Math.max(earliestTemp, earliestPO + TimeVesselUseOnOperation[pValues.getRoute()][precedenceOf - 1 - startNodes.length][earliestPO-1]);
                //System.out.println("Earliest time after: "+earliestTemp);
            }
        }
        return new int[]{earliestTemp,breakValue};
    }

    public static Boolean updateLatest(int latest, int indexInRoute, int routeIndex, int [][][] TimeVesselUseOnOperation,
                                    int [] startNodes, int [][][][] SailingTimes, List<List<OperationInRoute>> vesselroutes, String local, Map<Integer, ConnectedValues> simultaneousOp,
                                       Map<Integer, PrecedenceValues> precedenceOfOperations,Map<Integer, PrecedenceValues> precedenceOverOperations,int [][] twIntervals){
        int lastLatest=latest;
        //System.out.println("WITHIN UPDATE LATEST");
        //System.out.println("Last latest time: " + lastLatest);
        for(int k=indexInRoute-1;k>-1;k--) {
            /*
            System.out.println("K: "+k);
            System.out.println("SIZE route: "+String.valueOf(vesselroutes.get(routeIndex).size()-2));
            System.out.println("Index up dating: "+k);

             */
            OperationInRoute objectK = vesselroutes.get(routeIndex).get(k);
            if (local.equals("local")){
                //System.out.println("Infeasible in Construction.UpdateLatestTime");
                if (objectK.getLatestTime() - 1 < 0) {
                    return false;
                }
            }
            int opTimeK = TimeVesselUseOnOperation[routeIndex][objectK.getID() - startNodes.length - 1]
                    [objectK.getLatestTime() - 1];
            int updateSailingTime = 0;
            //System.out.println("ID operation "+ vesselroutes.get(routeIndex).get(k).getID() + " , " +"Route: "+ routeIndex);

            if (k == vesselroutes.get(routeIndex).size() - 2) {
                updateSailingTime = objectK.getLatestTime() ;
            }
            if (k < vesselroutes.get(routeIndex).size() - 2) {
                updateSailingTime = objectK.getLatestTime() + opTimeK;
            }
            //System.out.println("Latest already assigned K: "+ objectK.getLatestTime() + " , " + "Potential update latest K: "+
            //(lastLatest- SailingTimes[routeIndex][updateSailingTime-1][objectK.getID()-1]
                  //[vesselroutes.get(routeIndex).get(k+1).getID()-1] -opTimeK)) ;
            //System.out.println(updateSailingTime);
            if (local.equals("local")){
                //System.out.println("Infeasible in Construction.UpdateLatestTime");
                if (updateSailingTime - 1 < 0) {
                    return false;
                }
            }
            int newTime=Math.min(objectK.getLatestTime(),lastLatest-
                    SailingTimes[routeIndex][updateSailingTime-1][objectK.getID()-1]
                            [vesselroutes.get(routeIndex).get(k+1).getID()-1] -opTimeK);
            //System.out.println("New time: "+ newTime + " , " + "ID K: " +objectK.getID());
            if(newTime==objectK.getLatestTime()){
                break;
            }
            //Oppdatert latest-tid
            newTime = weatherLatestTimeSimPostInsert(newTime,objectK.getEarliestTime(),TimeVesselUseOnOperation,routeIndex,objectK.getID(),startNodes,SailingTimes,k,vesselroutes,simultaneousOp,
                                            precedenceOfOperations,precedenceOverOperations,-1,0,twIntervals);
            objectK.setLatestTime(newTime);
            //System.out.println(objectK.getLatestTime());
            lastLatest=newTime;
        }
        return true;
    }

    public static Boolean updateEarliest(int earliest, int indexInRoute, int routeIndex, int [][][] TimeVesselUseOnOperation,
                                      int[] startNodes, int [][][][] SailingTimes, List<List<OperationInRoute>> vesselroutes,
                                      String localSearch){
        int lastEarliest=earliest;
        /*
        for (int i = 0; i < vesselroutes.size(); i++) {
            System.out.println("VESSELINDEX " + i);
            if (vesselroutes.get(i) != null) {
                for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                    System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                            vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                }
            }
        }

         */
        for(int f=indexInRoute+1;f<vesselroutes.get(routeIndex).size();f++){
            OperationInRoute objectFMinus1=vesselroutes.get(routeIndex).get(f-1);
            OperationInRoute objectF=vesselroutes.get(routeIndex).get(f);
            int opTimeFMinus1=TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1]
                    [objectFMinus1.getEarliestTime()-1];
            if(localSearch.equals("local")){
                if(objectFMinus1.getEarliestTime()+opTimeFMinus1-1>59){
                    return false;
                }
            }
            /*
            System.out.println("task to be updated "+objectF.getID());
            System.out.println("Prev earliest "+vesselroutes.get(routeIndex).get(f-1).getEarliestTime());
            System.out.println("Maybe new earliest = last earliest: "+lastEarliest+" + sailing from last op "+objectFMinus1.getID()+": "+
                    SailingTimes[routeIndex][objectFMinus1.getEarliestTime()+opTimeFMinus1-1][objectFMinus1.getID()-1]
                    [objectF.getID()-1]+"+ op time last operation "+TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-
                    startNodes.length-1][objectFMinus1.getEarliestTime()-1]);
             */

            int newTime=Math.max(vesselroutes.get(routeIndex).get(f).getEarliestTime(),lastEarliest+
                    SailingTimes[routeIndex][objectFMinus1.getEarliestTime()+opTimeFMinus1-1][objectFMinus1.getID()-1]
                            [objectF.getID()-1]
                    +TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1][objectFMinus1.getEarliestTime()-1]);
            if(newTime==objectF.getEarliestTime()){
                break;
            }
            vesselroutes.get(routeIndex).get(f).setEarliestTime(newTime);
            /*
            System.out.println("Set earliest of "+vesselroutes.get(routeIndex).get(f).getID()+ " to "+newTime);
            System.out.println("Setting earliest time of operation: " + vesselroutes.get(routeIndex).get(f).getID() + " to " + vesselroutes.get(routeIndex).get(f).getEarliestTime());
            System.out.println("Actual earliest "+objectF.getEarliestTime());
            System.out.println();

             */
            lastEarliest=newTime;
        }
        return true;
    }

    public static Integer checkChangeEarliest(int earliestInsertionOperation, int indexInRoute, int routeIndex, int precedenceIndex, int earliestPrecedenceOperation,int o,
                                              int []startNodes, int[][][] TimeVesselUseOnOperation, int [][][][] SailingTimes,
                                              List<List<OperationInRoute>> vesselroutes){
        int lastEarliest=earliestInsertionOperation;
        for(int f=indexInRoute;f<vesselroutes.get(routeIndex).size();f++){
            int sailingtime;
            int operationtime;
            if (f==indexInRoute){
                int timeInsertionOperation=TimeVesselUseOnOperation[routeIndex][o-1-startNodes.length][earliestInsertionOperation-1];
                sailingtime= SailingTimes[routeIndex][earliestInsertionOperation+timeInsertionOperation-1][o-1]
                        [vesselroutes.get(routeIndex).get(f).getID()-1];
                operationtime=timeInsertionOperation;
            }
            else{
                OperationInRoute previous=vesselroutes.get(routeIndex).get(f-1);
                int timePreviousOperation=TimeVesselUseOnOperation[routeIndex][previous.getID()-1-startNodes.length][previous.getEarliestTime()-1];
                sailingtime=SailingTimes[routeIndex][previous.getEarliestTime()+timePreviousOperation-1][previous.getID()-1]
                        [vesselroutes.get(routeIndex).get(f).getID()-1];
                operationtime=timePreviousOperation;
            }
            int newTime=Math.max(vesselroutes.get(routeIndex).get(f).getEarliestTime(),lastEarliest+
                    sailingtime +operationtime);
            if(newTime==vesselroutes.get(routeIndex).get(f).getEarliestTime()){
                return 0;
            }
            lastEarliest=newTime;
            if(f==precedenceIndex) {
                break;
            }
        }
        return lastEarliest-earliestPrecedenceOperation;
    }

    public static Integer checkChangeLatest(int latestInsertionOperation, int indexInRoute, int routeIndex, int precedenceIndex,
                                            int latestPrecedenceOperation, int o, int []startNodes, int[][][] TimeVesselUseOnOperation,
                                            int [][][][] SailingTimes, List<List<OperationInRoute>> vesselroutes, Map<Integer, ConnectedValues> simultaneousOp,
                                            Map<Integer, PrecedenceValues> precedenceOfOperations,Map<Integer, PrecedenceValues> precedenceOverOperations,int[][] twIntervals){
        int returnValue=0;
        int lastLatest=latestInsertionOperation;
        int sailingTime;
        for(int k=indexInRoute-1;k>-1;k--){
            OperationInRoute kObject=vesselroutes.get(routeIndex).get(k);
            int timeOperationK=TimeVesselUseOnOperation[routeIndex][kObject.getID()-1-startNodes.length][kObject.getLatestTime()-1];
            if(k==indexInRoute-1){
                sailingTime=SailingTimes[routeIndex][Math.min(kObject.getLatestTime()+timeOperationK-1,SailingTimes[0].length-1)][kObject.getID()-1]
                        [o-1];
            }
            else{
                sailingTime=SailingTimes[routeIndex][kObject.getLatestTime()+timeOperationK-1][kObject.getID()-1]
                        [vesselroutes.get(routeIndex).get(k+1).getID()-1];
            }
            int newTime=Math.min(kObject.getLatestTime(),lastLatest- sailingTime
                    -timeOperationK);


            if(newTime==kObject.getLatestTime()){
                break;
            }
            lastLatest=newTime;
            if(k==indexInRoute-1){
                lastLatest = weatherLatestTimeSimPostInsert(lastLatest,kObject.getEarliestTime(),TimeVesselUseOnOperation,routeIndex,kObject.getID(),startNodes,SailingTimes,k,vesselroutes,
                        simultaneousOp,precedenceOfOperations,precedenceOverOperations,o, latestInsertionOperation,twIntervals);
            }else{
                lastLatest = weatherLatestTimeSimPostInsert(lastLatest,kObject.getEarliestTime(),TimeVesselUseOnOperation,routeIndex,kObject.getID(),startNodes,SailingTimes,k,vesselroutes,
                        simultaneousOp,precedenceOfOperations,precedenceOverOperations,-1,0,twIntervals);
            }

            if(lastLatest == -1000){
                returnValue = lastLatest;
                return returnValue;
            }
            if(k==precedenceIndex) {
                returnValue=latestPrecedenceOperation-lastLatest;
                break;
            }
        }
        return returnValue;
    }

    public static Integer checkChangeEarliestLastOperation(int earliestInsertionOperation, int earliestLastOperation, int indexInRoute, int routeIndex,int o,List<List<OperationInRoute>> vesselroutes,
                                                           int[][][] TimeVesselUseOnOperation, int[] startNodes, int [][][][] SailingTimes){
        if(indexInRoute==vesselroutes.get(routeIndex).size()){
            return 0;
        }
        int lastEarliest=earliestInsertionOperation;
        for(int f=indexInRoute;f<vesselroutes.get(routeIndex).size();f++){
            int sailingtime;
            int operationtime;
            if (f==indexInRoute){
                operationtime=TimeVesselUseOnOperation[routeIndex][o-startNodes.length-1][lastEarliest-1];
                sailingtime= SailingTimes[routeIndex][lastEarliest+operationtime-1][o-1]
                        [vesselroutes.get(routeIndex).get(f).getID()-1];
            }
            else{
                /*
                System.out.println("Last task "+vesselroutes.get(routeIndex).get(f-1).getID());
                System.out.println("Last earliest "+vesselroutes.get(routeIndex).get(f-1).getEarliestTime());

                 */
                int timeIndex=lastEarliest-1;
                /*
                if(timeIndex>59){
                    timeIndex=59;
                }
                 */
                operationtime=TimeVesselUseOnOperation[routeIndex][vesselroutes.get(routeIndex).get(f-1).getID()-startNodes.length-1][timeIndex];
                int timeIndex2=lastEarliest+operationtime-1;
                /*
                if(timeIndex2>59){
                    timeIndex2=59;
                }

                 */
                sailingtime=SailingTimes[routeIndex][timeIndex2][vesselroutes.get(routeIndex).get(f-1).getID()-1]
                        [vesselroutes.get(routeIndex).get(f).getID()-1];
            }
            int newTime=Math.max(vesselroutes.get(routeIndex).get(f).getEarliestTime(),lastEarliest+
                    sailingtime +operationtime);
            if(newTime==vesselroutes.get(routeIndex).get(f).getEarliestTime()){
                return 0;
            }
            lastEarliest=newTime;
        }
        return lastEarliest-earliestLastOperation;
    }

    public static int[] checkSimultaneousOfTimes(int o, int v, int earliestTemp, int latestTemp, int[][] simALNS, Map<Integer, ConnectedValues> simultaneousOp, int[] startNodes){
        int breakValue=0;
        int simultaneousOf=simALNS[o-1-startNodes.length][1];
        //System.out.println("Within check simultaneous of times");
        if(simultaneousOf!=0) {
            //System.out.println("Sim of operation: "+simultaneousOf);
            ConnectedValues sValues=simultaneousOp.get(simultaneousOf);
            if (sValues == null) {
                //System.out.println("break simultaneous of times");
                breakValue=1;
            }
            if(breakValue==0) {
                int earliestPO = sValues.getOperationObject().getEarliestTime();
                earliestTemp = Math.max(earliestTemp, earliestPO);
                int latestPO = sValues.getOperationObject().getLatestTime();
                latestTemp = Math.min(latestTemp, latestPO);
                //System.out.println("earliest and latest dependent on sim of operation");
            }
        }
        return new int[]{earliestTemp,latestTemp, breakValue};
    }

    public static Boolean checkSOfFeasible(int o, int v, int latestCurrent, int [] startNodes,
                                           int [][] simALNS, Map<Integer, ConnectedValues> simultaneousOp) {
        int simultaneousOf=simALNS[o-1-startNodes.length][1];
        if(simultaneousOf!=0) {
            ConnectedValues sValues=simultaneousOp.get(simultaneousOf);
            //System.out.println(sValues.getOperationObject().getID());
            if(sValues == null || sValues.getConnectedOperationObject() == null){
                return false;
            }
            int earliestPO = sValues.getConnectedOperationObject().getEarliestTime();
            if(latestCurrent < earliestPO){
                return false;
            }
        }
        return true;
    }


    public static void updateSimultaneous(List<Map<Integer, ConnectedValues>> simOpRoutes, int routeIndex, int indexInRoute,
                                          Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, PrecedenceValues>> precedenceOverRoutes,
                                          List<Map<Integer, PrecedenceValues>> precedenceOfRoutes, int[][][] TimeVesselUseOnOperation,
                                          int [] startNodes, int [][][][] SailingTimes, Map<Integer, PrecedenceValues> precedenceOverOperations,
                                          Map<Integer, PrecedenceValues> precedenceOfOperations, List<List<OperationInRoute>> vesselroutes, int[][] twIntervals) {
        //System.out.println("route index "+routeIndex);
        if(simOpRoutes.get(routeIndex)!=null){
            for (ConnectedValues sValues : simOpRoutes.get(routeIndex).values()) {
                /*
                System.out.println("Update caused by simultaneous " + sValues.getOperationObject().getID() + " in route " + routeIndex +
                        " with earliest time: " + sValues.getOperationObject().getEarliestTime()+" with latest time: " + sValues.getOperationObject().getLatestTime());
                for (int i = 0; i < vesselroutes.size(); i++) {
                    System.out.println("VESSELINDEX " + i);
                    if (vesselroutes.get(i) != null) {
                        for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                            System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                    vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                        }
                    }
                }

                 */

                int cur_earliestTemp = sValues.getOperationObject().getEarliestTime();
                int cur_latestTemp = sValues.getOperationObject().getLatestTime();

                int sIndex = sValues.getIndex();
                if(sValues.getConnectedOperationObject() != null) {
                    //System.out.println("Connected operation "+sValues.getConnectedOperationObject().getID());
                    OperationInRoute simOp = sValues.getConnectedOperationObject();
                    int earliestPO = simOp.getEarliestTime();
                    int earliestTemp = Math.max(cur_earliestTemp, earliestPO);
                    //System.out.println("Max of "+cur_earliestTemp+" and "+earliestPO);
                    //System.out.println("sVals earliest "+cur_earliestTemp + " . connected earliest" + earliestPO);
                    int latestPO = simOp.getLatestTime();
                    //System.out.println("sVals latest "+cur_latestTemp + " . connected latest" + latestPO);
                    int latestTemp = Math.min(cur_latestTemp, latestPO);
                    if (earliestTemp > cur_earliestTemp) {
                        cur_earliestTemp = earliestTemp;

                        /*
                        System.out.println("earliest if 1");
                        for (int i = 0; i < vesselroutes.size(); i++) {
                            System.out.println("VESSELINDEX " + i);
                            if (vesselroutes.get(i) != null) {
                                for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                                    System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                            vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                                }
                            }
                        }

                         */

                        sValues.getOperationObject().setEarliestTime(cur_earliestTemp);
                        /*
                        System.out.println("oppdaterer current op: " + sValues.getOperationObject().getID() + " med ny earliest tid " + sValues.getOperationObject().getEarliestTime());


                        for (int i = 0; i < vesselroutes.size(); i++) {
                            System.out.println("VESSELINDEX " + i);
                            if (vesselroutes.get(i) != null) {
                                for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                                    System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                            vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                                }
                            }
                        }
                        System.out.println("Index current "+sValues.getIndex());

                         */
                        updateEarliest(cur_earliestTemp,sValues.getIndex(),sValues.getRoute(),TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes,"notLocal");

                        /*
                        for (int i = 0; i < vesselroutes.size(); i++) {
                            System.out.println("VESSELINDEX " + i);
                            if (vesselroutes.get(i) != null) {
                                for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                                    System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                            vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                                }
                            }
                        }

                         */

                        updateSimultaneous(simOpRoutes,sValues.getRoute(),sValues.getIndex(),simultaneousOp,precedenceOverRoutes,
                                precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,precedenceOfOperations,vesselroutes,twIntervals);

                        updatePrecedenceOver(precedenceOverRoutes.get(sValues.getRoute()),sValues.getIndex(),simOpRoutes,precedenceOfOperations,
                                precedenceOverOperations,TimeVesselUseOnOperation,startNodes,precedenceOverRoutes,
                                precedenceOfRoutes,simultaneousOp,vesselroutes,SailingTimes,twIntervals);
                    }else if(earliestTemp>earliestPO){
                        //System.out.println(simOp);

                        /*
                        System.out.println("earliest if 2, sim op connected "+simOp.getID()+" new earliest "+cur_earliestTemp);
                        for (int i = 0; i < vesselroutes.size(); i++) {
                            System.out.println("VESSELINDEX " + i);
                            if (vesselroutes.get(i) != null) {
                                for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                                    System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                            vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                                }
                            }
                        }

                         */

                        ConnectedValues simOpObj = simultaneousOp.get(simOp.getID());
                        //System.out.println("Set op "+simOp.getID()+" earliest time to "+cur_earliestTemp);
                        simOpObj.getOperationObject().setEarliestTime(cur_earliestTemp);
                        updateEarliest(cur_earliestTemp,simOpObj.getIndex(),simOpObj.getRoute(), TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"notLocal");
                        updateSimultaneous(simOpRoutes,simOpObj.getRoute(),simOpObj.getIndex(),simultaneousOp,precedenceOverRoutes,
                                precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,precedenceOfOperations,vesselroutes,twIntervals);
                        updatePrecedenceOver(precedenceOverRoutes.get(simOpObj.getRoute()),simOpObj.getIndex(),simOpRoutes,precedenceOfOperations,
                                precedenceOverOperations,TimeVesselUseOnOperation,startNodes,precedenceOverRoutes,
                                precedenceOfRoutes,simultaneousOp,vesselroutes,SailingTimes,twIntervals);
                    }
                    if (latestTemp < cur_latestTemp) {
                        cur_latestTemp = latestTemp;

                        /*
                        System.out.println("Oppdaterer latest tid for sValues op "+sValues.getOperationObject().getID()+
                          " time: "+cur_latestTemp+", before weather update, old latest: "+latestTemp);

                         */


                        /*
                        for (int i = 0; i < vesselroutes.size(); i++) {
                            System.out.println("VESSELINDEX " + i);
                            if (vesselroutes.get(i) != null) {
                                for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                                    System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                            vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                                }
                            }
                        }

                         */

                        //Oppdatert latest-tid
                        // System.out.println("Cur latest before weather update "+cur_latestTemp);
                        cur_latestTemp = weatherLatestTimeSimPostInsert(cur_latestTemp,sValues.getOperationObject().getEarliestTime(),TimeVesselUseOnOperation,sValues.getRoute(),sValues.getOperationObject().getID(),
                                                            startNodes, SailingTimes,sValues.getIndex(),vesselroutes,simultaneousOp, precedenceOfOperations,precedenceOverOperations,-1,0,twIntervals);
                        //System.out.println("Cur latest after weather update "+cur_latestTemp);
                        sValues.getOperationObject().setLatestTime(cur_latestTemp);
                        //System.out.println("Set for op "+sValues.getOperationObject()+" latest time to "+cur_latestTemp);


                        //System.out.println("Set latest of "+sValues.getOperationObject().getID()+ " to "+cur_latestTemp);
                        updateLatest(cur_latestTemp,sValues.getIndex(),sValues.getRoute(), TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"notLocal",
                                    simultaneousOp, precedenceOfOperations,precedenceOverOperations,twIntervals);
                        updateSimultaneous(simOpRoutes,sValues.getRoute(),sValues.getIndex(),
                                simultaneousOp,precedenceOverRoutes, precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                                precedenceOfOperations,vesselroutes,twIntervals);
                        updatePrecedenceOf(precedenceOfRoutes.get(sValues.getRoute()),sValues.getIndex(),TimeVesselUseOnOperation,startNodes,simOpRoutes,
                                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,
                                vesselroutes,simultaneousOp,SailingTimes,twIntervals);
                    }else if(latestTemp<latestPO){
                        //System.out.println("Oppdaterer latest tid for current, set new latest for connected op "+simOp.getID());
                        //System.out.println(latestTemp + " , "+latestPO);
                        ConnectedValues simOpObj = simultaneousOp.get(simOp.getID());
                        //System.out.println("Latest of connected before update" + simOpObj.getOperationObject().getLatestTime());
                        //Oppdatert latest-tid
                        //System.out.println("cur_latestTemp before weather "+cur_latestTemp);

                        cur_latestTemp = weatherLatestTimeSimPostInsert(cur_latestTemp,simOpObj.getOperationObject().getEarliestTime(),TimeVesselUseOnOperation,simOpObj.getRoute(),simOpObj.getOperationObject().getID(),
                                                            startNodes,SailingTimes,simOpObj.getIndex(),vesselroutes,simultaneousOp, precedenceOfOperations,precedenceOverOperations,-1,0,twIntervals);
                        // System.out.println("Set latest of "+simOpObj.getOperationObject().getID()+ " to "+cur_latestTemp);
                        simOpObj.getOperationObject().setLatestTime(cur_latestTemp);
                        //System.out.println("Set for op "+simOpObj.getOperationObject()+" latest time to "+cur_latestTemp);

                        /*
                        System.out.println("After weather update "+cur_latestTemp);
                        for (int i = 0; i < vesselroutes.size(); i++) {
                            System.out.println("VESSELINDEX " + i);
                            if (vesselroutes.get(i) != null) {
                                for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                                    System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                            vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                                }
                            }
                        }

                         */

                        //System.out.println("oppdaterer: " + simOpObj.getOperationObject().getID() + " med ny latest tid " + simOpObj.getOperationObject().getLatestTime());
                        updateLatest(cur_latestTemp,simOpObj.getIndex(),simOpObj.getRoute(), TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"notLocal",
                                    simultaneousOp, precedenceOfOperations,precedenceOverOperations,twIntervals);
                        updateSimultaneous(simOpRoutes,simOpObj.getRoute(),simOpObj.getIndex(),
                                simultaneousOp,precedenceOverRoutes, precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                                precedenceOfOperations,vesselroutes,twIntervals);
                        updatePrecedenceOf(precedenceOfRoutes.get(simOpObj.getRoute()),simOpObj.getIndex(),TimeVesselUseOnOperation,startNodes,simOpRoutes,
                                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,
                                vesselroutes,simultaneousOp,SailingTimes,twIntervals);
                    }
                }
            }
        }
    }

    public static Boolean checkSimOpInRoute(Map<Integer,ConnectedValues> simOps, int o, int[][] simALNS,int[] startNodes){
        int simA = simALNS[o-startNodes.length-1][0];
        int simB = simALNS[o-startNodes.length-1][1];
        if(simOps!=null) {
            for (ConnectedValues op : simOps.values()) {
                if (simA != 0 || simB != 0) {
                    if (simA == op.getOperationObject().getID() ||
                            simB == op.getOperationObject().getID()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static Boolean checkSimultaneousFeasible(Map<Integer,ConnectedValues> simOps, int o, int v, int insertIndex, int earliestTemp,
                                                    int latestTemp, Map<Integer, ConnectedValues> simultaneousOp, int [][] simALNS,
                                                    int [] startNodes, int [][][][] SailingTimes, int [][][] TimeVesselUseOnOperation,
                                                    List<List<OperationInRoute>> vesselroutes, Map<Integer, PrecedenceValues> precedenceOverOperations,
                                                    Map<Integer, PrecedenceValues> precedenceOfOperations,int[][] twIntervals ){
        if(simOps!=null) {
            for (ConnectedValues op : simOps.values()) {
                //System.out.println("trying to insert operation " + o + " in position " + insertIndex+ " , " +op.getOperationObject().getID() + " simultaneous operation in route " +v + " with latest time " + op.getOperationObject().getLatestTime());
                ArrayList<ArrayList<Integer>> earliest_change = checkChangeEarliestSim(earliestTemp,insertIndex,v,o,op.getOperationObject().getID(),startNodes,
                                                                                        SailingTimes, TimeVesselUseOnOperation, simultaneousOp, vesselroutes);
                if (!earliest_change.isEmpty()) {
                    for (ArrayList<Integer> connectedTimes : earliest_change) {
                        //System.out.println(connectedTimes.get(0) + " , " + connectedTimes.get(1) + " earliest change");
                        if (connectedTimes.get(0) > connectedTimes.get(1)) {
                            //System.out.println("Sim infeasible");
                            return false;
                        }
                        ConnectedValues conOp = simultaneousOp.get(op.getConnectedOperationID());
                        //System.out.println(conOp.getOperationObject().getID() + " Con op operation ID " + conOp.getRoute() + " route index");
                        if(simALNS[o-startNodes.length-1][1] != 0 &&
                                simultaneousOp.get(simALNS[o-startNodes.length-1][1]).getRoute() == conOp.getRoute()){
                            //System.out.println(simultaneousOp.get(simALNS[o-startNodes.length-1][1]).getRoute() + " Con op of o ID" );
                            //System.out.println(conOp.getIndex());
                            if((simultaneousOp.get(simALNS[o-startNodes.length-1][1]).getIndex() - conOp.getIndex() > 0 &&
                                    insertIndex - op.getIndex() <= 0) || (simultaneousOp.get(simALNS[o-startNodes.length-1][1]).getIndex() -
                                    conOp.getIndex() <= 0 && insertIndex - op.getIndex() > 0)){
                                //System.out.println("Sim infeasible");
                                return false;
                            }
                        }
                    }
                }
                ArrayList<ArrayList<Integer>> latest_change = checkChangeLatestSim(latestTemp,insertIndex,v,o,op.getOperationObject().getID(),startNodes,
                                                                                    SailingTimes,TimeVesselUseOnOperation,
                        simultaneousOp,vesselroutes, precedenceOverOperations,precedenceOfOperations,twIntervals);
                if(!latest_change.isEmpty()){
                    for(ArrayList<Integer> connectedTimes : latest_change){
                        //System.out.println(connectedTimes.get(0) + " , " + connectedTimes.get(1) + " latest change");
                        if(connectedTimes.get(0) > connectedTimes.get(1)){
                            //System.out.println("Sim infeasible");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static ArrayList<ArrayList<Integer>> checkChangeEarliestSim(int earliestInsertionOperation, int indexInRoute,
                                                                       int routeIndex, int o, int simID, int[] startNodes,
                                                                       int [][][][] SailingTimes, int[][][] TimeVesselUseOnOperation,
                                                                       Map<Integer, ConnectedValues> simultaneousOp, List<List<OperationInRoute>> vesselroutes ){
        int lastEarliest=earliestInsertionOperation;
        ArrayList<ArrayList<Integer>> sim_earliests = new ArrayList<>();
        for(int f=indexInRoute;f<vesselroutes.get(routeIndex).size();f++){
            int sailingtime;
            int operationtime;
            if (f==indexInRoute){
                int timeInsertionOperation=TimeVesselUseOnOperation[routeIndex][o-1-startNodes.length][earliestInsertionOperation-1];
                sailingtime= SailingTimes[routeIndex][earliestInsertionOperation+timeInsertionOperation-1][o-1]
                        [vesselroutes.get(routeIndex).get(f).getID()-1];
                operationtime=timeInsertionOperation;
            }
            else{
                OperationInRoute previous=vesselroutes.get(routeIndex).get(f-1);
                int timePreviousOperation=TimeVesselUseOnOperation[routeIndex][previous.getID()-1-startNodes.length][previous.getEarliestTime()-1];
                sailingtime=SailingTimes[routeIndex][previous.getEarliestTime()+timePreviousOperation-1][previous.getID()-1]
                        [vesselroutes.get(routeIndex).get(f).getID()-1];
                operationtime=timePreviousOperation;
            }
            int newTime=Math.max(vesselroutes.get(routeIndex).get(f).getEarliestTime(),lastEarliest+
                    sailingtime +operationtime);
            if(newTime==vesselroutes.get(routeIndex).get(f).getEarliestTime()){
                return sim_earliests;
            }
            lastEarliest=newTime;
            if(vesselroutes.get(routeIndex).get(f).getID() == simID) {
                break;
            }
        }
        if(indexInRoute == vesselroutes.get(routeIndex).size()){
            OperationInRoute op = simultaneousOp.get(simID).getConnectedOperationObject();
            int new_earliest = op.getEarliestTime();
            int latest = op.getLatestTime();
            sim_earliests.add(new ArrayList<>(Arrays.asList(new_earliest,latest)));
            return sim_earliests;
        }
        OperationInRoute op = simultaneousOp.get(simID).getConnectedOperationObject();
        int new_earliest = Math.max(lastEarliest, op.getEarliestTime());
        int latest = op.getLatestTime();
        //System.out.println(op.getLatestTime() + " , " + op.getID());
        sim_earliests.add(new ArrayList<>(Arrays.asList(new_earliest,latest)));
        return sim_earliests;
    }

    public static ArrayList<ArrayList<Integer>> checkChangeLatestSim(int latestInsertionOperation, int indexInRoute, int routeIndex,
                                                                     int o, int simID, int[] startNodes,
                                                                     int [][][][] SailingTimes, int[][][] TimeVesselUseOnOperation,
                                                                     Map<Integer, ConnectedValues> simultaneousOp, List<List<OperationInRoute>> vesselroutes,
                                                                     Map<Integer, PrecedenceValues> precedenceOverOperations,
                                                                     Map<Integer, PrecedenceValues> precedenceOfOperations,int[][] twIntervals){
        ArrayList<ArrayList<Integer>> sim_latests = new ArrayList<>();
        int lastLatest=latestInsertionOperation;
        int sailingTime;
        int breakI=-1;
        for(int k=indexInRoute-1;k>-1;k--){
            OperationInRoute kObject=vesselroutes.get(routeIndex).get(k);
            //System.out.println(kObject.getLatestTime());
            //System.out.println(kObject.getID() + " , " + k);
            int timeOperationK=TimeVesselUseOnOperation[routeIndex][kObject.getID()-1-startNodes.length][kObject.getLatestTime()-1];
            if(k==indexInRoute-1){
                sailingTime=SailingTimes[routeIndex][Math.min(kObject.getLatestTime()+timeOperationK-1, SailingTimes[0].length-1)][kObject.getID()-1][o-1];
            }
            else{
                sailingTime=SailingTimes[routeIndex][kObject.getLatestTime()+timeOperationK-1][kObject.getID()-1]
                        [vesselroutes.get(routeIndex).get(k+1).getID()-1];
            }
            int newTime=Math.min(kObject.getLatestTime(),lastLatest- sailingTime
                    -timeOperationK);
            if(newTime==kObject.getLatestTime()){
                return sim_latests;
            }
            lastLatest=newTime;

            if(k==indexInRoute-1) {
                lastLatest = weatherLatestTimeSimPostInsert(lastLatest, kObject.getEarliestTime(), TimeVesselUseOnOperation, routeIndex, kObject.getID(), startNodes, SailingTimes, k, vesselroutes,
                        simultaneousOp, precedenceOfOperations, precedenceOverOperations,o,latestInsertionOperation,twIntervals);
            }else{
                lastLatest = weatherLatestTimeSimPostInsert(lastLatest, kObject.getEarliestTime(), TimeVesselUseOnOperation, routeIndex, kObject.getID(), startNodes, SailingTimes, k, vesselroutes,
                        simultaneousOp, precedenceOfOperations, precedenceOverOperations,-1,0,twIntervals);
            }

            if(lastLatest == -1000){
                return sim_latests;
            }
            if(vesselroutes.get(routeIndex).get(k).getID() == simID) {
                breakI=k;
                break;
            }
        }
        if(indexInRoute==0){
            return sim_latests;
        }
        OperationInRoute op = simultaneousOp.get(simID).getConnectedOperationObject();
        int new_latest = Math.min(lastLatest, op.getLatestTime());
        int earliest = op.getEarliestTime();
        sim_latests.add(new ArrayList<>(Arrays.asList(earliest,new_latest)));
        return sim_latests;
    }

    public static void updateConRoutes(Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, PrecedenceValues>> precedenceOfRoutes,
                                       List<Map<Integer, PrecedenceValues>> precedenceOverRoutes, int v, Map<Integer, PrecedenceValues> precedenceOverOperations,
                                       Map<Integer, PrecedenceValues> precedenceOfOperations, List<Map<Integer, ConnectedValues>> simOpRoutes,
                                       List<List<OperationInRoute>> vesselroutes, int[][][][]SailingTimes, int[] startNodes,
                                       int[][][]TimeVesselUseOnOperation, int[][] twIntervals, int[] EarliestStartingTimeForVessel){
        List<Integer> updatedRoutes = new ArrayList<>();
        if(!simultaneousOp.isEmpty()){
            for(ConnectedValues op : simultaneousOp.values()){
                int conRoute = op.getConnectedRoute();
                if(conRoute!= -1 && !updatedRoutes.contains(conRoute)) {
                    updatedRoutes.add(conRoute);
                    //System.out.println("Sim op add route: "+conRoute+ " operation: "+op.getOperationObject().getID());
                }
            }
        }
        if(!precedenceOfRoutes.get(v).isEmpty()){
            for(PrecedenceValues op : precedenceOfRoutes.get(v).values()){
                int conRoute = op.getConnectedRoute();
                if(conRoute!= -1 && !updatedRoutes.contains(conRoute)) {
                    updatedRoutes.add(conRoute);
                    //System.out.println("precedence of add route: "+conRoute+ " operation: "+op.getOperationObject().getID());
                }
            }
        }
        if(!precedenceOverRoutes.get(v).isEmpty()){
            for(PrecedenceValues op : precedenceOverRoutes.get(v).values()){
                int conRoute = op.getConnectedRoute();
                if(conRoute!= -1 && !updatedRoutes.contains(conRoute)) {
                    updatedRoutes.add(conRoute);
                    //System.out.println("precedence over add route: "+conRoute+ " operation: "+op.getOperationObject().getID());
                }
            }
        }

        for (int route : updatedRoutes) {
            if (vesselroutes.get(route) != null && vesselroutes.get(route).size() != 0) {
                //System.out.println("Updating route: " + route);
                int earliest = Math.max(SailingTimes[route][EarliestStartingTimeForVessel[route]][route]
                        [vesselroutes.get(route).get(0).getID() - 1] + 1, twIntervals[vesselroutes.get(route).get(0).getID() - startNodes.length - 1][0]);
                int latest = Math.min(SailingTimes[0].length, twIntervals
                        [vesselroutes.get(route).get(vesselroutes.get(route).size() - 1).getID() - 1 - startNodes.length][1]);
                vesselroutes.get(route).get(0).setEarliestTime(earliest);
                vesselroutes.get(route).get(vesselroutes.get(route).size() - 1).setLatestTime(latest);
                updateEarliestAfterRemoval(earliest, 0, route, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,twIntervals);
                updateLatestAfterRemoval(latest, vesselroutes.get(route).size() - 1, route, vesselroutes, TimeVesselUseOnOperation,
                        startNodes, SailingTimes,twIntervals, simultaneousOp, precedenceOverOperations,precedenceOfOperations);
            }
        }

        for (int route : updatedRoutes) {
            if (vesselroutes.get(route) != null && vesselroutes.get(route).size() != 0) {
                updatePrecedenceOver(precedenceOverRoutes.get(route), 0, simOpRoutes, precedenceOfOperations, precedenceOverOperations, TimeVesselUseOnOperation, startNodes, precedenceOverRoutes,
                        precedenceOfRoutes, simultaneousOp, vesselroutes, SailingTimes,twIntervals);
                updatePrecedenceOf(precedenceOfRoutes.get(route), vesselroutes.get(route).size() - 1, TimeVesselUseOnOperation, startNodes, simOpRoutes, precedenceOverOperations, precedenceOfOperations,
                        precedenceOfRoutes, precedenceOverRoutes, vesselroutes, simultaneousOp, SailingTimes,twIntervals);
                updateSimultaneous(simOpRoutes,route,0,simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,
                        startNodes,SailingTimes,precedenceOverOperations,precedenceOfOperations,vesselroutes,twIntervals);
            }
        }
    }

    public static void updateEarliestAfterRemoval(int earliest, int indexInRoute, int routeIndex, int [][][] TimeVesselUseOnOperation,
                                      int[] startNodes, int [][][][] SailingTimes, List<List<OperationInRoute>> vesselroutes,
                                                  int[][] twIntervals){
        int lastEarliest=earliest;
        for(int f=indexInRoute+1;f<vesselroutes.get(routeIndex).size();f++){
            OperationInRoute objectFMinus1=vesselroutes.get(routeIndex).get(f-1);
            OperationInRoute objectF=vesselroutes.get(routeIndex).get(f);
            int opTimeFMinus1=TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1]
                    [objectFMinus1.getEarliestTime()-1];
            int newTime=Math.max( lastEarliest+ SailingTimes[routeIndex][objectFMinus1.getEarliestTime()+opTimeFMinus1-1]
                    [objectFMinus1.getID()-1][objectF.getID()-1]
                    +TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1][objectFMinus1.getEarliestTime()-1] ,
                    twIntervals[objectF.getID()-1-startNodes.length][0]);

            vesselroutes.get(routeIndex).get(f).setEarliestTime(newTime);
            //System.out.println("Setting earliest time of: " + vesselroutes.get(routeIndex).get(f).getID() + " to " + vesselroutes.get(routeIndex).get(f).getEarliestTime() );
            lastEarliest=newTime;

        }
    }


    public static void updateLatestAfterRemoval(int latest, int indexInRoute, int routeIndex,
                                                List<List<OperationInRoute>> vesselroutes, int[][][] TimeVesselUseOnOperation, int[] startNodes,
                                                int[][][][] SailingTimes, int[][] twIntervals, Map<Integer, ConnectedValues> simultaneousOp, Map<Integer, PrecedenceValues> precedenceOverOperations,
                                                Map<Integer, PrecedenceValues> precedenceOfOperations){
        int lastLatest=latest;
        //System.out.println("WITHIN Update Latest After Removal");
        for(int k=indexInRoute-1;k>-1;k--){
            //System.out.println("on index k");
            OperationInRoute objectK=vesselroutes.get(routeIndex).get(k);
            int opTimeK=TimeVesselUseOnOperation[routeIndex][objectK.getID()-startNodes.length-1]
                    [objectK.getLatestTime()-1];
            int updateSailingTime=0;
            //System.out.println("Size of route: "+vesselroutes.get(routeIndex).size());
            if(k==vesselroutes.get(routeIndex).size()-2){
                updateSailingTime=objectK.getLatestTime() ;
            }
            if(k<vesselroutes.get(routeIndex).size()-2){
                updateSailingTime=objectK.getLatestTime()+opTimeK;
            }
            //System.out.println("New sailing time "+updateSailingTime);
            int newTime=Math.min(lastLatest-
                    SailingTimes[routeIndex][updateSailingTime-1][objectK.getID()-1]
                            [vesselroutes.get(routeIndex).get(k+1).getID()-1] -opTimeK,twIntervals[objectK.getID()-1-startNodes.length][1]);
            //System.out.println("New time: "+newTime + " , " +"ID K: "+objectK.getID());
            //Oppdatert latest-tid
            newTime = weatherLatestTimeSimPostInsert(newTime,objectK.getEarliestTime(),TimeVesselUseOnOperation,routeIndex,objectK.getID(),startNodes,SailingTimes,k,vesselroutes,simultaneousOp,precedenceOfOperations,
                                            precedenceOverOperations,-1,0,twIntervals);
            objectK.setLatestTime(newTime);
            lastLatest=newTime;
        }
    }

    public static void removeSimOf(ConnectedValues simOp,Map<Integer,ConnectedValues> simultaneousOp,
                                              List<List<OperationInRoute>> vesselroutes, int[][][] TimeVesselUseOnOperation, int[] startNodes,
                                              int[][][][] SailingTimes, int[][] twIntervals, List<OperationInRoute> unroutedTasks,
                                              List<Map<Integer, ConnectedValues>> simOpRoutes, Map<Integer, PrecedenceValues> precedenceOverOperations,
                                              Map<Integer, PrecedenceValues> precedenceOfOperations,List<Map<Integer, PrecedenceValues>> precedenceOverRoutes,
                                              List<Map<Integer, PrecedenceValues>> precedenceOfRoutes, int[] EarliestStartingTimeForVessel, int [][] precedenceALNS, int[][] bigTasksALNS){
        int prevEarliest=0;
        if(simOp.getIndex()>0){
            prevEarliest = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getEarliestTime();
            //System.out.println(prevEarliest + " prev earliest of operation " + vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getID());
        }
        if(simOp.getIndex()==0){
            if(vesselroutes.get(simOp.getRoute()).size()==1){
                prevEarliest= -1;
            }
            else {
                OperationInRoute firstOp = vesselroutes.get(simOp.getRoute()).get(1);
                int sailingTimeStartNodeToO = SailingTimes[simOp.getRoute()][EarliestStartingTimeForVessel[simOp.getRoute()]][simOp.getRoute()][firstOp.getID() - 1];
                prevEarliest = Math.max(EarliestStartingTimeForVessel[simOp.getRoute()] + sailingTimeStartNodeToO + 1, twIntervals[firstOp.getID() - startNodes.length - 1][0]);
                firstOp.setEarliestTime(prevEarliest);
            }
        }
        int simOpID=simOp.getOperationObject().getID();
        if(bigTasksALNS[simOpID-1-startNodes.length] == null){
            unroutedTasks.add(simOp.getOperationObject());
        }
        vesselroutes.get(simOp.getRoute()).remove(simOp.getIndex());
        simultaneousOp.remove(simOp.getOperationObject().getID());
        simOpRoutes.get(simOp.getRoute()).remove(simOp.getOperationObject().getID());
        int presOfOp=precedenceALNS[simOpID-1-startNodes.length][1];
        if(presOfOp!=0){
            precedenceOverOperations.get(presOfOp).setConnectedRoute(-1);
        }
        int nextLatest = 0;
        if (vesselroutes.get(simOp.getRoute()).size() > simOp.getIndex()) {
            nextLatest = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex()).getLatestTime();
        }
        if (simOp.getIndex() == vesselroutes.get(simOp.getRoute()).size()) {
            if(vesselroutes.get(simOp.getRoute()).size()==0){
                nextLatest= -1;
            }
            else {
                OperationInRoute lastOp = vesselroutes.get(simOp.getRoute()).get(vesselroutes.get(simOp.getRoute()).size() - 1);
                nextLatest = twIntervals[lastOp.getID() - startNodes.length - 1][1];
                lastOp.setLatestTime(nextLatest);
            }
        }
        updateIndexesRemoval(simOp.getRoute(),simOp.getIndex(), vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
        if(nextLatest!=-1 && prevEarliest!=-1) {
            updateEarliestAfterRemoval(prevEarliest, Math.max(simOp.getIndex() - 1, 0), simOp.getRoute(), TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals);
            updateLatestAfterRemoval(nextLatest, Math.min(simOp.getIndex(), vesselroutes.get(simOp.getRoute()).size() - 1), simOp.getRoute(), vesselroutes, TimeVesselUseOnOperation, startNodes,
                    SailingTimes, twIntervals, simultaneousOp,precedenceOverOperations,precedenceOfOperations);
            updateConRoutes(simultaneousOp,precedenceOfRoutes,precedenceOverRoutes,simOp.getRoute(),precedenceOverOperations,precedenceOfOperations,simOpRoutes, vesselroutes,
                    SailingTimes,startNodes,TimeVesselUseOnOperation,twIntervals,EarliestStartingTimeForVessel);
            updatePrecedenceOver(precedenceOverRoutes.get(simOp.getRoute()), simOp.getIndex(), simOpRoutes, precedenceOfOperations, precedenceOverOperations, TimeVesselUseOnOperation,
                    startNodes, precedenceOverRoutes, precedenceOfRoutes, simultaneousOp, vesselroutes, SailingTimes,twIntervals);
            updatePrecedenceOf(precedenceOfRoutes.get(simOp.getRoute()), simOp.getIndex(), TimeVesselUseOnOperation, startNodes, simOpRoutes,
                    precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, precedenceOverRoutes, vesselroutes, simultaneousOp, SailingTimes,twIntervals);
            updateSimultaneous(simOpRoutes,simOp.getRoute(),simOp.getIndex()-1,
                    simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                    precedenceOfOperations,vesselroutes,twIntervals);

            //updateSimultaneousAfterRemoval(simOpRoutes.get(simOp.getRoute()), simOp.getRoute(), simOp.getIndex() - 1,
            //        simultaneousOp, vesselroutes, TimeVesselUseOnOperation, startNodes, SailingTimes, twIntervals, EarliestStartingTimeForVessel);
        }

        //System.out.println("Update by removal VESSEL "+simOp.getRoute());
        /*
        for (int n = 0; n < vesselroutes.get(simOp.getRoute()).size(); n++) {
            //System.out.println("Number in order: "+n);
            //System.out.println("ID "+vesselroutes.get(simOp.getRoute()).get(n).getID());
            //System.out.println("Earliest starting time "+vesselroutes.get(simOp.getRoute()).get(n).getEarliestTime());
            //System.out.println("latest starting time "+vesselroutes.get(simOp.getRoute()).get(n).getLatestTime());
            //System.out.println(" ");
        }

         */
    }

    public static boolean checkPPlacement(int o, int n, int v,int[][] precedenceALNS, int[] startNodes, Map<Integer,PrecedenceValues> precedenceOverOperations,
                                          int[][] simALNS, Map<Integer,ConnectedValues> simultaneousOp){
        //FIRST THING THUESDAY: FIX THIS IN LSNINSERT!
        int precedenceOf=precedenceALNS[o-startNodes.length-1][1];
        if(precedenceOf!=0){
            PrecedenceValues pOver=precedenceOverOperations.get(precedenceOf);
            int precedenceOverOver=precedenceALNS[precedenceOf-startNodes.length-1][1];
            PrecedenceValues pOverOver=precedenceOverOperations.get(precedenceOverOver);
            int pOverSim=simALNS[precedenceOf-1-startNodes.length][1];
            ConnectedValues pOverSimValues= simultaneousOp.get(pOverSim);
            if(pOver.getRoute()==v){
                if(pOver.getIndex()>=n){
                    return false;
                }
            }
            if(pOverOver !=null){
                if(pOverOver.getRoute()==v){
                    if(pOverOver.getIndex()>=n){
                        return false;
                    }
                }
            }
            if(pOverSimValues !=null){
                if(pOverSimValues.getRoute()==v){
                    if(pOverSimValues.getIndex()>=n){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static ArrayList<Integer> updateConsolidatedOperations(int o, int routeIndex, ArrayList<Integer> removeConsolidatedSmallTasks, int[][] bigTasksALNS,
                                                                  int[] startNodes, Map<Integer,ConsolidatedValues> consolidatedOperations){
        if(bigTasksALNS[o-1-startNodes.length]!=null && bigTasksALNS[o- startNodes.length-1][0]==o){
            for(Integer small : bigTasksALNS[o-1-startNodes.length]){
                removeConsolidatedSmallTasks.add(small);
            }
            consolidatedOperations.put(o,new ConsolidatedValues(true,false,0,0,routeIndex));
        }
        else if(bigTasksALNS[o-1-startNodes.length]!=null && bigTasksALNS[o- startNodes.length-1][1]==o){
            consolidatedOperations.put(bigTasksALNS[o- startNodes.length-1][0],new ConsolidatedValues(false,true,routeIndex,0,0));
        }
        else if(bigTasksALNS[o-1-startNodes.length]!=null && bigTasksALNS[o- startNodes.length-1][2]==o){
            consolidatedOperations.replace(bigTasksALNS[o- startNodes.length-1][0],new ConsolidatedValues(false,
                    true,consolidatedOperations.get(bigTasksALNS[o- startNodes.length-1][0]).getConnectedRoute1(),routeIndex,0));
        }
        return removeConsolidatedSmallTasks;
    }

    public static void updateIndexesRemoval(int route, int index, List<List<OperationInRoute>> vesselroutes,
                                            Map<Integer,ConnectedValues> simultaneousOp, Map<Integer,PrecedenceValues> precedenceOverOperations,
                                            Map<Integer,PrecedenceValues> precedenceOfOperations){
        if(index<vesselroutes.get(route).size()){
            for(int i=index;i<vesselroutes.get(route).size();i++){
                //System.out.println("Evaluate index: "+index);
                OperationInRoute curOp=vesselroutes.get(route).get(i);
                int curOpId=curOp.getID();
                if(simultaneousOp.get(curOpId)!=null){
                    //System.out.println("index update because of sim");
                    int simIndex=simultaneousOp.get(curOpId).getIndex();
                    simultaneousOp.get(curOpId).setIndex(simIndex-1);
                }
                if(precedenceOverOperations.get(curOpId)!=null){
                    //System.out.println("index update because of pres over");
                    int pOverIndex=precedenceOverOperations.get(curOpId).getIndex();
                    precedenceOverOperations.get(curOpId).setIndex(pOverIndex-1);
                }
                if(precedenceOfOperations.get(curOpId)!=null){
                    //System.out.println("index update because of pres of");
                    int pOfIndex=precedenceOfOperations.get(curOpId).getIndex();
                    precedenceOfOperations.get(curOpId).setIndex(pOfIndex-1);
                }
            }
        }
    }

    public static void updateIndexesInsertion(int route, int index,List<List<OperationInRoute>> vesselroutes,
                                              Map<Integer,ConnectedValues> simultaneousOp, Map<Integer,PrecedenceValues> precedenceOverOperations,
                                              Map<Integer,PrecedenceValues> precedenceOfOperations){
        if(index+1<vesselroutes.get(route).size()){
            for(int i=index+1;i<vesselroutes.get(route).size();i++){
                //System.out.println("Evaluate index: "+index);
                OperationInRoute curOp=vesselroutes.get(route).get(i);
                int curOpId=curOp.getID();
                if(simultaneousOp.get(curOpId)!=null){
                    //System.out.println("index update because of sim");
                    int simIndex=simultaneousOp.get(curOpId).getIndex();
                    simultaneousOp.get(curOpId).setIndex(simIndex+1);
                }
                if(precedenceOverOperations.get(curOpId)!=null){
                    //System.out.println("index update because of pres over");
                    int pOverIndex=precedenceOverOperations.get(curOpId).getIndex();
                    precedenceOverOperations.get(curOpId).setIndex(pOverIndex+1);
                }
                if(precedenceOfOperations.get(curOpId)!=null){
                    //System.out.println("index update because of pres of");
                    int pOfIndex=precedenceOfOperations.get(curOpId).getIndex();
                    precedenceOfOperations.get(curOpId).setIndex(pOfIndex+1);
                }
            }
        }
    }

    public static int[] weatherFeasible(int [][][] timeVesselUseOnOperation, int v, int earliestTemp,
                                        int latestTemp, int o, int nTimePeriods, int[] startNodes){
        int [] startingTimes = new int[]{earliestTemp,latestTemp};
        int k = 1;
        boolean updated = false;
        if(earliestTemp < nTimePeriods && timeVesselUseOnOperation[v][o-startNodes.length-1][earliestTemp-1] == 10000){
            while(earliestTemp+k < latestTemp+1 && earliestTemp+k < nTimePeriods && !updated){
                if(timeVesselUseOnOperation[v][o-startNodes.length-1][earliestTemp+k-1] == 10000){
                    k++;
                }else{
                    startingTimes[0] = earliestTemp+k;
                    updated = true;
                }
            }
        }
        k = 1;
        if(latestTemp>0 && timeVesselUseOnOperation[v][o-startNodes.length-1][latestTemp-1] == 10000){
            if(earliestTemp == latestTemp){
                return null;
            }
            while(earliestTemp-1 < latestTemp-k && !updated){
                if(timeVesselUseOnOperation[v][o-startNodes.length-1][latestTemp-k-1] == 10000){
                    k++;
                }else{
                    startingTimes[1] = latestTemp-k;
                    updated = true;
                }
            }
        }
        return startingTimes;
    }


    public static int weatherLatestTimeSimPreInsert(int latestTemp, int earliestTemp, int [][][] timeVesselUseOnOperation, int v, int o, int[] startnodes,
                                        int[][][][] SailingTimes, int insertIndex, List<List<OperationInRoute>> vesselroutes,
                                        Map<Integer,ConnectedValues> simultaneousOp, Map<Integer,PrecedenceValues> precedenceOfOperations,
                                                    Map<Integer,PrecedenceValues> precedenceOverOperations, int toInsertObjectID){
        if(latestTemp-1 < 0) {
            return -1000;
        }
        int thisOp = timeVesselUseOnOperation[v][o - startnodes.length - 1][latestTemp - 1];
        //System.out.println(latestTemp + " , " + o);
        if(latestTemp+thisOp-1 > SailingTimes[0].length-1){
            return -1000;
        }
        int sailingOpToNext = SailingTimes[v][latestTemp + thisOp - 1][o - 1][vesselroutes.get(v).get(insertIndex).getID() - 1];
        int nextLatest = vesselroutes.get(v).get(insertIndex).getLatestTime();
        while (latestTemp + thisOp + sailingOpToNext > nextLatest) {
            if (earliestTemp < latestTemp) {
                latestTemp--;
                thisOp = timeVesselUseOnOperation[v][o - startnodes.length - 1][latestTemp - 1];
                sailingOpToNext = SailingTimes[v][latestTemp + thisOp - 1][o - 1][vesselroutes.get(v).get(insertIndex).getID() - 1];
                //System.out.println(latestTemp);
            } else {
                //System.out.println("Setting -1000 latest time for operation " + o + " in position , route " + insertIndex + " , " + v);
                return -1000;
            }
        }
        if (simultaneousOp.get(o) != null) {
            OperationInRoute conOp = simultaneousOp.get(o).getConnectedOperationObject();
            ConnectedValues CVconOp = simultaneousOp.get(conOp.getID());
            int conOpTime = timeVesselUseOnOperation[v][conOp.getID() - startnodes.length - 1][latestTemp - 1];
            int conOptoNext;
            int nextConOpLatest;
            if (vesselroutes.get(CVconOp.getRoute()).size()-1 == CVconOp.getIndex()){
                conOptoNext = 0;
                nextConOpLatest = 60;
            } else {
                conOptoNext = SailingTimes[simultaneousOp.get(conOp.getID()).getRoute()][latestTemp - 1][conOp.getID() - 1]
                        [vesselroutes.get(CVconOp.getRoute()).get(CVconOp.getIndex() + 1).getID() - 1];
                nextConOpLatest = vesselroutes.get(CVconOp.getRoute()).get(CVconOp.getIndex() + 1).getLatestTime();
            }
            while (latestTemp + conOpTime + conOptoNext > nextConOpLatest) {
                if (earliestTemp - 1 < latestTemp) {
                    latestTemp--;
                    //System.out.println("PROBLEM");
                } else {
                    //System.out.println("Setting -1000 latest time for operation " + o + " in position , route " + insertIndex + " , " + v);
                    return -1000;
                }
            }
        }
        //System.out.println(latestTemp + " latesttemp");
        if (precedenceOfOperations.get(o) != null){
            OperationInRoute precOp = precedenceOfOperations.get(o).getConnectedOperationObject();
            int precOpLatest = precOp.getLatestTime();
            int precOpTime = timeVesselUseOnOperation[v][precOp.getID() - startnodes.length - 1][precOpLatest-1];
            while(precOpLatest + precOpTime > latestTemp){
                //System.out.println(precOpTime);
                if(precOpLatest > precOp.getEarliestTime()-1) {
                    precOpLatest--;
                    //System.out.println(precOp.getID() + " , " + precOpLatest + " , " + precOp.getEarliestTime());
                    //precOpTime = timeVesselUseOnOperation[v][precOp.getID() - startnodes.length - 1][precOpLatest-1];
                    //System.out.println(timeVesselUseOnOperation[v][precOp.getID() - startnodes.length - 1][2-1]);
                }else{
                    //System.out.println("Infeasible because of precedence");
                    return -1000;
                }
            }
        }
        return latestTemp;
    }

    public static int weatherLatestTimeSimPostInsert(int latestTemp, int earliestTemp, int [][][] timeVesselUseOnOperation, int v, int o, int[] startnodes,
                                           int[][][][] SailingTimes, int insertIndex, List<List<OperationInRoute>> vesselroutes,
                                           Map<Integer,ConnectedValues> simultaneousOp, Map<Integer,PrecedenceValues> precedenceOfOperations,
                                           Map<Integer,PrecedenceValues> precedenceOverOperations, int toInsertObjectID, int toInsertObjectLatest, int[][] twIntervals){
        if(latestTemp < earliestTemp){
            int thisOp = timeVesselUseOnOperation[v][o- startnodes.length-1][earliestTemp-1];
            int thisOpToNext;
            if(toInsertObjectID != -1) {
                thisOpToNext = SailingTimes[v][earliestTemp - 1][o - 1][toInsertObjectID - 1];
                if(earliestTemp + thisOp + thisOpToNext <= toInsertObjectLatest){
                    return earliestTemp;
                }
            }else{
                Boolean last = false;
                /*
                for (int i = 0; i < vesselroutes.size(); i++) {
                    System.out.println("VESSELINDEX " + i);
                    if (vesselroutes.get(i) != null) {
                        for (int o2 = 0; o2 < vesselroutes.get(i).size(); o2++) {
                            System.out.println("Operation number: " + vesselroutes.get(i).get(o2).getID() + " Earliest start time: " +
                                    vesselroutes.get(i).get(o2).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o2).getLatestTime());
                        }
                    }
                }

                 */
                if(insertIndex==vesselroutes.get(v).size()-1){
                    last=true;
                }
                thisOpToNext=0;
                if(!last){
                thisOpToNext = SailingTimes[v][earliestTemp - 1][o - 1][vesselroutes.get(v).get(insertIndex + 1).getID() - 1];
                }
                else{
                    return earliestTemp;
                }
                if(earliestTemp + thisOp + thisOpToNext <= vesselroutes.get(v).get(insertIndex+1).getLatestTime()){
                    return earliestTemp;
                }
            }
        }

        if(latestTemp-1 < 0) {
            return -1000;
        }

        int thisOp = timeVesselUseOnOperation[v][o - startnodes.length - 1][latestTemp - 1];
        //System.out.println(latestTemp + " , " + o);
        /*
        if(latestTemp+thisOp-1 > SailingTimes[0].length-1){
            return -1000;
        }*/
        /*
        for (int i = 0; i < vesselroutes.size(); i++) {
            System.out.println("VESSELINDEX " + i);
            if (vesselroutes.get(i) != null) {
                for (int o2 = 0; o2 < vesselroutes.get(i).size(); o2++) {
                    System.out.println("Operation number: " + vesselroutes.get(i).get(o2).getID() + " Earliest start time: " +
                            vesselroutes.get(i).get(o2).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o2).getLatestTime());
                }
            }
        }

         */
        //System.out.println("Index "+insertIndex+" route "+v+" task "+o);
        int sailingOpToNext;
        int nextLatest;
        if(toInsertObjectID != -1) {
            sailingOpToNext = SailingTimes[v][latestTemp + thisOp - 1][o - 1][toInsertObjectID - 1];
            nextLatest = toInsertObjectLatest;
        }
        else if(insertIndex == vesselroutes.get(v).size()-1){
            sailingOpToNext = 0;
            nextLatest = SailingTimes[0].length;
            thisOp=0;
        }
        else{
            sailingOpToNext = SailingTimes[v][Math.min(twIntervals[o-1-startnodes.length][1],latestTemp + thisOp - 1)][o - 1][vesselroutes.get(v).get(insertIndex+1).getID() - 1];
            nextLatest = vesselroutes.get(v).get(insertIndex+1).getLatestTime();
        }
        while (latestTemp + thisOp + sailingOpToNext > nextLatest) {
            if (earliestTemp < latestTemp) {
                latestTemp--;
                thisOp = timeVesselUseOnOperation[v][o - startnodes.length - 1][latestTemp - 1];
                if(toInsertObjectID != -1) {
                    sailingOpToNext = SailingTimes[v][latestTemp + thisOp - 1][o - 1][toInsertObjectID - 1];
                }else if(insertIndex == vesselroutes.get(v).size()-1){
                    sailingOpToNext = 0;
                }else{
                    sailingOpToNext = SailingTimes[v][latestTemp + thisOp - 1][o - 1][vesselroutes.get(v).get(insertIndex+1).getID() - 1];
                }
            } else {
                //System.out.println("Setting -1000 latest time for operation " + o + " in position , route " + insertIndex + " , " + v);
                return -1000;
            }
        }
        if (simultaneousOp.get(o) != null && simultaneousOp.get(o).getConnectedOperationObject()!=null) {
            OperationInRoute conOp = simultaneousOp.get(o).getConnectedOperationObject();
            ConnectedValues CVconOp = simultaneousOp.get(conOp.getID());
            int conOpTime = timeVesselUseOnOperation[v][conOp.getID() - startnodes.length - 1][latestTemp - 1];
            int conOptoNext;
            int nextConOpLatest;
            if (vesselroutes.get(CVconOp.getRoute()).size()-1 == CVconOp.getIndex()){
                conOptoNext = 0;
                nextConOpLatest = SailingTimes[0].length;
            } else {
                conOptoNext = SailingTimes[simultaneousOp.get(conOp.getID()).getRoute()][latestTemp - 1][conOp.getID() - 1]
                        [vesselroutes.get(CVconOp.getRoute()).get(CVconOp.getIndex() + 1).getID() - 1];
                nextConOpLatest = vesselroutes.get(CVconOp.getRoute()).get(CVconOp.getIndex() + 1).getLatestTime();
            }
            while (latestTemp + conOpTime + conOptoNext > nextConOpLatest) {
                if (earliestTemp - 1 < latestTemp) {
                    latestTemp--;
                    //System.out.println("PROBLEM");
                } else {
                    //System.out.println("Setting -1000 latest time for operation " + o + " in position , route " + insertIndex + " , " + v);
                    return -1000;
                }
            }
        }
        if (precedenceOfOperations.get(o) != null){
            OperationInRoute precOp = precedenceOfOperations.get(o).getConnectedOperationObject();
            int precOpLatest = precOp.getLatestTime();
            int precOpTime = timeVesselUseOnOperation[v][precOp.getID() - startnodes.length - 1][precOpLatest-1];
            while(precOpLatest + precOpTime > latestTemp){
                //System.out.println(precOpTime);
                if(precOpLatest > precOp.getEarliestTime()-1) {
                    precOpLatest--;
                    //System.out.println(precOp.getID() + " , " + precOpLatest + " , " + precOp.getEarliestTime());
                    //precOpTime = timeVesselUseOnOperation[v][precOp.getID() - startnodes.length - 1][precOpLatest-1];
                    //System.out.println(timeVesselUseOnOperation[v][precOp.getID() - startnodes.length - 1][2-1]);
                }else{
                    //System.out.println("Infeasible because of precedence");
                    return -1000;
                }
            }
        }
        return latestTemp;
    }



    public static Boolean containsElement(int element, int[] list)   {
        boolean bol = false;
        for (Integer e: list)     {
            if(element == e){
                bol=true;
            }
        }
        return bol;
    }

    public static void printVessels(List<List<OperationInRoute>> vesselroutes, int[][][][] SailingTimes,
                                    int[][][] TimeVesselUseOnOperation,int[] startNodes){
        for (int i=0;i<vesselroutes.size();i++){
            int totalTime=0;
            if (vesselroutes.get(i)!=null) {
                //System.out.println("VESSEL ROUTE "+i);
                for (int o=0;o<vesselroutes.get(i).size();o++) {
                    //System.out.println("Operation number: "+vesselroutes.get(i).get(o).getID() + " Earliest start time: "+
                            //vesselroutes.get(i).get(o).getEarliestTime()+ " Latest Start time: "+ vesselroutes.get(i).get(o).getLatestTime());
                    if (o==0){
                        totalTime+=SailingTimes[i][0][i][vesselroutes.get(i).get(o).getID()-1];
                        totalTime+=TimeVesselUseOnOperation[i][vesselroutes.get(i).get(o).getID()-startNodes.length-1][0];
                        //System.out.println("temp total time: "+totalTime);
                    }
                    else{
                        totalTime+=SailingTimes[i][0][vesselroutes.get(i).get(o-1).getID()-1][vesselroutes.get(i).get(o).getID()-1];
                        if(o!=vesselroutes.get(i).size()-1) {
                            totalTime += TimeVesselUseOnOperation[i][vesselroutes.get(i).get(o).getID() - startNodes.length - 1][0];
                        }
                        //System.out.println("temp total time: "+totalTime);
                    }
                }
            }
            System.out.println("TOTAL DURATION FOR ROUTE: "+totalTime);
        }
    }

    public static void printSynchronizedDicts(Map<Integer, ConsolidatedValues> consolidatedOperations, Map<Integer,ConnectedValues> simultaneousOp,
                                              Map<Integer,PrecedenceValues> precedenceOverOperations, Map<Integer,PrecedenceValues> precedenceOfOperations){
        System.out.println("\nCONSOLIDATED DICTIONARY:");
        for(Map.Entry<Integer, ConsolidatedValues> entry : consolidatedOperations.entrySet()){
            ConsolidatedValues cv = entry.getValue();
            int key = entry.getKey();
            System.out.println("new entry in consolidated dictionary:");
            System.out.println("Key "+key);
            System.out.println("big task placed? "+cv.getConsolidated());
            System.out.println("small tasks placed? "+cv.getSmallTasks());
            System.out.println("small route 1 "+cv.getConnectedRoute1());
            System.out.println("small route 2 "+cv.getConnectedRoute2());
            System.out.println("route consolidated task "+cv.getConsolidatedRoute()+"\n");

        }
        System.out.println(" ");
        System.out.println("SIMULTANEOUS DICTIONARY");
        for(Map.Entry<Integer, ConnectedValues> entry : simultaneousOp.entrySet()){
            ConnectedValues simOp = entry.getValue();
            System.out.println("Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                    simOp.getRoute() + " with index: " + simOp.getIndex());
        }
        System.out.println("PRECEDENCE OVER DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOverOperations.entrySet()){
            PrecedenceValues presOverOp = entry.getValue();
            System.out.println("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                    presOverOp.getRoute() + " with index: " + presOverOp.getIndex());
        }
        System.out.println("PRECEDENCE OF DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfOperations.entrySet()){
            PrecedenceValues presOfOp = entry.getValue();
            System.out.println("Precedence of operation: " + presOfOp.getOperationObject().getID() + " in route: " +
                    presOfOp.getRoute() + " with index: " + presOfOp.getIndex());
        }
    }

    public void printInitialSolution(int[] vessseltypes){
        //PrintData.timeVesselUseOnOperations(TimeVesselUseOnOperation,startNodes.length);
        //PrintData.printSailingTimes(SailingTimes,3,nOperations-2*startNodes.length,startNodes.length);
        //PrintData.printOperationsForVessel(OperationsForVessel);
        PrintData.printPrecedenceALNS(precedenceALNS);
        PrintData.printSimALNS(simALNS);
        PrintData.printTimeWindows(timeWindowsForOperations);
        PrintData.printTimeWindowsIntervals(twIntervals);

        System.out.println("Sailing cost per route: "+Arrays.toString(routeSailingCost));
        System.out.println("Operation gain per route: "+Arrays.toString(routeOperationGain));
        int obj= IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum();
        System.out.println("Objective value: "+obj);
        for (int i=0;i<vesselroutes.size();i++){
            int totalTime=0;
            System.out.println("VESSELINDEX "+i+" VESSELTYPE "+vessseltypes[i]);
            if (vesselroutes.get(i)!=null) {
                for (int o=0;o<vesselroutes.get(i).size();o++) {
                    System.out.println("Operation number: "+vesselroutes.get(i).get(o).getID() + " Earliest start time: "+
                            vesselroutes.get(i).get(o).getEarliestTime()+ " Latest Start time: "+ vesselroutes.get(i).get(o).getLatestTime());
                    if (o==0){
                        totalTime+=SailingTimes[i][0][i][vesselroutes.get(i).get(o).getID()-1];
                        totalTime+=TimeVesselUseOnOperation[i][vesselroutes.get(i).get(o).getID()-startNodes.length-1][0];
                        //System.out.println("temp total time: "+totalTime);
                    }
                    else{
                        totalTime+=SailingTimes[i][0][vesselroutes.get(i).get(o-1).getID()-1][vesselroutes.get(i).get(o).getID()-1];
                        if(o!=vesselroutes.get(i).size()-1) {
                            totalTime += TimeVesselUseOnOperation[i][vesselroutes.get(i).get(o).getID() - startNodes.length - 1][0];
                        }
                        //System.out.println("temp total time: "+totalTime);
                    }
                }
            }
            System.out.println("TOTAL DURATION FOR ROUTE: "+totalTime);
        }
        if(!unroutedTasks.isEmpty()){
            System.out.println("UNROUTED TASKS");
            for(int n=0;n<unroutedTasks.size();n++) {
                System.out.println(unroutedTasks.get(n).getID());
            }
        }
        System.out.println("\nCONSOLIDATED DICTIONARY:");
        for(Map.Entry<Integer, ConsolidatedValues> entry : consolidatedOperations.entrySet()){
            ConsolidatedValues cv = entry.getValue();
            int key = entry.getKey();
            System.out.println("new entry in consolidated dictionary:");
            System.out.println("Key "+key);
            System.out.println("big task placed? "+cv.getConsolidated());
            System.out.println("small tasks placed? "+cv.getSmallTasks());
            System.out.println("small route 1 "+cv.getConnectedRoute1());
            System.out.println("small route 2 "+cv.getConnectedRoute2());
            System.out.println("route consolidated task "+cv.getConsolidatedRoute()+"\n");

        }
        System.out.println(" ");
        System.out.println("SIMULTANEOUS DICTIONARY");
        for(Map.Entry<Integer, ConnectedValues> entry : simultaneousOp.entrySet()){
            ConnectedValues simOp = entry.getValue();
            System.out.println("Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                    simOp.getRoute() + " with index: " + simOp.getIndex());
        }
        System.out.println("PRECEDENCE OVER DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOverOperations.entrySet()){
            PrecedenceValues presOverOp = entry.getValue();
            System.out.println("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                    presOverOp.getRoute() + " with index: " + presOverOp.getIndex());
        }
        System.out.println("PRECEDENCE OF DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfOperations.entrySet()){
            PrecedenceValues presOfOp = entry.getValue();
            System.out.println("Precedence of operation: " + presOfOp.getOperationObject().getID() + " in route: " +
                    presOfOp.getRoute() + " with index: " + presOfOp.getIndex());
        }
    }

    public Boolean checkSolution(List<List<OperationInRoute>> vesselroutes){
        for(List<OperationInRoute> route : vesselroutes){
            if(route != null) {
                for (OperationInRoute operation : route) {
                    if (operation.getEarliestTime() > operation.getLatestTime()) {
                        //System.out.println("Earliest time is larger than latest time, infeasible move");
                        return false;
                    }
                }
            }
        }
        if(!simultaneousOp.isEmpty()){
            for(ConnectedValues op : simultaneousOp.values()){
                OperationInRoute conOp = op.getConnectedOperationObject();
                if(op.getOperationObject().getEarliestTime() != conOp.getEarliestTime() ||
                        op.getOperationObject().getLatestTime() != conOp.getLatestTime()){
                    //System.out.println("Earliest and/or latest time for simultaneous op do not match, infeasible move");
                    return false;
                }
            }
        }
        if(!precedenceOverOperations.isEmpty()){
            for(PrecedenceValues op : precedenceOverOperations.values()){
                OperationInRoute conop = op.getConnectedOperationObject();
                if(conop != null) {
                    if (conop.getEarliestTime() < op.getOperationObject().getEarliestTime() + TimeVesselUseOnOperation[op.getRoute()]
                            [op.getOperationObject().getID() - 1-startNodes.length][op.getOperationObject().getEarliestTime()-1]) {
                        //System.out.println("Precedence infeasible, op: "+op.getOperationObject().getID()+ " and "+conop.getID());
                        return false;
                    }
                }
            }
        }
        return true;
    }


    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes = new int[]{3, 4, 5, 6};
        int[] startnodes = new int[]{3, 4, 5, 6};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes,
                "test_instances/25_3_locations_normalOpGenerator.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        ConstructionHeuristic a = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations(),dg.getOperationGainGurobi());
        PrintData.printSimALNS(dg.getSimultaneousALNS());
        PrintData.printPrecedenceALNS(dg.getPrecedenceALNS());
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),4);
        //PrintData.printTimeWindowsIntervals(dg.getTwIntervals());
        PrintData.printOperationsForVessel(dg.getOperationsForVessel());
        a.createSortedOperations();
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);
        System.out.println("Is solution feasible? "+a.checkSolution(a.getVesselroutes()));
        PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),startnodes.length);
        PrintData.printSailingTimes(dg.getSailingTimes(),2,dg.getOperationGain()[0].length,vesseltypes.length);

    }

    public List<OperationInRoute> getUnroutedTasks() {
        return unroutedTasks;
    }

    public List<List<OperationInRoute>> getVesselroutes() {
        return vesselroutes;
    }

    public int[] getRouteSailingCost() {
        return routeSailingCost;
    }

    public int[][][] getOperationGain() {
        return operationGain;
    }

    public int[] getRouteOperationGain() {
        return routeOperationGain;
    }

    public int getObjValue() {
        return objValue;
    }

    public Map<Integer, PrecedenceValues> getPrecedenceOverOperations() {
        return precedenceOverOperations;
    }

    public Map<Integer, PrecedenceValues> getPrecedenceOfOperations() {
        return precedenceOfOperations;
    }

    public Map<Integer, ConnectedValues> getSimultaneousOp() {
        return simultaneousOp;
    }

    public List<Map<Integer, ConnectedValues>> getSimOpRoutes() {
        return simOpRoutes;
    }

    public List<Map<Integer, PrecedenceValues>> getPrecedenceOfRoutes() {
        return precedenceOfRoutes;
    }

    public List<Map<Integer, PrecedenceValues>> getPrecedenceOverRoutes() {
        return precedenceOverRoutes;
    }

    public Map<Integer, ConsolidatedValues> getConsolidatedOperations() {
        return consolidatedOperations;
    }

}
