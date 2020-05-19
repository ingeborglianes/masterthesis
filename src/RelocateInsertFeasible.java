import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.IntStream;

public class RelocateInsertFeasible {

    private int [][] OperationsForVessel;
    private int[] vesseltypes;
    private int[][][][] SailingTimes;
    private  int [][][] TimeVesselUseOnOperation;
    private int [] SailingCostForVessel;
    private int [] EarliestStartingTimeForVessel;
    private int [] startNodes;
    private int [][] twIntervals;
    private int [] routeSailingCost;
    private int[] routeOperationGain;
    private int [][][] operationGain;
    private int [][][] operationGainGurobi;
    private int objValue;
    private int nVessels;
    private int nTimePeriods;
    private int[][] simALNS;
    private int[][] precedenceALNS;
    private int [][] bigTasksALNS;
    private List<OperationInRoute> unroutedTasks;
    private List<List<OperationInRoute>> vesselRoutes;
    //map for operations that are connected with precedence. ID= operation number. Value= Precedence value.
    private Map<Integer,PrecedenceValues> precedenceOverOperations;
    private Map<Integer,PrecedenceValues> precedenceOfOperations;
    //List for operations that are connected as simultaneous operations. ID= operation number. Value= Simultaneous value.
    private Map<Integer, ConnectedValues> simultaneousOp;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;
    private List<Map<Integer,PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer,PrecedenceValues>> precedenceOverRoutes;
    private Map<Integer, ConsolidatedValues> consolidatedOperations;


    public RelocateInsertFeasible(int [][] OperationsForVessel, int[] vesseltypes, int[][][][] SailingTimes,
                          int [][][] TimeVesselUseOnOperation, int[] SailingCostForVessel, int [] EarliestStartingTimeForVessel,
                          int [][] twIntervals, int [] routeSailingCost, int[] routeOperationGain, int[] startNodes, int[][] simALNS,
                          int[][] precedenceALNS,
                          int [][] bigTasksALNS, int [][][] operationGain, List<OperationInRoute> unroutedTasks, List<List<OperationInRoute>> vesselRoutes,
                          Map<Integer,PrecedenceValues> precedenceOverOperations,Map<Integer,PrecedenceValues> precedenceOfOperations,
                          Map<Integer, ConnectedValues> simultaneousOp,List<Map<Integer, ConnectedValues>> simOpRoutes,
                          List<Map<Integer,PrecedenceValues>> precedenceOfRoutes, List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                          Map<Integer, ConsolidatedValues> consolidatedOperations,int[][][] operationGainGurobi){
        this.OperationsForVessel = OperationsForVessel;
        this.vesseltypes = vesseltypes;
        this.SailingTimes = SailingTimes;
        this.TimeVesselUseOnOperation = TimeVesselUseOnOperation;
        this.EarliestStartingTimeForVessel = EarliestStartingTimeForVessel;
        this.SailingCostForVessel = SailingCostForVessel;
        this.twIntervals = twIntervals;
        this.routeSailingCost = routeSailingCost;
        this.routeOperationGain = routeOperationGain;
        this.objValue = IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum();
        this.vesselRoutes = vesselRoutes;
        this.unroutedTasks = unroutedTasks;
        this.startNodes = startNodes;
        this.simALNS = simALNS;
        this.precedenceALNS = precedenceALNS;
        this.bigTasksALNS = bigTasksALNS;
        this.operationGain = operationGain;
        this.precedenceOverOperations =precedenceOverOperations;
        this.precedenceOfOperations = precedenceOfOperations;
        this.simultaneousOp = simultaneousOp;
        this.simOpRoutes = simOpRoutes;
        this.precedenceOfRoutes = precedenceOfRoutes;
        this.precedenceOverRoutes = precedenceOverRoutes;
        this.consolidatedOperations = consolidatedOperations;
        this.nVessels=vesselRoutes.size();
        this.nTimePeriods=TimeVesselUseOnOperation[0][0].length;
        this.operationGainGurobi=operationGainGurobi;
    }


    public Boolean relocateInsert(){
        Map<Integer,List<InsertionValues>> feasibleInsertions = new HashMap<>();
        Map<Integer,InsertionValues> currentPosition = new HashMap<>();
        List<Integer> searchedVessels = new ArrayList<>();
        for(OperationInRoute unroutedTask : unroutedTasks) {
            for (int vessel = 0; vessel < vesselRoutes.size() - 1; vessel++) {
                //OperationInRoute unroutedTask = unroutedTasks.get(0);
                if (ConstructionHeuristic.containsElement(unroutedTask.getID(), OperationsForVessel[vessel]) && !searchedVessels.contains(vessel)) {
                    for (int task=0; task<vesselRoutes.get(vessel).size(); task++) {
                        currentPosition.put(vesselRoutes.get(vessel).get(task).getID(), new InsertionValues(-1, task, vessel,
                                vesselRoutes.get(vessel).get(task).getEarliestTime(), vesselRoutes.get(vessel).get(task).getLatestTime()));
                        for(int v=0; v<nVessels; v++) {
                            if(v != vessel) {
                                if(simALNS[vesselRoutes.get(vessel).get(task).getID()-startNodes.length-1][0] == 0 && simALNS[vesselRoutes.get(vessel).get(task).getID()-startNodes.length-1][1] == 0){
                                    if(precedenceALNS[vesselRoutes.get(vessel).get(task).getID()-startNodes.length-1][1] == 0){
                                        feasibleInsertions = findInsertionCosts(vesselRoutes.get(vessel).get(task), v, -1, -1, -1,
                                                -1, -1, -1, -1, feasibleInsertions, this.vesselRoutes);
                                    }else{
                                        PrecedenceValues precedeceOverOp = precedenceOverOperations.get(precedenceALNS[vesselRoutes.get(vessel).get(task).getID()-startNodes.length-1][1]);
                                        feasibleInsertions = findInsertionCosts(vesselRoutes.get(vessel).get(task), v, -1, -1, precedeceOverOp.getOperationObject().getEarliestTime(),
                                                precedeceOverOp.getRoute(), -1, precedeceOverOp.getIndex(), -1, feasibleInsertions, this.vesselRoutes);
                                    }
                                }
                            }
                        }
                    }
                }
                searchedVessels.add(vessel);
            }
        }
        int bestInsertionCombination = 0;
        InsertionValues toMove = null;
        InsertionValues toInsert = null;
        int toMoveKey = 0;
        int toInsertKey = 0;
        for (Map.Entry<Integer, List<InsertionValues>> entry : feasibleInsertions.entrySet()) {
            int key = entry.getKey();
            System.out.println("Evaluating operation " + key);
            List<InsertionValues> iValues = entry.getValue();
            for (InsertionValues iv : iValues) {
                if(iv.getBenenefitIncrease() > 0){
                    System.out.println("Benefit " + iv.getBenenefitIncrease() + " in route " + iv.getRouteIndex() +
                            " on index " + iv.getIndexInRoute());
                    List<List<OperationInRoute>> VRCopy = copyVesselroutesRI(vesselRoutes);
                    InsertionValues cp = currentPosition.get(key);
                    VRCopy.get(cp.routeIndex).remove(cp.indexInRoute);
                    Map<Integer,List<InsertionValues>> feasibleUnrouted = new HashMap<>();
                    for(OperationInRoute unroutedTask : unroutedTasks) {
                        if (simALNS[unroutedTask.getID() - 1 - startNodes.length][1] != 0 ||
                                simALNS[unroutedTask.getID() - 1 - startNodes.length][0] != 0){
                            feasibleUnrouted = feasibleSimultaneousInsertions(feasibleUnrouted,unroutedTask);
                        }
                        else if(precedenceALNS[unroutedTask.getID() - 1 - startNodes.length][1] != 0) {
                            int precedenceID = precedenceALNS[unroutedTask.getID()-startNodes.length-1][1];
                            PrecedenceValues precedenceOp = precedenceOverOperations.get(precedenceID);
                            if (ConstructionHeuristic.containsElement(unroutedTask.getID(), OperationsForVessel[cp.getRouteIndex()]) && precedenceOp != null) {
                                feasibleUnrouted = findInsertionCosts(unroutedTask, cp.getRouteIndex(), -1, -1, precedenceOp.getOperationObject().getEarliestTime(),
                                        precedenceOp.getRoute(), -1,
                                        precedenceOp.getIndex(), -1, feasibleUnrouted, VRCopy);
                            }
                        }else{
                            if (ConstructionHeuristic.containsElement(unroutedTask.getID(), OperationsForVessel[cp.getRouteIndex()])) {
                                feasibleUnrouted = findInsertionCosts(unroutedTask, cp.getRouteIndex(), -1, -1, -1, -1, -1,
                                        -1, -1, feasibleUnrouted, VRCopy);
                            }
                        }
                    }
                    for (Map.Entry<Integer, List<InsertionValues>> task : feasibleUnrouted.entrySet()) {
                        int unrouted = task.getKey();
                        System.out.println("Evaluating operation " + unrouted);
                        List<InsertionValues> positions = task.getValue();
                        for (InsertionValues po : positions) {
                            if (po.getBenenefitIncrease() > 0) {
                                int combinationBenefit = po.getBenenefitIncrease() + iv.getBenenefitIncrease();
                                if(combinationBenefit > bestInsertionCombination){
                                    bestInsertionCombination = combinationBenefit;
                                    toMoveKey = key;
                                    toInsertKey = unrouted;
                                    toMove = iv;
                                    toInsert = po;
                                }
                                System.out.println("Benefit " + po.getBenenefitIncrease() + " in route " + po.getRouteIndex() +
                                        " on index " + po.getIndexInRoute());
                            }
                        }
                        System.out.println(" ");
                    }

                }
            }
        }
        System.out.println(" ");

        for (Map.Entry<Integer, List<InsertionValues>> entry : feasibleInsertions.entrySet()) {
            int key = entry.getKey();
            System.out.println("Evaluating operation " + key);
            List<InsertionValues> iValues = entry.getValue();
            for (InsertionValues iv : iValues) {
                System.out.println("Benefit " + iv.getBenenefitIncrease() + " in route " + iv.getRouteIndex() +
                        " on index " + iv.getIndexInRoute());
            }
            System.out.println(" ");
        }


        System.out.println(bestInsertionCombination);
        System.out.println("By removing " + toMoveKey + " and inserting " + toInsertKey);

        if(bestInsertionCombination > 0) {
            System.out.println();
            InsertionValues currentPos = currentPosition.get(toMoveKey);
            removeNormalOp(currentPos.getRouteIndex(), currentPos.getIndexInRoute());
            insertOperation(toMoveKey, toMove.getEarliest(), toMove.getLatest(), toMove.getIndexInRoute(), toMove.getRouteIndex());
            if(simALNS[toInsertKey-startNodes.length-1][0] == 0) {
                insertOperation(toInsertKey, toInsert.getEarliest(), toInsert.getLatest(), toInsert.getIndexInRoute(), toInsert.getRouteIndex());
            }else{
                for(InsertionValues simOp : feasibleInsertions.get(simALNS[toInsertKey-startNodes.length-1][0])){
                    if(simOp.getEarliest() == toInsert.getEarliest() && simOp.getLatest() == toInsert.getLatest()){
                        insertOperation(toInsertKey, toInsert.getEarliest(), toInsert.getLatest(), toInsert.getIndexInRoute(), toInsert.getRouteIndex());
                        insertOperation(simALNS[toInsertKey-startNodes.length-1][0], toInsert.getEarliest(), toInsert.getLatest(), simOp.indexInRoute, simOp.getRouteIndex());
                    }
                }
            }
            if(bigTasksALNS[toMoveKey-startNodes.length-1] != null && bigTasksALNS[toMoveKey-startNodes.length-1][0] != 0){
                if(consolidatedOperations.get(toMoveKey) != null){
                    consolidatedOperations.get(toMoveKey).setConsolidatedRoute(toMove.getRouteIndex());
                }
            }
            if(bigTasksALNS[toInsertKey-startNodes.length-1] != null && bigTasksALNS[toInsertKey-startNodes.length-1][0] == toInsertKey){
                int unrouted = 0;
                int sim = 0;
                boolean found = false;
                while (!found && unrouted < unroutedTasks.size()) {
                    if(unroutedTasks.get(unrouted).getID() == bigTasksALNS[toInsertKey-startNodes.length-1][1]) {
                        unroutedTasks.remove(unrouted);
                        while (!found && sim < unroutedTasks.size()) {
                            if (unroutedTasks.get(sim).getID() == simALNS[bigTasksALNS[toInsertKey-startNodes.length-1][1] - startNodes.length - 1][0]
                                    || unroutedTasks.get(sim).getID() == simALNS[bigTasksALNS[toInsertKey-startNodes.length-1][1] - startNodes.length - 1][1]) {
                                unroutedTasks.remove(sim);
                                found = true;
                            }
                        }
                    }
                    unrouted++;
                }

            }else if(bigTasksALNS[toInsertKey-startNodes.length-1] != null && bigTasksALNS[toInsertKey-startNodes.length-1][1] == toInsertKey){
                int unrouted = 0;
                boolean found = false;
                while (!found && unrouted < unroutedTasks.size()) {
                    if (unroutedTasks.get(unrouted).getID() == bigTasksALNS[toInsertKey-startNodes.length-1][0]) {
                        unroutedTasks.remove(unrouted);
                        found = true;
                    }
                }
            }

            int unrouted = 0;
            int sim = 0;
            boolean found = false;
            while (!found && unrouted < unroutedTasks.size()) {
                if(simALNS[toInsertKey-startNodes.length-1][0] == 0 && simALNS[toInsertKey-startNodes.length-1][1] == 0){
                    if (unroutedTasks.get(unrouted).getID() == toInsertKey)  {
                        unroutedTasks.remove(unrouted);
                        found = true;
                    }
                }else{
                    if(unroutedTasks.get(unrouted).getID() == toInsertKey) {
                        unroutedTasks.remove(unrouted);
                        while (!found && sim < unroutedTasks.size()) {
                            if (unroutedTasks.get(sim).getID() == simALNS[toInsertKey - startNodes.length - 1][0] || unroutedTasks.get(sim).getID() == simALNS[toInsertKey - startNodes.length - 1][1]) {
                                unroutedTasks.remove(sim);
                                found = true;
                            }
                        }
                    }
                }
                unrouted++;
            }

            printInitialSolution(vesseltypes);
            return true;
        }
        printInitialSolution(vesseltypes);
        return false;
    }


    public void relocateAll(){
        boolean change = true;
        while(change){
            change = relocateInsert();
        }
    }


    public Boolean insertSimultaneous(){
        //Map<Integer,InsertionValues> currentPosition = new HashMap<>();
        for(OperationInRoute unroutedTask : unroutedTasks) {
            if (simALNS[unroutedTask.getID() - startNodes.length - 1][0] != 0) {
                Map<Integer, List<InsertionValues>> feasibleInsertions = new HashMap<>();
                int precedenceID = precedenceALNS[unroutedTask.getID()-startNodes.length-1][1];
                PrecedenceValues precedenceOp = precedenceOverOperations.get(precedenceID);
                for (int vessel = 0; vessel < vesselRoutes.size() - 1; vessel++) {
                    if (ConstructionHeuristic.containsElement(unroutedTask.getID(), OperationsForVessel[vessel])) {
                        if(precedenceID == 0 || precedenceOp != null) {
                            feasibleInsertions = findInsertionCosts(unroutedTask, vessel, -1, -1, precedenceOp.getOperationObject().getEarliestTime(),
                                    precedenceOp.getRoute(), -1, precedenceOp.getIndex(), -1, feasibleInsertions,
                                    this.vesselRoutes);
                        }
                    }
                }
                int bestInsertionCombination = 0;
                InsertionValues toMove = null;
                InsertionValues toInsert = null;
                int toMoveKey = 0;
                int toInsertKey = 0;
                for (Map.Entry<Integer, List<InsertionValues>> entry : feasibleInsertions.entrySet()) {
                    int key = entry.getKey();
                    System.out.println("Evaluating operation " + key);
                    List<InsertionValues> iValues = entry.getValue();
                    for (InsertionValues iv : iValues) {
                        if (iv.getBenenefitIncrease() > 0) {
                            System.out.println("Benefit " + iv.getBenenefitIncrease() + " in route " + iv.getRouteIndex() +
                                    " on index " + iv.getIndexInRoute());
                            Map<Integer, List<InsertionValues>> feasibleSecondSim = new HashMap<>();
                            boolean found = false;
                            int secondSimIndex = 0;
                            OperationInRoute secondSimOp = null;
                            while (!found && secondSimIndex < unroutedTasks.size()) {
                                if (unroutedTasks.get(secondSimIndex).getID() == simALNS[unroutedTask.getID()-startNodes.length-1][0]) {
                                    secondSimOp = unroutedTasks.get(secondSimIndex);
                                    found = true;
                                }
                                secondSimIndex++;
                            }
                            for(int vessel=0;vessel<vesselRoutes.size();vessel++) {
                                if (ConstructionHeuristic.containsElement(secondSimOp.getID(), OperationsForVessel[vessel]) && vessel != iv.routeIndex ) {
                                    feasibleSecondSim = findInsertionCosts(secondSimOp, vessel, iv.earliest, iv.latest, precedenceOp.getOperationObject().getEarliestTime(), precedenceOp.getRoute(),
                                            iv.routeIndex, precedenceOp.getIndex(), iv.indexInRoute, feasibleSecondSim, vesselRoutes);
                                }
                            }
                            for (Map.Entry<Integer, List<InsertionValues>> task : feasibleSecondSim.entrySet()) {
                                int unrouted = task.getKey();
                                System.out.println("Evaluating operation " + unrouted);
                                List<InsertionValues> positions = task.getValue();
                                for (InsertionValues po : positions) {
                                    if (po.getBenenefitIncrease() > 0) {
                                        int combinationBenefit = po.getBenenefitIncrease() + iv.getBenenefitIncrease();
                                        if (combinationBenefit > bestInsertionCombination) {
                                            bestInsertionCombination = combinationBenefit;
                                            toMoveKey = key;
                                            toInsertKey = unrouted;
                                            toMove = iv;
                                            toInsert = po;
                                        }
                                        System.out.println("Benefit " + po.getBenenefitIncrease() + " in route " + po.getRouteIndex() +
                                                " on index " + po.getIndexInRoute());
                                    }
                                }
                                System.out.println(" ");
                            }
                        }
                    }
                }
                System.out.println(" ");

                for (Map.Entry<Integer, List<InsertionValues>> entry : feasibleInsertions.entrySet()) {
                    int key = entry.getKey();
                    System.out.println("Evaluating operation " + key);
                    List<InsertionValues> iValues = entry.getValue();
                    for (InsertionValues iv : iValues) {
                        System.out.println("Benefit " + iv.getBenenefitIncrease() + " in route " + iv.getRouteIndex() +
                                " on index " + iv.getIndexInRoute());
                    }
                    System.out.println(" ");
                }


                System.out.println(bestInsertionCombination);
                System.out.println("By removing " + toMoveKey + " and inserting " + toInsertKey);

                if (bestInsertionCombination > 0) {
                    System.out.println();
                    insertOperation(toMoveKey, toMove.getEarliest(), toMove.getLatest(), toMove.getIndexInRoute(), toMove.getRouteIndex());
                    insertOperation(toInsertKey, toInsert.getEarliest(), toInsert.getLatest(), toInsert.getIndexInRoute(), toInsert.getRouteIndex());
                    int unrouted = 0;
                    boolean found1 = false;
                    boolean found2 = false;
                    while ((!found1 || !found2) && unrouted < unroutedTasks.size()) {
                        if (unroutedTasks.get(unrouted).getID() == toInsertKey ||
                                unroutedTasks.get(unrouted).getID() == toMoveKey) {
                            unroutedTasks.remove(unrouted);
                            if(!found1){
                                found1 = true;
                            }else{
                                found2 = true;
                            }
                        }
                        unrouted++;
                    }
                    System.out.println("Simultaneous insertion performed");
                    printInitialSolution(vesseltypes);
                    return true;
                }
            }
        }
        System.out.println("No feasible placements for the simultaneous tasks found");
        return false;
    }


    public Map<Integer, List<InsertionValues>> feasibleSimultaneousInsertions(Map<Integer, List<InsertionValues>> feasibleInsertions, OperationInRoute unroutedTask){
        //Map<Integer,InsertionValues> currentPosition = new HashMap<>();
        if (simALNS[unroutedTask.getID() - startNodes.length - 1][0] != 0) {
            int precedenceID = precedenceALNS[unroutedTask.getID()-startNodes.length-1][1];
            PrecedenceValues precedenceOp = precedenceOverOperations.get(precedenceID);
            for (int vessel = 0; vessel < vesselRoutes.size() - 1; vessel++) {
                if (ConstructionHeuristic.containsElement(unroutedTask.getID(), OperationsForVessel[vessel])) {
                    if(precedenceOp != null) {
                        feasibleInsertions = findInsertionCosts(unroutedTask, vessel, -1, -1, precedenceOp.getOperationObject().getEarliestTime(),
                                precedenceOp.getRoute(), -1, precedenceOp.getIndex(), -1, feasibleInsertions,
                                this.vesselRoutes);
                    }else if(precedenceID == 0){
                        feasibleInsertions = findInsertionCosts(unroutedTask, vessel, -1, -1, -1,
                                -1, -1, -1, -1, feasibleInsertions,
                                this.vesselRoutes);
                    }
                }
            }
            int bestInsertionCombination = 0;
            Map<Integer, List<InsertionValues>> infeasibleSim = new HashMap<>();
            for (Map.Entry<Integer, List<InsertionValues>> entry : feasibleInsertions.entrySet()) {
                int key = entry.getKey();
                System.out.println("Evaluating operation " + key);
                List<InsertionValues> iValues = entry.getValue();
                for (InsertionValues iv : iValues) {
                    if (iv.getBenenefitIncrease() > 0) {
                        System.out.println("Benefit " + iv.getBenenefitIncrease() + " in route " + iv.getRouteIndex() +
                                " on index " + iv.getIndexInRoute());
                        Map<Integer, List<InsertionValues>> feasibleSecondSim = new HashMap<>();
                        boolean found = false;
                        int secondSimIndex = 0;
                        OperationInRoute secondSimOp = null;
                        while (!found && secondSimIndex < unroutedTasks.size()) {
                            if (unroutedTasks.get(secondSimIndex).getID() == simALNS[unroutedTask.getID()-startNodes.length-1][0]) {
                                secondSimOp = unroutedTasks.get(secondSimIndex);
                                found = true;
                            }
                            secondSimIndex++;
                        }
                        for(int vessel=0;vessel<vesselRoutes.size();vessel++) {
                            if (ConstructionHeuristic.containsElement(secondSimOp.getID(), OperationsForVessel[vessel]) && vessel != iv.routeIndex ) {
                                if(precedenceOp != null) {
                                    feasibleSecondSim = findInsertionCosts(secondSimOp, vessel, iv.earliest, iv.latest, precedenceOp.getOperationObject().getEarliestTime(), precedenceOp.getRoute(),
                                            iv.routeIndex, precedenceOp.getIndex(), iv.indexInRoute, feasibleSecondSim, vesselRoutes);
                                }
                            }else if(precedenceID == 0){
                                feasibleSecondSim = findInsertionCosts(secondSimOp, vessel, iv.earliest, iv.latest, -1,
                                        -1, iv.routeIndex, -1, iv.indexInRoute, feasibleSecondSim, vesselRoutes);
                            }
                        }
                        for (Map.Entry<Integer, List<InsertionValues>> task : feasibleSecondSim.entrySet()) {
                            int unrouted = task.getKey();
                            System.out.println("Evaluating operation " + unrouted);
                            List<InsertionValues> positions = task.getValue();
                            for (InsertionValues po : positions) {
                                if (po.getBenenefitIncrease() > 0) {
                                    int combinationBenefit = po.getBenenefitIncrease() + iv.getBenenefitIncrease();
                                    if(feasibleInsertions.get(unrouted)==null){
                                        feasibleInsertions.put(unrouted, new ArrayList<>() {{
                                            add(new InsertionValues(po.getBenenefitIncrease(), po.getIndexInRoute(), po.getRouteIndex(), po.getEarliest(), po.getLatest()));
                                        }});
                                    }
                                    else{
                                        feasibleInsertions.get(unrouted).add(new InsertionValues(po.getBenenefitIncrease(), po.getIndexInRoute(), po.getRouteIndex(), po.getEarliest(), po.getLatest()));
                                    }
                                    if (combinationBenefit > bestInsertionCombination) {
                                        bestInsertionCombination = combinationBenefit;
                                    }
                                    System.out.println("Benefit " + po.getBenenefitIncrease() + " in route " + po.getRouteIndex() +
                                            " on index " + po.getIndexInRoute());
                                }
                                else{
                                    if(infeasibleSim.get(key)==null){
                                        infeasibleSim.put(key, new ArrayList<>() {{
                                            add(iv);
                                        }});
                                    }
                                    else{
                                        infeasibleSim.get(key).add(iv);
                                    }
                                    //feasibleInsertions.get(key).remove(iv);
                                }
                            }
                            System.out.println(" ");
                        }
                    }
                }
            }
            for(Map.Entry<Integer, List<InsertionValues>> entry:  infeasibleSim.entrySet()){
                int key = entry.getKey();
                List<InsertionValues> iValues = entry.getValue();
                for(InsertionValues iv : iValues) {
                    feasibleInsertions.get(key).remove(iv);
                }
            }
        }
        return feasibleInsertions;
    }


    public Boolean insertPrecedenceOf(){
        Map<Integer,List<InsertionValues>> feasibleInsertions = new HashMap<>();
        Map<Integer,InsertionValues> currentPosition = new HashMap<>();
        for(OperationInRoute unroutedOp : unroutedTasks) {
            if (precedenceALNS[unroutedOp.getID() - startNodes.length - 1][1] != 0) {
                int precOverId = precedenceALNS[unroutedOp.getID() - startNodes.length - 1][1];
                PrecedenceValues precOverOp = precedenceOverOperations.get(precOverId);
                if (precOverOp != null && simultaneousOp.get(precOverId) == null) {
                    currentPosition.put(precOverId, new InsertionValues(-1, precOverOp.getIndex(), precOverOp.getRoute(),
                            precOverOp.getOperationObject().getEarliestTime(), precOverOp.getOperationObject().getLatestTime()));
                    if (simALNS[unroutedOp.getID() - startNodes.length - 1][0] == 0 && simALNS[unroutedOp.getID() - startNodes.length - 1][1] == 0) {
                        for (int v = 0; v < vesselRoutes.size(); v++) {
                            feasibleInsertions = findInsertionCosts(unroutedOp, v, -1, -1, -1,
                                    -1, -1, -1, -1, feasibleInsertions, this.vesselRoutes);
                        }
                    } else {
                        feasibleInsertions = feasibleSimultaneousInsertions(feasibleInsertions,unroutedOp);
                    }
                    int bestInsertionCombination = 0;
                    InsertionValues toMove = null;
                    InsertionValues toInsert = null;
                    int toMoveKey = 0;
                    int toInsertKey = 0;
                    for (Map.Entry<Integer, List<InsertionValues>> entry : feasibleInsertions.entrySet()) {
                        int key = entry.getKey();
                        System.out.println("Evaluating operation " + key);
                        List<InsertionValues> iValues = entry.getValue();
                        for (InsertionValues iv : iValues) {
                            Map<Integer, List<InsertionValues>> feasibleMoves = new HashMap<>();
                            if (iv.getBenenefitIncrease() > 0) {
                                System.out.println("Benefit " + iv.getBenenefitIncrease() + " in route " + iv.getRouteIndex() +
                                        " on index " + iv.getIndexInRoute());
                                List<List<OperationInRoute>> VRCopy = copyVesselroutesRI(vesselRoutes);
                                InsertionValues cp = currentPosition.get(precOverId);
                                VRCopy.get(cp.routeIndex).remove(cp.indexInRoute);
                                feasibleMoves = findInsertionCosts(vesselRoutes.get(precOverOp.getRoute()).get(precOverOp.getIndex()), precOverOp.getRoute(), -1, -1, -1,
                                        -1, -1, -1, -1, feasibleMoves, VRCopy);

                            }
                            for (Map.Entry<Integer, List<InsertionValues>> task : feasibleMoves.entrySet()) {
                                int unrouted = task.getKey();
                                System.out.println("Evaluating operation " + unrouted);
                                List<InsertionValues> positions = task.getValue();
                                for (InsertionValues po : positions) {
                                    if (po.getBenenefitIncrease() > 0) {
                                        int combinationBenefit = po.getBenenefitIncrease() + iv.getBenenefitIncrease();
                                        if (combinationBenefit > bestInsertionCombination) {
                                            if (po.getEarliest() + TimeVesselUseOnOperation[po.getRouteIndex()][unrouted - startNodes.length - 1][po.getEarliest()] < iv.getEarliest()) {
                                                bestInsertionCombination = combinationBenefit;
                                                toMoveKey = unrouted;
                                                toInsertKey = key;
                                                toMove = po;
                                                toInsert = iv;
                                            }
                                        }
                                        System.out.println("Benefit " + po.getBenenefitIncrease() + " in route " + po.getRouteIndex() +
                                                " on index " + po.getIndexInRoute());
                                    }
                                }
                                System.out.println(" ");
                            }
                        }
                    }
                    System.out.println(bestInsertionCombination);
                    System.out.println("By removing " + toMoveKey + " and inserting " + toInsertKey);

                    if (bestInsertionCombination > 0) {
                        System.out.println();
                        InsertionValues currentPos = currentPosition.get(toMoveKey);
                        removeNormalOp(currentPos.getRouteIndex(), currentPos.getIndexInRoute());
                        insertOperation(toMoveKey, toMove.getEarliest(), toMove.getLatest(), toMove.getIndexInRoute(), toMove.getRouteIndex());
                        if(simALNS[toInsertKey-startNodes.length-1][0] == 0) {
                            insertOperation(toInsertKey, toInsert.getEarliest(), toInsert.getLatest(), toInsert.getIndexInRoute(), toInsert.getRouteIndex());
                        }else{
                            for(InsertionValues simOp : feasibleInsertions.get(simALNS[toInsertKey-startNodes.length-1][0])){
                                if(simOp.getEarliest() == toInsert.getEarliest() && simOp.getLatest() == toInsert.getLatest()){
                                    insertOperation(toInsertKey, toInsert.getEarliest(), toInsert.getLatest(), toInsert.getIndexInRoute(), toInsert.getRouteIndex());
                                    insertOperation(simALNS[toInsertKey-startNodes.length-1][0], toInsert.getEarliest(), toInsert.getLatest(), simOp.indexInRoute, simOp.getRouteIndex());
                                }
                            }
                        }
                        int unrouted = 0;
                        int sim = 0;
                        boolean found = false;
                        while (!found && unrouted < unroutedTasks.size()) {
                            if(simALNS[toInsertKey-startNodes.length-1][0] == 0 && simALNS[toInsertKey-startNodes.length-1][1] == 0){
                                if (unroutedTasks.get(unrouted).getID() == toInsertKey)  {
                                    unroutedTasks.remove(unrouted);
                                    found = true;
                                }
                            }else{
                                if(unroutedTasks.get(unrouted).getID() == toInsertKey) {
                                    unroutedTasks.remove(unrouted);
                                    while (!found && sim < unroutedTasks.size()) {
                                        if (unroutedTasks.get(sim).getID() == simALNS[toInsertKey - startNodes.length - 1][0] || unroutedTasks.get(sim).getID() == simALNS[toInsertKey - startNodes.length - 1][1]) {
                                            unroutedTasks.remove(sim);
                                            found = true;
                                        }
                                    }
                                }
                            }
                            unrouted++;
                        }
                        printInitialSolution(vesseltypes);
                        return true;
                    }
                }
            }
        }
        printInitialSolution(vesseltypes);
        return false;
    }


    public Map<Integer,List<InsertionValues>> findInsertionCosts(OperationInRoute operationToInsert, int v,int earliestSO, int latestSO, int earliestPO,
                                                                 int routeConnectedPrecedence, int routeConnectedSimultaneous, int pOFIndex, int simAIndex, Map<Integer,List<InsertionValues>> allFeasibleInsertions,
                                                                 List<List<OperationInRoute>> vesselRoutes){
        //WHAT TO DO WITH CONSOLIDATED?
        //Map<Integer,List<InsertionValues>> allFeasibleInsertions = new HashMap<>();
        int o=operationToInsert.getID();
        int benefitIncrease=-100000;
        int indexInRoute=0;
        int routeIndex=0;
        int earliest=0;
        int latest=nTimePeriods-1;
        System.out.println("On operation: "+o);
        boolean precedenceOverFeasible;
        boolean precedenceOfFeasible;
        boolean simultaneousFeasible;
        //System.out.println("ROUTE CONNECTED SIM: "+routeConnectedSimultaneous);
        if (DataGenerator.containsElement(o, OperationsForVessel[v]) && v!= routeConnectedSimultaneous) {
            System.out.println("Try vessel "+v);
            if (vesselRoutes.get(v) == null || vesselRoutes.get(v).isEmpty()) {
                //System.out.println("Empty route");
                //insertion into empty route
                int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                int sailingCost=sailingTimeStartNodeToO*SailingCostForVessel[v];
                int earliestTemp=Math.max(EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO+1,twIntervals[o-startNodes.length-1][0]);
                int latestTemp=Math.min(nTimePeriods,twIntervals[o-startNodes.length-1][1]);
                int precedenceOfValuesEarliest=checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence);
                earliestTemp=precedenceOfValuesEarliest;
                int [] simultaneousTimesValues = checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO);
                //System.out.println(simultaneousTimesValues[0] + "," + simultaneousTimesValues[1]+ " sim time");
                earliestTemp=simultaneousTimesValues[0];
                latestTemp=simultaneousTimesValues[1];
                if(earliestTemp<=latestTemp) {
                    //System.out.println("Feasible for empty route");
                    int benefitIncreaseTemp=operationGain[v][o-startNodes.length-1][earliestTemp-1]-sailingCost;
                    if(simALNS[o-startNodes.length-1][1]==0) {
                        InsertionValues iv = new InsertionValues(benefitIncreaseTemp, 0, v, earliestTemp, latestTemp);
                        insertFeasibleDict(o,iv, benefitIncreaseTemp,allFeasibleInsertions);
                    }
                    else{
                        if(benefitIncreaseTemp>benefitIncrease) {
                            benefitIncrease = benefitIncreaseTemp;
                            routeIndex = v;
                            indexInRoute = 0;
                            earliest = earliestTemp;
                            latest = latestTemp;
                        }
                    }
                }
            }
            else{
                for(int n=0;n<vesselRoutes.get(v).size();n++){
                    //System.out.println("On index "+n);
                    if(n==0) {
                        //check insertion in first position
                        int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                        int earliestTemp = Math.max(EarliestStartingTimeForVessel[v] + sailingTimeStartNodeToO + 1, twIntervals[o - startNodes.length - 1][0]);
                        int opTime=TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO];
                        int precedenceOfValuesEarliest=checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence);
                        earliestTemp=precedenceOfValuesEarliest;
                        if(earliestTemp<=nTimePeriods) {
                            int sailingTimeOToNext = SailingTimes[v][earliestTemp - 1][o - 1][vesselRoutes.get(v).get(0).getID() - 1];
                            int latestTemp = Math.min(vesselRoutes.get(v).get(0).getLatestTime() - sailingTimeOToNext - opTime,
                                    twIntervals[o - startNodes.length - 1][1]);
                            int [] simultaneousTimesValues = checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO);
                            earliestTemp = simultaneousTimesValues[0];
                            latestTemp = simultaneousTimesValues[1];
                            int timeIncrease = sailingTimeStartNodeToO + sailingTimeOToNext
                                    - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselRoutes.get(v).get(0).getID() - 1];
                            int sailingCost = timeIncrease * SailingCostForVessel[v];
                            Boolean pPlacementFeasible = checkPPlacementLNS(o, n, v,routeConnectedPrecedence,pOFIndex);
                            if (earliestTemp <= latestTemp && pPlacementFeasible) {
                                OperationInRoute lastOperation = vesselRoutes.get(v).get(vesselRoutes.get(v).size() - 1);
                                int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, 0, v, o,vesselRoutes,
                                        TimeVesselUseOnOperation,startNodes,SailingTimes);
                                int deltaOperationGainLastOperation = 0;
                                if (changedTime > 0) {
                                    deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                            operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                }
                                int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                precedenceOverFeasible = checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, 0, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods,
                                        SailingTimes, vesselRoutes, precedenceOfOperations, precedenceOverRoutes);
                                precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, 0, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                        vesselRoutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, simultaneousOp);
                                simultaneousFeasible = checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, 0, earliestTemp, latestTemp, simultaneousOp, simALNS,
                                        startNodes, SailingTimes, TimeVesselUseOnOperation,vesselRoutes,routeConnectedSimultaneous,simAIndex);
                                if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                    //System.out.println("Feasible for position n=0");
                                    if(simALNS[o-startNodes.length-1][1]==0) {
                                        InsertionValues iv = new InsertionValues(benefitIncreaseTemp, 0, v, earliestTemp, latestTemp);
                                        insertFeasibleDict(o,iv, benefitIncreaseTemp,allFeasibleInsertions);
                                    }
                                    else{
                                        if(benefitIncreaseTemp>benefitIncrease) {
                                            benefitIncrease = benefitIncreaseTemp;
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
                    if (n==vesselRoutes.get(v).size()-1){
                        //check insertion in last position
                        //System.out.println("Checking this position");
                        int earliestN=vesselRoutes.get(v).get(n).getEarliestTime();
                        int operationTimeN=TimeVesselUseOnOperation[v][vesselRoutes.get(v).get(n).getID()-1-startNodes.length][earliestN-1];
                        int startTimeSailingTimePrevToO=earliestN+operationTimeN;
                        if(startTimeSailingTimePrevToO >= nTimePeriods){
                            continue;
                        }
                        int sailingTimePrevToO=SailingTimes[v][startTimeSailingTimePrevToO-1]
                                [vesselRoutes.get(v).get(n).getID()-1][o - 1];
                        int earliestTemp=Math.max(earliestN + operationTimeN + sailingTimePrevToO
                                ,twIntervals[o-startNodes.length-1][0]);
                        int latestTemp=twIntervals[o-startNodes.length-1][1];
                        int precedenceOfValuesEarliest=checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence);
                        earliestTemp=precedenceOfValuesEarliest;
                        if(earliestTemp<=nTimePeriods) {
                            //System.out.println("Time feasible");
                            int [] simultaneousTimesValues = checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO);
                            earliestTemp = simultaneousTimesValues[0];
                            latestTemp = simultaneousTimesValues[1];
                            int timeIncrease = sailingTimePrevToO;
                            int sailingCost = timeIncrease * SailingCostForVessel[v];
                            if (earliestTemp <= latestTemp) {
                                //System.out.println("p placement feasible and time feasible");
                                OperationInRoute lastOperation = vesselRoutes.get(v).get(vesselRoutes.get(v).size() - 1);
                                int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, n + 1, v, o,vesselRoutes,
                                        TimeVesselUseOnOperation,startNodes,SailingTimes);
                                int deltaOperationGainLastOperation = 0;
                                if (changedTime > 0) {
                                    deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                            operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                }
                                int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                precedenceOverFeasible = checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods, SailingTimes,
                                        vesselRoutes, precedenceOfOperations, precedenceOverRoutes);
                                precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                        vesselRoutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, simultaneousOp);
                                simultaneousFeasible = checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp, simultaneousOp,
                                        simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation,vesselRoutes,routeConnectedSimultaneous,simAIndex);
                                if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                    //System.out.println("Feasible for last position in route");
                                    if(simALNS[o-startNodes.length-1][1]==0) {
                                        InsertionValues iv=new InsertionValues(benefitIncreaseTemp,n+1,v,earliestTemp,latestTemp);
                                        insertFeasibleDict(o,iv,benefitIncreaseTemp,allFeasibleInsertions);
                                    }
                                    else{
                                        //System.out.println("Benefit increase temp: "+benefitIncreaseTemp);
                                        if(benefitIncreaseTemp>benefitIncrease) {
                                            benefitIncrease = benefitIncreaseTemp;
                                            routeIndex = v;
                                            indexInRoute = n + 1;
                                            earliest = earliestTemp;
                                            latest = latestTemp;
                                            //System.out.println("earliest: "+earliest+" latest: "+latest);
                                            //System.out.println("SIM ROUTE "+routeConnectedSimultaneous+ " Sim index "+simAIndex+
                                            //        " sim earliest: "+earliestSO+" sim latest: "+latestSO);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if(n<vesselRoutes.get(v).size()-1) {
                        //check insertion for all other positions in the route
                        int earliestN = vesselRoutes.get(v).get(n).getEarliestTime();
                        int operationTimeN = TimeVesselUseOnOperation[v][vesselRoutes.get(v).get(n).getID() - 1 - startNodes.length][earliestN - 1];
                        int startTimeSailingTimePrevToO = earliestN + operationTimeN;
                        int sailingTimePrevToO = SailingTimes[v][Math.min(nTimePeriods-1,startTimeSailingTimePrevToO - 1)][vesselRoutes.get(v).get(n).getID() - 1][o - 1];
                        int earliestTemp = Math.max(earliestN + sailingTimePrevToO + operationTimeN, twIntervals[o - startNodes.length - 1][0]);
                        int precedenceOfValuesEarliest=checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence);
                        earliestTemp=precedenceOfValuesEarliest;
                        if(earliestTemp<=nTimePeriods) {
                            if (earliestTemp - 1 < nTimePeriods) {
                                int opTime = TimeVesselUseOnOperation[v][o - 1 - startNodes.length][earliestTemp - 1];
                                int sailingTimeOToNext = SailingTimes[v][Math.min(earliestTemp + opTime - 1, nTimePeriods - 1)][o - 1][vesselRoutes.get(v).get(n + 1).getID() - 1];

                                int latestTemp = Math.min(vesselRoutes.get(v).get(n + 1).getLatestTime() -
                                        sailingTimeOToNext - opTime, twIntervals[o - startNodes.length - 1][1]);
                                int [] simultaneousTimesValues = checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO);
                                earliestTemp = simultaneousTimesValues[0];
                                latestTemp = simultaneousTimesValues[1];
                                int sailingTimePrevToNext = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselRoutes.get(v).get(n).getID() - 1][vesselRoutes.get(v).get(n + 1).getID() - 1];
                                int timeIncrease = sailingTimePrevToO + sailingTimeOToNext - sailingTimePrevToNext;
                                int sailingCost = timeIncrease * SailingCostForVessel[v];
                                Boolean pPlacementFeasible = checkPPlacementLNS(o, n+1, v,routeConnectedPrecedence,pOFIndex);
                                if (earliestTemp <= latestTemp && pPlacementFeasible) {
                                    OperationInRoute lastOperation = vesselRoutes.get(v).get(vesselRoutes.get(v).size() - 1);
                                    int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                    int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute,
                                            n + 1, v, o,vesselRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes);
                                    int deltaOperationGainLastOperation = 0;
                                    if (changedTime > 0) {
                                        deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                    }
                                    int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                    int currentLatest = vesselRoutes.get(v).get(n).getLatestTime();
                                    simultaneousFeasible = ConstructionHeuristic.checkSOfFeasible(o, v, currentLatest, startNodes, simALNS, simultaneousOp);
                                    if (simultaneousFeasible) {
                                        simultaneousFeasible = checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp,
                                                simultaneousOp, simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation,
                                                vesselRoutes,routeConnectedSimultaneous,simAIndex);
                                    }
                                    precedenceOverFeasible = checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation,
                                            nTimePeriods, SailingTimes, vesselRoutes, precedenceOfOperations, precedenceOverRoutes);
                                    precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                            vesselRoutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, simultaneousOp);
                                    if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                        //System.out.println("Feasible for index: "+(n+1));
                                        if(simALNS[o-startNodes.length-1][1]==0) {
                                            InsertionValues iv=new InsertionValues(benefitIncreaseTemp,n+1,v,earliestTemp,latestTemp);
                                            insertFeasibleDict(o,iv,benefitIncreaseTemp,allFeasibleInsertions);
                                        }
                                        else{
                                            if(benefitIncreaseTemp>benefitIncrease) {
                                                benefitIncrease = benefitIncreaseTemp;
                                                routeIndex = v;
                                                indexInRoute = n + 1;
                                                earliest = earliestTemp;
                                                latest = latestTemp;
                                                //System.out.println("SIM ROUTE "+routeConnectedSimultaneous+ " Sim index "+simAIndex+
                                                //        " sim earliest: "+earliestSO+" sim latest: "+latestSO);
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        if(simALNS[o-startNodes.length-1][1]==0){
            System.out.println("Benefit increase for presOf or simOf "+benefitIncrease);
            int finalBenefitIncrease = benefitIncrease;
            int finalIndexInRoute = indexInRoute;
            int finalRouteIndex = routeIndex;
            int finalEarliest = earliest;
            int finalLatest = latest;
            if(allFeasibleInsertions.get(o)==null){
                allFeasibleInsertions.put(o, new ArrayList<>() {{
                    add(new InsertionValues(finalBenefitIncrease, finalIndexInRoute, finalRouteIndex, finalEarliest, finalLatest));
                }});
            }
            else{
                allFeasibleInsertions.get(o).add(new InsertionValues(finalBenefitIncrease, finalIndexInRoute, finalRouteIndex, finalEarliest, finalLatest));
            }
        }
        else{
            int finalBenefitIncrease = benefitIncrease;
            int finalIndexInRoute = indexInRoute;
            int finalRouteIndex = routeIndex;
            int finalEarliest = earliest;
            int finalLatest = latest;
            if (allFeasibleInsertions.get(o)==null){
                allFeasibleInsertions.put(o, new ArrayList<>() {{
                    add(new InsertionValues(finalBenefitIncrease, finalIndexInRoute, finalRouteIndex, finalEarliest, finalLatest));
                }});
            }
        }
        return allFeasibleInsertions;
    }


    public void insertOperation(int o, int earliest, int latest, int indexInRoute, int routeIndex){
        //=ConstructionHeuristic.updateConsolidatedOperations(o,routeIndex,removeConsolidatedSmallTasks,bigTasksALNS,
        //        startNodes, consolidatedOperations);
        OperationInRoute newOr=new OperationInRoute(o, earliest, latest);
        int presOver=precedenceALNS[o-1-startNodes.length][0];
        int presOf=precedenceALNS[o-1-startNodes.length][1];
        if (presOver!=0){
            //System.out.println(o+" added in precedence operations dictionary 0 "+presOver);
            PrecedenceValues pValues= new PrecedenceValues(newOr,null,presOver,indexInRoute,routeIndex,0);
            precedenceOverOperations.put(o,pValues);
            precedenceOverRoutes.get(routeIndex).put(o,pValues);
        }
        if (presOf!=0){
            //System.out.println("Operation precedence of: "+presOf);
            PrecedenceValues pValues= precedenceOverOperations.get(presOf);
            PrecedenceValues pValuesReplace=new PrecedenceValues(pValues.getOperationObject(),
                    newOr,pValues.getConnectedOperationID(),pValues.getIndex(),pValues.getRoute(),routeIndex);
            PrecedenceValues pValuesPut=new PrecedenceValues(newOr,pValues.getOperationObject(),presOf,indexInRoute,routeIndex,pValues.getRoute());
            precedenceOverOperations.put(presOf,pValuesReplace);
            precedenceOfOperations.put(o, pValuesPut);
            precedenceOverRoutes.get(pValues.getRoute()).put(presOf,pValuesReplace);
            precedenceOfRoutes.get(routeIndex).put(o, pValuesPut);
        }
        //while((o-1-startNodes.length+k) < simALNS.length && simALNS[o-1-startNodes.length+k][0]!=0 ){
        int simA = simALNS[o-1-startNodes.length][1];
        int simB = simALNS[o-1-startNodes.length][0];
        if(simB != 0 && simA == 0) {
            ConnectedValues sValue = new ConnectedValues(newOr, null,simB,indexInRoute,routeIndex, 0);
            simultaneousOp.put(o,sValue);
            simOpRoutes.get(routeIndex).put(o,sValue);
        }
        else if (simA != 0){
            ConnectedValues sValues = simultaneousOp.get(simA);
            if(sValues.getConnectedOperationObject() == null){
                ConnectedValues cValuesReplace=new ConnectedValues(sValues.getOperationObject(), newOr, sValues.getConnectedOperationID(),
                        sValues.getIndex(), sValues.getRoute(), routeIndex);
                simultaneousOp.replace(simA, sValues, cValuesReplace);
                simOpRoutes.get(sValues.getRoute()).replace(simA, sValues, cValuesReplace);
            }
            else{
                ConnectedValues cValuesPut1=new ConnectedValues(sValues.getOperationObject(), newOr, sValues.getConnectedOperationID(),
                        sValues.getIndex(), sValues.getRoute(), routeIndex);
                simultaneousOp.put(simA, cValuesPut1);
                simOpRoutes.get(sValues.getRoute()).put(simA, cValuesPut1);
            }
            ConnectedValues sim2=new ConnectedValues(newOr,sValues.getOperationObject(),simA,indexInRoute,routeIndex,sValues.getRoute());
            simultaneousOp.put(o, sim2);
            simOpRoutes.get(routeIndex).put(o, sim2);
        }
        System.out.println("INSERTION IS PERFORMED");
        System.out.println("NEW ADD: Vessel route "+routeIndex);
        System.out.println("Operation "+o);
        System.out.println("Earliest time "+ earliest);
        System.out.println("Latest time "+ latest);
        System.out.println("Route index "+indexInRoute);
        System.out.println(" ");
        if (vesselRoutes.get(routeIndex) == null) {
            int finalIndexInRoute = indexInRoute;
            vesselRoutes.set(routeIndex, new ArrayList<>() {{
                add(finalIndexInRoute, newOr);
            }});
        }
        else {
            vesselRoutes.get(routeIndex).add(indexInRoute,newOr);
        }
        ConstructionHeuristic.updateIndexesInsertion(routeIndex,indexInRoute, vesselRoutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
        //Update all earliest starting times forward
        LS_operators.updateEarliest(vesselRoutes.get(routeIndex).get(0).getEarliestTime(),0,routeIndex, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes,twIntervals,"oneexchange");
        LS_operators.updateLatest(nTimePeriods,vesselRoutes.get(routeIndex).size()-1,routeIndex, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes, twIntervals, "oneexchange");
        LS_operators.updateConRoutes(simOpRoutes,precedenceOfRoutes,precedenceOverRoutes,routeIndex,vesselRoutes,null,precedenceOverOperations,precedenceOfOperations,simultaneousOp,
                SailingTimes,twIntervals,EarliestStartingTimeForVessel,startNodes,TimeVesselUseOnOperation);
        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(routeIndex),indexInRoute,simOpRoutes,precedenceOfOperations,precedenceOverOperations,
                TimeVesselUseOnOperation, startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselRoutes,SailingTimes);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(routeIndex),indexInRoute,TimeVesselUseOnOperation,startNodes,simOpRoutes,
                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselRoutes,simultaneousOp,SailingTimes);
        LS_operators.updateSimultaneous(simOpRoutes,routeIndex,simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,
                startNodes,SailingTimes,precedenceOverOperations,precedenceOfOperations,vesselRoutes,0,0,"relocateInsert",EarliestStartingTimeForVessel);
    }

    public void removeNormalOp(int route, int index){
        OperationInRoute selectedTask = vesselRoutes.get(route).get(index);
        if (bigTasksALNS[selectedTask.getID() - 1 - startNodes.length] != null &&
                bigTasksALNS[selectedTask.getID() - startNodes.length - 1][0] == selectedTask.getID()) {
            consolidatedOperations.replace(bigTasksALNS[selectedTask.getID() - startNodes.length - 1][0],
                    new ConsolidatedValues(false, false, 0, 0, 0));
        }
        System.out.println("REMOVE NORMAL OP: "+selectedTask.getID());
        ObjectiveValues ov= ConstructionHeuristic.calculateObjective(vesselRoutes, TimeVesselUseOnOperation, startNodes,
                SailingTimes, SailingCostForVessel, EarliestStartingTimeForVessel, operationGainGurobi, new int[nVessels],
                new int[nVessels], 0, simALNS, bigTasksALNS);
        objValue=ov.getObjvalue();
        routeSailingCost=ov.getRouteSailingCost();
        routeOperationGain=ov.getRouteBenefitGain();
        //updateObjectives(selectedTask,route,index);
        vesselRoutes.get(route).remove(index);
        ConstructionHeuristic.updateIndexesRemoval(route, index, vesselRoutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
    }

    public void updateObjectives(OperationInRoute selectedTask, int route, int index){
        int currentBenefit = operationGain[route][selectedTask.getID()-startNodes.length-1][selectedTask.getEarliestTime()];
        int firstSailing;
        int secondSailing;
        int newSailing;
        if(index==0 && index==vesselRoutes.get(route).size()-1){
            firstSailing = SailingTimes[route][EarliestStartingTimeForVessel[route]][startNodes[route]][selectedTask.getID()-1];
            secondSailing = 0;
            newSailing = 0;
        }else if(index==0){
            firstSailing = SailingTimes[route][EarliestStartingTimeForVessel[route]][startNodes[route]][selectedTask.getID()-1];
            secondSailing = SailingTimes[route][selectedTask.getEarliestTime()][selectedTask.getID()-1][vesselRoutes.get(route).get(index+1).getID()-1];
            newSailing = SailingTimes[route][EarliestStartingTimeForVessel[route]][startNodes[route]][vesselRoutes.get(route).get(index+1).getID()-1];
        }else if(index==vesselRoutes.get(route).size()-1){
            firstSailing = SailingTimes[route][vesselRoutes.get(route).get(index-1).getEarliestTime()][vesselRoutes.get(route).get(index).getID()-1][selectedTask.getID()-1];
            secondSailing = 0;
            newSailing = 0;
        }else{
            secondSailing = SailingTimes[route][selectedTask.getEarliestTime()][selectedTask.getID()-1][vesselRoutes.get(route).get(index+1).getID()-1];
            firstSailing = SailingTimes[route][vesselRoutes.get(route).get(index-1).getEarliestTime()][vesselRoutes.get(route).get(index).getID()-1][selectedTask.getID()-1];
            newSailing = SailingTimes[route][vesselRoutes.get(route).get(index-1).getEarliestTime()][vesselRoutes.get(route).get(index-1).getID()-1][vesselRoutes.get(route).get(index+1).getID()-1];
        }
        routeOperationGain[route] = routeOperationGain[route] - currentBenefit;
        routeSailingCost[route] = routeSailingCost[route] - firstSailing - secondSailing + newSailing;
        objValue = objValue - currentBenefit - firstSailing - secondSailing + newSailing;
    }

    public int checkprecedenceOfEarliestLNS(int o, int earliestTemp, int earliestPO, int routeConnectedPrecedence){
        int precedenceOf=precedenceALNS[o-1-startNodes.length][1];
        if(precedenceOf!=0 && routeConnectedPrecedence!=-1) {
            earliestTemp = Math.max(earliestTemp, earliestPO + TimeVesselUseOnOperation[routeConnectedPrecedence][precedenceOf - 1 - startNodes.length][earliestPO-1]);
        }
        return earliestTemp;
    }

    public int[] checkSimultaneousOfTimesLNS(int o, int earliestTemp, int latestTemp, int earliestSO, int latestSO){
        int simultaneousOf=simALNS[o-1-startNodes.length][1];
        //System.out.println("Within check simultaneous of times");
        if(simultaneousOf!=0) {
            earliestTemp = Math.max(earliestTemp, earliestSO);
            latestTemp = Math.min(latestTemp, latestSO);
            //System.out.println("earliest and latest dependent on sim of operation");
        }
        return new int[]{earliestTemp,latestTemp};
    }

    public void insertFeasibleDict(int o,InsertionValues iv, int benefitIncreaseTemp, Map<Integer,List<InsertionValues>> allFeasibleInsertions){
        if (allFeasibleInsertions.get(o) == null) {
            allFeasibleInsertions.put(o, new ArrayList<>() {{
                add(iv);
            }});
        }
        else {
            if(benefitIncreaseTemp>=allFeasibleInsertions.get(o).get(0).getBenenefitIncrease()){
                allFeasibleInsertions.get(o).add(0,iv);
            }
            else if(benefitIncreaseTemp<allFeasibleInsertions.get(o).get(allFeasibleInsertions.get(o).size()-1).getBenenefitIncrease()){
                allFeasibleInsertions.get(o).add(allFeasibleInsertions.get(o).size(),iv);
            }
            else{
                for (int i=1;i<allFeasibleInsertions.get(o).size();i++){
                    if(benefitIncreaseTemp<allFeasibleInsertions.get(o).get(i-1).getBenenefitIncrease()&&
                            benefitIncreaseTemp>=allFeasibleInsertions.get(o).get(i).getBenenefitIncrease()){
                        allFeasibleInsertions.get(o).add(i,iv);
                        break;
                    }
                }
            }
        }
    }

    public boolean checkPPlacementLNS(int o, int n, int v, int pOFRoute, int pOFIndex){
        //FIRST THING THUESDAY: FIX THIS IN LSNINSERT!
        int precedenceOf=precedenceALNS[o-startNodes.length-1][1];
        if(precedenceOf!=0 && pOFRoute!=-1){
            //PrecedenceValues pOver=precedenceOverOperations.get(precedenceOf);
            int precedenceOverOver=precedenceALNS[precedenceOf-startNodes.length-1][1];
            PrecedenceValues pOverOver=precedenceOverOperations.get(precedenceOverOver);
            int pOverSim=simALNS[precedenceOf-1-startNodes.length][1];
            ConnectedValues pOverSimValues= simultaneousOp.get(pOverSim);
            if(pOFRoute==v){
                if(pOFIndex>=n){
                    return false;
                }
            }
            if(pOverOver !=null){
                if(pOverOver.getRoute()==v){
                    if(pOverOver.getIndex()>=n){
                        return false;
                    }
                }
            }
            if(pOverSimValues !=null){
                if(pOverSimValues.getRoute()==v){
                    if(pOverSimValues.getIndex()>=n){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Boolean checkSimultaneousFeasibleLNS(Map<Integer,ConnectedValues> simOps, int o, int v, int insertIndex, int earliestTemp,
                                                int latestTemp, Map<Integer, ConnectedValues> simultaneousOp, int [][] simALNS,
                                                int [] startNodes, int [][][][] SailingTimes, int [][][] TimeVesselUseOnOperation,
                                                List<List<OperationInRoute>> vesselroutes, int simARoute, int simAIndex){
        if(simOps!=null) {
            for (ConnectedValues op : simOps.values()) {
                //System.out.println("trying to insert operation " + o + " in position " + insertIndex+ " , " +op.getOperationObject().getID() + " simultaneous operation in route " +v);
                ArrayList<ArrayList<Integer>> earliest_change = ConstructionHeuristic.checkChangeEarliestSim(earliestTemp,insertIndex,v,o,op.getOperationObject().getID(),startNodes,
                        SailingTimes, TimeVesselUseOnOperation, simultaneousOp, vesselroutes);
                if (!earliest_change.isEmpty()) {
                    for (ArrayList<Integer> connectedTimes : earliest_change) {
                        //System.out.println(connectedTimes.get(0) + " , " + connectedTimes.get(1) + " earliest change");
                        if (connectedTimes.get(0) > connectedTimes.get(1)) {
                            //System.out.println("Sim infeasible");
                            return false;
                        }
                        ConnectedValues conOp = simultaneousOp.get(op.getConnectedOperationID());
                        //System.out.println(conOp.getOperationObject().getID() + " Con op operation ID " + conOp.getRoute() + " route index");
                        if(simALNS[o-startNodes.length-1][1] != 0 &&
                                simARoute == conOp.getRoute()){
                            System.out.println("Sim a route: "+simARoute+" conOp route: "+conOp.getRoute());
                            //System.out.println(simultaneousOp.get(simALNS[o-startNodes.length-1][1]).getRoute() + " Con op of o ID" );
                            //System.out.println(conOp.getIndex());
                            System.out.println("Sim a index: "+ simAIndex + " conOP index: "+conOp.getIndex() + " insertindex: "+
                                    insertIndex+ " op index: "+op.getIndex());
                            if((simAIndex - conOp.getIndex() > 0 &&
                                    insertIndex - op.getIndex() < 0) || (simAIndex -
                                    conOp.getIndex() <= 0 && insertIndex - op.getIndex() > 0)){
                                //System.out.println("Sim infeasible");
                                return false;
                            }
                        }
                    }
                }
                ArrayList<ArrayList<Integer>> latest_change = ConstructionHeuristic.checkChangeLatestSim(latestTemp,insertIndex,v,o,op.getOperationObject().getID(),startNodes,
                        SailingTimes,TimeVesselUseOnOperation,simultaneousOp,vesselroutes,precedenceOverOperations,precedenceOfOperations);
                if(!latest_change.isEmpty()){
                    for(ArrayList<Integer> connectedTimes : latest_change){
                        //System.out.println(connectedTimes.get(0) + " , " + connectedTimes.get(1) + " latest change");
                        if(connectedTimes.get(0) > connectedTimes.get(1)){
                            //System.out.println("Sim infeasible");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public Boolean checkPOverFeasibleLNS(Map<Integer,PrecedenceValues> precedenceOver, int o, int insertIndex,int earliest,
                                         int []startNodes, int[][][] TimeVesselUseOnOperation, int nTimePeriods,
                                         int [][][][] SailingTimes, List<List<OperationInRoute>> vesselroutes,
                                         Map<Integer,PrecedenceValues> precedenceOfOperations,
                                         List<Map<Integer,PrecedenceValues>> precedenceOverRoutes) {
        if(precedenceOver!=null) {
            for (PrecedenceValues pValues : precedenceOver.values()) {
                int route = pValues.getRoute();
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                if (secondOr != null && !unroutedTasks.contains(secondOr)) {
                    PrecedenceValues connectedOpPValues = precedenceOfOperations.get(secondOr.getID());
                    System.out.println("first operation: "+firstOr.getID());
                    int routeConnectedOp = connectedOpPValues.getRoute();
                    if (routeConnectedOp == pValues.getRoute()) {
                        continue;
                    }
                    int precedenceIndex = pValues.getIndex();
                    if (insertIndex <= precedenceIndex) {
                        int change = ConstructionHeuristic.checkChangeEarliest(earliest, insertIndex, route, precedenceIndex, pValues.getOperationObject().getEarliestTime(), o,
                                startNodes,TimeVesselUseOnOperation,SailingTimes, vesselroutes);
                        if (change!=0) {
                            int t= firstOr.getEarliestTime()+change-1;
                            if(t>nTimePeriods-1){
                                t=nTimePeriods-1;
                            }
                            int newESecondOr=firstOr.getEarliestTime() + TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                                    [t] + change;
                            if(newESecondOr>secondOr.getEarliestTime()) {
                                if (newESecondOr > secondOr.getLatestTime()) {
                                    //System.out.println("NOT PRECEDENCE OVER FEASIBLE EARLIEST/LATEST P-OPERATION "+firstOr.getID());
                                    //System.out.println("Precedence over infeasible");
                                    return false;
                                }
                                else{
                                    int secondOrIndex= connectedOpPValues.getIndex();
                                    if (!checkPOverFeasibleLNS(precedenceOverRoutes.get(routeConnectedOp),secondOr.getID(),secondOrIndex,newESecondOr,
                                            startNodes,TimeVesselUseOnOperation,nTimePeriods, SailingTimes, vesselroutes, precedenceOfOperations,precedenceOverRoutes)){
                                        //System.out.println("Precedence over infeasible");
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }


    public List<List<OperationInRoute>> copyVesselroutesRI(List<List<OperationInRoute>> vesselroutes) {
        List<List<OperationInRoute>> old_vesselroutes = new ArrayList<List<OperationInRoute>>();
        for (List<OperationInRoute> vesselroute : vesselroutes) {
            List<OperationInRoute> route = new ArrayList<>();
            if (vesselroute != null) {
                for (OperationInRoute operationInRoute : vesselroute) {
                    OperationInRoute op = new OperationInRoute(operationInRoute.getID(), operationInRoute.getEarliestTime(), operationInRoute.getLatestTime());
                    route.add(op);
                }
            }
            old_vesselroutes.add(route);
        }
        return old_vesselroutes;
    }

    public void runRelocateLSO(String method){
        if(method.equals("relocate")) {
            relocateAll();
        }
        else if(method.equals("precedence")){
            insertPrecedenceOf();
        }
        else if(method.equals("simultaneous")){
            insertSimultaneous();
        }
        ObjectiveValues ov= ConstructionHeuristic.calculateObjective(vesselRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,SailingCostForVessel,
                EarliestStartingTimeForVessel, operationGainGurobi, new int[vesselRoutes.size()],new int[vesselRoutes.size()],0, simALNS,bigTasksALNS);
        objValue=ov.getObjvalue();
        routeSailingCost=ov.getRouteSailingCost();
        routeOperationGain=ov.getRouteBenefitGain();
    }


    public void printInitialSolution(int[] vessseltypes){

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
        System.out.println("\nCONSOLIDATED DICTIONARY:");
        for(Map.Entry<Integer, ConsolidatedValues> entry : consolidatedOperations.entrySet()){
            ConsolidatedValues cv = entry.getValue();
            int key = entry.getKey();
            System.out.println("new entry in consolidated dictionary:");
            System.out.println("Key "+key);
            System.out.println("big task placed? "+cv.getConsolidated());
            System.out.println("small tasks placed? "+cv.getSmallTasks());
            System.out.println("small route 1 "+cv.getConnectedRoute1());
            System.out.println("small route 2 "+cv.getConnectedRoute2());
            System.out.println("route consolidated task "+cv.getConsolidatedRoute()+"\n");

        }
        System.out.println(" ");
        System.out.println("SIMULTANEOUS DICTIONARY");
        for(Map.Entry<Integer, ConnectedValues> entry : simultaneousOp.entrySet()){
            ConnectedValues simOp = entry.getValue();
            System.out.println("Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                    simOp.getRoute() + " with index: " + simOp.getIndex());
        }
        System.out.println("PRECEDENCE OVER DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOverOperations.entrySet()){
            PrecedenceValues presOverOp = entry.getValue();
            System.out.println("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                    presOverOp.getRoute() + " with index: " + presOverOp.getIndex());
        }
        System.out.println("PRECEDENCE OF DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfOperations.entrySet()){
            PrecedenceValues presOfOp = entry.getValue();
            System.out.println("Precedence of operation: " + presOfOp.getOperationObject().getID() + " in route: " +
                    presOfOp.getRoute() + " with index: " + presOfOp.getIndex());
        }
    }





    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes = new int[]{1, 2, 4, 5};
        int[] startnodes = new int[]{1, 2, 3, 4};
        DataGenerator dg = new DataGenerator(vesseltypes, 5, startnodes,
                "test_instances/20-5_e_m.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),startnodes.length);
        //PrintData.printOperationGain(dg.getOperationGain(), startnodes.length);
        //PrintData.printSailingTimes(dg.getSailingTimes(),4,27, 5);
        ConstructionHeuristic a = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(), dg.getSimultaneousALNS(), dg.getBigTasksALNS(), dg.getTimeWindowsForOperations(),dg.getOperationGainGurobi());
        a.createSortedOperations();
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);

        LS_operators LSO = new LS_operators(dg.getOperationsForVessel(), vesseltypes, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), a.getRouteSailingCost(), a.getRouteOperationGain(),
                dg.getStartNodes(), dg.getSimultaneousALNS(),dg.getPrecedenceALNS(), dg.getBigTasksALNS(), a.getOperationGain(), a.getVesselroutes(),a.getUnroutedTasks(),
                a.getPrecedenceOverOperations(), a.getPrecedenceOfOperations(), a.getSimultaneousOp(),
                a.getSimOpRoutes(), a.getPrecedenceOfRoutes(), a.getPrecedenceOverRoutes(), a.getConsolidatedOperations(),dg.getOperationGainGurobi());
        //List<List<OperationInRoute>> new_vesselroutes = LSO.two_relocate(a.vesselroutes,1,3,4,0,startnodes,a.getUnroutedTasks());
        //List<List<OperationInRoute>> new_vesselroutes = LSO.searchAll(a.vesselroutes, a.getUnroutedTasks());
        LSO.printLSOSolution(vesseltypes);

        RelocateInsertFeasible RI = new RelocateInsertFeasible(dg.getOperationsForVessel(), vesseltypes, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), a.getRouteSailingCost(), a.getRouteOperationGain(),
                dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), a.getOperationGain(),
                a.getUnroutedTasks(), a.getVesselroutes(), a.getPrecedenceOverOperations(), a.getPrecedenceOfOperations(), a.getSimultaneousOp(),
                a.getSimOpRoutes(), a.getPrecedenceOfRoutes(), a.getPrecedenceOverRoutes(), a.getConsolidatedOperations(),dg.getOperationGainGurobi());

        //RI.relocateInsert(new_vesselroutes,startnodes,a.getUnroutedTasks());
        RI.relocateAll();
        RI.insertPrecedenceOf();


    }

    public int[] getRouteSailingCost() {
        return routeSailingCost;
    }

    public int[] getRouteOperationGain() {
        return routeOperationGain;
    }

    public List<OperationInRoute> getUnroutedTasks() {
        return unroutedTasks;
    }

    public List<List<OperationInRoute>> getVesselRoutes() {
        return vesselRoutes;
    }
}


