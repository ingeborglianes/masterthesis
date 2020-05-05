import java.io.FileNotFoundException;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.IntStream;

public class ALNS {
    private ConstructionHeuristic ch;
    private DataGenerator dg;
    private int[] vessels=new int[]{1,2,4,5,5,6,2,4};
    private int[] locStart = new int[]{1,2,3,4,5,6,7,8};
    private int numberOfRemoval;
    private int randomSeed;
    private int[] insertionWeights = new int[]{1,1};
    private int[] removalWeights = new int[]{1,1,1,1,1,1};
    private int[] LSOweights = new int[]{1,1,1,1};
    private int[] insertionScore = new int[]{0,0};
    private int[] removalScore = new int[]{0,0,0,0,0,0};
    private double relatednessWeightDistance;
    private double relatednessWeightDuration;
    private double relatednessWeightTimewindows;
    private double relatednessWeightPrecedenceOver;
    public double relatednessWeightPrecedenceOf;
    public double relatednessWeightSimultaneous;
    private int[] bestRouteSailingCost;
    private int[] bestRouteOperationGain;
    private List<List<OperationInRoute>> bestRoutes;
    private List<OperationInRoute> bestUnrouted;
    private List<List<OperationInRoute>> vesselroutes;
    private List<OperationInRoute> unroutedTasks;
    private Map<Integer, PrecedenceValues> precedenceOverOperations;
    private Map<Integer, PrecedenceValues> precedenceOfOperations;
    private Map<Integer, ConnectedValues> simultaneousOp;
    private Map<Integer, ConsolidatedValues> consolidatedOperations;
    private List<Map<Integer, PrecedenceValues>> precedenceOverRoutes;
    private List<Map<Integer, PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;




    public ALNS(){
        int loc = ParameterFile.loc;
        String nameResultFile =ParameterFile.nameResultFile;
        String testInstance=ParameterFile.testInstance;
        int days = ParameterFile.days;
        String weatherFile = ParameterFile.weatherFile;
        numberOfRemoval=ParameterFile.numberOfRemoval;
        randomSeed=ParameterFile.randomSeed;
        relatednessWeightDistance=ParameterFile.relatednessWeightDistance;
        relatednessWeightDuration=ParameterFile.relatednessWeightDuration;
        relatednessWeightTimewindows=ParameterFile.relatednessWeightTimewindows;
        relatednessWeightPrecedenceOver=ParameterFile.relatednessWeightPrecedenceOver;
        relatednessWeightPrecedenceOf=ParameterFile.relatednessWeightPrecedenceOf;
        relatednessWeightSimultaneous=ParameterFile.relatednessWeightSimultaneous;
        if (loc == 20) {
            vessels = new int[]{1, 2, 3, 4, 5};
            locStart = new int[]{1, 2, 3, 4, 5};
        } else if (loc == 25) {
            vessels = new int[]{1, 2, 3, 4, 5, 6};
            locStart = new int[]{1, 2, 3, 4, 5, 6};
        }
        else if (loc == 30) {
            vessels = new int[]{1, 2, 3, 4, 5, 6,2};
            locStart = new int[]{1, 2, 3, 4, 5, 6,7};
        }
        else if (loc == 5) {
            vessels = new int[]{2,3,5};
            locStart = new int[]{1, 2, 3};
        }
        else if (loc == 10) {
            vessels = new int[]{2, 3, 5};
            locStart = new int[]{1, 2, 3};
        }
        else if (loc == 15) {
            vessels = new int[]{1 , 2 , 4,5};
            locStart = new int[]{1, 2, 3,4};
        }
        dg= new DataGenerator(vessels, days, locStart, testInstance, nameResultFile, weatherFile);
        try {
            dg.generateData();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ch = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(),dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations());
        ch.createSortedOperations();
        ch.constructionHeuristic();
        ch.printInitialSolution(vessels);

        bestRouteSailingCost = ch.getRouteSailingCost();
        bestRouteOperationGain = ch.getRouteOperationGain();
        bestRoutes = copyVesselroutes(ch.getVesselroutes());
        bestUnrouted = copyUnrouted(ch.getUnroutedTasks());
        precedenceOverOperations = ch.getPrecedenceOverOperations();
        precedenceOfOperations = ch.getPrecedenceOfOperations();
        simultaneousOp = ch.getSimultaneousOp();
        consolidatedOperations = ch.getConsolidatedOperations();
        precedenceOverRoutes = ch.getPrecedenceOverRoutes();
        precedenceOfRoutes = ch.getPrecedenceOfRoutes();
        simOpRoutes = ch.getSimOpRoutes();
        vesselroutes = ch.getVesselroutes();
        unroutedTasks = ch.getUnroutedTasks();


    }

    public List<List<OperationInRoute>> copyVesselroutes(List<List<OperationInRoute>> vesselroutes) {
        List<List<OperationInRoute>> old_vesselroutes = new ArrayList<List<OperationInRoute>>();
        for (List<OperationInRoute> vesselroute : vesselroutes) {
            List<OperationInRoute> route = new ArrayList<>();
            if (vesselroute != null) {
                for (OperationInRoute operationInRoute : vesselroute) {
                    OperationInRoute op = new OperationInRoute(operationInRoute.getID(), operationInRoute.getEarliestTime(), operationInRoute.getLatestTime());
                    route.add(op);
                }
            }
            old_vesselroutes.add(route);
        }
        return old_vesselroutes;
    }

    public List<OperationInRoute> copyUnrouted(List<OperationInRoute> unroutedTasks) {
        List<OperationInRoute> old_unrouted = new ArrayList<OperationInRoute>();
        for (OperationInRoute unroutedTask : unroutedTasks) {
            OperationInRoute op = new OperationInRoute(unroutedTask.getID(), unroutedTask.getEarliestTime(), unroutedTask.getLatestTime());
            old_unrouted.add(op);
        }
        return old_unrouted;
    }

    public void retainOldSolution(){
        vesselroutes = copyVesselroutes(bestRoutes);
        unroutedTasks = copyUnrouted(bestUnrouted);

        int[][] simALNS = dg.getSimultaneousALNS();
        int[][] precALNS = dg.getPrecedenceALNS();
        int[][] bigTaskALNS = dg.getBigTasksALNS();
        int nStartnodes = dg.getStartNodes().length;
        for(int vessel = 0; vessel < vesselroutes.size(); vessel++){
            for(int task = 0; task < vesselroutes.get(vessel).size(); task++) {
                int taskID = vesselroutes.get(vessel).get(task).getID();
                if (simALNS[taskID - nStartnodes - 1][0] != 0 || simALNS[taskID - nStartnodes - 1][1] != 0) {
                    if (simultaneousOp.get(taskID) != null) {
                        simOpRoutes.get(simultaneousOp.get(taskID).getRoute()).remove(taskID);
                        simultaneousOp.get(taskID).setIndex(task);
                        simultaneousOp.get(taskID).setRoute(vessel);
                        simultaneousOp.get(taskID).getOperationObject().setEarliestTime(vesselroutes.get(vessel).get(task).getEarliestTime());
                        simultaneousOp.get(taskID).getOperationObject().setLatestTime(vesselroutes.get(vessel).get(task).getLatestTime());
                        simOpRoutes.get(simultaneousOp.get(taskID).getRoute()).put(taskID,simultaneousOp.get(taskID));
                    } else {
                        int simOp = Math.max(simALNS[taskID - 1 - nStartnodes][0], simALNS[taskID - 1 - nStartnodes][1]);
                        if (simultaneousOp.get(simOp) == null) {
                            ConnectedValues sValue = new ConnectedValues(vesselroutes.get(vessel).get(task), null, simOp, task, vessel, -1);
                            simultaneousOp.put(taskID, sValue);
                            simOpRoutes.get(vessel).put(taskID, sValue);
                        } else {
                            ConnectedValues sValues = simultaneousOp.get(simOp);
                            if (sValues.getConnectedOperationObject() == null) {
                                ConnectedValues cValuesReplace = new ConnectedValues(sValues.getOperationObject(), vesselroutes.get(vessel).get(task), sValues.getConnectedOperationID(),
                                        sValues.getIndex(), sValues.getRoute(), vessel);
                                simultaneousOp.replace(simOp, sValues, cValuesReplace);
                                simOpRoutes.get(sValues.getRoute()).replace(simOp, sValues, cValuesReplace);
                            } else {
                                ConnectedValues cValuesPut1 = new ConnectedValues(sValues.getOperationObject(), vesselroutes.get(vessel).get(task), sValues.getConnectedOperationID(),
                                        sValues.getIndex(), sValues.getRoute(), vessel);
                                simultaneousOp.put(simOp, cValuesPut1);
                                simOpRoutes.get(sValues.getRoute()).put(simOp, cValuesPut1);
                            }
                            ConnectedValues sim2 = new ConnectedValues(vesselroutes.get(vessel).get(task), sValues.getOperationObject(), simOp, task, vessel, sValues.getRoute());
                            simultaneousOp.put(taskID, sim2);
                            simOpRoutes.get(vessel).put(taskID, sim2);
                        }
                    }
                }
                if (precALNS[taskID - nStartnodes - 1][0] != 0) {
                    if (precedenceOverOperations.get(taskID) != null) {
                        precedenceOverRoutes.get(precedenceOverOperations.get(taskID).getRoute()).remove(taskID);
                        precedenceOverOperations.get(taskID).setIndex(task);
                        precedenceOverOperations.get(taskID).setRoute(vessel);
                        precedenceOverOperations.get(taskID).getOperationObject().setEarliestTime(vesselroutes.get(vessel).get(task).getEarliestTime());
                        precedenceOverOperations.get(taskID).getOperationObject().setLatestTime(vesselroutes.get(vessel).get(task).getLatestTime());
                        precedenceOverRoutes.get(precedenceOverOperations.get(taskID).getRoute()).put(taskID,precedenceOverOperations.get(taskID));
                    } else {
                        int precOver = precALNS[taskID - nStartnodes - 1][0];
                        if(precedenceOfOperations.get(precOver)==null) {
                            PrecedenceValues pValues = new PrecedenceValues(vesselroutes.get(vessel).get(task), null, precOver, task, vessel, -1);
                            precedenceOverOperations.put(taskID, pValues);
                            precedenceOverRoutes.get(vessel).put(taskID,pValues);
                        }
                        if(precedenceOfOperations.get(precOver) != null) {
                            PrecedenceValues pValues = precedenceOfOperations.get(precOver);
                            PrecedenceValues pValuesReplace = new PrecedenceValues(pValues.getOperationObject(),
                                    vesselroutes.get(vessel).get(task), pValues.getConnectedOperationID(), pValues.getIndex(), pValues.getRoute(), vessel);
                            PrecedenceValues pValuesPut = new PrecedenceValues(vesselroutes.get(vessel).get(task), pValues.getOperationObject(), precOver, task, vessel, pValues.getRoute());
                            precedenceOfOperations.put(precOver, pValuesReplace);
                            precedenceOverOperations.put(taskID, pValuesPut);
                            precedenceOfRoutes.get(pValues.getRoute()).put(precOver, pValuesReplace);
                            precedenceOverRoutes.get(vessel).put(taskID, pValuesPut);

                        }
                    }
                }
                if (precALNS[taskID - nStartnodes - 1][1] != 0) {
                    if (precedenceOfOperations.get(taskID) != null) {
                        precedenceOfRoutes.get(precedenceOfOperations.get(taskID).getRoute()).remove(taskID);
                        precedenceOfOperations.get(taskID).setIndex(task);
                        precedenceOfOperations.get(taskID).setRoute(vessel);
                        precedenceOfOperations.get(taskID).getOperationObject().setEarliestTime(vesselroutes.get(vessel).get(task).getEarliestTime());
                        precedenceOfOperations.get(taskID).getOperationObject().setLatestTime(vesselroutes.get(vessel).get(task).getLatestTime());
                        precedenceOfRoutes.get(precedenceOfOperations.get(taskID).getRoute()).put(taskID,precedenceOfOperations.get(taskID));
                    } else {
                        int precOf = precALNS[taskID - nStartnodes - 1][1];
                        if(precedenceOverOperations.get(precOf) != null) {
                            PrecedenceValues pValues = precedenceOverOperations.get(precOf);
                            PrecedenceValues pValuesReplace = new PrecedenceValues(pValues.getOperationObject(),
                                    vesselroutes.get(vessel).get(task), pValues.getConnectedOperationID(), pValues.getIndex(), pValues.getRoute(), vessel);
                            PrecedenceValues pValuesPut = new PrecedenceValues(vesselroutes.get(vessel).get(task), pValues.getOperationObject(), precOf, task, vessel, pValues.getRoute());
                            precedenceOverOperations.put(precOf, pValuesReplace);
                            precedenceOfOperations.put(taskID, pValuesPut);
                            precedenceOverRoutes.get(pValues.getRoute()).put(precOf, pValuesReplace);
                            precedenceOfRoutes.get(vessel).put(taskID, pValuesPut);
                        }else if(precedenceOverOperations.get(precOf)==null){
                            PrecedenceValues pValues = new PrecedenceValues(vesselroutes.get(vessel).get(task), null, precOf, task, vessel, -1);
                            precedenceOfOperations.put(taskID, pValues);
                            precedenceOfRoutes.get(vessel).put(taskID, pValues);
                        }
                    }
                }
                if (bigTaskALNS[taskID-nStartnodes-1] != null) {
                    if (bigTaskALNS[taskID - nStartnodes - 1][0] == taskID) {
                        consolidatedOperations.get(taskID).setConsolidatedRoute(vessel);
                        consolidatedOperations.get(taskID).setConnectedRoute1(0);
                        consolidatedOperations.get(taskID).setConnectedRoute2(0);
                    } else if (bigTaskALNS[taskID - nStartnodes - 1][1] == taskID) {
                        consolidatedOperations.get(taskID).setConnectedRoute1(vessel);
                        consolidatedOperations.get(taskID).setConsolidatedRoute(0);
                    } else if (bigTaskALNS[taskID - nStartnodes - 1][2] == taskID) {
                        consolidatedOperations.get(taskID).setConnectedRoute2(vessel);
                        consolidatedOperations.get(taskID).setConsolidatedRoute(0);
                    }
                }
            }
        }
        for(OperationInRoute op : unroutedTasks){
        if(simultaneousOp.get(op.getID())!=null){
            simultaneousOp.remove(op.getID());
        }
        if(precedenceOfOperations.get(op.getID()) != null){
            precedenceOfOperations.remove(op.getID());
        }
        if(precedenceOverOperations.get(op.getID()) != null){
            precedenceOfOperations.remove(op.getID());
        }
    }
}


    public String chooseLSO() {
        double totalWeight = 0.0d;
        for (int i : LSOweights) {
            totalWeight += i;
        }
        int randomIndex = -1;
        double random = Math.random() * totalWeight;
        for (int i = 0; i < LSOweights.length; ++i) {
            random -= LSOweights[i];
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        List<String> LSOs = new ArrayList<>(Arrays.asList("1RL", "2RL", "1EX", "2EX"));
        return LSOs.get(randomIndex);
    }

    public String chooseRemovalMethod() {
        double totalWeight = 0.0d;
        for (int i : removalWeights) {
            totalWeight += i;
        }
        int randomIndex = -1;
        double random = Math.random() * totalWeight;
        for (int i = 0; i < removalWeights.length; ++i) {
            random -= removalWeights[i];
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        List<String> removalMethods = new ArrayList<>(Arrays.asList("random", "synchronized", "route", "worst", "related","worst_sailing"));
        return removalMethods.get(randomIndex);
    }

    public String chooseInsertionMethod(){
        double totalWeight = 0.0d;
        for (int i : insertionWeights) {
            totalWeight += i;
        }
        int randomIndex = -1;
        double random = Math.random() * totalWeight;
        for (int i = 0; i < insertionWeights.length; ++i) {
            random -= insertionWeights[i];
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        List<String> insertionMethods = new ArrayList<>(Arrays.asList("best", "regret"));
        return insertionMethods.get(randomIndex);
    }

    public Boolean acceptSolution(int[] routeOperationGain, int[] routeSailingCost, List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks){
        int bestObj= IntStream.of(bestRouteOperationGain).sum()-IntStream.of(bestRouteSailingCost).sum();
        int newObj = IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum();
        if(newObj > bestObj ){
            bestRouteSailingCost = routeSailingCost;
            bestRouteOperationGain = routeOperationGain;
            bestRoutes = copyVesselroutes(vesselroutes);
            bestUnrouted = copyUnrouted(unroutedTasks);
            return true;
        }
        else{
            System.out.println("Solution not accepted");
            for (int i=0;i<bestRoutes.size();i++){
                int totalTime=0;
                System.out.println("VESSELINDEX "+i+" VESSELTYPE "+vessels[i]);
                if (bestRoutes.get(i)!=null) {
                    for (int o=0;o<bestRoutes.get(i).size();o++) {
                        System.out.println("Operation number: "+bestRoutes.get(i).get(o).getID() + " Earliest start time: "+
                                bestRoutes.get(i).get(o).getEarliestTime()+ " Latest Start time: "+ bestRoutes.get(i).get(o).getLatestTime());
                        if (o==0){
                            totalTime+=dg.getSailingTimes()[i][0][i][bestRoutes.get(i).get(o).getID()-1];
                            totalTime+=dg.getTimeVesselUseOnOperation()[i][bestRoutes.get(i).get(o).getID()-dg.getStartNodes().length-1][0];
                            //System.out.println("temp total time: "+totalTime);
                        }
                        else{
                            totalTime+=dg.getSailingTimes()[i][0][bestRoutes.get(i).get(o-1).getID()-1][bestRoutes.get(i).get(o).getID()-1];
                            if(o!=bestRoutes.get(i).size()-1) {
                                totalTime += dg.getTimeVesselUseOnOperation()[i][bestRoutes.get(i).get(o).getID() - dg.getStartNodes().length - 1][0];
                            }
                            //System.out.println("temp total time: "+totalTime);
                        }
                    }
                }
                System.out.println("TOTAL DURATION FOR ROUTE: "+totalTime);
            }

            retainOldSolution();

            printLNSInsertSolution(vessels,bestRouteSailingCost,bestRouteOperationGain,bestRoutes,locStart,dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),bestUnrouted,precedenceOverOperations,
                                consolidatedOperations,precedenceOfOperations,simultaneousOp,simOpRoutes);
            return false;
        }
    }


    public void printLNSInsertSolution(int[] vessseltypes, int[]routeSailingCost,int[]routeOperationGain,List<List<OperationInRoute>> vesselRoutes,
                                       int[] startNodes, int[][][][] SailingTimes, int[][][] TimeVesselUseOnOperation, List<OperationInRoute> unroutedTasks,
                                       Map<Integer, PrecedenceValues> precedenceOverOperations, Map<Integer, ConsolidatedValues> consolidatedOperations,
                                       Map<Integer, PrecedenceValues> precedenceOfOperations,Map<Integer, ConnectedValues> simultaneousOp,
                                       List<Map<Integer, ConnectedValues>> simOpRoutes){

        System.out.println("SOLUTION AFTER RESTORE");

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
        System.out.println("Simultaneous 2");
        for(int v= 0;v<vesselRoutes.size();v++){
            for(Map.Entry<Integer, ConnectedValues> entry : simOpRoutes.get(v).entrySet()){
                ConnectedValues simOp = entry.getValue();
                System.out.println("In vesssel " + v+" Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                        simOp.getRoute() + " with index: " + simOp.getIndex());
            }
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
    }

    public void runLocalSearch(){
        //while improvement, choose an operator
        //if operator = switch, call method runSwitchConsolidated
        //if operator = relocate insert, call method runRelocateInsert
        //else (if operator is something else), call runLocalSearchNormalOperators and the selected operator
        Boolean improvement=true;
        while(improvement){
            runRelocateInsert();
            runLocalSearchNormalOperators();
            runSwitchConsolidated();
            /*
            if(solution has not improved){
                improvement=false;
            }
             */
        }
    }

    public void runSwitchConsolidated(){
        SwitchConsolidated sc = new SwitchConsolidated(precedenceOverOperations, precedenceOfOperations,
                simultaneousOp, simOpRoutes, precedenceOfRoutes, precedenceOverRoutes,
                consolidatedOperations, bestUnrouted, bestRoutes, dg.getTwIntervals(),
        dg.getPrecedenceALNS(), dg.getSimultaneousALNS(), dg.getStartNodes(), dg.getSailingTimes(),
        dg.getTimeVesselUseOnOperation(), dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(),
        dg.getOperationGain(), dg.getBigTasksALNS(), dg.getOperationsForVessel());
        sc.runSwitchConsolidated();

    }

    public void runRelocateInsert(){
        RelocateInsert RI = new RelocateInsert(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), bestRouteSailingCost, bestRouteOperationGain,
                dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(),
                bestUnrouted, bestRoutes, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                simOpRoutes,precedenceOfRoutes,precedenceOverRoutes,consolidatedOperations);
        String method = "relocate"; //chooseMethod
        if(method.equals("relocate")) {
            RI.relocateAll();
        }else if(method.equals("precedence")){
            RI.insertPrecedenceOf();
        }else if(method.equals("simultaneous")){
            RI.insertSimultaneous();
        }
    }

    public void runLocalSearchNormalOperators(){
        LS_operators LSO = new LS_operators(dg.getOperationsForVessel(),vessels,dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),
                dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),dg.getTwIntervals(),bestRouteSailingCost,bestRouteOperationGain,
                dg.getStartNodes(), dg.getSimultaneousALNS(),dg.getPrecedenceALNS(),dg.getBigTasksALNS(), dg.getOperationGain(), bestRoutes,bestUnrouted,
                precedenceOverOperations, precedenceOfOperations,simultaneousOp,
                simOpRoutes,precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations);
        String method = "2RL"; //chooseLSO();
        switch (method) {
            case "1RL":
                System.out.println("1-relocate chosen");
                LSO.oneRelocateAll();
                break;
            case "2RL":
                System.out.println("2-relocate chosen");
                LSO.twoRelocateAll();
                break;
            case "1EX":
                System.out.println("1-exchange chosen");
                LSO.oneExchangeAll();
                break;
            case "2EX":
                System.out.println("2-exchange chosen");
                LSO.twoExchangeAll();
                break;
        }
    }

    public void runDestroyRepair(){
        System.out.println(" ");
        System.out.println("Print før kjøring");
        printLNSInsertSolution(vessels,bestRouteSailingCost,bestRouteOperationGain,bestRoutes,locStart,dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),bestUnrouted,precedenceOverOperations,
                consolidatedOperations,precedenceOfOperations,simultaneousOp,simOpRoutes);


        LargeNeighboorhoodSearchRemoval LNSR = new LargeNeighboorhoodSearchRemoval(precedenceOverOperations,precedenceOfOperations,
                simultaneousOp,simOpRoutes,precedenceOfRoutes,precedenceOverRoutes,
                consolidatedOperations,unroutedTasks,vesselroutes, dg.getTwIntervals(),
                dg.getPrecedenceALNS(), dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),numberOfRemoval,randomSeed,dg.getDistOperationsInInstance(),
                relatednessWeightDistance,relatednessWeightDuration,relatednessWeightTimewindows,relatednessWeightPrecedenceOver,
                relatednessWeightPrecedenceOf,relatednessWeightSimultaneous);

        //for run removal, insert method, alternatives: worst, synchronized, route, related, random, worst_sailing
        String removalMethod = chooseRemovalMethod();
        LNSR.runLNSRemoval(removalMethod);
        System.out.println("------Removal method " + removalMethod+ " -----------");
        LNSR.printLNSSolution(vessels);

        //PrintData.printSailingTimes(dg.getSailingTimes(),4,dg.getSimultaneousALNS().length,a.getVesselroutes().size());
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),dg.getStartNodes().length);
        LargeNeighboorhoodSearchInsert LNSI = new LargeNeighboorhoodSearchInsert(precedenceOverOperations,precedenceOfOperations,
                simultaneousOp,simOpRoutes,precedenceOfRoutes,precedenceOverRoutes, consolidatedOperations,unroutedTasks,vesselroutes,
                LNSR.getRemovedOperations(), dg.getTwIntervals(), dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),dg.getOperationsForVessel());

        //for run insertion, insert method, alternatives: best, regret
        String insertionMethod = chooseInsertionMethod();
        System.out.println("-------Insertion method " + insertionMethod + " ----------");
        LNSI.runLNSInsert(insertionMethod);


        if(acceptSolution(LNSI.getRouteOperationGain(),LNSI.getRouteSailingCost(),LNSI.getVesselRoutes(),LNSI.getUnroutedTasks())){
            LNSI.printLNSInsertSolution(vessels);
            System.out.println("New objective value: " + (IntStream.of(bestRouteOperationGain).sum()-IntStream.of(bestRouteSailingCost).sum()));
        }else{
            System.out.println("Same objective value: " + (IntStream.of(bestRouteOperationGain).sum()-IntStream.of(bestRouteSailingCost).sum()));

        }

        //printLNSInsertSolution(vessels,bestRouteSailingCost,bestRouteOperationGain,bestRoutes,dg.getStartNodes(),dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),bestUnrouted,precedenceOverOperations,
        //                       consolidatedOperations,precedenceOfOperations,simultaneousOp);
        //LNSI.switchConsolidated();

    }

    public static void main(String[] args) throws FileNotFoundException {
        long startTime = System.nanoTime();
        ALNS alns = new ALNS();
        int constructionObjective=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();
        List<Integer> unroutedList=new ArrayList<>();
        for (OperationInRoute ur:alns.bestUnrouted){
            unroutedList.add(ur.getID());
        }
        //alns.runRelocateInsert();
        int afterFirstLocalObjective=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();
        for (int i = 0; i < 100; i++) {
            System.out.println("Iteration nr: " + i);
            alns.runDestroyRepair();
        }
        int afterLarge=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();
        alns.runLocalSearchNormalOperators();
        //alns.runRelocateInsert();
        int bestObjective=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();
        System.out.println("Construction Objective "+constructionObjective);
        System.out.println("afterFirstLocalObjective "+afterFirstLocalObjective);
        System.out.println("afterLarge "+afterLarge);
        System.out.println("bestObjective "+bestObjective);
        long endTime   = System.nanoTime();
        long totalTime = endTime - startTime;
        System.out.println("Time "+totalTime/1000000000);
        //System.out.println(alns.generator.doubles());

        System.out.println("Unrouted construction");
        for (Integer urInt:unroutedList){
            System.out.println(urInt);
        }

        System.out.println("Unrouted after all search");
        for (OperationInRoute ur:alns.bestUnrouted){
            System.out.println(ur.getID());
        }
        alns.printLNSInsertSolution(alns.vessels,alns.bestRouteSailingCost,alns.bestRouteOperationGain,alns.bestRoutes,alns.dg.getStartNodes(),alns.dg.getSailingTimes(),
                alns.dg.getTimeVesselUseOnOperation(),alns.bestUnrouted,alns.precedenceOverOperations,alns.consolidatedOperations,
                alns.precedenceOfOperations,alns.simultaneousOp,alns.simOpRoutes);
    }
}
