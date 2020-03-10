import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.sql.SQLOutput;
import java.util.*;
import java.util.SortedSet;
import java.util.TreeSet;
public class ConstructionHeuristic {
    private int [][] OperationsForVessel;
    private int [][] TimeWindowsForOperations;
    private int [][][] Edges;
    private int [][][][] SailingTimes;
    private int [][][] TimeVesselUseOnOperation;
    private int [] EarliestStartingTimeForVessel;
    private int [] SailingCostForVessel;
    private int [] Penalty;
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
    //VesselTakenIntervals: Outer list: vessels. Inner list: list of intervals. Each interval is defined by four elements:
    //[earliest operation in interval][start time interval][end time interval][last operation in interval]
    //start time (fra og med), end time (til)
    List<List<int[]>> vesselTakenIntervals = new ArrayList<List<int[]>>();
    List<Integer> allOperations =  new ArrayList<Integer>();
    private int[] sortedOperations;
    private int [][] twIntervals;
    private int [][] precedenceALNS;
    private int [] simALNS;
    private int [][] bigTasksALNS;
    private int[] routeSailingCost;
    private int[][] timeWindowsForOperations;
    private int[] actionTime;
    //map with the earliest starting time of operations. ID= operation number. Value= earliest starting time.
    private Map<Integer,PrecedenceValues> precedenceOverOperations=new HashMap<>();
    private Map<Integer,PrecedenceValues> precedenceOfOperations=new HashMap<>();

    public ConstructionHeuristic(int [][] OperationsForVessel, int [][] TimeWindowsForOperations, int [][][] Edges, int [][][][] SailingTimes,
                int [][][] TimeVesselUseOnOperation, int [] EarliestStartingTimeForVessel,
                int [] SailingCostForVessel, int [] Penalty, int [][] Precedence, int [][] Simultaneous,
                int [] BigTasks, Map<Integer, List<Integer>> ConsolidatedTasks, int[] endNodes, int[] startNodes, double[] endPenaltyforVessel,
                int[][] twIntervals, int[][] precedenceALNS, int[] simALNS, int[][] bigTasksALNS, int[][] timeWindowsForOperations){
        this.OperationsForVessel=OperationsForVessel;
        this.TimeWindowsForOperations=TimeWindowsForOperations;
        this.Edges=Edges;
        this.SailingTimes=SailingTimes;
        this.TimeVesselUseOnOperation=TimeVesselUseOnOperation;
        this.EarliestStartingTimeForVessel=EarliestStartingTimeForVessel;
        this.SailingCostForVessel=SailingCostForVessel;
        this.Penalty=Penalty;
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
        this.routeSailingCost=new int[nVessels+1];
        for(int o = startNodes.length+1; o<nOperations-endNodes.length+1;o++){
            allOperations.add(o);
        }
        this.actionTime=new int[startNodes.length];
        this.timeWindowsForOperations=timeWindowsForOperations;
        System.out.println("Number of operations: "+(nOperations-startNodes.length*2));
        System.out.println("START NODES: "+Arrays.toString(this.startNodes));
        System.out.println("END NODES: "+Arrays.toString(this.endNodes));
    }

    public void createSortedOperations(){
        SortedSet<KeyValuePair> penaltiesDict = new TreeSet<KeyValuePair>();
        for (int p=0;p<this.Penalty.length;p++){
            System.out.println((p+1+startNodes.length)+" "+Penalty[p]);
            //Key value (= operation number) in penaltiesDict is not null indexed
            penaltiesDict.add(new KeyValuePair(p+1+startNodes.length,Penalty[p]));
        }
        int index=0;
        for (KeyValuePair keyValuePair : penaltiesDict) {
            sortedOperations[index]=keyValuePair.key;
            index+=1;
        }
        System.out.println(Arrays.toString(sortedOperations));
    }

    //TODO:
    //1. Time windows
    //2. Precedence
    //3. Synkronisering
    //4. Consolidated tasks

    public void constructionHeuristic(){
        for (int n = 0; n < nVessels; n++) {
            vesselroutes.add(null);
        }
        for (Integer o : sortedOperations){
            System.out.println("On operation: "+o);
            int timeAdded=100000;
            int indexInRoute=0;
            int routeIndex=0;
            int earliest=0;
            int latest=nTimePeriods;
            for (int v = 0; v < nVessels; v++) {
                if(actionTime[v]+TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]>60){
                    continue;
                }
                List<PrecedenceValues> precedenceOver= checkPrecedence(v,0);
                List<PrecedenceValues> precedenceOf= checkPrecedence(v,1);
                boolean precedenceOverFeasible=true;
                boolean precedenceOfFeasible=true;
                if (DataGenerator.containsElement(o, OperationsForVessel[v])) {
                    if (vesselroutes.get(v) == null) {
                        //insertion into empty route
                        int timeIncrease=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                        int earliestTemp=Math.max(EarliestStartingTimeForVessel[v]+SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1]+1,twIntervals[o-startNodes.length-1][0]);
                        int latestTemp=Math.min(nTimePeriods,twIntervals[o-startNodes.length-1][1]);
                        int[] precedenceOfValues=checkprecedenceOf(o,v,earliestTemp,latestTemp);
                        if (precedenceOfValues[2]==1) {
                            break;
                        }
                        earliestTemp=precedenceOfValues[0];
                        latestTemp=precedenceOfValues[1];
                        if(timeIncrease < timeAdded && earliestTemp<=latestTemp) {
                            timeAdded = timeIncrease;
                            routeIndex = v;
                            indexInRoute = 0;
                            earliest = earliestTemp;
                            latest = latestTemp;
                        }
                    }
                    else{
                        for(int n=0;n<vesselroutes.get(v).size();n++){
                            if(n==0) {
                                //check insertion in first position
                                int timeIncrease=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1]
                                        + SailingTimes[v][EarliestStartingTimeForVessel[v]][o - 1][vesselroutes.get(v).get(0).getID() - 1]
                                        - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselroutes.get(v).get(0).getID() - 1];
                                int earliestTemp = Math.max(EarliestStartingTimeForVessel[v] + SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1] + 1, twIntervals[o - startNodes.length - 1][0]);
                                int latestTemp = Math.min(vesselroutes.get(v).get(0).getLatestTime() - SailingTimes[v][EarliestStartingTimeForVessel[v]][o - 1]
                                                [vesselroutes.get(v).get(0).getID() - 1]
                                                - TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]
                                        , twIntervals[o - startNodes.length - 1][1]);
                                int[] precedenceOfValues=checkprecedenceOf(o,v,earliestTemp,latestTemp);
                                if (precedenceOfValues[2]==1) {
                                    break;
                                }
                                earliestTemp=precedenceOfValues[0];
                                latestTemp=precedenceOfValues[1];
                                precedenceOverFeasible= checkPOverFeasible(precedenceOver, o, timeIncrease,0, earliestTemp);
                                precedenceOfFeasible= checkPOfFeasible(precedenceOf, o, timeIncrease,0,latestTemp);
                                if(timeIncrease < timeAdded && earliestTemp<=latestTemp && precedenceOverFeasible && precedenceOfFeasible && earliestTemp<vesselroutes.get(v).get(0).getEarliestTime()) {
                                    timeAdded = timeIncrease;
                                    routeIndex = v;
                                    indexInRoute = 0;
                                    earliest = earliestTemp;
                                    latest = latestTemp;
                                }
                            }
                            if (n==vesselroutes.get(v).size()-1){
                                //check insertion in last position
                                int timeIncrease=SailingTimes[v][EarliestStartingTimeForVessel[v]]
                                        [vesselroutes.get(v).get(n).getID()-1][o - 1];
                                int earliestTemp=Math.max(vesselroutes.get(v).get(n).getEarliestTime()+
                                                SailingTimes[v][EarliestStartingTimeForVessel[v]]
                                                        [vesselroutes.get(v).get(n).getID()-1][o - 1]
                                                +TimeVesselUseOnOperation[v][vesselroutes.get(v).get(n).getID()-1-startNodes.length]
                                                [EarliestStartingTimeForVessel[v]]
                                        ,twIntervals[o-startNodes.length-1][0]);
                                int latestTemp=twIntervals[o-startNodes.length-1][1];
                                int[] precedenceOfValues=checkprecedenceOf(o,v,earliestTemp,latestTemp);
                                if (precedenceOfValues[2]==1) {
                                    break;
                                }
                                earliestTemp=precedenceOfValues[0];
                                latestTemp=precedenceOfValues[1];
                                precedenceOverFeasible= checkPOverFeasible(precedenceOver, o, timeIncrease,n+1,earliestTemp);
                                precedenceOfFeasible= checkPOfFeasible(precedenceOf, o, timeIncrease,n+1,latestTemp);
                                if(timeIncrease < timeAdded && earliestTemp<=latestTemp && precedenceOverFeasible && precedenceOfFeasible) {
                                    timeAdded = timeIncrease;
                                    routeIndex = v;
                                    indexInRoute = n+1;
                                    earliest = earliestTemp;
                                    latest = latestTemp;
                                }
                            }
                            if(n!=0 && n!=vesselroutes.get(v).size()-1){
                                //check insertion for all other positions in the route
                                int timeIncrease=SailingTimes[v][EarliestStartingTimeForVessel[v]][vesselroutes.get(v).get(n).getID()-1][o - 1]
                                        +SailingTimes[v][EarliestStartingTimeForVessel[v]][o-1][vesselroutes.get(v).get(n+1).getID()-1]
                                        - SailingTimes[v][EarliestStartingTimeForVessel[v]][vesselroutes.get(v).get(n).getID()-1][vesselroutes.get(v).get(n+1).getID()-1];
                                int earliestTemp=Math.max(vesselroutes.get(v).get(n).getEarliestTime()+
                                                SailingTimes[v][EarliestStartingTimeForVessel[v]]
                                                        [vesselroutes.get(v).get(n).getID()-1][o - 1]
                                                +TimeVesselUseOnOperation[v][vesselroutes.get(v).get(n).getID()-1-startNodes.length]
                                                [EarliestStartingTimeForVessel[v]]
                                        ,twIntervals[o-startNodes.length-1][0]);
                                int latestTemp=Math.min(vesselroutes.get(v).get(n+1).getLatestTime()-
                                        SailingTimes[v][EarliestStartingTimeForVessel[v]]
                                                [o - 1][vesselroutes.get(v).get(n+1).getID()-1]-
                                        TimeVesselUseOnOperation[v][o-startNodes.length-1]
                                                [EarliestStartingTimeForVessel[v]],twIntervals[o-startNodes.length-1][1]);
                                int[] precedenceOfValues=checkprecedenceOf(o,v,earliestTemp,latestTemp);
                                if (precedenceOfValues[2]==1) {
                                    break;
                                }
                                earliestTemp=precedenceOfValues[0];
                                latestTemp=precedenceOfValues[1];
                                precedenceOverFeasible= checkPOverFeasible(precedenceOver, o, timeIncrease,n+1,earliestTemp);
                                precedenceOfFeasible= checkPOfFeasible(precedenceOf, o, timeIncrease,n+1,latestTemp);
                                if(timeIncrease < timeAdded && earliestTemp<=latestTemp && precedenceOverFeasible && precedenceOfFeasible && earliestTemp<vesselroutes.get(v).get(n+1).getEarliestTime()) {
                                    timeAdded = timeIncrease;
                                    routeIndex = v;
                                    indexInRoute = n+1;
                                    earliest = earliestTemp;
                                    latest = latestTemp;
                                }
                            }
                        }
                    }
                }
            }
            //After iterating through all possible insertion places, we here add the operation at the best insertion place
            if(timeAdded!=100000) {
                if(vesselroutes.get(routeIndex)!= null && indexInRoute!=vesselroutes.get(routeIndex).size()-1){
                    actionTime[routeIndex]+=timeAdded;
                }
                else{
                    actionTime[routeIndex]+=timeAdded+TimeVesselUseOnOperation[routeIndex][o-startNodes.length-1][EarliestStartingTimeForVessel[routeIndex]];
                }
                OperationInRoute newOr=new OperationInRoute(o, earliest, latest);
                int presOver=precedenceALNS[o-1-startNodes.length][0];
                int presOf=precedenceALNS[o-1-startNodes.length][1];
                if (presOver!=0){
                    //System.out.println(o+" added in precedence operations dictionary 0 "+presOver);
                    PrecedenceValues pValues= new PrecedenceValues(newOr,null,presOver,indexInRoute,routeIndex,0);
                    precedenceOverOperations.put(o,pValues);
                }
                if (presOf!=0){
                    for (Map.Entry<Integer,PrecedenceValues> entry : precedenceOverOperations.entrySet()) {
                        int pKey = entry.getKey();
                        PrecedenceValues pValues = entry.getValue();
                        System.out.println(pValues.getConnectedOperationObject());
                        if (pValues.getConnectedOperationID()==o){
                            precedenceOverOperations.replace(pKey,pValues,new PrecedenceValues(pValues.getOperationObject(),
                                    newOr,pValues.getConnectedOperationID(),pValues.getIndex(),pValues.getRoute(),routeIndex));
                            precedenceOfOperations.put(o,
                                    new PrecedenceValues(newOr,pValues.getOperationObject(),presOf,indexInRoute,routeIndex,pValues.getRoute()));
                            System.out.println(o+" added to route");
                        }
                    }
                }
                /*
                System.out.println("NEW ADD: Vessel route "+routeIndex);
                System.out.println("Operation "+o);
                System.out.println("Earliest time "+ earliest);
                System.out.println("Latest time "+ latest);
                System.out.println("Route index "+indexInRoute);
                System.out.println(" ");
                 */
                routeSailingCost[routeIndex]+=timeAdded*SailingCostForVessel[routeIndex];
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
                //Update all earliest starting times forward
                updateEarliest(earliest,indexInRoute,routeIndex);
                updateLatest(latest,indexInRoute,routeIndex);
                updatePrecedenceOver(checkPrecedence(routeIndex,0),indexInRoute);
                updatePrecedenceOf(checkPrecedence(routeIndex,1),indexInRoute);
                /*
                System.out.println("VESSEL "+routeIndex);
                for(int n=0;n<vesselroutes.get(routeIndex).size();n++){
                    System.out.println("Number in order: "+n);
                    System.out.println("ID "+vesselroutes.get(routeIndex).get(n).getID());
                    System.out.println("Earliest starting time "+vesselroutes.get(routeIndex).get(n).getEarliestTime());
                    System.out.println("latest starting time "+vesselroutes.get(routeIndex).get(n).getLatestTime());
                    System.out.println(" ");
                }
                 */
            }
        }
        for(Integer taskLeft : allOperations){
            unroutedTasks.add(new OperationInRoute(taskLeft,0,nTimePeriods));
            routeSailingCost[nVessels]+=Penalty[taskLeft-startNodes.length-1];
        }
    }

    /*
    public void constructionHeuristic(){
        PrintData.timeVesselUseOnOperations(TimeVesselUseOnOperation,startNodes.length);
        PrintData.printSailingTimes(SailingTimes,1,nOperations-2*startNodes.length,startNodes.length);
        for (int n = 0; n < nVessels; n++) {
            vesselroutes.add(null);
            vesselTakenIntervals.add(null);
        }
        for (Integer o : sortedOperations){
            int vesselIndex=0;
            int startTime=0;
            int costAdded=100000;
            int endTime=0;
            int intervalIndex=-1;
            for (int v = 0; v < nVessels; v++) {
                if (DataGenerator.containsElement(o, OperationsForVessel[v])) {
                    if (vesselroutes.get(v)==null) {
                        int cost = SailingCostForVessel[v] * SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o-1];
                        if (cost < costAdded) {
                            costAdded = cost;
                            vesselIndex = v;
                            startTime = EarliestStartingTimeForVessel[v]+1 + SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                            endTime=startTime+TimeVesselUseOnOperation[v][o-startNodes.length-1][startTime];
                            intervalIndex=-1;
                        }
                    } else {
                        if (vesselTakenIntervals.get(v).size() == 1) {
                            int t = vesselTakenIntervals.get(v).get(0)[2];
                            if(t<=nTimePeriods){
                                int lastNode = vesselTakenIntervals.get(v).get(0)[3];
                                int cost = SailingCostForVessel[v] * SailingTimes[v][t]
                                        [lastNode - 1][o - 1];
                                int tempStartTime = t + SailingTimes[v][t][lastNode - 1][o - 1];
                                if (cost < costAdded && tempStartTime <= nTimePeriods) {
                                    costAdded = cost;
                                    vesselIndex = v;
                                    startTime = tempStartTime;
                                    endTime = startTime + TimeVesselUseOnOperation[v][o - startNodes.length - 1][startTime];
                                    intervalIndex = 0;
                                }
                            }
                        } else {
                            for (int i = 0; i < vesselTakenIntervals.get(v).size() - 1; i++) {
                                int t = vesselTakenIntervals.get(v).get(i)[2];
                                if(t<=nTimePeriods) {
                                    int lastNode = vesselTakenIntervals.get(v).get(0)[3];
                                    int cost = SailingCostForVessel[v] * SailingTimes[v][t]
                                            [lastNode][o - 1];
                                    int tempStartTime = t + SailingTimes[v][t][lastNode - 1][o - 1];
                                    int tempEndTime = tempStartTime + TimeVesselUseOnOperation[v][o - startNodes.length - 1][startTime];
                                    int totalTime = tempEndTime + SailingTimes[v][tempEndTime][o - 1][vesselTakenIntervals.get(v).get(i + 1)[0] - 1];
                                    if (cost < costAdded && tempStartTime <= nTimePeriods && totalTime <= vesselTakenIntervals.get(v).get(i + 1)[1]) {
                                        costAdded = cost;
                                        vesselIndex = v;
                                        startTime = tempStartTime;
                                        endTime = tempEndTime;
                                        intervalIndex = i;
                                    }
                                }
                            }
                            int lastIndex=vesselTakenIntervals.get(v).size() - 1;
                            int t = vesselTakenIntervals.get(v).get(lastIndex)[2];
                            if(t<=nTimePeriods) {
                                int lastNode = vesselTakenIntervals.get(v).get(lastIndex)[3];
                                int cost = SailingCostForVessel[v] * SailingTimes[v][t][lastNode - 1][o - 1];
                                int tempStartTime = t + SailingTimes[v][t][lastNode - 1][o - 1];
                                if (cost < costAdded && tempStartTime <= nTimePeriods) {
                                    costAdded = cost;
                                    vesselIndex = v;
                                    startTime = tempStartTime;
                                    endTime = startTime + TimeVesselUseOnOperation[v][o - startNodes.length - 1][startTime];
                                    intervalIndex = lastIndex;
                                }
                            }
                        }
                    }
                }
            }
            if(costAdded!=100000) {
                if (vesselroutes.get(vesselIndex) == null) {
                    int finalStartTime = startTime;
                    vesselroutes.set(vesselIndex, new ArrayList<>() {{
                        add(new OperationInRoute(o, finalStartTime));
                    }});
                } else {
                    vesselroutes.get(vesselIndex).add(new OperationInRoute(o, startTime));
                }
                allOperations.remove(Integer.valueOf(o));
                if (intervalIndex==-1){
                    int finalEndTime = endTime;
                    int finalVesselIndex = vesselIndex;
                    vesselTakenIntervals.set(vesselIndex,new ArrayList<>() {{add(new int[]{startNodes[finalVesselIndex], 1, finalEndTime,o});}});
                    System.out.println("First ADD for vessel");
                    System.out.println("VESSEL INDEX "+vesselIndex);
                    System.out.println("Operation time "+ o+" "+ TimeVesselUseOnOperation[vesselIndex][o-startNodes.length-1][startTime]);
                    System.out.println("Vessel index "+vesselIndex);
                    System.out.println("Sailing time "+ startNodes[vesselIndex] + " to "+ (o)+" is "+SailingTimes[vesselIndex][EarliestStartingTimeForVessel[vesselIndex]][vesselIndex][o-1]);
                    System.out.println(Arrays.toString(vesselTakenIntervals.get(vesselIndex).get(0)));
                    System.out.println(" ");
                }
                else if (intervalIndex>-1){
                    System.out.println("VESSEL INDEX "+vesselIndex);
                    System.out.println("Operation time "+ o+" "+ TimeVesselUseOnOperation[vesselIndex][o-startNodes.length-1][startTime]);
                    List<int[]> test=vesselTakenIntervals.get(vesselIndex);
                    int prevLastOp=vesselTakenIntervals.get(vesselIndex).get(intervalIndex)[3];
                    int prevEndTime=vesselTakenIntervals.get(vesselIndex).get(intervalIndex)[2];
                    System.out.println("Previous operation: "+prevLastOp);
                    System.out.println("Sailing time from "+prevLastOp+" to "+o+ " is "+ SailingTimes[vesselIndex][prevEndTime][prevLastOp-1][o-1]);
                    int startOperation=vesselTakenIntervals.get(vesselIndex).get(intervalIndex)[0];
                    int intervalStart=vesselTakenIntervals.get(vesselIndex).get(intervalIndex)[1];
                    vesselTakenIntervals.get(vesselIndex).set(intervalIndex,new int[]{startOperation,intervalStart,endTime,o});
                    System.out.println(Arrays.toString(test.get(intervalIndex)));
                    System.out.println(" ");

                }
            }
        }
        for(Integer tasksLeft : allOperations){
            unroutedTasks.add(new OperationInRoute(tasksLeft,0));
        }
    }
    // intervallene i VesselAvailableIntervals listene er fra (altså ekskluderende fra start) og til og med (altså
    // inkluderende på øvre grense i intervallet)
  */

    public List<PrecedenceValues> checkPrecedence(int v, int presType){
        List<PrecedenceValues> dependencies= new ArrayList<>();
        Map<Integer, PrecedenceValues> dict = null;
        if(presType==0){
            dict=precedenceOverOperations;
        }
        else if(presType==1){
            dict = precedenceOfOperations;
        }
        if (vesselroutes.get(v)!=null) {
            for (int i=0; i <vesselroutes.get(v).size();i++) {
                for (Integer orP : dict.keySet()) {
                    if (orP == vesselroutes.get(v).get(i).getID()) {
                        dependencies.add(dict.get(orP));
                    }
                }
            }
        }
        return dependencies;
    }

    public void updatePrecedenceOver(List<PrecedenceValues> precedenceOver, int insertIndex) {
        if(!precedenceOver.isEmpty()){
            for (PrecedenceValues pValues : precedenceOver) {
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                if (secondOr != null) {
                    PrecedenceValues connectedOpPValues = precedenceOfOperations.get(secondOr.getID());
                    int routeConnectedOp = connectedOpPValues.getRoute();
                    int route = pValues.getRoute();
                    if (routeConnectedOp == pValues.getRoute()) {
                        continue;
                    }
                    int newESecondOr = firstOr.getEarliestTime() + TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                            [EarliestStartingTimeForVessel[route]];
                    int indexConnected = connectedOpPValues.getIndex();
                    int precedenceIndex = pValues.getIndex();
                    if (insertIndex <= precedenceIndex) {
                        pValues.setIndex(precedenceIndex + 1);
                        System.out.println("Index demands update");
                        System.out.println("Old earliest: " + secondOr.getEarliestTime());
                        System.out.println("New earliest: " + newESecondOr);
                        if (secondOr.getEarliestTime() < newESecondOr) {
                            secondOr.setEarliestTime(newESecondOr);
                            updateEarliest(newESecondOr, indexConnected, routeConnectedOp);
                        }
                        System.out.println("update earliest because of precedence over");
                    }
                }
            }
        }
    }

    public void updatePrecedenceOf(List<PrecedenceValues> precedenceOf, int insertIndex) {
        if(!precedenceOf.isEmpty()){
            for (PrecedenceValues pValues : precedenceOf) {
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                PrecedenceValues connectedOpPValues = precedenceOverOperations.get(secondOr.getID());
                int routeConnectedOp = connectedOpPValues.getRoute();
                int route = pValues.getRoute();
                if (routeConnectedOp == pValues.getRoute()) {
                    continue;
                }
                int indexConnected = connectedOpPValues.getIndex();
                int newLSecondOr = firstOr.getLatestTime() - TimeVesselUseOnOperation[pValues.getConnectedRoute()][secondOr.getID() - startNodes.length - 1]
                        [EarliestStartingTimeForVessel[route]];
                int precedenceIndex = pValues.getIndex();
                if (insertIndex > precedenceIndex) {
                    System.out.println("Index demands update");
                    System.out.println("Old latest: " + secondOr.getLatestTime());
                    System.out.println("New latest: " + newLSecondOr);
                    if (secondOr.getLatestTime() > newLSecondOr) {
                        secondOr.setLatestTime(newLSecondOr);
                        updateLatest(newLSecondOr, indexConnected, pValues.getConnectedRoute());
                        System.out.println("update latest because of precedence of");
                    }
                }
                if (insertIndex <= precedenceIndex) {
                    pValues.setIndex(precedenceIndex);
                }
            }
        }
    }

    public Boolean checkPOverFeasible(List<PrecedenceValues> precedenceOver, int o, int sailingTime, int insertIndex,int earliest) {
        List<PrecedenceValues> pOver=precedenceOver;
        if(!precedenceOver.isEmpty()) {
            for (PrecedenceValues pValues : precedenceOver) {
                int route = pValues.getRoute();
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                if (secondOr != null) {
                    PrecedenceValues connectedOpPValues = precedenceOfOperations.get(secondOr.getID());
                    int routeConnectedOp = connectedOpPValues.getRoute();
                    if (routeConnectedOp == pValues.getRoute()) {
                        continue;
                    }
                    int precedenceIndex = pValues.getIndex();
                    if (insertIndex <= precedenceIndex) {
                        Boolean change = checkChangeEarliest(earliest, insertIndex, route, precedenceIndex, pValues.getOperationObject().getEarliestTime(), o);
                        if (change) {
                            int newESecondOr = firstOr.getEarliestTime() + TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                                    [EarliestStartingTimeForVessel[route]]
                                    + sailingTime + TimeVesselUseOnOperation[route][o - startNodes.length - 1][EarliestStartingTimeForVessel[route]];
                            if (newESecondOr > secondOr.getLatestTime()) {
                                System.out.println("NOT PRECEDENCE OVER FEASIBLE");
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public Boolean checkPOfFeasible(List<PrecedenceValues> precedenceOf, int o, int sailingTime, int insertIndex, int latest) {
        if(!precedenceOf.isEmpty()) {
            for (PrecedenceValues pValues : precedenceOf) {
                int route = pValues.getRoute();
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                PrecedenceValues connectedOpPValues = precedenceOverOperations.get(secondOr.getID());
                int routeConnectedOp = connectedOpPValues.getRoute();
                if (routeConnectedOp == pValues.getRoute()) {
                    continue;
                }
                int precedenceIndex = pValues.getIndex();
                if (insertIndex > precedenceIndex) {
                    Boolean change = checkChangeLatest(latest, insertIndex, route, pValues.getIndex(), pValues.getOperationObject().getLatestTime(), o);
                    if (change) {
                        int newLSecondOr = firstOr.getLatestTime() - TimeVesselUseOnOperation[pValues.getConnectedRoute()][secondOr.getID() - startNodes.length - 1]
                                [EarliestStartingTimeForVessel[route]]
                                - sailingTime - TimeVesselUseOnOperation[route][o - startNodes.length - 1][EarliestStartingTimeForVessel[route]];
                        if (newLSecondOr < secondOr.getEarliestTime()) {
                            System.out.println("NOT PRECEDENCE OF FEASIBLE");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public int[] checkprecedenceOf(int o, int v, int earliestTemp, int latestTemp){
        int breakValue=0;
        int precedenceOf=precedenceALNS[o-1-startNodes.length][1];
        if(precedenceOf!=0) {
            PrecedenceValues pValues=precedenceOverOperations.get(precedenceOf);
            if (pValues == null) {
                breakValue=1;
                //System.out.println("break true");
                //System.out.println("Tried: 0 "+o);
            }
            if(breakValue==0) {
                int earliestPO = pValues.getOperationObject().getEarliestTime();
                int latestPO = pValues.getOperationObject().getLatestTime();
                earliestTemp = Math.max(earliestTemp, earliestPO + TimeVesselUseOnOperation[pValues.getRoute()][precedenceOf - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]);
                //System.out.println("Vessel "+v+" Test earliest temp precedence of, operation "+o+" earliesttemp: "+earliestTemp);
                latestTemp = Math.min(latestTemp, latestPO + TimeVesselUseOnOperation[pValues.getRoute()][precedenceOf - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]);
                //System.out.println(latestTemp);
            }
        }
        return new int[]{earliestTemp,latestTemp,breakValue};
    }

    public void updateLatest(int latest, int indexInRoute, int routeIndex){
        int lastLatest=latest;
        for(int k=indexInRoute-1;k>-1;k--){
            int newTime=Math.min(vesselroutes.get(routeIndex).get(k).getLatestTime(),lastLatest-
                    SailingTimes[routeIndex][EarliestStartingTimeForVessel[routeIndex]][vesselroutes.get(routeIndex).get(k).getID()-1]
                            [vesselroutes.get(routeIndex).get(k+1).getID()-1]
                    -TimeVesselUseOnOperation[routeIndex][vesselroutes.get(routeIndex).get(k).getID()-startNodes.length-1][EarliestStartingTimeForVessel[routeIndex]]);
            if(newTime==vesselroutes.get(routeIndex).get(k).getLatestTime()){
                break;
            }
            vesselroutes.get(routeIndex).get(k).setLatestTime(newTime);
            lastLatest=newTime;
        }
    }

    public void updateEarliest(int earliest, int indexInRoute, int routeIndex){
        int lastEarliest=earliest;
        for(int f=indexInRoute+1;f<vesselroutes.get(routeIndex).size();f++){
            int newTime=Math.max(vesselroutes.get(routeIndex).get(f).getEarliestTime(),lastEarliest+
                    SailingTimes[routeIndex][EarliestStartingTimeForVessel[routeIndex]][vesselroutes.get(routeIndex).get(f-1).getID()-1]
                            [vesselroutes.get(routeIndex).get(f).getID()-1]
                    +TimeVesselUseOnOperation[routeIndex][vesselroutes.get(routeIndex).get(f-1).getID()-startNodes.length-1][EarliestStartingTimeForVessel[routeIndex]]);
            if(newTime==vesselroutes.get(routeIndex).get(f).getEarliestTime()){
                break;
            }
            vesselroutes.get(routeIndex).get(f).setEarliestTime(newTime);
            lastEarliest=newTime;

        }
    }

    public Boolean checkChangeEarliest(int earliestInsertionOperation, int indexInRoute, int routeIndex, int precedenceIndex, int earliestPrecedenceOperation,int o){
        int lastEarliest=earliestInsertionOperation;
        for(int f=indexInRoute;f<vesselroutes.get(routeIndex).size();f++){
            int sailingtime;
            int operationtime=0;
            if (f==indexInRoute){
                sailingtime= SailingTimes[routeIndex][EarliestStartingTimeForVessel[routeIndex]][o-1]
                        [vesselroutes.get(routeIndex).get(f).getID()-1];
                operationtime=TimeVesselUseOnOperation[routeIndex][o-startNodes.length-1][EarliestStartingTimeForVessel[routeIndex]];
            }
            else{
                sailingtime=SailingTimes[routeIndex][EarliestStartingTimeForVessel[routeIndex]][vesselroutes.get(routeIndex).get(f-1).getID()-1]
                        [vesselroutes.get(routeIndex).get(f).getID()-1];
                operationtime=TimeVesselUseOnOperation[routeIndex][vesselroutes.get(routeIndex).get(f-1).getID()-startNodes.length-1][EarliestStartingTimeForVessel[routeIndex]];
            }
            int newTime=Math.max(vesselroutes.get(routeIndex).get(f).getEarliestTime(),lastEarliest+
                sailingtime +operationtime);
            if(newTime==vesselroutes.get(routeIndex).get(f).getEarliestTime()){
                return false;
            }
            lastEarliest=newTime;
            if(f==precedenceIndex) {
                break;
            }
        }
        if(lastEarliest>earliestPrecedenceOperation){
            return true;
        }
        else{
            return false;
        }
    }

    public Boolean checkChangeLatest(int latestInsertionOperation, int indexInRoute, int routeIndex, int precedenceIndex, int latestPrecedenceOperation, int o){
        int lastLatest=latestInsertionOperation;
        int sailingTime=0;
        for(int k=indexInRoute-1;k>-1;k--){
            if(k==indexInRoute-1){
                sailingTime=SailingTimes[routeIndex][EarliestStartingTimeForVessel[routeIndex]][vesselroutes.get(routeIndex).get(k).getID()-1]
                        [o-1];
            }
            else{
                sailingTime=SailingTimes[routeIndex][EarliestStartingTimeForVessel[routeIndex]][vesselroutes.get(routeIndex).get(k).getID()-1]
                        [vesselroutes.get(routeIndex).get(k+1).getID()-1];
            }
            int newTime=Math.min(vesselroutes.get(routeIndex).get(k).getLatestTime(),lastLatest- sailingTime
                    -TimeVesselUseOnOperation[routeIndex][vesselroutes.get(routeIndex).get(k).getID()-startNodes.length-1][EarliestStartingTimeForVessel[routeIndex]]);
            if(newTime==vesselroutes.get(routeIndex).get(k).getLatestTime()){
                return false;
            }
            lastLatest=newTime;
            if(k==precedenceIndex) {
                break;
            }
        }
        if(lastLatest<latestPrecedenceOperation){
            return true;
        }
        else{
            return false;
        }
    }

    public static Boolean containsElement(int element, int[] list)   {
        Boolean bol = false;
        for (Integer e: list)     {
            if(element == e){
                bol=true;
            }
        }
        return bol;
    }

    public void printInitialSolution(int[] vessseltypes){
        //PrintData.timeVesselUseOnOperations(TimeVesselUseOnOperation,startNodes.length);
        //PrintData.printSailingTimes(SailingTimes,3,nOperations-2*startNodes.length,startNodes.length);
        //PrintData.printOperationsForVessel(OperationsForVessel);
        PrintData.printPrecedenceALNS(precedenceALNS);
        PrintData.printTimeWindows(timeWindowsForOperations);
        PrintData.printTimeWindowsIntervals(twIntervals);

        System.out.println(Arrays.toString(routeSailingCost));
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
    }

    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes =new int[]{1,2,3,4,5,4};
        int[] startnodes=new int[]{1,2,3,4,5,6};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes,
                "test_instances/test_instance_15_locations_PRECEDENCEtest3.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        ConstructionHeuristic a = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getPenalty(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations());
        PrintData.printPrecedenceALNS(dg.getPrecedenceALNS());
        a.createSortedOperations();
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);
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
}
