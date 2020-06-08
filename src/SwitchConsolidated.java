import java.util.*;

public class SwitchConsolidated {
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
    private int[][][] operationGainGurobi;
    private int[] routeSailingCost;
    private int[] routeOperationGain;
    private int objValue;
    private int nVessels;
    private int nTimePeriods;
    private int [][] OperationsForVessel;
    private int [] vesselTypes;
    private int countSwap=0;
    private Double[][] weatherPenaltyOperations;

    Map<Integer,List<InsertionValues>> allFeasibleInsertions = new HashMap<>();

    public SwitchConsolidated(Map<Integer, PrecedenceValues> precedenceOverOperations, Map<Integer, PrecedenceValues> precedenceOfOperations,
                              Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, ConnectedValues>> simOpRoutes,
                              List<Map<Integer, PrecedenceValues>> precedenceOfRoutes, List<Map<Integer, PrecedenceValues>> precedenceOverRoutes,
                              Map<Integer, ConsolidatedValues> consolidatedOperations, List<OperationInRoute> unroutedTasks,
                              List<List<OperationInRoute>> vesselRoutes, int[][] twIntervals,
                              int[][] precedenceALNS, int[][] simALNS, int[] startNodes, int[][][][] SailingTimes,
                              int[][][] TimeVesselUseOnOperation, int[] SailingCostForVessel, int[] EarliestStartingTimeForVessel,
                              int[][][] operationGain, int[][] bigTasksALNS, int[][] OperationsForVessel, int[][][] operationGainGurobi,
                              int[] vesselTypes,Double[][] weatherPenaltyOperations){
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
        this.nVessels=vesselRoutes.size();
        this.nTimePeriods=TimeVesselUseOnOperation[0][0].length;
        this.OperationsForVessel=OperationsForVessel;
        unroutedTasks.sort(Comparator.comparing(OperationInRoute::getID));
        this.operationGainGurobi=operationGainGurobi;
        this.vesselTypes=vesselTypes;
        this.weatherPenaltyOperations=weatherPenaltyOperations;
    }

    public int calculateProfitIncrease(int r, int i){
        OperationInRoute or = vesselRoutes.get(r).get(i);
        int operationID = or.getID();
        int sailingTimePrevToCurrent=0;
        int sailingTimeCurrentToNext = 0;
        int sailingTimePrevToNext = 0;
        int profitIncreaseForOperation;
        if (vesselRoutes.get(r).size() == 1) {
            sailingTimePrevToCurrent = SailingTimes[r][EarliestStartingTimeForVessel[r]][r][operationID - 1];
        } else {
            if (i!=0){
                OperationInRoute prevOr = vesselRoutes.get(r).get(i - 1);
                int earliestPrev = prevOr.getEarliestTime();
                int operationTimePrev = TimeVesselUseOnOperation[r][prevOr.getID() - 1 - startNodes.length][earliestPrev - 1];
                int startTimeSailingTimePrev = earliestPrev + operationTimePrev;
                sailingTimePrevToCurrent = SailingTimes[r][startTimeSailingTimePrev - 1][prevOr.getID() - 1][or.getID() - 1];
                if(i!=vesselRoutes.get(r).size()-1){
                    OperationInRoute nextOr = vesselRoutes.get(r).get(i + 1);
                    sailingTimePrevToNext = SailingTimes[r][startTimeSailingTimePrev - 1][prevOr.getID() - 1][nextOr.getID() - 1];
                }
            }
            if(i!=vesselRoutes.get(r).size()-1){
                OperationInRoute nextOr = vesselRoutes.get(r).get(i + 1);
                int earliestCurrent = or.getEarliestTime();
                int operationTimeCurrent = TimeVesselUseOnOperation[r][or.getID() - 1 - startNodes.length][earliestCurrent - 1];
                int startTimeSailingTimeCurrent = earliestCurrent + operationTimeCurrent;
                sailingTimeCurrentToNext = SailingTimes[r][startTimeSailingTimeCurrent - 1][or.getID() - 1][nextOr.getID() - 1];
            }

            if (i == 0) {
                OperationInRoute nextOr = vesselRoutes.get(r).get(i + 1);
                sailingTimePrevToCurrent = SailingTimes[r][EarliestStartingTimeForVessel[r]][r][operationID - 1];
                sailingTimePrevToNext = SailingTimes[r][EarliestStartingTimeForVessel[r]][r][nextOr.getID() - 1];
            }
            if (i == vesselRoutes.get(r).size()-1) {
                sailingTimeCurrentToNext = 0;
                sailingTimePrevToNext = 0;
            }
        }
        int sailingDiff = -sailingTimePrevToCurrent - sailingTimeCurrentToNext + sailingTimePrevToNext;
        profitIncreaseForOperation = operationGain[r][operationID - startNodes.length - 1][or.getEarliestTime()-1] +
                sailingDiff * SailingCostForVessel[r];
        return profitIncreaseForOperation;
    }

    public void removeSynchronizedOp(ConnectedValues simOp, PrecedenceValues precedenceOverOp, PrecedenceValues precedenceOfOp,int selectedTaskID,
                                     OperationInRoute selectedTask){
        //System.out.println("SIM dict: "+simultaneousOp.get(selectedTaskID));
        int route=0;
        int index=0;
        if(simOp!=null){
            //System.out.println("Simultaneous, operation: "+selectedTask.getID());
            route=simOp.getRoute();
            index=simOp.getIndex();
        }
        else if(precedenceOverOp!=null){
            //System.out.println("Precedence over, operation: "+selectedTask.getID());
            route=precedenceOverOp.getRoute();
            index=precedenceOverOp.getIndex();
        }
        else if(precedenceOfOp!=null){
            //System.out.println("Precedence of, operation: "+selectedTask.getID());
            route=precedenceOfOp.getRoute();
            index=precedenceOfOp.getIndex();
        }
        if(bigTasksALNS[selectedTask.getID()-1-startNodes.length]==null){
            unroutedTasks.add(selectedTask);
        }
        else{
            int bigTaskID=bigTasksALNS[selectedTaskID-1-startNodes.length][0];
            OperationInRoute bigOP=null;
            for(OperationInRoute oir: unroutedTasks){
                if (oir.getID()==bigTaskID){
                    bigOP=oir;
                }
            }
            if(!unroutedTasks.contains(bigOP)){
                unroutedTasks.add(selectedTask);
                unroutedTasks.add(new OperationInRoute(bigTasksALNS[selectedTask.getID()-1-startNodes.length][0],0,0));
                if(selectedTaskID==bigTasksALNS[selectedTask.getID()-1-startNodes.length][1]){
                    unroutedTasks.add(new OperationInRoute(bigTasksALNS[selectedTask.getID()-1-startNodes.length][2],0,0));
                }
                else{
                    unroutedTasks.add(new OperationInRoute(bigTasksALNS[selectedTask.getID()-1-startNodes.length][1],0,0));
                }
            }
        }
        //System.out.println("Operation to remove: "+selectedTaskID);
        vesselRoutes.get(route).remove(index);
        ConstructionHeuristic.updateIndexesRemoval(route, index, vesselRoutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
        if(simOp!=null){
            simultaneousOp.remove(selectedTaskID);
            //System.out.println("operation removed sim: "+selectedTaskID);
            simOpRoutes.get(route).remove(selectedTaskID);
            if (bigTasksALNS[selectedTaskID - 1 - startNodes.length] != null && bigTasksALNS[selectedTaskID - startNodes.length - 1][2] == selectedTaskID) {
                consolidatedOperations.replace(bigTasksALNS[selectedTaskID - startNodes.length - 1][0],
                        new ConsolidatedValues(false, false, 0, 0, 0));
            }
            else if (bigTasksALNS[selectedTaskID - 1 - startNodes.length] != null && bigTasksALNS[selectedTaskID - startNodes.length - 1][1] == selectedTaskID) {
                consolidatedOperations.put(bigTasksALNS[selectedTaskID - startNodes.length - 1][0],
                        new ConsolidatedValues(false, false, 0, 0, 0));
            }
        }
        if(precedenceOverOp!=null){
            //System.out.println("operation removed pres over: "+selectedTaskID);
            precedenceOverOperations.remove(selectedTaskID);
            precedenceOverRoutes.get(route).remove(selectedTaskID);
        }
        if(precedenceOfOp!=null){
            //System.out.println("operation removed pres of: "+selectedTaskID);
            precedenceOfOperations.remove(selectedTaskID);
            precedenceOfRoutes.get(route).remove(selectedTaskID);
        }
    }

    public void removeNormalOp(OperationInRoute selectedTask, int route, int index){
        if (bigTasksALNS[selectedTask.getID() - 1 - startNodes.length] != null &&
                bigTasksALNS[selectedTask.getID() - startNodes.length - 1][0] == selectedTask.getID()) {
            consolidatedOperations.replace(bigTasksALNS[selectedTask.getID() - startNodes.length - 1][0],
                    new ConsolidatedValues(false, false, 0, 0, 0));
        }
        if(bigTasksALNS[selectedTask.getID()-1-startNodes.length]==null){
            unroutedTasks.add(selectedTask);
        }
        else{
            unroutedTasks.add(selectedTask);
            unroutedTasks.add(new OperationInRoute(bigTasksALNS[selectedTask.getID()-1-startNodes.length][1],0,0));
            unroutedTasks.add(new OperationInRoute(bigTasksALNS[selectedTask.getID()-1-startNodes.length][2],0,0));
        }
        //System.out.println("REMOVE NORMAL OP: "+selectedTask.getID());
        vesselRoutes.get(route).remove(index);
        ConstructionHeuristic.updateIndexesRemoval(route, index, vesselRoutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
    }

    public void updatePrecedenceOverAfterRemovals(Map<Integer,PrecedenceValues> precedenceOver){
        if(precedenceOver!=null){
            for (PrecedenceValues pValues : precedenceOver.values()) {
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                if (secondOr != null) {
                    PrecedenceValues connectedOpPValues = precedenceOfOperations.get(secondOr.getID());
                    if(connectedOpPValues!=null) {
                        int routeConnectedOp = connectedOpPValues.getRoute();
                        int route = pValues.getRoute();
                        if (routeConnectedOp == pValues.getRoute()) {
                            continue;
                        }
                        int newESecondOr = firstOr.getEarliestTime() + TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                                [firstOr.getEarliestTime() - 1];
                        int indexConnected = connectedOpPValues.getIndex();
                        //System.out.println("Index demands update");
                        //System.out.println("Old earliest: " + secondOr.getEarliestTime());
                        //System.out.println("New earliest: " + newESecondOr);
                        if (secondOr.getEarliestTime() < newESecondOr) {
                            secondOr.setEarliestTime(newESecondOr);
                            ConstructionHeuristic.updateEarliest(newESecondOr, indexConnected, routeConnectedOp, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes,"not local");
                            ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(routeConnectedOp), connectedOpPValues.getIndex(), simOpRoutes, precedenceOfOperations,
                                    precedenceOverOperations, TimeVesselUseOnOperation, startNodes, precedenceOverRoutes,
                                    precedenceOfRoutes, simultaneousOp, vesselRoutes, SailingTimes,twIntervals);
                            ConstructionHeuristic.updateSimultaneous(simOpRoutes, routeConnectedOp, connectedOpPValues.getIndex(),
                                    simultaneousOp, precedenceOverRoutes, precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations,
                                    precedenceOfOperations, vesselRoutes,twIntervals);
                        }
                        //System.out.println("update earliest because of precedence over");
                    }
                }
            }
        }
    }

    public void updatePrecedenceOfAfterRemovals(Map<Integer,PrecedenceValues> precedenceOf){
        if(precedenceOf!=null){
            for (PrecedenceValues pValues : precedenceOf.values()) {
                OperationInRoute firstOr = pValues.getOperationObject();
                //System.out.println("FirstOr: "+firstOr.getID());
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                //System.out.println(" Second or: "+secondOr.getID());
                PrecedenceValues connectedOpPValues = precedenceOverOperations.get(secondOr.getID());
                if(connectedOpPValues!=null) {
                    //System.out.println(connectedOpPValues);
                    //System.out.println("STATUS BEFORE REMOVAL");
                    int routeConnectedOp = connectedOpPValues.getRoute();
                    if (routeConnectedOp == pValues.getRoute()) {
                        continue;
                    }
                    int indexConnected = connectedOpPValues.getIndex();
                    int newLSecondOr = firstOr.getLatestTime() - TimeVesselUseOnOperation[pValues.getConnectedRoute()][secondOr.getID() - startNodes.length - 1]
                            [secondOr.getLatestTime() - 1];
                    if (secondOr.getLatestTime() > newLSecondOr) {
                        secondOr.setLatestTime(newLSecondOr);
                        //System.out.println("index connected: " + indexConnected);
                        ConstructionHeuristic.updateLatest(newLSecondOr, indexConnected, pValues.getConnectedRoute(), TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes,"notLocal",
                                                        simultaneousOp,precedenceOfOperations,precedenceOverOperations,twIntervals);
                        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(routeConnectedOp), connectedOpPValues.getIndex(), TimeVesselUseOnOperation,
                                startNodes, simOpRoutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, precedenceOverRoutes,
                                vesselRoutes, simultaneousOp, SailingTimes,twIntervals);
                        ConstructionHeuristic.updateSimultaneous(simOpRoutes, routeConnectedOp, connectedOpPValues.getIndex(), simultaneousOp, precedenceOverRoutes,
                                precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations, precedenceOfOperations, vesselRoutes,twIntervals);
                        //System.out.println("update latest because of precedence of");
                    }
                }
            }
        }
    }

    public void updateAllTimesAfterRemoval(){
        //System.out.println("UPDATE TIMES AFTER ALL REMOVALS");
        List<Integer> updatedRoutes = new ArrayList<Integer>();
        for (int route : updatedRoutes) {
            if (vesselRoutes.get(route) != null && vesselRoutes.get(route).size() != 0) {
                //System.out.println("Updating route: " + route);
                int earliest = Math.max(SailingTimes[route][EarliestStartingTimeForVessel[route]][startNodes[route] - 1]
                        [vesselRoutes.get(route).get(0).getID() - 1] + 1, twIntervals[vesselRoutes.get(route).get(0).getID() - startNodes.length - 1][0]);
                int latest = Math.min(SailingTimes[0].length, twIntervals
                        [vesselRoutes.get(route).get(vesselRoutes.get(route).size() - 1).getID() - 1 - startNodes.length][1]);
                vesselRoutes.get(route).get(0).setEarliestTime(earliest);
                vesselRoutes.get(route).get(vesselRoutes.get(route).size() - 1).setLatestTime(latest);
                ConstructionHeuristic.updateEarliestAfterRemoval(earliest, 0, route, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes,twIntervals);
                ConstructionHeuristic.updateLatestAfterRemoval(latest, vesselRoutes.get(route).size() - 1, route, vesselRoutes, TimeVesselUseOnOperation,
                        startNodes, SailingTimes,twIntervals,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
            }
        }

        for (int route : updatedRoutes) {
            if (vesselRoutes.get(route) != null && vesselRoutes.get(route).size() != 0) {
                ConstructionHeuristic.updateSimultaneous(simOpRoutes, route, 0, simultaneousOp, precedenceOverRoutes,
                        precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations, precedenceOfOperations,
                        vesselRoutes,twIntervals);
                updatePrecedenceOverAfterRemovals(precedenceOverRoutes.get(route));
                updatePrecedenceOfAfterRemovals(precedenceOfRoutes.get(route));
            }
        }
    }

    public void runSwitchConsolidated(){
        allFeasibleInsertions=new HashMap<>();
        String alreadyInserted=" ";
        for (Map.Entry<Integer, ConsolidatedValues> entry : consolidatedOperations.entrySet()) {
            int bigTask = entry.getKey();
            int small1= bigTasksALNS[bigTask-1-startNodes.length][1];
            int small2= bigTasksALNS[bigTask-1-startNodes.length][2];
            ConsolidatedValues conVals = entry.getValue();
            if (!(!conVals.getConsolidated() && !conVals.getSmallTasks())){
                InsertionValues small1InsertionValues = null;
                InsertionValues small2InsertionValues = null;
                InsertionValues bigTaskInsertionValues = null;
                if(conVals.getConsolidated()){
                    alreadyInserted="big";
                    int routeBigTask=conVals.getConsolidatedRoute();
                    int index=-1;
                    int earliest=-1;
                    int latest=-1;
                    for(int n=0;n<vesselRoutes.get(routeBigTask).size();n++){
                        if(vesselRoutes.get(routeBigTask).get(n).getID()==bigTask){
                            index=n;
                            earliest=vesselRoutes.get(routeBigTask).get(n).getEarliestTime();
                            latest=vesselRoutes.get(routeBigTask).get(n).getLatestTime();
                        }
                    }
                    /*
                    for (int i = 0; i < vesselRoutes.size(); i++) {
                        System.out.println("VESSELINDEX " + i);
                        if (vesselRoutes.get(i) != null) {
                            for (int o = 0; o < vesselRoutes.get(i).size(); o++) {
                                System.out.println("Operation number: " + vesselRoutes.get(i).get(o).getID() + " Earliest start time: " +
                                        vesselRoutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselRoutes.get(i).get(o).getLatestTime());
                            }
                        }
                    }
                    System.out.println("Route big task "+routeBigTask);
                    System.out.println("Big task: "+bigTask);

                     */

                    int benefitIncreaseBigTask=calculateProfitIncrease(routeBigTask,index);
                    bigTaskInsertionValues=new InsertionValues(benefitIncreaseBigTask,index,routeBigTask,earliest,latest);
                    removeNormalOp(vesselRoutes.get(routeBigTask).get(index),routeBigTask,index);
                    updateAllTimesAfterRemoval();
                    //System.out.println("finished remove operations");
                    //Start to evaluate and insert the simultaneous small tasks
                    if (precedenceALNS[small1-startNodes.length-1][1]!=0) {
                        int presOfOp=precedenceALNS[small1-startNodes.length-1][1];
                        PrecedenceValues pv = precedenceOverOperations.get(presOfOp);
                        LargeNeighboorhoodSearchInsert.findInsertionCosts(new OperationInRoute(small1,0,0),-1,-1,
                                pv.getOperationObject().getEarliestTime(),pv.getRoute(),-1,pv.getIndex(),-1
                                ,nTimePeriods,nVessels,OperationsForVessel,vesselRoutes,
                                SailingTimes,EarliestStartingTimeForVessel,SailingCostForVessel,twIntervals,startNodes,simALNS,
                                operationGain,precedenceALNS,TimeVesselUseOnOperation,allFeasibleInsertions,precedenceOverOperations,precedenceOfOperations,
                                simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,simOpRoutes,unroutedTasks,vesselTypes,
                                false,null,0,weatherPenaltyOperations);
                    }
                    else{
                        LargeNeighboorhoodSearchInsert.findInsertionCosts(new OperationInRoute(small1,0,0),-1,-1,
                                -1,-1,-1,-1,-1
                                ,nTimePeriods,nVessels,OperationsForVessel,vesselRoutes,
                                SailingTimes,EarliestStartingTimeForVessel,SailingCostForVessel,twIntervals,startNodes,simALNS,
                                operationGain,precedenceALNS,TimeVesselUseOnOperation,allFeasibleInsertions,precedenceOverOperations,precedenceOfOperations,
                                simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,simOpRoutes,unroutedTasks,vesselTypes,false,null,0,weatherPenaltyOperations);
                    }
                    //System.out.println("insertion possibilities found for sim a");
                    for (int i=0;i<allFeasibleInsertions.get(small1).size();i++){
                        InsertionValues option =allFeasibleInsertions.get(small1).get(i);
                        if(option.getBenenefitIncrease()==-100000){
                            if(allFeasibleInsertions.get(small2)==null){
                                allFeasibleInsertions.put(small2, new ArrayList<>() {{
                                    add(new InsertionValues(-100000, -1, -1, -1, -1));
                                }});
                            }
                            else{
                                allFeasibleInsertions.get(small2).
                                        add(new InsertionValues(-100000, -1, -1, -1, -1));
                            }
                        }
                        else{
                            int presOfOp=precedenceALNS[small2-1-startNodes.length][1];
                            PrecedenceValues pv = precedenceOverOperations.get(presOfOp);
                            int precedenceIndex=-1;
                            int precedenceRoute=-1;
                            int earliestP=-1;
                            if(pv!=null){
                                precedenceIndex=pv.getIndex();
                                precedenceRoute=pv.getRoute();
                                earliestP=pv.getOperationObject().getEarliestTime();
                            }
                            LargeNeighboorhoodSearchInsert.findInsertionCosts(new OperationInRoute(small2,0,0),option.getEarliest(),
                                    option.getLatest(),earliestP,precedenceRoute,option.getRouteIndex(),precedenceIndex,option.getIndexInRoute()
                                    ,nTimePeriods,nVessels,OperationsForVessel,vesselRoutes,
                                    SailingTimes,EarliestStartingTimeForVessel,SailingCostForVessel,twIntervals,startNodes,simALNS,
                                    operationGain,precedenceALNS,TimeVesselUseOnOperation,allFeasibleInsertions,precedenceOverOperations,precedenceOfOperations,
                                    simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,simOpRoutes,unroutedTasks,vesselTypes,false,null,0,weatherPenaltyOperations);
                            int size=allFeasibleInsertions.get(small2).size();
                            InsertionValues ourValues=allFeasibleInsertions.get(small2).get(size-1);
                            int ourBenefitIncrease=ourValues.getBenenefitIncrease();
                            if(ourBenefitIncrease==-100000) {
                                option.setBenenefitIncrease(-100000);
                            }
                            else {
                                int benefitIncreaseBothOperations = option.getBenenefitIncrease() + ourBenefitIncrease;
                                if (i > 0) {
                                    if (benefitIncreaseBothOperations >= allFeasibleInsertions.get(small1).get(0).getBenenefitIncrease()) {
                                        allFeasibleInsertions.get(small1).remove(i);
                                        allFeasibleInsertions.get(small1).add(0, option);
                                        allFeasibleInsertions.get(small2).remove(size - 1);
                                        allFeasibleInsertions.get(small2).add(0, ourValues);
                                    } else if (!(benefitIncreaseBothOperations < allFeasibleInsertions.get(small1).get(allFeasibleInsertions.get(small1).size() - 1).getBenenefitIncrease())) {
                                        for (int s = 1; s < allFeasibleInsertions.get(small1).size(); s++) {
                                            if (benefitIncreaseBothOperations < allFeasibleInsertions.get(small1).get(s - 1).getBenenefitIncrease() &&
                                                    benefitIncreaseBothOperations >= allFeasibleInsertions.get(small1).get(s).getBenenefitIncrease()) {
                                                allFeasibleInsertions.get(small1).remove(i);
                                                allFeasibleInsertions.get(small1).add(s, option);
                                                allFeasibleInsertions.get(small2).remove(size - 1);
                                                allFeasibleInsertions.get(small2).add(s, ourValues);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    small1InsertionValues=allFeasibleInsertions.get(small1).get(0);
                    small2InsertionValues=allFeasibleInsertions.get(small2).get(0);
                }
                else if(conVals.getSmallTasks()){
                    alreadyInserted="small";
                    int routeSmall1=conVals.getConnectedRoute1();
                    int routeSmall2=conVals.getConnectedRoute2();
                    int index=-1;
                    int earliest=-1;
                    int latest=-1;
                    for(int n=0;n<vesselRoutes.get(routeSmall1).size();n++){
                        if(vesselRoutes.get(routeSmall1).get(n).getID()==small1){
                            index=n;
                            earliest=vesselRoutes.get(routeSmall1).get(n).getEarliestTime();
                            latest=vesselRoutes.get(routeSmall1).get(n).getLatestTime();
                        }
                    }
                    /*
                    for (int i = 0; i < vesselRoutes.size(); i++) {
                        System.out.println("VESSELINDEX " + i);
                        if (vesselRoutes.get(i) != null) {
                            for (int o = 0; o < vesselRoutes.get(i).size(); o++) {
                                System.out.println("Operation number: " + vesselRoutes.get(i).get(o).getID() + " Earliest start time: " +
                                        vesselRoutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselRoutes.get(i).get(o).getLatestTime());
                            }
                        }
                    }
                    System.out.println(small1);

                     */
                    int benefitIncreaseSmall1=calculateProfitIncrease(routeSmall1,index);
                    small1InsertionValues=new InsertionValues(benefitIncreaseSmall1,index,routeSmall1,earliest,latest);
                    removeSynchronizedOp(simultaneousOp.get(small1),precedenceOverOperations.get(small1),precedenceOfOperations.get(small1),
                            small1,vesselRoutes.get(routeSmall1).get(index));
                    int index2=-1;
                    int earliest2=-1;
                    int latest2=-1;
                    for(int n=0;n<vesselRoutes.get(routeSmall2).size();n++){
                        if(vesselRoutes.get(routeSmall2).get(n).getID()==small2){
                            index2=n;
                            earliest2=vesselRoutes.get(routeSmall2).get(n).getEarliestTime();
                            latest2=vesselRoutes.get(routeSmall2).get(n).getLatestTime();
                        }
                    }
                    int benefitIncreaseSmall2=calculateProfitIncrease(routeSmall2,index2);
                    small2InsertionValues=new InsertionValues(benefitIncreaseSmall2,index2,routeSmall2,earliest2,latest2);
                    removeSynchronizedOp(simultaneousOp.get(small2),precedenceOverOperations.get(small2),precedenceOfOperations.get(small2),
                            small2,vesselRoutes.get(routeSmall2).get(index2));
                    updateAllTimesAfterRemoval();
                    LargeNeighboorhoodSearchInsert.findInsertionCosts(new OperationInRoute(bigTask,0,0),-1,-1,
                            -1,-1,-1,-1,-1
                            ,nTimePeriods,nVessels,OperationsForVessel,vesselRoutes,
                            SailingTimes,EarliestStartingTimeForVessel,SailingCostForVessel,twIntervals,startNodes,simALNS,
                            operationGain,precedenceALNS,TimeVesselUseOnOperation,allFeasibleInsertions,precedenceOverOperations,precedenceOfOperations,
                            simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,simOpRoutes,unroutedTasks,vesselTypes,false,null,0,weatherPenaltyOperations);
                    bigTaskInsertionValues=allFeasibleInsertions.get(bigTask).get(0);

                }
                int benefitIncreaseSmall=small1InsertionValues.getBenenefitIncrease()+small2InsertionValues.getBenenefitIncrease();
                int benefitIncreaseBigTask=bigTaskInsertionValues.getBenenefitIncrease();
                if(benefitIncreaseBigTask>=benefitIncreaseSmall && benefitIncreaseBigTask>0){
                    if(alreadyInserted.equals("small")){
                        System.out.println("swap consolidated performed");
                        countSwap+=1;
                    }
                    LargeNeighboorhoodSearchInsert.insertOperation(bigTask,bigTaskInsertionValues.getEarliest(),bigTaskInsertionValues.getLatest(),bigTaskInsertionValues.getIndexInRoute(),
                            bigTaskInsertionValues.getRouteIndex(),precedenceALNS,startNodes,precedenceOverOperations,precedenceOverRoutes,precedenceOfOperations,precedenceOfRoutes,
                            simALNS,simultaneousOp,simOpRoutes, vesselRoutes, TimeVesselUseOnOperation,SailingTimes,twIntervals);
                    unroutedTasks.removeIf(unrouted -> unrouted.getID() == bigTask || unrouted.getID() == small1 || unrouted.getID() == small2);
                    consolidatedOperations.put(bigTask,new ConsolidatedValues(
                            true,false,0,0,
                            bigTaskInsertionValues.getRouteIndex()));
                }
                else if(benefitIncreaseBigTask<benefitIncreaseSmall && benefitIncreaseSmall>0){
                    if(alreadyInserted.equals("big")){
                        System.out.println("swap consolidated performed");
                        countSwap+=1;
                    }
                    LargeNeighboorhoodSearchInsert.insertOperation(small1,small1InsertionValues.getEarliest(),small1InsertionValues.getLatest(),small1InsertionValues.getIndexInRoute(),
                            small1InsertionValues.getRouteIndex(),precedenceALNS,startNodes,precedenceOverOperations,precedenceOverRoutes,precedenceOfOperations,precedenceOfRoutes,
                            simALNS,simultaneousOp,simOpRoutes, vesselRoutes, TimeVesselUseOnOperation,SailingTimes,twIntervals);
                    LargeNeighboorhoodSearchInsert.insertOperation(small2,small2InsertionValues.getEarliest(),small2InsertionValues.getLatest(),small2InsertionValues.getIndexInRoute(),
                            small2InsertionValues.getRouteIndex(),precedenceALNS,startNodes,precedenceOverOperations,precedenceOverRoutes,precedenceOfOperations,precedenceOfRoutes,
                            simALNS,simultaneousOp,simOpRoutes, vesselRoutes, TimeVesselUseOnOperation,SailingTimes,twIntervals);
                    unroutedTasks.removeIf(unrouted -> unrouted.getID() == bigTask || unrouted.getID() == small1 || unrouted.getID() == small2);
                    consolidatedOperations.put(bigTask,new ConsolidatedValues(
                            false,true,small1InsertionValues.getRouteIndex(),small2InsertionValues.getRouteIndex(),
                            0));
                }
            }
        }

        ObjectiveValues ov= ConstructionHeuristic.calculateObjective(vesselRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,SailingCostForVessel,
                EarliestStartingTimeForVessel, operationGainGurobi, new int[vesselRoutes.size()],new int[vesselRoutes.size()],0, simALNS,bigTasksALNS);
        this.objValue=ov.getObjvalue();
        this.routeSailingCost=ov.getRouteSailingCost();
        this.routeOperationGain=ov.getRouteBenefitGain();
        //System.out.println("Finished switch method");
    }

    public List<OperationInRoute> getUnroutedTasks() {
        return unroutedTasks;
    }

    public List<List<OperationInRoute>> getVesselRoutes() {
        return vesselRoutes;
    }

    public int[] getRouteSailingCost() {
        return routeSailingCost;
    }

    public int[] getRouteOperationGain() {
        return routeOperationGain;
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

    public int getCountSwap() {
        return countSwap;
    }

    public void setCountSwap(int countSwap) {
        this.countSwap = countSwap;
    }
}
