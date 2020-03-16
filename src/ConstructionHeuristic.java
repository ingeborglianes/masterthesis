import java.io.FileNotFoundException;
import java.util.*;

public class ConstructionHeuristic {
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

    public ConstructionHeuristic(int [][] OperationsForVessel, int [][] TimeWindowsForOperations, int [][][] Edges, int [][][][] SailingTimes,
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
            for (int v = 0; v < nVessels; v++) {
                if(actionTime[v]+TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]>60){
                    System.out.println("Break because of actiontime");
                    continue;
                }
                List<PrecedenceValues> precedenceOver= checkPrecedence(v,0);
                List<PrecedenceValues> precedenceOf= checkPrecedence(v,1);
                boolean precedenceOverFeasible=true;
                boolean precedenceOfFeasible=true;
                if (DataGenerator.containsElement(o, OperationsForVessel[v])) {
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
                            System.out.println("BREAK");
                            break outer;
                        }
                        earliestTemp=precedenceOfValuesEarliest[0];
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
                                    System.out.println("BREAK");
                                    break outer;
                                }
                                earliestTemp=precedenceOfValuesEarliest[0];
                                int sailingTimeOToNext=SailingTimes[v][earliestTemp-1][o - 1][vesselroutes.get(v).get(0).getID() - 1];
                                int latestTemp = Math.min(vesselroutes.get(v).get(0).getLatestTime() - sailingTimeOToNext
                                                - opTime
                                        , twIntervals[o - startNodes.length - 1][1]);
                                int timeIncrease=sailingTimeStartNodeToO + sailingTimeOToNext
                                        - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselroutes.get(v).get(0).getID() - 1];
                                if(timeIncrease < timeAdded && earliestTemp<=latestTemp && earliestTemp<vesselroutes.get(v).get(0).getEarliestTime()) {
                                    precedenceOverFeasible = checkPOverFeasible(precedenceOver, o, timeIncrease, 0, earliestTemp);
                                    precedenceOfFeasible = checkPOfFeasible(precedenceOf, o, timeIncrease, 0, latestTemp);
                                    if(precedenceOverFeasible && precedenceOfFeasible ) {
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
                                int earliestN=vesselroutes.get(v).get(n).getEarliestTime();
                                int operationTimeN=TimeVesselUseOnOperation[v][vesselroutes.get(v).get(n).getID()-1-startNodes.length][earliestN-1];
                                int startTimeSailingTimePrevToO=earliestN+operationTimeN;
                                int sailingTimePrevToO=SailingTimes[v][startTimeSailingTimePrevToO-1]
                                        [vesselroutes.get(v).get(n).getID()-1][o - 1];
                                int earliestTemp=Math.max(earliestN + operationTimeN + sailingTimePrevToO
                                        ,twIntervals[o-startNodes.length-1][0]);
                                int latestTemp=twIntervals[o-startNodes.length-1][1];
                                int[] precedenceOfValuesEarliest=checkprecedenceOfEarliest(o,v,earliestTemp);
                                if (precedenceOfValuesEarliest[1]==1) {
                                    System.out.println("BREAK");
                                    break outer;
                                }
                                earliestTemp=precedenceOfValuesEarliest[0];
                                int timeIncrease=sailingTimePrevToO;
                                if(timeIncrease < timeAdded && earliestTemp<=latestTemp) {
                                    precedenceOverFeasible= checkPOverFeasible(precedenceOver, o, timeIncrease,n+1,earliestTemp);
                                    precedenceOfFeasible= checkPOfFeasible(precedenceOf, o, timeIncrease,n+1,latestTemp);
                                    if(precedenceOverFeasible && precedenceOfFeasible) {
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
                            if(n<vesselroutes.get(v).size()-1){
                                //check insertion for all other positions in the route
                                int earliestN=vesselroutes.get(v).get(n).getEarliestTime();
                                int operationTimeN=TimeVesselUseOnOperation[v][vesselroutes.get(v).get(n).getID()-1-startNodes.length][earliestN-1];
                                int startTimeSailingTimePrevToO=earliestN+operationTimeN;
                                int sailingTimePrevToO=SailingTimes[v][startTimeSailingTimePrevToO-1][vesselroutes.get(v).get(n).getID()-1][o - 1];
                                int earliestTemp=Math.max(earliestN+ sailingTimePrevToO +operationTimeN,twIntervals[o-startNodes.length-1][0]);
                                int[] precedenceOfValuesEarliest=checkprecedenceOfEarliest(o,v,earliestTemp);
                                if (precedenceOfValuesEarliest[1]==1) {
                                    System.out.println("BREAK");
                                    break outer;
                                }
                                earliestTemp = precedenceOfValuesEarliest[0];
                                int opTime = TimeVesselUseOnOperation[v][o - 1 - startNodes.length][earliestTemp - 1];
                                int sailingTimeOToNext = SailingTimes[v][Math.min(earliestTemp + opTime - 1, 59)][o - 1][vesselroutes.get(v).get(n + 1).getID() - 1];
                                int sailingTimePrevToNext = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselroutes.get(v).get(n).getID() - 1][vesselroutes.get(v).get(n + 1).getID() - 1];
                                int latestTemp = Math.min(vesselroutes.get(v).get(n + 1).getLatestTime() -
                                        sailingTimeOToNext - opTime, twIntervals[o - startNodes.length - 1][1]);
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
                                    if(precedenceOverFeasible && precedenceOfFeasible) {
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
                /*
                System.out.println("NEW ADD: Vessel route "+routeIndex);
                System.out.println("Operation "+o);
                System.out.println("Earliest time "+ earliest);
                System.out.println("Latest time "+ latest);
                System.out.println("Route index "+indexInRoute);
                System.out.println(" ");

                 */
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
                            if(t>59){
                                t=59;
                            }
                            int newESecondOr=firstOr.getEarliestTime() + TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                                    [t]
                                    + change;
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
        for(int k=indexInRoute-1;k>-1;k--){
            OperationInRoute objectK=vesselroutes.get(routeIndex).get(k);
            int opTimeK=TimeVesselUseOnOperation[routeIndex][objectK.getID()-startNodes.length-1]
                    [objectK.getLatestTime()-1];
            int updateSailingTime=0;
            if(k==vesselroutes.get(routeIndex).size()-2){
                updateSailingTime=objectK.getLatestTime();
            }
            if(k<vesselroutes.get(routeIndex).size()-2){
                updateSailingTime=objectK.getLatestTime()+opTimeK;
            }
            int newTime=Math.min(objectK.getLatestTime(),lastLatest-
                    SailingTimes[routeIndex][updateSailingTime-1][objectK.getID()-1]
                            [vesselroutes.get(routeIndex).get(k+1).getID()-1]
                    -opTimeK);
            if(newTime==objectK.getLatestTime()){
                break;
            }
            objectK.setLatestTime(newTime);
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
                "test_instances/test_instance_15_locations_PRECEDENCEtest4.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        ConstructionHeuristic a = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
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
