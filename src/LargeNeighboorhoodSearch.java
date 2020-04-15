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
    private int [] startNodes;
    private int [][][][] SailingTimes;
    private int [][][] TimeVesselUseOnOperation;
    private int numberOfRemoval;
    private int[] SailingCostForVessel;
    private int[] EarliestStartingTimeForVessel;
    private int[][][] operationGain;
    private int[] routeSailingCost;
    private int[] routeOperationGain;
    private int objValue;
    private ArrayList<Integer> removedOperations = new ArrayList<>();
    Random generator = new Random(12);

    public LargeNeighboorhoodSearch(Map<Integer,PrecedenceValues> precedenceOverOperations, Map<Integer,PrecedenceValues> precedenceOfOperations,
                                    Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, ConnectedValues>> simOpRoutes,
                                    List<Map<Integer,PrecedenceValues>> precedenceOfRoutes, List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                                    Map<Integer, ConsolidatedValues> consolidatedOperations, List<OperationInRoute> unroutedTasks,
                                    List<List<OperationInRoute>> vesselRoutes, int [][]twIntervals,
                                    int[][] precedenceALNS,int[][] simALNS,int [] startNodes, int [][][][] SailingTimes,
                                    int [][][] TimeVesselUseOnOperation, int[] SailingCostForVessel,int[] EarliestStartingTimeForVessel,
                                    int[][][] operationGain,int numberOfRemoval){
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
            OperationInRoute selectedTask=vesselRoutes.get(randomRoute).get(randomIndex);
            removeOperation(selectedTask, randomRoute,randomIndex);
        }
    }

    public void removeOperation(OperationInRoute selectedTask, int route, int index){
        //synchronized tasks --> either simultaneous or precedence
        int selectedTaskID=selectedTask.getID();
        if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 || simALNS[selectedTaskID-startNodes.length-1][0] != 0
                || precedenceALNS[selectedTaskID-startNodes.length-1][1] != 0 || precedenceALNS[selectedTaskID-startNodes.length-1][0] != 0) {
            System.out.println("Remove synchronized task: "+selectedTask.getID());
            removeSynchronizedOp(simultaneousOp.get(selectedTaskID),precedenceOverOperations.get(selectedTaskID),
                    precedenceOfOperations.get(selectedTaskID),selectedTaskID, selectedTask);
            removeDependentOperations(selectedTaskID);
        }
        //normal tasks
        else{
            System.out.println("Remove normal task: "+selectedTask.getID());
            removeNormalOp(selectedTask, route, index);
        }
    }

    public void removeDependentOperations(int selectedTaskID){
        if(simALNS[selectedTaskID-startNodes.length-1][0] != 0 ) {
            int dependentOperation= simALNS[selectedTaskID-startNodes.length-1][0];
            removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                    precedenceOverOperations.get(dependentOperation),
                    precedenceOfOperations.get(dependentOperation),dependentOperation, simultaneousOp.get(dependentOperation).getOperationObject());
            if(precedenceALNS[dependentOperation-startNodes.length-1][0] != 0){
                removeDependentOperations(dependentOperation);
            }
        }
        if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 ) {
            int dependentOperation= simALNS[selectedTaskID-startNodes.length-1][1];
            removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                    precedenceOverOperations.get(dependentOperation),
                    precedenceOfOperations.get(dependentOperation),dependentOperation, simultaneousOp.get(dependentOperation).getOperationObject());
            if(precedenceALNS[dependentOperation-startNodes.length-1][0] != 0){
                removeDependentOperations(dependentOperation);
            }
        }
        if(precedenceALNS[selectedTaskID-startNodes.length-1][0] != 0 ) {
            int dependentOperation= precedenceALNS[selectedTaskID-startNodes.length-1][0];
            removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                    precedenceOverOperations.get(dependentOperation),
                    precedenceOfOperations.get(dependentOperation),dependentOperation, precedenceOverOperations.get(dependentOperation).getOperationObject());
            if(precedenceALNS[dependentOperation-startNodes.length-1][0] != 0){
                removeDependentOperations(dependentOperation);
            }
        }
    }

    public void worstRemoval(){

    }

    public void randomRemovalWRandomness(){

    }

    public void worstRemovalWRandomness(){

    }

    public void synchronizedRemoval(){

    }

    public void routeRemoval(){

    }

    public void removeSynchronizedOp(ConnectedValues simOp, PrecedenceValues precedenceOverOp, PrecedenceValues precedenceOfOp,int selectedTaskID,
                                     OperationInRoute selectedTask){
        removedOperations.add(selectedTaskID);
        int route=0;
        int index=0;
        if(simOp!=null){
            route=simOp.getRoute();
            index=simOp.getIndex();
        }
        else if(precedenceOverOp!=null){
            route=precedenceOverOp.getRoute();
            index=precedenceOverOp.getIndex();
        }
        else if(precedenceOfOp!=null){
            route=precedenceOfOp.getRoute();
            index=precedenceOfOp.getIndex();
        }
        int prevEarliest=findPrevEarliest(route,index);
        unroutedTasks.add(selectedTask);
        vesselRoutes.get(route).remove(index);
        if(simOp!=null){
            simultaneousOp.remove(selectedTaskID);
            simOpRoutes.get(route).remove(selectedTaskID);
        }
        if(precedenceOverOp!=null){
            precedenceOverOperations.remove(selectedTaskID);
            precedenceOverRoutes.get(route).remove(selectedTaskID);
        }
        if(precedenceOfOp!=null){
            precedenceOfOperations.remove(selectedTaskID);
            precedenceOfRoutes.get(route).remove(selectedTaskID);
        }
        int nextLatest = findNextLatest(route,index);
        if (nextLatest!=-1 && prevEarliest!=-1) {
            updateDependencies(prevEarliest, index, route, nextLatest);
        }
    }

    public void removeNormalOp(OperationInRoute selectedTask, int route, int index){
        removedOperations.add(selectedTask.getID());
        int prevEarliest=findPrevEarliest(route, index);
        unroutedTasks.add(selectedTask);
        vesselRoutes.get(route).remove(index);
        int nextLatest = findNextLatest(route,index);
        if (nextLatest!=-1 && prevEarliest!=-1) {
            updateDependencies(prevEarliest, index, route, nextLatest);
        }
    }

    public void updateDependencies(int prevEarliest, int index, int route, int nextLatest){
        ConstructionHeuristic.updateEarliest(prevEarliest, Math.max(index - 1,0), route, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes);
        ConstructionHeuristic.updateLatestAfterRemoval(nextLatest, Math.min(index, vesselRoutes.get(route).size() - 1), route,vesselRoutes, TimeVesselUseOnOperation, startNodes,
                SailingTimes, twIntervals);
        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(route), index,simOpRoutes,precedenceOfOperations,precedenceOverOperations,TimeVesselUseOnOperation,
                startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselRoutes,SailingTimes);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOverRoutes.get(route), index,TimeVesselUseOnOperation,startNodes,simOpRoutes,
                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselRoutes,simultaneousOp,SailingTimes);
        ConstructionHeuristic.updateSimultaneousAfterRemoval(simOpRoutes.get(route), route, index- 1,
                simultaneousOp, vesselRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes,twIntervals,EarliestStartingTimeForVessel);
    }

    public int findNextLatest(int route, int index){
        int nextLatest = 0;
        if (vesselRoutes.get(route).size() > index) {
            nextLatest = vesselRoutes.get(route).get(index).getLatestTime();
        }
        if (index == vesselRoutes.get(route).size()) {
            if(vesselRoutes.get(route).size()==0){
                return -1;
            }
            OperationInRoute lastOp = vesselRoutes.get(route).get(vesselRoutes.get(route).size() - 1);
            nextLatest = twIntervals[lastOp.getID() - startNodes.length - 1][1];
            lastOp.setLatestTime(nextLatest);
        }
        return nextLatest;
    }

    public int findPrevEarliest(int route, int index){
        int prevEarliest=0;
        if(index>0){
            prevEarliest = vesselRoutes.get(route).get(index - 1).getEarliestTime();
        }
        if(index==0){
            if(vesselRoutes.get(route).size()==1){
                return -1;
            }
            OperationInRoute firstOp = vesselRoutes.get(route).get(1);
            int sailingTimeStartNodeToO=SailingTimes[route][EarliestStartingTimeForVessel[route]][route][firstOp.getID() - 1];
            prevEarliest=Math.max(EarliestStartingTimeForVessel[route]+sailingTimeStartNodeToO+1,twIntervals[firstOp.getID()-startNodes.length-1][0]);
            firstOp.setEarliestTime(prevEarliest);
        }
        return prevEarliest;
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

    public void runLNS(){
        randomRemoval();
        ConstructionHeuristic.calculateObjective(vesselRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,SailingCostForVessel,
                EarliestStartingTimeForVessel, operationGain, routeSailingCost,routeOperationGain,objValue);
    }

    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes = new int[]{1,2,3,4};
        int[] startnodes=new int[]{1,2,3,4};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes ,
                "test_instances/test_instance_15_locations_PRECEDENCEtest4.txt",
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
                dg.getOperationGain(),10);
        LNS.runLNS();
        System.out.println("-----------------");
        LNS.printLNSSolution(vesseltypes);
    }
}
