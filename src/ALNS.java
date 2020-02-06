import java.io.FileNotFoundException;
import java.util.*;

public class ALNS {

    private int [][] OperationsForVessel;
    private int [][] TimeWindowsForOperations;
    private int [][][] Edges;
    private int [][][][] SailingTimes;
    private int [][][] TimeVesselUseOnOperation;
    private int [] EarliestStartingTimeForVessel;
    private int [] SailingCostForVessel;
    private int [] Penalty;
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

    public ALNS(int [][] OperationsForVessel, int [][] TimeWindowsForOperations, int [][][] Edges, int [][][][] SailingTimes,
                int [][][] TimeVesselUseOnOperation, int [] EarliestStartingTimeForVessel,
                int [] SailingCostForVessel, int [] Penalty, int [][] Precedence, int [][] Simultaneous,
                int [] BigTasks, Map<Integer, List<Integer>> ConsolidatedTasks, int[] endNodes, int[] startNodes, double[] endPenaltyforVessel){
        this.OperationsForVessel=OperationsForVessel;
        this.TimeWindowsForOperations=TimeWindowsForOperations;
        this.Edges=Edges;
        this.SailingTimes=SailingTimes;
        this.TimeVesselUseOnOperation=TimeVesselUseOnOperation;
        this.EarliestStartingTimeForVessel=EarliestStartingTimeForVessel;
        this.SailingCostForVessel=SailingCostForVessel;
        this.Penalty=Penalty;
        this.Precedence=Precedence;
        this.Simultaneous=Simultaneous;
        this.BigTasks=BigTasks;
        this.ConsolidatedTasks=ConsolidatedTasks;
        this.nVessels=this.OperationsForVessel.length;
        this.nOperations=TimeWindowsForOperations.length;
        this.nTimePeriods=TimeWindowsForOperations[0].length;
        this.endNodes=endNodes;
        this.startNodes=startNodes;
        this.endPenaltyforVessel=endPenaltyforVessel;
        System.out.println(nOperations-startNodes.length*2);
        System.out.println(Arrays.toString(this.endNodes));
        System.out.println(Arrays.toString(this.startNodes));
    }

    public void constructionHeuristic(){
        List<Integer> currentTimeVessel= new ArrayList<Integer>();
        for (Integer earliestStartTime : EarliestStartingTimeForVessel){
            currentTimeVessel.add(earliestStartTime);
        }
        List<Integer> allOperations = new ArrayList<Integer>();
        for (int n = 0; n < nVessels; n++) {
            vesselroutes.add(null);
        }
        for(int o=this.startNodes.length;o<this.nOperations-this.endNodes.length;o++){
            allOperations.add(o);
        }
        for (int n = 0; n < nVessels; n++) {
            //Look up list contains all sailing times fom a vessel's current location to all other nodes
            int[] lookUpList = this.SailingTimes[n][currentTimeVessel.get(n)][startNodes[n]];
            boolean continueAdd = true;
            while(continueAdd) {
                int min = 100000;
                int index = 0;
                int timeOpStart=0;
                int timeOpEnd=0;
                continueAdd=false;
                for (int i = nVessels; i < lookUpList.length - nVessels; i++) {
                    if (containsElement(i, OperationsForVessel[n]) && allOperations.contains(i)) {
                        timeOpStart=currentTimeVessel.get(n) +lookUpList[i];
                        if(timeOpStart>=nTimePeriods){
                            continue;
                        }
                        timeOpEnd= timeOpStart+TimeVesselUseOnOperation[n][i-nVessels][timeOpStart];
                        if (lookUpList[i] < min && timeOpEnd <= nTimePeriods) {
                            min = lookUpList[i];
                            index = i;
                            continueAdd=true;
                        }
                    }
                }
                if(continueAdd) {
                    allOperations.remove(Integer.valueOf(index));
                    if(vesselroutes.get(n) == null){
                        final int indexCopy =index;
                        final int timeOpStartCopy=timeOpStart;
                        vesselroutes.set(n, new ArrayList<>(){{add(new OperationInRoute(indexCopy, timeOpStartCopy));}});
                    }
                    else{
                        vesselroutes.get(n).add(new OperationInRoute(index, timeOpStart));
                    }
                    currentTimeVessel.set(n, timeOpEnd);
                    lookUpList = this.SailingTimes[n][currentTimeVessel.get(n)][index];
                }
            }
        }
        for(Integer tasksLeft : allOperations){
            unroutedTasks.add(new OperationInRoute(tasksLeft,0));
        }

    }

    public void printInitialSolution(){
        for (int i=0;i<vesselroutes.size();i++){
            System.out.println("VESSEL "+i);
            if (vesselroutes.get(i)!=null) {
                for (OperationInRoute opInRoute : vesselroutes.get(i)) {
                    System.out.println("Operation number: "+opInRoute.getID() +" Time: "+opInRoute.getTimeperiod());
                }
            }
        }
        if(!unroutedTasks.isEmpty()){
            System.out.println("UNROUTED TASKS");
            for(int n=0;n<unroutedTasks.size();n++) {
                System.out.println(unroutedTasks.get(n).getID());
            }
        }
    }

    /*
    Important to note:
    1. Sailingtimes[v][i][j][t] is changed to Sailingtimes[v][t][i][j]
    2. OperationInRoute objects are initialized with an ID equal to the index of the operation in the list, hence if we have two start nodes,
    the first node in the list after the start nodes has index and ID 2
    3. The current times of vessels are updated with the time they finish their previous operation, i.e when they are
    available for sailing to a new operation. Meanwhile OperationInRoute objects get initialized with their start time.
    */

    public static Boolean containsElement(int element, int[] list)   {
        Boolean bol = false;
        for (Integer e: list)     {
            if(element == e){
                bol=true;
            }
        }
        return bol;
    }

    public static void main(String[] args) throws FileNotFoundException {
        DataGenerator dg = new DataGenerator(new int[]{1,2,4,5}, 5, new int[]{1,2,3,4},
                "test_instances/test_instance_15_locations_first_test.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        ALNS a = new ALNS(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getPenalty(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel());
        a.constructionHeuristic();
        a.printInitialSolution();
    }
}



/*
1. Initialize route for each vessel
2. Iteratively assign tasks to each vessel, find the task that is closest, update the location of the vessel
 - If time window, fix the time
 - If synchronization, fix the twin visit to the same time
*/

/* OTHER ALTERNATIVE TO HAVING A POOL OF UNPERFORMED TASKS
while(!allOperations.isEmpty()) {
    for (int n = 0; n < nVessels; n++) {
        int timeOpStart = 0;
        int timeOpEnd = 0;
        for (int i = nVessels; i < this.nOperations - nVessels; i++) {
            if (containsElement(i, OperationsForVessel[n]) && allOperations.contains(i)) {
                timeOpStart = currentTimeVessel.get(n) + SailingTimes[n][currentTimeVessel.get(n)]
                        [vesselroutes.get(n).get(vesselroutes.get(n).size()).getID()][i];
                timeOpEnd = timeOpStart + TimeVesselUseOnOperation[n][i][currentTimeVessel.get(n) +
                        SailingTimes[n][currentTimeVessel.get(n)][vesselroutes.get(n).get(vesselroutes.get(n).size()).getID()][i]];
                allOperations.remove(Integer.valueOf(i));
                vesselroutes.get(n).add(new OperationInRoute(i, timeOpStart));
                currentTimeVessel.set(n, timeOpEnd);
                break;
            }
        }
    }
}
*/
