import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;

public class LargeNeighboorhoodSearch {
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
    private int[] routeSailingCost;
    private int[] routeOperationGain;
    private ArrayList<Integer> sortedOperationsByProfitDecrease;
    private int objValue;
    private ArrayList<Integer> removedOperations = new ArrayList<>();
    Random generator = new Random(35);

    public LargeNeighboorhoodSearch(Map<Integer,PrecedenceValues> precedenceOverOperations, Map<Integer,PrecedenceValues> precedenceOfOperations,
                                        Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, ConnectedValues>> simOpRoutes,
                                        List<Map<Integer,PrecedenceValues>> precedenceOfRoutes, List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                                        Map<Integer, ConsolidatedValues> consolidatedOperations, List<OperationInRoute> unroutedTasks,
                                        List<List<OperationInRoute>> vesselRoutes, int [][]twIntervals,
                                        int[][] precedenceALNS,int[][] simALNS,int [] startNodes, int [][][][] SailingTimes,
                                        int [][][] TimeVesselUseOnOperation, int[] SailingCostForVessel,int[] EarliestStartingTimeForVessel,
                                        int[][][] operationGain, int[][] bigTasksALNS, int numberOfRemoval){
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
        this.numberOfRemoval=numberOfRemoval;
        this.routeSailingCost=new int[vesselRoutes.size()];
        this.routeOperationGain=new int[vesselRoutes.size()];
        this.objValue=0;
        this.sortedOperationsByProfitDecrease=new ArrayList<>();
        this.bigTasksALNS=bigTasksALNS;
    }
    //removal methods

    public void randomRemoval(){
        while (removedOperations.size()<numberOfRemoval){
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
            removeOperations(selectedTask,randomRoute,randomIndex);
        }
    }

    public void sortOperationsProfitDecrease(){
        SortedSet<KeyValuePair> profitDecrease = new TreeSet<KeyValuePair>();
        ArrayList<Integer> takenSyncOperations=new ArrayList<>();
        //modelling choice per now: choose to not consider sync tasks - this is an approximation, discuss this next meeting
        for (int r=0;r< vesselRoutes.size();r++) {
            for(int i=0;i<vesselRoutes.get(r).size();i++){
                //System.out.println((g+1+startNodes.length)+" "+operationGain[0][g][0]);
                //Key value (= operation number) in savingValues is not null indexed
                OperationInRoute or=vesselRoutes.get(r).get(i);
                int operationID = or.getID();
                int sailingCostPrevToCurrent=0;
                int sailingCostCurrentToNext=0;
                int sailingCostPrevToNext=0;
                int profitDecreaseForOperation=0;
                if(vesselRoutes.get(r).size()==1){
                    int sailingTimeStartNodeToO=SailingTimes[r][EarliestStartingTimeForVessel[r]][r][operationID - 1];
                    profitDecreaseForOperation+=sailingTimeStartNodeToO*SailingCostForVessel[r];
                }
                else if (i==0){
                    int sailingTimeStartNodeToO=SailingTimes[r][EarliestStartingTimeForVessel[r]][r][operationID - 1];
                    profitDecreaseForOperation+=sailingTimeStartNodeToO*SailingCostForVessel[r];
                }
                else if(i==vesselRoutes.get(r).size()){

                }
                else{

                }
                /*
                if(simultaneousOp.get(operationID)!=null){

                }
                else if(precedenceOverOperations.get(operationID)==null){

                }
                else if(precedenceOfOperations.get(operationID)==null){

                }
                else{

                }
                 */
                profitDecrease.add(new KeyValuePair(operationID+1+startNodes.length,profitDecreaseForOperation));
            }
        }
        /*
        int index=0;
        for (KeyValuePair keyValuePair : penaltiesDict) {
            if(!DataGenerator.containsElement(keyValuePair.key,sortedOperations)){
                sortedOperations[index] = keyValuePair.key;
            }
            if(bigTasksALNS[keyValuePair.key-startNodes.length-1]!= null && bigTasksALNS[keyValuePair.key- startNodes.length-1][0]==keyValuePair.key){
                for (int i=1;i<bigTasksALNS[keyValuePair.key-startNodes.length-1].length;i++){
                    index+=1;
                    sortedOperations[index]=bigTasksALNS[keyValuePair.key-startNodes.length-1][i];
                }
            }
            index+=1;
        }
        System.out.println("BIG TASK ALNS LIST");
        PrintData.printBigTasksALNS(bigTasksALNS,nOperations);
        System.out.println("Sorted by operation gain: ");
        for(Integer op : sortedOperations){
            System.out.println("Operation "+op+" Gain: "+operationGain[0][op-1-startNodes.length][0]);
        }
        System.out.println("Sorted tasks: "+Arrays.toString(sortedOperations));

         */
    }

    public void worstRemoval(){
        while (removedOperations.size()<numberOfRemoval){
            int selectedTaskID=sortedOperationsByProfitDecrease.get(0);
            sortedOperationsByProfitDecrease.remove(0);
            //removeOperation()
        }
    }

    public void relatedRemoval(){

    }

    public void synchronizedRemoval(){
        for(int i=0;i<precedenceALNS.length;i++){
            PrintData.printPrecedenceALNS(precedenceALNS);
            if (precedenceALNS[i][0]!=0){
                System.out.println("REMOVE PRECEDENCE");
                int selectedTaskID=i+startNodes.length+1;
                if(precedenceOverOperations.get(selectedTaskID)!=null){
                    PrecedenceValues pv= precedenceOverOperations.get(selectedTaskID);
                    removeSynchronizedOp(simultaneousOp.get(selectedTaskID), precedenceOverOperations.get(selectedTaskID),
                            precedenceOfOperations.get(selectedTaskID), selectedTaskID, pv.getOperationObject());
                    removeDependentOperations(selectedTaskID);
                }
            }
        }
        for(int i=0;i<simALNS.length;i++){
            if (simALNS[i][0]!=0){
                System.out.println("REMOVE SIMULTANEOUS");
                int selectedTaskID=i+startNodes.length+1;
                if(simultaneousOp.get(selectedTaskID)!=null){
                    ConnectedValues cv= simultaneousOp.get(selectedTaskID);
                    removeSynchronizedOp(simultaneousOp.get(selectedTaskID), precedenceOverOperations.get(selectedTaskID),
                            precedenceOfOperations.get(selectedTaskID), selectedTaskID, cv.getOperationObject());
                    removeDependentOperations(selectedTaskID);
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
            System.out.println("REMOVE ROUTE: "+randomRoute);
            int sizeOfRoute=vesselRoutes.get(randomRoute).size();
            for(int n=0;n<sizeOfRoute;n++){
                OperationInRoute or=vesselRoutes.get(randomRoute).get(0);
                removeOperations(or,randomRoute,0);
            }
        }
    }

    public void removeOperations(OperationInRoute selectedTask,int route, int index){
        int selectedTaskID=selectedTask.getID();
        if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 || simALNS[selectedTaskID-startNodes.length-1][0] != 0
                || precedenceALNS[selectedTaskID-startNodes.length-1][1] != 0 || precedenceALNS[selectedTaskID-startNodes.length-1][0] != 0) {
            //System.out.println("Remove synchronized task: "+selectedTask.getID());
            removeSynchronizedOp(simultaneousOp.get(selectedTaskID),precedenceOverOperations.get(selectedTaskID),
                    precedenceOfOperations.get(selectedTaskID),selectedTaskID, selectedTask);
            removeDependentOperations(selectedTaskID);
        }
        //normal tasks
        else{
            //System.out.println("Remove normal task: "+selectedTask.getID());
            removeNormalOp(selectedTask, route, index);
        }
    }

    public void removeDependentOperations(int selectedTaskID){
        if(simALNS[selectedTaskID-startNodes.length-1][0] != 0 ){
            int dependentOperation = simALNS[selectedTaskID - startNodes.length - 1][0];
            if(simultaneousOp.get(dependentOperation)!=null) {
                removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                        precedenceOverOperations.get(dependentOperation),
                        precedenceOfOperations.get(dependentOperation), dependentOperation, simultaneousOp.get(dependentOperation).getOperationObject());
                if (precedenceALNS[dependentOperation - startNodes.length - 1][0] != 0) {
                    removeDependentOperations(dependentOperation);
                }
            }
        }
        if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 ) {
            int dependentOperation= simALNS[selectedTaskID-startNodes.length-1][1];
            if(simultaneousOp.get(dependentOperation)!=null) {
                removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                        precedenceOverOperations.get(dependentOperation),
                        precedenceOfOperations.get(dependentOperation), dependentOperation, simultaneousOp.get(dependentOperation).getOperationObject());
                if (precedenceALNS[dependentOperation - startNodes.length - 1][0] != 0) {
                    removeDependentOperations(dependentOperation);
                }
            }
        }
        if(precedenceALNS[selectedTaskID-startNodes.length-1][0] != 0 ) {
            int dependentOperation= precedenceALNS[selectedTaskID-startNodes.length-1][0];
            if(precedenceOfOperations.get(dependentOperation)!=null){
                removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                        precedenceOverOperations.get(dependentOperation),
                        precedenceOfOperations.get(dependentOperation),dependentOperation,
                        precedenceOfOperations.get(dependentOperation).getOperationObject());
                if(precedenceALNS[dependentOperation-startNodes.length-1][0] != 0){
                    removeDependentOperations(dependentOperation);
                }
            }
        }
    }

    public void removeSynchronizedOp(ConnectedValues simOp, PrecedenceValues precedenceOverOp, PrecedenceValues precedenceOfOp,int selectedTaskID,
                                     OperationInRoute selectedTask){
        removedOperations.add(selectedTaskID);
        int route=0;
        int index=0;
        if(simOp!=null){
            System.out.println("Simultaneous, operation: "+selectedTask.getID());
            route=simOp.getRoute();
            index=simOp.getIndex();
        }
        else if(precedenceOverOp!=null){
            System.out.println("Precedence over, operation: "+selectedTask.getID());
            route=precedenceOverOp.getRoute();
            index=precedenceOverOp.getIndex();
        }
        else if(precedenceOfOp!=null){
            System.out.println("Precedence of, operation: "+selectedTask.getID());
            route=precedenceOfOp.getRoute();
            index=precedenceOfOp.getIndex();
        }
        unroutedTasks.add(selectedTask);
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
        vesselRoutes.get(route).remove(index);
        ConstructionHeuristic.updateIndexesRemoval(route, index, vesselRoutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
        System.out.println("Operation to remove: "+selectedTaskID);
        if(simOp!=null){
            simultaneousOp.remove(selectedTaskID);
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
            precedenceOverOperations.remove(selectedTaskID);
            precedenceOverRoutes.get(route).remove(selectedTaskID);
        }
        if(precedenceOfOp!=null){
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
        removedOperations.add(selectedTask.getID());
        unroutedTasks.add(selectedTask);
        System.out.println("REMOVE NORMAL OP: "+selectedTask.getID());
        vesselRoutes.get(route).remove(index);
        ConstructionHeuristic.updateIndexesRemoval(route, index, vesselRoutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
    }

    public void updateAllTimesAfterRemoval(){
        System.out.println("UPDATE TIMES AFTER ALL REMOVALS");
        for(int r=0;r<vesselRoutes.size();r++) {
            System.out.println("Updating route: " + r);
            int earliest = Math.max(SailingTimes[r][EarliestStartingTimeForVessel[r]][startNodes[r] - 1][vesselRoutes.get(r).get(0).getID() - 1] + 1,
                    twIntervals[vesselRoutes.get(r).get(0).getID() - 1-startNodes.length][0]);
            int latest = Math.min(SailingTimes[0].length,twIntervals[vesselRoutes.get(r).get(vesselRoutes.get(r).size()-1).getID()-1-startNodes.length][1]);
            vesselRoutes.get(r).get(0).setEarliestTime(earliest);
            vesselRoutes.get(r).get(vesselRoutes.get(r).size() - 1).setLatestTime(latest);
            ConstructionHeuristic.updateEarliestAfterRemoval(earliest, 0, r, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes, twIntervals);
            ConstructionHeuristic.updateLatestAfterRemoval(latest, vesselRoutes.get(r).size() - 1, r, vesselRoutes, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, twIntervals);
        }
    }

    public void runLNS(){
        if(numberOfRemoval>simALNS.length){
            System.out.println("Not possible, number of removal is larger than the number of tasks");
        }
        else{
            //randomRemoval();
            synchronizedRemoval();
            //routeRemoval();
            ConstructionHeuristic.calculateObjective(vesselRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,SailingCostForVessel,
                    EarliestStartingTimeForVessel, operationGain, routeSailingCost,routeOperationGain,objValue);
            updateAllTimesAfterRemoval();
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

        System.out.println("SOLUTION AFTER LNS");

        System.out.println("Sailing cost per route: "+ Arrays.toString(routeSailingCost));
        System.out.println("Operation gain per route: "+Arrays.toString(routeOperationGain));
        System.out.println("Objective value: "+objValue);
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
        a.printInitialSolution(vesseltypes);
        LargeNeighboorhoodSearch LNS = new LargeNeighboorhoodSearch(a.getPrecedenceOverOperations(),a.getPrecedenceOfOperations(),
                a.getSimultaneousOp(),a.getSimOpRoutes(),a.getPrecedenceOfRoutes(),a.getPrecedenceOverRoutes(),
                a.getConsolidatedOperations(),a.getUnroutedTasks(),a.getVesselroutes(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(), dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),5);
        LNS.runLNS();
        System.out.println("-----------------");
        LNS.printLNSSolution(vesseltypes);
    }
}
