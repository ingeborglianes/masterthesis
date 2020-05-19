import java.io.FileNotFoundException;
import java.sql.Array;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Random;

public class ALNS {
    private ConstructionHeuristic ch;
    private DataGenerator dg;
    private int[] vessels=new int[]{1,2,4,5,5,6,2};
    private int[] locStart = new int[]{1,2,3,4,5,6,7};
    private int numberOfRemoval;
    private int randomSeed;
    private double[] insertionWeights = new double[]{1,1,1};
    private double[] removalWeights = new double[]{1,1,1,1,1,1};
    private int[] insertionScore = new int[]{0,0,0};
    private int[] removalScore = new int[]{0,0,0,0,0,0};
    private int[] insertionVisitsLastSegment = new int[]{0,0,0};
    private int[] removalVisitsLastSegment = new int[]{0,0,0,0,0,0};
    private double relatednessWeightDistance;
    private double relatednessWeightDuration;
    private double relatednessWeightTimewindows;
    private double relatednessWeightPrecedenceOver;
    public double relatednessWeightPrecedenceOf;
    public double relatednessWeightSimultaneous;
    private int[] bestRouteSailingCost;
    private int[] bestRouteOperationGain;
    private int[] currentRouteSailingCost;
    private int[] currentRouteOperationGain;
    private List<List<OperationInRoute>> bestRoutes;
    private List<OperationInRoute> bestUnrouted;
    private List<List<OperationInRoute>> currentRoutes;
    private List<OperationInRoute> currentUnrouted;
    private List<List<OperationInRoute>> vesselroutes;
    private List<OperationInRoute> unroutedTasks;
    private Map<Integer, PrecedenceValues> precedenceOverOperations;
    private Map<Integer, PrecedenceValues> precedenceOfOperations;
    private Map<Integer, ConnectedValues> simultaneousOp;
    private Map<Integer, ConsolidatedValues> consolidatedOperations;
    private List<Map<Integer, PrecedenceValues>> precedenceOverRoutes;
    private List<Map<Integer, PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;
    private Random generator;
    private List<Integer> discoveredSolutions= new ArrayList<>();
    private int segmentIteration=0;
    private int numberOfSegmentIterations;
    private int numberOfIterations;
    private double TParameter=-1;
    private double T_decrease_parameter;
    private double controlParameter;
    private int reward1;
    private int reward2;
    private int reward3;
    private int iterationsWithoutImprovementParameter=0;
    private int iterationsWithoutImprovement=0;
    public double lowerThresholdWeights;

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
        numberOfIterations=ParameterFile.numberOfIterations;
        controlParameter=ParameterFile.controlParameter;
        iterationsWithoutImprovementParameter=ParameterFile.iterationsWithoutImprovementParameter;
        lowerThresholdWeights=ParameterFile.lowerThresholdWeights;
        reward1=ParameterFile.reward1;
        reward2=ParameterFile.reward2;
        reward3=ParameterFile.reward3;
        numberOfSegmentIterations=ParameterFile.numberOfSegmentIterations;
        T_decrease_parameter=Math.pow(0.2,1.0/numberOfIterations);

        if (loc == 20) {
            vessels = new int[]{1, 2, 3, 4, 5};
            locStart = new int[]{1, 2, 3, 4, 5};
        } else if (loc == 25) {
            vessels = new int[]{3, 4, 5, 6};
            locStart = new int[]{3, 4, 5, 6};
        }
        else if (loc == 30 || loc == 35) {
            vessels = new int[]{1, 3, 4, 5, 6};
            locStart = new int[]{1,3, 4, 5, 6};
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
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations(),dg.getOperationGainGurobi());
        ch.createSortedOperations();
        ch.constructionHeuristic();
        ch.printInitialSolution(vessels);

        bestRouteSailingCost = ch.getRouteSailingCost();
        bestRouteOperationGain = ch.getRouteOperationGain();

        currentRouteSailingCost = ch.getRouteSailingCost();
        currentRouteOperationGain = ch.getRouteOperationGain();

        //CopyValues cv= copyVesselRoutesAndSynchronization(ch.getVesselroutes());
        bestRoutes=copyVesselRoutes(ch.getVesselroutes());
        bestUnrouted = copyUnrouted(ch.getUnroutedTasks());

        currentRoutes=copyVesselRoutes(ch.getVesselroutes());
        currentUnrouted=copyUnrouted(ch.getUnroutedTasks());

        precedenceOverOperations = ch.getPrecedenceOverOperations();
        precedenceOfOperations = ch.getPrecedenceOfOperations();
        simultaneousOp = ch.getSimultaneousOp();
        consolidatedOperations = ch.getConsolidatedOperations();
        precedenceOverRoutes = ch.getPrecedenceOverRoutes();
        precedenceOfRoutes = ch.getPrecedenceOfRoutes();
        simOpRoutes = ch.getSimOpRoutes();

        vesselroutes = ch.getVesselroutes();
        unroutedTasks = ch.getUnroutedTasks();
        generator=new Random(randomSeed);
        discoveredSolutions.add(IntStream.of(bestRouteOperationGain).sum()-IntStream.of(bestRouteSailingCost).sum());
    }

    public List<List<OperationInRoute>> copyVesselRoutes(List<List<OperationInRoute>> vesselroutes){
        List<List<OperationInRoute>> old_vesselroutes = new ArrayList<>();
        for (int vessel=0; vessel<vesselroutes.size();vessel++) {
            List<OperationInRoute> route = new ArrayList<>();
            if (vesselroutes.get(vessel) != null) {
                for (int s=0;s<vesselroutes.get(vessel).size();s++) {
                    OperationInRoute operationInRoute = vesselroutes.get(vessel).get(s);
                    int ID = operationInRoute.getID();
                    OperationInRoute op = new OperationInRoute(ID, operationInRoute.getEarliestTime(), operationInRoute.getLatestTime());
                    route.add(op);
                }
            }
            old_vesselroutes.add(route);
        }
        return old_vesselroutes;
    }

    public CopyValues copyVesselRoutesAndSynchronization(List<List<OperationInRoute>> vesselroutes) {
        List<List<OperationInRoute>> old_vesselroutes = new ArrayList<>();
        Map<Integer, PrecedenceValues> precedenceOverOperationsCopy=new HashMap<>();
        Map<Integer, PrecedenceValues> precedenceOfOperationsCopy=new HashMap<>();
        Map<Integer, ConnectedValues> simultaneousOpCopy=new HashMap<>();
        List<Map<Integer, PrecedenceValues>> precedenceOverRoutesCopy=new ArrayList<>();
        List<Map<Integer, PrecedenceValues>> precedenceOfRoutesCopy=new ArrayList<>();
        List<Map<Integer, ConnectedValues>> simOpRoutesCopy=new ArrayList<>();
        int[][] simALNS = dg.getSimultaneousALNS();
        int[][] precALNS = dg.getPrecedenceALNS();
        int[][] bigTaskALNS = dg.getBigTasksALNS();
        for(int i=0;i<vesselroutes.size();i++){
            precedenceOverRoutesCopy.add(new HashMap<>());
            precedenceOfRoutesCopy.add(new HashMap<>());
            simOpRoutesCopy.add(new HashMap<>());
        }
        for (int vessel=0; vessel<vesselroutes.size();vessel++) {
            List<OperationInRoute> route = new ArrayList<>();
            if (vesselroutes.get(vessel) != null) {
                for (int s=0;s<vesselroutes.get(vessel).size();s++) {
                    OperationInRoute operationInRoute = vesselroutes.get(vessel).get(s);
                    int ID = operationInRoute.getID();
                    int matrixIndex=ID-1-dg.getStartNodes().length;
                    OperationInRoute op = new OperationInRoute(ID, operationInRoute.getEarliestTime(), operationInRoute.getLatestTime());
                    route.add(op);
                    if(simALNS[matrixIndex][0]!=0){
                        int connectedID = simALNS[matrixIndex][0];
                        OperationInRoute connectedObject;
                        int connectedRoute;
                        if(simultaneousOpCopy.get(connectedID)!=null){
                            connectedObject=simultaneousOpCopy.get(connectedID).getOperationObject();
                            connectedRoute=simultaneousOpCopy.get(connectedID).getRoute();
                            simultaneousOpCopy.get(connectedID).setConnectedOperationObject(op);
                            simultaneousOpCopy.get(connectedID).setConnectedRoute(vessel);
                        }
                        else{
                            connectedObject=null;
                            connectedRoute=0;
                        }
                        ConnectedValues newCV=new ConnectedValues(op,connectedObject,connectedID,s,vessel,connectedRoute);
                        simultaneousOpCopy.put(ID,newCV);
                        simOpRoutesCopy.get(vessel).put(ID,newCV);
                    }
                    if(simALNS[matrixIndex][1]!=0){
                        int connectedID = simALNS[matrixIndex][1];
                        OperationInRoute connectedObject;
                        int connectedRoute;
                        if(simultaneousOpCopy.get(connectedID)!=null){
                            connectedObject=simultaneousOpCopy.get(connectedID).getOperationObject();
                            connectedRoute=simultaneousOpCopy.get(connectedID).getRoute();
                            simultaneousOpCopy.get(connectedID).setConnectedOperationObject(op);
                            simultaneousOpCopy.get(connectedID).setConnectedRoute(vessel);
                        }
                        else{
                            connectedObject=null;
                            connectedRoute=0;
                        }
                        ConnectedValues newCV=new ConnectedValues(op,connectedObject,connectedID,s,vessel,connectedRoute);
                        simultaneousOpCopy.put(ID,newCV);
                        simOpRoutesCopy.get(vessel).put(ID,newCV);
                    }
                    if(precALNS[matrixIndex][0]!=0){
                        int connectedID = precALNS[matrixIndex][0];
                        OperationInRoute connectedObject;
                        int connectedRoute;
                        if(precedenceOfOperationsCopy.get(connectedID)!=null){
                            connectedObject=precedenceOfOperationsCopy.get(connectedID).getOperationObject();
                            connectedRoute=precedenceOfOperationsCopy.get(connectedID).getRoute();
                            precedenceOfOperationsCopy.get(connectedID).setConnectedOperationObject(op);
                            precedenceOfOperationsCopy.get(connectedID).setConnectedRoute(vessel);
                        }
                        else{
                            connectedObject=null;
                            connectedRoute=0;
                        }
                        PrecedenceValues newPV=new PrecedenceValues(op,connectedObject,connectedID,s,vessel,connectedRoute);
                        precedenceOverOperationsCopy.put(ID,newPV);
                        precedenceOverRoutesCopy.get(vessel).put(ID,newPV);
                    }
                    if(precALNS[matrixIndex][1]!=0){
                        int connectedID = precALNS[matrixIndex][1];
                        OperationInRoute connectedObject;
                        int connectedRoute;
                        if(precedenceOverOperationsCopy.get(connectedID)!=null){
                            connectedObject=precedenceOverOperationsCopy.get(connectedID).getOperationObject();
                            connectedRoute=precedenceOverOperationsCopy.get(connectedID).getRoute();
                            precedenceOverOperationsCopy.get(connectedID).setConnectedOperationObject(op);
                            precedenceOverOperationsCopy.get(connectedID).setConnectedRoute(vessel);
                        }
                        else{
                            connectedObject=null;
                            connectedRoute=0;
                        }
                        PrecedenceValues newPV=new PrecedenceValues(op,connectedObject,connectedID,s,vessel,connectedRoute);
                        precedenceOfOperationsCopy.put(ID,newPV);
                        precedenceOfRoutesCopy.get(vessel).put(ID,newPV);
                    }
                    if (bigTaskALNS[matrixIndex] != null) {
                        if (bigTaskALNS[matrixIndex][0] == ID) {
                            consolidatedOperations.get(ID).setConsolidatedRoute(vessel);
                            consolidatedOperations.get(ID).setConnectedRoute1(0);
                            consolidatedOperations.get(ID).setConnectedRoute2(0);
                        } else if (bigTaskALNS[matrixIndex][1] == ID) {
                            consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConnectedRoute1(vessel);
                            consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConsolidatedRoute(0);
                        } else if (bigTaskALNS[matrixIndex][2] == ID) {
                            consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConnectedRoute2(vessel);
                            consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConsolidatedRoute(0);
                        }
                    }
                }
            }
            old_vesselroutes.add(route);
        }
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfOperationsCopy.entrySet()){
            int taskID=entry.getKey();
            PrecedenceValues pvOf=entry.getValue();
            int connectedPOverTaskID=precALNS[taskID-1-dg.getStartNodes().length][1];
            if(pvOf.getConnectedOperationObject()==null){
                PrecedenceValues pvOver = precedenceOverOperationsCopy.get(connectedPOverTaskID);
                pvOf.setConnectedOperationObject(pvOver.getOperationObject());
                pvOf.setConnectedRoute(pvOver.getRoute());
            }
        }
        return new CopyValues(old_vesselroutes,precedenceOverOperationsCopy,precedenceOfOperationsCopy,simultaneousOpCopy,
                precedenceOverRoutesCopy,precedenceOfRoutesCopy,simOpRoutesCopy);
    }

    public List<OperationInRoute> copyUnrouted(List<OperationInRoute> unroutedTasks) {
        List<OperationInRoute> old_unrouted = new ArrayList<OperationInRoute>();
        for (OperationInRoute unroutedTask : unroutedTasks) {
            OperationInRoute op = new OperationInRoute(unroutedTask.getID(), unroutedTask.getEarliestTime(), unroutedTask.getLatestTime());
            old_unrouted.add(op);
        }
        return old_unrouted;
    }

    public void retainCurrentBestSolution(String typeSolution){
        CopyValues copyRoutes;
        if(typeSolution.equals("current")){
            copyRoutes= copyVesselRoutesAndSynchronization(currentRoutes);
            unroutedTasks = copyUnrouted(currentUnrouted);
        }
        else{
            copyRoutes= copyVesselRoutesAndSynchronization(bestRoutes);
            unroutedTasks = copyUnrouted(bestUnrouted);
        }
        vesselroutes = copyRoutes.getVesselRoutes();
        simultaneousOp=copyRoutes.getSimultaneousOp();
        precedenceOfOperations=copyRoutes.getPrecedenceOfOperations();
        precedenceOverOperations=copyRoutes.getPrecedenceOverOperations();
        simOpRoutes=copyRoutes.getSimOpRoutes();
        precedenceOfRoutes=copyRoutes.getPrecedenceOfRoutes();
        precedenceOverRoutes=copyRoutes.getPrecedenceOverRoutes();
    }

    public String chooseRemovalMethod() {
        double totalWeight = 0.0d;
        for (double i : removalWeights) {
            totalWeight += i;
        }
        int randomIndex = -1;
        double random = generator.nextDouble() * totalWeight;
        for (int i = 0; i < removalWeights.length; ++i) {
            random -= removalWeights[i];
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        List<String> removalMethods = new ArrayList<>(Arrays.asList("random", "synchronized", "route",
                "worst", "related","worst_sailing"));
        return removalMethods.get(randomIndex);
    }

    public String chooseInsertionMethod(){
        double totalWeight = 0.0d;
        for (double i : insertionWeights) {
            totalWeight += i;
        }
        int randomIndex = -1;
        double random = generator.nextDouble() * totalWeight;
        for (int i = 0; i < insertionWeights.length; ++i) {
            random -= insertionWeights[i];
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        List<String> insertionMethods = new ArrayList<>(Arrays.asList("best", "regret","regret_3"));
        return insertionMethods.get(randomIndex);
    }

    public void setScoresAndVisits(int scoreIncrease, String insertMethod, String removeMethod){
        if(insertMethod.equals("best")){
            insertionScore[0]+=scoreIncrease;
            insertionVisitsLastSegment[0]+=1;
        }
        if(insertMethod.equals("regret")){
            insertionScore[1]+=scoreIncrease;
            insertionVisitsLastSegment[1]+=1;
        }
        if(insertMethod.equals("regret_3")){
            insertionScore[2]+=scoreIncrease;
            insertionVisitsLastSegment[2]+=1;
        }
        if(removeMethod.equals("random")){
            removalScore[0]+=scoreIncrease;
            removalVisitsLastSegment[0]+=1;
        }
        if(removeMethod.equals("synchronized")){
            removalScore[1]+=scoreIncrease;
            removalVisitsLastSegment[1]+=1;
        }
        if(removeMethod.equals("route")){
            removalScore[2]+=scoreIncrease;
            removalVisitsLastSegment[2]+=1;
        }
        if(removeMethod.equals("worst")){
            removalScore[3]+=scoreIncrease;
            removalVisitsLastSegment[3]+=1;
        }
        if(removeMethod.equals("related")){
            removalScore[4]+=scoreIncrease;
            removalVisitsLastSegment[4]+=1;
        }
        if(removeMethod.equals("worst_sailing")){
            removalScore[5]+=scoreIncrease;
            removalVisitsLastSegment[5]+=1;
        }
    }

    public Boolean acceptedByProb(int currentObj, int newObj){
        if(TParameter==-1){
            TParameter=(-(currentObj-newObj)*Math.log(Math.exp(1)))/Math.log(0.5);
            System.out.println("T start "+TParameter);
            System.out.println("T decrase parameter "+T_decrease_parameter);
        }
        double randomNum=generator.nextDouble();
        double prob=Math.exp(-(currentObj-newObj)/TParameter);
        System.out.println("probability "+prob);
        if(randomNum<=prob){
            return true;
        }
        return false;
    }

    public void evaluateSolution(int[] routeOperationGain, int[] routeSailingCost, List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks,
                                 String removalMethod, String insertMethod){
        int bestObj= IntStream.of(bestRouteOperationGain).sum()-IntStream.of(bestRouteSailingCost).sum();
        int currentObj= IntStream.of(currentRouteOperationGain).sum()-IntStream.of(currentRouteSailingCost).sum();
        int newObj = IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum();
        if(newObj>bestObj){
            System.out.println("New best global solution "+newObj);
            bestRouteSailingCost = routeSailingCost;
            bestRouteOperationGain = routeOperationGain;
            bestRoutes = copyVesselRoutes(vesselroutes);
            bestUnrouted = copyUnrouted(unroutedTasks);
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks);
            discoveredSolutions.add(newObj);
            iterationsWithoutImprovement=0;
            setScoresAndVisits(reward1,insertMethod,removalMethod);
        }
        else if(newObj>currentObj && !discoveredSolutions.contains(newObj)){
            System.out.println("New best current solution "+newObj);
            //og ikke discovered før, oppdater current
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks);
            discoveredSolutions.add(newObj);
            iterationsWithoutImprovement=0;
            setScoresAndVisits(reward2,insertMethod,removalMethod);
        }
        else if( !discoveredSolutions.contains(newObj) && acceptedByProb(currentObj,newObj)){
            System.out.println("New solution, worse than current, but selected by probability "+newObj);
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks);
            discoveredSolutions.add(newObj);
            iterationsWithoutImprovement=0;
            setScoresAndVisits(reward3,insertMethod,removalMethod);
        }
        else if(iterationsWithoutImprovement==iterationsWithoutImprovementParameter){
            System.out.println("New solution because of search will not move on "+newObj);
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks);
            if(!discoveredSolutions.contains(newObj)){
                discoveredSolutions.add(newObj);
            }
            iterationsWithoutImprovement=0;
        }
        else{
            System.out.println("Continue with current solution");
            iterationsWithoutImprovement+=1;
            retainCurrentBestSolution("current");
        }
        /*
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
        */


/*
            printLNSInsertSolution(vessels,bestRouteSailingCost,bestRouteOperationGain,bestRoutes,locStart,dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),bestUnrouted,precedenceOverOperations,
                                consolidatedOperations,precedenceOfOperations,simultaneousOp,simOpRoutes);

 */

    }


    public void printLNSInsertSolution(int[] vessseltypes, int[]routeSailingCost,int[]routeOperationGain,List<List<OperationInRoute>> vesselRoutes,
                                       int[] startNodes, int[][][][] SailingTimes, int[][][] TimeVesselUseOnOperation, List<OperationInRoute> unroutedTasks,
                                       Map<Integer, PrecedenceValues> precedenceOverOperations, Map<Integer, ConsolidatedValues> consolidatedOperations,
                                       Map<Integer, PrecedenceValues> precedenceOfOperations,Map<Integer, ConnectedValues> simultaneousOp,
                                       List<Map<Integer, ConnectedValues>> simOpRoutes){

        System.out.println("Print Solution ALNS class");

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
                    simOp.getRoute() + " with index: " + simOp.getIndex() + " earliest "+simOp.getOperationObject().getEarliestTime()
                    + " earliest "+simOp.getOperationObject().getLatestTime()+ " connected ID "+simOp.getConnectedOperationObject().getID() + " connected route "+simOp.getConnectedRoute());
        }
        /*
        System.out.println("Simultaneous 2");
        for(int v= 0;v<vesselRoutes.size();v++){
            for(Map.Entry<Integer, ConnectedValues> entry : simOpRoutes.get(v).entrySet()){
                ConnectedValues simOp = entry.getValue();
                System.out.println("In vesssel " + v+" Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                        simOp.getRoute() + " with index: " + simOp.getIndex());
            }
        }

         */
        System.out.println("PRECEDENCE OVER DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOverOperations.entrySet()){
            PrecedenceValues presOverOp = entry.getValue();
            if(presOverOp.getConnectedOperationObject()!=null){
                System.out.println("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                        presOverOp.getRoute() + " with index: " + presOverOp.getIndex()+ " connected ID "+presOverOp.getConnectedOperationObject().getID() + " connected route "+presOverOp.getConnectedRoute());
            }
            else{
                System.out.println("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                        presOverOp.getRoute() + " with index: " + presOverOp.getIndex());
            }

        }
        System.out.println("PRECEDENCE OF DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfOperations.entrySet()){
            PrecedenceValues presOfOp = entry.getValue();
            if(presOfOp.getConnectedOperationObject()!=null){
                System.out.println("Precedence over operation: " + presOfOp.getOperationObject().getID() + " in route: " +
                        presOfOp.getRoute() + " with index: " + presOfOp.getIndex()+ " connected ID "+presOfOp.getConnectedOperationObject().getID() + " connected route "+presOfOp.getConnectedRoute());
            }
            else{
                System.out.println("Precedence over operation: " + presOfOp.getOperationObject().getID() + " in route: " +
                        presOfOp.getRoute() + " with index: " + presOfOp.getIndex());
            }
        }
        System.out.println("\nCONSOLIDATED DICTIONARY:");
        if(consolidatedOperations!=null) {
            for (Map.Entry<Integer, ConsolidatedValues> entry : consolidatedOperations.entrySet()) {
                ConsolidatedValues cv = entry.getValue();
                int key = entry.getKey();
                System.out.println("new entry in consolidated dictionary:");
                System.out.println("Key " + key);
                System.out.println("big task placed? " + cv.getConsolidated());
                System.out.println("small tasks placed? " + cv.getSmallTasks());
                System.out.println("small route 1 " + cv.getConnectedRoute1());
                System.out.println("small route 2 " + cv.getConnectedRoute2());
                System.out.println("route consolidated task " + cv.getConsolidatedRoute() + "\n");

            }
        }
        System.out.println("Is solution feasible? "+checkSolution(vesselRoutes));
    }

    public Boolean checkSolution(List<List<OperationInRoute>> vesselroutes){
        for(List<OperationInRoute> route : vesselroutes){
            if(route != null) {
                for (OperationInRoute operation : route) {
                    if (operation.getEarliestTime() > operation.getLatestTime()) {
                        System.out.println("Earliest time is larger than latest time, infeasible move");
                        return false;
                    }
                }
            }
        }
        if(simultaneousOp!=null && !simultaneousOp.isEmpty()){
            for(ConnectedValues op : simultaneousOp.values()){
                OperationInRoute conOp = op.getConnectedOperationObject();
                /*
                System.out.println("Op ID "+op.getOperationObject().getID());
                System.out.println("conOp ID "+conOp.getID());
                System.out.println("Op earliest "+op.getOperationObject().getEarliestTime());
                System.out.println("Op latest "+op.getOperationObject().getLatestTime());
                System.out.println("ConOp earliest "+conOp.getEarliestTime());
                System.out.println("ConOp latest "+conOp.getLatestTime());

                 */
                if(op.getOperationObject().getEarliestTime() != conOp.getEarliestTime() ||
                        op.getOperationObject().getLatestTime() != conOp.getLatestTime()){
                    System.out.println("Earliest and/or latest time for simultaneous op do not match, infeasible move");
                    return false;
                }
            }
        }
        if(precedenceOverRoutes!=null && !precedenceOverOperations.isEmpty()){
            for(PrecedenceValues op : precedenceOverOperations.values()){
                OperationInRoute conop = op.getConnectedOperationObject();
                if(conop != null) {
                    if (conop.getEarliestTime() < op.getOperationObject().getEarliestTime() + dg.getTimeVesselUseOnOperation()[op.getRoute()]
                            [op.getOperationObject().getID() - 1-dg.getStartNodes().length][op.getOperationObject().getEarliestTime()]) {
                        System.out.println("Precedence infeasible, op: "+op.getOperationObject().getID()+ " and "+conop.getID());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void runDestroyRepair(){
        for (int i =0; i<numberOfIterations; i++){
            System.out.println("iteration "+i);
            //System.out.println("Print før kjøring");
            //printLNSInsertSolution(vessels,bestRouteSailingCost,bestRouteOperationGain,bestRoutes,locStart,dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),bestUnrouted,precedenceOverOperations,
            //consolidatedOperations,precedenceOfOperations,simultaneousOp,simOpRoutes);
            LargeNeighboorhoodSearchRemoval LNSR = new LargeNeighboorhoodSearchRemoval(precedenceOverOperations,precedenceOfOperations,
                    simultaneousOp,simOpRoutes,precedenceOfRoutes,precedenceOverRoutes,
                    consolidatedOperations,unroutedTasks,vesselroutes, dg.getTwIntervals(),
                    dg.getPrecedenceALNS(), dg.getSimultaneousALNS(),dg.getStartNodes(),
                    dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                    dg.getOperationGain(),dg.getBigTasksALNS(),numberOfRemoval,randomSeed,dg.getDistOperationsInInstance(),
                    relatednessWeightDistance,relatednessWeightDuration,relatednessWeightTimewindows,relatednessWeightPrecedenceOver,
                    relatednessWeightPrecedenceOf,relatednessWeightSimultaneous,dg.getOperationGainGurobi(),vessels);

            //for run removal, insert method, alternatives: worst, synchronized, route, related, random, worst_sailing
            String removalMethod = chooseRemovalMethod();
            LNSR.runLNSRemoval(removalMethod);
            System.out.println("---------- Removal method " + removalMethod+ " -----------");
            /*
            printLNSInsertSolution(vessels, currentRouteSailingCost, currentRouteOperationGain, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                    dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                    precedenceOfOperations, simultaneousOp, simOpRoutes);

             */
            //printLNSInsertSolution(vessels,bestRouteSailingCost,bestRouteOperationGain,vesselroutes,dg.getStartNodes(),dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),
            //unroutedTasks,precedenceOverOperations,consolidatedOperations,precedenceOfOperations,simultaneousOp,simOpRoutes);
            //PrintData.printSailingTimes(dg.getSailingTimes(),4,dg.getSimultaneousALNS().length,a.getVesselroutes().size());
            //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),dg.getStartNodes().length);
            LargeNeighboorhoodSearchInsert LNSI = new LargeNeighboorhoodSearchInsert(precedenceOverOperations,precedenceOfOperations,
                    simultaneousOp,simOpRoutes,precedenceOfRoutes,precedenceOverRoutes, consolidatedOperations,unroutedTasks,vesselroutes,
                    LNSR.getRemovedOperations(), dg.getTwIntervals(), dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getStartNodes(),
                    dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                    dg.getOperationGain(),dg.getBigTasksALNS(),dg.getOperationsForVessel(),dg.getOperationGainGurobi(),vessels);

            //for run insertion, insert method, alternatives: best, regret
            String insertionMethod = chooseInsertionMethod();
            System.out.println("-------Insertion method " + insertionMethod + " ----------");
            LNSI.runLNSInsert(insertionMethod);
            //LNSI.printLNSInsertSolution(vessels);
            evaluateSolution(LNSI.getRouteOperationGain(),LNSI.getRouteSailingCost(),LNSI.getVesselRoutes(),LNSI.getUnroutedTasks(), removalMethod, insertionMethod);
            //LNSI.printLNSInsertSolution(vessels);
            updateWeightsAndTemperatureAndSegmentIterations();
        }
    }

    public void updateWeightsAndTemperatureAndSegmentIterations(){
        if(TParameter!=-1){
            TParameter=TParameter*T_decrease_parameter;
        }
        System.out.println("T parameter "+TParameter);
        if(segmentIteration<numberOfSegmentIterations){
            segmentIteration+=1;
        }
        else{
            segmentIteration=1;
            for (int n=0;n<insertionWeights.length;n++){
                if(insertionVisitsLastSegment[n]!=0) {
                    double newWeight = insertionWeights[n] * (1 - controlParameter) + (controlParameter * insertionScore[n]) / insertionVisitsLastSegment[n];
                    insertionWeights[n] = newWeight;
                    if(insertionWeights[n] <1){
                        insertionWeights[n] =1;
                    }
                }
            }
            for (int n=0;n<removalWeights.length;n++){
                if(removalVisitsLastSegment[n]!=0) {
                    double newWeight = removalWeights[n] * (1 - controlParameter) + (controlParameter * removalScore[n]) / removalVisitsLastSegment[n];
                    removalWeights[n] = newWeight;
                    if(removalWeights[n] <1){
                        removalWeights[n] =1;
                    }
                }
            }
            System.out.println("Insertion weights");
            System.out.println(Arrays.toString(insertionWeights));
            System.out.println("Removal weights");
            System.out.println(Arrays.toString(removalWeights));
            Arrays.fill(removalVisitsLastSegment, 0);
            Arrays.fill(insertionVisitsLastSegment, 0);
            Arrays.fill(insertionScore, 0);
            Arrays.fill(removalScore, 0);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        long startTime = System.nanoTime();
        ALNS alns = new ALNS();
        int constructionObjective=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();
        List<Integer> unroutedList=new ArrayList<>();
        for (OperationInRoute ur:alns.bestUnrouted){
            unroutedList.add(ur.getID());
        }
        alns.runDestroyRepair();
        alns.retainCurrentBestSolution("best");
        alns.printLNSInsertSolution(alns.vessels, alns.bestRouteSailingCost, alns.bestRouteOperationGain, alns.bestRoutes,
                alns.dg.getStartNodes(), alns.dg.getSailingTimes(), alns.dg.getTimeVesselUseOnOperation(), alns.unroutedTasks,
                alns.precedenceOverOperations, alns.consolidatedOperations,
                alns.precedenceOfOperations, alns.simultaneousOp, alns.simOpRoutes);
        int afterLarge=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();
        System.out.println("Construction Objective "+constructionObjective);
        System.out.println("afterALNS "+afterLarge);
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
    }
}
