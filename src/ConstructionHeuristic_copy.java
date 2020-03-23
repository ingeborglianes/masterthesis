import java.io.FileNotFoundException;
import java.util.*;

public class ConstructionHeuristic_copy {
    private int [][] OperationsForVessel;
    private int [][] TimeWindowsForOperations;
    private int [][][] Edges;
    private int [][][][] SailingTimes;
    private int [][][] TimeVesselUseOnOperation;
    private int [] EarliestStartingTimeForVessel;
    private int [] SailingCostForVessel;
    private int [][][] operationGain;
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
    //List for operations that are connected as simultaneous operations. ID= operation number. Value= Simultaneous value.
    private Map<Integer, ConnectedValues> simultaneousOp = new HashMap<>();



    public ConstructionHeuristic_copy(int [][] OperationsForVessel, int [][] TimeWindowsForOperations, int [][][] Edges, int [][][][] SailingTimes,
                                 int [][][] TimeVesselUseOnOperation, int [] EarliestStartingTimeForVessel,
                                 int [] SailingCostForVessel, int [][][] operationGain, int [][] Precedence, int [][] Simultaneous,
                                 int [] BigTasks, Map<Integer, List<Integer>> ConsolidatedTasks, int[] endNodes, int[] startNodes, double[] endPenaltyforVessel,
                                 int[][] twIntervals, int[][] precedenceALNS, int[][] simALNS, int[][] bigTasksALNS, int[][] timeWindowsForOperations){
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
        for(int o = startNodes.length+1; o<nOperations-endNodes.length+1;o++){
            allOperations.add(o);
        }
        //actionTime is only a heuristic with changing weather, maybe drop it?
        this.actionTime=new int[startNodes.length];
        this.timeWindowsForOperations=timeWindowsForOperations;
        System.out.println("Number of operations: "+(nOperations-startNodes.length*2));
        System.out.println("START NODES: "+Arrays.toString(this.startNodes));
        System.out.println("END NODES: "+Arrays.toString(this.endNodes));
    }

    public void createSortedOperations(){
        //Use time 0 and vessel 0 kind of randomly, as we need to know the relationship between the operations
        SortedSet<KeyValuePair> penaltiesDict = new TreeSet<KeyValuePair>();
        for (int g=0;g<this.operationGain[0].length;g++){
            System.out.println((g+1+startNodes.length)+" "+operationGain[0][g][0]);
            //Key value (= operation number) in penaltiesDict is not null indexed
            penaltiesDict.add(new KeyValuePair(g+1+startNodes.length,operationGain[0][g][0]));
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
        outer: for (Integer o : sortedOperations){
            System.out.println("On operation: "+o);
            int timeAdded=100000;
            int indexInRoute=0;
            int routeIndex=0;
            int earliest=0;
            int latest=nTimePeriods;
            Boolean isActionTime = false;
            for (int v = 0; v < nVessels; v++) {

                List<ConnectedValues> simOps = checkSimultaneous(v);
                Boolean notThisVessel = checkSimOpInRoute(simOps,o);
                /*
                if(actionTime[v]+TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]>nTimePeriods){
                    System.out.println("Break because of actiontime");
                    isActionTime = true;
                    continue;
                }

                 */
                List<PrecedenceValues> precedenceOver= checkPrecedence(v,0);
                List<PrecedenceValues> precedenceOf= checkPrecedence(v,1);
                boolean precedenceOverFeasible=true;
                boolean precedenceOfFeasible=true;
                boolean simultaneousFeasible=true;

                if (DataGenerator.containsElement(o, OperationsForVessel[v]) && notThisVessel) {
                    System.out.println("Try vessel "+v);
                    if (vesselroutes.get(v) == null) {
                        System.out.println("Empty route");
                        //insertion into empty route
                        int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                        int timeIncrease=sailingTimeStartNodeToO;
                        int earliestTemp=Math.max(EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO+1,twIntervals[o-startNodes.length-1][0]);
                        int latestTemp=Math.min(nTimePeriods,twIntervals[o-startNodes.length-1][1]);
                        int[] precedenceOfValuesEarliest=checkprecedenceOfEarliest(o,v,earliestTemp);
                        if (precedenceOfValuesEarliest[1]==1) {
                            System.out.println("BREAK PRECEDENCE");
                            continue outer;
                        }
                        earliestTemp=precedenceOfValuesEarliest[0];
                        int [] simultaneousTimesValues = checkSimultaneousOfTimes(o,v,earliestTemp,latestTemp);
                        System.out.println(simultaneousTimesValues[0] +"," + simultaneousTimesValues[1]+ " sim time");
                        if(simultaneousTimesValues[2]==1){
                            System.out.println("BREAK SIMULTANEOUS");
                            continue outer;
                        }
                        earliestTemp=simultaneousTimesValues[0];
                        latestTemp=simultaneousTimesValues[1];

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
                            System.out.println("On index "+n);
                            if(n==0) {
                                //check insertion in first position
                                int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                                int earliestTemp = Math.max(EarliestStartingTimeForVessel[v] + sailingTimeStartNodeToO + 1, twIntervals[o - startNodes.length - 1][0]);
                                int opTime=TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO];
                                int[] precedenceOfValuesEarliest=checkprecedenceOfEarliest(o,v,earliestTemp);
                                if (precedenceOfValuesEarliest[1]==1) {
                                    System.out.println("BREAK PRECEDENCE");
                                    continue outer;
                                }
                                earliestTemp=precedenceOfValuesEarliest[0];

                                int sailingTimeOToNext=SailingTimes[v][earliestTemp-1][o - 1][vesselroutes.get(v).get(0).getID() - 1];
                                int latestTemp = Math.min(vesselroutes.get(v).get(0).getLatestTime() - sailingTimeOToNext - opTime,
                                        twIntervals[o - startNodes.length - 1][1]);

                                int [] simultaneousTimesValues = checkSimultaneousOfTimes(o,v,earliestTemp,latestTemp);
                                if(simultaneousTimesValues[2]==1){
                                    System.out.println("BREAK SIMULTANEOUS");
                                    continue outer;
                                }
                                earliestTemp=simultaneousTimesValues[0];
                                latestTemp=simultaneousTimesValues[1];

                                int timeIncrease=sailingTimeStartNodeToO + sailingTimeOToNext
                                        - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselroutes.get(v).get(0).getID() - 1];
                                if(timeIncrease < timeAdded && earliestTemp<=latestTemp && earliestTemp<vesselroutes.get(v).get(0).getEarliestTime()) {
                                    precedenceOverFeasible = checkPOverFeasible(precedenceOver, o, timeIncrease, 0, earliestTemp);
                                    precedenceOfFeasible = checkPOfFeasible(precedenceOf, o, timeIncrease, 0, latestTemp);
                                    simultaneousFeasible = checkSimultaneousFeasible(simOps,o,v,0,earliestTemp,latestTemp);
                                    if(precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                        OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                                        int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                        int changedTime = checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, 0, v, o);
                                        if (changedTime > 0) {
                                            int deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                    operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                            if (deltaOperationGainLastOperation > 0 && deltaOperationGainLastOperation >
                                                    operationGain[v][o - 1 - startNodes.length][earliestTemp - 1] - timeIncrease * SailingCostForVessel[v]) {
                                                continue;
                                            }
                                        }
                                        timeAdded = timeIncrease;
                                        routeIndex = v;
                                        indexInRoute = 0;
                                        earliest = earliestTemp;
                                        latest = latestTemp;
                                    }
                                }
                            }
                            if (n==vesselroutes.get(v).size()-1){
                                //check insertion in last position
                                System.out.println("Checking this position");
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
                                int[] precedenceOfValuesEarliest=checkprecedenceOfEarliest(o,v,earliestTemp);
                                if (precedenceOfValuesEarliest[1]==1) {
                                    System.out.println("BREAK PRECEDENCE");
                                    continue outer;
                                }
                                earliestTemp=precedenceOfValuesEarliest[0];

                                int [] simultaneousTimesValues = checkSimultaneousOfTimes(o,v,earliestTemp,latestTemp);
                                if(simultaneousTimesValues[2]==1){
                                    System.out.println("BREAK SIMULTANEOUS");
                                    continue outer;
                                }
                                earliestTemp=simultaneousTimesValues[0];
                                latestTemp=simultaneousTimesValues[1];

                                int timeIncrease=sailingTimePrevToO;
                                if(timeIncrease < timeAdded && earliestTemp<=latestTemp) {
                                    precedenceOverFeasible= checkPOverFeasible(precedenceOver, o, timeIncrease,n+1,earliestTemp);
                                    precedenceOfFeasible= checkPOfFeasible(precedenceOf, o, timeIncrease,n+1,latestTemp);
                                    simultaneousFeasible = checkSimultaneousFeasible(simOps,o,v,n+1,earliestTemp,latestTemp);
                                    if(precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                        OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                                        int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                        int changedTime = checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, n + 1, v, o);
                                        if (changedTime > 0) {
                                            int deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                    operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                            if (deltaOperationGainLastOperation > 0 && deltaOperationGainLastOperation >
                                                    operationGain[v][o - 1 - startNodes.length][earliestTemp - 1] - timeIncrease * SailingCostForVessel[v]) {
                                                continue;
                                            }
                                        }
                                        timeAdded = timeIncrease;
                                        routeIndex = v;
                                        indexInRoute = n + 1;
                                        earliest = earliestTemp;
                                        latest = latestTemp;
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
                                int[] precedenceOfValuesEarliest = checkprecedenceOfEarliest(o, v, earliestTemp);
                                if (precedenceOfValuesEarliest[1] == 1) {
                                    System.out.println("BREAK PRECEDENCE");
                                    continue outer;
                                }
                                earliestTemp = precedenceOfValuesEarliest[0];
                                if (earliestTemp - 1 < nTimePeriods) {
                                    int opTime = TimeVesselUseOnOperation[v][o - 1 - startNodes.length][earliestTemp - 1];
                                    int sailingTimeOToNext = SailingTimes[v][Math.min(earliestTemp + opTime - 1, nTimePeriods-1)][o - 1][vesselroutes.get(v).get(n + 1).getID() - 1];

                                    int latestTemp = Math.min(vesselroutes.get(v).get(n + 1).getLatestTime() -
                                            sailingTimeOToNext - opTime, twIntervals[o - startNodes.length - 1][1]);

                                    int[] simultaneousTimesValues = checkSimultaneousOfTimes(o, v, earliestTemp, latestTemp);
                                    if (simultaneousTimesValues[2] == 1) {
                                        System.out.println("BREAK SIMULTANEOUS");
                                        continue outer;
                                    }
                                    earliestTemp = simultaneousTimesValues[0];
                                    latestTemp = simultaneousTimesValues[1];

                                    int sailingTimePrevToNext = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselroutes.get(v).get(n).getID() - 1][vesselroutes.get(v).get(n + 1).getID() - 1];
                                    int timeIncrease = sailingTimePrevToO + sailingTimeOToNext - sailingTimePrevToNext;
                                /*
                                System.out.println("P over feasible: "+precedenceOverFeasible);
                                System.out.println("P of feasible: "+precedenceOfFeasible);
                                System.out.println("TimeIncrease: "+timeIncrease+" less than Time added "+timeAdded);
                                System.out.println("Earliest temp "+ earliestTemp+ " less than or equal to latest temp "+latestTemp);
                                System.out.println("Earliest temp "+ earliestTemp+ " less than next earliest "+vesselroutes.get(v).get(n+1).getEarliestTime());
                                 */
                                    if (timeIncrease < timeAdded && earliestTemp <= latestTemp && earliestTemp < vesselroutes.get(v).get(n + 1).getEarliestTime()) {
                                        precedenceOverFeasible = checkPOverFeasible(precedenceOver, o, timeIncrease, n + 1, earliestTemp);
                                        precedenceOfFeasible = checkPOfFeasible(precedenceOf, o, timeIncrease, n + 1, latestTemp);
                                        int currentLatest = vesselroutes.get(v).get(n).getLatestTime();
                                        simultaneousFeasible = checkSOfFeasible(o, v, currentLatest);
                                        if(simultaneousFeasible){
                                            simultaneousFeasible = checkSimultaneousFeasible(simOps,o,v,n+1,earliestTemp,latestTemp);
                                        }
                                        if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                            OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                                            int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                            int changedTime = checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, n + 1, v, o);
                                            if (changedTime > 0) {
                                                int deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                        operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                                if (deltaOperationGainLastOperation > 0 && deltaOperationGainLastOperation >
                                                        operationGain[v][o - 1 - startNodes.length][earliestTemp - 1] - timeIncrease * SailingCostForVessel[v]) {
                                                    continue;
                                                }
                                            }
                                            timeAdded = timeIncrease;
                                            routeIndex = v;
                                            indexInRoute = n + 1;
                                            earliest = earliestTemp;
                                            latest = latestTemp;
                                            System.out.println("FEASIBLE");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if((timeAdded==100000 && simALNS[o-startNodes.length-1][1] != 0 ) ||
                    (isActionTime && simALNS[o-startNodes.length-1][1] != 0) ) {
                ConnectedValues simOp = simultaneousOp.get(simALNS[o-startNodes.length-1][1]);
                System.out.println(simOp.getRoute());
                int prevEarliest = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex()-1).getEarliestTime();
                System.out.println(prevEarliest + " Prev earliest");
                int nextLatest = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex()).getLatestTime();
                System.out.println(nextLatest + " Next latest");
                unroutedTasks.add(simOp.getOperationObject());
                vesselroutes.get(simOp.getRoute()).remove(simOp.getIndex());
                simultaneousOp.remove(simOp.getOperationObject().getID(), simOp);
                updateEarliest(prevEarliest,simOp.getIndex()-1,simOp.getRoute());
                System.out.println(vesselroutes.get(simOp.getRoute()).size());
                if(simOp.getIndex() == vesselroutes.get(simOp.getRoute()).size()){
                    OperationInRoute lastOp = vesselroutes.get(simOp.getRoute()).get(vesselroutes.get(simOp.getRoute()).size()-1);
                    lastOp.setLatestTime(nextLatest);
                }
                updateLatestAfterRemoval(nextLatest,Math.min(simOp.getIndex(),vesselroutes.get(simOp.getRoute()).size()-1),simOp.getRoute());
                updatePrecedenceOver(checkPrecedence(simOp.getRoute(),0),simOp.getIndex());
                updatePrecedenceOf(checkPrecedence(simOp.getRoute(),1),simOp.getIndex());
                updateSimultaneousAfterRemoval(checkSimultaneous(simOp.getRoute()),simOp.getRoute(),simOp.getIndex()-1, o);

                System.out.println("Update by removal VESSEL "+simOp.getRoute());
                for(int n=0;n<vesselroutes.get(simOp.getRoute()).size();n++){
                    System.out.println("Number in order: "+n);
                    System.out.println("ID "+vesselroutes.get(simOp.getRoute()).get(n).getID());
                    System.out.println("Earliest starting time "+vesselroutes.get(simOp.getRoute()).get(n).getEarliestTime());
                    System.out.println("latest starting time "+vesselroutes.get(simOp.getRoute()).get(n).getLatestTime());
                    System.out.println(" ");
                }

            }

            //After iterating through all possible insertion places, we here add the operation at the best insertion place
            if(timeAdded!=100000) {
                actionTime[routeIndex]+=timeAdded+TimeVesselUseOnOperation[routeIndex][o-startNodes.length-1][EarliestStartingTimeForVessel[routeIndex]];
                OperationInRoute newOr=new OperationInRoute(o, earliest, latest);
                int presOver=precedenceALNS[o-1-startNodes.length][0];
                int presOf=precedenceALNS[o-1-startNodes.length][1];
                if (presOver!=0){
                    //System.out.println(o+" added in precedence operations dictionary 0 "+presOver);
                    PrecedenceValues pValues= new PrecedenceValues(newOr,null,presOver,indexInRoute,routeIndex,0);
                    precedenceOverOperations.put(o,pValues);
                }
                if (presOf!=0){
                    PrecedenceValues pValues= precedenceOverOperations.get(presOf);
                    precedenceOverOperations.replace(presOf,pValues,new PrecedenceValues(pValues.getOperationObject(),
                            newOr,pValues.getConnectedOperationID(),pValues.getIndex(),pValues.getRoute(),routeIndex));
                    precedenceOfOperations.put(o,
                            new PrecedenceValues(newOr,pValues.getOperationObject(),presOf,indexInRoute,routeIndex,pValues.getRoute()));

                }

                //while((o-1-startNodes.length+k) < simALNS.length && simALNS[o-1-startNodes.length+k][0]!=0 ){
                int simA = simALNS[o-1-startNodes.length][1];
                int simB = simALNS[o-1-startNodes.length][0];
                if(simB != 0 && simA == 0) {
                    ConnectedValues sValue = new ConnectedValues(newOr, null,simB,indexInRoute,routeIndex, 0);
                    simultaneousOp.put(o,sValue);
                }else if (simA != 0){
                    ConnectedValues sValues = simultaneousOp.get(simA);
                    if(sValues.getConnectedOperationObject() == null) {
                        simultaneousOp.replace(simA, sValues, new ConnectedValues(sValues.getOperationObject(), newOr, sValues.getConnectedOperationID(),
                                sValues.getIndex(), sValues.getRoute(), routeIndex));
                    }else{
                        simultaneousOp.put(simA, new ConnectedValues(sValues.getOperationObject(), newOr, sValues.getConnectedOperationID(),
                                sValues.getIndex(), sValues.getRoute(), routeIndex));
                    }
                    simultaneousOp.put(o, new ConnectedValues(newOr,sValues.getOperationObject(),simB,indexInRoute,routeIndex,sValues.getRoute()));
                }


                System.out.println("NEW ADD: Vessel route "+routeIndex);
                System.out.println("Operation "+o);
                System.out.println("Earliest time "+ earliest);
                System.out.println("Latest time "+ latest);
                System.out.println("Route index "+indexInRoute);
                System.out.println(" ");


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

                System.out.println("VESSEL "+routeIndex);
                for(int n=0;n<vesselroutes.get(routeIndex).size();n++){
                    System.out.println("Number in order: "+n);
                    System.out.println("ID "+vesselroutes.get(routeIndex).get(n).getID());
                    System.out.println("Earliest starting time "+vesselroutes.get(routeIndex).get(n).getEarliestTime());
                    System.out.println("latest starting time "+vesselroutes.get(routeIndex).get(n).getLatestTime());
                    System.out.println(" ");
                }
                updatePrecedenceOver(checkPrecedence(routeIndex,0),indexInRoute);
                updatePrecedenceOf(checkPrecedence(routeIndex,1),indexInRoute);
                updateSimultaneous(checkSimultaneous(routeIndex),routeIndex,indexInRoute,o);

                for(int r=0;r<nVessels;r++) {
                    System.out.println("VESSEL " + r);
                    if(vesselroutes.get(r) != null) {
                        for (int n = 0; n < vesselroutes.get(r).size(); n++) {
                            System.out.println("Number in order: " + n);
                            System.out.println("ID " + vesselroutes.get(r).get(n).getID());
                            System.out.println("Earliest starting time " + vesselroutes.get(r).get(n).getEarliestTime());
                            System.out.println("latest starting time " + vesselroutes.get(r).get(n).getLatestTime());
                            System.out.println(" ");
                        }
                    }
                }
            }
        }
        for(Integer taskLeft : allOperations){
            unroutedTasks.add(new OperationInRoute(taskLeft,0,nTimePeriods));
        }
        //Calculate objective
        calculateObjective();
    }

    public void calculateObjective(){
        for (int r=0;r<vesselroutes.size();r++){
            if(vesselroutes.get(r)!=null) {
                OperationInRoute or = vesselroutes.get(r).get(0);
                int opTime = TimeVesselUseOnOperation[r][or.getID() - 1 - startNodes.length][or.getEarliestTime()-1];
                int cost0 = SailingTimes[r][or.getEarliestTime() + opTime-1][r][or.getID() - 1]*SailingCostForVessel[r];
                int gain0 = operationGain[r][or.getID() - 1 - startNodes.length][or.getEarliestTime()-1];
                routeSailingCost[r] += cost0;
                routeOperationGain[r] += gain0;
                objValue += gain0;
                objValue -= cost0;
                for (int o = 1; o < vesselroutes.get(r).size(); o++) {
                    OperationInRoute orCurrent = vesselroutes.get(r).get(o);
                    OperationInRoute orPrevious = vesselroutes.get(r).get(o - 1);
                    int opPreviousTime = TimeVesselUseOnOperation[r][orPrevious.getID() - 1 - startNodes.length][orPrevious.getEarliestTime()-1];
                    int costN = SailingTimes[r][orPrevious.getEarliestTime() + opPreviousTime-1][orPrevious.getID() - 1][orCurrent.getID() - 1]*SailingCostForVessel[r];
                    int gainN = operationGain[r][orCurrent.getID() - 1 - startNodes.length][orCurrent.getEarliestTime()-1];
                    routeSailingCost[r] += costN;
                    routeOperationGain[r] += gainN;
                    objValue-=costN;
                    objValue+=gainN;
                }
            }
        }
    }

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
                int IDOp=vesselroutes.get(v).get(i).getID();
                if(dict.get(IDOp)!=null){
                    dependencies.add(dict.get(IDOp));
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
                            [firstOr.getEarliestTime()-1];
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
                        [secondOr.getLatestTime()-1];
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
                        int change = checkChangeEarliest(earliest, insertIndex, route, precedenceIndex, pValues.getOperationObject().getEarliestTime(), o);
                        if (change!=0) {
                            int t= firstOr.getEarliestTime()+change-1;
                            if(t>nTimePeriods-1){
                                t=nTimePeriods-1;
                            }
                            int newESecondOr=firstOr.getEarliestTime() + TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                                    [t] + change;
                            if(newESecondOr>secondOr.getEarliestTime()) {
                                if (newESecondOr > secondOr.getLatestTime()) {
                                    System.out.println("NOT PRECEDENCE OVER FEASIBLE EARLIEST/LATEST P-OPERATION "+firstOr.getID());
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
                    int change = checkChangeLatest(latest, insertIndex, route, pValues.getIndex(), pValues.getOperationObject().getLatestTime(), o);
                    if (change!=0) {
                        //System.out.println("Previous latest time for previous of "+firstOr.getLatestTime());
                        //System.out.println("change in previous of latest time "+change);
                        int t =secondOr.getLatestTime()-change-1;
                        if(t<0){
                            t=0;
                        }
                        int newLSecondOr = firstOr.getLatestTime() - change - TimeVesselUseOnOperation[pValues.getConnectedRoute()][secondOr.getID() - startNodes.length - 1][t];
                        if (newLSecondOr < secondOr.getLatestTime()) {
                            //int newLSecondOr = secondOr.getLatestTime() - change;
                            if (newLSecondOr < secondOr.getEarliestTime()) {
                                System.out.println("NOT PRECEDENCE OF FEASIBLE EARLIEST/LATEST P-OPERATION "+firstOr.getID());
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public int[] checkprecedenceOfEarliest(int o, int v, int earliestTemp){
        int breakValue=0;
        int precedenceOf=precedenceALNS[o-1-startNodes.length][1];
        if(precedenceOf!=0) {
            PrecedenceValues pValues=precedenceOverOperations.get(precedenceOf);
            if (pValues == null) {
                breakValue=1;
            }
            if(breakValue==0) {
                int earliestPO = pValues.getOperationObject().getEarliestTime();
                earliestTemp = Math.max(earliestTemp, earliestPO + TimeVesselUseOnOperation[pValues.getRoute()][precedenceOf - 1 - startNodes.length][earliestPO-1]);
            }
        }
        return new int[]{earliestTemp,breakValue};
    }

    public void updateLatest(int latest, int indexInRoute, int routeIndex){
        int lastLatest=latest;
        System.out.println("Hit" + lastLatest);
        for(int k=indexInRoute-1;k>-1;k--){
            System.out.println(k);
            OperationInRoute objectK=vesselroutes.get(routeIndex).get(k);
            int opTimeK=TimeVesselUseOnOperation[routeIndex][objectK.getID()-startNodes.length-1]
                    [objectK.getLatestTime()-1];
            int updateSailingTime=0;
            System.out.println(vesselroutes.get(routeIndex).get(k).getID() + " , " + routeIndex);

            if(k==vesselroutes.get(routeIndex).size()-2){
                updateSailingTime=objectK.getLatestTime();
            }
            if(k<vesselroutes.get(routeIndex).size()-2){
                updateSailingTime=objectK.getLatestTime()+opTimeK;
            }
            System.out.println(objectK.getLatestTime() + " , " +
                    (lastLatest- SailingTimes[routeIndex][updateSailingTime-1][objectK.getID()-1]
                            [vesselroutes.get(routeIndex).get(k+1).getID()-1] -opTimeK)) ;
            int newTime=Math.min(objectK.getLatestTime(),lastLatest-
                    SailingTimes[routeIndex][updateSailingTime-1][objectK.getID()-1]
                            [vesselroutes.get(routeIndex).get(k+1).getID()-1] -opTimeK);
            System.out.println(newTime + " , " +objectK.getID());
            if(newTime==objectK.getLatestTime()){
                break;
            }
            objectK.setLatestTime(newTime);
            System.out.println(objectK.getLatestTime());
            lastLatest=newTime;
        }
    }

    public void updateEarliest(int earliest, int indexInRoute, int routeIndex){
        int lastEarliest=earliest;
        for(int f=indexInRoute+1;f<vesselroutes.get(routeIndex).size();f++){
            OperationInRoute objectFMinus1=vesselroutes.get(routeIndex).get(f-1);
            OperationInRoute objectF=vesselroutes.get(routeIndex).get(f);
            int opTimeFMinus1=TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1]
                    [objectFMinus1.getLatestTime()-1];
            int newTime=Math.max(vesselroutes.get(routeIndex).get(f).getEarliestTime(),lastEarliest+
                    SailingTimes[routeIndex][objectFMinus1.getEarliestTime()+opTimeFMinus1-1][objectFMinus1.getID()-1]
                            [objectF.getID()-1]
                    +TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1][objectFMinus1.getEarliestTime()-1]);
            if(newTime==objectF.getEarliestTime()){
                break;
            }
            vesselroutes.get(routeIndex).get(f).setEarliestTime(newTime);
            lastEarliest=newTime;

        }
    }

    public Integer checkChangeEarliest(int earliestInsertionOperation, int indexInRoute, int routeIndex, int precedenceIndex, int earliestPrecedenceOperation,int o){
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

    public Integer checkChangeLatest(int latestInsertionOperation, int indexInRoute, int routeIndex,
                                     int precedenceIndex, int latestPrecedenceOperation, int o){
        int returnValue=0;
        int lastLatest=latestInsertionOperation;
        int sailingTime;
        for(int k=indexInRoute-1;k>-1;k--){
            OperationInRoute kObject=vesselroutes.get(routeIndex).get(k);
            int timeOperationK=TimeVesselUseOnOperation[routeIndex][kObject.getID()-1-startNodes.length][kObject.getLatestTime()-1];
            if(k==indexInRoute-1){
                sailingTime=SailingTimes[routeIndex][latestInsertionOperation-1][kObject.getID()-1]
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
            if(k==precedenceIndex) {
                returnValue=latestPrecedenceOperation-lastLatest;
                break;
            }
        }
        return returnValue;
    }

    public Integer checkChangeEarliestLastOperation(int earliestInsertionOperation, int earliestLastOperation, int indexInRoute, int routeIndex,int o){
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
                operationtime=TimeVesselUseOnOperation[routeIndex][vesselroutes.get(routeIndex).get(f-1).getID()-startNodes.length-1][lastEarliest-1];
                sailingtime=SailingTimes[routeIndex][lastEarliest+operationtime-1][vesselroutes.get(routeIndex).get(f-1).getID()-1]
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

    public List<ConnectedValues> checkSimultaneous(int v){
        List<ConnectedValues> dependencies= new ArrayList<>();
        Map<Integer, ConnectedValues> dict = simultaneousOp;
        if (vesselroutes.get(v)!=null) {
            for (int i=0; i <vesselroutes.get(v).size();i++) {
                if(simALNS[vesselroutes.get(v).get(i).getID()-startNodes.length-1][0] !=0 ||
                        simALNS[vesselroutes.get(v).get(i).getID()-startNodes.length-1][1] !=0){
                    int IDop = vesselroutes.get(v).get(i).getID();
                    if(dict.get(IDop) != null){
                        dependencies.add(dict.get(IDop));
                    }
                }
            }
        }
        return dependencies;
    }

    public int[] checkSimultaneousOfTimes(int o, int v, int earliestTemp, int latestTemp){
        int breakValue=0;
        int simultaneousOf=simALNS[o-1-startNodes.length][1];
        if(simultaneousOf!=0) {
            System.out.println(simultaneousOf);
            ConnectedValues sValues=simultaneousOp.get(simultaneousOf);
            if (sValues == null) {
                breakValue=1;
            }
            if(breakValue==0) {
                int earliestPO = sValues.getOperationObject().getEarliestTime();
                earliestTemp = Math.max(earliestTemp, earliestPO);
                int latestPO = sValues.getOperationObject().getLatestTime();
                latestTemp = Math.min(latestTemp, latestPO);
            }
        }
        return new int[]{earliestTemp,latestTemp, breakValue};
    }



    public Boolean checkSOfFeasible(int o, int v, int latestCurrent) {
        int simultaneousOf=simALNS[o-1-startNodes.length][1];
        if(simultaneousOf!=0) {
            ConnectedValues sValues=simultaneousOp.get(simultaneousOf);
            System.out.println(sValues.getOperationObject().getID());
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


    public void updateSimultaneous(List<ConnectedValues> simultaneous, int routeIndex, int indexInRoute, int o) {
        if(!simultaneous.isEmpty()){
            for (ConnectedValues sValues : simultaneous) {
                //System.out.println("Update caused by simultaneous " + sValues.getOperationObject().getID() + " in route " + routeIndex);
                int cur_earliestTemp = sValues.getOperationObject().getEarliestTime();
                int cur_latestTemp = sValues.getOperationObject().getLatestTime();

                int sIndex = sValues.getIndex();
                if(indexInRoute < sIndex){
                    sValues.setIndex(sIndex+1);
                } else if(indexInRoute == sIndex && sValues.getOperationObject().getID() != o){
                    sValues.setIndex(sIndex+1);
                }
                if(sValues.getConnectedOperationObject() != null) {
                    OperationInRoute simOp = sValues.getConnectedOperationObject();
                    int earliestPO = simOp.getEarliestTime();
                    int earliestTemp = Math.max(cur_earliestTemp, earliestPO);
                    int latestPO = simOp.getLatestTime();
                    int latestTemp = Math.min(cur_latestTemp, latestPO);
                    if (earliestTemp > cur_earliestTemp) {
                        cur_earliestTemp = earliestTemp;
                        sValues.getOperationObject().setEarliestTime(cur_earliestTemp);
                        updateEarliest(cur_earliestTemp,indexInRoute,routeIndex);
                    }else if(earliestTemp>earliestPO){
                        ConnectedValues simOpObj = simultaneousOp.get(simOp.getID());
                        simOpObj.getOperationObject().setEarliestTime(cur_earliestTemp);
                        updateEarliest(cur_earliestTemp,simOpObj.getIndex(),simOpObj.getRoute());
                    }
                    if (latestTemp < cur_latestTemp) {
                        cur_latestTemp = latestTemp;
                        sValues.getOperationObject().setLatestTime(cur_latestTemp);
                        updateLatest(cur_latestTemp,indexInRoute,routeIndex);
                    }else if(latestTemp<latestPO){
                        ConnectedValues simOpObj = simultaneousOp.get(simOp.getID());
                        simOpObj.getOperationObject().setLatestTime(cur_latestTemp);
                        updateLatest(cur_latestTemp,simOpObj.getIndex(),simOpObj.getRoute());
                    }
                }
            }
        }
    }

    public Boolean checkSimOpInRoute(List<ConnectedValues> simOps, int o){
        int simA = simALNS[o-startNodes.length-1][0];
        int simB = simALNS[o-startNodes.length-1][1];
        for(ConnectedValues op : simOps){
            if(simA != 0 || simB != 0) {
                if (simA == op.getOperationObject().getID() ||
                        simB == op.getOperationObject().getID()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Boolean checkSimultaneousFeasible(List<ConnectedValues> simOps, int o, int v, int insertIndex, int earliestTemp, int latestTemp){
        if(!simOps.isEmpty()) {
            for (ConnectedValues op : simOps) {
                //System.out.println("trying to insert operation " + o + " in position " + insertIndex+ " , " +op.getOperationObject().getID() + " simultaneous operation in route " +v);
                ArrayList<ArrayList<Integer>> earliest_change = checkChangeEarliestSim(earliestTemp,insertIndex,v,o,op.getOperationObject().getID());
                if (!earliest_change.isEmpty()) {
                    for (ArrayList<Integer> connectedTimes : earliest_change) {
                        //System.out.println(connectedTimes.get(0) + " , " + connectedTimes.get(1) + " earliest change");
                        if (connectedTimes.get(0) > connectedTimes.get(1)) {
                            return false;
                        }
                    }
                }
                ArrayList<ArrayList<Integer>> latest_change = checkChangeLatestSim(latestTemp,insertIndex,v,o,op.getOperationObject().getID());
                if(!latest_change.isEmpty()){
                    for(ArrayList<Integer> connectedTimes : latest_change){
                        //System.out.println(connectedTimes.get(0) + " , " + connectedTimes.get(1) + " latest change");
                        if(connectedTimes.get(0) > connectedTimes.get(1)){
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public ArrayList<ArrayList<Integer>> checkChangeEarliestSim(int earliestInsertionOperation, int indexInRoute, int routeIndex, int o, int simID){
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
        System.out.println(op.getLatestTime() + " , " + op.getID());
        sim_earliests.add(new ArrayList<>(Arrays.asList(new_earliest,latest)));
        return sim_earliests;
    }

    public ArrayList<ArrayList<Integer>> checkChangeLatestSim(int latestInsertionOperation, int indexInRoute, int routeIndex,
                                     int o, int simID){
        ArrayList<ArrayList<Integer>> sim_latests = new ArrayList<>();
        int lastLatest=latestInsertionOperation;
        int sailingTime;
        for(int k=indexInRoute-1;k>-1;k--){
            OperationInRoute kObject=vesselroutes.get(routeIndex).get(k);
            int timeOperationK=TimeVesselUseOnOperation[routeIndex][kObject.getID()-1-startNodes.length][kObject.getLatestTime()-1];
            if(k==indexInRoute-1){
                sailingTime=SailingTimes[routeIndex][latestInsertionOperation-1][kObject.getID()-1]
                        [o-1];
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
            if(vesselroutes.get(routeIndex).get(k).getID() == simID) {
                break;
            }
        }
        OperationInRoute op = simultaneousOp.get(simID).getConnectedOperationObject();
        int new_latest = Math.min(lastLatest, op.getLatestTime());
        int earliest = op.getEarliestTime();
        sim_latests.add(new ArrayList<>(Arrays.asList(earliest,new_latest)));
        return sim_latests;
    }


    public void updateSimultaneousAfterRemoval(List<ConnectedValues> simultaneous, int routeIndex, int indexInRoute, int o) {
        if(!simultaneous.isEmpty()){
            for (ConnectedValues sValues : simultaneous) {
                System.out.println("Nå er vi her");
                //System.out.println("Update caused by simultaneous " + sValues.getOperationObject().getID() + " in route " + routeIndex);
                int cur_earliestTemp = sValues.getOperationObject().getEarliestTime();
                int cur_latestTemp = sValues.getOperationObject().getLatestTime();
                System.out.println(cur_earliestTemp);
                System.out.println(cur_latestTemp);
                int sIndex = sValues.getIndex();
                if(indexInRoute < sIndex){
                    sValues.setIndex(sIndex-1);
                }

                if(sValues.getConnectedOperationObject() != null) {
                    OperationInRoute simOp = sValues.getConnectedOperationObject();
                    ConnectedValues simOpObj = simultaneousOp.get(simOp.getID());
                    int conOpPrevEarliest = vesselroutes.get(simOpObj.getRoute()).get(simOpObj.getIndex()-1).getEarliestTime();
                    int conOpNextLatest = vesselroutes.get(simOpObj.getRoute()).get(simOpObj.getIndex()+1).getLatestTime();
                    int conOpPrevOpTime = TimeVesselUseOnOperation[simOpObj.getRoute()]
                            [vesselroutes.get(simOpObj.getRoute()).get(simOpObj.getIndex()-1).getID()-startNodes.length-1][conOpPrevEarliest];
                    int earliestPO = conOpPrevEarliest + conOpPrevOpTime
                             + SailingTimes[simOpObj.getRoute()][conOpPrevEarliest+conOpPrevOpTime]
                            [vesselroutes.get(simOpObj.getRoute()).get(simOpObj.getIndex()-1).getID()-1][simOpObj.getOperationObject().getID()-1];
                    System.out.println(earliestPO);
                    int conOpOpTime = TimeVesselUseOnOperation[simOpObj.getRoute()]
                            [simOpObj.getOperationObject().getID()-startNodes.length-1][earliestPO];
                    int latestPO = conOpNextLatest - conOpOpTime -
                            SailingTimes[simOpObj.getRoute()][earliestPO][simOpObj.getOperationObject().getID()-1]
                            [vesselroutes.get(simOpObj.getRoute()).get(simOpObj.getIndex()+1).getID()-1];
                    System.out.println(latestPO);
                    int earliestTemp = Math.max(cur_earliestTemp, earliestPO);
                    int latestTemp = Math.min(cur_latestTemp, latestPO);

                    if (earliestTemp > cur_earliestTemp) {
                        cur_earliestTemp = earliestTemp;
                        sValues.getOperationObject().setEarliestTime(cur_earliestTemp);
                        simOpObj.getOperationObject().setEarliestTime(cur_earliestTemp);
                        updateEarliest(cur_earliestTemp,sValues.getIndex(),routeIndex);
                    }else if(earliestTemp>earliestPO){
                        simOpObj.getOperationObject().setEarliestTime(cur_earliestTemp);
                        updateEarliest(cur_earliestTemp,simOpObj.getIndex(),simOpObj.getRoute());
                    }
                    if (latestTemp < cur_latestTemp) {
                        cur_latestTemp = latestTemp;
                        simOpObj.getOperationObject().setLatestTime(cur_latestTemp);
                        sValues.getOperationObject().setLatestTime(cur_latestTemp);
                        updateLatest(cur_latestTemp,sValues.getIndex(),routeIndex);
                    }else if(latestTemp<latestPO){
                        simOpObj.getOperationObject().setLatestTime(cur_latestTemp);
                        updateLatest(cur_latestTemp,simOpObj.getIndex(),simOpObj.getRoute());
                    }
                }
            }
        }
    }
    public void updateLatestAfterRemoval(int latest, int indexInRoute, int routeIndex){
        int lastLatest=latest;
        System.out.println("Hit");
        for(int k=indexInRoute-1;k>-1;k--){
            System.out.println(k);
            OperationInRoute objectK=vesselroutes.get(routeIndex).get(k);
            int opTimeK=TimeVesselUseOnOperation[routeIndex][objectK.getID()-startNodes.length-1]
                    [objectK.getLatestTime()-1];
            int updateSailingTime=0;
            System.out.println(vesselroutes.get(routeIndex).size());
            if(k==vesselroutes.get(routeIndex).size()-2){
                updateSailingTime=objectK.getLatestTime();
            }
            if(k<vesselroutes.get(routeIndex).size()-2){
                updateSailingTime=objectK.getLatestTime()+opTimeK;
            }
            System.out.println(updateSailingTime);
            int newTime=lastLatest-
                    SailingTimes[routeIndex][updateSailingTime-1][objectK.getID()-1]
                            [vesselroutes.get(routeIndex).get(k+1).getID()-1] -opTimeK;
            System.out.println(newTime + " , " +objectK.getID());
            if(newTime==objectK.getLatestTime()){
                break;
            }
            objectK.setLatestTime(newTime);
            lastLatest=newTime;
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
        PrintData.printSimALNS(simALNS);
        PrintData.printTimeWindows(timeWindowsForOperations);
        PrintData.printTimeWindowsIntervals(twIntervals);

        System.out.println("Sailing cost per route: "+Arrays.toString(routeSailingCost));
        System.out.println("Operation gain per route: "+Arrays.toString(routeOperationGain));
        System.out.println("Objective value: "+objValue);
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
        int[] vesseltypes =new int[]{1,2,3,4};
        int[] startnodes=new int[]{1,2,3,4};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes,
                "test_instances/test_instance_15_locations_SIMtest1.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        ConstructionHeuristic_copy a = new ConstructionHeuristic_copy(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations());
        PrintData.printSimALNS(dg.getSimultaneousALNS());
        PrintData.printPrecedenceALNS(dg.getPrecedenceALNS());
        //PrintData.printSailingTimes(dg.getSailingTimes(),4,17,4);
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),4);
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
