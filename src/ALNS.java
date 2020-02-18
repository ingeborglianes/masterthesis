import java.io.FileNotFoundException;
import java.sql.Time;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    //VesselTakenIntervals: Outer list: vessels. Inner list: list of intervals. Each interval is defined by four elements:
    //[earliest operation in interval][start time interval][end time interval][last operation in interval]
    //start time (fra og med), end time (til)
    List<List<int[]>> vesselTakenIntervals = new ArrayList<List<int[]>>();
    List<Integer> allOperations =  new ArrayList<Integer>();
    private int[] sortedOperations;
    int [][] twIntervals;
    int[][] precedenceALNS;
    int[] simALNS;
    int[][] bigTasksALNS;

    public ALNS(int [][] OperationsForVessel, int [][] TimeWindowsForOperations, int [][][] Edges, int [][][][] SailingTimes,
                int [][][] TimeVesselUseOnOperation, int [] EarliestStartingTimeForVessel,
                int [] SailingCostForVessel, int [] Penalty, int [][] Precedence, int [][] Simultaneous,
                int [] BigTasks, Map<Integer, List<Integer>> ConsolidatedTasks, int[] endNodes, int[] startNodes, double[] endPenaltyforVessel,
                int[][] twIntervals, int[][] precedenceALNS, int[] simALNS, int[][] bigTasksALNS){
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
        //nOperations is the number of all nodes, including start and end nodes
        this.nOperations=TimeWindowsForOperations.length;
        this.nTimePeriods=TimeWindowsForOperations[0].length;
        this.endNodes=endNodes;
        this.startNodes=startNodes;
        this.endPenaltyforVessel=endPenaltyforVessel;
        this.sortedOperations=new int[nOperations-2*startNodes.length];
        this.twIntervals=twIntervals;
        this.precedenceALNS=precedenceALNS;
        this.simALNS=simALNS;
        this.bigTasksALNS=bigTasksALNS;
        for(int o = startNodes.length+1; o<nOperations-endNodes.length+1;o++){
           allOperations.add(o);
        }
        System.out.println("Number of operations: "+(nOperations-startNodes.length*2));
        System.out.println("START NODES: "+Arrays.toString(this.startNodes));
        System.out.println("END NODES: "+Arrays.toString(this.endNodes));
    }

    public void createSortedOperations(){
        TreeMap<Integer,Integer> penaltiesDict = new TreeMap<Integer, Integer>();
        for (int p=0;p<this.Penalty.length;p++){
            //Key value (= operation number) in penaltiesDict is not null indexed
            penaltiesDict.put(p+1+startNodes.length,Penalty[p]);
        }
        //System.out.println(Arrays.toString(Penalty));
        int index=Penalty.length-1;
        for (Map.Entry<Integer, Integer> entry  : entriesSortedByValues(penaltiesDict)) {
            System.out.println(entry.getKey()+":"+entry.getValue());
            sortedOperations[index]=entry.getKey();
            index-=1;
        }
        System.out.println(Arrays.toString(sortedOperations));
    }

    //TODO:
    //1. Time windows
    //2. Precedence
    //3. Synkronisering
    //4. Consolidated tasks

    public void constructionHeuristic(){
        for (int n = 0; n < nVessels; n++) {
            vesselroutes.add(null);
        }
        for (Integer o : sortedOperations){
            int costAdded=100000;
            int indexInRoute=0;
            int routeIndex=0;
            int earliest=0;
            int latest=nTimePeriods;
            for (int v = 0; v < nVessels; v++) {
                if (DataGenerator.containsElement(o, OperationsForVessel[v])) {
                    if (vesselroutes.get(v) == null) {
                        int cost = SailingCostForVessel[v] * SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                        int earliestTemp=Math.max(EarliestStartingTimeForVessel[v],twIntervals[o-startNodes.length][0]);
                        int latestTemp=Math.min(nTimePeriods,twIntervals[o-startNodes.length][1]);
                        if (cost < costAdded & earliestTemp<=latestTemp) {
                            costAdded = cost;
                            routeIndex = v;
                            indexInRoute = 0;
                            earliest = earliestTemp;
                            latest = latestTemp;
                        }
                    }
                    else{
                        for(int n=0;n<vesselroutes.get(v).size();n++){
                            if (n==0){
                                int cost = SailingCostForVessel[v] * (SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1]
                                        +SailingTimes[v][EarliestStartingTimeForVessel[v]][o-1][vesselroutes.get(v).get(0).getID()-1]
                                - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselroutes.get(v).get(0).getID()-1]);
                                int earliestTemp=Math.max(EarliestStartingTimeForVessel[v],twIntervals[o-startNodes.length][0]);
                                int latestTemp=Math.min(vesselroutes.get(v).get(0).getLatestTime(),twIntervals[o-startNodes.length][1]);
                                if (cost < costAdded & earliestTemp<=latestTemp) {
                                    costAdded = cost;
                                    routeIndex = v;
                                    indexInRoute = 0;
                                    earliest = earliestTemp;
                                    latest = latestTemp;
                                }
                            }
                            else if (n==vesselroutes.get(v).size()){
                                
                            }
                            else{
                                int cost = SailingCostForVessel[v] * SailingTimes[v][EarliestStartingTimeForVessel[v]]
                                        [vesselroutes.get(v).get(vesselroutes.get(v).size()).getID()-1][o - 1];
                                int earliestTemp=Math.max(vesselroutes.get(v).get(vesselroutes.get(v).size()).getEarliestTime()+
                                        SailingTimes[v][EarliestStartingTimeForVessel[v]]
                                                [vesselroutes.get(v).get(vesselroutes.get(v).size()).getID()-1][o - 1],twIntervals[o-startNodes.length][0]);
                                int latestTemp=twIntervals[o-startNodes.length][1];
                                if (cost < costAdded & earliestTemp<=latestTemp) {
                                    costAdded = cost;
                                    routeIndex = v;
                                    indexInRoute = 0;
                                    earliest = earliestTemp;
                                    latest = latestTemp;
                                }
                            }
                        }
                    }
                }
            }
            //After iterating through all possible insertion places, we here add the operation at the best insertion place
        }
    }



    /*
    public void constructionHeuristic(){
        PrintData.timeVesselUseOnOperations(TimeVesselUseOnOperation,startNodes.length);
        PrintData.printSailingTimes(SailingTimes,1,nOperations-2*startNodes.length,startNodes.length);
        for (int n = 0; n < nVessels; n++) {
            vesselroutes.add(null);
            vesselTakenIntervals.add(null);
        }
        for (Integer o : sortedOperations){
            int vesselIndex=0;
            int startTime=0;
            int costAdded=100000;
            int endTime=0;
            int intervalIndex=-1;
            for (int v = 0; v < nVessels; v++) {
                if (DataGenerator.containsElement(o, OperationsForVessel[v])) {
                    if (vesselroutes.get(v)==null) {
                        int cost = SailingCostForVessel[v] * SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o-1];
                        if (cost < costAdded) {
                            costAdded = cost;
                            vesselIndex = v;
                            startTime = EarliestStartingTimeForVessel[v]+1 + SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                            endTime=startTime+TimeVesselUseOnOperation[v][o-startNodes.length-1][startTime];
                            intervalIndex=-1;
                        }
                    } else {
                        if (vesselTakenIntervals.get(v).size() == 1) {
                            int t = vesselTakenIntervals.get(v).get(0)[2];
                            if(t<=nTimePeriods){
                                int lastNode = vesselTakenIntervals.get(v).get(0)[3];
                                int cost = SailingCostForVessel[v] * SailingTimes[v][t]
                                        [lastNode - 1][o - 1];
                                int tempStartTime = t + SailingTimes[v][t][lastNode - 1][o - 1];
                                if (cost < costAdded && tempStartTime <= nTimePeriods) {
                                    costAdded = cost;
                                    vesselIndex = v;
                                    startTime = tempStartTime;
                                    endTime = startTime + TimeVesselUseOnOperation[v][o - startNodes.length - 1][startTime];
                                    intervalIndex = 0;
                                }
                            }
                        } else {
                            for (int i = 0; i < vesselTakenIntervals.get(v).size() - 1; i++) {
                                int t = vesselTakenIntervals.get(v).get(i)[2];
                                if(t<=nTimePeriods) {
                                    int lastNode = vesselTakenIntervals.get(v).get(0)[3];
                                    int cost = SailingCostForVessel[v] * SailingTimes[v][t]
                                            [lastNode][o - 1];
                                    int tempStartTime = t + SailingTimes[v][t][lastNode - 1][o - 1];
                                    int tempEndTime = tempStartTime + TimeVesselUseOnOperation[v][o - startNodes.length - 1][startTime];
                                    int totalTime = tempEndTime + SailingTimes[v][tempEndTime][o - 1][vesselTakenIntervals.get(v).get(i + 1)[0] - 1];
                                    if (cost < costAdded && tempStartTime <= nTimePeriods && totalTime <= vesselTakenIntervals.get(v).get(i + 1)[1]) {
                                        costAdded = cost;
                                        vesselIndex = v;
                                        startTime = tempStartTime;
                                        endTime = tempEndTime;
                                        intervalIndex = i;
                                    }
                                }
                            }
                            int lastIndex=vesselTakenIntervals.get(v).size() - 1;
                            int t = vesselTakenIntervals.get(v).get(lastIndex)[2];
                            if(t<=nTimePeriods) {
                                int lastNode = vesselTakenIntervals.get(v).get(lastIndex)[3];
                                int cost = SailingCostForVessel[v] * SailingTimes[v][t][lastNode - 1][o - 1];
                                int tempStartTime = t + SailingTimes[v][t][lastNode - 1][o - 1];
                                if (cost < costAdded && tempStartTime <= nTimePeriods) {
                                    costAdded = cost;
                                    vesselIndex = v;
                                    startTime = tempStartTime;
                                    endTime = startTime + TimeVesselUseOnOperation[v][o - startNodes.length - 1][startTime];
                                    intervalIndex = lastIndex;
                                }
                            }
                        }
                    }
                }
            }
            if(costAdded!=100000) {
                if (vesselroutes.get(vesselIndex) == null) {
                    int finalStartTime = startTime;
                    vesselroutes.set(vesselIndex, new ArrayList<>() {{
                        add(new OperationInRoute(o, finalStartTime));
                    }});
                } else {
                    vesselroutes.get(vesselIndex).add(new OperationInRoute(o, startTime));
                }
                allOperations.remove(Integer.valueOf(o));
                if (intervalIndex==-1){
                    int finalEndTime = endTime;
                    int finalVesselIndex = vesselIndex;
                    vesselTakenIntervals.set(vesselIndex,new ArrayList<>() {{add(new int[]{startNodes[finalVesselIndex], 1, finalEndTime,o});}});
                    System.out.println("First ADD for vessel");
                    System.out.println("VESSEL INDEX "+vesselIndex);
                    System.out.println("Operation time "+ o+" "+ TimeVesselUseOnOperation[vesselIndex][o-startNodes.length-1][startTime]);
                    System.out.println("Vessel index "+vesselIndex);
                    System.out.println("Sailing time "+ startNodes[vesselIndex] + " to "+ (o)+" is "+SailingTimes[vesselIndex][EarliestStartingTimeForVessel[vesselIndex]][vesselIndex][o-1]);
                    System.out.println(Arrays.toString(vesselTakenIntervals.get(vesselIndex).get(0)));
                    System.out.println(" ");
                }
                else if (intervalIndex>-1){
                    System.out.println("VESSEL INDEX "+vesselIndex);
                    System.out.println("Operation time "+ o+" "+ TimeVesselUseOnOperation[vesselIndex][o-startNodes.length-1][startTime]);
                    List<int[]> test=vesselTakenIntervals.get(vesselIndex);
                    int prevLastOp=vesselTakenIntervals.get(vesselIndex).get(intervalIndex)[3];
                    int prevEndTime=vesselTakenIntervals.get(vesselIndex).get(intervalIndex)[2];
                    System.out.println("Previous operation: "+prevLastOp);
                    System.out.println("Sailing time from "+prevLastOp+" to "+o+ " is "+ SailingTimes[vesselIndex][prevEndTime][prevLastOp-1][o-1]);
                    int startOperation=vesselTakenIntervals.get(vesselIndex).get(intervalIndex)[0];
                    int intervalStart=vesselTakenIntervals.get(vesselIndex).get(intervalIndex)[1];
                    vesselTakenIntervals.get(vesselIndex).set(intervalIndex,new int[]{startOperation,intervalStart,endTime,o});
                    System.out.println(Arrays.toString(test.get(intervalIndex)));
                    System.out.println(" ");

                }
            }
        }
        for(Integer tasksLeft : allOperations){
            unroutedTasks.add(new OperationInRoute(tasksLeft,0));
        }
    }
    // intervallene i VesselAvailableIntervals listene er fra (altså ekskluderende fra start) og til og med (altså
    // inkluderende på øvre grense i intervallet)
  */
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



    public void printInitialSolution(int[] vessseltypes){
        for (int i=0;i<vesselroutes.size();i++){
            System.out.println("VESSELINDEX "+i+" VESSELTYPE "+vessseltypes[i]);
            if (vesselroutes.get(i)!=null) {
                for (OperationInRoute opInRoute : vesselroutes.get(i)) {
                    System.out.println("Operation number: "+opInRoute.getID() +" Time: ");
                }
            }
        }
        for (int i=0;i<vesselTakenIntervals.size();i++){
            System.out.println("Vessel taken intervals for vessel number "+i+" VESSELTYPE "+vessseltypes[i]);
            if(vesselTakenIntervals.get(i)!=null) {
                for (int n = 0; n < vesselTakenIntervals.get(i).size(); n++) {
                    System.out.println("INTERVAL " + n);
                    int[] interval = vesselTakenIntervals.get(i).get(n);
                    System.out.println("Start task " + interval[0] + " Start time " + interval
                            [1] + " End time " + interval[2] + " End task " + interval[3]);
                }
            }
            System.out.println(" ");
        }
        if(!unroutedTasks.isEmpty()){
            System.out.println("UNROUTED TASKS");
            for(int n=0;n<unroutedTasks.size();n++) {
                System.out.println(unroutedTasks.get(n).getID());
            }
        }
    }


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
        int[] vesseltypes =new int[]{1,2,4,5};
        int[] startnodes=new int[]{1,2,3,4};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes ,
                "test_instances/test_instance_15_locations_first_test.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        ALNS a = new ALNS(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getPenalty(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS());
        a.createSortedOperations();
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);
    }

    public List<OperationInRoute> getUnroutedTasks() {
        return unroutedTasks;
    }

    public List<List<OperationInRoute>> getVesselroutes() {
        return vesselroutes;
    }
}



/*
1. Iterate over operations and insert to best location
*/

