import java.io.FileNotFoundException;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.IntStream;

public class LS_operators {
    private int[][] OperationsForVessel;
    private int[] vesseltypes;
    private int[][][][] SailingTimes;
    private int[][][] TimeVesselUseOnOperation;
    private int[] SailingCostForVessel;
    private int[] EarliestStartingTimeForVessel;
    private int[] startNodes;
    private int[][] twIntervals;
    private int nTimePeriods;
    private int[] routeSailingCost;
    private int[] routeOperationGain;
    private int[][][] operationGain;
    private int[][][] operationGainGurobi;
    private int objValue;
    private int[][] simALNS;
    private int[][] precedenceALNS;
    private int[][] bigTasksALNS;
    private List<List<OperationInRoute>> vesselroutes;
    private List<OperationInRoute> unroutedTasks;
    //map for operations that are connected with precedence. ID= operation number. Value= Precedence value.
    private Map<Integer, PrecedenceValues> precedenceOverOperations;
    private Map<Integer, PrecedenceValues> precedenceOfOperations;
    //List for operations that are connected as simultaneous operations. ID= operation number. Value= Simultaneous value.
    private Map<Integer, ConnectedValues> simultaneousOp;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;
    private List<Map<Integer, PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer, PrecedenceValues>> precedenceOverRoutes;
    private Map<Integer, ConsolidatedValues> consolidatedOperations;
    private Boolean print=false;
    private int count1RL=0;
    private int count2RL=0;
    private int count1EX=0;
    private int count2EX=0;
    private int countNormalInsertion=0;


    public LS_operators(int[][] OperationsForVessel, int[] vesseltypes, int[][][][] SailingTimes,
                                int[][][] TimeVesselUseOnOperation, int[] SailingCostForVessel, int[] EarliestStartingTimeForVessel,
                                int[][] twIntervals, int[] routeSailingCost, int[] routeOperationGain, int[] startNodes, int[][] simALNS, int [][] precedenceALNS,
                                int[][] bigTasksALNS, int[][][] operationGain, List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks,
                                Map<Integer, PrecedenceValues> precedenceOverOperations, Map<Integer, PrecedenceValues> precedenceOfOperations,
                                Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, ConnectedValues>> simOpRoutes,
                                List<Map<Integer, PrecedenceValues>> precedenceOfRoutes, List<Map<Integer, PrecedenceValues>> precedenceOverRoutes,
                                Map<Integer, ConsolidatedValues> consolidatedOperations, int[][][] operationGainGurobi) {
        this.OperationsForVessel = OperationsForVessel;
        this.vesseltypes = vesseltypes;
        this.SailingTimes = SailingTimes;
        this.TimeVesselUseOnOperation = TimeVesselUseOnOperation;
        this.EarliestStartingTimeForVessel = EarliestStartingTimeForVessel;
        this.SailingCostForVessel = SailingCostForVessel;
        this.twIntervals = twIntervals;
        this.routeSailingCost = routeSailingCost;
        this.routeOperationGain = routeOperationGain;
        this.objValue = IntStream.of(routeOperationGain).sum() - IntStream.of(routeSailingCost).sum();
        this.startNodes = startNodes;
        this.simALNS = simALNS;
        this.precedenceALNS = precedenceALNS;
        this.bigTasksALNS = bigTasksALNS;
        this.operationGain = operationGain;
        this.precedenceOverOperations = precedenceOverOperations;
        this.precedenceOfOperations = precedenceOfOperations;
        this.simultaneousOp = simultaneousOp;
        this.simOpRoutes = simOpRoutes;
        this.precedenceOfRoutes = precedenceOfRoutes;
        this.precedenceOverRoutes = precedenceOverRoutes;
        this.consolidatedOperations = consolidatedOperations;
        this.vesselroutes = vesselroutes;
        this.unroutedTasks = unroutedTasks;
        this.nTimePeriods = SailingTimes[0].length;
        this.operationGainGurobi=operationGainGurobi;
    }


    public List<List<OperationInRoute>> copyVesselroutes(List<List<OperationInRoute>> vesselroutes) {
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


    public Boolean one_relocate(int vessel, int pos1, int pos2, int[] startnodes) {

        if (pos1 == pos2) {
            //System.out.println("Cannot relocate task to same position");
            return false;
        }

        if (simultaneousOp.get(vesselroutes.get(vessel).get(pos1).getID()) != null) {
            //System.out.println("Cannot relocate simultaneousOp with this method");
            return false;
        }

        int nStartnodes = startnodes.length;
        int old_pos1_dist;
        int old_pos2_dist;
        int oldObjValue = objValue;

        //Tracker gammel tid for både pos 1 og pos 2 (21.02)
        if (pos1 == 0) {

            old_pos1_dist = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID() - 1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID() - 1][vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];
            //+ TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            //System.out.println(old_pos1_dist + " Old pos 1 dist");

        } else if (pos1 == vesselroutes.get(vessel).size() - 1) {
            int tIndex=vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime() +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()];
            if(tIndex>59){
                tIndex=59;
            }
            old_pos1_dist = SailingTimes[vessel][tIndex]
                    [vesselroutes.get(vessel).get(pos1 - 1).getID() - 1][vesselroutes.get(vessel).get(pos1).getID() - 1];
            //+ TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            //System.out.println(old_pos1_dist + " Old pos 1 dist");

        } else {
            int tInd= vesselroutes.get(vessel).get(pos1).getEarliestTime() + TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes]
                    [vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            if(tInd>59){
                tInd=59;
            }
            old_pos1_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime() +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1 - 1).getID() - 1][vesselroutes.get(vessel).get(pos1).getID() - 1] +
                    SailingTimes[vessel][tInd][vesselroutes.get(vessel).get(pos1).getID() - 1][vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];
            //+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
            //System.out.println(old_pos1_dist + " Old pos 1 dist");


        }
        if (pos2 == 0) {
            old_pos2_dist = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos2).getID() - 1];
            //System.out.println(old_pos2_dist + " old pos 2 dist");
        } else if (pos2 == vesselroutes.get(vessel).size() - 1) {
            old_pos2_dist = 0;
            //System.out.println(old_pos2_dist + " old pos 2 dist");
        } else {
            old_pos2_dist = SailingTimes[vessel][Math.min(nTimePeriods - 1, vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime() +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2 - 1).getID() - 1 - nStartnodes][Math.min(nTimePeriods - 1, vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime())])]
                    [vesselroutes.get(vessel).get(pos2 - 1).getID() - 1][vesselroutes.get(vessel).get(pos2).getID() - 1];
            //System.out.println(old_pos2_dist + " old pos 2 dist");

        }
        // Save old vesselroutes list
        List<List<OperationInRoute>> new_vesselroutes = copyVesselroutes(vesselroutes);

        // Commit changes in route
        OperationInRoute toMove = new_vesselroutes.get(vessel).get(pos1);
        new_vesselroutes.get(vessel).remove(pos1);
        new_vesselroutes.get(vessel).add(pos2, toMove);

        int new_pos1_dist;
        int new_pos2_dist;
        int new_second_sailing;
        if (pos1 < pos2) {
            if (pos1 == 0) {
                new_pos1_dist = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][vesselroutes.get(vessel).get(0).getID() - 1];
                //System.out.println(new_pos1_dist + " new pos 1 dist");
            } else {
                new_pos1_dist = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime() +
                        TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos1 - 1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()]]
                        [new_vesselroutes.get(vessel).get(pos1 - 1).getID() - 1][new_vesselroutes.get(vessel).get(pos1).getID() - 1];
                //System.out.println(new_pos1_dist + " new pos 1 dist");
            }
        }
        else {
            if (pos1 == vesselroutes.get(vessel).size() - 1) {
                new_pos1_dist = 0;
                //System.out.println(new_pos1_dist + " new pos 1 dist");
            }
            else {
                //System.out.println("vessel "+vessel+" pos 2 "+pos2);
                //System.out.println("vessel "+vessel+" pos 1 "+pos1);
                //System.out.println(new_vesselroutes.get(vessel).get(pos2).getID());
                //System.out.println(new_vesselroutes.get(vessel).get(pos2).getEarliestTime());
                int tIndex=new_vesselroutes.get(vessel).get(pos2).getEarliestTime() +
                        TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel).get(pos2).getEarliestTime()-1];
                if(tIndex>59){
                    tIndex=59;
                }
                new_pos1_dist = SailingTimes[vessel][tIndex]
                        [new_vesselroutes.get(vessel).get(pos1).getID() - 1][new_vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];
                //System.out.println(new_pos1_dist + " new pos 1 dist");
            }
        }
        if (pos2 == 0) {
            new_second_sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][new_vesselroutes.get(vessel).get(pos2).getID() - 1];
            new_pos2_dist = new_second_sailing +
                    SailingTimes[vessel][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2).getID() - 1 - nStartnodes][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel).get(pos1).getEarliestTime())])]
                            [new_vesselroutes.get(vessel).get(pos2).getID() - 1][new_vesselroutes.get(vessel).get(pos2 + 1).getID() - 1];
            // +TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][new_vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            //System.out.println(new_pos2_dist + " New pos 2 dist");
        } else if (pos2 == new_vesselroutes.get(vessel).size() - 1) {
            new_second_sailing = SailingTimes[vessel][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime() +
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2 - 1).getID() - 1 - nStartnodes][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime())])]
                    [new_vesselroutes.get(vessel).get(pos2 - 1).getID() - 1][new_vesselroutes.get(vessel).get(pos2).getID() - 1];
            new_pos2_dist = new_second_sailing;
            //System.out.println(new_pos2_dist + " New pos 2 dist");

        } else {
            int tIndex=new_vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime() +
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2 - 1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime()];
            if(tIndex>59){
                tIndex=59;
            }
            new_second_sailing = SailingTimes[vessel][tIndex]
                    [new_vesselroutes.get(vessel).get(pos2 - 1).getID() - 1][new_vesselroutes.get(vessel).get(pos2).getID() - 1];
            new_pos2_dist = new_second_sailing +
                    SailingTimes[vessel][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel).get(pos1).getEarliestTime() + TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2).getID() - 1 - nStartnodes]
                            [Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel).get(pos1).getEarliestTime())])][new_vesselroutes.get(vessel).get(pos2).getID() - 1][new_vesselroutes.get(vessel).get(pos2 + 1).getID() - 1];
            //System.out.println(new_pos2_dist + " New pos 2 dist");
        }

        //Calculating objective function changes
        int first_delta;
        int second_delta;
        if (pos1 < pos2) {
            first_delta = -(old_pos1_dist) + (new_pos1_dist);
            second_delta = -(old_pos2_dist) + (new_pos2_dist);
        } else {
            first_delta = -(old_pos2_dist) + (new_pos2_dist);
            second_delta = -(old_pos1_dist) + (new_pos1_dist);
        }
        int sailingdelta = first_delta + second_delta;

        OperationInRoute oldLastOpVessel1 = vesselroutes.get(vessel).get(vesselroutes.get(vessel).size() - 1);
        int vessel1Gain = 0;
        int old1Gain = 0;
        if (pos1 != vesselroutes.get(vessel).size() - 1 &&
                pos2 != new_vesselroutes.get(vessel).size() - 1) {
            old1Gain = operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1];
            vessel1Gain = -old1Gain + operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][0];
        } else {
            OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel).get(new_vesselroutes.get(vessel).size() - 1);
            old1Gain = operationGain[vessel][newLastOpVessel1.getID() - startnodes.length - 1][newLastOpVessel1.getEarliestTime() - 1]
                    + operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1];
            vessel1Gain = -old1Gain + operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][0] +
                    operationGain[vessel][newLastOpVessel1.getID() - startnodes.length - 1][0];
        }
        int delta = -sailingdelta + vessel1Gain;


        if (delta <= 0) {
            return false;
        }

        //Rearranging the new_vesselroutes lists to the original order
        OperationInRoute move = new_vesselroutes.get(vessel).get(pos2);
        new_vesselroutes.get(vessel).remove(pos2);
        new_vesselroutes.get(vessel).add(pos1, move);

        // Relocating the elements in the original vesselroutes list to secure the same object structure
        OperationInRoute toMoveOriginal = vesselroutes.get(vessel).get(pos1);
        vesselroutes.get(vessel).remove(pos1);
        ConstructionHeuristic.updateIndexesRemoval(vessel,pos1,vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);

        if (pos1 == 0) {
            int sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][vessel][vesselroutes.get(vessel).get(pos1).getID() - 1] + 1;
            vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(twIntervals[vesselroutes.get(vessel).get(pos1).getID() - 1 - startnodes.length][0], sailing));
            updateEarliest(Math.max(twIntervals[vesselroutes.get(vessel).get(pos1).getID() - 1 - startnodes.length][0], sailing), 0, vessel, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes, twIntervals, "none");
        }
        else if (pos1 == vesselroutes.get(vessel).size()) {
            vesselroutes.get(vessel).get(pos1 - 1).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel).get(pos1 - 1).getID() - 1 - startnodes.length][1], nTimePeriods));
            updateLatest(vesselroutes.get(vessel).get(pos1 - 1).getLatestTime(), pos1 - 1, vessel, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes, twIntervals, "none");
        } else if (pos1 == vesselroutes.get(vessel).size() - 1) {
            vesselroutes.get(vessel).get(pos1).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel).get(pos1).getID() - 1 - startnodes.length][1], nTimePeriods));

            updateLatest(vesselroutes.get(vessel).get(pos1).getLatestTime(), pos1, vessel, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes, twIntervals, "oneRelocate");
        } else {
            updateEarliest(vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime(), pos1 - 1, vessel, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes, twIntervals, "oneRelocate");

            updateLatest(vesselroutes.get(vessel).get(pos1 + 1).getLatestTime(), pos1 + 1, vessel, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes, twIntervals, "oneRelocate");
        }

        int[] startingTimes;
        if(precedenceALNS[toMoveOriginal.getID()-1-startNodes.length][1]!=0) {
                PrecedenceValues precedenceOverValues = precedenceOverOperations.get(precedenceALNS[toMoveOriginal.getID() - 1 - startNodes.length][1]);
            startingTimes = findInsertionCosts(vessel, pos2, toMoveOriginal, -1, -1, precedenceOverValues.getOperationObject().getEarliestTime(),
                    precedenceOverValues.getRoute(), -1, precedenceOverValues.getIndex(), -1,
                    vesselroutes);
        }
        else{
            startingTimes = findInsertionCosts(vessel, pos2, toMoveOriginal, -1, -1, -1, -1, -1, -1, -1,
                    vesselroutes);
        }

        if (startingTimes == null) {
            //System.out.println("Infeasible one-relocate");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            //printLSOSolution(vesseltypes);
            return false;
        }

        vesselroutes.get(vessel).add(pos2, toMoveOriginal);
        ConstructionHeuristic.updateIndexesInsertion(vessel,pos2,vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);

        vesselroutes.get(vessel).get(pos2).setEarliestTime(Math.max(startingTimes[0], twIntervals[toMoveOriginal.getID() - startnodes.length - 1][0]));
        vesselroutes.get(vessel).get(pos2).setLatestTime(Math.min(startingTimes[1], twIntervals[toMoveOriginal.getID() - startnodes.length - 1][1]));
        updateEarliest(startingTimes[0], pos2, vessel, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "oneexchange");
        updateLatest(startingTimes[1], pos2, vessel, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "oneexchange");

        //System.out.println("Latest set in find insertion cost "+startingTimes[1]);

        boolean feasible;
        //System.out.println("update con routes");

        int ID2=vesselroutes.get(vessel).get(pos2).getID();
        if(precedenceOfOperations.get(ID2)!=null){
            precedenceOfOperations.get(ID2).setIndex(pos2);
        }
        if(precedenceOverOperations.get(ID2)!=null){
            precedenceOverOperations.get(ID2).setIndex(pos2);
        }


        feasible = updateConRoutes(simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, vessel, vesselroutes, null, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                SailingTimes, twIntervals, EarliestStartingTimeForVessel, startNodes, TimeVesselUseOnOperation);

        if(!feasible){
            //System.out.println("Infeasible one-relocate");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(vessel), pos2, simOpRoutes, precedenceOfOperations, precedenceOverOperations,
                TimeVesselUseOnOperation, startNodes, precedenceOverRoutes, precedenceOfRoutes, simultaneousOp,
                vesselroutes, SailingTimes,twIntervals);

        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(vessel), pos2, TimeVesselUseOnOperation, startNodes, simOpRoutes,
                precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, precedenceOverRoutes,
                vesselroutes, simultaneousOp, SailingTimes,twIntervals);

        updateSimultaneous(simOpRoutes, vessel, simultaneousOp,
                precedenceOverRoutes, precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations,
                precedenceOfOperations, vesselroutes, pos1, pos2, "onerelocate",EarliestStartingTimeForVessel,twIntervals);

        //Checking feasibility:
        if (!checkSolution(vesselroutes, startNodes, TimeVesselUseOnOperation, precedenceOverOperations, simultaneousOp)) {
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }


        int[] newRouteSailingCost = new int[vesselroutes.size()];
        int[] newRouteOperationGain = new int[vesselroutes.size()];
        int newObjValue = 0;
        ConstructionHeuristic.calculateObjective(vesselroutes, TimeVesselUseOnOperation, startNodes, SailingTimes, SailingCostForVessel, EarliestStartingTimeForVessel, operationGainGurobi, newRouteSailingCost,
                newRouteOperationGain, newObjValue, simALNS, bigTasksALNS);

        if (newObjValue > oldObjValue) {
        routeSailingCost = newRouteSailingCost;
        routeOperationGain = newRouteOperationGain;
        objValue = newObjValue;
        }

        else {
            //System.out.println("Objective not improved");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }
        count1RL+=1;

        System.out.println("One Relocate performed");
        //printLSOSolution(vesseltypes);
        return true;

    }


    public Boolean two_relocate(int vessel1, int vessel2, int pos1, int pos2, int[] startnodes) {
        if ((vessel1 == vessel2) ||
                !(ConstructionHeuristic.containsElement(vesselroutes.get(vessel1).get(pos1).getID(), OperationsForVessel[vessel2]))) {
            //System.out.println("Cannot relocate task to same vessel with this method, or new vessel cannot perform task");
            return false;
        }

        if (simultaneousOp.get(vesselroutes.get(vessel1).get(pos1).getID()) != null) {
            //System.out.println("Cannot relocate simultaneousOp with this method");
            return false;
        }

        int oldObjValue = objValue;
        int nStartnodes = startnodes.length;
        int old_vessel1_dist;
        int old_vessel2_dist;

        //Tracker gammel tid for vessel 1
        if (vesselroutes.get(vessel1).size() == 1) {
            old_vessel1_dist = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID() - 1];
            //System.out.println(old_vessel1_dist + " Old vessel 1 dist");
        } else if (pos1 == 0) {
            old_vessel1_dist = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID() - 1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() -1+
                            TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID() - 1 - nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()-1]]
                            [vesselroutes.get(vessel1).get(pos1).getID() - 1][vesselroutes.get(vessel1).get(pos1 + 1).getID() - 1];

            //System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else if (pos1 == vesselroutes.get(vessel1).size() - 1) {
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1][vesselroutes.get(vessel1).get(pos1).getID() - 1];
            //System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else {
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1][vesselroutes.get(vessel1).get(pos1).getID() - 1] +
                    SailingTimes[vessel1][Math.min(nTimePeriods-1,vesselroutes.get(vessel1).get(pos1).getEarliestTime() -1+ TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID() - 1 - nStartnodes]
                            [Math.min(nTimePeriods-1,vesselroutes.get(vessel1).get(pos1).getEarliestTime()-1)])][vesselroutes.get(vessel1).get(pos1).getID() - 1][vesselroutes.get(vessel1).get(pos1 + 1).getID() - 1];

            //System.out.println(old_vessel1_dist + " Old vessel 1 dist");
        }

        if (vesselroutes.get(vessel2) == null || vesselroutes.get(vessel2).size() == 0) {
            old_vessel2_dist = 0;
            //System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if (pos2 == 0) {
            old_vessel2_dist = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID() - 1];
            //System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if (pos2 == vesselroutes.get(vessel2).size()) {
            old_vessel2_dist = 0;
            //System.out.println(old_vessel2_dist + " Old vessel2 dist her");
        } else {
            old_vessel2_dist = SailingTimes[vessel2][Math.min(nTimePeriods - 1, vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime() -1+ TimeVesselUseOnOperation[vessel2]
                    [vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1])]
                    [vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1][vesselroutes.get(vessel2).get(pos2).getID() - 1];
            //System.out.println(old_vessel2_dist + " Old vessel2 dist her");
        }

        //Tracking new times for vessel 1
        int new_vessel1_dist;

        if (vesselroutes.get(vessel1).size() == 1) {
            new_vessel1_dist = 0;
            //System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if (pos1 == 0) {
            new_vessel1_dist = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1 + 1).getID() - 1];
            //System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if (pos1 == vesselroutes.get(vessel1).size() - 1) {
            new_vessel1_dist = 0;
            //System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else {
            new_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1][vesselroutes.get(vessel1).get(pos1 + 1).getID() - 1];
            //System.out.println(new_vessel1_dist + " New vessel1 dist");
        }


        // Make copy of vesselroutes list
        List<List<OperationInRoute>> new_vesselroutes = copyVesselroutes(vesselroutes);

        //Commit change of positions in new vesselroutes list
        OperationInRoute toMove = new_vesselroutes.get(vessel1).get(pos1);
        new_vesselroutes.get(vessel1).remove(pos1);
        new_vesselroutes.get(vessel2).add(pos2, toMove);


        //Use method for insertion to update time of vessel 2
        int new_second_sailing;
        int new_vessel2_dist;

        if (new_vesselroutes.get(vessel2).size() == 1) {
            new_second_sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][new_vesselroutes.get(vessel2).get(pos2).getID() - 1];
            new_vessel2_dist = new_second_sailing;
            //System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2 == 0) {
            new_second_sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][new_vesselroutes.get(vessel2).get(pos2).getID() - 1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][Math.min(nTimePeriods-1,new_second_sailing +
                            TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID() - 1 - nStartnodes][Math.min(nTimePeriods-1,new_second_sailing)])]
                            [new_vesselroutes.get(vessel2).get(pos2).getID() - 1][new_vesselroutes.get(vessel2).get(pos2 + 1).getID() - 1];
            //System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2 == new_vesselroutes.get(vessel2).size() - 1) {
            new_second_sailing = SailingTimes[vessel2][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1 - nStartnodes]
                            [Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1)])][new_vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1]
                    [new_vesselroutes.get(vessel2).get(pos2).getID() - 1];
            new_vessel2_dist = new_second_sailing;
            //System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else {
            new_second_sailing = SailingTimes[vessel2][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1 - nStartnodes][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1)])]
                    [new_vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1][new_vesselroutes.get(vessel2).get(pos2).getID() - 1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1 +
                            new_second_sailing + TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID() - 1 - nStartnodes]
                            [Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime() + new_second_sailing-1)])]
                            [new_vesselroutes.get(vessel2).get(pos2).getID() - 1][new_vesselroutes.get(vessel2).get(pos2 + 1).getID() - 1];
            //System.out.println(new_vessel2_dist + " New vessel2 dist");
        }

        //Calculating delta changes for sailing and operation gain
        int vessel1_delta = -old_vessel1_dist + new_vessel1_dist;
        int vessel2_delta = -old_vessel2_dist + new_vessel2_dist;

        OperationInRoute oldLastOpVessel1 = vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size() - 1);
        OperationInRoute newLastOpVessel2 = new_vesselroutes.get(vessel2).get(new_vesselroutes.get(vessel2).size() - 1);
        int vessel1Gain = 0;
        int vessel2Gain = 0;
        int old1Gain = 0;
        int old2Gain = 0;
        if (pos1 != vesselroutes.get(vessel1).size() - 1 &&
                pos2 != new_vesselroutes.get(vessel2).size() - 1) {
            OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel1).get(new_vesselroutes.get(vessel1).size() - 1);
            old1Gain = operationGain[vessel1][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1] +
                    operationGain[vessel1][vesselroutes.get(vessel1).get(pos1).getID() - startnodes.length - 1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() - 1];
            vessel1Gain = -old1Gain + operationGain[vessel1][newLastOpVessel1.getID() - startnodes.length - 1][Math.min(nTimePeriods - 1, newLastOpVessel1.getEarliestTime() + vessel1_delta - 1)];

            OperationInRoute oldLastOpVessel2 = vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size() - 1);
            old2Gain = operationGain[vessel2][oldLastOpVessel2.getID() - startnodes.length - 1][oldLastOpVessel2.getEarliestTime() - 1];
            vessel2Gain = -old2Gain + operationGain[vessel2][newLastOpVessel2.getID() - startnodes.length - 1][Math.min(nTimePeriods - 1, newLastOpVessel2.getEarliestTime() + vessel2_delta - 1)] +
                    operationGain[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID() - startnodes.length - 1][0];
        } else {
            if (!new_vesselroutes.get(vessel1).isEmpty()) {
                OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel1).get(new_vesselroutes.get(vessel1).size() - 1);
                old1Gain = operationGain[vessel1][newLastOpVessel1.getID() - startnodes.length - 1][newLastOpVessel1.getEarliestTime() - 1]
                        + operationGain[vessel1][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1];
                vessel1Gain = -old1Gain + operationGain[vessel1][newLastOpVessel1.getID() - startnodes.length - 1][Math.min(nTimePeriods - 1, Math.max(0, newLastOpVessel1.getEarliestTime() + vessel1_delta - 1))];
            } else {
                old1Gain = operationGain[vessel1][vesselroutes.get(vessel1).get(pos1).getID() - startnodes.length - 1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() - 1];
                vessel1Gain = -old1Gain;
            }
            if (vesselroutes.get(vessel2) != null) {
                if (vesselroutes.get(vessel2).size() != 0) {
                    OperationInRoute oldLastOpVessel2 = vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size() - 1);
                    old2Gain = operationGain[vessel2][newLastOpVessel2.getID() - startnodes.length - 1][newLastOpVessel2.getEarliestTime() - 1]
                            + operationGain[vessel2][oldLastOpVessel2.getID() - startnodes.length - 1][oldLastOpVessel2.getEarliestTime() - 1];
                    vessel2Gain = -old2Gain + operationGain[vessel2][oldLastOpVessel2.getID() - startnodes.length - 1][0] +
                            operationGain[vessel2][newLastOpVessel2.getID() - startnodes.length - 1][Math.min(nTimePeriods - 1, newLastOpVessel2.getEarliestTime() + vessel2_delta - 1)];
                }
            } else {
                old2Gain = operationGain[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID() - startnodes.length - 1][0];
                vessel2Gain = old2Gain;
            }
        }
        int delta = -vessel1_delta - vessel2_delta + vessel1Gain + vessel2Gain;

        if (delta <= 0) {
            return false;
        }

        // Relocating the elements in the original vesselroutes list to secure the same object structure
        OperationInRoute toMoveOriginal = vesselroutes.get(vessel1).get(pos1);
        vesselroutes.get(vessel1).remove(pos1);
        ConstructionHeuristic.updateIndexesRemoval(vessel1, pos1, vesselroutes,
                simultaneousOp, precedenceOverOperations,
                precedenceOfOperations);

        //Rearranging the new_vesselroutes lists to the original order
        OperationInRoute toMoveBack = new_vesselroutes.get(vessel2).get(pos2);
        new_vesselroutes.get(vessel2).remove(pos2);
        new_vesselroutes.get(vessel1).add(pos1, toMoveBack);


        if (vesselroutes.get(vessel1).isEmpty()) {

        } else if (pos1 == 0) {
            int sailing = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startNodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID() - 1] + 1;
            vesselroutes.get(vessel1).get(pos1).setEarliestTime(Math.max(twIntervals[vesselroutes.get(vessel1).get(pos1).getID() - 1 - startnodes.length][0], sailing));
            updateEarliest(sailing, 0, vessel1, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes, twIntervals, "twoRelocate");
        } else if (pos1 == vesselroutes.get(vessel1).size()) {
            vesselroutes.get(vessel1).get(pos1 - 1).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1 - startnodes.length][1], nTimePeriods));
            updateLatest(vesselroutes.get(vessel1).get(pos1 - 1).getLatestTime(), pos1 - 1, vessel1, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes, twIntervals, "twoRelocate");
        } else if (pos1 == vesselroutes.get(vessel1).size() - 1) {
            vesselroutes.get(vessel1).get(pos1).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel1).get(pos1).getID() - 1 - startnodes.length][1], nTimePeriods));
            updateLatest(vesselroutes.get(vessel1).get(pos1).getLatestTime(), pos1, vessel1, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes, twIntervals, "twoRelocate");
        } else {
            updateEarliest(vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime(), pos1 - 1, vessel1, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes, twIntervals, "twoRelocate");
            updateLatest(vesselroutes.get(vessel1).get(pos1 + 1).getLatestTime(), pos1 + 1, vessel1, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes, twIntervals, "twoRelocate");
        }

        int[] startingTimes;

        if(precedenceALNS[toMoveOriginal.getID()-1-startNodes.length][1]!=0) {
            PrecedenceValues precedenceOverValues = precedenceOverOperations.get(precedenceALNS[toMoveOriginal.getID() - 1 - startNodes.length][1]);
            startingTimes = findInsertionCosts(vessel2, pos2, toMoveOriginal, -1, -1, precedenceOverValues.getOperationObject().getEarliestTime(),
                    precedenceOverValues.getRoute(), -1, precedenceOverValues.getIndex(), -1,
                    vesselroutes);
        }
        else{
            startingTimes = findInsertionCosts(vessel2, pos2, toMoveOriginal, -1, -1, -1, -1, -1, -1, -1,
                    vesselroutes);
        }

        if(startingTimes == null){
            //System.out.println("Infeasible insertion");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            //printLSOSolution(vesseltypes);
            return false;
        }

        //Inserting in vesselroutes
        if (vesselroutes.get(vessel2) == null) {
            vesselroutes.set(vessel2, new ArrayList<>() {{
                add(pos2, toMoveOriginal);
            }});
        } else {
            vesselroutes.get(vessel2).add(pos2, toMoveOriginal);
            ConstructionHeuristic.updateIndexesInsertion(vessel2,pos2,vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
        }

        vesselroutes.get(vessel2).get(pos2).setEarliestTime(Math.max(twIntervals[vesselroutes.get(vessel2).get(pos2).getID() - 1 - startnodes.length][0], startingTimes[0]));
        vesselroutes.get(vessel2).get(pos2).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel2).get(pos2).getID() - 1 - startnodes.length][1], startingTimes[1]));
        updateEarliest(startingTimes[0], pos2, vessel2, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "tworelocate");
        updateLatest(startingTimes[1], pos2, vessel2, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "tworelocate");


        List<Integer> updatedRoutes = new ArrayList<>() {{
            add(vessel2);
        }};

        boolean feasible;
        int ID2=vesselroutes.get(vessel2).get(pos2).getID();
        if(precedenceOfOperations.get(ID2)!=null){
            precedenceOfOperations.get(ID2).setIndex(pos2);
        }
        if(precedenceOverOperations.get(ID2)!=null){
            precedenceOverOperations.get(ID2).setIndex(pos2);
        }
        feasible = updateConRoutes(simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, vessel2, vesselroutes, null, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                SailingTimes, twIntervals, EarliestStartingTimeForVessel, startNodes, TimeVesselUseOnOperation);
        if(!feasible) {
            //System.out.println("Infeasible two-relocate");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }
        //ConstructionHeuristic.updateIndexesInsertion(vessel2, pos2, vesselroutes, simultaneousOp, precedenceOverOperations, precedenceOfOperations);
        //ConstructionHeuristic.updateIndexesRemoval(vessel1, pos1, vesselroutes, simultaneousOp, precedenceOverOperations, precedenceOfOperations);
        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(vessel2), pos2, simOpRoutes, precedenceOfOperations, precedenceOverOperations,
                TimeVesselUseOnOperation, startNodes, precedenceOverRoutes, precedenceOfRoutes, simultaneousOp, vesselroutes, SailingTimes,twIntervals);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(vessel2), pos2, TimeVesselUseOnOperation, startNodes, simOpRoutes,
                precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, precedenceOverRoutes, vesselroutes, simultaneousOp, SailingTimes,twIntervals);

        updateSimultaneous(simOpRoutes, vessel2, simultaneousOp,
                precedenceOverRoutes, precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations,
                precedenceOfOperations, vesselroutes, pos1, pos2, "noUpdate",EarliestStartingTimeForVessel,twIntervals);


        //Checking feasibility:
        if (!checkSolution(vesselroutes, startNodes, TimeVesselUseOnOperation, precedenceOverOperations, simultaneousOp)) {
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        //Checking profitability

        int[] newRouteSailingCost = new int[vesselroutes.size()];
        int[] newRouteOperationGain = new int[vesselroutes.size()];
        int newObjValue = 0;
        ConstructionHeuristic.calculateObjective(vesselroutes,TimeVesselUseOnOperation,startNodes,SailingTimes,SailingCostForVessel,EarliestStartingTimeForVessel,operationGainGurobi,newRouteSailingCost,
                newRouteOperationGain,newObjValue,simALNS,bigTasksALNS);

        if (newObjValue > oldObjValue) {
        routeSailingCost = newRouteSailingCost;
        routeOperationGain = newRouteOperationGain;
        objValue = newObjValue;
        }

        else {
            //System.out.println("Objective not improved");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }
        count2RL+=1;
        System.out.println("Two relocate performed");
        //printLSOSolution(vesseltypes);

        if (bigTasksALNS[vesselroutes.get(vessel2).get(pos2).getID() - 1 - startNodes.length] != null &&
                bigTasksALNS[vesselroutes.get(vessel2).get(pos2).getID() - startNodes.length - 1][0] == vesselroutes.get(vessel2).get(pos2).getID()) {
            if (consolidatedOperations.get(vesselroutes.get(vessel2).get(pos2).getID()) != null) {
                consolidatedOperations.get(vesselroutes.get(vessel2).get(pos2).getID()).setConsolidatedRoute(vessel2);
            }
        }

        return true;

    }


    public Boolean one_exchange(int vessel, int cur_pos, int new_pos, int[] startnodes) {
        if (cur_pos == new_pos) {
            return false;
        }
        if (simultaneousOp.get(vesselroutes.get(vessel).get(cur_pos).getID()) != null ||
                simultaneousOp.get(vesselroutes.get(vessel).get(new_pos).getID()) != null) {
            //System.out.println("Cannot exchange simultaneousOp with this method");
            return false;
        }

        int pos1 = Math.min(cur_pos, new_pos);
        int pos2 = Math.max(cur_pos, new_pos);

        int nStartnodes = startnodes.length;
        int old_pos1_dist;
        int old_pos2_dist;
        int oldObjValue = objValue;

        if (pos1 == 0) {
            old_pos1_dist = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID() - 1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()-1 +
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()-1]]
                            [vesselroutes.get(vessel).get(pos1).getID() - 1][vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];
        } else {
            old_pos1_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel).get(pos1 - 1).getID() - 1][vesselroutes.get(vessel).get(pos1).getID() - 1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime() -1+ TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()-1]][vesselroutes.get(vessel).get(pos1).getID() - 1][vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];
        }
        //System.out.println(old_pos1_dist + " Old pos 1 dist");


        if (pos2 == vesselroutes.get(vessel).size() - 1) {
            old_pos2_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime() -1+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel).get(pos2 - 1).getID() - 1][vesselroutes.get(vessel).get(pos2).getID() - 1];

        } else {
            old_pos2_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel).get(pos2 - 1).getID() - 1][vesselroutes.get(vessel).get(pos2).getID() - 1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()-1 + TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID() - 1 - nStartnodes]
                            [vesselroutes.get(vessel).get(pos2).getEarliestTime()-1]][vesselroutes.get(vessel).get(pos2).getID() - 1][vesselroutes.get(vessel).get(pos2 + 1).getID() - 1];
        }
        //System.out.println(old_pos2_dist + " Old pos 2 dist");

        //Make new copy of the vesselroutes list
        List<List<OperationInRoute>> new_vesselroutes = copyVesselroutes(vesselroutes);

        // Commit the change in the routes
        OperationInRoute toMove1 = new_vesselroutes.get(vessel).get(pos1);
        OperationInRoute toMove2 = new_vesselroutes.get(vessel).get(pos2);
        new_vesselroutes.get(vessel).remove(pos1);
        new_vesselroutes.get(vessel).add(pos2, toMove1);
        new_vesselroutes.get(vessel).remove(pos2 - 1);
        new_vesselroutes.get(vessel).add(pos1, toMove2);

        int new_first_dist;
        int new_second_dist;
        int new_first_sailing;
        int new_second_sailing;

        int second_pos1_sailing;

        if (pos1 == 0) {
            new_first_sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][new_vesselroutes.get(vessel).get(pos1).getID() - 1];
            int tIndex=new_vesselroutes.get(vessel).get(pos2).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos1).getID() - nStartnodes - 1][new_vesselroutes.get(vessel).get(pos2).getEarliestTime()-1];
            if(tIndex>59){
                tIndex=59;
            }
            second_pos1_sailing = SailingTimes[vessel][tIndex]
                    [new_vesselroutes.get(vessel).get(pos1).getID() - 1][new_vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];
            new_first_dist = new_first_sailing + second_pos1_sailing;
            //System.out.println(new_first_dist + " new first dist");

        } else {
            new_first_sailing = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime() -1+
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos1 - 1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()-1]]
                    [new_vesselroutes.get(vessel).get(pos1 - 1).getID() - 1][new_vesselroutes.get(vessel).get(pos1).getID() - 1];
            second_pos1_sailing = SailingTimes[vessel][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel).get(pos2).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel).get(pos2).getEarliestTime()-1)])]
                    [new_vesselroutes.get(vessel).get(pos1).getID() - 1][new_vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];
            new_first_dist = new_first_sailing + second_pos1_sailing;
            //System.out.println(new_first_dist + " new first dist");
        }


        //Assumption: We can use old Timeperiod for the time used on the operation and time used for sailing
        int new_last_sailing;
        if (pos2 == new_vesselroutes.get(vessel).size() - 1) {
            new_second_sailing = SailingTimes[vessel][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2 - 1).getID() - 1 - nStartnodes][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime()-1)])]
                    [new_vesselroutes.get(vessel).get(pos2 - 1).getID() - 1][new_vesselroutes.get(vessel).get(pos2).getID() - 1];
            new_last_sailing = 0;
            new_second_dist = new_second_sailing;
            //System.out.println(new_second_dist + " new second dist");
        } else {
            new_second_sailing = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2 - 1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime()-1]]
                    [new_vesselroutes.get(vessel).get(pos2 - 1).getID() - 1][new_vesselroutes.get(vessel).get(pos2).getID() - 1];
            new_last_sailing = SailingTimes[vessel][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel).get(pos1).getEarliestTime() -1+ TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2).getID() - 1 - nStartnodes]
                    [Math.min(nTimePeriods-1,new_vesselroutes.get(vessel).get(pos1).getEarliestTime()-1)])][new_vesselroutes.get(vessel).get(pos2).getID() - 1][new_vesselroutes.get(vessel).get(pos2 + 1).getID() - 1];
            new_second_dist = new_second_sailing + new_last_sailing;

            //System.out.println(new_second_dist + " new second dist");
        }


        //Calculating objective function differences
        int first_delta = -(old_pos1_dist) + (new_first_dist);
        int second_delta = -(old_pos2_dist) + (new_second_dist);

        int sailingdelta = first_delta + second_delta;


        OperationInRoute oldLastOpVessel1 = vesselroutes.get(vessel).get(vesselroutes.get(vessel).size() - 1);
        OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel).get(new_vesselroutes.get(vessel).size() - 1);
        int vessel1Gain = 0;
        int old1Gain = 0;
        if (pos2 != vesselroutes.get(vessel).size() - 1) {
            old1Gain = operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1];
            vessel1Gain = -old1Gain + operationGain[vessel][newLastOpVessel1.getID() - startnodes.length - 1][0];
        } else {
            old1Gain = operationGain[vessel][newLastOpVessel1.getID() - startnodes.length - 1][newLastOpVessel1.getEarliestTime() - 1] +
                    operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1];
            vessel1Gain = -old1Gain + operationGain[vessel][newLastOpVessel1.getID() - startnodes.length - 1][0]
                    + operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][0];
        }
        int delta = -sailingdelta + vessel1Gain;
        if (delta <= 0) {
            //System.out.println("Objective change negative or unchanged, undesirable relocate. Returning old vesselroutes");
            return false;
        }
        //Rearranging the new_vesselroutes lists to the original order
        OperationInRoute moveBack2 = new_vesselroutes.get(vessel).get(pos1);
        OperationInRoute moveBack1 = new_vesselroutes.get(vessel).get(pos2);
        new_vesselroutes.get(vessel).remove(pos1);
        new_vesselroutes.get(vessel).add(pos2, moveBack2);
        new_vesselroutes.get(vessel).remove(pos2 - 1);
        new_vesselroutes.get(vessel).add(pos1, moveBack1);


        // Relocating the elements in the original vesselroutes list to secure the same object structure
        OperationInRoute toMoveOriginal1 = vesselroutes.get(vessel).get(pos1);
        OperationInRoute toMoveOriginal2 = vesselroutes.get(vessel).get(pos2);
        vesselroutes.get(vessel).remove(pos1);
        vesselroutes.get(vessel).remove(pos2 - 1);

        int[] startingTimes2;
        if(precedenceALNS[toMoveOriginal2.getID()-1-startNodes.length][1]!=0) {
            PrecedenceValues precedenceOverValues = precedenceOverOperations.get(precedenceALNS[toMoveOriginal2.getID() - 1 - startNodes.length][1]);
            startingTimes2 = findInsertionCosts(vessel, pos1, toMoveOriginal2, -1, -1, precedenceOverValues.getOperationObject().getEarliestTime(),
                    precedenceOverValues.getRoute(), -1, precedenceOverValues.getIndex(), -1,
                    vesselroutes);
        }
        else{
            startingTimes2 = findInsertionCosts(vessel, pos1, toMoveOriginal2, -1, -1, -1, -1, -1, -1, -1,
                    vesselroutes);
        }
        if(startingTimes2 == null){
            //System.out.println("Infeasible one-exchange");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        vesselroutes.get(vessel).add(pos1, toMoveOriginal2);

        vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(startingTimes2[0], twIntervals[toMoveOriginal2.getID() - startnodes.length - 1][0]));
        vesselroutes.get(vessel).get(pos1).setLatestTime(Math.min(startingTimes2[1], twIntervals[toMoveOriginal2.getID() - startnodes.length - 1][1]));
        updateEarliest(startingTimes2[0], pos1, vessel, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "oneexchange");
        updateLatest(startingTimes2[1], pos1, vessel, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "oneexchange");

        int[] startingTimes1;
        if(precedenceALNS[toMoveOriginal1.getID()-1-startNodes.length][1]!=0) {
            PrecedenceValues precedenceOverValues = precedenceOverOperations.get(precedenceALNS[toMoveOriginal1.getID() - 1 - startNodes.length][1]);
            startingTimes1 = findInsertionCosts(vessel, pos2, toMoveOriginal1, -1, -1, precedenceOverValues.getOperationObject().getEarliestTime(),
                    precedenceOverValues.getRoute(), -1, precedenceOverValues.getIndex(), -1,
                    vesselroutes);
        }
        else{
            startingTimes1 = findInsertionCosts(vessel, pos2, toMoveOriginal1, -1, -1, -1, -1, -1, -1, -1,
                    vesselroutes);
        }

        if(startingTimes1 == null){
            //System.out.println("Infeasible one-exchange");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        vesselroutes.get(vessel).add(pos2, toMoveOriginal1);

        vesselroutes.get(vessel).get(pos2).setEarliestTime(Math.max(startingTimes1[0], twIntervals[toMoveOriginal1.getID() - startnodes.length - 1][0]));
        vesselroutes.get(vessel).get(pos2).setLatestTime(Math.min(startingTimes1[1], twIntervals[toMoveOriginal1.getID() - startnodes.length - 1][1]));
        updateEarliest(startingTimes1[0], pos2, vessel, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "oneexchange");
        updateLatest(startingTimes1[1], pos2, vessel, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "oneexchange");

        int ID1=vesselroutes.get(vessel).get(pos1).getID();
        int ID2=vesselroutes.get(vessel).get(pos2).getID();
        if(precedenceOfOperations.get(ID1)!=null){
            precedenceOfOperations.get(ID1).setIndex(pos1);
        }
        if(precedenceOfOperations.get(ID2)!=null){
            precedenceOfOperations.get(ID2).setIndex(pos2);
        }
        if(precedenceOverOperations.get(ID1)!=null){
            precedenceOverOperations.get(ID1).setIndex(pos1);
        }
        if(precedenceOverOperations.get(ID2)!=null){
            precedenceOverOperations.get(ID2).setIndex(pos2);
        }
        boolean feasible;
        feasible = updateConRoutes(simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, vessel, vesselroutes, null, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                SailingTimes, twIntervals, EarliestStartingTimeForVessel, startNodes, TimeVesselUseOnOperation);
        if(!feasible) {
            //System.out.println("Infeasible one-exchange");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(vessel), pos2, simOpRoutes, precedenceOfOperations, precedenceOverOperations,
                TimeVesselUseOnOperation, startNodes, precedenceOverRoutes, precedenceOfRoutes, simultaneousOp, vesselroutes, SailingTimes,twIntervals);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(vessel), pos2, TimeVesselUseOnOperation, startNodes, simOpRoutes, precedenceOverOperations,
                precedenceOfOperations, precedenceOfRoutes, precedenceOverRoutes, vesselroutes, simultaneousOp, SailingTimes,twIntervals);

        updateSimultaneous(simOpRoutes, vessel, simultaneousOp,
                precedenceOverRoutes, precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations,
                precedenceOfOperations, vesselroutes, pos1, pos2, "oneExchange",EarliestStartingTimeForVessel,twIntervals);


        //Checking feasibility:
        if (!checkSolution(vesselroutes, startNodes, TimeVesselUseOnOperation, precedenceOverOperations, simultaneousOp)) {
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            //System.out.println("Taking back old solution");
            return false;
        }

        //Checking profitability
        int[] newRouteSailingCost = new int[vesselroutes.size()];
        int[] newRouteOperationGain = new int[vesselroutes.size()];
        int newObjValue = 0;
        ConstructionHeuristic.calculateObjective(vesselroutes, TimeVesselUseOnOperation, startNodes, SailingTimes, SailingCostForVessel, EarliestStartingTimeForVessel, operationGainGurobi, newRouteSailingCost,
                newRouteOperationGain, newObjValue, simALNS, bigTasksALNS);

        if (newObjValue > oldObjValue) {
        routeSailingCost = newRouteSailingCost;
        routeOperationGain = newRouteOperationGain;
        objValue = newObjValue;
        }

        else {
            //System.out.println("Objective not improved");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }
        count1EX+=1;
        System.out.println("Exchange performed");
        //printLSOSolution(vesseltypes);

        return true;
    }


    public Boolean two_exchange(int vessel1, int vessel2, int pos1, int pos2, int[] startnodes) {

        if ((vessel1 == vessel2) ||
                vesselroutes.get(vessel1).size() < 1 || vesselroutes.get(vessel2).size() < 1) {
            //System.out.println("Cannot exchange to same route, or one of the routes contains no tasks");
            return false;
        }

        if (!((ConstructionHeuristic.containsElement(vesselroutes.get(vessel1).get(pos1).getID(), OperationsForVessel[vessel2])) &&
                ConstructionHeuristic.containsElement(vesselroutes.get(vessel2).get(pos2).getID(), OperationsForVessel[vessel1]))) {
            //System.out.println("One of the vessels cannot perform this task");
            return false;
        }

        if (simultaneousOp.get(vesselroutes.get(vessel1).get(pos1).getID()) != null ||
                simultaneousOp.get(vesselroutes.get(vessel2).get(pos2).getID()) != null) {
            //System.out.println("Cannot exchange simultaneousOp with this method");
            return false;
        }
        //System.out.println("try to exchange "+vesselroutes.get(vessel1).get(pos1).getID()+ " and "+vesselroutes.get(vessel2).get(pos2).getID());

        int nStartnodes = startnodes.length;
        int old_vessel1_dist;
        int old_vessel2_dist;
        int oldObjValue = objValue;


        if (vesselroutes.get(vessel1).size() == 1) {
            old_vessel1_dist = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID() - 1];
            //System.out.println(old_vessel1_dist + " Old vessel 1 dist");
        } else if (pos1 == 0) {
            old_vessel1_dist = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID() - 1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()-1 +
                            TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID() - 1 - nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()-1]]
                            [vesselroutes.get(vessel1).get(pos1).getID() - 1][vesselroutes.get(vessel1).get(pos1 + 1).getID() - 1];
            //System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else if (pos1 == vesselroutes.get(vessel1).size() - 1) {
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime() -1+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1][vesselroutes.get(vessel1).get(pos1).getID() - 1];
            //System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else {
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1][vesselroutes.get(vessel1).get(pos1).getID() - 1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() -1+ TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID() - 1 - nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()-1]][vesselroutes.get(vessel1).get(pos1).getID() - 1][vesselroutes.get(vessel1).get(pos1 + 1).getID() - 1];
            //System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        }

        if (vesselroutes.get(vessel2).size() == 1) {
            old_vessel2_dist = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID() - 1];
            //System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if (pos2 == 0) {
            old_vessel2_dist = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID() - 1] +
                    SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()-1 +
                            TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID() - 1 - nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()-1]]
                            [vesselroutes.get(vessel2).get(pos2).getID() - 1][vesselroutes.get(vessel2).get(pos2 + 1).getID() - 1];
            //+ TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1 - nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            //System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if (pos2 == vesselroutes.get(vessel2).size() - 1) {
            old_vessel2_dist = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime() -1+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1][vesselroutes.get(vessel2).get(pos2).getID() - 1];
            // +TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            //System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else {
            old_vessel2_dist = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime() -1 +
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1][vesselroutes.get(vessel2).get(pos2).getID() - 1] +
                    SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()-1 + TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID() - 1 - nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2).getEarliestTime()-1]][vesselroutes.get(vessel2).get(pos2).getID() - 1][vesselroutes.get(vessel2).get(pos2 + 1).getID() - 1];
            //+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()] ;
            //System.out.println(old_vessel2_dist + " Old vessel2 dist");
        }

        //Make copy of the vesselroutes list
        List<List<OperationInRoute>> new_vesselroutes = copyVesselroutes(vesselroutes);

        //Commit exchange - this works as supposed to
        OperationInRoute toMove1 = new_vesselroutes.get(vessel1).get(pos1);
        OperationInRoute toMove2 = new_vesselroutes.get(vessel2).get(pos2);
        new_vesselroutes.get(vessel1).remove(pos1);
        new_vesselroutes.get(vessel2).remove(pos2);
        new_vesselroutes.get(vessel2).add(pos2, toMove1);
        new_vesselroutes.get(vessel1).add(pos1, toMove2);

        // Track new time usage

        int new_vessel1_dist;
        int new_vessel2_dist;
        int new_first_sailing;
        int new_second_sailing;

        if (new_vesselroutes.get(vessel1).size() == 1) {
            new_first_sailing = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][new_vesselroutes.get(vessel1).get(pos1).getID() - 1];
            new_vessel1_dist = new_first_sailing;
            //System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if (pos1 == 0) {
            new_first_sailing = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][new_vesselroutes.get(vessel1).get(pos1).getID() - 1];
            int tIndex=new_vesselroutes.get(vessel2).get(pos2).getEarliestTime() -1+
                    TimeVesselUseOnOperation[vessel1][new_vesselroutes.get(vessel1).get(pos1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel2).get(pos2).getEarliestTime()-1];
            if(tIndex>59){
                tIndex=59;
            }
            new_vessel1_dist = new_first_sailing +
                    SailingTimes[vessel1][tIndex]
                            [new_vesselroutes.get(vessel1).get(pos1).getID() - 1][new_vesselroutes.get(vessel1).get(pos1 + 1).getID() - 1];

            //System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if (pos1 == new_vesselroutes.get(vessel1).size() - 1) {
            new_first_sailing = SailingTimes[vessel1][new_vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel1][new_vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1]]
                    [new_vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1][new_vesselroutes.get(vessel1).get(pos1).getID() - 1];
            new_vessel1_dist = new_first_sailing;
            //System.out.println(new_vessel1_dist + " New vessel1 dist");

        } else {
            new_first_sailing = SailingTimes[vessel1][new_vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime() -1+
                    TimeVesselUseOnOperation[vessel1][new_vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel1).get(pos1 - 1).getEarliestTime()-1]]
                    [new_vesselroutes.get(vessel1).get(pos1 - 1).getID() - 1][new_vesselroutes.get(vessel1).get(pos1).getID() - 1];
            new_vessel1_dist = new_first_sailing +
                    SailingTimes[vessel1][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel2).get(pos2).getEarliestTime() -1 + TimeVesselUseOnOperation[vessel1][new_vesselroutes.get(vessel1).get(pos1).getID() - 1 - nStartnodes]
                            [Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel2).get(pos2).getEarliestTime()-1)])][new_vesselroutes.get(vessel1).get(pos1).getID() - 1][new_vesselroutes.get(vessel1).get(pos1 + 1).getID() - 1];
            //System.out.println(new_vessel1_dist + " New vessel1 dist");
        }

        if (new_vesselroutes.get(vessel2).size() == 1) {
            new_second_sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][new_vesselroutes.get(vessel2).get(pos2).getID() - 1];
            new_vessel2_dist = new_second_sailing;
            //System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2 == 0) {
            new_second_sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][new_vesselroutes.get(vessel2).get(pos2).getID() - 1];
            int tIndex=new_vesselroutes.get(vessel1).get(pos1).getEarliestTime() -1+
                    TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel1).get(pos1).getEarliestTime()-1];
            if(tIndex>59){
                tIndex=59;
            }
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][tIndex]
                            [new_vesselroutes.get(vessel2).get(pos2).getID() - 1][new_vesselroutes.get(vessel2).get(pos2 + 1).getID() - 1];
            //System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2 == new_vesselroutes.get(vessel2).size() - 1) {
            new_second_sailing = SailingTimes[vessel2][new_vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1]]
                    [new_vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1][new_vesselroutes.get(vessel2).get(pos2).getID() - 1];
            new_vessel2_dist = new_second_sailing;
            //System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else {
            new_second_sailing = SailingTimes[vessel2][new_vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime() -1+
                    TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime()-1]]
                    [new_vesselroutes.get(vessel2).get(pos2 - 1).getID() - 1][new_vesselroutes.get(vessel2).get(pos2).getID() - 1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][Math.min(nTimePeriods - 1, new_vesselroutes.get(vessel1).get(pos1).getEarliestTime() -1 + TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID() - 1 - nStartnodes]
                            [new_vesselroutes.get(vessel1).get(pos1).getEarliestTime()-1])][new_vesselroutes.get(vessel2).get(pos2).getID() - 1][new_vesselroutes.get(vessel2).get(pos2 + 1).getID() - 1];

            //System.out.println(new_vessel2_dist + " New vessel2 dist");
        }


        int vessel1_delta = -old_vessel1_dist + new_vessel1_dist;
        int vessel2_delta = -old_vessel2_dist + new_vessel2_dist;

        OperationInRoute oldLastOpVessel1 = vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size() - 1);
        OperationInRoute newLastOpVessel2 = new_vesselroutes.get(vessel2).get(new_vesselroutes.get(vessel2).size() - 1);
        int vessel1Gain = 0;
        int vessel2Gain = 0;
        int old1Gain = 0;
        int old2Gain = 0;
        if (pos1 != vesselroutes.get(vessel1).size() - 1) {
            OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel1).get(new_vesselroutes.get(vessel1).size() - 1);
            old1Gain = operationGain[vessel1][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1] +
                    operationGain[vessel1][vesselroutes.get(vessel1).get(pos1).getID() - startnodes.length - 1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() - 1];
            vessel1Gain = -old1Gain + operationGain[vessel1][newLastOpVessel1.getID() - startnodes.length - 1][Math.min(nTimePeriods - 1, newLastOpVessel1.getEarliestTime() + vessel1_delta - 1)] +
                    operationGain[vessel1][new_vesselroutes.get(vessel1).get(pos1).getID() - startnodes.length - 1][0];
        } else {
            OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel1).get(new_vesselroutes.get(vessel1).size() - 1);
            old1Gain = operationGain[vessel1][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1];
            vessel1Gain = -old1Gain + operationGain[vessel1][new_vesselroutes.get(vessel1).get(pos1).getID() - startnodes.length - 1][0];
        }
        if (pos2 != new_vesselroutes.get(vessel2).size() - 1) {
            OperationInRoute oldLastOpVessel2 = vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size() - 1);
            old2Gain = operationGain[vessel2][oldLastOpVessel2.getID() - startnodes.length - 1][oldLastOpVessel2.getEarliestTime() - 1] +
                    operationGain[vessel2][vesselroutes.get(vessel2).get(pos2).getID() - startnodes.length - 1][vesselroutes.get(vessel2).get(pos2).getEarliestTime()-1];
            vessel2Gain = -old2Gain + operationGain[vessel2][newLastOpVessel2.getID() - startnodes.length - 1][Math.min(nTimePeriods - 1, newLastOpVessel2.getEarliestTime() + vessel2_delta - 1)] +
                    operationGain[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID() - startnodes.length - 1][0];
        } else {
            OperationInRoute oldLastOpVessel2 = vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size() - 1);
            old2Gain = operationGain[vessel2][oldLastOpVessel2.getID() - startnodes.length - 1][oldLastOpVessel2.getEarliestTime() - 1];
            vessel2Gain = -old2Gain + operationGain[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID() - startnodes.length - 1][0];
        }

        int delta = -vessel1_delta - vessel2_delta + vessel1Gain + vessel2Gain;

        if (delta <= 0) {
            //System.out.println("Objective change negative or unchanged, undesirable relocate. Returning old vesselroutes");
            return false;
        }

        //Rearranging the new_vesselroutes lists to the original order
        OperationInRoute toMoveBack2 = new_vesselroutes.get(vessel1).get(pos1);
        OperationInRoute toMoveBack1 = new_vesselroutes.get(vessel2).get(pos2);
        new_vesselroutes.get(vessel1).remove(pos1);
        new_vesselroutes.get(vessel2).remove(pos2);
        new_vesselroutes.get(vessel2).add(pos2, toMoveBack2);
        new_vesselroutes.get(vessel1).add(pos1, toMoveBack1);

        // Exchanging the elements in the original vesselroutes list to secure the same object structure
        OperationInRoute toMoveOriginal1 = vesselroutes.get(vessel1).get(pos1);
        OperationInRoute toMoveOriginal2 = vesselroutes.get(vessel2).get(pos2);
        vesselroutes.get(vessel1).remove(pos1);
        vesselroutes.get(vessel2).remove(pos2);

        int[] startingTimes2;
        if(precedenceALNS[toMoveOriginal2.getID()-1-startNodes.length][1]!=0) {
            PrecedenceValues precedenceOverValues = precedenceOverOperations.get(precedenceALNS[toMoveOriginal2.getID() - 1 - startNodes.length][1]);
            startingTimes2 = findInsertionCosts(vessel1, pos1, toMoveOriginal2, -1, -1, precedenceOverValues.getOperationObject().getEarliestTime(),
                    precedenceOverValues.getRoute(), -1, precedenceOverValues.getIndex(), -1,
                    vesselroutes);
        }
        else{
            startingTimes2 = findInsertionCosts(vessel1, pos1, toMoveOriginal2, -1, -1, -1, -1, -1, -1, -1,
                    vesselroutes);
        }

        if(startingTimes2 == null){
            //System.out.println("Infeasible two-exchange");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        vesselroutes.get(vessel1).add(pos1, toMoveOriginal2);

        vesselroutes.get(vessel1).get(pos1).setEarliestTime(Math.max(startingTimes2[0], twIntervals[toMoveOriginal2.getID() - startnodes.length - 1][0]));
        vesselroutes.get(vessel1).get(pos1).setLatestTime(Math.min(startingTimes2[1], twIntervals[toMoveOriginal2.getID() - startnodes.length - 1][1]));
        updateEarliest(startingTimes2[0], pos1, vessel1, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "twoexchange");
        updateLatest(startingTimes2[1], pos1, vessel1, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "twoexchange");
        if(vesselroutes.get(vessel2).size()!=0){
            updateEarliest(vesselroutes.get(vessel2).get(0).getEarliestTime(), 0, vessel2, TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes,twIntervals,"oneexchange");
            updateLatest(vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getLatestTime(),vesselroutes.get(vessel2).size()-1,vessel2,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes,
                    twIntervals,"oneexchange");
        }

        int[] startingTimes1;
        if(precedenceALNS[toMoveOriginal1.getID()-1-startNodes.length][1]!=0) {
            PrecedenceValues precedenceOverValues = precedenceOverOperations.get(precedenceALNS[toMoveOriginal1.getID() - 1 - startNodes.length][1]);
            startingTimes1 = findInsertionCosts(vessel2, pos2, toMoveOriginal1, -1, -1, precedenceOverValues.getOperationObject().getEarliestTime(),
                    precedenceOverValues.getRoute(), -1, precedenceOverValues.getIndex(), -1,
                    vesselroutes);
        }
        else{
            startingTimes1 = findInsertionCosts(vessel2, pos2, toMoveOriginal1, -1, -1, -1, -1, -1, -1, -1,
                    vesselroutes);
        }

        if(startingTimes1 == null){
            //System.out.println("Infeasible two-exchange");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        vesselroutes.get(vessel2).add(pos2, toMoveOriginal1);

        vesselroutes.get(vessel2).get(pos2).setEarliestTime(Math.max(startingTimes1[0], twIntervals[toMoveOriginal1.getID() - startnodes.length - 1][0]));
        vesselroutes.get(vessel2).get(pos2).setLatestTime(Math.min(startingTimes1[1], twIntervals[toMoveOriginal1.getID() - startnodes.length - 1][1]));
        updateEarliest(startingTimes1[0], pos2, vessel2, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "twoexchange");
        updateLatest(startingTimes1[1], pos2, vessel2, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "twoexchange");

        int ID1=vesselroutes.get(vessel1).get(pos1).getID();
        int ID2=vesselroutes.get(vessel2).get(pos2).getID();
        if(precedenceOfOperations.get(ID1)!=null){
            precedenceOfOperations.get(ID1).setIndex(pos1);
        }
        if(precedenceOfOperations.get(ID2)!=null){
            precedenceOfOperations.get(ID2).setIndex(pos2);
        }
        if(precedenceOverOperations.get(ID1)!=null){
            precedenceOverOperations.get(ID1).setIndex(pos1);
        }
        if(precedenceOverOperations.get(ID2)!=null){
            precedenceOverOperations.get(ID2).setIndex(pos2);
        }
        boolean feasible;
        feasible = updateConRoutes(simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, vessel1, vesselroutes, null, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                SailingTimes, twIntervals, EarliestStartingTimeForVessel, startNodes, TimeVesselUseOnOperation);
        if(!feasible) {
            //System.out.println("Infeasible two-exchange");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(vessel1), pos1, simOpRoutes, precedenceOfOperations, precedenceOverOperations,
                TimeVesselUseOnOperation, startNodes, precedenceOverRoutes, precedenceOfRoutes, simultaneousOp, vesselroutes, SailingTimes,twIntervals);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(vessel1), pos1, TimeVesselUseOnOperation, startNodes, simOpRoutes, precedenceOverOperations, precedenceOfOperations,
                precedenceOfRoutes, precedenceOverRoutes, vesselroutes, simultaneousOp, SailingTimes,twIntervals);

        updateSimultaneous(simOpRoutes, vessel2, simultaneousOp,
                precedenceOverRoutes, precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations,
                precedenceOfOperations, vesselroutes, pos1, pos2, "twoExchange",EarliestStartingTimeForVessel,twIntervals);

        //Checking feasibility:
        if (!checkSolution(vesselroutes, startNodes, TimeVesselUseOnOperation, precedenceOverOperations, simultaneousOp)) {
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        //Checking profitability
        int[] newRouteSailingCost = new int[vesselroutes.size()];
        int[] newRouteOperationGain = new int[vesselroutes.size()];
        int newObjValue = 0;
        ConstructionHeuristic.calculateObjective(vesselroutes, TimeVesselUseOnOperation, startNodes, SailingTimes, SailingCostForVessel, EarliestStartingTimeForVessel, operationGainGurobi, newRouteSailingCost,
                newRouteOperationGain, newObjValue, simALNS, bigTasksALNS);

        if (newObjValue > oldObjValue) {
            routeSailingCost = newRouteSailingCost;
            routeOperationGain = newRouteOperationGain;
            objValue = newObjValue;
        }
        else {
            //System.out.println("Objective not improved");
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        System.out.println("Two exchange performed");
        count2EX+=1;
        //printLSOSolution(vesseltypes);

        if (bigTasksALNS[vesselroutes.get(vessel1).get(pos1).getID() - 1 - startNodes.length] != null &&
                bigTasksALNS[vesselroutes.get(vessel1).get(pos1).getID() - startNodes.length - 1][0] == vesselroutes.get(vessel1).get(pos1).getID()) {
            if (consolidatedOperations.get(vesselroutes.get(vessel1).get(pos1).getID()) != null) {
                consolidatedOperations.get(vesselroutes.get(vessel1).get(pos1).getID()).setConsolidatedRoute(vessel2);
            }
        }

        if (bigTasksALNS[vesselroutes.get(vessel2).get(pos2).getID() - 1 - startNodes.length] != null &&
                bigTasksALNS[vesselroutes.get(vessel2).get(pos2).getID() - startNodes.length - 1][0] == vesselroutes.get(vessel2).get(pos2).getID()) {
            if (consolidatedOperations.get(vesselroutes.get(vessel2).get(pos2).getID()) != null) {
                consolidatedOperations.get(vesselroutes.get(vessel2).get(pos2).getID()).setConsolidatedRoute(vessel2);
            }
        }

        return true;
    }


    public Boolean insert(OperationInRoute unrouted_operation, int vessel, int pos1, int[] startnodes) {


        if (simALNS[unrouted_operation.getID() - 1 - startnodes.length][1] != 0 ||
                simALNS[unrouted_operation.getID() - 1 - startnodes.length][0] != 0) {
            //System.out.println("Cannot insert simultaneousOp with this method");
            return false;
        }

        if (!(ConstructionHeuristic.containsElement(unrouted_operation.getID(), OperationsForVessel[vessel]))) {
            //System.out.println("Vessel cannot perform this task");
            return false;
        }

        int nStartnodes = startnodes.length;
        //Make copy of vesselroutes list
        List<List<OperationInRoute>> new_vesselroutes = copyVesselroutes(vesselroutes);

        //Perform change
        vesselroutes.get(vessel).add(pos1, unrouted_operation);

        //Calculate new time
        int new_second_sailing;
        int new_dist;


        if (vesselroutes.get(vessel).size() == 1) {
            new_second_sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID() - 1];
            new_dist = new_second_sailing;
            //System.out.println(new_dist + " New dist");
        } else if (pos1 == 0) {
            new_second_sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID() - 1];
            if(new_second_sailing +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes][new_second_sailing-1]-1>50){
                CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
                vesselroutes = cv.getVesselRoutes();
                precedenceOfOperations=cv.getPrecedenceOfOperations();
                precedenceOfRoutes=cv.getPrecedenceOfRoutes();
                precedenceOverOperations=cv.getPrecedenceOverOperations();
                precedenceOverRoutes=cv.getPrecedenceOverRoutes();
                simOpRoutes=cv.getSimOpRoutes();
                simultaneousOp=cv.getSimultaneousOp();
                return false;
            }
            new_dist = new_second_sailing +
                    SailingTimes[vessel][new_second_sailing +
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes][new_second_sailing-1]-1]
                            [vesselroutes.get(vessel).get(pos1).getID() - 1][vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];
            //System.out.println(new_dist + " New dist");
        } else if (pos1 == vesselroutes.get(vessel).size() - 1) {
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()-1 +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1 - 1).getID() - 1 - nStartnodes][vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()-1]]
                    [vesselroutes.get(vessel).get(pos1 - 1).getID() - 1][vesselroutes.get(vessel).get(pos1).getID() - 1];
            new_dist = new_second_sailing;
            //System.out.println(new_dist + " New dist");

        } else {
            new_second_sailing = SailingTimes[vessel][Math.min(nTimePeriods - 1, -1+vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime() +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1 - 1).getID() - 1 - nStartnodes][Math.min(vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()-1, nTimePeriods - 1)])]
                    [vesselroutes.get(vessel).get(pos1 - 1).getID() - 1][vesselroutes.get(vessel).get(pos1).getID() - 1];
            if((vesselroutes.get(vessel).get(pos1 + 1).getEarliestTime() + TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes]
                    [vesselroutes.get(vessel).get(pos1 + 1).getEarliestTime()-1]-1)>59){
                CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
                vesselroutes = cv.getVesselRoutes();
                precedenceOfOperations=cv.getPrecedenceOfOperations();
                precedenceOfRoutes=cv.getPrecedenceOfRoutes();
                precedenceOverOperations=cv.getPrecedenceOverOperations();
                precedenceOverRoutes=cv.getPrecedenceOverRoutes();
                simOpRoutes=cv.getSimOpRoutes();
                simultaneousOp=cv.getSimultaneousOp();
                return false;
            }
            new_dist = new_second_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1 + 1).getEarliestTime() + TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes]
                            [vesselroutes.get(vessel).get(pos1 + 1).getEarliestTime()-1]-1][vesselroutes.get(vessel).get(pos1).getID() - 1][vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];
            //System.out.println(new_dist + " New dist");
        }


        if (vesselroutes.get(vessel).size() == 1) {
            int sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startNodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID() - 1];
            vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(sailing, twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));
            vesselroutes.get(vessel).get(pos1).setLatestTime(Math.min(nTimePeriods - 1, twIntervals[unrouted_operation.getID() - 1 - startnodes.length][1]));
        } else if (pos1 == 0) {
            int sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startNodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID() - 1];
            vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(sailing, twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));

            int opTime = TimeVesselUseOnOperation[vessel][unrouted_operation.getID() - 1 - startNodes.length][vesselroutes.get(vessel).get(pos1).getEarliestTime()-1];
            int sailingFromNext = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime() + opTime-1]
                    [unrouted_operation.getID() - 1 - startNodes.length][vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];

            vesselroutes.get(vessel).get(pos1).setLatestTime(Math.min(vesselroutes.get(vessel).get(pos1 + 1).getLatestTime() - opTime - sailingFromNext,
                    twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));

        } else if (pos1 == vesselroutes.get(vessel).size() - 1) {
            vesselroutes.get(vessel).get(pos1).setLatestTime(Math.min(nTimePeriods - 1, twIntervals[unrouted_operation.getID() - 1 - startnodes.length][1]));

            int prevOp = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1 - 1).getID() - 1 - startNodes.length]
                    [vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()-1];
            int sailingToOp = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime() + prevOp-1]
                    [vesselroutes.get(vessel).get(pos1 - 1).getID() - 1][unrouted_operation.getID() - 1];

            vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime() + prevOp + sailingToOp,
                    twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));


        } else {
            int prevOp = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1 - 1).getID() - 1 - startNodes.length]
                    [Math.min(vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime()-1 , nTimePeriods - 1)];
            int sailingToOp = SailingTimes[vessel][Math.min(nTimePeriods - 1, vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime() + prevOp-1)]
                    [vesselroutes.get(vessel).get(pos1 - 1).getID() - 1][unrouted_operation.getID() - 1];

            vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(vesselroutes.get(vessel).get(pos1 - 1).getEarliestTime() + prevOp + sailingToOp,
                    twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));

            int opTime = TimeVesselUseOnOperation[vessel][unrouted_operation.getID() - 1 - startNodes.length][Math.min(nTimePeriods - 1, vesselroutes.get(vessel).get(pos1).getEarliestTime()-1)];
            int sailingFromNext = SailingTimes[vessel][Math.min(nTimePeriods - 1, vesselroutes.get(vessel).get(pos1).getEarliestTime() + opTime-1)]
                    [unrouted_operation.getID() - 1 - startNodes.length][vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];

            vesselroutes.get(vessel).get(pos1).setLatestTime(Math.min(vesselroutes.get(vessel).get(pos1 + 1).getLatestTime() - opTime - sailingFromNext,
                    twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));
        }


        //Checking preliminary feasibility
        if (vesselroutes.get(vessel).get(pos1).getLatestTime() <
                vesselroutes.get(vessel).get(pos1).getEarliestTime()) {
            //System.out.println("Infeasible insertion - earliest time larger than latest time");
            vesselroutes.get(vessel).remove(pos1);
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        ConstructionHeuristic.updateEarliest(vesselroutes.get(vessel).get(pos1).getEarliestTime(), pos1, vessel, TimeVesselUseOnOperation, startNodes,
                SailingTimes, vesselroutes,"local");
        ConstructionHeuristic.updateLatest(vesselroutes.get(vessel).get(pos1).getLatestTime(), pos1, vessel, TimeVesselUseOnOperation, startNodes,
                SailingTimes, vesselroutes,"local",simultaneousOp,precedenceOfOperations,precedenceOverOperations,twIntervals);
        //updateConRoutes(simOpRoutes,precedenceOfRoutes, precedenceOverRoutes, vessel,vesselroutes,null);

        //ConstructionHeuristic.updatePrecedenceOver(checkPrecedence(routeIndex,0),indexInRoute);
        //ConstructionHeuristic.updatePrecedenceOf(checkPrecedence(routeIndex,1),indexInRoute);

        ConstructionHeuristic.updateIndexesInsertion(vessel, pos1, vesselroutes, simultaneousOp, precedenceOverOperations, precedenceOfOperations);
        updateSimultaneous(simOpRoutes, vessel, simultaneousOp,
                precedenceOverRoutes, precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations,
                precedenceOfOperations, vesselroutes, pos1, 1000, "insert",EarliestStartingTimeForVessel,twIntervals);

        //Checking feasibility:
        if (!checkSolution(vesselroutes, startNodes, TimeVesselUseOnOperation, precedenceOverOperations, simultaneousOp)) {
            new_vesselroutes.get(vessel).remove(pos1);
            ConstructionHeuristic.updateIndexesRemoval(vessel, pos1, new_vesselroutes, simultaneousOp, precedenceOverOperations, precedenceOfOperations);
            CopyValues cv = retainOldSolution(new_vesselroutes,consolidatedOperations,simALNS,precedenceALNS,bigTasksALNS,startNodes);
            vesselroutes = cv.getVesselRoutes();
            precedenceOfOperations=cv.getPrecedenceOfOperations();
            precedenceOfRoutes=cv.getPrecedenceOfRoutes();
            precedenceOverOperations=cv.getPrecedenceOverOperations();
            precedenceOverRoutes=cv.getPrecedenceOverRoutes();
            simOpRoutes=cv.getSimOpRoutes();
            simultaneousOp=cv.getSimultaneousOp();
            return false;
        }

        //System.out.println("Unrouted insertion performed");
        unroutedTasks.remove(unroutedTasks.indexOf(unrouted_operation));
        //printLSOSolution(vesseltypes);
        objValue = objValue + (new_dist * SailingCostForVessel[vessel]) + operationGain[vessel]
                [vesselroutes.get(vessel).get(pos1).getID() - 1 - startNodes.length][vesselroutes.get(vessel).get(pos1).getEarliestTime()-1];
        routeSailingCost[vessel] = routeSailingCost[vessel] + (new_dist * SailingCostForVessel[vessel]);
        routeOperationGain[vessel] = routeOperationGain[vessel] + operationGain[vessel]
                [vesselroutes.get(vessel).get(pos1).getID() - 1 - startNodes.length][vesselroutes.get(vessel).get(pos1).getEarliestTime()-1];
        return true;
    }


    public static void updateEarliest(int earliest, int indexInRoute, int routeIndex,
                                      int[][][] TimeVesselUseOnOperation, int[] startNodes, int[][][][] SailingTimes,
                                      List<List<OperationInRoute>> vesselroutes, int[][] twIntervals, String method) {
        int lastEarliest = earliest;
        for (int f = indexInRoute + 1; f < vesselroutes.get(routeIndex).size(); f++) {
            //System.out.println("Index updating: "+f);
            OperationInRoute objectFMinus1 = vesselroutes.get(routeIndex).get(f - 1);
            OperationInRoute objectF = vesselroutes.get(routeIndex).get(f);
            int opTimeFMinus1 = TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID() - startNodes.length - 1]
                    [Math.min(SailingTimes[0].length - 1, objectFMinus1.getEarliestTime()-1)];
            int newTime = Math.max(lastEarliest +
                            SailingTimes[routeIndex][Math.min(SailingTimes[0].length - 1, objectFMinus1.getEarliestTime() + opTimeFMinus1 - 1)][objectFMinus1.getID() - 1][objectF.getID() - 1]
                            + opTimeFMinus1, twIntervals[objectF.getID() - startNodes.length - 1][0]);
            //System.out.println("New time: "+ newTime + " , " + "ID F: " +objectF.getID());
            if (!method.equals("oneexchange") && newTime == objectF.getEarliestTime()) {
                break;
            }
            vesselroutes.get(routeIndex).get(f).setEarliestTime(newTime);
            lastEarliest = newTime;

        }
    }

    public static void updateLatest(int latest, int indexInRoute, int routeIndex,
                                    int[][][] TimeVesselUseOnOperation, int[] startNodes, int[][][][] SailingTimes,
                                    List<List<OperationInRoute>> vesselroutes, int[][] twIntervals, String method) {
        int lastLatest = latest;
        //System.out.println("WITHIN UPDATE LATEST");
        //System.out.println("Last latest time: " + lastLatest);
        for (int k = indexInRoute - 1; k > -1; k--) {
            //System.out.println("Index updating: "+k);
            OperationInRoute objectK = vesselroutes.get(routeIndex).get(k);
            int opTimeK = TimeVesselUseOnOperation[routeIndex][objectK.getID() - startNodes.length - 1]
                    [objectK.getLatestTime() - 1];
            int updateSailingTime = 0;
            //System.out.println("ID operation "+ vesselroutes.get(routeIndex).get(k).getID() + " , " +"Route: "+ routeIndex);

            if (k == vesselroutes.get(routeIndex).size() - 2) {
                updateSailingTime = objectK.getLatestTime();
            }
            if (k < vesselroutes.get(routeIndex).size() - 2) {
                updateSailingTime = objectK.getLatestTime() + opTimeK;
            }
            //System.out.println("Latest already assigned K: "+ objectK.getLatestTime() + " , " + "Potential update latest K: "+
            //        (lastLatest- SailingTimes[routeIndex][updateSailingTime-1][objectK.getID()-1]
            //                [vesselroutes.get(routeIndex).get(k+1).getID()-1] -opTimeK)) ;
            int newTime = Math.min(lastLatest -
                            SailingTimes[routeIndex][Math.min(SailingTimes[0].length - 1, updateSailingTime - 1)][objectK.getID() - 1]
                                    [vesselroutes.get(routeIndex).get(k + 1).getID() - 1] - opTimeK,
                    twIntervals[objectK.getID() - startNodes.length - 1][1]);
            //System.out.println("New time: "+ newTime + " , " + "ID K: " +objectK.getID());

            if (!method.equals("oneexchange") && newTime == objectK.getLatestTime()) {
                break;
            }
            objectK.setLatestTime(newTime);
            //System.out.println(objectK.getLatestTime());
            lastLatest = newTime;
        }
    }

    public static boolean updateSimultaneous(List<Map<Integer, ConnectedValues>> simOpRoutes, int routeIndex,
                                             Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, PrecedenceValues>> precedenceOverRoutes,
                                             List<Map<Integer, PrecedenceValues>> precedenceOfRoutes, int[][][] TimeVesselUseOnOperation,
                                             int[] startNodes, int[][][][] SailingTimes, Map<Integer, PrecedenceValues> precedenceOverOperations,
                                             Map<Integer, PrecedenceValues> precedenceOfOperations, List<List<OperationInRoute>> vesselroutes,
                                             int pos1, int pos2, String function, int[] EarliestStartingTimeForVessel, int[][] twIntervals) {
        boolean feasible = true;
        //System.out.println("New call to update sim");
        if (simOpRoutes.get(routeIndex) != null) {
            for (ConnectedValues sValues : simOpRoutes.get(routeIndex).values()) {

                int cur_earliestTemp = sValues.getOperationObject().getEarliestTime();
                int cur_latestTemp = sValues.getOperationObject().getLatestTime();

                int sIndex = sValues.getIndex();

                if (sValues.getConnectedOperationObject() != null) {
                    ConnectedValues simOp = simultaneousOp.get(sValues.getConnectedOperationObject().getID());
                    //System.out.println("Connected op "+simOp.getOperationObject().getID());
                    int earliestPO;
                    int latestPO;
                    if (simOp.getIndex() == 0) {
                        earliestPO = SailingTimes[simOp.getRoute()][EarliestStartingTimeForVessel[simOp.getRoute()]][simOp.getRoute()][simOp.getOperationObject().getID() - 1];
                    } else {
                        int prevOpTime = TimeVesselUseOnOperation[simOp.getRoute()][vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getID() - startNodes.length - 1]
                                [Math.min(SailingTimes[0].length - 1, vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getEarliestTime()-1)];
                        int SailingPrevOptoOp = SailingTimes[simOp.getRoute()][Math.min(SailingTimes[0].length - 1, vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getEarliestTime() + prevOpTime-1)]
                                [vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getID() - 1][simOp.getOperationObject().getID() - 1];
                        earliestPO = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getEarliestTime() + prevOpTime + SailingPrevOptoOp;
                    }
                    int earliestTemp = Math.max(cur_earliestTemp, earliestPO);
                    if (simOp.getIndex() == vesselroutes.get(simOp.getRoute()).size() - 1) {
                        latestPO = SailingTimes[0].length;
                    } else {
                        int nextOpTime = TimeVesselUseOnOperation[simOp.getRoute()][simOp.getOperationObject().getID() - startNodes.length - 1]
                                [Math.min(SailingTimes[0].length - 1, simOp.getOperationObject().getLatestTime())];
                        //System.out.println(simOp.getOperationObject().getLatestTime() + " latest time of operation " + simOp.getOperationObject().getID());
                        int SailingOptoNextOp =0;
                        latestPO=0;
                        if(simOp.getIndex()==vesselroutes.get(simOp.getRoute()).size()-1){
                            latestPO=twIntervals[vesselroutes.get(simOp.getRoute()).get(simOp.getIndex()).getID()-1-startNodes.length][1];
                        }
                        if(simOp.getIndex()<vesselroutes.get(simOp.getRoute()).size()-1) {
                            SailingOptoNextOp = SailingTimes[simOp.getRoute()][Math.min(SailingTimes[0].length - 1, simOp.getOperationObject().getLatestTime() + nextOpTime - 1)]
                                    [simOp.getOperationObject().getID() - 1][vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() + 1).getID() - 1];
                            latestPO = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() + 1).getLatestTime() - nextOpTime - SailingOptoNextOp;
                        }
                    }
                    //System.out.println("Latest connected "+latestPO);
                    int latestTemp = Math.min(cur_latestTemp, latestPO);

                    if (earliestTemp > cur_earliestTemp) {
                        //System.out.println("first earliest");
                        cur_earliestTemp = earliestTemp;
                        sValues.getOperationObject().setEarliestTime(cur_earliestTemp);
                        //System.out.println("Setting earliest time of operation: " + sValues.getOperationObject().getID() + " to " + sValues.getOperationObject().getEarliestTime() +
                        //        " with index " + sValues.getIndex() + " index in route: " + indexInRoute);
                        feasible = ConstructionHeuristic.updateEarliest(cur_earliestTemp, sValues.getIndex(), routeIndex, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"local");
                        if(!feasible){
                            return feasible;
                        }
                    } else if (earliestTemp > earliestPO) {
                        simOp.getOperationObject().setEarliestTime(cur_earliestTemp);
                        //System.out.println("Setting earliest time of operation: " + simOp.getOperationObject().getID() + " to " + simOp.getOperationObject().getEarliestTime());
                        //System.out.println(simOp.getOperationObject().getEarliestTime() + " setting the earliest time of " + simOp.getOperationObject().getID() );
                        feasible = ConstructionHeuristic.updateEarliest(cur_earliestTemp, simOp.getIndex(), simOp.getRoute(), TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"local");
                        if(!feasible){
                            return feasible;
                        }

                        ConstructionHeuristic.updateSimultaneous(simOpRoutes, simOp.getRoute(), simOp.getIndex(), simultaneousOp, precedenceOverRoutes,
                                precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations, precedenceOfOperations, vesselroutes,twIntervals);

                        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(simOp.getRoute()), simOp.getIndex(), simOpRoutes, precedenceOfOperations,
                                precedenceOverOperations, TimeVesselUseOnOperation, startNodes, precedenceOverRoutes, precedenceOfRoutes, simultaneousOp, vesselroutes, SailingTimes,twIntervals);

                    }
                    if (latestTemp < cur_latestTemp) {
                        cur_latestTemp = latestTemp;
                        //System.out.println("Set latest of "+sValues.getOperationObject().getID()+ " to "+cur_latestTemp);
                        sValues.getOperationObject().setLatestTime(cur_latestTemp);
                        //System.out.println(sValues.getOperationObject().getLatestTime() + " setting the latest time of " + sValues.getOperationObject().getID() );
                        feasible = ConstructionHeuristic.updateLatest(cur_latestTemp, sValues.getIndex(), routeIndex, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"local",
                                                                        simultaneousOp,precedenceOfOperations,precedenceOverOperations,twIntervals);
                        if(!feasible){
                            return feasible;
                        }
                    } else if (latestTemp < latestPO) {
                        //System.out.println(latestTemp + " , " +latestPO + " , " + cur_latestTemp);
                        //System.out.println("Set latest of "+simOp.getOperationObject().getID()+ " to "+cur_latestTemp);
                        simOp.getOperationObject().setLatestTime(cur_latestTemp);
                        //System.out.println(simOp.getOperationObject().getLatestTime() + " setting the latest time of " + simOp.getOperationObject().getID() );

                        feasible = ConstructionHeuristic.updateLatest(cur_latestTemp, simOp.getIndex(), simOp.getRoute(), TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"local",
                                                                        simultaneousOp,precedenceOfOperations,precedenceOverOperations,twIntervals);
                        if(!feasible){
                            return feasible;
                        }
                        ConstructionHeuristic.updateSimultaneous(simOpRoutes, simOp.getRoute(), simOp.getIndex(),
                                simultaneousOp, precedenceOverRoutes, precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations,
                                precedenceOfOperations, vesselroutes,twIntervals);
                        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(simOp.getRoute()), simOp.getIndex(), TimeVesselUseOnOperation, startNodes, simOpRoutes,
                                precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, precedenceOverRoutes, vesselroutes, simultaneousOp, SailingTimes,twIntervals);
                    }
                }
            }
        }
        return feasible;
    }

    public static boolean updateConRoutes(List<Map<Integer, ConnectedValues>> simOpRoutes, List<Map<Integer, PrecedenceValues>> precedenceOfRoutes,
                                          List<Map<Integer, PrecedenceValues>> precedenceOverRoutes, int v,
                                          List<List<OperationInRoute>> vesselroutes, List<Integer> updatedRoutes, Map<Integer, PrecedenceValues> precedenceOverOperations,
                                          Map<Integer, PrecedenceValues> precedenceOfOperations, Map<Integer, ConnectedValues> simultaneousOp,
                                          int[][][][] SailingTimes, int[][] twIntervals, int[] EarliestStartingTimeForVessel,
                                          int[] startNodes, int[][][] TimeVesselUseOnOperation) {
        //System.out.println("In con routes");
        List<Integer> routesToUpdate = new ArrayList<>();
        boolean feasible = true;

        for (int route = 0; route < vesselroutes.size() - 1; route++) {
            if (!simOpRoutes.get(route).isEmpty() || !precedenceOfRoutes.get(route).isEmpty() || !precedenceOverRoutes.get(route).isEmpty()) {
                routesToUpdate.add(route);
            }
        }
        if (updatedRoutes != null) {
            for (int route : updatedRoutes) {
                if (routesToUpdate.contains(route)) {
                    routesToUpdate.remove(Integer.valueOf(route));
                }
            }
        }
        for (int route : routesToUpdate) {
            //System.out.println("update route number "+route);
            if (vesselroutes.get(route) != null && vesselroutes.get(route).size() != 0) {
                //System.out.println("Updating route: " + route);
                int earliest = Math.max(SailingTimes[route][EarliestStartingTimeForVessel[route]][route]
                        [vesselroutes.get(route).get(0).getID() - 1] + 1, twIntervals[vesselroutes.get(route).get(0).getID() - startNodes.length - 1][0]);
                int latest = Math.min(SailingTimes[0].length, twIntervals
                        [vesselroutes.get(route).get(vesselroutes.get(route).size() - 1).getID() - 1 - startNodes.length][1]);
                vesselroutes.get(route).get(0).setEarliestTime(earliest);
                vesselroutes.get(route).get(vesselroutes.get(route).size() - 1).setLatestTime(latest);
                updateEarliest(earliest, 0, route, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "oneexchange");
                //System.out.println("update latest from con routes");
                updateLatest(latest, vesselroutes.get(route).size() - 1, route, TimeVesselUseOnOperation,
                        startNodes, SailingTimes, vesselroutes, twIntervals, "oneexchange");
            }
        }

        /*
        System.out.println("after earliest and latest is set only based on sailing");
        for (int i = 0; i < vesselroutes.size(); i++) {
            System.out.println("VESSELINDEX " + i);
            if (vesselroutes.get(i) != null) {
                for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                    System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                            vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                }
            }
        }

         */

        for (int route : routesToUpdate) {
            if (vesselroutes.get(route) != null && vesselroutes.get(route).size() != 0) {
                feasible = updatePrecedenceOver(precedenceOverRoutes.get(route), 0, simOpRoutes, precedenceOfOperations, precedenceOverOperations, TimeVesselUseOnOperation, startNodes, precedenceOverRoutes,
                        precedenceOfRoutes, simultaneousOp, vesselroutes, SailingTimes,twIntervals);
                if(!feasible){
                    return feasible;
                }

                /*
                System.out.println("before call to update from update con routes");
                for (int i = 0; i < vesselroutes.size(); i++) {
                    System.out.println("VESSELINDEX " + i);
                    if (vesselroutes.get(i) != null) {
                        for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                            System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                    vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                        }
                    }
                }

                 */
                ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(route), vesselroutes.get(route).size() - 1, TimeVesselUseOnOperation, startNodes, simOpRoutes, precedenceOverOperations, precedenceOfOperations,
                        precedenceOfRoutes, precedenceOverRoutes, vesselroutes, simultaneousOp, SailingTimes,twIntervals);
                //System.out.println("New route to update "+route);
                //System.out.println("test con feasible ");
                feasible = updateSimultaneous(simOpRoutes, route, simultaneousOp, precedenceOverRoutes, precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations, precedenceOfOperations,
                        vesselroutes, 0, 0, "conRoutes",EarliestStartingTimeForVessel,twIntervals);
                //System.out.println("feasible true returned from update sim ");
                if(!feasible){
                    return feasible;
                }
            }
        }
        return feasible;
    }


    public int[] findInsertionCosts(int v, int pos2, OperationInRoute operationToInsert, int earliestSO, int latestSO, int earliestPO,
                                    int routeConnectedPrecedence, int routeConnectedSimultaneous, int pOFIndex, int simAIndex,
                                    List<List<OperationInRoute>> vesselroutes) {
        int o = operationToInsert.getID();
        Map<Integer,List<InsertionValues>> allFeasibleInsertions = new HashMap<>();
        int benefitIncrease = -100000;
        int indexInRoute = -1;
        int routeIndex = -1;
        int earliest = -1;
        int latest = nTimePeriods - 1;
        boolean precedenceOverFeasible;
        boolean precedenceOfFeasible;
        boolean simultaneousFeasible;
        int n = pos2-1;
        //System.out.println("ROUTE CONNECTED SIM: "+routeConnectedSimultaneous);
        //System.out.println("route connected simultaneous "+routeConnectedSimultaneous);
        if (DataGenerator.containsElement(o, OperationsForVessel[v]) && v != routeConnectedSimultaneous) {
            //System.out.println("Try vessel "+v);
            if (vesselroutes.get(v) == null || vesselroutes.get(v).isEmpty()) {
                //System.out.println("Empty route");
                //insertion into empty route
                int sailingTimeStartNodeToO = SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                int sailingCost = sailingTimeStartNodeToO * SailingCostForVessel[v];
                int earliestTemp = Math.max(EarliestStartingTimeForVessel[v] + sailingTimeStartNodeToO + 1, twIntervals[o - startNodes.length - 1][0]);
                int latestTemp = Math.min(nTimePeriods, twIntervals[o - startNodes.length - 1][1]);
                earliestTemp = LargeNeighboorhoodSearchInsert.checkprecedenceOfEarliestLNS(o, earliestTemp, earliestPO, routeConnectedPrecedence, precedenceALNS, startNodes,
                        TimeVesselUseOnOperation);
                int[] simultaneousTimesValues = LargeNeighboorhoodSearchInsert.checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO, simALNS,
                        startNodes);
                //System.out.println(simultaneousTimesValues[0] + "," + simultaneousTimesValues[1]+ " sim time");
                earliestTemp = simultaneousTimesValues[0];
                latestTemp = simultaneousTimesValues[1];
                if(precedenceALNS[o-startNodes.length-1][0]!=0){
                    PrecedenceValues pOfOpVals=precedenceOfOperations.get(precedenceALNS[o-startNodes.length-1][0]);
                    if(pOfOpVals!=null){
                        if(earliestTemp+TimeVesselUseOnOperation[v][o-1-startNodes.length][earliestTemp]>pOfOpVals.getOperationObject().getEarliestTime()){
                            return null;
                        }
                    }

                }
                if (earliestTemp <= latestTemp) {
                    //System.out.println("Feasible for empty route");
                    int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost;
                    if (simALNS[o - startNodes.length - 1][1] == 0) {
                        InsertionValues iv = new InsertionValues(benefitIncreaseTemp, 0, v, earliestTemp, latestTemp);

                    } else {
                        if (benefitIncreaseTemp > 0 && benefitIncreaseTemp > benefitIncrease) {
                            benefitIncrease = benefitIncreaseTemp;
                            routeIndex = v;
                            indexInRoute = 0;
                            earliest = earliestTemp;
                            latest = latestTemp;
                        }
                    }
                }
            } else {
                if (n == -1) {
                    int sailingTimeStartNodeToO = SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                    int earliestTemp = Math.max(EarliestStartingTimeForVessel[v] + sailingTimeStartNodeToO + 1, twIntervals[o - startNodes.length - 1][0]);
                    int opTime = TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v] + sailingTimeStartNodeToO];
                    int precedenceOfValuesEarliest = LargeNeighboorhoodSearchInsert.checkprecedenceOfEarliestLNS(o, earliestTemp, earliestPO, routeConnectedPrecedence, precedenceALNS,
                            startNodes, TimeVesselUseOnOperation);
                    earliestTemp = precedenceOfValuesEarliest;
                    if (earliestTemp <= 60) {
                        int sailingTimeOToNext = SailingTimes[v][earliestTemp - 1][o - 1][vesselroutes.get(v).get(0).getID() - 1];
                        int latestTemp = Math.min(vesselroutes.get(v).get(0).getLatestTime() - sailingTimeOToNext - opTime,
                                twIntervals[o - startNodes.length - 1][1]);
                        int[] simultaneousTimesValues = LargeNeighboorhoodSearchInsert.checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO, simALNS,
                                startNodes);
                        earliestTemp = simultaneousTimesValues[0];
                        latestTemp = simultaneousTimesValues[1];
                        int timeIncrease = sailingTimeStartNodeToO + sailingTimeOToNext
                                - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselroutes.get(v).get(0).getID() - 1];
                        int sailingCost = timeIncrease * SailingCostForVessel[v];
                        Boolean pPlacementFeasible = LargeNeighboorhoodSearchInsert.checkPPlacementLNS(o, n+1, v, routeConnectedPrecedence, pOFIndex, precedenceALNS,
                                startNodes, precedenceOverOperations, simALNS, simultaneousOp);
                        if (earliestTemp <= latestTemp && pPlacementFeasible) {
                            if(precedenceALNS[o-startNodes.length-1][0]!=0){
                                PrecedenceValues pOfOpVals=precedenceOfOperations.get(precedenceALNS[o-startNodes.length-1][0]);
                                if(pOfOpVals!=null){
                                    if(earliestTemp+TimeVesselUseOnOperation[v][o-1-startNodes.length][earliestTemp]>pOfOpVals.getOperationObject().getEarliestTime()){
                                        return null;
                                    }
                                }
                            }
                            OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                            int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                            int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, 0, v, o, vesselroutes,
                                    TimeVesselUseOnOperation, startNodes, SailingTimes);
                            int deltaOperationGainLastOperation = 0;
                            if (changedTime > 0) {
                                deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                        operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                            }
                            int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                            if (benefitIncreaseTemp > 0) {
                                precedenceOverFeasible = LargeNeighboorhoodSearchInsert.checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, 0, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods,
                                        SailingTimes, vesselroutes, precedenceOfOperations, precedenceOverRoutes, unroutedTasks);
                                precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, 0, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                        vesselroutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, simultaneousOp,twIntervals);
                                simultaneousFeasible = LargeNeighboorhoodSearchInsert.checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, 0, earliestTemp, latestTemp, simultaneousOp, simALNS,
                                        startNodes, SailingTimes, TimeVesselUseOnOperation, vesselroutes, routeConnectedSimultaneous, simAIndex,precedenceOfOperations,precedenceOverOperations,twIntervals);
                                if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                    //System.out.println("Feasible for position n=0");
                                    /*if (simALNS[o - startNodes.length - 1][1] == 0) {
                                        InsertionValues iv = new InsertionValues(benefitIncreaseTemp, 0, v, earliestTemp, latestTemp);

                                    } else {
                                        if (benefitIncreaseTemp > benefitIncrease) {*/
                                    benefitIncrease = benefitIncreaseTemp;
                                    routeIndex = v;
                                    indexInRoute = 0;
                                    earliest = earliestTemp;
                                    latest = latestTemp;
                                    //}
                                    //}
                                }
                            }
                        }
                    }
                }
                else if (n == vesselroutes.get(v).size() - 1) {
                    //check insertion in last position
                    //System.out.println("Checking this position");
                    int earliestN = vesselroutes.get(v).get(n).getEarliestTime();
                    int operationTimeN = TimeVesselUseOnOperation[v][vesselroutes.get(v).get(n).getID() - 1 - startNodes.length][earliestN - 1];
                    int startTimeSailingTimePrevToO = earliestN + operationTimeN;
                    if (startTimeSailingTimePrevToO >= nTimePeriods) {
                        return null;
                    }
                    int sailingTimePrevToO = SailingTimes[v][startTimeSailingTimePrevToO - 1]
                            [vesselroutes.get(v).get(n).getID() - 1][o - 1];
                    int earliestTemp = Math.max(earliestN + operationTimeN + sailingTimePrevToO
                            , twIntervals[o - startNodes.length - 1][0]);
                    int latestTemp = twIntervals[o - startNodes.length - 1][1];
                    int precedenceOfValuesEarliest = LargeNeighboorhoodSearchInsert.checkprecedenceOfEarliestLNS(o, earliestTemp, earliestPO, routeConnectedPrecedence, precedenceALNS, startNodes, TimeVesselUseOnOperation);
                    earliestTemp = precedenceOfValuesEarliest;
                    if (earliestTemp <= 60) {
                        //System.out.println("Time feasible");
                        int[] simultaneousTimesValues = LargeNeighboorhoodSearchInsert.checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO, simALNS, startNodes);
                        earliestTemp = simultaneousTimesValues[0];
                        latestTemp = simultaneousTimesValues[1];
                        int timeIncrease = sailingTimePrevToO;
                        int sailingCost = timeIncrease * SailingCostForVessel[v];
                        if (earliestTemp <= latestTemp) {
                            if(precedenceALNS[o-startNodes.length-1][0]!=0){
                                PrecedenceValues pOfOpVals=precedenceOfOperations.get(precedenceALNS[o-startNodes.length-1][0]);
                                if(pOfOpVals!=null){
                                    if(earliestTemp+TimeVesselUseOnOperation[v][o-1-startNodes.length][earliestTemp-1]>pOfOpVals.getOperationObject().getEarliestTime()){
                                        return null;
                                    }
                                }

                            }
                            OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                            int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                            int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, n + 1, v, o, vesselroutes,
                                    TimeVesselUseOnOperation, startNodes, SailingTimes);
                            int deltaOperationGainLastOperation = 0;
                            if (changedTime > 0) {
                                deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                        operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                            }
                            int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                            if (benefitIncreaseTemp > 0) {
                                precedenceOverFeasible = LargeNeighboorhoodSearchInsert.checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods, SailingTimes,
                                        vesselroutes, precedenceOfOperations, precedenceOverRoutes, unroutedTasks);
                                precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                        vesselroutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, simultaneousOp,twIntervals);
                                simultaneousFeasible = LargeNeighboorhoodSearchInsert.checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp, simultaneousOp,
                                        simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation, vesselroutes,
                                        routeConnectedSimultaneous, simAIndex, precedenceOfOperations,precedenceOverOperations,twIntervals);
                                if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                    //System.out.println("Feasible for last position in route");
                                    /*if (simALNS[o - startNodes.length - 1][1] == 0) {
                                        InsertionValues iv = new InsertionValues(benefitIncreaseTemp, n + 1, v, earliestTemp, latestTemp);
                                        LargeNeighboorhoodSearchInsert.insertFeasibleDict(o,iv,benefitIncreaseTemp, allFeasibleInsertions);
                                    } else {
                                        System.out.println("Benefit increase temp: "+benefitIncreaseTemp);

                                     */
                                    //if (benefitIncreaseTemp > benefitIncrease) {
                                    benefitIncrease = benefitIncreaseTemp;
                                    routeIndex = v;
                                    indexInRoute = n + 1;
                                    earliest = earliestTemp;
                                    latest = latestTemp;
                                    //}
                                    //}
                                }
                            }
                        }
                    }
                }
                else if (n < vesselroutes.get(v).size() - 1) {
                    //check insertion for all other positions in the route
                    int earliestN = vesselroutes.get(v).get(n).getEarliestTime();
                    int operationTimeN = TimeVesselUseOnOperation[v][vesselroutes.get(v).get(n).getID() - 1 - startNodes.length][earliestN - 1];
                    int startTimeSailingTimePrevToO = earliestN + operationTimeN;
                    int sailingTimePrevToO = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselroutes.get(v).get(n).getID() - 1][o - 1];
                    int earliestTemp = Math.max(earliestN + sailingTimePrevToO + operationTimeN, twIntervals[o - startNodes.length - 1][0]);
                    int precedenceOfValuesEarliest = LargeNeighboorhoodSearchInsert.checkprecedenceOfEarliestLNS(o, earliestTemp, earliestPO, routeConnectedPrecedence, precedenceALNS, startNodes, TimeVesselUseOnOperation);
                    earliestTemp = precedenceOfValuesEarliest;
                    if (earliestTemp <= 60) {
                        if (earliestTemp - 1 < nTimePeriods) {
                            int opTime = TimeVesselUseOnOperation[v][o - 1 - startNodes.length][earliestTemp - 1];
                            int sailingTimeOToNext = SailingTimes[v][Math.min(earliestTemp + opTime - 1, nTimePeriods - 1)][o - 1][vesselroutes.get(v).get(n + 1).getID() - 1];

                            int latestTemp = Math.min(vesselroutes.get(v).get(n + 1).getLatestTime() -
                                    sailingTimeOToNext - opTime, twIntervals[o - startNodes.length - 1][1]);
                            int[] simultaneousTimesValues = LargeNeighboorhoodSearchInsert.checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO, simALNS, startNodes);
                            earliestTemp = simultaneousTimesValues[0];
                            latestTemp = simultaneousTimesValues[1];
                            int sailingTimePrevToNext = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselroutes.get(v).get(n).getID() - 1][vesselroutes.get(v).get(n + 1).getID() - 1];
                            int timeIncrease = sailingTimePrevToO + sailingTimeOToNext - sailingTimePrevToNext;
                            int sailingCost = timeIncrease * SailingCostForVessel[v];
                            Boolean pPlacementFeasible = LargeNeighboorhoodSearchInsert.checkPPlacementLNS(o, n + 1, v, routeConnectedPrecedence, pOFIndex, precedenceALNS,
                                    startNodes, precedenceOverOperations, simALNS, simultaneousOp);
                            if (earliestTemp <= latestTemp && pPlacementFeasible) {
                                if(precedenceALNS[o-startNodes.length-1][0]!=0){
                                    PrecedenceValues pOfOpVals=precedenceOfOperations.get(precedenceALNS[o-startNodes.length-1][0]);
                                    if(pOfOpVals!=null){
                                        if(earliestTemp+TimeVesselUseOnOperation[v][o-1-startNodes.length][earliestTemp]>pOfOpVals.getOperationObject().getEarliestTime()){
                                            return null;
                                        }
                                    }

                                }
                                //System.out.println("p placement feasible and time feasible");
                                OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                                int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute,
                                        n + 1, v, o, vesselroutes, TimeVesselUseOnOperation, startNodes, SailingTimes);
                                int deltaOperationGainLastOperation = 0;
                                if (changedTime > 0) {
                                    deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                            operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                }
                                int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                if (benefitIncreaseTemp > 0) {
                                    //System.out.println("Benefit increase feasible");
                                    int currentLatest = vesselroutes.get(v).get(n).getLatestTime();
                                    simultaneousFeasible = ConstructionHeuristic.checkSOfFeasible(o, v, currentLatest, startNodes, simALNS, simultaneousOp);
                                    if (simultaneousFeasible) {
                                        simultaneousFeasible = LargeNeighboorhoodSearchInsert.checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp,
                                                simultaneousOp, simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation,
                                                vesselroutes, routeConnectedSimultaneous, simAIndex,precedenceOfOperations,precedenceOverOperations,twIntervals);
                                    }
                                    precedenceOverFeasible = LargeNeighboorhoodSearchInsert.checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation,
                                            nTimePeriods, SailingTimes, vesselroutes, precedenceOfOperations, precedenceOverRoutes, unroutedTasks);
                                    precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                            vesselroutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, simultaneousOp,twIntervals);
                                    if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                        //System.out.println("Feasible for index: "+(n+1));
                                        /*if (simALNS[o - startNodes.length - 1][1] == 0) {
                                            InsertionValues iv = new InsertionValues(benefitIncreaseTemp, n + 1, v, earliestTemp, latestTemp);
                                            LargeNeighboorhoodSearchInsert.insertFeasibleDict(o,iv,benefitIncreaseTemp, allFeasibleInsertions);
                                        } else {
                                            if (benefitIncreaseTemp > benefitIncrease) {*/
                                        benefitIncrease = benefitIncreaseTemp;
                                        routeIndex = v;
                                        indexInRoute = n + 1;
                                        earliest = earliestTemp;
                                        latest = latestTemp;
                                        //System.out.println("SIM ROUTE "+routeConnectedSimultaneous+ " Sim index "+simAIndex+
                                        //        " sim earliest: "+earliestSO+" sim latest: "+latestSO);
                                        //}
                                        //}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //System.out.println(benefitIncrease);
        int finalEarliest = earliest;
        int finalLatest = latest;
        int[] startingTimes = new int[]{finalEarliest,finalLatest};
        if(benefitIncrease > -10000){
            //System.out.println("earliest returned "+finalEarliest);
            //System.out.println("latest returned "+finalLatest);
            return startingTimes;
        }
        return null;
    }

    public static boolean updatePrecedenceOver(Map<Integer,PrecedenceValues> precedenceOver, int insertIndex,
                                               List<Map<Integer, ConnectedValues>> simOpRoutes,
                                               Map<Integer,PrecedenceValues> precedenceOfOperations,
                                               Map<Integer,PrecedenceValues> precedenceOverOperations,
                                               int[][][] TimeVesselUseOnOperation, int [] startNodes,
                                               List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                                               List<Map<Integer,PrecedenceValues>> precedenceOfRoutes,
                                               Map<Integer,ConnectedValues> simultaneousOp,
                                               List<List<OperationInRoute>> vesselroutes,
                                               int [][][][] SailingTimes, int[][] twIntervals){
        boolean feasible = true;
        if(precedenceOver!=null){
            for (PrecedenceValues pValues : precedenceOver.values()) {
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                int precedenceIndex =pValues.getIndex();
                if (secondOr != null) {
                    //System.out.println("First or "+firstOr.getID());
                    //System.out.println("second or "+secondOr.getID());
                    PrecedenceValues connectedOpPValues = precedenceOfOperations.get(secondOr.getID());
                    if(connectedOpPValues!=null) {
                        int routeConnectedOp = connectedOpPValues.getRoute();
                        int route = pValues.getRoute();
                        if (routeConnectedOp == pValues.getRoute()) {
                            continue;
                        }
                        int newESecondOr = firstOr.getEarliestTime() + TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                                [firstOr.getEarliestTime() - 1];
                        //System.out.println("first or earliest: "+firstOr.getEarliestTime());
                        //System.out.println("time vessel use on operation: "+TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                        //        [firstOr.getEarliestTime() - 1]);
                        int indexConnected = connectedOpPValues.getIndex();
                        if (insertIndex <= precedenceIndex) {
                            //System.out.println("Index demands update");
                            //System.out.println("Old earliest: " + secondOr.getEarliestTime());
                            //System.out.println("New earliest: " + newESecondOr);
                            //System.out.println(newESecondOr);
                            //System.out.println(secondOr.getEarliestTime());
                            if (secondOr.getEarliestTime() < newESecondOr) {
                                secondOr.setEarliestTime(newESecondOr);
                                feasible = ConstructionHeuristic.updateEarliest(newESecondOr, indexConnected, routeConnectedOp, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes,"local");
                                if(!feasible){
                                    return feasible;
                                }
                                ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(routeConnectedOp), connectedOpPValues.getIndex(), simOpRoutes, precedenceOfOperations,
                                        precedenceOverOperations, TimeVesselUseOnOperation, startNodes, precedenceOverRoutes,
                                        precedenceOfRoutes, simultaneousOp, vesselroutes, SailingTimes,twIntervals);
                                ConstructionHeuristic.updateSimultaneous(simOpRoutes, routeConnectedOp, connectedOpPValues.getIndex(),
                                        simultaneousOp, precedenceOverRoutes, precedenceOfRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes, precedenceOverOperations,
                                        precedenceOfOperations, vesselroutes,twIntervals);
                            }
                            //System.out.println("update earliest because of precedence over");
                        }
                    }
                }
            }
        }
        return true;
    }


    public static Boolean checkSolution(List<List<OperationInRoute>> vesselroutes, int[]startNodes, int[][][] TimeVesselUseOnOperation,
                                        Map<Integer, PrecedenceValues> precedenceOverOperations, Map<Integer, ConnectedValues> simultaneousOp){
        for(List<OperationInRoute> route : vesselroutes){
            if(route != null) {
                for (OperationInRoute operation : route) {
                    if (operation.getEarliestTime() > operation.getLatestTime()) {
                        //System.out.println("Earliest time is larger than latest time, infeasible move");
                        return false;
                    }
                }
            }
        }
        if(!simultaneousOp.isEmpty()){
            for(ConnectedValues op : simultaneousOp.values()){
                OperationInRoute conOp = op.getConnectedOperationObject();
                if(op.getOperationObject().getEarliestTime() != conOp.getEarliestTime() ||
                        op.getOperationObject().getLatestTime() != conOp.getLatestTime()){
                    //System.out.println("Earliest and/or latest time for simultaneous op do not match, infeasible move");
                    return false;
                }
            }
        }
        if(!precedenceOverOperations.isEmpty()){
            for(PrecedenceValues op : precedenceOverOperations.values()){
                OperationInRoute conop = op.getConnectedOperationObject();
                if(conop != null) {
                    if (conop.getEarliestTime() < op.getOperationObject().getEarliestTime() + TimeVesselUseOnOperation[op.getRoute()]
                            [op.getOperationObject().getID() - 1-startNodes.length][op.getOperationObject().getEarliestTime()]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static CopyValues retainOldSolution(List<List<OperationInRoute>> vesselroutes, Map<Integer,ConsolidatedValues> consolidatedOperations,
                                                                 int[][] simALNS, int[][] precALNS, int[][] bigTaskALNS, int[] startNodes){

        List<List<OperationInRoute>> old_vesselroutes = new ArrayList<>();
        Map<Integer, PrecedenceValues> precedenceOverOperationsCopy=new HashMap<>();
        Map<Integer, PrecedenceValues> precedenceOfOperationsCopy=new HashMap<>();
        Map<Integer, ConnectedValues> simultaneousOpCopy=new HashMap<>();
        List<Map<Integer, PrecedenceValues>> precedenceOverRoutesCopy=new ArrayList<>();
        List<Map<Integer, PrecedenceValues>> precedenceOfRoutesCopy=new ArrayList<>();
        List<Map<Integer, ConnectedValues>> simOpRoutesCopy=new ArrayList<>();
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
                    int matrixIndex=ID-1-startNodes.length;
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
                            consolidatedOperations.get(ID).setConsolidatedRoute(vessel);
                            consolidatedOperations.get(ID).setConnectedRoute1(0);
                            consolidatedOperations.get(ID).setConnectedRoute2(0);
                        } else if (bigTaskALNS[matrixIndex][1] == ID) {
                            consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConnectedRoute1(vessel);
                            consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConsolidatedRoute(0);
                        } else if (bigTaskALNS[matrixIndex][2] == ID) {
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
            int connectedPOverTaskID=precALNS[taskID-1-startNodes.length][1];
            if(pvOf.getConnectedOperationObject()==null){
                PrecedenceValues pvOver = precedenceOverOperationsCopy.get(connectedPOverTaskID);
                pvOf.setConnectedOperationObject(pvOver.getOperationObject());
                pvOf.setConnectedRoute(pvOver.getRoute());
            }
        }
        return new CopyValues(old_vesselroutes,precedenceOverOperationsCopy,precedenceOfOperationsCopy,simultaneousOpCopy,
                precedenceOverRoutesCopy,precedenceOfRoutesCopy,simOpRoutesCopy);
    }

    public void twoRelocateAll (){
        boolean changePerformed;
        String lastChange = null;
        boolean done = false;
        while(!done) {
            int count=0;
            for (int vessel = 0; vessel < vesselroutes.size(); vessel++) {
                if (vesselroutes.get(vessel) != null) {
                    for (int vessel2 = 0; vessel2 < vesselroutes.size(); vessel2++) {
                        if (vesselroutes.get(vessel2) == null) {
                            int position = 0;
                            for (int task = 0; task < vesselroutes.get(vessel).size(); task++) {
                                //System.out.println(vessel + " , " + vessel2 + " , " + task + " , " + position);
                                changePerformed = two_relocate(vessel, vessel2, task, position, startNodes);
                                if(changePerformed || lastChange==null){
                                    lastChange = vessel+","+vessel2+","+task+","+position;
                                }else{
                                    if((vessel + "," + vessel2 + "," + task + "," + position).equals(lastChange)){
                                        done = true;
                                    }
                                }
                            }
                        } else {
                            for (int position = 0; position < vesselroutes.get(vessel2).size() + 1; position++) {
                                for (int task = 0; task < vesselroutes.get(vessel).size(); task++) {
                                    //System.out.println(vessel + " , " + vessel2 + " , " + task + " , " + position);
                                    changePerformed = two_relocate(vessel, vessel2, task, position, startNodes);
                                    if(changePerformed || lastChange==null){
                                        lastChange = vessel+","+vessel2+","+task+","+position;
                                        if(changePerformed){
                                            done=true;
                                        }
                                    }else {
                                        if ((vessel + "," + vessel2 + "," + task + "," + position).equals(lastChange)) {
                                            done = true;
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

    public void twoExchangeAll(){
        boolean changePerformed;
        String lastChange = null;
        boolean done = false;
        while(!done) {
            for(int vessel=0; vessel<vesselroutes.size();vessel++) {
                if (vesselroutes.get(vessel) != null) {
                    for (int vessel2 = 0; vessel2 < vesselroutes.size(); vessel2++) {
                        if(vesselroutes.get(vessel2) != null) {
                            for (int position = 0; position < vesselroutes.get(vessel2).size(); position++) {
                                for (int task = 0; task < vesselroutes.get(vessel).size(); task++) {
                                    //System.out.println(vessel + " , " + vessel2 + " , " + task + " , " + position);
                                    changePerformed = two_exchange(vessel, vessel2, task, position, startNodes);
                                    if (changePerformed || lastChange == null) {
                                        lastChange = vessel + "," + vessel2 + "," + task + "," + position;
                                        if(changePerformed){
                                            done=true;
                                        }
                                    } else {
                                        if ((vessel + "," + vessel2 + "," + task + "," + position).equals(lastChange)) {
                                            done = true;
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

    public void oneExchangeAll(){
        boolean changePerformed;
        String lastChange = null;
        boolean done = false;
        while(!done) {
            for(int vessel=0; vessel<vesselroutes.size();vessel++) {
                if (vesselroutes.get(vessel) != null) {
                    for (int position = 0; position < vesselroutes.get(vessel).size(); position++) {
                        for (int task = 0; task < vesselroutes.get(vessel).size(); task++) {
                            //System.out.println(vessel + " , " + task + " , " + position);
                            changePerformed = one_exchange(vessel, task, position, startNodes);
                            if (changePerformed || lastChange == null) {
                                lastChange = vessel + "," + task + "," + position;
                                if(changePerformed){
                                    done=true;
                                }
                            } else {
                                if ((vessel + "," + task + "," + position).equals(lastChange)) {
                                    done = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void oneRelocateAll(){
        boolean changePerformed;
        String lastChange = null;
        boolean done = false;
        while(!done) {
            for(int vessel=0; vessel<vesselroutes.size();vessel++) {
                if (vesselroutes.get(vessel) != null) {
                    for (int position = 0; position < vesselroutes.get(vessel).size(); position++) {
                        for (int task = 0; task < vesselroutes.get(vessel).size(); task++) {
                            //System.out.println(vessel + " , " + task + " , " + position);
                            //System.out.println("try pos1 "+task +" pos2 "+position);
                            changePerformed = one_relocate(vessel, task, position, startNodes);
                            if (changePerformed || lastChange==null) {
                                lastChange = vessel + "," + task + "," + position;
                                if(changePerformed){
                                    done=true;
                                }
                            } else {
                                if ((vessel + "," + task + "," + position).equals(lastChange)) {
                                    done = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public Boolean containsInUnrouted (int unrouted){
        for(OperationInRoute unR : unroutedTasks){
            if (unR.getID()==unrouted){
                return true;
            }
        }
        return false;
    }

    public List<List<OperationInRoute>> insertAll (){
        Map<Integer,List<InsertionValues>> allFeasibleInsertions = new HashMap<>();
        if (!unroutedTasks.isEmpty()) {
            ArrayList<OperationInRoute> insertedTasks=new ArrayList<>();
            for (OperationInRoute task : unroutedTasks) {
                if (simALNS[task.getID() - 1 - startNodes.length][0] == 0 && simALNS[task.getID() - 1 - startNodes.length][1] == 0) {
                    if (precedenceALNS[task.getID() - 1 - startNodes.length][1] != 0) {
                        if (!containsInUnrouted(precedenceALNS[task.getID() - 1 - startNodes.length][1])) {
                            PrecedenceValues precedenceOverValues = precedenceOverOperations.get(precedenceALNS[task.getID() - 1 - startNodes.length][1]);
                            InsertionValues unroutedInsert=findInsertionCostsLS(task, -1, -1, precedenceOverValues.getOperationObject().getEarliestTime(),
                                    precedenceOverValues.getRoute(), -1, precedenceOverValues.getIndex(),
                                    -1, allFeasibleInsertions,  false, null, 0);
                            if (unroutedInsert.getBenenefitIncrease() > 0) {
                                LargeNeighboorhoodSearchInsert.insertOperation(task.getID(), unroutedInsert.getEarliest(),
                                        unroutedInsert.getLatest(), unroutedInsert.getIndexInRoute(), unroutedInsert.getRouteIndex(), precedenceALNS,
                                        startNodes, precedenceOverOperations, precedenceOverRoutes, precedenceOfOperations, precedenceOfRoutes,
                                        simALNS, simultaneousOp, simOpRoutes, vesselroutes, TimeVesselUseOnOperation, SailingTimes, twIntervals);
                                insertedTasks.add(task);
                                countNormalInsertion+=1;
                                System.out.println("Task " + task.getID() + " inserted performed");
                            }
                        }
                    } else {
                        InsertionValues unroutedInsert=findInsertionCostsLS(task, -1, -1, -1,
                                -1, -1, -1, -1, allFeasibleInsertions,false, null, 0);
                        if (unroutedInsert.getBenenefitIncrease() > 0) {
                            LargeNeighboorhoodSearchInsert.insertOperation(task.getID(), unroutedInsert.getEarliest(),
                                    unroutedInsert.getLatest(), unroutedInsert.getIndexInRoute(), unroutedInsert.getRouteIndex(), precedenceALNS,
                                    startNodes, precedenceOverOperations, precedenceOverRoutes, precedenceOfOperations, precedenceOfRoutes,
                                    simALNS, simultaneousOp, simOpRoutes, vesselroutes, TimeVesselUseOnOperation, SailingTimes, twIntervals);
                            insertedTasks.add(task);
                            countNormalInsertion+=1;
                            System.out.println("Task " + task.getID() + " inserted performed");
                        }
                    }
                }
            }
            if(insertedTasks.size()!=0){
                for(OperationInRoute insertedOr : insertedTasks){
                    //System.out.println("Unrouted remove "+insertedOr.getID());
                    unroutedTasks.remove(insertedOr);
                    if(insertedOr.getID()==6){
                        print=true;
                    }
                }
            }
        }
        return vesselroutes;
    }

    public void printLSOSolution(int[] vessseltypes){

        System.out.println("Sailing cost per route: "+ Arrays.toString(routeSailingCost));
        System.out.println("Operation gain per route: "+Arrays.toString(routeOperationGain));
        System.out.println("Objective value: "+(IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum()));
        for (int i=0;i<vesselroutes.size();i++){
            int totalTime=0;
            System.out.println("VESSELINDEX "+i+" VESSELTYPE "+vessseltypes[i]);
            if (vesselroutes.get(i)!=null) {
                for (int o=0;o<vesselroutes.get(i).size();o++) {
                    System.out.println("Operation number: "+vesselroutes.get(i).get(o).getID() + " Earliest start time: "+
                            vesselroutes.get(i).get(o).getEarliestTime()+ " Latest Start time: "+ vesselroutes.get(i).get(o).getLatestTime());
                    if (o==0){
                        totalTime+=SailingTimes[i][0][i][vesselroutes.get(i).get(o).getID()-1];
                        totalTime+=TimeVesselUseOnOperation[i][vesselroutes.get(i).get(o).getID()-startNodes.length-1][0];
                        //System.out.println("temp total time: "+totalTime);
                    }
                    else{
                        totalTime+=SailingTimes[i][0][vesselroutes.get(i).get(o-1).getID()-1][vesselroutes.get(i).get(o).getID()-1];
                        if(o!=vesselroutes.get(i).size()-1) {
                            totalTime += TimeVesselUseOnOperation[i][vesselroutes.get(i).get(o).getID() - startNodes.length - 1][0];
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
                    simOp.getRoute() + " with index: " + simOp.getIndex());
        }
        System.out.println("Simultaneous 2");
        for(int v= 0;v<vesselroutes.size();v++){
            for(Map.Entry<Integer, ConnectedValues> entry : simOpRoutes.get(v).entrySet()){
                ConnectedValues simOp = entry.getValue();
                System.out.println("In vesssel " + v+" Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                        simOp.getRoute() + " with index: " + simOp.getIndex());
            }
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
    }

    public void runNormalLSO(String method){
        switch (method) {
            case "1RL":
                oneRelocateAll();
                break;
            case "2RL":
                twoRelocateAll();
                break;
            case "1EX":
                oneExchangeAll();
                break;
            case "2EX":
                twoExchangeAll();
                break;
            case "insertNormal":
                insertAll();
                /*
                if(print){
                    printLSOSolution(vesseltypes);
                    print=false;
                }

                 */
                break;
        }
        ObjectiveValues ov= ConstructionHeuristic.calculateObjective(vesselroutes,TimeVesselUseOnOperation,startNodes,SailingTimes,SailingCostForVessel,
                EarliestStartingTimeForVessel, operationGainGurobi, new int[vesselroutes.size()],new int[vesselroutes.size()],0, simALNS,bigTasksALNS);
        objValue=ov.getObjvalue();
        routeSailingCost=ov.getRouteSailingCost();
        routeOperationGain=ov.getRouteBenefitGain();
    }

    public void runLocalSearchFullEnumeration(){
        Boolean continueLocal=true;
        while(continueLocal) {
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

            System.out.println("run 1RL");
            LS_operators LSO = new LS_operators(OperationsForVessel, vesseltypes, SailingTimes, TimeVesselUseOnOperation,
                    SailingCostForVessel, EarliestStartingTimeForVessel, twIntervals, routeSailingCost, routeOperationGain,
                    startNodes, simALNS, precedenceALNS, bigTasksALNS, operationGain, vesselroutes, unroutedTasks,
                    precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                    simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, operationGainGurobi);
            LSO.runNormalLSO("1RL");

            System.out.println("run 2RL");
            LSO.runNormalLSO("2RL");

            System.out.println("run 1EX");
            LSO.runNormalLSO("1EX");

            System.out.println("run 2EX");
            LSO.runNormalLSO("2EX");

            System.out.println("run insert normal");
            LSO.runNormalLSO("insertNormal");

            System.out.println("run relocate");
            System.out.println("Before relocate in ALNS");
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
            RelocateInsert RI = new RelocateInsert(OperationsForVessel, vesseltypes, SailingTimes, TimeVesselUseOnOperation,
                    SailingCostForVessel, EarliestStartingTimeForVessel, twIntervals, routeSailingCost, routeOperationGain,
                    startNodes, simALNS, precedenceALNS, bigTasksALNS, operationGain,
                    unroutedTasks, vesselroutes, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                    simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, operationGainGurobi);
            RI.runRelocateLSO("relocate");

            System.out.println("run precedence");
            RI.runRelocateLSO("precedence");

            System.out.println("run simultaneous");
            RI.runRelocateLSO("simultaneous");

            System.out.println("Before consolidated in ALNS");
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

        }
    }

    public InsertionValues findInsertionCostsLS(OperationInRoute operationToInsert, int earliestSO, int latestSO, int earliestPO,
                                          int routeConnectedPrecedence, int routeConnectedSimultaneous, int pOFIndex, int simAIndex,
                                          Map<Integer,List<InsertionValues>> allFeasibleInsertions, Boolean noise, Random generator, int nMax){
        int o=operationToInsert.getID();
        int benefitIncrease=-100000;
        int indexInRoute=-1;
        int routeIndex=-1;
        int earliest=-1;
        int latest=nTimePeriods-1;
        //System.out.println("On operation: "+o);
        for (int v = 0; v < startNodes.length; v++) {
            boolean precedenceOverFeasible;
            boolean precedenceOfFeasible;
            boolean simultaneousFeasible;
            //System.out.println("ROUTE CONNECTED SIM: "+routeConnectedSimultaneous);
            //System.out.println("route connected simultaneous "+routeConnectedSimultaneous);
            if (DataGenerator.containsElement(o, OperationsForVessel[v]) && v!= routeConnectedSimultaneous) {
                //ConstructionHeuristic.printVessels(vesselRoutes,vesseltypes,SailingTimes,TimeVesselUseOnOperation,startNodes);
                //System.out.println("Try vessel "+v);
                if (vesselroutes.get(v) == null || vesselroutes.get(v).isEmpty()) {
                    //System.out.println("Empty route");
                    //insertion into empty route
                    int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                    int sailingCost=sailingTimeStartNodeToO*SailingCostForVessel[v];
                    int earliestTemp=Math.max(EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO+1,twIntervals[o-startNodes.length-1][0]);
                    int latestTemp=Math.min(nTimePeriods,twIntervals[o-startNodes.length-1][1]);
                    earliestTemp=LargeNeighboorhoodSearchInsert.checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence,precedenceALNS,startNodes,
                            TimeVesselUseOnOperation);
                    int [] simultaneousTimesValues = LargeNeighboorhoodSearchInsert.checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO,simALNS,
                            startNodes);
                    //System.out.println(simultaneousTimesValues[0] + "," + simultaneousTimesValues[1]+ " sim time");
                    earliestTemp=simultaneousTimesValues[0];
                    latestTemp=simultaneousTimesValues[1];

                    int[] startingTimes = ConstructionHeuristic.weatherFeasible(TimeVesselUseOnOperation,v,earliestTemp,latestTemp,o,nTimePeriods,startNodes);
                    if(startingTimes != null){
                        earliestTemp = startingTimes[0];
                        latestTemp = startingTimes[1];
                    }

                    if(earliestTemp<=latestTemp) {
                        //System.out.println("Feasible for empty route");
                        int benefitIncreaseTemp=operationGain[v][o-startNodes.length-1][earliestTemp-1]-sailingCost;
                        if(precedenceALNS[o-1-startNodes.length][0]!=0){
                            benefitIncreaseTemp+=(nTimePeriods-earliestTemp)*ParameterFile.earlyPrecedenceFactor;
                        }
                        if(noise){
                            //System.out.println("before noise "+benefitIncreaseTemp);
                            benefitIncreaseTemp+=LargeNeighboorhoodSearchInsert.findNoise(generator,nMax);
                            //System.out.println("after noise "+benefitIncreaseTemp);
                        }
                        if(benefitIncreaseTemp>0 && benefitIncreaseTemp>benefitIncrease) {
                            benefitIncrease=benefitIncreaseTemp;
                            routeIndex = v;
                            indexInRoute = 0;
                            earliest = earliestTemp;
                            latest = latestTemp;
                        }
                    }
                }
                else{
                    for(int n=0;n<vesselroutes.get(v).size();n++){
                        //System.out.println("On index "+n);
                        if(n==0) {
                            //check insertion in first position
                            int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                            int earliestTemp = Math.max(EarliestStartingTimeForVessel[v] + sailingTimeStartNodeToO + 1, twIntervals[o - startNodes.length - 1][0]);
                            int opTime=TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO];
                            int precedenceOfValuesEarliest=LargeNeighboorhoodSearchInsert.checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence,precedenceALNS,
                                    startNodes,TimeVesselUseOnOperation);
                            earliestTemp=precedenceOfValuesEarliest;
                            if(earliestTemp<=nTimePeriods) {
                                int sailingTimeOToNext = SailingTimes[v][earliestTemp - 1][o - 1][vesselroutes.get(v).get(0).getID() - 1];
                                int latestTemp = Math.min(vesselroutes.get(v).get(0).getLatestTime() - sailingTimeOToNext - opTime,
                                        twIntervals[o - startNodes.length - 1][1]);
                                latestTemp = ConstructionHeuristic.weatherLatestTimeSimPreInsert(latestTemp,earliestTemp,TimeVesselUseOnOperation, v, o, startNodes, SailingTimes, 0, vesselroutes,
                                        simultaneousOp,precedenceOfOperations,precedenceOverOperations,-1);

                                int [] simultaneousTimesValues = LargeNeighboorhoodSearchInsert.checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO,simALNS,
                                        startNodes);
                                earliestTemp = simultaneousTimesValues[0];
                                latestTemp = simultaneousTimesValues[1];

                                int timeIncrease = sailingTimeStartNodeToO + sailingTimeOToNext
                                        - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselroutes.get(v).get(0).getID() - 1];
                                int sailingCost = timeIncrease * SailingCostForVessel[v];
                                Boolean pPlacementFeasible = LargeNeighboorhoodSearchInsert.checkPPlacementLNS(o, n, v,routeConnectedPrecedence,pOFIndex,precedenceALNS,
                                        startNodes,precedenceOverOperations,simALNS,simultaneousOp);

                                int[] startingTimes = ConstructionHeuristic.weatherFeasible(TimeVesselUseOnOperation,v,earliestTemp,latestTemp,o,nTimePeriods,startNodes);
                                if(startingTimes != null){
                                    earliestTemp = startingTimes[0];
                                    latestTemp = startingTimes[1];
                                }
                                if (earliestTemp <= latestTemp && pPlacementFeasible) {
                                    OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                                    int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                    int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, 0, v, o,vesselroutes,
                                            TimeVesselUseOnOperation,startNodes,SailingTimes);
                                    int deltaOperationGainLastOperation = 0;
                                    if (changedTime > 0) {
                                        deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                    }
                                    int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                    if (benefitIncreaseTemp > 0) {
                                        if(precedenceALNS[o-1-startNodes.length][0]!=0){
                                            benefitIncreaseTemp+=(nTimePeriods-earliestTemp)*ParameterFile.earlyPrecedenceFactor;
                                        }
                                        precedenceOverFeasible = LargeNeighboorhoodSearchInsert.checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, 0, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods,
                                                SailingTimes, vesselroutes, precedenceOfOperations, precedenceOverRoutes,unroutedTasks);
                                        precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, 0, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                vesselroutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, simultaneousOp,twIntervals);
                                        simultaneousFeasible = LargeNeighboorhoodSearchInsert.checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, 0, earliestTemp, latestTemp, simultaneousOp, simALNS,
                                                startNodes, SailingTimes, TimeVesselUseOnOperation,vesselroutes,
                                                routeConnectedSimultaneous,simAIndex,precedenceOfOperations,precedenceOverOperations,twIntervals);
                                        if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                            //System.out.println("Feasible for position n=0");
                                            if(noise){
                                                //System.out.println("before noise "+benefitIncreaseTemp);
                                                benefitIncreaseTemp+=LargeNeighboorhoodSearchInsert.findNoise(generator,nMax);
                                                //System.out.println("after noise "+benefitIncreaseTemp);
                                            }
                                            if(benefitIncreaseTemp>benefitIncrease) {
                                                benefitIncrease=benefitIncreaseTemp;
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
                        if (n==vesselroutes.get(v).size()-1){
                            //check insertion in last position
                            //System.out.println("Checking this position");
                            int earliestN=vesselroutes.get(v).get(n).getEarliestTime();
                            int operationTimeN=TimeVesselUseOnOperation[v][vesselroutes.get(v).get(n).getID()-1-startNodes.length][earliestN-1];
                            int startTimeSailingTimePrevToO=earliestN+operationTimeN;
                            if(startTimeSailingTimePrevToO >= nTimePeriods){
                                continue;
                            }
                            int sailingTimePrevToO=SailingTimes[v][startTimeSailingTimePrevToO-1]
                                    [vesselroutes.get(v).get(n).getID()-1][o - 1];
                            int earliestTemp=Math.max(earliestN + operationTimeN + sailingTimePrevToO
                                    ,twIntervals[o-startNodes.length-1][0]);
                            int latestTemp=twIntervals[o-startNodes.length-1][1];
                            int precedenceOfValuesEarliest=LargeNeighboorhoodSearchInsert.checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence,precedenceALNS,startNodes,TimeVesselUseOnOperation);
                            earliestTemp=precedenceOfValuesEarliest;
                            if(earliestTemp<=nTimePeriods) {
                                //System.out.println("Time feasible");
                                int [] simultaneousTimesValues = LargeNeighboorhoodSearchInsert.checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO,simALNS,startNodes);
                                earliestTemp = simultaneousTimesValues[0];
                                latestTemp = simultaneousTimesValues[1];
                                int timeIncrease = sailingTimePrevToO;
                                int sailingCost = timeIncrease * SailingCostForVessel[v];

                                int[] startingTimes = ConstructionHeuristic.weatherFeasible(TimeVesselUseOnOperation,v,earliestTemp,latestTemp,o,nTimePeriods,startNodes);
                                if(startingTimes != null){
                                    earliestTemp = startingTimes[0];
                                    latestTemp = startingTimes[1];
                                }
                                if (earliestTemp <= latestTemp) {
                                    //System.out.println("p placement feasible and time feasible");
                                    OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                                    int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                    int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, n + 1, v, o,vesselroutes,
                                            TimeVesselUseOnOperation,startNodes,SailingTimes);
                                    int deltaOperationGainLastOperation = 0;
                                    if (changedTime > 0) {
                                        deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                    }
                                    int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                    if (benefitIncreaseTemp > 0) {
                                        if(precedenceALNS[o-1-startNodes.length][0]!=0){
                                            benefitIncreaseTemp+=(nTimePeriods-earliestTemp)*ParameterFile.earlyPrecedenceFactor;
                                        }
                                        precedenceOverFeasible = LargeNeighboorhoodSearchInsert.checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods, SailingTimes,
                                                vesselroutes, precedenceOfOperations, precedenceOverRoutes,unroutedTasks);
                                        precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                vesselroutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, simultaneousOp,twIntervals);
                                        simultaneousFeasible = LargeNeighboorhoodSearchInsert.checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp, simultaneousOp,
                                                simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation,vesselroutes,
                                                routeConnectedSimultaneous,simAIndex, precedenceOfOperations,precedenceOverOperations,twIntervals);
                                        if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                            //System.out.println("Feasible for last position in route");
                                            if(noise){
                                                //System.out.println("before noise "+benefitIncreaseTemp);
                                                benefitIncreaseTemp+=LargeNeighboorhoodSearchInsert.findNoise(generator,nMax);
                                                //System.out.println("after noise "+benefitIncreaseTemp);
                                            }
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
                        if(n<vesselroutes.get(v).size()-1) {
                            //check insertion for all other positions in the route
                            int earliestN = vesselroutes.get(v).get(n).getEarliestTime();
                            int operationTimeN = TimeVesselUseOnOperation[v][vesselroutes.get(v).get(n).getID() - 1 - startNodes.length][earliestN - 1];
                            int startTimeSailingTimePrevToO = earliestN + operationTimeN;
                            int sailingTimePrevToO = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselroutes.get(v).get(n).getID() - 1][o - 1];
                            int earliestTemp = Math.max(earliestN + sailingTimePrevToO + operationTimeN, twIntervals[o - startNodes.length - 1][0]);
                            int precedenceOfValuesEarliest=LargeNeighboorhoodSearchInsert.checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence,precedenceALNS,startNodes,TimeVesselUseOnOperation);
                            earliestTemp=precedenceOfValuesEarliest;
                            if(earliestTemp<=nTimePeriods) {
                                if (earliestTemp - 1 < nTimePeriods) {
                                    int opTime = TimeVesselUseOnOperation[v][o - 1 - startNodes.length][earliestTemp - 1];
                                    int sailingTimeOToNext = SailingTimes[v][Math.min(earliestTemp + opTime - 1, nTimePeriods - 1)][o - 1][vesselroutes.get(v).get(n + 1).getID() - 1];

                                    int latestTemp = Math.min(vesselroutes.get(v).get(n + 1).getLatestTime() -
                                            sailingTimeOToNext - opTime, twIntervals[o - startNodes.length - 1][1]);
                                    int [] simultaneousTimesValues = LargeNeighboorhoodSearchInsert.checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO,simALNS,startNodes);
                                    earliestTemp = simultaneousTimesValues[0];
                                    latestTemp = simultaneousTimesValues[1];

                                    latestTemp = ConstructionHeuristic.weatherLatestTimeSimPreInsert(latestTemp,earliestTemp,TimeVesselUseOnOperation,v,o,startNodes,SailingTimes,n+1,
                                            vesselroutes,simultaneousOp,precedenceOfOperations,precedenceOverOperations,-1);

                                    int sailingTimePrevToNext = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselroutes.get(v).get(n).getID() - 1][vesselroutes.get(v).get(n + 1).getID() - 1];
                                    int timeIncrease = sailingTimePrevToO + sailingTimeOToNext - sailingTimePrevToNext;
                                    int sailingCost = timeIncrease * SailingCostForVessel[v];
                                    Boolean pPlacementFeasible = LargeNeighboorhoodSearchInsert.checkPPlacementLNS(o, n+1, v,routeConnectedPrecedence,pOFIndex,precedenceALNS,
                                            startNodes,precedenceOverOperations,simALNS,simultaneousOp);

                                    int[] startingTimes = ConstructionHeuristic.weatherFeasible(TimeVesselUseOnOperation,v,earliestTemp,latestTemp,o,nTimePeriods,startNodes);
                                    if(startingTimes != null){
                                        earliestTemp = startingTimes[0];
                                        latestTemp = startingTimes[1];
                                    }

                                    if (earliestTemp <= latestTemp && pPlacementFeasible) {
                                        OperationInRoute lastOperation = vesselroutes.get(v).get(vesselroutes.get(v).size() - 1);
                                        int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                        /*
                                        for (int i = 0; i < vesselRoutes.size(); i++) {
                                            System.out.println("VESSELINDEX " + i);
                                            if (vesselRoutes.get(i) != null) {
                                                for (int o2 = 0; o2 < vesselRoutes.get(i).size(); o2++) {
                                                    System.out.println("Operation number: " + vesselRoutes.get(i).get(o2).getID() + " Earliest start time: " +
                                                            vesselRoutes.get(i).get(o2).getEarliestTime() + " Latest Start time: " + vesselRoutes.get(i).get(o2).getLatestTime());
                                                }
                                            }
                                        }

                                         */
                                        int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute,
                                                n + 1, v, o,vesselroutes,TimeVesselUseOnOperation,startNodes,SailingTimes);
                                        int deltaOperationGainLastOperation = 0;
                                        if (changedTime > 0) {
                                            deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                    operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                        }
                                        int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                        if (benefitIncreaseTemp > 0) {
                                            if(precedenceALNS[o-1-startNodes.length][0]!=0){
                                                benefitIncreaseTemp+=(nTimePeriods-earliestTemp)*ParameterFile.earlyPrecedenceFactor;
                                            }
                                            int currentLatest = vesselroutes.get(v).get(n).getLatestTime();
                                            simultaneousFeasible = ConstructionHeuristic.checkSOfFeasible(o, v, currentLatest, startNodes, simALNS, simultaneousOp);
                                            if (simultaneousFeasible) {
                                                simultaneousFeasible = LargeNeighboorhoodSearchInsert.checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp,
                                                        simultaneousOp, simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation,
                                                        vesselroutes,routeConnectedSimultaneous,simAIndex, precedenceOfOperations,precedenceOverOperations,twIntervals);
                                            }
                                            precedenceOverFeasible = LargeNeighboorhoodSearchInsert.checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation,
                                                    nTimePeriods, SailingTimes, vesselroutes, precedenceOfOperations, precedenceOverRoutes,unroutedTasks);
                                            precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                    vesselroutes, precedenceOverOperations, precedenceOfOperations, precedenceOfRoutes, simultaneousOp,twIntervals);
                                            if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                                //System.out.println("Feasible for index: "+(n+1));
                                                if(noise){
                                                    //System.out.println("before noise "+benefitIncreaseTemp);
                                                    benefitIncreaseTemp+=LargeNeighboorhoodSearchInsert.findNoise(generator,nMax);
                                                    //System.out.println("after noise "+benefitIncreaseTemp);
                                                }
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
        }
        return new InsertionValues(benefitIncrease, indexInRoute, routeIndex, earliest, latest);
    }


    public static void main(String[] args) throws FileNotFoundException {
        int [] vesseltypes = new int[]{3, 4, 5, 6};
        int [] startnodes = new int[]{3, 4, 5, 6};
        DataGenerator dg = new DataGenerator(vesseltypes, 5, startnodes,
                "test_instances/25_4_locations_normalOpGenerator.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),startnodes.length);
        //PrintData.printOperationGain(dg.getOperationGain(), startnodes.length);
        //PrintData.printSailingTimes(dg.getSailingTimes(),2,27, 4);
        //PrintData.printSailingTimes(dg.getSailingTimes(),3,23, 4);
        ConstructionHeuristic a = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(),dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations(),dg.getOperationGainGurobi(),dg.getWeatherPenaltyOperations());
        a.createSortedOperations();
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);
        LS_operators LSO = new LS_operators(dg.getOperationsForVessel(),vesseltypes,dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),
                dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),dg.getTwIntervals(),a.getRouteSailingCost(),a.getRouteOperationGain(),
                dg.getStartNodes(), dg.getSimultaneousALNS(),dg.getPrecedenceALNS(),dg.getBigTasksALNS(), a.getOperationGain(), a.getVesselroutes(),a.getUnroutedTasks(),
                a.getPrecedenceOverOperations(), a.getPrecedenceOfOperations(),a.getSimultaneousOp(),
                a.getSimOpRoutes(),a.getPrecedenceOfRoutes(), a.getPrecedenceOverRoutes(), a.getConsolidatedOperations(),dg.getOperationGainGurobi());
        //List<List<OperationInRoute>> new_vesselroutes = LSO.two_relocate(a.vesselroutes,1,3,4,0,startnodes,a.getUnroutedTasks());
        //LSO.twoRelocateAll();
        LSO.runNormalLSO("1RL");
        //LSO.runLocalSearchFullEnumeration();
        //List<List<OperationInRoute>> new_vesselroutes = LSO.insert(a.vesselroutes,a.unroutedTasks.get(2),1,1, startnodes,a.getUnroutedTasks());
        LSO.printLSOSolution(vesseltypes);
    }

    public int[] getRouteSailingCost() {
        return routeSailingCost;
    }

    public int[] getRouteOperationGain() {
        return routeOperationGain;
    }

    public List<List<OperationInRoute>> getVesselroutes() {
        return vesselroutes;
    }

    public List<OperationInRoute> getUnroutedTasks() {
        return unroutedTasks;
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

    public Boolean getPrint() {
        return print;
    }

    public void setPrint(Boolean print) {
        this.print = print;
    }

    public int getCount1RL() {
        return count1RL;
    }

    public void setCount1RL(int count1RL) {
        this.count1RL = count1RL;
    }

    public int getCount2RL() {
        return count2RL;
    }

    public void setCount2RL(int count2RL) {
        this.count2RL = count2RL;
    }

    public int getCount1EX() {
        return count1EX;
    }

    public void setCount1EX(int count1EX) {
        this.count1EX = count1EX;
    }

    public int getCount2EX() {
        return count2EX;
    }

    public void setCount2EX(int count2EX) {
        this.count2EX = count2EX;
    }

    public int getCountNormalInsertion() {
        return countNormalInsertion;
    }

    public void setCountNormalInsertion(int countNormalInsertion) {
        this.countNormalInsertion = countNormalInsertion;
    }
}