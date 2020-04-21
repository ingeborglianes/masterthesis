import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import java.util.stream.IntStream;

public class LargeNeighboorhoodSearchInsert {
    private Map<Integer, PrecedenceValues> precedenceOverOperations;
    private Map<Integer, PrecedenceValues> precedenceOfOperations;
    //map for operations that are connected as simultaneous operations. ID= operation number. Value= Simultaneous value.
    private Map<Integer, ConnectedValues> simultaneousOp;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;
    private List<Map<Integer, PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer, PrecedenceValues>> precedenceOverRoutes;
    private Map<Integer, ConsolidatedValues> consolidatedOperations;
    private List<OperationInRoute> unroutedTasks;
    private List<List<OperationInRoute>> vesselRoutes;
    private int[][] twIntervals;
    private int[][] precedenceALNS;
    private int[][] simALNS;
    private int[][] bigTasksALNS;
    private int[] startNodes;
    private int[][][][] SailingTimes;
    private int[][][] TimeVesselUseOnOperation;
    private int[] SailingCostForVessel;
    private int[] EarliestStartingTimeForVessel;
    private int[][][] operationGain;
    private int[] routeSailingCost;
    private int[] routeOperationGain;
    private int objValue;
    private int nVessels;
    private int nTimePeriods;
    private int [][] OperationsForVessel;
    Random generator;
    private List<List<InsertionValues>> allFeasibleInsertions = new ArrayList<List<InsertionValues>>();

    public LargeNeighboorhoodSearchInsert(Map<Integer, PrecedenceValues> precedenceOverOperations, Map<Integer, PrecedenceValues> precedenceOfOperations,
                                          Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, ConnectedValues>> simOpRoutes,
                                          List<Map<Integer, PrecedenceValues>> precedenceOfRoutes, List<Map<Integer, PrecedenceValues>> precedenceOverRoutes,
                                          Map<Integer, ConsolidatedValues> consolidatedOperations, List<OperationInRoute> unroutedTasks,
                                          List<List<OperationInRoute>> vesselRoutes, int[][] twIntervals,
                                          int[][] precedenceALNS, int[][] simALNS, int[] startNodes, int[][][][] SailingTimes,
                                          int[][][] TimeVesselUseOnOperation, int[] SailingCostForVessel, int[] EarliestStartingTimeForVessel,
                                          int[][][] operationGain, int[][] bigTasksALNS, int[][] OperationsForVessel, int randomSeed) {
        this.precedenceOverOperations = precedenceOverOperations;
        this.precedenceOfOperations = precedenceOfOperations;
        this.simultaneousOp = simultaneousOp;
        this.simOpRoutes = simOpRoutes;
        this.precedenceOfRoutes = precedenceOfRoutes;
        this.precedenceOverRoutes = precedenceOverRoutes;
        this.unroutedTasks = unroutedTasks;
        this.vesselRoutes = vesselRoutes;
        this.consolidatedOperations = consolidatedOperations;
        this.twIntervals = twIntervals;
        this.precedenceALNS = precedenceALNS;
        this.simALNS = simALNS;
        this.startNodes = startNodes;
        this.SailingTimes = SailingTimes;
        this.TimeVesselUseOnOperation = TimeVesselUseOnOperation;
        this.SailingCostForVessel = SailingCostForVessel;
        this.EarliestStartingTimeForVessel = EarliestStartingTimeForVessel;
        this.operationGain = operationGain;
        this.routeSailingCost = new int[vesselRoutes.size()];
        this.routeOperationGain = new int[vesselRoutes.size()];
        this.objValue = 0;
        this.bigTasksALNS = bigTasksALNS;
        this.generator = new Random(randomSeed);
        this.nVessels=vesselRoutes.size();
        this.nTimePeriods=TimeVesselUseOnOperation[0][0].length;
        this.OperationsForVessel=OperationsForVessel;
        for (int n = 0; n < simALNS.length; n++) {
            allFeasibleInsertions.add(null);
        }
        for (int i=1;i<this.unroutedTasks.size();i++){
            if(this.unroutedTasks.get(i-1).getID()<this.unroutedTasks.get(i).getID()){
                Collections.swap(unroutedTasks, i-1, i);
            }
        }
    }

    public void bestInsertion(){
        for(OperationInRoute our : unroutedTasks){
            
        }

    }

    public void regretInsertion(){

    }

    public int[] checkprecedenceOfEarliestLNS(int o, int earliestTemp, int earliestPO, int routeConnectedPrecedence){
        int breakValue=0;
        int precedenceOf=precedenceALNS[o-1-startNodes.length][1];
        if(precedenceOf!=0) {
            System.out.println("Precedence of operation: "+o);
            System.out.println("Earliest time before: "+earliestTemp);
            earliestTemp = Math.max(earliestTemp, earliestPO + TimeVesselUseOnOperation[routeConnectedPrecedence][precedenceOf - 1 - startNodes.length][earliestPO-1]);
            System.out.println("Earliest time after: "+earliestTemp);
        }
        return new int[]{earliestTemp,breakValue};
    }

    public int[] checkSimultaneousOfTimesLNS(int o, int v, int earliestTemp, int latestTemp, int earliestSO, int latestSO){
        int breakValue=0;
        int simultaneousOf=simALNS[o-1-startNodes.length][1];
        //System.out.println("Within check simultaneous of times");
        if(simultaneousOf!=0) {
            earliestTemp = Math.max(earliestTemp, earliestSO);
            latestTemp = Math.min(latestTemp, latestSO);
            //System.out.println("earliest and latest dependent on sim of operation");
        }
        return new int[]{earliestTemp,latestTemp, breakValue};
    }

    public void insertFeasibleDict(int o, int v, InsertionValues iv, int benefitIncreaseTemp){
        if (allFeasibleInsertions.get(o-1-startNodes.length) == null) {
            allFeasibleInsertions.set(o-1-startNodes.length, new ArrayList<>() {{
                add(iv);
            }});
        }
        else {
            if(benefitIncreaseTemp>=allFeasibleInsertions.get(o-1-startNodes.length).get(0).getBenenefitIncrease()){
                allFeasibleInsertions.get(v).add(iv);
            }
            else if(benefitIncreaseTemp<allFeasibleInsertions.get(o-1-startNodes.length).get(allFeasibleInsertions.size()-1).getBenenefitIncrease()){
                allFeasibleInsertions.get(v).add(allFeasibleInsertions.size(),iv);
            }
            else{
                for (int i=1;i<allFeasibleInsertions.size();i++){
                    if(benefitIncreaseTemp<allFeasibleInsertions.get(o-1-startNodes.length).get(i-1).getBenenefitIncrease()&&
                            benefitIncreaseTemp>=allFeasibleInsertions.get(o-1-startNodes.length).get(i).getBenenefitIncrease()){
                        allFeasibleInsertions.get(v).add(i,iv);
                        break;
                    }
                }
            }
        }
    }

    public void findInsertionCosts(OperationInRoute operationToInsert){
        //WHAT TO DO WITH CONSOLIDATED?
        int o=operationToInsert.getID();
        int benefitIncrease=-100000;
        int indexInRoute=0;
        int routeIndex=0;
        int earliest=0;
        int latest=nTimePeriods-1;
        int timeAdded=0;
        System.out.println("On operation: "+o);
        for (int v = 0; v < nVessels; v++) {
            Boolean notThisVessel = ConstructionHeuristic.checkSimOpInRoute(simOpRoutes.get(v),o,simALNS,startNodes);
            boolean precedenceOverFeasible=true;
            boolean precedenceOfFeasible=true;
            boolean simultaneousFeasible=true;
            if (DataGenerator.containsElement(o, OperationsForVessel[v]) && notThisVessel) {
                System.out.println("Try vessel "+v);
                if (vesselRoutes.get(v) == null) {
                    //System.out.println("Empty route");
                    //insertion into empty route
                    int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                    int timeIncrease=sailingTimeStartNodeToO;
                    int sailingCost=sailingTimeStartNodeToO*SailingCostForVessel[v];
                    int earliestTemp=Math.max(EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO+1,twIntervals[o-startNodes.length-1][0]);
                    int latestTemp=Math.min(nTimePeriods,twIntervals[o-startNodes.length-1][1]);
                    int[] precedenceOfValuesEarliest=ConstructionHeuristic.checkprecedenceOfEarliest(o,v,earliestTemp,
                            precedenceALNS,startNodes,precedenceOverOperations,TimeVesselUseOnOperation);
                    if (precedenceOfValuesEarliest[1]==1) {
                        //System.out.println("BREAK PRECEDENCE");
                    }
                    earliestTemp=precedenceOfValuesEarliest[0];
                    int [] simultaneousTimesValues = ConstructionHeuristic.checkSimultaneousOfTimes(o,v,earliestTemp,latestTemp,simALNS,simultaneousOp,startNodes);
                    //System.out.println(simultaneousTimesValues[0] + "," + simultaneousTimesValues[1]+ " sim time");
                    if(simultaneousTimesValues[2]==1){
                        //System.out.println("BREAK SIMULTANEOUS");
                    }
                    earliestTemp=simultaneousTimesValues[0];
                    latestTemp=simultaneousTimesValues[1];

                    if(earliestTemp<=latestTemp) {
                        int benefitIncreaseTemp=operationGain[v][o-startNodes.length-1][earliestTemp-1]-sailingCost;
                        if(simALNS[o-startNodes.length-1][0]!=0 || precedenceALNS[o-startNodes.length-1][0]!=0) {
                            if (benefitIncreaseTemp > 0) {
                                InsertionValues iv = new InsertionValues(benefitIncreaseTemp, v, 0, earliestTemp, latestTemp);
                                insertFeasibleDict(o, v, iv, benefitIncreaseTemp);
                            }
                        }
                        else{
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
                }
                else{
                    for(int n=0;n<vesselRoutes.get(v).size();n++){
                        System.out.println("On index "+n);
                        if(n==0) {
                            //check insertion in first position
                            int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                            int earliestTemp = Math.max(EarliestStartingTimeForVessel[v] + sailingTimeStartNodeToO + 1, twIntervals[o - startNodes.length - 1][0]);
                            int opTime=TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO];
                            int[] precedenceOfValuesEarliest=ConstructionHeuristic.checkprecedenceOfEarliest(o,v,earliestTemp,precedenceALNS,
                                    startNodes,precedenceOverOperations,TimeVesselUseOnOperation);
                            if (precedenceOfValuesEarliest[1]==1) {
                                //System.out.println("BREAK PRECEDENCE");
                                //System.out.println("precedence over task not placed");
                            }
                            earliestTemp=precedenceOfValuesEarliest[0];
                            if(earliestTemp<=60) {
                                int sailingTimeOToNext = SailingTimes[v][earliestTemp - 1][o - 1][vesselRoutes.get(v).get(0).getID() - 1];
                                int latestTemp = Math.min(vesselRoutes.get(v).get(0).getLatestTime() - sailingTimeOToNext - opTime,
                                        twIntervals[o - startNodes.length - 1][1]);
                                int[] simultaneousTimesValues = ConstructionHeuristic.checkSimultaneousOfTimes(o, v, earliestTemp, latestTemp,simALNS,
                                        simultaneousOp,startNodes);
                                if (simultaneousTimesValues[2] == 1) {
                                    //System.out.println("BREAK SIMULTANEOUS");
                                    //System.out.println("sim task not placed");
                                }
                                earliestTemp = simultaneousTimesValues[0];
                                latestTemp = simultaneousTimesValues[1];
                                int timeIncrease = sailingTimeStartNodeToO + sailingTimeOToNext
                                        - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselRoutes.get(v).get(0).getID() - 1];
                                int sailingCost = timeIncrease * SailingCostForVessel[v];
                                Boolean pPlacementFeasible = ConstructionHeuristic.checkPPlacement(o, n, v,precedenceALNS,startNodes,precedenceOverOperations);
                                if (earliestTemp <= latestTemp && pPlacementFeasible) {
                                    OperationInRoute lastOperation = vesselRoutes.get(v).get(vesselRoutes.get(v).size() - 1);
                                    int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                    int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, 0, v, o,vesselRoutes,
                                            TimeVesselUseOnOperation,startNodes,SailingTimes);
                                    int deltaOperationGainLastOperation = 0;
                                    if (changedTime > 0) {
                                        deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                    }
                                    int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                    if (benefitIncreaseTemp > 0) {
                                        precedenceOverFeasible = ConstructionHeuristic.checkPOverFeasible(precedenceOverRoutes.get(v), o, 0, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods,
                                                SailingTimes, vesselRoutes, precedenceOfOperations, precedenceOverRoutes);
                                        precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, 0, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                vesselRoutes, precedenceOverOperations, precedenceOfRoutes);
                                        simultaneousFeasible = ConstructionHeuristic.checkSimultaneousFeasible(simOpRoutes.get(v), o, v, 0, earliestTemp, latestTemp, simultaneousOp, simALNS,
                                                startNodes, SailingTimes, TimeVesselUseOnOperation,vesselRoutes);
                                        if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                            if(simALNS[o-startNodes.length-1][0]!=0 || precedenceALNS[o-startNodes.length-1][0]!=0) {
                                                InsertionValues iv = new InsertionValues(benefitIncreaseTemp, v, 0, earliestTemp, latestTemp);
                                                insertFeasibleDict(o, v, iv, benefitIncreaseTemp);
                                            }
                                            else{
                                                if(benefitIncreaseTemp>benefitIncrease) {
                                                    benefitIncrease = benefitIncreaseTemp;
                                                    routeIndex = v;
                                                    indexInRoute = 0;
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
                        if (n==vesselRoutes.get(v).size()-1){
                            //check insertion in last position
                            //System.out.println("Checking this position");
                            int earliestN=vesselRoutes.get(v).get(n).getEarliestTime();
                            int operationTimeN=TimeVesselUseOnOperation[v][vesselRoutes.get(v).get(n).getID()-1-startNodes.length][earliestN-1];
                            int startTimeSailingTimePrevToO=earliestN+operationTimeN;
                            if(startTimeSailingTimePrevToO >= nTimePeriods){
                                continue;
                            }
                            int sailingTimePrevToO=SailingTimes[v][startTimeSailingTimePrevToO-1]
                                    [vesselRoutes.get(v).get(n).getID()-1][o - 1];
                            int earliestTemp=Math.max(earliestN + operationTimeN + sailingTimePrevToO
                                    ,twIntervals[o-startNodes.length-1][0]);
                            int latestTemp=twIntervals[o-startNodes.length-1][1];
                            int[] precedenceOfValuesEarliest=ConstructionHeuristic.checkprecedenceOfEarliest(o,v,earliestTemp,precedenceALNS,
                                    startNodes,precedenceOverOperations,TimeVesselUseOnOperation);
                            if (precedenceOfValuesEarliest[1]==1) {
                                //System.out.println("BREAK PRECEDENCE");
                                System.out.println("precedence over task not placed");
                            }
                            earliestTemp=precedenceOfValuesEarliest[0];
                            if(earliestTemp<=60) {
                                int[] simultaneousTimesValues = ConstructionHeuristic.checkSimultaneousOfTimes(o, v, earliestTemp, latestTemp,simALNS,
                                        simultaneousOp,startNodes);
                                if (simultaneousTimesValues[2] == 1) {
                                    //System.out.println("BREAK SIMULTANEOUS");
                                    //System.out.println("sim task not placed");
                                }
                                earliestTemp = simultaneousTimesValues[0];
                                latestTemp = simultaneousTimesValues[1];

                                int timeIncrease = sailingTimePrevToO;
                                int sailingCost = timeIncrease * SailingCostForVessel[v];
                                if (earliestTemp <= latestTemp) {
                                    OperationInRoute lastOperation = vesselRoutes.get(v).get(vesselRoutes.get(v).size() - 1);
                                    int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                    int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, n + 1, v, o,vesselRoutes,
                                            TimeVesselUseOnOperation,startNodes,SailingTimes);
                                    int deltaOperationGainLastOperation = 0;
                                    if (changedTime > 0) {
                                        deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                    }
                                    int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                    if (benefitIncreaseTemp > 0) {
                                        precedenceOverFeasible = ConstructionHeuristic.checkPOverFeasible(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods, SailingTimes,
                                                vesselRoutes, precedenceOfOperations, precedenceOverRoutes);
                                        precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                vesselRoutes, precedenceOverOperations, precedenceOfRoutes);
                                        simultaneousFeasible = ConstructionHeuristic.checkSimultaneousFeasible(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp, simultaneousOp,
                                                simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation,vesselRoutes);
                                        if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                            if(simALNS[o-startNodes.length-1][0]!=0 || precedenceALNS[o-startNodes.length-1][0]!=0) {
                                                InsertionValues iv=new InsertionValues(benefitIncreaseTemp,v,n+1,earliestTemp,latestTemp);
                                                insertFeasibleDict(o,v,iv,benefitIncreaseTemp);
                                            }
                                            else{
                                                if(benefitIncreaseTemp>benefitIncrease) {
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
                        if(n<vesselRoutes.get(v).size()-1) {
                            //check insertion for all other positions in the route
                            int earliestN = vesselRoutes.get(v).get(n).getEarliestTime();
                            int operationTimeN = TimeVesselUseOnOperation[v][vesselRoutes.get(v).get(n).getID() - 1 - startNodes.length][earliestN - 1];
                            int startTimeSailingTimePrevToO = earliestN + operationTimeN;
                            int sailingTimePrevToO = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselRoutes.get(v).get(n).getID() - 1][o - 1];
                            int earliestTemp = Math.max(earliestN + sailingTimePrevToO + operationTimeN, twIntervals[o - startNodes.length - 1][0]);
                            int[] precedenceOfValuesEarliest = ConstructionHeuristic.checkprecedenceOfEarliest(o, v, earliestTemp,precedenceALNS,
                                    startNodes,precedenceOverOperations,TimeVesselUseOnOperation);
                            if (precedenceOfValuesEarliest[1] == 1) {
                                //System.out.println("BREAK PRECEDENCE");
                                //System.out.println("precedence over task not placed");
                            }
                            earliestTemp = precedenceOfValuesEarliest[0];
                            if(earliestTemp<=60) {
                                if (earliestTemp - 1 < nTimePeriods) {
                                    int opTime = TimeVesselUseOnOperation[v][o - 1 - startNodes.length][earliestTemp - 1];
                                    int sailingTimeOToNext = SailingTimes[v][Math.min(earliestTemp + opTime - 1, nTimePeriods - 1)][o - 1][vesselRoutes.get(v).get(n + 1).getID() - 1];

                                    int latestTemp = Math.min(vesselRoutes.get(v).get(n + 1).getLatestTime() -
                                            sailingTimeOToNext - opTime, twIntervals[o - startNodes.length - 1][1]);

                                    int[] simultaneousTimesValues = ConstructionHeuristic.checkSimultaneousOfTimes(o, v, earliestTemp, latestTemp,simALNS,
                                            simultaneousOp,startNodes);
                                    if (simultaneousTimesValues[2] == 1) {
                                        //System.out.println("BREAK SIMULTANEOUS");
                                        //System.out.println("sim task not placed");
                                    }
                                    earliestTemp = simultaneousTimesValues[0];
                                    latestTemp = simultaneousTimesValues[1];
                                    int sailingTimePrevToNext = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselRoutes.get(v).get(n).getID() - 1][vesselRoutes.get(v).get(n + 1).getID() - 1];
                                    int timeIncrease = sailingTimePrevToO + sailingTimeOToNext - sailingTimePrevToNext;
                                    int sailingCost = timeIncrease * SailingCostForVessel[v];
                                    Boolean pPlacementFeasible = ConstructionHeuristic.checkPPlacement(o, n + 1, v,precedenceALNS,startNodes,
                                            precedenceOverOperations);
                                    if (earliestTemp <= latestTemp && pPlacementFeasible) {
                                        OperationInRoute lastOperation = vesselRoutes.get(v).get(vesselRoutes.get(v).size() - 1);
                                        int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                        int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute,
                                                n + 1, v, o,vesselRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes);
                                        int deltaOperationGainLastOperation = 0;
                                        if (changedTime > 0) {
                                            deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                    operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                        }
                                        int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                        if (benefitIncreaseTemp > 0) {
                                            int currentLatest = vesselRoutes.get(v).get(n).getLatestTime();
                                            simultaneousFeasible = ConstructionHeuristic.checkSOfFeasible(o, v, currentLatest, startNodes, simALNS, simultaneousOp);
                                            if (simultaneousFeasible) {
                                                simultaneousFeasible = ConstructionHeuristic.checkSimultaneousFeasible(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp,
                                                        simultaneousOp, simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation,
                                                        vesselRoutes);
                                            }
                                            precedenceOverFeasible = ConstructionHeuristic.checkPOverFeasible(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation,
                                                    nTimePeriods, SailingTimes, vesselRoutes, precedenceOfOperations, precedenceOverRoutes);
                                            precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                    vesselRoutes, precedenceOverOperations, precedenceOfRoutes);
                                            if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                                if(simALNS[o-startNodes.length-1][0]!=0 || precedenceALNS[o-startNodes.length-1][0]!=0) {
                                                    InsertionValues iv=new InsertionValues(benefitIncreaseTemp,v,n+1,earliestTemp,latestTemp);
                                                    insertFeasibleDict(o,v,iv,benefitIncreaseTemp);
                                                }
                                                else{
                                                    if(benefitIncreaseTemp>benefitIncrease) {
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
        }
        if(!(simALNS[o-startNodes.length-1][0]!=0 || precedenceALNS[o-startNodes.length-1][0]!=0)){
            int finalBenefitIncrease = benefitIncrease;
            int finalIndexInRoute = indexInRoute;
            int finalRouteIndex = routeIndex;
            int finalEarliest = earliest;
            int finalLatest = latest;
            allFeasibleInsertions.set(o-1-startNodes.length, new ArrayList<>() {{
                add(new InsertionValues(finalBenefitIncrease, finalIndexInRoute, finalRouteIndex, finalEarliest, finalLatest));
            }});
        }
    }

    public void insertOperation(int o, int earliest, int latest, int indexInRoute, int routeIndex){
        //=ConstructionHeuristic.updateConsolidatedOperations(o,routeIndex,removeConsolidatedSmallTasks,bigTasksALNS,
        //        startNodes, consolidatedOperations);
        OperationInRoute newOr=new OperationInRoute(o, earliest, latest);
        int presOver=precedenceALNS[o-1-startNodes.length][0];
        int presOf=precedenceALNS[o-1-startNodes.length][1];
        if (presOver!=0){
            //System.out.println(o+" added in precedence operations dictionary 0 "+presOver);
            PrecedenceValues pValues= new PrecedenceValues(newOr,null,presOver,indexInRoute,routeIndex,0);
            precedenceOverOperations.put(o,pValues);
            precedenceOverRoutes.get(routeIndex).put(o,pValues);
        }
        if (presOf!=0){
            System.out.println("Operation precedence of: "+presOf);
            PrecedenceValues pValues= precedenceOverOperations.get(presOf);
            PrecedenceValues pValuesReplace=new PrecedenceValues(pValues.getOperationObject(),
                    newOr,pValues.getConnectedOperationID(),pValues.getIndex(),pValues.getRoute(),routeIndex);
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
            ConnectedValues sValue = new ConnectedValues(newOr, null,simB,indexInRoute,routeIndex, 0);
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
        System.out.println("NEW ADD: Vessel route "+routeIndex);
        System.out.println("Operation "+o);
        System.out.println("Earliest time "+ earliest);
        System.out.println("Latest time "+ latest);
        System.out.println("Route index "+indexInRoute);
        System.out.println(" ");


        if (vesselRoutes.get(routeIndex) == null) {
            int finalIndexInRoute = indexInRoute;
            vesselRoutes.set(routeIndex, new ArrayList<>() {{
                add(finalIndexInRoute, newOr);
            }});
        }
        else {
            vesselRoutes.get(routeIndex).add(indexInRoute,newOr);
        }
        ConstructionHeuristic.updateIndexesInsertion(routeIndex,indexInRoute, vesselRoutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
        //Update all earliest starting times forward
        ConstructionHeuristic.updateEarliest(earliest,indexInRoute,routeIndex, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes);
        ConstructionHeuristic.updateLatest(latest,indexInRoute,routeIndex, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes);
        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(routeIndex),indexInRoute,simOpRoutes,precedenceOfOperations,precedenceOverOperations,
                TimeVesselUseOnOperation, startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselRoutes,SailingTimes);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(routeIndex),indexInRoute,TimeVesselUseOnOperation,startNodes,simOpRoutes,
                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselRoutes,simultaneousOp,SailingTimes);
        ConstructionHeuristic.updateSimultaneous(simOpRoutes,routeIndex,indexInRoute,simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,
                startNodes,SailingTimes,precedenceOverOperations,precedenceOfOperations,vesselRoutes);
        for(int r=0;r<nVessels;r++) {
            System.out.println("VESSEL " + r);
            if(vesselRoutes.get(r) != null) {
                for (int n = 0; n < vesselRoutes.get(r).size(); n++) {
                    System.out.println("Number in order: " + n);
                    System.out.println("ID " + vesselRoutes.get(r).get(n).getID());
                    System.out.println("Earliest starting time " + vesselRoutes.get(r).get(n).getEarliestTime());
                    System.out.println("latest starting time " + vesselRoutes.get(r).get(n).getLatestTime());
                    System.out.println(" ");
                }
            }
        }
    }


    public void runLNSInsert(){
        ConstructionHeuristic.calculateObjective(vesselRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,SailingCostForVessel,
                EarliestStartingTimeForVessel, operationGain, routeSailingCost,routeOperationGain,objValue);
    }

    public void printLNSInsertSolution(int[] vessseltypes){
        //PrintData.timeVesselUseOnOperations(TimeVesselUseOnOperation,startNodes.length);
        //PrintData.printSailingTimes(SailingTimes,3,nOperations-2*startNodes.length,startNodes.length);
        //PrintData.printOperationsForVessel(OperationsForVessel);
        //PrintData.printPrecedenceALNS(precedenceALNS);
        //PrintData.printSimALNS(simALNS);
        //PrintData.printTimeWindows(timeWindowsForOperations);
        //PrintData.printTimeWindowsIntervals(twIntervals);

        System.out.println("SOLUTION AFTER LNS INSERT");

        System.out.println("Sailing cost per route: "+ Arrays.toString(routeSailingCost));
        System.out.println("Operation gain per route: "+Arrays.toString(routeOperationGain));
        int obj= IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum();
        System.out.println("Objective value: "+obj);
        for (int i=0;i<vesselRoutes.size();i++){
            int totalTime=0;
            System.out.println("VESSELINDEX "+i+" VESSELTYPE "+vessseltypes[i]);
            if (vesselRoutes.get(i)!=null) {
                for (int o=0;o<vesselRoutes.get(i).size();o++) {
                    System.out.println("Operation number: "+vesselRoutes.get(i).get(o).getID() + " Earliest start time: "+
                            vesselRoutes.get(i).get(o).getEarliestTime()+ " Latest Start time: "+ vesselRoutes.get(i).get(o).getLatestTime());
                    if (o==0){
                        totalTime+=SailingTimes[i][0][i][vesselRoutes.get(i).get(o).getID()-1];
                        totalTime+=TimeVesselUseOnOperation[i][vesselRoutes.get(i).get(o).getID()-startNodes.length-1][0];
                        //System.out.println("temp total time: "+totalTime);
                    }
                    else{
                        totalTime+=SailingTimes[i][0][vesselRoutes.get(i).get(o-1).getID()-1][vesselRoutes.get(i).get(o).getID()-1];
                        if(o!=vesselRoutes.get(i).size()-1) {
                            totalTime += TimeVesselUseOnOperation[i][vesselRoutes.get(i).get(o).getID() - startNodes.length - 1][0];
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

    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes = new int[]{1,2,3,4};
        int[] startnodes=new int[]{1,2,3,4};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes ,
                "test_instances/test_LNS.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),startnodes.length);
        //PrintData.printSailingTimes(dg.getSailingTimes(),2,17, 4);
        //PrintData.printSailingTimes(dg.getSailingTimes(),3,23, 4);
        ConstructionHeuristic a = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(),dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations());
        a.createSortedOperations();
        a.constructionHeuristic();
        //a.printInitialSolution(vesseltypes);
        LargeNeighboorhoodSearchRemoval LNSR = new LargeNeighboorhoodSearchRemoval(a.getPrecedenceOverOperations(),a.getPrecedenceOfOperations(),
                a.getSimultaneousOp(),a.getSimOpRoutes(),a.getPrecedenceOfRoutes(),a.getPrecedenceOverRoutes(),
                a.getConsolidatedOperations(),a.getUnroutedTasks(),a.getVesselroutes(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(), dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),5,21,dg.getDistOperationsInInstance(),
                0.08,0.5,0.01,0.1,
                0.1,0.1);
        LNSR.runLNSRemoval();
        System.out.println("-----------------");
        LNSR.printLNSSolution(vesseltypes);
        //PrintData.printSailingTimes(dg.getSailingTimes(),4,dg.getSimultaneousALNS().length,a.getVesselroutes().size());
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),dg.getStartNodes().length);
        LargeNeighboorhoodSearchInsert LNSI = new LargeNeighboorhoodSearchInsert(LNSR.getPrecedenceOverOperations(),LNSR.getPrecedenceOfOperations(),
                LNSR.getSimultaneousOp(),LNSR.getSimOpRoutes(),LNSR.getPrecedenceOfRoutes(),LNSR.getPrecedenceOverRoutes(),
                LNSR.getConsolidatedOperations(),LNSR.getUnroutedTasks(),LNSR.getVesselRoutes(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(), dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),dg.getOperationsForVessel(),5);
        System.out.println("-----------------");
        LNSI.printLNSInsertSolution(vesseltypes);
        //LNSI.runLNSInsert();
    }
}