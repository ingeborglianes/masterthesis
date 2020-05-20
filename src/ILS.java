import java.io.FileNotFoundException;
import java.sql.Array;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Random;

public class ILS {
    private ConstructionHeuristic ch;
    private DataGenerator dg;
    private int[] vessels=new int[]{1,2,4,5,5,6,2};
    private int[] locStart = new int[]{1,2,3,4,5,6,7};
    private int numberOfRemoval;
    private int randomSeed;
    private double[] insertionWeights = new double[]{1,1};
    private double[] removalWeights = new double[]{1,1,1,1,1,1};
    private double[] LSOweights = new double[]{1,1,1,1,1,1,1,1,1};
    private int[] LSOscores = new int[]{0,0,0,0,0,0,0,0,0};
    private int[] LSOvisits = new int[]{1,1,1,1,1,1,1,1,1};
    private int[] insertionScore = new int[]{0,0};
    private int[] removalScore = new int[]{0,0,0,0,0,0};
    private int[] insertionVisitsLastSegment = new int[]{0,0};
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
    private int segmentIterationLNS=0;
    private int segmentIterationLocal=0;
    private int numberOfSegmentIterations;
    private int numberOfIterations;
    private double TParameter=-1;
    private double T_decrease_parameter;
    private double controlParameter;
    private int reward1;
    private int reward2;
    private int reward3;
    private int iterationsWithoutImprovementParameter;
    private int iterationsWithoutLocalImprovement=0;
    private int localOptimumIterations;
    public double lowerThresholdWeights;

    public ILS(){
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
        lowerThresholdWeights=ParameterFile.lowerThresholdWeights;
        reward1=ParameterFile.reward1;
        reward2=ParameterFile.reward2;
        reward3=ParameterFile.reward3;
        numberOfSegmentIterations=ParameterFile.numberOfSegmentIterations;
        T_decrease_parameter=Math.pow(0.2,1.0/numberOfIterations);
        localOptimumIterations=ParameterFile.localOptimumIterations;

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
                    int matrixIndex=ID-1-dg.getStartNodes().length;
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
        int[][] simils = dg.getSimultaneousALNS();
        int[][] precils = dg.getPrecedenceALNS();
        int[][] bigTaskils = dg.getBigTasksALNS();
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
                    if(simils[matrixIndex][0]!=0){
                        int connectedID = simils[matrixIndex][0];
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
                    if(simils[matrixIndex][1]!=0){
                        int connectedID = simils[matrixIndex][1];
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
                    if(precils[matrixIndex][0]!=0){
                        int connectedID = precils[matrixIndex][0];
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
                    if(precils[matrixIndex][1]!=0){
                        int connectedID = precils[matrixIndex][1];
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
                    if (bigTaskils[matrixIndex] != null) {
                        if (bigTaskils[matrixIndex][0] == ID) {
                            consolidatedOperations.get(ID).setConsolidatedRoute(vessel);
                            consolidatedOperations.get(ID).setConnectedRoute1(0);
                            consolidatedOperations.get(ID).setConnectedRoute2(0);
                        } else if (bigTaskils[matrixIndex][1] == ID) {
                            consolidatedOperations.get(bigTaskils[matrixIndex][0]).setConnectedRoute1(vessel);
                            consolidatedOperations.get(bigTaskils[matrixIndex][0]).setConsolidatedRoute(0);
                        } else if (bigTaskils[matrixIndex][2] == ID) {
                            consolidatedOperations.get(bigTaskils[matrixIndex][0]).setConnectedRoute2(vessel);
                            consolidatedOperations.get(bigTaskils[matrixIndex][0]).setConsolidatedRoute(0);
                        }
                    }
                }
            }
            old_vesselroutes.add(route);
        }
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfOperationsCopy.entrySet()){
            int taskID=entry.getKey();
            PrecedenceValues pvOf=entry.getValue();
            int connectedPOverTaskID=precils[taskID-1-dg.getStartNodes().length][1];
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
            //System.out.println("new best");
            copyRoutes= copyVesselRoutesAndSynchronization(bestRoutes);
            unroutedTasks = copyUnrouted(bestUnrouted);
        }
        /*
        System.out.println("Test best routes");
        for (int i=0;i<bestRoutes.size();i++){
            System.out.println("VESSELINDEX "+i);
            if (bestRoutes.get(i)!=null) {
                for (int o=0;o<bestRoutes.get(i).size();o++) {
                    System.out.println("Operation number: "+bestRoutes.get(i).get(o).getID() + " Earliest start time: "+
                            bestRoutes.get(i).get(o).getEarliestTime()+ " Latest Start time: "+ bestRoutes.get(i).get(o).getLatestTime());
                }
            }
        }

         */
        vesselroutes = copyRoutes.getVesselRoutes();
        simultaneousOp=copyRoutes.getSimultaneousOp();
        precedenceOfOperations=copyRoutes.getPrecedenceOfOperations();
        precedenceOverOperations=copyRoutes.getPrecedenceOverOperations();
        simOpRoutes=copyRoutes.getSimOpRoutes();
        precedenceOfRoutes=copyRoutes.getPrecedenceOfRoutes();
        precedenceOverRoutes=copyRoutes.getPrecedenceOverRoutes();
    }

    public String chooseLSO() {
        double totalWeight = 0.0d;
        for (double i : LSOweights) {
            totalWeight += i;
        }
        int randomIndex = -1;
        double random = generator.nextDouble() * totalWeight;
        for (int i = 0; i < LSOweights.length; ++i) {
            random -= LSOweights[i];
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        List<String> LSOs = new ArrayList<>(Arrays.asList("1RL", "2RL", "1EX", "2EX","insertNormal",
                "switch_consolidated","relocate","precedence","simultaneous"));
        return LSOs.get(randomIndex);
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
        List<String> insertionMethods = new ArrayList<>(Arrays.asList("best", "regret"));
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

    public void setScoresAndVisitsLocalSearch(int scoreIncrease, String localMethod){
        switch (localMethod) {
            case "1RL":
                LSOscores[0]+=scoreIncrease;
                LSOvisits[0]+=1;
            case "2RL":
                LSOscores[1]+=scoreIncrease;
                LSOvisits[1]+=1;
            case "1EX":
                LSOscores[2]+=scoreIncrease;
                LSOvisits[2]+=1;
            case "2EX":
                LSOscores[3]+=scoreIncrease;
                LSOvisits[3]+=1;
            case "insertNormal":
                LSOscores[4]+=scoreIncrease;
                LSOvisits[4]+=1;
            case "switch_consolidated":
                LSOscores[5]+=scoreIncrease;
                LSOvisits[5]+=1;
            case "relocate":
                LSOscores[6]+=scoreIncrease;
                LSOvisits[6]+=1;
            case "precedence":
                LSOscores[7]+=scoreIncrease;
                LSOvisits[7]+=1;
            case "simultaneous":
                LSOscores[8]+=scoreIncrease;
                LSOvisits[8]+=1;
        }
    }

    public Boolean evaluateSolutionLocal(int[] routeOperationGain, int[] routeSailingCost, List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks,
                                 String localMethod){
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
            iterationsWithoutLocalImprovement=0;
            setScoresAndVisitsLocalSearch(reward1,localMethod);
            System.out.println("Best updated");
            return true;
        }
        else if(newObj>currentObj && !discoveredSolutions.contains(newObj)){
            System.out.println("New best current solution "+newObj);
            //og ikke discovered før, oppdater current
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks);
            discoveredSolutions.add(newObj);
            iterationsWithoutLocalImprovement=0;
            setScoresAndVisitsLocalSearch(reward2,localMethod);
            return true;
        }
        /*
        else if(!discoveredSolutions.contains(newObj) && acceptedByProb(currentObj,newObj)){
            System.out.println("New solution, worse than current, but selected by probability "+newObj);
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks);
            discoveredSolutions.add(newObj);
            iterationsWithoutLocalImprovement+=1;
            setScoresAndVisitsLocalSearch(reward3,localMethod);
        }
         */
        else{
            System.out.println("Continue with current solution");
            iterationsWithoutLocalImprovement+=1;
            retainCurrentBestSolution("current");
            return false;
            /*
            printLNSInsertSolution(vessels,currentRouteSailingCost,currentRouteOperationGain,vesselroutes,locStart,dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),unroutedTasks,precedenceOverOperations,
                    consolidatedOperations,precedenceOfOperations,simultaneousOp,simOpRoutes);

             */
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
        System.out.println("Evaluation after perturbation");
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
            //setScoresAndVisits(reward1,insertMethod,removalMethod);
        }
        else{
            System.out.println("New best current solution "+newObj);
            //og ikke discovered før, oppdater current
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks);
            discoveredSolutions.add(newObj);
            //setScoresAndVisits(reward2,insertMethod,removalMethod);
        }
        /*
        else if(!discoveredSolutions.contains(newObj)){
            System.out.println("New solution, worse than current, but still selected "+newObj);
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks);
            discoveredSolutions.add(newObj);
            setScoresAndVisits(reward3,insertMethod,removalMethod);
        }

         */
        System.out.println("Best solution in evaluate method");
        printLNSInsertSolution(vessels,bestRouteSailingCost,bestRouteOperationGain,bestRoutes,locStart,dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),bestUnrouted,precedenceOverOperations,
                            consolidatedOperations,precedenceOfOperations,simultaneousOp,simOpRoutes);

    }


    public void printLNSInsertSolution(int[] vessseltypes, int[]routeSailingCost,int[]routeOperationGain,List<List<OperationInRoute>> vesselRoutes,
                                       int[] startNodes, int[][][][] SailingTimes, int[][][] TimeVesselUseOnOperation, List<OperationInRoute> unroutedTasks,
                                       Map<Integer, PrecedenceValues> precedenceOverOperations, Map<Integer, ConsolidatedValues> consolidatedOperations,
                                       Map<Integer, PrecedenceValues> precedenceOfOperations,Map<Integer, ConnectedValues> simultaneousOp,
                                       List<Map<Integer, ConnectedValues>> simOpRoutes){

        System.out.println("Print Solution ils class");

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

    public void runLocalSearchFullEnumeration(){
        int i=0;
        while(i<localOptimumIterations){
            System.out.println("Iteration local "+i);
            Boolean change=true;
            while(change){
                LS_operators LSO=new LS_operators(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                        dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                        dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(), vesselroutes, unroutedTasks,
                        precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                        simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, dg.getOperationGainGurobi());
                LSO.runNormalLSO("1RL");
                change=evaluateSolutionLocal(LSO.getRouteOperationGain(),LSO.getRouteSailingCost(),LSO.getVesselroutes(),LSO.getUnroutedTasks(), "1RL");
            }
            System.out.println("run 2RL");
            change=true;
            while(change){
                LS_operators LSO=new LS_operators(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                        dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                        dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(), vesselroutes, unroutedTasks,
                        precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                        simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, dg.getOperationGainGurobi());
                LSO.runNormalLSO("2RL");
                change=evaluateSolutionLocal(LSO.getRouteOperationGain(),LSO.getRouteSailingCost(),LSO.getVesselroutes(),LSO.getUnroutedTasks(), "2RL");
            }
            System.out.println("run 1EX");
            change=true;
            while(change){
                LS_operators LSO=new LS_operators(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                        dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                        dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(), vesselroutes, unroutedTasks,
                        precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                        simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, dg.getOperationGainGurobi());
                LSO.runNormalLSO("1EX");
                change=evaluateSolutionLocal(LSO.getRouteOperationGain(),LSO.getRouteSailingCost(),LSO.getVesselroutes(),LSO.getUnroutedTasks(), "1EX");
            }
            System.out.println("run 2EX");
            change=true;
            while(change){
                LS_operators LSO=new LS_operators(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                        dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                        dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(), vesselroutes, unroutedTasks,
                        precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                        simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, dg.getOperationGainGurobi());
                LSO.runNormalLSO("2EX");
                change=evaluateSolutionLocal(LSO.getRouteOperationGain(),LSO.getRouteSailingCost(),LSO.getVesselroutes(),LSO.getUnroutedTasks(), "2EX");
            }
            change=true;
            while(change){
                LS_operators LSO=new LS_operators(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                        dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                        dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(), vesselroutes, unroutedTasks,
                        precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                        simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, dg.getOperationGainGurobi());
                LSO.runNormalLSO("insertNormal");
                change=evaluateSolutionLocal(LSO.getRouteOperationGain(),LSO.getRouteSailingCost(),LSO.getVesselroutes(),LSO.getUnroutedTasks(), "insertNormal");
            }

            System.out.println("run relocate");
            change=true;
            while(change){
                RelocateInsert RI = new RelocateInsert(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                        dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                        dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(),
                        unroutedTasks, vesselroutes, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                        simOpRoutes,precedenceOfRoutes,precedenceOverRoutes,consolidatedOperations,dg.getOperationGainGurobi());
                RI.runRelocateLSO("relocate");
                change=evaluateSolutionLocal(RI.getRouteOperationGain(),RI.getRouteSailingCost(),RI.getVesselRoutes(),RI.getUnroutedTasks(), "relocate");
            }
            System.out.println("run precedence");
            change=true;
            while(change){
                RelocateInsert RI = new RelocateInsert(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                        dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                        dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(),
                        unroutedTasks, vesselroutes, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                        simOpRoutes,precedenceOfRoutes,precedenceOverRoutes,consolidatedOperations,dg.getOperationGainGurobi());
                RI.runRelocateLSO("precedence");
                change=evaluateSolutionLocal(RI.getRouteOperationGain(),RI.getRouteSailingCost(),RI.getVesselRoutes(),RI.getUnroutedTasks(), "precedence");
            }
            System.out.println("run simultaneous");
            change=true;
            while(change){
                RelocateInsert RI = new RelocateInsert(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                        dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                        dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(),
                        unroutedTasks, vesselroutes, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                        simOpRoutes,precedenceOfRoutes,precedenceOverRoutes,consolidatedOperations,dg.getOperationGainGurobi());
                RI.runRelocateLSO("simultaneous");
                change=evaluateSolutionLocal(RI.getRouteOperationGain(),RI.getRouteSailingCost(),RI.getVesselRoutes(),RI.getUnroutedTasks(), "simultaneous");
            }
            SwitchConsolidated sc = new SwitchConsolidated(precedenceOverOperations, precedenceOfOperations,
                    simultaneousOp, simOpRoutes, precedenceOfRoutes, precedenceOverRoutes,
                    consolidatedOperations, unroutedTasks, vesselroutes, dg.getTwIntervals(),
                    dg.getPrecedenceALNS(), dg.getSimultaneousALNS(), dg.getStartNodes(), dg.getSailingTimes(),
                    dg.getTimeVesselUseOnOperation(), dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(),
                    dg.getOperationGain(), dg.getBigTasksALNS(), dg.getOperationsForVessel(),dg.getOperationGainGurobi(),vessels);
            System.out.println("run consolidated");
            sc.runSwitchConsolidated();
            evaluateSolutionLocal(sc.getRouteOperationGain(),sc.getRouteSailingCost(),sc.getVesselRoutes(),sc.getUnroutedTasks(), "consolidated");
            System.out.println("Obj 1 full: "+(IntStream.of(currentRouteOperationGain).sum() - IntStream.of(currentRouteSailingCost).sum()));
            i+=1;
        }
    }


    public void runLocalSearchAdaptive() {
        while (iterationsWithoutLocalImprovement<localOptimumIterations){
            String method = chooseLSO();
            if (method.equals("1RL") || method.equals("2RL") || method.equals("1EX") || method.equals("2EX") || method.equals("insertNormal")) {
                    LS_operators LSO = new LS_operators(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                            dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                            dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(), vesselroutes, unroutedTasks,
                            precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                            simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, dg.getOperationGainGurobi());
                    LSO.runNormalLSO(method);
                    evaluateSolutionLocal(LSO.getRouteOperationGain(),LSO.getRouteSailingCost(),LSO.getVesselroutes(),LSO.getUnroutedTasks(), method);
            }
            else if (method.equals("relocate") || method.equals("precedence") || method.equals("simultaneous")) {
                    RelocateInsert RI = new RelocateInsert(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                            dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                            dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(),
                            unroutedTasks, vesselroutes, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                            simOpRoutes,precedenceOfRoutes,precedenceOverRoutes,consolidatedOperations,dg.getOperationGainGurobi());
                    RI.runRelocateLSO(method);
                    evaluateSolutionLocal(RI.getRouteOperationGain(),RI.getRouteSailingCost(),RI.getVesselRoutes(),RI.getUnroutedTasks(), method);
            }
            else if(method.equals("switch_consolidated")){
                SwitchConsolidated sc = new SwitchConsolidated(precedenceOverOperations, precedenceOfOperations,
                        simultaneousOp, simOpRoutes, precedenceOfRoutes, precedenceOverRoutes,
                        consolidatedOperations, unroutedTasks, vesselroutes, dg.getTwIntervals(),
                        dg.getPrecedenceALNS(), dg.getSimultaneousALNS(), dg.getStartNodes(), dg.getSailingTimes(),
                        dg.getTimeVesselUseOnOperation(), dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(),
                        dg.getOperationGain(), dg.getBigTasksALNS(), dg.getOperationsForVessel(),dg.getOperationGainGurobi(),vessels);
                sc.runSwitchConsolidated();
                evaluateSolutionLocal(sc.getRouteOperationGain(),sc.getRouteSailingCost(),sc.getVesselRoutes(),sc.getUnroutedTasks(), method);
            }
            updateWeightsAndTemperatureAndSegmentIterationsLocal();
        }
    }

    public void runDestroyRepair(){
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
        //PrintData.printSailingTimes(dg.getSailingTimes(),4,dg.getSimultaneousils().length,a.getVesselroutes().size());
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),dg.getStartNodes().length);
        Boolean noise=false;
        LargeNeighboorhoodSearchInsert LNSI = new LargeNeighboorhoodSearchInsert(precedenceOverOperations,precedenceOfOperations,
                simultaneousOp,simOpRoutes,precedenceOfRoutes,precedenceOverRoutes, consolidatedOperations,unroutedTasks,vesselroutes,
                LNSR.getRemovedOperations(), dg.getTwIntervals(), dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),dg.getOperationsForVessel(),dg.getOperationGainGurobi(),dg.getMaxDistance(),vessels,noise);
        //for run insertion, insert method, alternatives: best, regret
        String insertionMethod = chooseInsertionMethod();
        System.out.println("-------Insertion method " + insertionMethod + " ----------");
        LNSI.runLNSInsert(insertionMethod);
        //LNSI.printLNSInsertSolution(vessels);
        evaluateSolution(LNSI.getRouteOperationGain(),LNSI.getRouteSailingCost(),LNSI.getVesselRoutes(),LNSI.getUnroutedTasks(), removalMethod, insertionMethod);
        updateWeightsAndTemperatureAndSegmentIterationsLNS();
        /*
        printLNSInsertSolution(vessels, currentRouteSailingCost, currentRouteOperationGain, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                precedenceOfOperations, simultaneousOp, simOpRoutes);

         */
    }

    public void updateWeightsAndTemperatureAndSegmentIterationsLocal() {
        if (TParameter != -1) {
            TParameter = TParameter * T_decrease_parameter;
        }
        System.out.println("T parameter " + TParameter);
        if (segmentIterationLocal < numberOfSegmentIterations) {
            segmentIterationLocal += 1;
        } else{
            for (int n = 0; n < LSOweights.length; n++) {
                if (LSOvisits[n] != 0) {
                    double newWeight = LSOweights[n] * (1 - controlParameter) + (controlParameter * removalScore[n]) / removalVisitsLastSegment[n];
                    LSOweights[n] = newWeight;
                    if (LSOweights[n] < 1) {
                        LSOweights[n] = 1;
                    }
                }
            }
            System.out.println("LSO weights");
            System.out.println(Arrays.toString(removalWeights));
            Arrays.fill(LSOvisits, 0);
            Arrays.fill(LSOscores, 0);
        }
    }

    public void updateWeightsAndTemperatureAndSegmentIterationsLNS() {
        if (segmentIterationLNS < numberOfSegmentIterations) {
            segmentIterationLNS += 1;
        } else{
            segmentIterationLNS=1;
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

    public void runILS(String localSearchStrategy){
        int improvementsLocal=0;
        int perturbation=0;
        for (int i=0;i<numberOfIterations;i++){
            System.out.println("ITERATION "+i);
            if(localSearchStrategy.equals("full")){
                int objBeforeLocal=IntStream.of(bestRouteOperationGain).sum() - IntStream.of(bestRouteSailingCost).sum();
                runLocalSearchFullEnumeration();
                int objAfterLocal=IntStream.of(bestRouteOperationGain).sum() - IntStream.of(bestRouteSailingCost).sum();
                System.out.println("Objective after 1 iteration with local search "+ objAfterLocal);
                if(objAfterLocal>objBeforeLocal){
                    improvementsLocal+=1;
                }
            }
            else{
                int objBeforePerturbation=IntStream.of(bestRouteOperationGain).sum() - IntStream.of(bestRouteSailingCost).sum();
                runLocalSearchAdaptive();
                int objAfterPerturbation=IntStream.of(bestRouteOperationGain).sum() - IntStream.of(bestRouteSailingCost).sum();
                System.out.println("Objective after 1 iteration with perturbation "+ objAfterPerturbation);
                if(objAfterPerturbation>objBeforePerturbation){
                    perturbation+=1;
                }
            }
            runDestroyRepair();
        }
        System.out.println("Improvements local "+improvementsLocal);
        System.out.println("Improvements perturbation "+perturbation);
    }


    public static void main(String[] args) throws FileNotFoundException {
        long startTime = System.nanoTime();
        ILS ils = new ILS();
        int constructionObjective=IntStream.of(ils.bestRouteOperationGain).sum()-IntStream.of(ils.bestRouteSailingCost).sum();
        List<Integer> unroutedList=new ArrayList<>();
        for (OperationInRoute ur:ils.bestUnrouted){
            unroutedList.add(ur.getID());
        }
        ils.runILS("full");
        ils.retainCurrentBestSolution("best");
        ils.printLNSInsertSolution(ils.vessels, ils.bestRouteSailingCost, ils.bestRouteOperationGain, ils.bestRoutes,
                ils.dg.getStartNodes(), ils.dg.getSailingTimes(), ils.dg.getTimeVesselUseOnOperation(), ils.unroutedTasks,
                ils.precedenceOverOperations, ils.consolidatedOperations,
                ils.precedenceOfOperations, ils.simultaneousOp, ils.simOpRoutes);
        int bestObjective=IntStream.of(ils.bestRouteOperationGain).sum()-IntStream.of(ils.bestRouteSailingCost).sum();
        System.out.println("Construction Objective "+constructionObjective);
        System.out.println("bestObjective "+bestObjective);
        long endTime   = System.nanoTime();
        long totalTime = endTime - startTime;
        System.out.println("Time "+totalTime/1000000000);
        //System.out.println(ils.generator.doubles());

        System.out.println("Unrouted construction");
        for (Integer urInt:unroutedList){
            System.out.println(urInt);
        }

        System.out.println("Unrouted after all search");
        for (OperationInRoute ur:ils.bestUnrouted){
            System.out.println(ur.getID());
        }
    }
}
