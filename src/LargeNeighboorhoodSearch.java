import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
    private int numberOfRemoval;
    private int [][] twIntervals;
    private int [][] precedenceALNS;
    private int [][] simALNS;
    private int [] startNodes;
    private int [][][][] SailingTimes;
    private int [][][] TimeVesselUseOnOperation;

    public LargeNeighboorhoodSearch(Map<Integer,PrecedenceValues> precedenceOverOperations, Map<Integer,PrecedenceValues> precedenceOfOperations,
                                    Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, ConnectedValues>> simOpRoutes,
                                    List<Map<Integer,PrecedenceValues>> precedenceOfRoutes, List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                                    Map<Integer, ConsolidatedValues> consolidatedOperations, List<OperationInRoute> unroutedTasks,
                                    List<List<OperationInRoute>> vesselRoutes, int [][]twIntervals,
                                    int[][] precedenceALNS,int[][] simALNS,int [] startNodes, int [][][][] SailingTimes,
                                    int [][][] TimeVesselUseOnOperation, int numberOfRemoval){
        this.precedenceOverOperations=precedenceOverOperations;
        this.precedenceOfOperations=precedenceOfOperations;
        this.simultaneousOp=simultaneousOp;
        this.simOpRoutes=simOpRoutes;
        this.precedenceOfRoutes=precedenceOfRoutes;
        this.precedenceOverRoutes=precedenceOverRoutes;
        this.unroutedTasks=unroutedTasks;
        this.vesselRoutes=vesselRoutes;
        this.consolidatedOperations=consolidatedOperations;
        this.numberOfRemoval=numberOfRemoval;
        this.twIntervals=twIntervals;
        this.precedenceALNS=precedenceALNS;
        this.simALNS=simALNS;
        this.startNodes=startNodes;
        this.SailingTimes=SailingTimes;
        this.TimeVesselUseOnOperation=TimeVesselUseOnOperation;

    }
    //removal methods

    public void randomRemoval(){
        for (int i=0;i<numberOfRemoval;i++){
            int randomRoute = ThreadLocalRandom.current().nextInt(0, vesselRoutes.size());
            int randomIndex = ThreadLocalRandom.current().nextInt(0, vesselRoutes.get(randomRoute).size());
            OperationInRoute selectedTask=vesselRoutes.get(randomRoute).get(randomIndex);
            removeOperation(selectedTask, randomRoute,randomIndex);
        }
    }

    public void removeOperation(OperationInRoute selectedTask, int route, int index){
        //synchronized tasks --> either simultaneous or precedence
        int selectedTaskID=selectedTask.getID();
        if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 || simALNS[selectedTaskID-startNodes.length-1][0] != 0
                || precedenceALNS[selectedTaskID-startNodes.length-1][1] != 0 || precedenceALNS[selectedTaskID-startNodes.length-1][0] != 0) {
            removeSynchronizedOp(simultaneousOp.get(selectedTaskID),precedenceOverOperations.get(selectedTaskID),precedenceOfOperations.get(selectedTaskID));
            removeDependentOperations(selectedTaskID);
        }
        //normal tasks
        else{
            removeNormalOp(selectedTask, route, index);
        }
    }

    public void removeDependentOperations(int selectedTaskID){
        if(simALNS[selectedTaskID-startNodes.length-1][0] != 0 ) {
            int dependentOperation= simALNS[selectedTaskID-startNodes.length-1][0];
            removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                    precedenceOverOperations.get(dependentOperation),
                    precedenceOfOperations.get(dependentOperation));
            if(precedenceALNS[dependentOperation-startNodes.length-1][0] != 0){
                removeDependentOperations(dependentOperation);
            }
        }
        if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 ) {
            int dependentOperation= simALNS[selectedTaskID-startNodes.length-1][1];
            removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                    precedenceOverOperations.get(dependentOperation),
                    precedenceOfOperations.get(dependentOperation));
            if(precedenceALNS[dependentOperation-startNodes.length-1][0] != 0){
                removeDependentOperations(dependentOperation);
            }
        }
        if(precedenceALNS[selectedTaskID-startNodes.length-1][0] != 0 ) {
            int dependentOperation= precedenceALNS[selectedTaskID-startNodes.length-1][0];
            removeSynchronizedOp(simultaneousOp.get(dependentOperation),
                    precedenceOverOperations.get(dependentOperation),
                    precedenceOfOperations.get(dependentOperation));
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

    public void removeSynchronizedOp(ConnectedValues simOp, PrecedenceValues precedenceOverOp, PrecedenceValues precedenceOfOp){
        int route;
        int index;
        if(simOp!=null){
            route=simOp.getRoute();
            index=simOp.getIndex();
        }
        else if(precedenceOverOp!=null){
            route=precedenceOverOp.getRoute();
            index=precedenceOverOp.getIndex();
        }
        else{
            route=precedenceOverOp.getRoute();
            index=simOp.getIndex();
        }
        int prevEarliest=findPrevEarliest(route,index);
        unroutedTasks.add(simOp.getOperationObject());
        vesselRoutes.get(route).remove(index);
        if(simOp!=null){
            simultaneousOp.remove(simOp.getOperationObject().getID());
            simOpRoutes.get(simOp.getRoute()).remove(simOp.getOperationObject().getID());
        }
        if(precedenceOverOp!=null){
            precedenceOverOperations.remove(precedenceOverOp.getOperationObject().getID());
            precedenceOverRoutes.get(route).remove(precedenceOverOp.getOperationObject().getID());
        }
        if(precedenceOfOp!=null){
            precedenceOfOperations.remove(precedenceOfOp.getOperationObject().getID());
            precedenceOfRoutes.get(route).remove(precedenceOfOp.getOperationObject().getID());
        }
        int nextLatest = findNextLatest(route,index);
        if (vesselRoutes.get(route).size() > index) {
            nextLatest = vesselRoutes.get(route).get(index).getLatestTime();
        }
        if (index == vesselRoutes.get(route).size()) {
            OperationInRoute lastOp = vesselRoutes.get(route).get(vesselRoutes.get(route).size() - 1);
            nextLatest = twIntervals[lastOp.getID() - startNodes.length - 1][1];
            lastOp.setLatestTime(nextLatest);
        }
        updateDependencies(prevEarliest, index, route, nextLatest);
    }

    public void removeNormalOp(OperationInRoute selectedTask, int route, int index){
        int prevEarliest=findPrevEarliest(route, index);
        unroutedTasks.add(selectedTask);
        vesselRoutes.get(route).remove(index);
        int nextLatest = findNextLatest(route,index);
        updateDependencies(prevEarliest, index, route, nextLatest);
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
                simultaneousOp, vesselRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes);
    }

    public int findNextLatest(int route, int index){
        int nextLatest = 0;
        if (vesselRoutes.get(route).size() > index) {
            nextLatest = vesselRoutes.get(route).get(index).getLatestTime();
        }
        if (index == vesselRoutes.get(route).size()) {
            OperationInRoute lastOp = vesselRoutes.get(route).get(vesselRoutes.get(route).size() - 1);
            nextLatest = twIntervals[lastOp.getID() - startNodes.length - 1][1];
            lastOp.setLatestTime(nextLatest);
        }
        return nextLatest;
    }

    public int findPrevEarliest(int route, int index){
        int prevEarliest=0;
        if(index - 1!=-1){
            prevEarliest = vesselRoutes.get(route).get(index - 1).getEarliestTime();
        }
        if(index - 1==-1){
            OperationInRoute firstOp = vesselRoutes.get(route).get(0);
            if(twIntervals[firstOp.getID()-startNodes.length-1][0]==0){
                prevEarliest = 1;
            }
            else{
                prevEarliest=twIntervals[firstOp.getID()-startNodes.length-1][0];
            }
            firstOp.setEarliestTime(prevEarliest);
        }
        return prevEarliest;
    }

    /*

    public void printLSNSolution(int[] vessseltypes){
        //PrintData.timeVesselUseOnOperations(TimeVesselUseOnOperation,startNodes.length);
        //PrintData.printSailingTimes(SailingTimes,3,nOperations-2*startNodes.length,startNodes.length);
        //PrintData.printOperationsForVessel(OperationsForVessel);
        //PrintData.printPrecedenceALNS(precedenceALNS);
        //PrintData.printSimALNS(simALNS);
        //PrintData.printTimeWindows(timeWindowsForOperations);
        //PrintData.printTimeWindowsIntervals(twIntervals);

        System.out.println("Sailing cost per route: "+ Arrays.toString(routeSailingCost));
        //System.out.println("Operation gain per route: "+Arrays.toString(routeOperationGain));
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
    }
    
     */

    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes = new int[]{1,2,3,4};
        int[] startnodes=new int[]{1,2,3,4};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes ,
                "test_instances/test_instance_15_locations_PRECEDENCEtest.txt",
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
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),10);


    }

}
