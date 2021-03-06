import java.io.FileNotFoundException;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import java.util.stream.IntStream;

public class LargeNeighboorhoodSearchRemoval {
    private Map<Integer,PrecedenceValues> precedenceOverOperations;
    private Map<Integer,PrecedenceValues> precedenceOfOperations;
    //map for operations that are connected as simultaneous operations. ID= operation number. Value= Simultaneous value.
    private Map<Integer, ConnectedValues> simultaneousOp;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;
    private List<Map<Integer,PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer,PrecedenceValues>> precedenceOverRoutes;
    private Map<Integer, ConsolidatedValues> consolidatedOperations;
    private List<OperationInRoute> unroutedTasks;
    private List<List<OperationInRoute>> vesselRoutes;
    private int [][] twIntervals;
    private int [][] precedenceALNS;
    private int [][] simALNS;
    private int [][] bigTasksALNS;
    private int [] startNodes;
    private int [][][][] SailingTimes;
    private int [][][] TimeVesselUseOnOperation;
    private int numberOfRemoval;
    private int[] SailingCostForVessel;
    private int[] EarliestStartingTimeForVessel;
    private int[][][] operationGain;
    private int[][][] operationGainGurobi;
    private int[] routeSailingCost;
    private int[] routeOperationGain;
    private ArrayList<Integer> sortedOperationsByProfitDecrease;
    private ArrayList<Integer> sortedOperationsByRelatedness;
    private ArrayList<OperationInRoute> setOfRelatedOperations = new ArrayList<>();
    private int objValue;
    private ArrayList<Integer> removedOperations = new ArrayList<>();
    private Map<Integer,routeIndexObjectForOperation> routeObjectDict=new HashMap<>();
    private int[][] distOperationsInInstance;
    private double relatednessWeightDistance;
    private double relatednessWeightDuration;
    private double relatednessWeightTimewindows;
    private double relatednessWeightPrecedenceOver;
    private double relatednessWeightPrecedenceOf;
    private double relatednessWeightSimultaneous;
    private List<Integer> randomIntStream;
    Random generator;
    private int[] vesseltypes;

    public LargeNeighboorhoodSearchRemoval(Map<Integer,PrecedenceValues> precedenceOverOperations, Map<Integer,PrecedenceValues> precedenceOfOperations,
                                        Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, ConnectedValues>> simOpRoutes,
                                        List<Map<Integer,PrecedenceValues>> precedenceOfRoutes, List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                                        Map<Integer, ConsolidatedValues> consolidatedOperations, List<OperationInRoute> unroutedTasks,
                                        List<List<OperationInRoute>> vesselRoutes, int [][]twIntervals,
                                        int[][] precedenceALNS,int[][] simALNS,int [] startNodes, int [][][][] SailingTimes,
                                        int [][][] TimeVesselUseOnOperation, int[] SailingCostForVessel,int[] EarliestStartingTimeForVessel,
                                        int[][][] operationGain, int[][] bigTasksALNS, double numberOfRemoval, int randomSeed,
                                    int[][] distOperationsInInstance,double relatednessWeightDistance, double relatednessWeightDuration,
                                    double relatednessWeightTimewindows, double relatednessWeightPrecedenceOver, double relatednessWeightPrecedenceOf,
                                    double relatednessWeightSimultaneous, int[][][] operationGainGurobi,int[]vesseltypes){
        this.precedenceOverOperations=precedenceOverOperations;
        this.precedenceOfOperations=precedenceOfOperations;
        this.simultaneousOp=simultaneousOp;
        this.simOpRoutes=simOpRoutes;
        this.precedenceOfRoutes=precedenceOfRoutes;
        this.precedenceOverRoutes=precedenceOverRoutes;
        this.unroutedTasks=unroutedTasks;
        this.vesselRoutes=vesselRoutes;
        this.consolidatedOperations=consolidatedOperations;
        this.twIntervals=twIntervals;
        this.precedenceALNS=precedenceALNS;
        this.simALNS=simALNS;
        this.startNodes=startNodes;
        this.SailingTimes=SailingTimes;
        this.TimeVesselUseOnOperation=TimeVesselUseOnOperation;
        this.SailingCostForVessel=SailingCostForVessel;
        this.EarliestStartingTimeForVessel=EarliestStartingTimeForVessel;
        this.operationGain=operationGain;
        this.numberOfRemoval=(int) numberOfRemoval;
        this.numberOfRemoval= (int) Math.floor(numberOfRemoval*(TimeVesselUseOnOperation[0].length-unroutedTasks.size()));
        this.routeSailingCost=new int[vesselRoutes.size()];
        this.routeOperationGain=new int[vesselRoutes.size()];
        this.objValue=0;
        this.sortedOperationsByProfitDecrease=new ArrayList<>();
        this.bigTasksALNS=bigTasksALNS;
        this.generator= new Random(randomSeed);
        this.distOperationsInInstance=distOperationsInInstance;
        this.relatednessWeightDistance=relatednessWeightDistance;
        this.relatednessWeightDuration=relatednessWeightDuration;
        this.relatednessWeightTimewindows=relatednessWeightTimewindows;
        this.relatednessWeightPrecedenceOver=relatednessWeightPrecedenceOver;
        this.relatednessWeightPrecedenceOf=relatednessWeightPrecedenceOf;
        this.relatednessWeightSimultaneous=relatednessWeightSimultaneous;
        this.operationGainGurobi=operationGainGurobi;
        this.vesseltypes=vesseltypes;
    }
    //removal methods

    public int[] findRandomIndexRoute(){
        int randomRoute=0;
        Boolean emptyRoute=true;
        while (emptyRoute) {
            randomRoute=generator.nextInt(vesselRoutes.size());
            //randomRoute = ThreadLocalRandom.current().nextInt(0, vesselRoutes.size());
            if(vesselRoutes.get(randomRoute)!=null && vesselRoutes.get(randomRoute).size()>0){
                emptyRoute=false;
            }
        }
        //int randomIndex = ThreadLocalRandom.current().nextInt(0, vesselRoutes.get(randomRoute).size());
        int randomIndex = generator.nextInt(vesselRoutes.get(randomRoute).size());
        return new int[]{randomRoute,randomIndex};
    }

    public void randomRemoval(){
        while (removedOperations.size()<numberOfRemoval){
            int [] indexRoute= findRandomIndexRoute();
            int randomRoute=indexRoute[0];
            int randomIndex =indexRoute[1];
            /*
            System.out.println("STATUS BEFORE REMOVAL");
            for(int r=0;r<vesselRoutes.size();r++) {
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

             */
            OperationInRoute selectedTask=vesselRoutes.get(randomRoute).get(randomIndex);
            //synchronized tasks --> either simultaneous or precedence
            removeOperations(selectedTask,randomRoute,randomIndex,"randRemoval");
        }
    }

    public void worstRemoval(){
        sortOperationsProfitDecrease("benefit");
        while (removedOperations.size()<numberOfRemoval && sortedOperationsByProfitDecrease.size()>0){
            double randomY=generator.nextDouble();
            int removeIndex = (int) Math.floor(Math.pow(randomY,ParameterFile.randomnessParameterRemoval)*sortedOperationsByProfitDecrease.size());
            int selectedTaskID=sortedOperationsByProfitDecrease.get(removeIndex);
            routeIndexObjectForOperation properties=routeObjectDict.get(selectedTaskID);
            OperationInRoute selectedTask = properties.getOr();
            int route=properties.getRoute();
            int indexInRoute=0;
            for (int i=0;i<vesselRoutes.get(route).size();i++){
                if(vesselRoutes.get(route).get(i).getID()==selectedTaskID){
                    indexInRoute=i;
                }
            }
            removeOperations(selectedTask,route,indexInRoute,"worstRemoval");
        }
    }

    public void worstRemovalSailing(){
        sortOperationsProfitDecrease("sailing");
        while (removedOperations.size()<numberOfRemoval && sortedOperationsByProfitDecrease.size()>0){
            double randomY=generator.nextDouble();
            int removeIndex = (int) Math.floor(Math.pow(randomY,ParameterFile.randomnessParameterRemoval)*sortedOperationsByProfitDecrease.size());
            //System.out.println("remove index: "+removeIndex);
            //System.out.println("length sorted list: "+sortedOperationsByProfitDecrease.size());
            int selectedTaskID=sortedOperationsByProfitDecrease.get(removeIndex);
            routeIndexObjectForOperation properties=routeObjectDict.get(selectedTaskID);
            OperationInRoute selectedTask = properties.getOr();
            int route=properties.getRoute();
            int index=0;
            for (int i=0;i<vesselRoutes.get(route).size();i++){
                if(vesselRoutes.get(route).get(i).getID()==selectedTaskID){
                    index=i;
                }
            }
            removeOperations(selectedTask,route,index,"worstRemoval");
        }
    }

    public void relatedRemoval(){
        int [] indexRoute= findRandomIndexRoute();
        int randomRoute=indexRoute[0];
        int randomIndex =indexRoute[1];
        OperationInRoute randomOp=vesselRoutes.get(randomRoute).get(randomIndex);
        setOfRelatedOperations.add(randomOp);
        removeOperations(randomOp,randomRoute,randomIndex,"relatedRemoval");
        while (removedOperations.size()<numberOfRemoval){
            int randomIndexFromRelatedSet=generator.nextInt(setOfRelatedOperations.size());
            OperationInRoute chosenTask=setOfRelatedOperations.get(randomIndexFromRelatedSet);
            //System.out.println("Operation to be compared randomness with "+chosenTask.getID());
            sortOperationsRelatedness(chosenTask,"simple");
            double randomY=generator.nextDouble();
            //System.out.println("number to multiply with length to find index "+Math.pow(randomY,ParameterFile.randomnessParameterRemoval));
            //System.out.println("floor value of "+Math.floor(Math.pow(randomY,ParameterFile.randomnessParameterRemoval)*sortedOperationsByRelatedness.size()));
            //System.out.println("not floor value of "+Math.pow(randomY,ParameterFile.randomnessParameterRemoval)*sortedOperationsByRelatedness.size());
            int removeIndex = (int) Math.floor(Math.pow(randomY,ParameterFile.randomnessParameterRemoval)*sortedOperationsByRelatedness.size());
            //System.out.println("remove index: "+removeIndex);
            //System.out.println("length sorted list: "+sortedOperationsByRelatedness.size());
            int selectedTaskID=sortedOperationsByRelatedness.get(removeIndex);
            routeIndexObjectForOperation properties=routeObjectDict.get(selectedTaskID);
            OperationInRoute selectedTask = properties.getOr();
            int route=properties.getRoute();
            int index=0;
            for (int i=0;i<vesselRoutes.get(route).size();i++){
                if(vesselRoutes.get(route).get(i).getID()==selectedTaskID){
                    index=i;
                }
            }
            removeOperations(selectedTask,route,index,"relatedRemoval");
        }
    }

    public void synchronizedRemoval(){
        for(int i=0;i<precedenceALNS.length;i++){
            //PrintData.printPrecedenceALNS(precedenceALNS);
            if (precedenceALNS[i][0]!=0){
                //System.out.println("REMOVE PRECEDENCE");
                int selectedTaskID=i+startNodes.length+1;
                if(precedenceOverOperations.get(selectedTaskID)!=null){
                    PrecedenceValues pv= precedenceOverOperations.get(selectedTaskID);
                    //System.out.println("Remove pres");
                    removeSynchronizedOp(simultaneousOp.get(selectedTaskID), precedenceOverOperations.get(selectedTaskID),
                            precedenceOfOperations.get(selectedTaskID), selectedTaskID, pv.getOperationObject(),"syncRemoval");
                    removeDependentOperations(selectedTaskID, "syncRemoval");
                }
            }
        }
        for(int i=0;i<simALNS.length;i++){
            if (simALNS[i][0]!=0){
                //System.out.println("REMOVE SIMULTANEOUS");
                int selectedTaskID=i+startNodes.length+1;
                if(simultaneousOp.get(selectedTaskID)!=null){
                    ConnectedValues cv= simultaneousOp.get(selectedTaskID);
                    //System.out.println("Remove sim");
                    removeSynchronizedOp(simultaneousOp.get(selectedTaskID), precedenceOverOperations.get(selectedTaskID),
                            precedenceOfOperations.get(selectedTaskID), selectedTaskID, cv.getOperationObject(),"syncRemoval");
                    removeDependentOperations(selectedTaskID,"syncRemoval");
                }
            }
        }
    }

    public void routeRemoval(){
        while (removedOperations.size()<numberOfRemoval) {
            int randomRoute = 0;
            Boolean emptyRoute = true;
            while (emptyRoute) {
                randomRoute = generator.nextInt(vesselRoutes.size());
                //randomRoute = ThreadLocalRandom.current().nextInt(0, vesselRoutes.size());
                if (vesselRoutes.get(randomRoute) != null && vesselRoutes.get(randomRoute).size() > 0) {
                    emptyRoute = false;
                }
            }
            int sizeOfRoute=vesselRoutes.get(randomRoute).size();
            //System.out.println("ROUTE TO REMOVE: "+randomRoute);
            for(int n=0;n<sizeOfRoute;n++){
                if(vesselRoutes.get(randomRoute).size()>0) {
                    OperationInRoute or = vesselRoutes.get(randomRoute).get(0);
                    removeOperations(or, randomRoute, 0, "routeRemoval");
                }
            }
        }
    }

    public void sortOperationsRelatedness(OperationInRoute chosenTask, String relatedType){
        //want to remove the operations that causes the smallest profit decrease if it is removed
        Map<Integer,Double> relatedness = new TreeMap<Integer,Double>();
        ArrayList<Integer> sortedOperationsByRelatednessTemp=new ArrayList<>();
        //modelling choice per now: choose to not consider sync tasks - this is an approximation, discuss this next meeting
        for (int r=0;r< vesselRoutes.size();r++) {
            if(vesselRoutes.get(r)!= null && vesselRoutes.get(r).size()>0) {
                for (int i = 0; i < vesselRoutes.get(r).size(); i++) {
                    //System.out.println((g+1+startNodes.length)+" "+operationGain[0][g][0]);
                    //Key value (= operation number) in savingValues is not null indexed
                    OperationInRoute evaluatedOr = vesselRoutes.get(r).get(i);
                    double distance= distOperationsInInstance[chosenTask.getID()-startNodes.length-1][evaluatedOr.getID()-startNodes.length-1];
                    double differenceDuration=Math.abs(TimeVesselUseOnOperation[r][chosenTask.getID()-startNodes.length-1][chosenTask.getEarliestTime()-1]-
                            TimeVesselUseOnOperation[r][evaluatedOr.getID()-startNodes.length-1][evaluatedOr.getEarliestTime()-1]);
                    double differenceTimeWindows = Math.abs((twIntervals[chosenTask.getID()-startNodes.length-1][1]-twIntervals[chosenTask.getID()-startNodes.length-1][0])
                            -(twIntervals[evaluatedOr.getID()-startNodes.length-1][1]-twIntervals[evaluatedOr.getID()-startNodes.length-1][0]));
                    double precdenceOver=1.0;
                    if(precedenceALNS[chosenTask.getID()-startNodes.length-1][0]!=0 && precedenceALNS[evaluatedOr.getID()-startNodes.length-1][0] !=0){
                        precdenceOver=0.0;
                    }
                    double precdenceOf=1.0;
                    if(precedenceALNS[chosenTask.getID()-startNodes.length-1][1]!= 0 && precedenceALNS[evaluatedOr.getID()-startNodes.length-1][1]!=0){
                        precdenceOf=0.0;
                    }
                    int sim1 =simALNS[chosenTask.getID()-startNodes.length-1][0]+simALNS[chosenTask.getID()-startNodes.length-1][1];
                    int sim2=simALNS[evaluatedOr.getID()-startNodes.length-1][0]+simALNS[evaluatedOr.getID()-startNodes.length-1][1];
                    double sim=1.0;
                    if(sim1!=0 && sim2!=0){
                        sim=0.0;
                    }
                    double relatednessForOperation;
                    if(relatedType.equals("not_simple")) {
                        relatednessForOperation = relatednessWeightDistance * distance + relatednessWeightDuration * differenceDuration +
                                relatednessWeightTimewindows * differenceTimeWindows + relatednessWeightPrecedenceOver * precdenceOver +
                                relatednessWeightPrecedenceOf * precdenceOf + relatednessWeightSimultaneous * sim;
                    }
                    else{
                        relatednessForOperation = relatednessWeightDistance * distance + relatednessWeightDuration * differenceDuration;
                    }
                    relatedness.put(evaluatedOr.getID(), relatednessForOperation);
                    //System.out.println("relatedness for operation: "+relatednessForOperation);
                    routeObjectDict.put(evaluatedOr.getID(), new routeIndexObjectForOperation(r, evaluatedOr));
                }
            }
        }
        //System.out.println("Before sorting"+relatedness);
        SortedSet<Map.Entry<Integer,Double>> sortedRelatedness=entriesSortedByValues(relatedness);
        //System.out.println("After sorting"+sortedRelatedness);
        //System.out.println("Print all keys + values");
        for (Map.Entry<Integer, Double> entry  : sortedRelatedness) {
            //System.out.println("Key "+entry.getKey()+" : Value "+entry.getValue());
            sortedOperationsByRelatednessTemp.add(entry.getKey());
        }
        sortedOperationsByRelatedness=sortedOperationsByRelatednessTemp;
    }

    /*
    public Integer checkChangeEarliestLastOperation(int earliestInsertionOperation, int earliestLastOperation, int indexInRoute, int routeIndex,int o,List<List<OperationInRoute>> vesselroutes,
                                                           int[][][] TimeVesselUseOnOperation, int[] startNodes, int [][][][] SailingTimes){
        System.out.println("operation to remove: "+vesselroutes.get(routeIndex).get(indexInRoute+1).getID());
        if(vesselroutes.get(routeIndex).size()==1){
            return 0;
        }
        if (indexInRoute == vesselroutes.get(routeIndex).size() - 2) {
            return earliestLastOperation - vesselroutes.get(routeIndex).get(indexInRoute).getEarliestTime();
        }
        int lastEarliest=0;
        if(indexInRoute!=-1) {
            lastEarliest=earliestInsertionOperation;
            System.out.println("Last earliest start "+lastEarliest);
            for (int f = indexInRoute+2; f < vesselroutes.get(routeIndex).size(); f++) {
                int sailingtime=0;
                int operationtime=0;
                if (f == indexInRoute+2) {
                    operationtime = TimeVesselUseOnOperation[routeIndex][vesselroutes.get(routeIndex).get(f - 2).getID() - startNodes.length - 1][lastEarliest - 1];
                    sailingtime = SailingTimes[routeIndex][lastEarliest + operationtime - 1][vesselroutes.get(routeIndex).get(f - 2).getID() - 1]
                            [vesselroutes.get(routeIndex).get(f).getID() - 1];
                }
                else if(f>indexInRoute+2){
                    operationtime = TimeVesselUseOnOperation[routeIndex][vesselroutes.get(routeIndex).get(f - 1).getID() - startNodes.length - 1][lastEarliest - 1];
                    System.out.println("last operation: "+ vesselroutes.get(routeIndex).get(f - 1).getID() );
                    System.out.println("Last earliest: "+lastEarliest+ " operationTime "+operationtime);
                    sailingtime = SailingTimes[routeIndex][lastEarliest + operationtime - 1][vesselroutes.get(routeIndex).get(f - 1).getID() - 1]
                            [vesselroutes.get(routeIndex).get(f).getID() - 1];
                }
                int newTime = Math.min(vesselroutes.get(routeIndex).get(f).getEarliestTime(), lastEarliest +
                        sailingtime + operationtime);
                if (newTime == vesselroutes.get(routeIndex).get(f).getEarliestTime()) {
                    System.out.println("other limiting constraint");
                    return 0;
                }
                lastEarliest = newTime;
                System.out.println("Operation "+vesselroutes.get(routeIndex).get(f).getID()+ "new earliest " + lastEarliest);
            }
        }
        if(indexInRoute==-1){
            OperationInRoute startIteratingOperation=vesselroutes.get(routeIndex).get(1);
            lastEarliest=EarliestStartingTimeForVessel[routeIndex]+SailingTimes[routeIndex]
                    [EarliestStartingTimeForVessel[routeIndex]][routeIndex][startIteratingOperation.getID()-1];
            for (int f = 1; f < vesselroutes.get(routeIndex).size(); f++) {
                int sailingtime;
                int operationtime;
                if (f == 1) {
                    if (vesselroutes.get(routeIndex).size()==2) {
                        return earliestLastOperation-lastEarliest;
                    }
                    else{
                        continue;
                    }
                }
                else {
                    operationtime = TimeVesselUseOnOperation[routeIndex][vesselroutes.get(routeIndex).get(f - 1).getID() - startNodes.length - 1][lastEarliest - 1];
                    sailingtime = SailingTimes[routeIndex][lastEarliest + operationtime - 1][vesselroutes.get(routeIndex).get(f - 1).getID() - 1]
                            [vesselroutes.get(routeIndex).get(f).getID() - 1];
                }
                int newTime = Math.max(vesselroutes.get(routeIndex).get(f).getEarliestTime(), lastEarliest +
                        sailingtime + operationtime);
                if (newTime == vesselroutes.get(routeIndex).get(f).getEarliestTime()) {
                    return 0;
                }
                lastEarliest = newTime;
            }
        }
        return earliestLastOperation-lastEarliest;
    }

     */

    public void sortOperationsProfitDecrease(String measurement){
        //want to remove the operations that causes the smallest profit decrease if it is removed
        Map<Integer,Integer> profitDecrease = new TreeMap<Integer,Integer>();
        //modelling choice per now: choose to not consider sync tasks - this is an approximation, discuss this next meeting
        for (int r=0;r< vesselRoutes.size();r++) {
            if(vesselRoutes.get(r)!=null && vesselRoutes.get(r).size()>0) {
                for (int i = 0; i < vesselRoutes.get(r).size(); i++) {
                    //System.out.println((g+1+startNodes.length)+" "+operationGain[0][g][0]);
                    //Key value (= operation number) in savingValues is not null indexed
                    OperationInRoute or = vesselRoutes.get(r).get(i);
                    int operationID = or.getID();
                    int sailingTimePrevToCurrent=0;
                    int sailingTimeCurrentToNext = 0;
                    int sailingTimePrevToNext = 0;
                    int profitDecreaseForOperation;
                    int deltaOperationGainLastOperation=0;
                    if (vesselRoutes.get(r).size() == 1) {
                        sailingTimePrevToCurrent = SailingTimes[r][EarliestStartingTimeForVessel[r]][r][operationID - 1];
                    }
                    else {
                        OperationInRoute prevOr = null;
                        if (i!=0){
                            prevOr = vesselRoutes.get(r).get(i - 1);
                            int earliestPrev = prevOr.getEarliestTime();
                            int operationTimePrev = TimeVesselUseOnOperation[r][prevOr.getID() - 1 - startNodes.length][earliestPrev - 1];
                            int startTimeSailingTimePrev = earliestPrev + operationTimePrev;
                            sailingTimePrevToCurrent = SailingTimes[r][startTimeSailingTimePrev - 1][prevOr.getID() - 1][operationID - 1];
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
/*
                        OperationInRoute lastOperation = vesselRoutes.get(r).get(vesselRoutes.get(r).size() - 1);
                        int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                        int changedTime;
                        if(prevOr!=null){
                            changedTime = checkChangeEarliestLastOperation(prevOr.getEarliestTime(), earliestTimeLastOperationInRoute,
                                    i-1, r, prevOr.getID(),vesselRoutes, TimeVesselUseOnOperation,startNodes,SailingTimes);
                        }
                        else{
                            changedTime = checkChangeEarliestLastOperation(0, earliestTimeLastOperationInRoute,
                                    i-1, r, 0,vesselRoutes, TimeVesselUseOnOperation,startNodes,SailingTimes);
                        }
                        deltaOperationGainLastOperation = 0;
                        if (changedTime > 0) {
                            deltaOperationGainLastOperation = operationGain[r][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - changedTime - 1]-
                                    operationGain[r][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1];
                            System.out.println("DELTA OPERATION GAIN "+deltaOperationGainLastOperation);
                        }

 */

                    }
                    int sailingDiff = -sailingTimePrevToCurrent - sailingTimeCurrentToNext + sailingTimePrevToNext;
                    profitDecreaseForOperation=0;
                    if(measurement.equals("sailing")){
                        profitDecreaseForOperation = sailingDiff * SailingCostForVessel[r]-deltaOperationGainLastOperation;
                    }
                    else if(measurement.equals("benefit")){
                        profitDecreaseForOperation = operationGain[r][operationID - startNodes.length - 1][or.getEarliestTime()-1] +
                                sailingDiff * SailingCostForVessel[r]-deltaOperationGainLastOperation;
                    }
                    profitDecrease.put(or.getID(), profitDecreaseForOperation);
                    routeObjectDict.put(or.getID(), new routeIndexObjectForOperation(r, or));
                }
            }
        }
        //System.out.println("Before sorting"+profitDecrease);
        SortedSet<Map.Entry<Integer,Integer>> sortedProfitDecrease=entriesSortedByValues(profitDecrease);
        //System.out.println("After sorting"+sortedProfitDecrease);
        //System.out.println("Print all keys + values");
        for (Map.Entry<Integer, Integer> entry  : sortedProfitDecrease) {
            //System.out.println("Key "+entry.getKey()+" : Value "+entry.getValue());
            sortedOperationsByProfitDecrease.add(entry.getKey());
        }
    }


    public void removeOperations(OperationInRoute selectedTask,int route, int index, String removalType){
        int selectedTaskID=selectedTask.getID();
        if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 || simALNS[selectedTaskID-startNodes.length-1][0] != 0
                || precedenceALNS[selectedTaskID-startNodes.length-1][1] != 0 || precedenceALNS[selectedTaskID-startNodes.length-1][0] != 0) {
            /*
            System.out.println("remove synchronized op "+selectedTaskID);
            System.out.println("SIMULTANEOUS DICTIONARY");
            for(Map.Entry<Integer, ConnectedValues> entry : simultaneousOp.entrySet()){
                ConnectedValues simOp = entry.getValue();
                System.out.println("Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                        simOp.getRoute() + " with index: " + simOp.getIndex() + " connected ID "+
                        simOp.getConnectedOperationObject().getID() + " connected route "+simOp.getConnectedRoute());
            }
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

             */
            removeSynchronizedOp(simultaneousOp.get(selectedTaskID),precedenceOverOperations.get(selectedTaskID),
                    precedenceOfOperations.get(selectedTaskID),selectedTaskID, selectedTask,removalType);
            removeDependentOperations(selectedTaskID,removalType);
        }
        //normal tasks
        else{
            //System.out.println("remove normal op "+selectedTaskID);
            //System.out.println("Remove normal task: "+selectedTask.getID());
            removeNormalOp(selectedTask, route, index, removalType);
        }
    }

    public void removeDependentOperations(int selectedTaskID, String removalType){
        if(simALNS[selectedTaskID-startNodes.length-1][0] != 0 ){
            int dependentOperation = simALNS[selectedTaskID - startNodes.length - 1][0];
            if(simultaneousOp.get(dependentOperation)!=null) {
                //System.out.println("Remove operation sim"+dependentOperation);
                removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                        precedenceOverOperations.get(dependentOperation),
                        precedenceOfOperations.get(dependentOperation), dependentOperation, simultaneousOp.get(dependentOperation).getOperationObject(),
                        removalType);
                if (precedenceALNS[dependentOperation - startNodes.length - 1][0] != 0) {
                    if(precedenceOverOperations.get(precedenceALNS[dependentOperation - startNodes.length - 1][0])!=null) {
                        removeDependentOperations(dependentOperation, removalType);
                    }
                }
            }
        }
        if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 ) {
            //System.out.println("REMOVE DEPENDENT OPERATION "+simALNS[selectedTaskID-startNodes.length-1][1]);
            int dependentOperation= simALNS[selectedTaskID-startNodes.length-1][1];
            if(simultaneousOp.get(dependentOperation)!=null) {
                //System.out.println("Remove operation sim"+dependentOperation);
                removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                        precedenceOverOperations.get(dependentOperation),
                        precedenceOfOperations.get(dependentOperation), dependentOperation, simultaneousOp.get(dependentOperation).getOperationObject(),
                        removalType);
                if (precedenceALNS[dependentOperation - startNodes.length - 1][0] != 0) {
                    if(precedenceOverOperations.get(precedenceALNS[dependentOperation - startNodes.length - 1][0])!=null) {
                        removeDependentOperations(dependentOperation, removalType);
                    }
                }
            }
        }
        if(precedenceALNS[selectedTaskID-startNodes.length-1][0] != 0 ) {
            int dependentOperation= precedenceALNS[selectedTaskID-startNodes.length-1][0];
            //System.out.println("dependent operation to remove"+dependentOperation);
            if(precedenceOfOperations.get(dependentOperation)!=null){
                //System.out.println("dependent op in pres of dict"+precedenceOfOperations.get(dependentOperation).getOperationObject().getID());
                removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                        precedenceOverOperations.get(dependentOperation),
                        precedenceOfOperations.get(dependentOperation),dependentOperation,
                        precedenceOfOperations.get(dependentOperation).getOperationObject(),removalType);
                if(precedenceALNS[dependentOperation-startNodes.length-1][0] !=0){
                    if(precedenceOverOperations.get(precedenceALNS[dependentOperation-startNodes.length-1][0])!=null) {
                        removeDependentOperations(dependentOperation, removalType);
                    }
                }
                if(simALNS[dependentOperation-startNodes.length-1][1] != 0){
                    if(simultaneousOp.get(simALNS[dependentOperation-startNodes.length-1][1])!=null){
                        removeDependentOperations(dependentOperation, removalType);
                    }
                }
                if(simALNS[dependentOperation-startNodes.length-1][0] != 0){
                    if(simultaneousOp.get(simALNS[dependentOperation-startNodes.length-1][0])!=null){
                        removeDependentOperations(dependentOperation, removalType);
                    }
                }
            }
        }
    }

    public void removeSynchronizedOp(ConnectedValues simOp, PrecedenceValues precedenceOverOp, PrecedenceValues precedenceOfOp,int selectedTaskID,
                                     OperationInRoute selectedTask, String removalType){
        removedOperations.add(selectedTaskID);
        if(removalType.equals("worstRemoval")){
            sortedOperationsByProfitDecrease.remove(Integer.valueOf(selectedTaskID));
        }
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
            //System.out.println("big "+bigTaskID);
            OperationInRoute bigOP=null;
            for(OperationInRoute oir: unroutedTasks){
                if (oir.getID()==bigTaskID){
                    bigOP=oir;
                }
            }
            if(!unroutedTasks.contains(bigOP)){
                //System.out.println(selectedTask.getID()+" added to unrouted");
                unroutedTasks.add(selectedTask);
                int big=bigTasksALNS[selectedTask.getID()-1-startNodes.length][0];
                //System.out.println(big +" added to unrouted");
                unroutedTasks.add(new OperationInRoute(bigTasksALNS[selectedTask.getID()-1-startNodes.length][0],0,0));
                /*
                System.out.println("big task entries "+bigTasksALNS[selectedTask.getID()-1-startNodes.length][0]+" "+bigTasksALNS[selectedTask.getID()-1-startNodes.length][1]
                +" "+bigTasksALNS[selectedTask.getID()-1-startNodes.length][2]);

                 */
                if(selectedTaskID==bigTasksALNS[selectedTask.getID()-1-startNodes.length][1]){
                    int otherSmall=bigTasksALNS[selectedTask.getID()-1-startNodes.length][2];
                    //System.out.println(otherSmall +" added to unrouted last entry");
                    unroutedTasks.add(new OperationInRoute(bigTasksALNS[selectedTask.getID()-1-startNodes.length][2],0,0));
                }
                else{
                    int otherSmall=bigTasksALNS[selectedTask.getID()-1-startNodes.length][1];
                    //System.out.println(otherSmall +" added to unrouted first entry");
                    unroutedTasks.add(new OperationInRoute(bigTasksALNS[selectedTask.getID()-1-startNodes.length][1],0,0));
                }
            }
        }
        //System.out.println("Operation to remove: "+selectedTaskID);
        //System.out.println("route "+route+" index "+index + "task "+selectedTaskID);
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
            int presOverOp=precedenceOfOp.getConnectedOperationObject().getID();
            if(precedenceOverOperations.get(presOverOp)!=null){
                precedenceOverOperations.get(presOverOp).setConnectedRoute(-1);
                precedenceOverOperations.get(presOverOp).setConnectedOperationObject(null);
            }
            precedenceOfOperations.remove(selectedTaskID);
            precedenceOfRoutes.get(route).remove(selectedTaskID);

        }
    }

    public void removeNormalOp(OperationInRoute selectedTask, int route, int index, String removalType){
        if (bigTasksALNS[selectedTask.getID() - 1 - startNodes.length] != null &&
                bigTasksALNS[selectedTask.getID() - startNodes.length - 1][0] == selectedTask.getID()) {
            consolidatedOperations.replace(bigTasksALNS[selectedTask.getID() - startNodes.length - 1][0],
                    new ConsolidatedValues(false, false, 0, 0, 0));
        }
        removedOperations.add(selectedTask.getID());
        if(bigTasksALNS[selectedTask.getID()-1-startNodes.length]==null){
            unroutedTasks.add(selectedTask);
        }
        else{
            unroutedTasks.add(selectedTask);
            unroutedTasks.add(new OperationInRoute(bigTasksALNS[selectedTask.getID()-1-startNodes.length][1],0,0));
            unroutedTasks.add(new OperationInRoute(bigTasksALNS[selectedTask.getID()-1-startNodes.length][2],0,0));
        }
        if(removalType.equals("worstRemoval")){
            sortedOperationsByProfitDecrease.remove(Integer.valueOf(selectedTask.getID()));
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
                            ConstructionHeuristic.updateEarliest(newESecondOr, indexConnected, routeConnectedOp, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes,"notLocal");
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
                        newLSecondOr = ConstructionHeuristic.weatherLatestTimeSimPostInsert(newLSecondOr, secondOr.getEarliestTime(),TimeVesselUseOnOperation,pValues.getConnectedRoute(),secondOr.getID(),
                                                                                            startNodes,SailingTimes,connectedOpPValues.getIndex(),vesselRoutes,simultaneousOp,precedenceOfOperations,precedenceOverOperations,
                                                                                            -1,0,twIntervals);
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
        for (int route=0;route<vesselRoutes.size();route++) {
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

        for (int route=0;route<vesselRoutes.size();route++) {
            if (vesselRoutes.get(route) != null && vesselRoutes.get(route).size() != 0) {
                updatePrecedenceOverAfterRemovals(precedenceOverRoutes.get(route));
                updatePrecedenceOfAfterRemovals(precedenceOfRoutes.get(route));
                ConstructionHeuristic.updateSimultaneous(simOpRoutes, route, 0, simultaneousOp, precedenceOverRoutes,
                        precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations, precedenceOfOperations,
                        vesselRoutes,twIntervals);
            }
        }
        /*
        System.out.println("After all updates");
        printLNSSolution(vesseltypes);

         */
    }

    //Insertion methods


    public void runLNSRemoval(String method){
        if(numberOfRemoval>simALNS.length-unroutedTasks.size()){
            System.out.println("Not possible, number of removal is larger than the number of tasks");
        }
        else{
            if(method.equals("random")){
                randomRemoval();
            }
            if(method.equals("synchronized")){
                synchronizedRemoval();
            }
            if(method.equals("route")){
                routeRemoval();
            }
            if(method.equals("worst")){
                worstRemoval();
            }
            if(method.equals("related")){
                relatedRemoval();
            }
            if(method.equals("worst_sailing")){
                worstRemovalSailing();
            }
            updateAllTimesAfterRemoval();

            ObjectiveValues ov = ConstructionHeuristic.calculateObjective(vesselRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,SailingCostForVessel,
                    EarliestStartingTimeForVessel, operationGainGurobi, new int[vesselRoutes.size()],new int[vesselRoutes.size()],0,simALNS,bigTasksALNS);
            objValue=ov.getObjvalue();
            routeSailingCost=ov.getRouteSailingCost();
            routeOperationGain=ov.getRouteBenefitGain();
            //System.out.println("After removal in removal class");
            //printLNSSolution(vesseltypes);
        }
    }

    public void printLNSSolution(int[] vessseltypes){
        //PrintData.timeVesselUseOnOperations(TimeVesselUseOnOperation,startNodes.length);
        //PrintData.printSailingTimes(SailingTimes,3,nOperations-2*startNodes.length,startNodes.length);
        //PrintData.printOperationsForVessel(OperationsForVessel);
        //PrintData.printPrecedenceALNS(precedenceALNS);
        //PrintData.printSimALNS(simALNS);
        //PrintData.printTimeWindows(timeWindowsForOperations);
        //PrintData.printTimeWindowsIntervals(twIntervals);

        System.out.println("SOLUTION AFTER LNS REMOVAL");

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
        System.out.println("REMOVED Operations");
        for(Integer rO: removedOperations){
            System.out.println(rO);
        }
        System.out.println(" ");
        System.out.println("SIMULTANEOUS DICTIONARY");
        for(Map.Entry<Integer, ConnectedValues> entry : simultaneousOp.entrySet()){
            ConnectedValues simOp = entry.getValue();
            System.out.println("Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                    simOp.getRoute() + " with index: " + simOp.getIndex() + " earliest "+simOp.getOperationObject().getEarliestTime()
                    + " earliest "+simOp.getOperationObject().getLatestTime()+ " connected ID "+simOp.getConnectedOperationObject().getID() + " connected route "+simOp.getConnectedRoute());

        }
        System.out.println("PRECEDENCE OVER DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOverOperations.entrySet()){
            PrecedenceValues presOverOp = entry.getValue();
            System.out.println("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                    presOverOp.getRoute() + " with index: " + presOverOp.getIndex());
        }
        /*
        System.out.println("Precedence over 2");
        for(int v= 0;v<vesselRoutes.size();v++){
            for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOverRoutes.get(v).entrySet()){
                PrecedenceValues simOp = entry.getValue();
                System.out.println("In vessel " + v+" Precedence over operation: " + simOp.getOperationObject().getID() + " in route: " +
                        simOp.getRoute() + " with index: " + simOp.getIndex());
            }
        }

         */
        System.out.println("PRECEDENCE OF DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfOperations.entrySet()){
            PrecedenceValues presOfOp = entry.getValue();
            System.out.println("Precedence of operation: " + presOfOp.getOperationObject().getID() + " in route: " +
                    presOfOp.getRoute() + " with index: " + presOfOp.getIndex());
        }
        /*
        System.out.println("Precedence of 2");
        for(int v= 0;v<vesselRoutes.size();v++){
            for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfRoutes.get(v).entrySet()){
                PrecedenceValues simOp = entry.getValue();
                System.out.println("In vesssel " + v+" Precedence of operation: " + simOp.getOperationObject().getID() + " in route: " +
                        simOp.getRoute() + " with index: " + simOp.getIndex());
            }
        }

         */
    }

    static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        int res = e1.getValue().compareTo(e2.getValue());
                        return res != 0 ? res : 1; // Special fix to preserve items with equal values
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
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

    public List<OperationInRoute> getUnroutedTasks() {
        return unroutedTasks;
    }

    public List<List<OperationInRoute>> getVesselRoutes() {
        return vesselRoutes;
    }

    public ArrayList<Integer> getRemovedOperations() {
        return removedOperations;
    }

    public int[] getRouteSailingCost() {
        return routeSailingCost;
    }

    public int[] getRouteOperationGain() {
        return routeOperationGain;
    }

    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes = new int[]{1,2,3,4,5,6,2};
        int[] startnodes=new int[]{1,2,3,4,5,6,7};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes ,
                "test_instances/30_locations_normalOpGenerator.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),startnodes.length);
        //PrintData.printSailingTimes(dg.getSailingTimes(),2,17, 4);
        //PrintData.printSailingTimes(dg.getSailingTimes(),3,23, 4);
        ConstructionHeuristic a = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(),dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations(),dg.getOperationGainGurobi(),dg.getWeatherPenaltyOperations());
        a.createSortedOperations();
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);
        LargeNeighboorhoodSearchRemoval LNS = new LargeNeighboorhoodSearchRemoval(a.getPrecedenceOverOperations(),a.getPrecedenceOfOperations(),
                a.getSimultaneousOp(),a.getSimOpRoutes(),a.getPrecedenceOfRoutes(),a.getPrecedenceOverRoutes(),
                a.getConsolidatedOperations(),a.getUnroutedTasks(),a.getVesselroutes(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(), dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),15,21,dg.getDistOperationsInInstance(),
                0.08,0.5,0.01,0.1,
                0.1,0.1,dg.getOperationGainGurobi(),vesseltypes);
        LNS.runLNSRemoval("worst_sailing");
        System.out.println("-----------------");
        LNS.printLNSSolution(vesseltypes);
        //PrintData.printSailingTimes(dg.getSailingTimes(),4,dg.getSimultaneousALNS().length,a.getVesselroutes().size());
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),dg.getStartNodes().length);
    }
}
