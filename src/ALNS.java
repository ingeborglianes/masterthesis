import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.IntStream;

public class ALNS {
    private ConstructionHeuristic ch;
    private DataGenerator dg;
    private int[] vessels=new int[]{1,2,4,5,5,6,2,4};
    private int[] locStart = new int[]{1,2,3,4,5,6,7,8};
    private int numberOfRemoval;
    private int randomSeed;
    private double relatednessWeightDistance;
    private double relatednessWeightDuration;
    private double relatednessWeightTimewindows;
    private double relatednessWeightPrecedenceOver;
    public double relatednessWeightPrecedenceOf;
    public double relatednessWeightSimultaneous;


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
    }

    public void runALNS(){
        LargeNeighboorhoodSearchRemoval LNSR = new LargeNeighboorhoodSearchRemoval(ch.getPrecedenceOverOperations(),ch.getPrecedenceOfOperations(),
                ch.getSimultaneousOp(),ch.getSimOpRoutes(),ch.getPrecedenceOfRoutes(),ch.getPrecedenceOverRoutes(),
                ch.getConsolidatedOperations(),ch.getUnroutedTasks(),ch.getVesselroutes(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(), dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),numberOfRemoval,randomSeed,dg.getDistOperationsInInstance(),
                relatednessWeightDistance,relatednessWeightDuration,relatednessWeightTimewindows,relatednessWeightPrecedenceOver,
                relatednessWeightPrecedenceOf,relatednessWeightSimultaneous);
        //for run removal, insert method, alternatives: worst, synchronized, route, related, random
        LNSR.runLNSRemoval("route");
        System.out.println("-----------------");
        LNSR.printLNSSolution(vessels);
        //PrintData.printSailingTimes(dg.getSailingTimes(),4,dg.getSimultaneousALNS().length,a.getVesselroutes().size());
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),dg.getStartNodes().length);
        LargeNeighboorhoodSearchInsert LNSI = new LargeNeighboorhoodSearchInsert(LNSR.getPrecedenceOverOperations(),LNSR.getPrecedenceOfOperations(),
                LNSR.getSimultaneousOp(),LNSR.getSimOpRoutes(),LNSR.getPrecedenceOfRoutes(),LNSR.getPrecedenceOverRoutes(),
                LNSR.getConsolidatedOperations(),LNSR.getUnroutedTasks(),LNSR.getVesselRoutes(),LNSR.getRemovedOperations(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),dg.getOperationsForVessel(),randomSeed);
        System.out.println("-----------------");
        PrintData.printPrecedenceALNS(dg.getPrecedenceALNS());
        PrintData.printSimALNS(dg.getSimultaneousALNS());
        //for run insertion, insert method, alternatives: best, regret
        LNSI.runLNSInsert("regret");
        LNSI.switchConsolidated();
        LNSI.printLNSInsertSolution(vessels);
    }

    public static void main(String[] args) throws FileNotFoundException {
        ALNS alns= new ALNS();
        alns.runALNS();
    }
}
