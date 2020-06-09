import java.io.*;
import java.util.*;
import java.util.stream.IntStream;
import java.util.Random;

public class ALNS_ILS {
    private ConstructionHeuristic ch;
    private DataGenerator dg;
    private int[] vessels=new int[]{};
    private double numberOfRemoval;
    private int randomSeed;
    private double[] noiseWeights = new double[]{1,1};
    private int[] noiseScore = new int[]{0,0};
    private int[] noiseLastSegment = new int[]{0,0};
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
    public double lowerThresholdWeights;
    public int loc;
    public String testInstance;
    private int numberOfImprovementsLocal=0;
    private int iterationsWithoutAcceptanceCount=0;

    public ALNS_ILS(){
        this.loc = Parameterfile_ALNS_ILS.loc;
        this.numberOfRemoval=Parameterfile_ALNS_ILS.numberOfRemoval;
        this.testInstance=Parameterfile_ALNS_ILS.testInstance;
        String nameResultFile =Parameterfile_ALNS_ILS.nameResultFile+testInstance;
        int days = Parameterfile_ALNS_ILS.days;
        String weatherFile = Parameterfile_ALNS_ILS.weatherFile;
        randomSeed=Parameterfile_ALNS_ILS.randomSeed;
        relatednessWeightDistance=Parameterfile_ALNS_ILS.relatednessWeightDistance;
        relatednessWeightDuration=Parameterfile_ALNS_ILS.relatednessWeightDuration;
        relatednessWeightTimewindows=Parameterfile_ALNS_ILS.relatednessWeightTimewindows;
        relatednessWeightPrecedenceOver=Parameterfile_ALNS_ILS.relatednessWeightPrecedenceOver;
        relatednessWeightPrecedenceOf=Parameterfile_ALNS_ILS.relatednessWeightPrecedenceOf;
        relatednessWeightSimultaneous=Parameterfile_ALNS_ILS.relatednessWeightSimultaneous;
        numberOfIterations=Parameterfile_ALNS_ILS.numberOfIterations;
        controlParameter=Parameterfile_ALNS_ILS.controlParameter;
        lowerThresholdWeights=Parameterfile_ALNS_ILS.lowerThresholdWeights;
        reward1=Parameterfile_ALNS_ILS.reward1;
        reward2=Parameterfile_ALNS_ILS.reward2;
        reward3=Parameterfile_ALNS_ILS.reward3;
        numberOfSegmentIterations=Parameterfile_ALNS_ILS.numberOfSegmentIterations;
        T_decrease_parameter=Math.pow(0.2,1.0/numberOfIterations);

        int[] locStart = new int[]{};
        if (loc == 20) {
            vessels = new int[]{3, 4, 5};
            locStart = new int[]{94, 95, 96};
        } else if (loc == 25) {
            vessels = new int[]{3, 4, 5, 6};
            locStart = new int[]{3, 4, 5, 6};
        }
        else if (loc == 30 || loc == 35) {
            vessels = new int[]{1, 3, 4, 5, 6};
            locStart = new int[]{1, 3, 4, 5, 6};
        }
        else if (loc == 40) {
            vessels = new int[]{1,3,4,5,6};
            locStart = new int[]{94,94,96,97,98};
        }
        else if (loc == 10) {
            vessels = new int[]{2, 3, 5};
            locStart = new int[]{1, 2, 3};
        }
        else if (loc == 15) {
            vessels = new int[]{1 , 2 , 4,5};
            locStart = new int[]{1, 2, 3,4};
        }
        else if (loc == 60) {
            vessels = new int[]{1,2,3,4,5,6,3,4};
            locStart = new int[]{94,95,96,97,98,99,100,101};
        }
        dg= new DataGenerator(vessels, days, locStart, testInstance, "routing", weatherFile);
        try {
            dg.generateData();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ch = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(),dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations(),dg.getOperationGainGurobi(),dg.getWeatherPenaltyOperations());
        ch.createSortedOperations();
        ch.constructionHeuristic();
        //ch.printInitialSolution(vessels);

        bestRouteSailingCost = ch.getRouteSailingCost();
        bestRouteOperationGain = ch.getRouteOperationGain();

        currentRouteSailingCost = ch.getRouteSailingCost();
        currentRouteOperationGain = ch.getRouteOperationGain();

        //CopyValues cv= copyVesselRoutesAndSynchronization(ch.getVesselroutes());
        bestRoutes=copyVesselRoutes(ch.getVesselroutes());
        bestUnrouted = copyUnrouted(ch.getUnroutedTasks(),true);

        currentRoutes=copyVesselRoutes(ch.getVesselroutes());
        currentUnrouted=copyUnrouted(ch.getUnroutedTasks(),true);

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
                            consolidatedOperations.get(ID).setConsolidated(true);
                            consolidatedOperations.get(ID).setConsolidatedRoute(vessel);
                            consolidatedOperations.get(ID).setConnectedRoute1(0);
                            consolidatedOperations.get(ID).setConnectedRoute2(0);
                        } else if (bigTaskALNS[matrixIndex][1] == ID) {
                            consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setSmallTasks(true);
                            consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConnectedRoute1(vessel);
                            consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConsolidatedRoute(0);
                        } else if (bigTaskALNS[matrixIndex][2] == ID) {
                            consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setSmallTasks(true);
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

    public List<OperationInRoute> copyUnrouted(List<OperationInRoute> unroutedTasks,Boolean init) {
        List<OperationInRoute> old_unrouted = new ArrayList<OperationInRoute>();
        for (OperationInRoute unroutedTask : unroutedTasks) {
            OperationInRoute op = new OperationInRoute(unroutedTask.getID(), unroutedTask.getEarliestTime(), unroutedTask.getLatestTime());
            old_unrouted.add(op);
            if(!init) {
                if (dg.getBigTasksALNS()[op.getID() - 1 - dg.getStartNodes().length] != null) {
                    if (dg.getBigTasksALNS()[op.getID() - 1 - dg.getStartNodes().length][0] == op.getID()) {
                        consolidatedOperations.get(op.getID()).setConsolidated(false);
                        consolidatedOperations.get(op.getID()).setConsolidatedRoute(0);
                    } else if (dg.getBigTasksALNS()[op.getID() - 1 - dg.getStartNodes().length][1] == op.getID()) {
                        consolidatedOperations.get(dg.getBigTasksALNS()[op.getID() - 1 - dg.getStartNodes().length][0]).setSmallTasks(false);
                        consolidatedOperations.get(dg.getBigTasksALNS()[op.getID() - 1 - dg.getStartNodes().length][0]).setConnectedRoute1(0);
                    } else if (dg.getBigTasksALNS()[op.getID() - 1 - dg.getStartNodes().length][2] == op.getID()) {
                        consolidatedOperations.get(dg.getBigTasksALNS()[op.getID() - 1 - dg.getStartNodes().length][0]).setSmallTasks(false);
                        consolidatedOperations.get(dg.getBigTasksALNS()[op.getID() - 1 - dg.getStartNodes().length][0]).setConnectedRoute2(0);
                    }
                }
            }
        }
        return old_unrouted;
    }

    public void retainCurrentBestSolution(String typeSolution){
        CopyValues copyRoutes;
        if(typeSolution.equals("current")){
            copyRoutes= copyVesselRoutesAndSynchronization(currentRoutes);
            unroutedTasks = copyUnrouted(currentUnrouted,false);
        }
        else{
            copyRoutes= copyVesselRoutesAndSynchronization(bestRoutes);
            unroutedTasks = copyUnrouted(bestUnrouted,false);
        }
        vesselroutes = copyRoutes.getVesselRoutes();
        simultaneousOp=copyRoutes.getSimultaneousOp();
        precedenceOfOperations=copyRoutes.getPrecedenceOfOperations();
        precedenceOverOperations=copyRoutes.getPrecedenceOverOperations();
        simOpRoutes=copyRoutes.getSimOpRoutes();
        precedenceOfRoutes=copyRoutes.getPrecedenceOfRoutes();
        precedenceOverRoutes=copyRoutes.getPrecedenceOverRoutes();
    }

    public void updateSolAfterLocal(List<List<OperationInRoute>> vesselRoutesFromLocal,List<OperationInRoute> unrouted){
        CopyValues copyRoutes;
        copyRoutes= copyVesselRoutesAndSynchronization(vesselRoutesFromLocal);
        unroutedTasks = copyUnrouted(unrouted,false);
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

    public Boolean chooseNoise(){
        double totalWeight = 0.0d;
        for (double i : noiseWeights) {
            totalWeight += i;
        }
        int randomIndex = -1;
        double random = generator.nextDouble() * totalWeight;
        for (int i = 0; i < noiseWeights.length; ++i) {
            random -= noiseWeights[i];
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        List<Boolean> insertionMethods = new ArrayList<>(Arrays.asList(false, true));
        return insertionMethods.get(randomIndex);
    }

    public void setScoresAndVisits(int scoreIncrease, String insertMethod, String removeMethod, Boolean noise){
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
        if(!noise){
            noiseScore[0]+=scoreIncrease;
            noiseLastSegment[0]+=1;
        }
        if(noise) {
            noiseScore[1] += scoreIncrease;
            noiseLastSegment[1] += 1;
        }
    }

    public Boolean acceptedByProb(int currentObj, int newObj){
        double current= (double) currentObj;
        double newO= (double) newObj;
        if(TParameter==-1 && (newO / current >0.95)){
            TParameter=(-(currentObj-newObj)*Math.log(Math.exp(1)))/Math.log(0.5);
            //System.out.println("T start "+TParameter);
            //System.out.println("T decrase parameter "+T_decrease_parameter);
        }
        if(TParameter==-1){
            return false;
        }
        double randomNum=generator.nextDouble();
        double prob=Math.exp(-(currentObj-newObj)/TParameter);
        //System.out.println("probability "+prob);
        if(randomNum<=prob){
            return true;
        }
        return false;
    }

    public void evaluateSolution(int[] routeOperationGain, int[] routeSailingCost, List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks,
                                 String removalMethod, String insertMethod, Boolean noise){
        int bestObj= IntStream.of(bestRouteOperationGain).sum()-IntStream.of(bestRouteSailingCost).sum();
        int currentObj= IntStream.of(currentRouteOperationGain).sum()-IntStream.of(currentRouteSailingCost).sum();
        int newObj = IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum();
        if(newObj>bestObj){
            //System.out.println("New best global solution "+newObj);
            bestRouteSailingCost = routeSailingCost;
            bestRouteOperationGain = routeOperationGain;
            bestRoutes = copyVesselRoutes(vesselroutes);
            bestUnrouted = copyUnrouted(unroutedTasks,false);
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks,false);
            discoveredSolutions.add(newObj);
            setScoresAndVisits(reward1,insertMethod,removalMethod,noise);
        }
        else if(newObj>currentObj){
            //System.out.println("New best current solution "+newObj);
            //og ikke discovered før, oppdater current
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks,false);
            discoveredSolutions.add(newObj);
            if(!discoveredSolutions.contains(newObj) ){
                setScoresAndVisits(reward2,insertMethod,removalMethod,noise);
            }
            else{
                setScoresAndVisits(0,insertMethod,removalMethod,noise);
            }
        }
        else if(acceptedByProb(currentObj,newObj)){
            //System.out.println("New solution, worse than current, but selected by probability "+newObj);
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks,false);
            discoveredSolutions.add(newObj);
            if(!discoveredSolutions.contains(newObj) ){
                setScoresAndVisits(reward3,insertMethod,removalMethod,noise);
            }
            else{
                setScoresAndVisits(0,insertMethod,removalMethod,noise);
            }
        }
        else{
            //System.out.println("Continue with current solution");
            retainCurrentBestSolution("current");
            setScoresAndVisits(0,insertMethod,removalMethod,noise);
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


    public void writeToFile(List<String> parameters, String filename){
        try(FileWriter fw = new FileWriter(filename, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            for(String s : parameters) {
                out.println(s);
            }
        } catch (IOException e) {
            //exception handling left as an exercise for the reader

        }
    }


    public List<String> printLNSInsertSolution(int[] vessseltypes, int[]routeSailingCost, int[]routeOperationGain, List<List<OperationInRoute>> vesselRoutes,
                                               int[] startNodes, int[][][][] SailingTimes, int[][][] TimeVesselUseOnOperation, List<OperationInRoute> unroutedTasks,
                                               Map<Integer, PrecedenceValues> precedenceOverOperations, Map<Integer, ConsolidatedValues> consolidatedOperations,
                                               Map<Integer, PrecedenceValues> precedenceOfOperations, Map<Integer, ConnectedValues> simultaneousOp,
                                               List<Map<Integer, ConnectedValues>> simOpRoutes){
        List<String> final_routes=new ArrayList<>();
        System.out.println("Print Solution ALNS class");

        System.out.println("Sailing cost per route: "+ Arrays.toString(routeSailingCost));
        final_routes.add("Sailing cost per route: "+ Arrays.toString(routeSailingCost));
        System.out.println("Operation gain per route: "+Arrays.toString(routeOperationGain));
        final_routes.add("Operation gain per route: "+Arrays.toString(routeOperationGain));
        int obj= IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum();
        System.out.println("Objective value: "+obj);
        final_routes.add("Objective value: "+obj);
        for (int i=0;i<vesselRoutes.size();i++){
            int totalTime=0;
            System.out.println("VESSELINDEX "+i+" VESSELTYPE "+vessseltypes[i]);
            final_routes.add("VESSELINDEX "+i+" VESSELTYPE "+vessseltypes[i]);
            if (vesselRoutes.get(i)!=null) {
                for (int o=0;o<vesselRoutes.get(i).size();o++) {
                    System.out.println("Operation number: "+vesselRoutes.get(i).get(o).getID() + " Earliest start time: "+
                            vesselRoutes.get(i).get(o).getEarliestTime()+ " Latest Start time: "+ vesselRoutes.get(i).get(o).getLatestTime());
                    final_routes.add("Operation number: "+vesselRoutes.get(i).get(o).getID() + " Earliest start time: "+
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
            final_routes.add("TOTAL DURATION FOR ROUTE: "+totalTime);
        }
        if(!unroutedTasks.isEmpty()){
            System.out.println("UNROUTED TASKS");
            final_routes.add("UNROUTED TASKS");
            for(int n=0;n<unroutedTasks.size();n++) {
                System.out.println(unroutedTasks.get(n).getID());
                final_routes.add(String.valueOf(unroutedTasks.get(n).getID()));
            }
        }
        System.out.println(" ");
        final_routes.add(" ");
        System.out.println("SIMULTANEOUS DICTIONARY");
        final_routes.add("SIMULTANEOUS DICTIONARY");
        for(Map.Entry<Integer, ConnectedValues> entry : simultaneousOp.entrySet()){
            ConnectedValues simOp = entry.getValue();
            System.out.println("Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                    simOp.getRoute() + " with index: " + simOp.getIndex() + " earliest "+simOp.getOperationObject().getEarliestTime()
                    + " earliest "+simOp.getOperationObject().getLatestTime()+ " connected ID "+simOp.getConnectedOperationObject().getID() + " connected route "+simOp.getConnectedRoute());
            final_routes.add("Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
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
        final_routes.add("PRECEDENCE OVER DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOverOperations.entrySet()){
            PrecedenceValues presOverOp = entry.getValue();
            if(presOverOp.getConnectedOperationObject()!=null){
                System.out.println("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                        presOverOp.getRoute() + " with index: " + presOverOp.getIndex()+ " connected ID "+presOverOp.getConnectedOperationObject().getID() + " connected route "+presOverOp.getConnectedRoute());
                final_routes.add("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                        presOverOp.getRoute() + " with index: " + presOverOp.getIndex()+ " connected ID "+presOverOp.getConnectedOperationObject().getID() + " connected route "+presOverOp.getConnectedRoute());
            }
            else{
                System.out.println("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                        presOverOp.getRoute() + " with index: " + presOverOp.getIndex());
                final_routes.add("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                        presOverOp.getRoute() + " with index: " + presOverOp.getIndex());
            }

        }
        System.out.println("PRECEDENCE OF DICTIONARY");
        final_routes.add("PRECEDENCE OF DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfOperations.entrySet()){
            PrecedenceValues presOfOp = entry.getValue();
            if(presOfOp.getConnectedOperationObject()!=null){
                System.out.println("Precedence over operation: " + presOfOp.getOperationObject().getID() + " in route: " +
                        presOfOp.getRoute() + " with index: " + presOfOp.getIndex()+ " connected ID "+presOfOp.getConnectedOperationObject().getID() + " connected route "+presOfOp.getConnectedRoute());
                final_routes.add("Precedence over operation: " + presOfOp.getOperationObject().getID() + " in route: " +
                        presOfOp.getRoute() + " with index: " + presOfOp.getIndex()+ " connected ID "+presOfOp.getConnectedOperationObject().getID() + " connected route "+presOfOp.getConnectedRoute());
            }
            else{
                System.out.println("Precedence over operation: " + presOfOp.getOperationObject().getID() + " in route: " +
                        presOfOp.getRoute() + " with index: " + presOfOp.getIndex());
                final_routes.add("Precedence over operation: " + presOfOp.getOperationObject().getID() + " in route: " +
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
        boolean feasibleSolution = checkSolution(vesselRoutes);
        System.out.println("Is solution feasible? "+ feasibleSolution);
        final_routes.add("Is solution feasible? "+feasibleSolution);
        return final_routes;
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
                    System.out.println("Tasks: "+op.getOperationObject().getID()+" , "+conOp.getID());
                    System.out.println("Earliest times: "+op.getOperationObject().getEarliestTime()+" , "+conOp.getEarliestTime());
                    System.out.println("Latest times: "+op.getOperationObject().getLatestTime()+" , "+conOp.getLatestTime());


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
                        //System.out.println("Precedence infeasible, op: "+op.getOperationObject().getID()+ " and "+conop.getID());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void runDestroyRepair(){
        for (int i =0; i<numberOfIterations; i++){
            System.out.println("Iteration "+i);
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
            //System.out.println("---------- Removal method " + removalMethod+ " -----------");
            /*
            printLNSInsertSolution(vessels, currentRouteSailingCost, currentRouteOperationGain, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                    dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                    precedenceOfOperations, simultaneousOp, simOpRoutes);

             */
            //printLNSInsertSolution(vessels,bestRouteSailingCost,bestRouteOperationGain,vesselroutes,dg.getStartNodes(),dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),
            //unroutedTasks,precedenceOverOperations,consolidatedOperations,precedenceOfOperations,simultaneousOp,simOpRoutes);
            //PrintData.printSailingTimes(dg.getSailingTimes(),4,dg.getSimultaneousALNS().length,a.getVesselroutes().size());
            //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),dg.getStartNodes().length);
            Boolean noise=chooseNoise();
            LargeNeighboorhoodSearchInsert LNSI = new LargeNeighboorhoodSearchInsert(precedenceOverOperations,precedenceOfOperations,
                    simultaneousOp,simOpRoutes,precedenceOfRoutes,precedenceOverRoutes, consolidatedOperations,unroutedTasks,vesselroutes,
                    LNSR.getRemovedOperations(), dg.getTwIntervals(), dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getStartNodes(),
                    dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                    dg.getOperationGain(),dg.getBigTasksALNS(),dg.getOperationsForVessel(),dg.getOperationGainGurobi(),dg.getMaxDistance(),vessels, noise,dg.getWeatherPenaltyOperations());
            //for run insertion, insert method, alternatives: best, regret
            String insertionMethod = chooseInsertionMethod();
            //System.out.println("-------Insertion method " + insertionMethod + " ----------");
            LNSI.runLNSInsert(insertionMethod);
            //LNSI.printLNSInsertSolution(vessels);
            evaluateSolution(LNSI.getRouteOperationGain(),LNSI.getRouteSailingCost(),LNSI.getVesselRoutes(),LNSI.getUnroutedTasks(), removalMethod, insertionMethod,noise);
            //LNSI.printLNSInsertSolution(vessels);
            updateWeightsAndTemperatureAndSegmentIterations();
        }
    }

    public String[] runDestroyRepairILS(){
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
        //vesselroutes=LNSR.getVesselRoutes();
        //unroutedTasks=LNSR.getUnroutedTasks();
        //updateSolAfterLocal(vesselroutes,unroutedTasks);

        /*
        System.out.println("---------- Removal method " + removalMethod+ " -----------");
        LNSR.printLNSSolution(vessels);
        printLNSInsertSolution(vessels, currentRouteSailingCost, currentRouteOperationGain, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                precedenceOfOperations, simultaneousOp, simOpRoutes);

         */

        //printLNSInsertSolution(vessels,bestRouteSailingCost,bestRouteOperationGain,vesselroutes,dg.getStartNodes(),dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),
        //unroutedTasks,precedenceOverOperations,consolidatedOperations,precedenceOfOperations,simultaneousOp,simOpRoutes);
        //PrintData.printSailingTimes(dg.getSailingTimes(),4,dg.getSimultaneousALNS().length,a.getVesselroutes().size());
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),dg.getStartNodes().length);
        Boolean noise=chooseNoise();
        LargeNeighboorhoodSearchInsert LNSI = new LargeNeighboorhoodSearchInsert(precedenceOverOperations,precedenceOfOperations,
                simultaneousOp,simOpRoutes,precedenceOfRoutes,precedenceOverRoutes, consolidatedOperations,unroutedTasks,vesselroutes,
                LNSR.getRemovedOperations(), dg.getTwIntervals(), dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),dg.getOperationsForVessel(),dg.getOperationGainGurobi(),dg.getMaxDistance(),
                vessels, noise,dg.getWeatherPenaltyOperations());
        //for run insertion, insert method, alternatives: best, regret
        String insertionMethod = chooseInsertionMethod();
        //System.out.println("-------Insertion method " + insertionMethod + " ----------");
        LNSI.runLNSInsert(insertionMethod);
        //vesselroutes=LNSI.getVesselRoutes();
        //unroutedTasks=LNSI.getUnroutedTasks();
        //updateSolAfterLocal(vesselroutes,unroutedTasks);
        /*
        printLNSInsertSolution(vessels,bestRouteSailingCost,bestRouteOperationGain,vesselroutes,dg.getStartNodes(),dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),
        unroutedTasks,precedenceOverOperations,consolidatedOperations,precedenceOfOperations,simultaneousOp,simOpRoutes);
        LNSI.printLNSInsertSolution(vessels);

         */



        //LNSI.printLNSInsertSolution(vessels);
        updateWeightsAndTemperatureAndSegmentIterations();
        return new String[]{removalMethod,insertionMethod,String.valueOf(noise)};
    }

    public Boolean evaluateSolutionLocal(int[] routeOperationGain, int[] routeSailingCost, List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks){
        int bestObj= IntStream.of(bestRouteOperationGain).sum()-IntStream.of(bestRouteSailingCost).sum();
        int currentObj= IntStream.of(currentRouteOperationGain).sum()-IntStream.of(currentRouteSailingCost).sum();
        int newObj = IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum();
        if(newObj>currentObj){
            //System.out.println("New best global solution "+newObj);
            /*
            bestRouteSailingCost = routeSailingCost;
            bestRouteOperationGain = routeOperationGain;
            bestRoutes = copyVesselRoutes(vesselroutes);
            bestUnrouted = copyUnrouted(unroutedTasks,false);

             */
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks,false);
            //System.out.println("Do one more iteration local search");
            numberOfImprovementsLocal+=1;


            return true;
        }

        else{
            //retainCurrentBestSolution("current");
            return false;
        }
    }

    public void runILS(){
        runLocalSearchFullEnumeration();
        System.out.println("After first local search");
        /*
        printLNSInsertSolution(vessels, currentRouteSailingCost, currentRouteOperationGain, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                precedenceOfOperations, simultaneousOp, simOpRoutes);

         */
        for (int i = 0; i<Parameterfile_ALNS_ILS.numberOfILSIterations;i++){
            System.out.println("Iteration "+i);
            /*
            System.out.println("Called from runILS before destroy repair");
            ObjectiveValues ov3= ConstructionHeuristic.calculateObjective(vesselroutes,dg.getTimeVesselUseOnOperation(),dg.getStartNodes(),
                    dg.getSailingTimes(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),dg.getOperationGainGurobi(),
                    new int[vesselroutes.size()],new int[vesselroutes.size()],0, dg.getSimultaneousALNS(),dg.getBigTasksALNS());
            int[] routeSailingCostEV3=ov3.getRouteSailingCost();
            int[] routeOperationGainEV3=ov3.getRouteBenefitGain();
            printLNSInsertSolution(vessels, routeSailingCostEV3, routeOperationGainEV3, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                    dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                    precedenceOfOperations, simultaneousOp, simOpRoutes);

             */
            String[] perturbValues=runDestroyRepairILS();
            /*
            System.out.println("Called after destroy reapair");
            ObjectiveValues ov4= ConstructionHeuristic.calculateObjective(vesselroutes,dg.getTimeVesselUseOnOperation(),dg.getStartNodes(),
                    dg.getSailingTimes(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),dg.getOperationGainGurobi(),
                    new int[vesselroutes.size()],new int[vesselroutes.size()],0, dg.getSimultaneousALNS(),dg.getBigTasksALNS());
            int[] routeSailingCostEV4=ov4.getRouteSailingCost();
            int[] routeOperationGainEV4=ov4.getRouteBenefitGain();
            printLNSInsertSolution(vessels, routeSailingCostEV4, routeOperationGainEV4, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                    dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                    precedenceOfOperations, simultaneousOp, simOpRoutes);

             */
            EvaluateValues ev=runLocalSearchFullEnumeration();
            Boolean noise;
            noise= perturbValues[2].equals("true");
            evaluateSolution(ev.getRouteOperationGain(),ev.getRouteSailingCost(),ev.getVesselroutes(),ev.getUnrouted(),
                    perturbValues[0],perturbValues[1],noise);




        }


    }

    public EvaluateValues runLocalSearchFullEnumeration(){
        EvaluateValues returnValues = null;
        Boolean continueLocal=true;
        int localRuns=0;
        /*
        ObjectiveValues ov2= ConstructionHeuristic.calculateObjective(vesselroutes,dg.getTimeVesselUseOnOperation(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),dg.getOperationGainGurobi(),
                new int[vesselroutes.size()],new int[vesselroutes.size()],0, dg.getSimultaneousALNS(),dg.getBigTasksALNS());
        int[] routeSailingCostEV2=ov2.getRouteSailingCost();
        int[] routeOperationGainEV2=ov2.getRouteBenefitGain();
        evaluateSolutionLocal(routeOperationGainEV2, routeSailingCostEV2, vesselroutes, unroutedTasks);

         */
        while(continueLocal) {
            System.out.println("Number of times local is run for this call to full enumeration "+localRuns);
            if(localRuns>0){
                numberOfImprovementsLocal+=1;
            }
            ObjectiveValues ov1= ConstructionHeuristic.calculateObjective(vesselroutes,dg.getTimeVesselUseOnOperation(),dg.getStartNodes(),
                    dg.getSailingTimes(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),dg.getOperationGainGurobi(),
                    new int[vesselroutes.size()],new int[vesselroutes.size()],0, dg.getSimultaneousALNS(),dg.getBigTasksALNS());
            int[] routeSailingCostEV1=ov1.getRouteSailingCost();
            int[] routeOperationGainEV1=ov1.getRouteBenefitGain();
            int newObj1 = IntStream.of(routeOperationGainEV1).sum()-IntStream.of(routeSailingCostEV1).sum();
            System.out.println("New obj 1 "+newObj1);
            localRuns+=1;
            /*
            System.out.println("Before locAL SEARCH");
            for (int i = 0; i < vesselroutes.size(); i++) {
                System.out.println("VESSELINDEX " + i);
                if (vesselroutes.get(i) != null) {
                    for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                        System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                    }
                }
            }
            System.out.println("Unrouted ");
            for (OperationInRoute un : unroutedTasks) {
                System.out.println(un.getID());
            }

             */


            System.out.println("run 1RL");
            LS_operators LSO = new LS_operators(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                    dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                    dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(), vesselroutes, unroutedTasks,
                    precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                    simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, dg.getOperationGainGurobi());
            LSO.runNormalLSO("1RL");
            vesselroutes = LSO.getVesselroutes();
            unroutedTasks=LSO.getUnroutedTasks();
            updateSolAfterLocal(vesselroutes,unroutedTasks);
            System.out.println("run 2RL");
            //evaluateSolutionLocal(LSO.getRouteOperationGain(), LSO.getRouteSailingCost(), LSO.getVesselroutes(), LSO.getUnroutedTasks());
            LSO.runNormalLSO("2RL");
            vesselroutes = LSO.getVesselroutes();
            unroutedTasks=LSO.getUnroutedTasks();
            updateSolAfterLocal(vesselroutes,unroutedTasks);
            System.out.println("run 1EX");
            //evaluateSolutionLocal(LSO.getRouteOperationGain(), LSO.getRouteSailingCost(), LSO.getVesselroutes(), LSO.getUnroutedTasks());
            LSO.runNormalLSO("1EX");
            vesselroutes = LSO.getVesselroutes();
            unroutedTasks=LSO.getUnroutedTasks();
            updateSolAfterLocal(vesselroutes,unroutedTasks);
            //System.out.println("index 25 in sim op before 2EX: "+simultaneousOp.get(25).getIndex());

            /*
            ObjectiveValues ov3= ConstructionHeuristic.calculateObjective(vesselroutes,dg.getTimeVesselUseOnOperation(),dg.getStartNodes(),
                    dg.getSailingTimes(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),dg.getOperationGainGurobi(),
                    new int[vesselroutes.size()],new int[vesselroutes.size()],0, dg.getSimultaneousALNS(),dg.getBigTasksALNS());
            int[] routeSailingCostEV3=ov3.getRouteSailingCost();
            int[] routeOperationGainEV3=ov3.getRouteBenefitGain();

            printLNSInsertSolution(vessels, routeSailingCostEV3, routeOperationGainEV3, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                    dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                    precedenceOfOperations, simultaneousOp, simOpRoutes);

             */

            System.out.println("run 2EX");
            //evaluateSolutionLocal(LSO.getRouteOperationGain(), LSO.getRouteSailingCost(), LSO.getVesselroutes(), LSO.getUnroutedTasks());
            LSO.runNormalLSO("2EX");
            vesselroutes = LSO.getVesselroutes();
            unroutedTasks = LSO.getUnroutedTasks();
            updateSolAfterLocal(vesselroutes,unroutedTasks);

            //evaluateSolutionLocal(LSO.getRouteOperationGain(), LSO.getRouteSailingCost(), LSO.getVesselroutes(), LSO.getUnroutedTasks());
            //System.out.println("index 25 in sim op before insert normal: "+simultaneousOp.get(25).getIndex());
            System.out.println("run insert normal");
            LSO.runNormalLSO("insertNormal");
            vesselroutes = LSO.getVesselroutes();
            unroutedTasks=LSO.getUnroutedTasks();
            updateSolAfterLocal(vesselroutes,unroutedTasks);
            /*
            if(LSO.getPrint()){
                ObjectiveValues ov3= ConstructionHeuristic.calculateObjective(vesselroutes,dg.getTimeVesselUseOnOperation(),dg.getStartNodes(),
                        dg.getSailingTimes(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),dg.getOperationGainGurobi(),
                        new int[vesselroutes.size()],new int[vesselroutes.size()],0, dg.getSimultaneousALNS(),dg.getBigTasksALNS());
                int[] routeSailingCostEV3=ov3.getRouteSailingCost();
                int[] routeOperationGainEV3=ov3.getRouteBenefitGain();
                printLNSInsertSolution(vessels, routeSailingCostEV3, routeOperationGainEV3, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                        dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                        precedenceOfOperations, simultaneousOp, simOpRoutes);
                LSO.setPrint(false);
            }

             */



            /*
            printLNSInsertSolution(vessels, LSO.getRouteSailingCost(), LSO.getRouteOperationGain(), vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                    dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                    precedenceOfOperations, simultaneousOp, simOpRoutes);
            */
            //evaluateSolutionLocal(LSO.getRouteOperationGain(), LSO.getRouteSailingCost(), LSO.getVesselroutes(), LSO.getUnroutedTasks());
            System.out.println("run relocate LSOs");
            RelocateInsert RI = new RelocateInsert(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                    dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                    dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(),
                    unroutedTasks, vesselroutes, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                    simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, dg.getOperationGainGurobi());

            RI.runRelocateLSO("relocate");
            vesselroutes = RI.getVesselRoutes();
            unroutedTasks=RI.getUnroutedTasks();
            updateSolAfterLocal(vesselroutes,unroutedTasks);
            //continueLocal=evaluateSolutionLocal(RI.getRouteOperationGain(), RI.getRouteSailingCost(), RI.getVesselRoutes(), RI.getUnroutedTasks());

            System.out.println("run precedence");
            RI.runRelocateLSO("precedence");
            vesselroutes = RI.getVesselRoutes();
            unroutedTasks=RI.getUnroutedTasks();
            updateSolAfterLocal(vesselroutes,unroutedTasks);
            //evaluateSolutionLocal(RI.getRouteOperationGain(), RI.getRouteSailingCost(), RI.getVesselRoutes(), RI.getUnroutedTasks());

            System.out.println("run simultaneous");
            RI.runRelocateLSO("simultaneous");
            vesselroutes = RI.getVesselRoutes();
            unroutedTasks=RI.getUnroutedTasks();
            updateSolAfterLocal(vesselroutes,unroutedTasks);
            System.out.println("Called from full enumeration");



            //evaluateSolutionLocal(RI.getRouteOperationGain(), RI.getRouteSailingCost(), RI.getVesselRoutes(), RI.getUnroutedTasks());

            SwitchConsolidated sc = new SwitchConsolidated(precedenceOverOperations, precedenceOfOperations,
                    simultaneousOp, simOpRoutes, precedenceOfRoutes, precedenceOverRoutes,
                    consolidatedOperations, unroutedTasks, vesselroutes, dg.getTwIntervals(),
                    dg.getPrecedenceALNS(), dg.getSimultaneousALNS(), dg.getStartNodes(), dg.getSailingTimes(),
                    dg.getTimeVesselUseOnOperation(), dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(),
                    dg.getOperationGain(), dg.getBigTasksALNS(), dg.getOperationsForVessel(), dg.getOperationGainGurobi(), vessels,dg.getWeatherPenaltyOperations());
            System.out.println("run consolidated");

            /*
            printLNSInsertSolution(vessels, routeSailingCostEV1, routeOperationGainEV1, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                    dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                    precedenceOfOperations, simultaneousOp, simOpRoutes);

             */
            ObjectiveValues ov2= ConstructionHeuristic.calculateObjective(vesselroutes,dg.getTimeVesselUseOnOperation(),dg.getStartNodes(),
                    dg.getSailingTimes(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),dg.getOperationGainGurobi(),
                    new int[vesselroutes.size()],new int[vesselroutes.size()],0, dg.getSimultaneousALNS(),dg.getBigTasksALNS());
            int[] routeSailingCostEV2=ov2.getRouteSailingCost();
            int[] routeOperationGainEV2=ov2.getRouteBenefitGain();
            int newObj2 = IntStream.of(routeOperationGainEV2).sum()-IntStream.of(routeSailingCostEV2).sum();
            if(newObj2==newObj1){
                continueLocal=false;
            }
            sc.runSwitchConsolidated();
            vesselroutes=sc.getVesselRoutes();
            unroutedTasks=sc.getUnroutedTasks();
            updateSolAfterLocal(vesselroutes,unroutedTasks);

            //evaluateSolutionLocal(sc.getRouteOperationGain(), sc.getRouteSailingCost(), sc.getVesselRoutes(), sc.getUnroutedTasks());
            System.out.println("New obj 2 "+newObj2);
            returnValues=new EvaluateValues(sc.getRouteOperationGain(), sc.getRouteSailingCost(), sc.getVesselRoutes(), sc.getUnroutedTasks());
            if(localRuns>10){
                continueLocal=false;
            }

            //System.out.println(continueLocal);
        }

        return returnValues;
    }

    public void updateWeightsAndTemperatureAndSegmentIterations(){
        if(TParameter!=-1){
            TParameter=TParameter*T_decrease_parameter;
        }
        //System.out.println("T parameter "+TParameter);
        if(segmentIteration<numberOfSegmentIterations){
            segmentIteration+=1;
        }
        else{
            segmentIteration=1;
            for (int n=0;n<insertionWeights.length;n++){
                if(insertionVisitsLastSegment[n]!=0) {
                    double newWeight = insertionWeights[n] * (1 - controlParameter) + (controlParameter * insertionScore[n]) / insertionVisitsLastSegment[n];
                    insertionWeights[n] = newWeight;
                    if(insertionWeights[n] <lowerThresholdWeights){
                        insertionWeights[n] =lowerThresholdWeights;
                    }
                }
            }
            for (int n=0;n<removalWeights.length;n++){
                if(removalVisitsLastSegment[n]!=0) {
                    double newWeight = removalWeights[n] * (1 - controlParameter) + (controlParameter * removalScore[n]) / removalVisitsLastSegment[n];
                    removalWeights[n] = newWeight;
                    if(removalWeights[n] <lowerThresholdWeights){
                        removalWeights[n] =lowerThresholdWeights;
                    }
                }
            }
            for (int n=0;n<noiseWeights.length;n++){
                if(noiseLastSegment[n]!=0) {
                    double newWeight = noiseWeights[n] * (1 - controlParameter) + (controlParameter * noiseScore[n]) / noiseLastSegment[n];
                    noiseWeights[n] = newWeight;
                    if(noiseWeights[n] <lowerThresholdWeights){
                        noiseWeights[n] =lowerThresholdWeights;
                    }
                }
            }
            /*
            System.out.println("Insertion weights");
            System.out.println(Arrays.toString(insertionWeights));
            System.out.println("Removal weights");
            System.out.println(Arrays.toString(removalWeights));

            System.out.println("Noise weights");
            System.out.println(Arrays.toString(noiseWeights));
             */
            Arrays.fill(removalVisitsLastSegment, 0);
            Arrays.fill(insertionVisitsLastSegment, 0);
            Arrays.fill(noiseLastSegment, 0);
            Arrays.fill(insertionScore, 0);
            Arrays.fill(removalScore, 0);
            Arrays.fill(noiseScore, 0);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Start ILS");
        long startTime = System.nanoTime();
        ALNS_ILS alns = new ALNS_ILS();
        int constructionObjective=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();
        List<Integer> unroutedList=new ArrayList<>();
        for (OperationInRoute ur:alns.bestUnrouted){
            unroutedList.add(ur.getID());
        }

        alns.runDestroyRepair();

        int afterLarge=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();
        List<Integer> unroutedList2=new ArrayList<>();
        for (OperationInRoute ur:alns.bestUnrouted){
            unroutedList2.add(ur.getID());
        }
        alns.runLocalSearchFullEnumeration();
        //alns.runILS();
        alns.retainCurrentBestSolution("best");
        long endTime   = System.nanoTime();
        int localObjective=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();

        alns.printLNSInsertSolution(alns.vessels, alns.bestRouteSailingCost, alns.bestRouteOperationGain, alns.bestRoutes,
                alns.dg.getStartNodes(), alns.dg.getSailingTimes(), alns.dg.getTimeVesselUseOnOperation(), alns.unroutedTasks,
                alns.precedenceOverOperations, alns.consolidatedOperations,
                alns.precedenceOfOperations, alns.simultaneousOp, alns.simOpRoutes);
        System.out.println("Construction Objective "+constructionObjective);
        System.out.println("afterALNS "+afterLarge);
        System.out.println("Local Objective "+localObjective);
        long totalTime = endTime - startTime;
        System.out.println("Time "+totalTime/1000000000);
        //System.out.println(alns.generator.doubles());

        System.out.println("Unrouted construction");
        for (Integer urInt:unroutedList){
            System.out.println(urInt);
        }

        System.out.println("Unrouted after large");
        for (Integer urInt:unroutedList2){
            System.out.println(urInt);
        }

        System.out.println("Unrouted after local search");
        for (OperationInRoute ur:alns.bestUnrouted){
            System.out.println(ur.getID());
        }
        System.out.println("improvements local "+alns.numberOfImprovementsLocal);

    }

}
