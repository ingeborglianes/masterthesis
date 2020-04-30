import javafx.scene.control.ListCell;

import javax.imageio.plugins.tiff.TIFFImageReadParam;
import java.io.FileNotFoundException;
import java.sql.Time;
import java.util.*;
import java.util.stream.IntStream;

public class LS_operators {
    private int [][] OperationsForVessel;
    private int[] vesseltypes;
    private int[][][][] SailingTimes;
    private  int [][][] TimeVesselUseOnOperation;
    private int [] SailingCostForVessel;
    private int [] EarliestStartingTimeForVessel;
    private int [] startNodes;
    private int [][] twIntervals;
    private int nTimePeriods = 60;
    private int [] routeSailingCost;
    private int[] routeOperationGain;
    private int [][][] operationGain;
    private int objValue;
    private int[][] simALNS;
    private int [][] bigTasksALNS;
    //map for operations that are connected with precedence. ID= operation number. Value= Precedence value.
    private Map<Integer,PrecedenceValues> precedenceOverOperations;
    private Map<Integer,PrecedenceValues> precedenceOfOperations;
    //List for operations that are connected as simultaneous operations. ID= operation number. Value= Simultaneous value.
    private Map<Integer, ConnectedValues> simultaneousOp;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;
    private List<Map<Integer,PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer,PrecedenceValues>> precedenceOverRoutes;
    private Map<Integer, ConsolidatedValues> consolidatedOperations;


    public LS_operators(int [][] OperationsForVessel, int[] vesseltypes, int[][][][] SailingTimes,
                        int [][][] TimeVesselUseOnOperation, int[] SailingCostForVessel, int [] EarliestStartingTimeForVessel,
                        int [][] twIntervals, int [] routeSailingCost, int[] routeOperationGain, int[] startNodes, int[][] simALNS,
                        int [][] bigTasksALNS, int [][][] operationGain,
                        Map<Integer,PrecedenceValues> precedenceOverOperations,Map<Integer,PrecedenceValues> precedenceOfOperations,
                        Map<Integer, ConnectedValues> simultaneousOp,List<Map<Integer, ConnectedValues>> simOpRoutes,
                        List<Map<Integer,PrecedenceValues>> precedenceOfRoutes, List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                        Map<Integer, ConsolidatedValues> consolidatedOperations){
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
        this.startNodes = startNodes;
        this.simALNS = simALNS;
        this.bigTasksALNS = bigTasksALNS;
        this.operationGain = operationGain;
        this.precedenceOverOperations =precedenceOverOperations;
        this.precedenceOfOperations = precedenceOfOperations;
        this.simultaneousOp = simultaneousOp;
        this.simOpRoutes = simOpRoutes;
        this.precedenceOfRoutes = precedenceOfRoutes;
        this.precedenceOverRoutes = precedenceOverRoutes;
        this.consolidatedOperations = consolidatedOperations;
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


    public List<List<OperationInRoute>> one_relocate(List<List<OperationInRoute>> vesselroutes, int vessel, int pos1, int pos2,
                                                     int[] startnodes, List<OperationInRoute> unroutedTasks){
        if (pos1 == pos2){
            System.out.println("Cannot relocate task to same position");
            return vesselroutes;
        }

        if(simultaneousOp.get(vesselroutes.get(vessel).get(pos1).getID()) != null){
            System.out.println("Cannot relocate simultaneousOp with this method");
            return vesselroutes;
        }

        int nStartnodes = startnodes.length;
        int old_pos1_dist;
        int old_pos2_dist;
        int oldObjValue = objValue;

        //Tracker gammel tid for både pos 1 og pos 2 (21.02)
        if(pos1==0) {

            old_pos1_dist = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1 + 1).getID()-1] ;
            //+ TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(old_pos1_dist + " Old pos 1 dist");

        } else if(pos1==vesselroutes.get(vessel).size()-1){
            old_pos1_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1] ;
            //+ TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(old_pos1_dist + " Old pos 1 dist");

        } else{
            old_pos1_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1];
            //+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
            System.out.println(old_pos1_dist + " Old pos 1 dist");


        }
        if(pos2 == 0){
            old_pos2_dist = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel]-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            System.out.println(old_pos2_dist + " old pos 2 dist");
        }else if(pos2 == vesselroutes.get(vessel).size()-1){
            old_pos2_dist = 0;
            System.out.println(old_pos2_dist + " old pos 2 dist");
        } else {
            old_pos2_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            System.out.println(old_pos2_dist + " old pos 2 dist");

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

        if(pos1 < pos2){
            if(pos1==0){
                new_pos1_dist = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel]-1][vesselroutes.get(vessel).get(0).getID()-1];
                System.out.println(new_pos1_dist + " new pos 1 dist");
            } else{
                new_pos1_dist = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                        TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][new_vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                        [new_vesselroutes.get(vessel).get(pos1-1).getID()-1][new_vesselroutes.get(vessel).get(pos1).getID()-1];
                System.out.println(new_pos1_dist + " new pos 1 dist");
            }
        }else{
            if(pos1 == vesselroutes.get(vessel).size()-1) {
                new_pos1_dist = 0;
                System.out.println(new_pos1_dist + " new pos 1 dist");
            }
            else {
                new_pos1_dist = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos2).getEarliestTime() +
                        TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes][new_vesselroutes.get(vessel).get(pos2).getEarliestTime()]]
                        [new_vesselroutes.get(vessel).get(pos1).getID() - 1][new_vesselroutes.get(vessel).get(pos1 + 1).getID() - 1];
                System.out.println(new_pos1_dist + " new pos 1 dist");
            }
        }


        if(pos2==0) {
            new_second_sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][new_vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist = new_second_sailing +
                    SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][new_vesselroutes.get(vessel).get(pos1).getEarliestTime()]]
                            [new_vesselroutes.get(vessel).get(pos2).getID()-1][new_vesselroutes.get(vessel).get(pos2+1).getID()-1] ;
            // +TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][new_vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(new_pos2_dist + " New pos 2 dist");
        } else if (pos2==new_vesselroutes.get(vessel).size()-1){
            new_second_sailing = SailingTimes[vessel][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][new_vesselroutes.get(vessel).get(pos2-1).getEarliestTime()])]
                    [new_vesselroutes.get(vessel).get(pos2-1).getID()-1][new_vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist =  new_second_sailing ;
            System.out.println(new_pos2_dist + " New pos 2 dist");

        } else{
            new_second_sailing = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][new_vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [new_vesselroutes.get(vessel).get(pos2-1).getID()-1][new_vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist = new_second_sailing +
                    SailingTimes[vessel][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [Math.min(nTimePeriods-1,new_vesselroutes.get(vessel).get(pos1).getEarliestTime())])][new_vesselroutes.get(vessel).get(pos2).getID()-1][new_vesselroutes.get(vessel).get(pos2+1).getID()-1] ;
            System.out.println(new_pos2_dist + " New pos 2 dist");
        }

        //Calculating objective function changes
        int first_delta;
        int second_delta;
        if (pos1<pos2){
            first_delta = -(old_pos1_dist) + (new_pos1_dist);
            second_delta = -(old_pos2_dist) + (new_pos2_dist);
        }else{
            first_delta = -(old_pos2_dist) + (new_pos2_dist);
            second_delta = -(old_pos1_dist) + (new_pos1_dist);
        }
        int sailingdelta = first_delta + second_delta;

        OperationInRoute oldLastOpVessel1 = vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1);
        int vessel1Gain = 0;
        int old1Gain = 0;
        if(pos1 != vesselroutes.get(vessel).size()-1 &&
                pos2 != new_vesselroutes.get(vessel).size()-1) {
            old1Gain = operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime()-1];
            vessel1Gain = -old1Gain + operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length-1][0];
        }else{
            OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel).get(new_vesselroutes.get(vessel).size() - 1);
            old1Gain = operationGain[vessel][newLastOpVessel1.getID()-startnodes.length-1][newLastOpVessel1.getEarliestTime()-1]
                    +  operationGain[vessel][oldLastOpVessel1.getID()-startnodes.length-1][oldLastOpVessel1.getEarliestTime()-1];
            vessel1Gain = -old1Gain + operationGain[vessel][oldLastOpVessel1.getID()-startnodes.length-1][0] +
                        operationGain[vessel][newLastOpVessel1.getID()-startnodes.length-1][0];
        }
        int delta = - sailingdelta + vessel1Gain;



        if(delta <= 0){
            System.out.println("Objective change negative or unchanged, undesirable relocate. Returning old vesselroutes");
            return vesselroutes;
        }

        if(pos2 == new_vesselroutes.get(vessel).size()-1){
            int newEarliestTime = new_vesselroutes.get(vessel).get(pos1).getEarliestTime() + first_delta + new_second_sailing;
            if(newEarliestTime > nTimePeriods){
                System.out.println("Infeasible one-relocate");
                return vesselroutes;
            }
        }else{
            int newEarliestTime = new_vesselroutes.get(vessel).get(new_vesselroutes.get(vessel).size()-1).getEarliestTime() + first_delta + second_delta;
            if(newEarliestTime > nTimePeriods){
                System.out.println("Infeasible one-relocate");
                return vesselroutes;
            }
        }

        int earliest_insert = pos1;
        int latest_insert = pos2;
        if(pos2<pos1){
            earliest_insert = pos2;
            latest_insert = pos1;
        }
        int earliest;
        int latest;
        if(earliest_insert==0){
            earliest = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startNodes[vessel]-1][new_vesselroutes.get(vessel).get(earliest_insert).getID()-1]+1;
            earliest = Math.max(earliest, twIntervals[new_vesselroutes.get(vessel).get(earliest_insert).getID()-startnodes.length-1][0]);
        }else {
            earliest = new_vesselroutes.get(vessel).get(earliest_insert - 1).getEarliestTime() +
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(earliest_insert - 1).getID() - 1 - startNodes.length][new_vesselroutes.get(vessel).get(earliest_insert - 1).getEarliestTime()]
                    + SailingTimes[vessel][new_vesselroutes.get(vessel).get(earliest_insert - 1).getEarliestTime() +
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(earliest_insert - 1).getID() - 1 - startNodes.length][new_vesselroutes.get(vessel).get(earliest_insert - 1).getEarliestTime()]]
                    [new_vesselroutes.get(vessel).get(earliest_insert - 1).getID() - 1][new_vesselroutes.get(vessel).get(earliest_insert).getID() - 1];
            earliest = Math.max(earliest, twIntervals[new_vesselroutes.get(vessel).get(earliest_insert).getID()-startnodes.length-1][0]);
        }
        if(latest_insert == new_vesselroutes.get(vessel).size()-1){
            latest = Math.min(nTimePeriods, twIntervals[new_vesselroutes.get(vessel).get(latest_insert).getID()-startnodes.length-1][1]);
        }else {
            latest = new_vesselroutes.get(vessel).get(latest_insert + 1).getLatestTime() - SailingTimes[vessel][new_vesselroutes.get(vessel).get(earliest_insert).getLatestTime() +
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(latest_insert).getID() - 1 - startnodes.length][new_vesselroutes.get(vessel).get(earliest_insert).getLatestTime()]]
                    [new_vesselroutes.get(vessel).get(latest_insert).getID() - 1][new_vesselroutes.get(vessel).get(latest_insert + 1).getID()-1] -
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(latest_insert).getID() - 1 - startnodes.length][new_vesselroutes.get(vessel).get(earliest_insert).getLatestTime()];
            latest = Math.min(latest, twIntervals[new_vesselroutes.get(vessel).get(latest_insert).getID()-startnodes.length-1][1]);
        }

        //TODO:
        //Comparing the new latest and earliest times with the old ones, gå gjennom dette med Ingeborg for å test logikken
        if(earliest >  vesselroutes.get(vessel).get(earliest_insert).getLatestTime()){
            System.out.println("Infeasible relocate, earliest time larger than latest time");
            return vesselroutes;
        }
        if(latest < vesselroutes.get(vessel).get(latest_insert).getEarliestTime()){
            System.out.println("Infeasible relocate, earliest time larger than latest time");
            return vesselroutes;
        }

        // Relocating the elements in the original vesselroutes list to secure the same object structure
        OperationInRoute toMoveOriginal = vesselroutes.get(vessel).get(pos1);
        vesselroutes.get(vessel).remove(pos1);
        vesselroutes.get(vessel).add(pos2, toMoveOriginal);

        //Rearranging the new_vesselroutes lists to the original order
        new_vesselroutes.get(vessel).remove(pos2);
        new_vesselroutes.get(vessel).add(pos1,toMoveOriginal);

        vesselroutes.get(vessel).get(earliest_insert).setEarliestTime(Math.max(earliest,twIntervals[toMoveOriginal.getID()-startnodes.length-1][0]));
        vesselroutes.get(vessel).get(latest_insert).setLatestTime(Math.min(latest,twIntervals[toMoveOriginal.getID()-startnodes.length-1][1]));
        updateEarliest(earliest,earliest_insert,vessel,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes,twIntervals, "onerelocate");
        updateLatest(latest,latest_insert,vessel,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes,twIntervals,"onerelocate");
        updateConRoutes(simOpRoutes,precedenceOfRoutes, precedenceOverRoutes, vessel,vesselroutes,null,precedenceOverOperations,precedenceOfOperations,simultaneousOp,
                        SailingTimes,twIntervals,EarliestStartingTimeForVessel,startNodes,TimeVesselUseOnOperation);

        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(vessel),pos2,simOpRoutes,precedenceOfOperations,precedenceOverOperations,
                                                    TimeVesselUseOnOperation,startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,
                                                    vesselroutes,SailingTimes);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(vessel),pos2, TimeVesselUseOnOperation,startNodes,simOpRoutes,
                                                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,
                                                vesselroutes,simultaneousOp,SailingTimes);


        updateSimultaneous(simOpRoutes,vessel, simultaneousOp,
                precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                precedenceOfOperations,vesselroutes, pos1,pos2, "onerelocate");

        //Checking feasibility:
        if(!checkSolution(vesselroutes,startNodes,TimeVesselUseOnOperation,precedenceOverOperations,simultaneousOp)){
            return retainOldSolution(new_vesselroutes,unroutedTasks,simultaneousOp,precedenceOfOperations,precedenceOverOperations);
        }

        //Checking profitability
        if(pos1 != vesselroutes.get(vessel).size()-1 &&
                pos2 != vesselroutes.get(vessel).size()-1) {
            vessel1Gain = operationGain[vessel][vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1).getID()-startnodes.length-1]
                    [vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1).getEarliestTime()-1];
        }else{
            OperationInRoute newLastOpVessel1 = vesselroutes.get(vessel).get(vesselroutes.get(vessel).size() - 1);
            vessel1Gain = operationGain[vessel][oldLastOpVessel1.getID()-startnodes.length-1][0] +
                    operationGain[vessel][newLastOpVessel1.getID()-startnodes.length-1][Math.min(nTimePeriods-1,newLastOpVessel1.getEarliestTime()-1)];
        }

        int opGainChange1 = routeOperationGain[vessel] - old1Gain + vessel1Gain;
        int newObjValue = oldObjValue + (sailingdelta*SailingCostForVessel[vessel]) + opGainChange1;
        if(newObjValue > oldObjValue){
            routeSailingCost[vessel] = routeSailingCost[vessel] + (sailingdelta*SailingCostForVessel[vessel]);
            routeOperationGain[vessel] = routeOperationGain[vessel] + opGainChange1;
            objValue = newObjValue;
        }else{
            System.out.println("Objective not improved");
            return retainOldSolution(new_vesselroutes,unroutedTasks,simultaneousOp,precedenceOfOperations,precedenceOverOperations);
        }

        System.out.println("Relocate performed");
        printLSOSolution(vesseltypes,vesselroutes,unroutedTasks);

        return vesselroutes;

    }


    public List<List<OperationInRoute>> two_relocate(List<List<OperationInRoute>> vesselroutes, int vessel1, int vessel2, int pos1, int pos2,
                                                     int[] startnodes, List<OperationInRoute> unroutedTasks){
        if ((vessel1 == vessel2) ||
                !(ConstructionHeuristic.containsElement (vesselroutes.get(vessel1).get(pos1).getID(), OperationsForVessel[vessel2]))){
            System.out.println("Cannot relocate task to same vessel with this method, or new vessel cannot perform task");
            return vesselroutes;
        }

        if(simultaneousOp.get(vesselroutes.get(vessel1).get(pos1).getID()) != null){
            System.out.println("Cannot relocate simultaneousOp with this method");
            return vesselroutes;
        }

        int oldObjValue = objValue;
        int nStartnodes = startnodes.length;
        int old_vessel1_dist;
        int old_vessel2_dist;

        //Tracker gammel tid for vessel 1
        if(vesselroutes.get(vessel1).size() == 1){
            old_vessel1_dist = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");
        } else if(pos1==0) {
            old_vessel1_dist = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1 + 1).getID()-1];

            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] ;
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else{
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] ;

            System.out.println(old_vessel1_dist + " Old vessel 1 dist");
        }

        if(vesselroutes.get(vessel2) == null || vesselroutes.get(vessel2).size() == 0){
            old_vessel2_dist = 0;
            System.out.println( old_vessel2_dist + " Old vessel2 dist");
        } else if(pos2==0) {
            old_vessel2_dist = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2]-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if(pos2==vesselroutes.get(vessel2).size()){
            old_vessel2_dist = 0;
            System.out.println(old_vessel2_dist + " Old vessel2 dist her");
        }
        else{
            old_vessel2_dist = SailingTimes[vessel2][Math.min(nTimePeriods-1,vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+TimeVesselUseOnOperation[vessel2]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()])]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            System.out.println(old_vessel2_dist + " Old vessel2 dist her");
        }

        //Tracking new times for vessel 1
        int new_vessel1_dist;

        if(vesselroutes.get(vessel1).size() == 1){
            new_vessel1_dist = 0;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1==0) {
            new_vessel1_dist = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1]-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] ;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1 == vesselroutes.get(vessel1).size()-1){
            new_vessel1_dist = 0;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        }
        else{
            new_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1];
            System.out.println(new_vessel1_dist + " New vessel1 dist");
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

        if(new_vesselroutes.get(vessel2).size() == 1){
            new_second_sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][new_vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if(pos2==0) {
            new_second_sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][new_vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][new_second_sailing +
                            TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][new_second_sailing]]
                            [new_vesselroutes.get(vessel2).get(pos2).getID()-1][new_vesselroutes.get(vessel2).get(pos2+1).getID()-1] ;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2==new_vesselroutes.get(vessel2).size()-1){
            new_second_sailing = SailingTimes[vessel2][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes]
                            [Math.min(nTimePeriods-1,new_vesselroutes.get(vessel2).get(pos2-1).getEarliestTime())])][new_vesselroutes.get(vessel2).get(pos2-1).getID()-1]
                    [new_vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else{
            new_second_sailing = SailingTimes[vessel2][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel2).get(pos2-1).getEarliestTime())])]
                    [new_vesselroutes.get(vessel2).get(pos2-1).getID()-1][new_vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                            new_second_sailing + TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [Math.min(nTimePeriods-1,new_vesselroutes.get(vessel2).get(pos2-1).getEarliestTime() + new_second_sailing)])]
                            [new_vesselroutes.get(vessel2).get(pos2).getID()-1][new_vesselroutes.get(vessel2).get(pos2+1).getID()-1];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        }

        //Calculating delta changes for sailing and operation gain
        int vessel1_delta = -old_vessel1_dist + new_vessel1_dist;
        int vessel2_delta = -old_vessel2_dist + new_vessel2_dist;

        OperationInRoute oldLastOpVessel1 = vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size()-1);
        OperationInRoute newLastOpVessel2 = new_vesselroutes.get(vessel2).get(new_vesselroutes.get(vessel2).size()-1);
        int vessel1Gain = 0;
        int vessel2Gain = 0;
        int old1Gain = 0;
        int old2Gain = 0;
        if(pos1 != vesselroutes.get(vessel1).size()-1 &&
                pos2 != new_vesselroutes.get(vessel2).size()-1) {
            OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel1).get(new_vesselroutes.get(vessel1).size() - 1);
            old1Gain = operationGain[vessel1][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime()-1] +
                    operationGain[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-startnodes.length-1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()-1];
            vessel1Gain = - old1Gain + operationGain[vessel1][newLastOpVessel1.getID() - startnodes.length - 1][Math.min(nTimePeriods-1,newLastOpVessel1.getEarliestTime() + vessel1_delta -1)];

            OperationInRoute oldLastOpVessel2 = vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size() - 1);
            old2Gain = operationGain[vessel2][oldLastOpVessel2.getID() - startnodes.length - 1][oldLastOpVessel2.getEarliestTime()-1];
            vessel2Gain = - old2Gain + operationGain[vessel2][newLastOpVessel2.getID() - startnodes.length - 1][Math.min(nTimePeriods-1,newLastOpVessel2.getEarliestTime() + vessel2_delta-1)] +
                            operationGain[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID()-startnodes.length-1][0];
        }
        else{
            if(!new_vesselroutes.get(vessel1).isEmpty()) {
                OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel1).get(new_vesselroutes.get(vessel1).size() - 1);
                old1Gain = operationGain[vessel1][newLastOpVessel1.getID() - startnodes.length - 1][newLastOpVessel1.getEarliestTime() - 1]
                        + operationGain[vessel1][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1];
                vessel1Gain = -old1Gain + operationGain[vessel1][newLastOpVessel1.getID() - startnodes.length - 1][Math.min(nTimePeriods - 1, Math.max(0,newLastOpVessel1.getEarliestTime() + vessel1_delta - 1))];
            }else{
                old1Gain = operationGain[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-startnodes.length-1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()-1];
                vessel1Gain = - old1Gain;
            }
            if(vesselroutes.get(vessel2) != null){
                if(vesselroutes.get(vessel2).size() != 0) {
                    OperationInRoute oldLastOpVessel2 = vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size() - 1);
                    old2Gain = operationGain[vessel2][newLastOpVessel2.getID() - startnodes.length - 1][newLastOpVessel2.getEarliestTime() - 1]
                            + operationGain[vessel2][oldLastOpVessel2.getID() - startnodes.length - 1][oldLastOpVessel2.getEarliestTime() - 1];
                    vessel2Gain = - old2Gain + operationGain[vessel2][oldLastOpVessel2.getID() - startnodes.length - 1][0] +
                            operationGain[vessel2][newLastOpVessel2.getID() - startnodes.length - 1][Math.min(nTimePeriods-1,newLastOpVessel2.getEarliestTime() + vessel2_delta-1)];
                }
            }else{
                old2Gain = operationGain[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID()-startnodes.length-1][0];
                vessel2Gain = old2Gain;
            }
        }
        int delta = - vessel1_delta - vessel2_delta + vessel1Gain + vessel2Gain;

        if(delta <= 0){
            System.out.println("Change in objective negative or unchanged, undesirable relocate. Returning old vesselroutes");
            return vesselroutes;
        }

        // Relocating the elements in the original vesselroutes list to secure the same object structure
        OperationInRoute toMoveOriginal = vesselroutes.get(vessel1).get(pos1);
        vesselroutes.get(vessel1).remove(pos1);
        if(vesselroutes.get(vessel2) == null) {
            int finalIndexInRoute = pos2;
            vesselroutes.set(vessel2, new ArrayList<>() {{
                add(finalIndexInRoute, toMoveOriginal);
            }});
        }else {
                vesselroutes.get(vessel2).add(pos2,toMoveOriginal);
        }

        //Rearranging the new_vesselroutes lists to the original order
        OperationInRoute toMoveBack = new_vesselroutes.get(vessel2).get(pos2);
        new_vesselroutes.get(vessel2).remove(pos2);
        new_vesselroutes.get(vessel1).add(pos1,toMoveBack);

        if(vesselroutes.get(vessel1).isEmpty()){

        }else if(pos1 == 0){
            int sailing = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startNodes[vessel1]-1][vesselroutes.get(vessel1).get(pos1).getID()-1]+1;
            vesselroutes.get(vessel1).get(pos1).setEarliestTime(Math.max(twIntervals[vesselroutes.get(vessel1).get(pos1).getID()-1-startnodes.length][0],sailing));
            updateEarliest(sailing, 0, vessel1, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes,twIntervals,"twoRelocate");
        }else if(pos1 == vesselroutes.get(vessel1).size()){
            vesselroutes.get(vessel1).get(pos1-1).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel1).get(pos1-1).getID()-1-startnodes.length][1],nTimePeriods));
            updateLatest(vesselroutes.get(vessel1).get(pos1-1).getLatestTime(),pos1-1,vessel1,TimeVesselUseOnOperation,
                    startNodes,SailingTimes,vesselroutes,twIntervals,"twoRelocate");
        }else if(pos1 == vesselroutes.get(vessel1).size()-1){
            vesselroutes.get(vessel1).get(pos1).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel1).get(pos1).getID()-1-startnodes.length][1],nTimePeriods));
            updateLatest(vesselroutes.get(vessel1).get(pos1-1).getLatestTime(),pos1,vessel1,TimeVesselUseOnOperation,
                    startNodes,SailingTimes,vesselroutes,twIntervals,"twoRelocate");
        }
        else {
            updateEarliest(vesselroutes.get(vessel1).get(pos1-1).getEarliestTime(), pos1 - 1, vessel1, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes,twIntervals,"twoRelocate");
            updateLatest(vesselroutes.get(vessel1).get(pos1+1).getLatestTime(),pos1+1,vessel1, TimeVesselUseOnOperation,
                    startNodes,SailingTimes,vesselroutes,twIntervals,"twoRelocate");
        }
        if(vesselroutes.get(vessel2).size() == 1) {
            int sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startNodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID() - 1] + 1;
            vesselroutes.get(vessel2).get(pos2).setEarliestTime(Math.max(twIntervals[vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][0],sailing));
            vesselroutes.get(vessel2).get(pos2).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][1],nTimePeriods));

        }else if(pos2 == 0){
            int sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startNodes[vessel2]-1][vesselroutes.get(vessel2).get(pos2).getID()-1]+1;
            int opTime = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][sailing];
            int sailingToNext = SailingTimes[vessel2][sailing+opTime][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];
            vesselroutes.get(vessel2).get(pos2).setEarliestTime(Math.max(twIntervals[vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][0],sailing));
            vesselroutes.get(vessel2).get(pos2).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][1],vesselroutes.get(vessel2).get(pos2+1).getLatestTime()-sailingToNext-opTime));

        }else if(pos2== vesselroutes.get(vessel2).size()-1){
            int prevOp = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-startnodes.length][Math.min(nTimePeriods-1,vesselroutes.get(vessel2).get(pos2-1).getEarliestTime())];
            int sailingToOp = SailingTimes[vessel2][Math.min(vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+prevOp,nTimePeriods-1)][vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            vesselroutes.get(vessel2).get(pos2).setEarliestTime(Math.max(twIntervals[vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][0],vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+sailingToOp));
            vesselroutes.get(vessel2).get(pos2).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][1],nTimePeriods));

        }else {
            int prevOp = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-startnodes.length][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()];
            int sailingToOp = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+prevOp][vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];

            vesselroutes.get(vessel2).get(pos2).setEarliestTime(Math.max(twIntervals[vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][0],vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+sailingToOp));

            int opTime = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            int sailingToNext = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+opTime][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];

            vesselroutes.get(vessel2).get(pos2).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][1],
                                                                        vesselroutes.get(vessel2).get(pos2+1).getLatestTime()-sailingToNext-opTime));
        }

        if(vesselroutes.get(vessel2).get(pos2).getEarliestTime() > vesselroutes.get(vessel2).get(pos2).getLatestTime()){
            System.out.println("Infeasible relocate, eraliest time is larger than latest time");
            return retainOldSolution(new_vesselroutes,unroutedTasks,simultaneousOp,precedenceOfOperations,precedenceOverOperations);
        }

        updateEarliest(vesselroutes.get(vessel2).get(pos2).getEarliestTime(),pos2,vessel2,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes,twIntervals,"tworelocate");
        updateLatest(vesselroutes.get(vessel2).get(pos2).getLatestTime(),pos2,vessel2,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes,twIntervals,"tworelocate");

        List<Integer> updatedRoutes = new ArrayList<>(){{add(vessel2);}};
        updateConRoutes(simOpRoutes,precedenceOfRoutes, precedenceOverRoutes, vessel1,vesselroutes,updatedRoutes,precedenceOverOperations,precedenceOfOperations,simultaneousOp,
                        SailingTimes,twIntervals,EarliestStartingTimeForVessel,startNodes,TimeVesselUseOnOperation);
        ConstructionHeuristic.updateIndexesInsertion(vessel2,pos2,vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
        ConstructionHeuristic.updateIndexesRemoval(vessel1,pos1,vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(vessel2),pos2,simOpRoutes,precedenceOfOperations,precedenceOverOperations,
                                                    TimeVesselUseOnOperation,startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselroutes,SailingTimes);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(vessel2),pos2,TimeVesselUseOnOperation,startNodes, simOpRoutes,
                                                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselroutes,simultaneousOp,SailingTimes);

        updateSimultaneous(simOpRoutes,vessel2, simultaneousOp,
                precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                precedenceOfOperations,vesselroutes,pos1, pos2,"noUpdate");


        //Checking feasibility:
        if(!checkSolution(vesselroutes,startNodes, TimeVesselUseOnOperation,precedenceOverOperations,simultaneousOp)){
            ConstructionHeuristic.updateIndexesInsertion(vessel1,pos1,new_vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
            ConstructionHeuristic.updateIndexesRemoval(vessel2,pos2,new_vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
            return retainOldSolution(new_vesselroutes,unroutedTasks,simultaneousOp,precedenceOfOperations,precedenceOverOperations);
        }

        //Checking profitability
        int opGainChange1;
        int opGainChange2;

        if(pos1 != new_vesselroutes.get(vessel1).size()-1){
            opGainChange1 = - old1Gain
                    + operationGain[vessel1][vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size()-1).getID()-startnodes.length-1][vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size()-1).getEarliestTime()-1];
        }else{
            OperationInRoute newLastOpVessel1 = vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size() - 1);
            opGainChange1 = - old1Gain +
                    operationGain[vessel1][newLastOpVessel1.getID()-startnodes.length-1][Math.min(nTimePeriods-1,newLastOpVessel1.getEarliestTime()-1)];
        }
        if (pos2 != vesselroutes.get(vessel2).size()-1){
            opGainChange2 = - old2Gain + operationGain[vessel2][vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getID()-startnodes.length-1]
                    [vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getEarliestTime()-1] +
                    operationGain[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-startnodes.length-1][vesselroutes.get(vessel2).get(pos2).getEarliestTime()-1];
        }else{
            opGainChange2 = - old2Gain +
                    operationGain[vessel2][vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getID()-startnodes.length-1]
                            [Math.min(nTimePeriods-1,vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getEarliestTime()-1)];
        }
        int newObjValue = oldObjValue - (vessel1_delta*SailingCostForVessel[vessel1]) - (vessel2_delta*SailingCostForVessel[vessel2]) + opGainChange1 + opGainChange2;
        if(newObjValue > oldObjValue){
            routeSailingCost[vessel1] = routeSailingCost[vessel1] + (vessel1_delta*SailingCostForVessel[vessel1]);
            routeSailingCost[vessel2] = routeSailingCost[vessel2] + (vessel2_delta*SailingCostForVessel[vessel2]);
            routeOperationGain[vessel1] = routeOperationGain[vessel1] + opGainChange1;
            routeOperationGain[vessel2] = routeOperationGain[vessel2] + opGainChange2;
            objValue = newObjValue;
        }else{
            System.out.println("Objective not improved");
            ConstructionHeuristic.updateIndexesInsertion(vessel1,pos1,new_vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
            ConstructionHeuristic.updateIndexesRemoval(vessel2,pos2,new_vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
            return retainOldSolution(new_vesselroutes,unroutedTasks,simultaneousOp,precedenceOfOperations,precedenceOverOperations);
        }


        System.out.println("Two relocate performed");
        printLSOSolution(vesseltypes,vesselroutes,unroutedTasks);

        if(bigTasksALNS[vesselroutes.get(vessel2).get(pos2).getID()-1-startNodes.length]!=null &&
                bigTasksALNS[vesselroutes.get(vessel2).get(pos2).getID()-startNodes.length-1][0] == vesselroutes.get(vessel2).get(pos2).getID()){
            if(consolidatedOperations.get(vesselroutes.get(vessel2).get(pos2).getID()) != null) {
                consolidatedOperations.get(vesselroutes.get(vessel2).get(pos2).getID()).setConsolidatedRoute(vessel2);
            }
        }

        return vesselroutes;

    }


    public List<List<OperationInRoute>> one_exchange(List<List<OperationInRoute>> vesselroutes, int vessel, int cur_pos, int new_pos,
                                                     int[] startnodes, List<OperationInRoute> unroutedTasks){
        if (cur_pos == new_pos){
            return vesselroutes;
        }
        if(simultaneousOp.get(vesselroutes.get(vessel).get(cur_pos).getID()) != null ||
                simultaneousOp.get(vesselroutes.get(vessel).get(new_pos).getID())!= null){
            System.out.println("Cannot exchange simultaneousOp with this method");
            return vesselroutes;
        }

        int pos1 = Math.min(cur_pos,new_pos);
        int pos2 = Math.max(cur_pos,new_pos);

        int nStartnodes = startnodes.length;
        int old_pos1_dist;
        int old_pos2_dist;
        int oldObjValue = objValue;

        if (pos1==0) {
            old_pos1_dist = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1 + 1).getID()-1];
        } else{
            old_pos1_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1];
        }
        System.out.println(old_pos1_dist + " Old pos 1 dist");


        if(pos2==vesselroutes.get(vessel).size()-1){
            old_pos2_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1] ;

        } else{
            old_pos2_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos2).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1];
        }
        System.out.println(old_pos2_dist + " Old pos 2 dist");

        //Make new copy of the vesselroutes list
        List<List<OperationInRoute>> new_vesselroutes = copyVesselroutes(vesselroutes);

        // Commit the change in the routes
        OperationInRoute toMove1 = new_vesselroutes.get(vessel).get(pos1);
        OperationInRoute toMove2 = new_vesselroutes.get(vessel).get(pos2);
        new_vesselroutes.get(vessel).remove(pos1);
        new_vesselroutes.get(vessel).add(pos2, toMove1);
        new_vesselroutes.get(vessel).remove(pos2-1);
        new_vesselroutes.get(vessel).add(pos1, toMove2);

        int new_first_dist;
        int new_second_dist;
        int new_first_sailing;
        int new_second_sailing;

        int second_pos1_sailing;

        if(pos1==0){
            new_first_sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel]-1][new_vesselroutes.get(vessel).get(pos1).getID()-1];
            second_pos1_sailing =  SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos2).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos1).getID()-nStartnodes-1][new_vesselroutes.get(vessel).get(pos2).getEarliestTime()]]
                    [new_vesselroutes.get(vessel).get(pos1).getID()-1][new_vesselroutes.get(vessel).get(pos1+1).getID()-1];
            new_first_dist = new_first_sailing + second_pos1_sailing;
            System.out.println(new_first_dist + " new first dist");

        } else{
            new_first_sailing = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][new_vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [new_vesselroutes.get(vessel).get(pos1-1).getID()-1][new_vesselroutes.get(vessel).get(pos1).getID()-1];
            second_pos1_sailing = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos2).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][new_vesselroutes.get(vessel).get(pos2).getEarliestTime()]]
                    [new_vesselroutes.get(vessel).get(pos1).getID()-1][new_vesselroutes.get(vessel).get(pos1+1).getID()-1] ;
            new_first_dist = new_first_sailing + second_pos1_sailing;
            System.out.println(new_first_dist + " new first dist");
        }


        //Assumption: We can use old Timeperiod for the time used on the operation and time used for sailing
        int new_last_sailing;
        if(pos2 == new_vesselroutes.get(vessel).size()-1){
            new_second_sailing = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][new_vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [new_vesselroutes.get(vessel).get(pos2-1).getID()-1][new_vesselroutes.get(vessel).get(pos2).getID()-1];
            new_last_sailing = 0;
            new_second_dist =  new_second_sailing ;
            System.out.println(new_second_dist + " new second dist");
        } else {
            new_second_sailing = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][new_vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [new_vesselroutes.get(vessel).get(pos2-1).getID()-1][new_vesselroutes.get(vessel).get(pos2).getID()-1];
            new_last_sailing = SailingTimes[vessel][new_vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][new_vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                    [new_vesselroutes.get(vessel).get(pos1).getEarliestTime()]][new_vesselroutes.get(vessel).get(pos2).getID()-1][new_vesselroutes.get(vessel).get(pos2+1).getID()-1];
            new_second_dist = new_second_sailing + new_last_sailing;

            System.out.println(new_second_dist + " new second dist");
        }


        //Calculating differences
        int first_delta = -(old_pos1_dist) + (new_first_dist);
        int second_delta = -(old_pos2_dist) + (new_second_dist);

        int sailingdelta = first_delta + second_delta;


        OperationInRoute oldLastOpVessel1 = vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1);
        OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel).get(new_vesselroutes.get(vessel).size() - 1);
        int vessel1Gain = 0;
        int old1Gain = 0;
        if(pos2 != vesselroutes.get(vessel).size()-1) {
            old1Gain = operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime()-1];
            vessel1Gain = - old1Gain + operationGain[vessel][newLastOpVessel1.getID() - startnodes.length - 1][0];
        }
        else{
            old1Gain = operationGain[vessel][newLastOpVessel1.getID() - startnodes.length - 1][newLastOpVessel1.getEarliestTime() - 1] +
                       operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1];
            vessel1Gain = - old1Gain + operationGain[vessel][newLastOpVessel1.getID() - startnodes.length - 1][0]
                                     + operationGain[vessel][oldLastOpVessel1.getID() - startnodes.length - 1][0];
        }
        int delta = - sailingdelta + vessel1Gain;

        if(delta <= 0){
            System.out.println("Objective change negative or unchanged, undesirable relocate. Returning old vesselroutes");
            return vesselroutes;
        }


        // Relocating the elements in the original vesselroutes list to secure the same object structure
        OperationInRoute toMoveOriginal1 = vesselroutes.get(vessel).get(pos1);
        OperationInRoute toMoveOriginal2 = vesselroutes.get(vessel).get(pos2);
        vesselroutes.get(vessel).remove(pos1);
        vesselroutes.get(vessel).add(pos2, toMoveOriginal1);
        vesselroutes.get(vessel).remove(pos2-1);
        vesselroutes.get(vessel).add(pos1, toMoveOriginal2);


        //Rearranging the new_vesselroutes lists to the original order
        OperationInRoute moveBack2 = new_vesselroutes.get(vessel).get(pos1);
        OperationInRoute moveBack1 = new_vesselroutes.get(vessel).get(pos2);
        new_vesselroutes.get(vessel).remove(pos1);
        new_vesselroutes.get(vessel).add(pos2,moveBack2);
        new_vesselroutes.get(vessel).remove(pos2-1);
        new_vesselroutes.get(vessel).add(pos1,moveBack1);

        int pos1earliest;
        int pos2latest;
        if(pos1 == 0) {
            pos1earliest = new_first_sailing;
        }else{
            pos1earliest = vesselroutes.get(vessel).get(pos1-1).getEarliestTime() + new_first_sailing +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-startnodes.length-1]
                            [vesselroutes.get(vessel).get(pos1-1).getEarliestTime()];
        }
        if(pos2 == vesselroutes.get(vessel).size()){
            pos2latest = nTimePeriods;
        }else{
            pos2latest = vesselroutes.get(vessel).get(pos2+1).getLatestTime() - new_last_sailing -
                    TimeVesselUseOnOperation[vessel][toMoveOriginal2.getID()-startnodes.length-1][toMoveOriginal1.getLatestTime()];
        }

        vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(pos1earliest,twIntervals[toMoveOriginal1.getID()-startnodes.length-1][0]));
        vesselroutes.get(vessel).get(pos2).setLatestTime(Math.min(pos2latest,twIntervals[toMoveOriginal2.getID()-startnodes.length-1][1]));

        updateEarliest(pos1earliest,pos1,vessel,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes,twIntervals, "oneexchange");
        updateLatest(pos2latest,pos2,vessel,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes,twIntervals, "oneexchange");

        updateConRoutes(simOpRoutes,precedenceOfRoutes, precedenceOverRoutes, vessel,vesselroutes,null,precedenceOverOperations,precedenceOfOperations,simultaneousOp,
                        SailingTimes,twIntervals,EarliestStartingTimeForVessel,startNodes,TimeVesselUseOnOperation);

        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(vessel),pos2,simOpRoutes,precedenceOfOperations,precedenceOverOperations,
                                                    TimeVesselUseOnOperation,startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselroutes,SailingTimes);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(vessel),pos2,TimeVesselUseOnOperation,startNodes,simOpRoutes,precedenceOverOperations,
                                                precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselroutes,simultaneousOp,SailingTimes);

        updateSimultaneous(simOpRoutes,vessel, simultaneousOp,
                precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                precedenceOfOperations,vesselroutes, pos1,pos2, "oneExchange");


        //Checking feasibility:
        if(!checkSolution(vesselroutes,startNodes,TimeVesselUseOnOperation,precedenceOverOperations,simultaneousOp)){
            List<List<OperationInRoute>> old_solution = retainOldSolution(new_vesselroutes,unroutedTasks,simultaneousOp,precedenceOfOperations,precedenceOverOperations);
            System.out.println("Taking back old solution");
            printLSOSolution(vesseltypes, old_solution,unroutedTasks);
            return old_solution;
        }

        //Checking profitability
        if(pos2 != vesselroutes.get(vessel).size()-1) {
            vessel1Gain = operationGain[vessel][vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1).getID()-startnodes.length-1]
                    [vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1).getEarliestTime()-1];
        }else{
            OperationInRoute lastOpVessel1 = vesselroutes.get(vessel).get(vesselroutes.get(vessel).size() - 1);
            vessel1Gain = operationGain[vessel][oldLastOpVessel1.getID()-startnodes.length-1][0] +
                    operationGain[vessel][lastOpVessel1.getID()-startnodes.length-1][Math.min(nTimePeriods-1,lastOpVessel1.getEarliestTime()-1)];
        }

        int opGainChange1 = routeOperationGain[vessel] - old1Gain + vessel1Gain;
        int newObjValue = oldObjValue + (sailingdelta*SailingCostForVessel[vessel]) + opGainChange1;
        if(newObjValue > oldObjValue){
            routeSailingCost[vessel] = routeSailingCost[vessel] + (sailingdelta*SailingCostForVessel[vessel]);
            routeOperationGain[vessel] = routeOperationGain[vessel] + opGainChange1;
            objValue = newObjValue;
        }else{
            System.out.println("Objective not improved");
            return retainOldSolution(new_vesselroutes,unroutedTasks,simultaneousOp,precedenceOfOperations,precedenceOverOperations);
        }

        System.out.println("Exchange performed");
        printLSOSolution(vesseltypes,vesselroutes,unroutedTasks);

        return vesselroutes;
    }


    public List<List<OperationInRoute>> two_exchange(List<List<OperationInRoute>> vesselroutes, int vessel1, int vessel2, int pos1,
                                                     int pos2, int[] startnodes, List<OperationInRoute> unroutedTasks){

        if ((vessel1 == vessel2) ||
                vesselroutes.get(vessel1).size()<1 || vesselroutes.get(vessel2).size()<1){
            System.out.println("Cannot exchange to same route, or one of the routes contains no tasks");
            return vesselroutes;
        }

        if(!((ConstructionHeuristic.containsElement (vesselroutes.get(vessel1).get(pos1).getID(), OperationsForVessel[vessel2])) &&
                ConstructionHeuristic.containsElement(vesselroutes.get(vessel2).get(pos2).getID(), OperationsForVessel[vessel1]))){
            System.out.println("One of the vessels cannot perform this task");
            return vesselroutes;
        }

        if(simultaneousOp.get(vesselroutes.get(vessel1).get(pos1).getID()) != null ||
                simultaneousOp.get(vesselroutes.get(vessel2).get(pos2).getID())!= null){
            System.out.println("Cannot exchange simultaneousOp with this method");
            return vesselroutes;
        }

        int nStartnodes = startnodes.length;
        int old_vessel1_dist;
        int old_vessel2_dist;
        int oldObjValue = objValue;


        if(vesselroutes.get(vessel1).size()==1){
            old_vessel1_dist = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");
        } else if(pos1==0) {
            old_vessel1_dist = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1 + 1).getID()-1];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else{
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        }

        if(vesselroutes.get(vessel2).size() == 1){
            old_vessel2_dist = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if(pos2==0) {
            old_vessel2_dist = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2 + 1).getID()-1];
            //+ TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1 - nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if(pos2==vesselroutes.get(vessel2).size()-1){
            old_vessel2_dist = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            // +TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else{
            old_vessel2_dist = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];
            //+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()] ;
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
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

        if(new_vesselroutes.get(vessel1).size() == 1){
            new_first_sailing = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1]-1][new_vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist = new_first_sailing;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1==0) {
            new_first_sailing = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startnodes[vessel1]-1][new_vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist = new_first_sailing  +
                    SailingTimes[vessel1][new_vesselroutes.get(vessel2).get(pos2).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1][new_vesselroutes.get(vessel1).get(pos1).getID()-1- nStartnodes][new_vesselroutes.get(vessel2).get(pos2).getEarliestTime()]]
                            [new_vesselroutes.get(vessel1).get(pos1).getID()-1][new_vesselroutes.get(vessel1).get(pos1+1).getID()-1];

            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1==new_vesselroutes.get(vessel1).size()-1){
            new_first_sailing = SailingTimes[vessel1][new_vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][new_vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][new_vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [new_vesselroutes.get(vessel1).get(pos1-1).getID()-1][new_vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist =  new_first_sailing;
            System.out.println(new_vessel1_dist + " New vessel1 dist");

        } else{
            new_first_sailing = SailingTimes[vessel1][new_vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][new_vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][new_vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [new_vesselroutes.get(vessel1).get(pos1-1).getID()-1][new_vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist = new_first_sailing +
                    SailingTimes[vessel1][new_vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel1][new_vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [new_vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][new_vesselroutes.get(vessel1).get(pos1).getID()-1][new_vesselroutes.get(vessel1).get(pos1+1).getID()-1];
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        }

        if(new_vesselroutes.get(vessel2).size() == 1) {
            new_second_sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][new_vesselroutes.get(vessel2).get(pos2).getID() - 1];
            new_vessel2_dist = new_second_sailing;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if(pos2==0) {
            new_second_sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startnodes[vessel2] - 1][new_vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][new_vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][new_vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [new_vesselroutes.get(vessel2).get(pos2).getID()-1][new_vesselroutes.get(vessel2).get(pos2 + 1).getID()-1];
            //      + TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2==new_vesselroutes.get(vessel2).size()-1){
            new_second_sailing = SailingTimes[vessel2][new_vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][new_vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [new_vesselroutes.get(vessel2).get(pos2-1).getID()-1][new_vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else{
            new_second_sailing = SailingTimes[vessel2][new_vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][new_vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [new_vesselroutes.get(vessel2).get(pos2-1).getID()-1][new_vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][Math.min(nTimePeriods-1,new_vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [new_vesselroutes.get(vessel1).get(pos1).getEarliestTime()])][new_vesselroutes.get(vessel2).get(pos2).getID()-1][new_vesselroutes.get(vessel2).get(pos2+1).getID()-1];

            System.out.println(new_vessel2_dist + " New vessel2 dist");
        }


        int vessel1_delta = -old_vessel1_dist + new_vessel1_dist;
        int vessel2_delta = -old_vessel2_dist + new_vessel2_dist;

        OperationInRoute oldLastOpVessel1 = vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size()-1);
        OperationInRoute newLastOpVessel2 = new_vesselroutes.get(vessel2).get(new_vesselroutes.get(vessel2).size()-1);
        int vessel1Gain = 0;
        int vessel2Gain = 0;
        int old1Gain = 0;
        int old2Gain = 0;
        if(pos1 != vesselroutes.get(vessel1).size()-1) {
            OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel1).get(new_vesselroutes.get(vessel1).size() - 1);
            old1Gain = operationGain[vessel1][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1] +
                    operationGain[vessel1][vesselroutes.get(vessel1).get(pos1).getID() - startnodes.length - 1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() - 1];
            vessel1Gain = -old1Gain + operationGain[vessel1][newLastOpVessel1.getID() - startnodes.length - 1][Math.min(nTimePeriods - 1, newLastOpVessel1.getEarliestTime() + vessel1_delta - 1)] +
                    operationGain[vessel1][new_vesselroutes.get(vessel1).get(pos1).getID() - startnodes.length - 1][0];
        }else {
            OperationInRoute newLastOpVessel1 = new_vesselroutes.get(vessel1).get(new_vesselroutes.get(vessel1).size() - 1);
            old1Gain = operationGain[vessel1][oldLastOpVessel1.getID() - startnodes.length - 1][oldLastOpVessel1.getEarliestTime() - 1];
            vessel1Gain = -old1Gain + operationGain[vessel1][new_vesselroutes.get(vessel1).get(pos1).getID() - startnodes.length - 1][0];
        }
        if(pos2 != new_vesselroutes.get(vessel2).size()-1) {
            OperationInRoute oldLastOpVessel2 = vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size() - 1);
            old2Gain = operationGain[vessel2][oldLastOpVessel2.getID() - startnodes.length - 1][oldLastOpVessel2.getEarliestTime()-1] +
                    operationGain[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-startnodes.length-1][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            vessel2Gain = - old2Gain + operationGain[vessel2][newLastOpVessel2.getID() - startnodes.length - 1][Math.min(nTimePeriods-1,newLastOpVessel2.getEarliestTime() + vessel2_delta-1)] +
                    operationGain[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID()-startnodes.length-1][0];
        }else{
            OperationInRoute oldLastOpVessel2 = vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size() - 1);
            old2Gain = operationGain[vessel2][oldLastOpVessel2.getID() - startnodes.length - 1][oldLastOpVessel2.getEarliestTime()-1];
            vessel2Gain = - old2Gain + operationGain[vessel2][new_vesselroutes.get(vessel2).get(pos2).getID()-startnodes.length-1][0];
        }

        int delta = - vessel1_delta - vessel2_delta + vessel1Gain + vessel2Gain;


        if(delta <= 0){
            System.out.println("Objective change negative or unchanged, undesirable relocate. Returning old vesselroutes");
            return vesselroutes;
        }


        // Exchanging the elements in the original vesselroutes list to secure the same object structure
        OperationInRoute toMoveOriginal1 = vesselroutes.get(vessel1).get(pos1);
        OperationInRoute toMoveOriginal2 = vesselroutes.get(vessel2).get(pos2);
        vesselroutes.get(vessel1).remove(pos1);
        vesselroutes.get(vessel2).remove(pos2);
        vesselroutes.get(vessel2).add(pos2, toMoveOriginal1);
        vesselroutes.get(vessel1).add(pos1, toMoveOriginal2);

        //Rearranging the new_vesselroutes lists to the original order
        OperationInRoute toMoveBack2 = new_vesselroutes.get(vessel1).get(pos1);
        OperationInRoute toMoveBack1 = new_vesselroutes.get(vessel2).get(pos2);
        new_vesselroutes.get(vessel1).remove(pos1);
        new_vesselroutes.get(vessel2).remove(pos2);
        new_vesselroutes.get(vessel2).add(pos2, toMoveBack2);
        new_vesselroutes.get(vessel1).add(pos1, toMoveBack1);

        if(pos1 == 0){
            int sailing = SailingTimes[vessel1][EarliestStartingTimeForVessel[vessel1]][startNodes[vessel1]-1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            vesselroutes.get(vessel1).get(pos1).setEarliestTime(Math.max(twIntervals[vesselroutes.get(vessel1).get(pos1).getID()-1-startnodes.length][0],sailing));
            updateEarliest(sailing, 0, vessel1, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes,twIntervals,"twoExchange");
        }else if(pos1 == vesselroutes.get(vessel1).size()-1){
            vesselroutes.get(vessel1).get(pos1).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel1).get(pos1).getID()-1-startnodes.length][1],nTimePeriods));
            updateLatest(vesselroutes.get(vessel1).get(pos1).getLatestTime(),pos1,vessel1,TimeVesselUseOnOperation,
                    startNodes,SailingTimes,vesselroutes,twIntervals,"twoExchange");
        }
        else {
            updateEarliest(vesselroutes.get(vessel1).get(pos1-1).getEarliestTime(), pos1 - 1, vessel1, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes,twIntervals,"twoExchange");
            updateLatest(vesselroutes.get(vessel1).get(pos1+1).getLatestTime(),pos1+1,vessel1, TimeVesselUseOnOperation,
                    startNodes,SailingTimes,vesselroutes,twIntervals,"twoExchange");
        }




        if(pos2 == 0){
            int sailing = SailingTimes[vessel2][EarliestStartingTimeForVessel[vessel2]][startNodes[vessel2]-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            vesselroutes.get(vessel2).get(pos2).setEarliestTime(Math.max(twIntervals[vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][0],sailing));
            updateEarliest(sailing, 0, vessel2, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes,twIntervals,"twoExchange");
        }else if(pos2== vesselroutes.get(vessel2).size()-1){
            vesselroutes.get(vessel2).get(pos2).setLatestTime(Math.min(twIntervals[vesselroutes.get(vessel2).get(pos2).getID()-1-startnodes.length][1],nTimePeriods));
            updateLatest(vesselroutes.get(vessel2).get(pos2).getLatestTime(),pos2,vessel2, TimeVesselUseOnOperation,
                    startNodes,SailingTimes,vesselroutes,twIntervals,"twoExchange");
        }else {
            updateEarliest(vesselroutes.get(vessel2).get(pos2 - 1).getEarliestTime(), pos2 - 1, vessel2, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes,twIntervals,"twoExchange");
            updateLatest(vesselroutes.get(vessel2).get(pos2 + 1).getLatestTime(), pos2 + 1, vessel2, TimeVesselUseOnOperation,
                    startNodes, SailingTimes, vesselroutes,twIntervals,"twoExchange");
        }

        List<Integer> updatedRoutes = new ArrayList<>(){{add(vessel2);}};
        updateConRoutes(simOpRoutes,precedenceOfRoutes, precedenceOverRoutes, vessel1,vesselroutes,updatedRoutes, precedenceOverOperations,precedenceOfOperations,simultaneousOp,
                        SailingTimes,twIntervals,EarliestStartingTimeForVessel,startNodes, TimeVesselUseOnOperation);

        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(vessel1),pos1,simOpRoutes,precedenceOfOperations,precedenceOverOperations,
                TimeVesselUseOnOperation,startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselroutes,SailingTimes);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(vessel1),pos1,TimeVesselUseOnOperation,startNodes,simOpRoutes,precedenceOverOperations,precedenceOfOperations,
                                                precedenceOfRoutes,precedenceOverRoutes,vesselroutes,simultaneousOp,SailingTimes);

        updateSimultaneous(simOpRoutes,vessel2, simultaneousOp,
                precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                precedenceOfOperations,vesselroutes,pos1, pos2,"twoExchange");

        //Checking feasibility:
        if(!checkSolution(vesselroutes,startNodes,TimeVesselUseOnOperation,precedenceOverOperations,simultaneousOp)){
            return retainOldSolution(new_vesselroutes,unroutedTasks,simultaneousOp,precedenceOfOperations,precedenceOverOperations);
        }

        //Checking profitability
        int opGainChange1;
        int opGainChange2;

        if(pos1 != new_vesselroutes.get(vessel1).size()-1){
            opGainChange1 = routeOperationGain[vessel1] - old1Gain
                    + operationGain[vessel1][vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size()-1).getID()-startnodes.length-1][vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size()-1).getEarliestTime()-1]
                    + operationGain[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-startnodes.length-1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()-1];
        }else{
            OperationInRoute newLastOpVessel1 = vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size() - 1);
            opGainChange1 = routeOperationGain[vessel1] - old1Gain +
                    operationGain[vessel1][newLastOpVessel1.getID()-startnodes.length-1][Math.min(nTimePeriods-1,newLastOpVessel1.getEarliestTime()-1)];
        }

        if (pos2 != vesselroutes.get(vessel2).size()-1){
            opGainChange2 = routeOperationGain[vessel2] - old2Gain
                    + operationGain[vessel2][vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getID()-startnodes.length-1][vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getEarliestTime()-1]
                    + operationGain[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-startnodes.length-1][vesselroutes.get(vessel2).get(pos2).getEarliestTime()-1];
        }else{
            opGainChange2 = routeOperationGain[vessel2] - old2Gain +
                    operationGain[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-startNodes.length-1][Math.min(nTimePeriods-1,vesselroutes.get(vessel2).get(pos2).getEarliestTime()-1)];
        }
        int newObjValue = oldObjValue + (vessel1_delta*SailingCostForVessel[vessel1]) + (vessel2_delta*SailingCostForVessel[vessel2]) + opGainChange1 + opGainChange2;
        if(newObjValue > oldObjValue){
            routeSailingCost[vessel1] = routeSailingCost[vessel1] + (vessel1_delta*SailingCostForVessel[vessel1]);
            routeSailingCost[vessel2] = routeSailingCost[vessel2] + (vessel2_delta*SailingCostForVessel[vessel2]);
            routeOperationGain[vessel1] = routeOperationGain[vessel1] + opGainChange1;
            routeOperationGain[vessel2] = routeOperationGain[vessel2] + opGainChange2;
            objValue = newObjValue;
        }else{
            System.out.println("Objective not improved");
            return retainOldSolution(new_vesselroutes,unroutedTasks,simultaneousOp,precedenceOfOperations,precedenceOverOperations);
        }

        System.out.println("Two exchange performed");
        printLSOSolution(vesseltypes,vesselroutes,unroutedTasks);

        if(bigTasksALNS[vesselroutes.get(vessel1).get(pos1).getID()-1-startNodes.length]!=null &&
                bigTasksALNS[vesselroutes.get(vessel1).get(pos1).getID()-startNodes.length-1][0] == vesselroutes.get(vessel1).get(pos1).getID()){
            if(consolidatedOperations.get(vesselroutes.get(vessel1).get(pos1).getID()) != null) {
                consolidatedOperations.get(vesselroutes.get(vessel1).get(pos1).getID()).setConsolidatedRoute(vessel2);
            }
        }

        if(bigTasksALNS[vesselroutes.get(vessel2).get(pos2).getID()-1-startNodes.length]!=null &&
                bigTasksALNS[vesselroutes.get(vessel2).get(pos2).getID()-startNodes.length-1][0] == vesselroutes.get(vessel2).get(pos2).getID()){
            if(consolidatedOperations.get(vesselroutes.get(vessel2).get(pos2).getID()) != null) {
                consolidatedOperations.get(vesselroutes.get(vessel2).get(pos2).getID()).setConsolidatedRoute(vessel2);
            }
        }


        return vesselroutes;

    }


    public List<List<OperationInRoute>> insert(List<List<OperationInRoute>> vesselroutes, OperationInRoute unrouted_operation, int vessel, int pos1,
                       int[] startnodes, List<OperationInRoute> unroutedTasks) {


        if(simALNS[unrouted_operation.getID()-1-startnodes.length][1] != 0 ||
                simALNS[unrouted_operation.getID()-1-startnodes.length][0] != 0){
            System.out.println("Cannot insert simultaneousOp with this method");
            return vesselroutes;
        }

        if(!(ConstructionHeuristic.containsElement (unrouted_operation.getID(), OperationsForVessel[vessel]))){
            System.out.println("Vessel cannot perform this task");
            return vesselroutes;
        }

        int nStartnodes = startnodes.length;
        //Make copy of vesselroutes list
        List<List<OperationInRoute>> new_vesselroutes = copyVesselroutes(vesselroutes);

        //Perform change
        vesselroutes.get(vessel).add(pos1,unrouted_operation);

        //Calculate new time
        int new_second_sailing;
        int new_dist;


        if(vesselroutes.get(vessel).size() == 1){
            new_second_sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_dist = new_second_sailing;
            System.out.println(new_dist + " New dist");
        } else if(pos1==0) {
            new_second_sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_dist = new_second_sailing +
                    SailingTimes[vessel][new_second_sailing +
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][new_second_sailing]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] ;
            System.out.println(new_dist + " New dist");
        } else if (pos1==vesselroutes.get(vessel).size()-1){
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_dist =  new_second_sailing ;
            System.out.println(new_dist + " New dist");

        } else{
            new_second_sailing = SailingTimes[vessel][Math.min(nTimePeriods-1,vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][Math.min(vesselroutes.get(vessel).get(pos1-1).getEarliestTime(), nTimePeriods-1)])]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_dist = new_second_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1+1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1+1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] ;
            System.out.println(new_dist + " New dist");
        }


        if(vesselroutes.get(vessel).size() == 1){
            int sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startNodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID() - 1];
            vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(sailing, twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));
            vesselroutes.get(vessel).get(pos1).setLatestTime(Math.min(nTimePeriods - 1, twIntervals[unrouted_operation.getID() - 1 - startnodes.length][1]));
        } else if(pos1 == 0) {
            int sailing = SailingTimes[vessel][EarliestStartingTimeForVessel[vessel]][startNodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID() - 1];
            vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(sailing, twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));

            int opTime = TimeVesselUseOnOperation[vessel][unrouted_operation.getID()-1-startNodes.length][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            int sailingFromNext = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+opTime]
                    [unrouted_operation.getID()-1-startNodes.length][vesselroutes.get(vessel).get(pos1+1).getID()-1];

            vesselroutes.get(vessel).get(pos1).setLatestTime(Math.min(vesselroutes.get(vessel).get(pos1+1).getLatestTime()-opTime-sailingFromNext,
                    twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));

        }else if(pos1== vesselroutes.get(vessel).size()-1) {
            vesselroutes.get(vessel).get(pos1).setLatestTime(Math.min(nTimePeriods - 1, twIntervals[unrouted_operation.getID() - 1 - startnodes.length][1]));

            int prevOp = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-startNodes.length]
                    [vesselroutes.get(vessel).get(pos1-1).getEarliestTime()];
            int sailingToOp = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+prevOp]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][unrouted_operation.getID()-1];

            vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+prevOp+sailingToOp,
                    twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));


        }else{
            int prevOp = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-startNodes.length]
                    [Math.min(vesselroutes.get(vessel).get(pos1-1).getEarliestTime(),nTimePeriods-1)];
            int sailingToOp = SailingTimes[vessel][Math.min(nTimePeriods-1,vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+prevOp)]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][unrouted_operation.getID()-1];

            vesselroutes.get(vessel).get(pos1).setEarliestTime(Math.max(vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+prevOp+sailingToOp,
                    twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));

            int opTime = TimeVesselUseOnOperation[vessel][unrouted_operation.getID()-1-startNodes.length][Math.min(nTimePeriods-1,vesselroutes.get(vessel).get(pos1).getEarliestTime())];
            int sailingFromNext = SailingTimes[vessel][Math.min(nTimePeriods-1,vesselroutes.get(vessel).get(pos1).getEarliestTime()+opTime)]
                    [unrouted_operation.getID()-1-startNodes.length][vesselroutes.get(vessel).get(pos1+1).getID()-1];

            vesselroutes.get(vessel).get(pos1).setLatestTime(Math.min(vesselroutes.get(vessel).get(pos1+1).getLatestTime()-opTime-sailingFromNext,
                    twIntervals[unrouted_operation.getID() - 1 - startnodes.length][0]));
        }


        //Checking preliminary feasibility
        if(vesselroutes.get(vessel).get(pos1).getLatestTime() <
                vesselroutes.get(vessel).get(pos1).getEarliestTime() ){
            System.out.println("Infeasible insertion - earliest time larger than latest time");
            vesselroutes.get(vessel).remove(pos1);
            return vesselroutes;
        }

        ConstructionHeuristic.updateEarliest(vesselroutes.get(vessel).get(pos1).getEarliestTime(),pos1,vessel,TimeVesselUseOnOperation,startNodes,
                SailingTimes,vesselroutes);
        ConstructionHeuristic.updateLatest(vesselroutes.get(vessel).get(pos1).getLatestTime(),pos1,vessel,TimeVesselUseOnOperation,startNodes,
                SailingTimes,vesselroutes);
        //updateConRoutes(simOpRoutes,precedenceOfRoutes, precedenceOverRoutes, vessel,vesselroutes,null);

        //ConstructionHeuristic.updatePrecedenceOver(checkPrecedence(routeIndex,0),indexInRoute);
        //ConstructionHeuristic.updatePrecedenceOf(checkPrecedence(routeIndex,1),indexInRoute);

        ConstructionHeuristic.updateIndexesInsertion(vessel,pos1,vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
        updateSimultaneous(simOpRoutes,vessel, simultaneousOp,
                precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                precedenceOfOperations,vesselroutes,pos1, 1000,"insert");

        //Checking feasibility:
        if(!checkSolution(vesselroutes,startNodes,TimeVesselUseOnOperation,precedenceOverOperations,simultaneousOp)){
            new_vesselroutes.get(vessel).remove(pos1);
            ConstructionHeuristic.updateIndexesRemoval(vessel,pos1,new_vesselroutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
            return retainOldSolution(new_vesselroutes,unroutedTasks,simultaneousOp,precedenceOfOperations,precedenceOverOperations);
        }

        System.out.println("Unrouted insertion performed");
        unroutedTasks.remove(unroutedTasks.indexOf(unrouted_operation));
        printLSOSolution(vesseltypes,vesselroutes,unroutedTasks);
        objValue = objValue + (new_dist*SailingCostForVessel[vessel]) + operationGain[vessel]
                [vesselroutes.get(vessel).get(pos1).getID()-1-startNodes.length][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
        routeSailingCost[vessel] = routeSailingCost[vessel] + (new_dist*SailingCostForVessel[vessel]);
        routeOperationGain[vessel] = routeOperationGain[vessel] + operationGain[vessel]
                [vesselroutes.get(vessel).get(pos1).getID()-1-startNodes.length][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
        return vesselroutes;
    }


    /*

    public List<List<OperationInRoute>> insertSim(List<List<OperationInRoute>> vesselroutes, int[] startnodes, List<OperationInRoute> unroutedTasks){
        for(OperationInRoute unroutesSim1 : unroutedTasks) {
            if (simALNS[unroutesSim1.getID() - 1 - startnodes.length][1] == 0 ||
                    simALNS[unroutesSim1.getID() - 1 - startnodes.length][0] == 0) {

                for(int vessel=0;vessel<vesselroutes.size()-1;vessel++){
                    if(ConstructionHeuristic.containsElement (unroutesSim1.getID(), OperationsForVessel[vessel])){
                        for(int pos1=0;pos1<vesselroutes.get(vessel).size()-1;pos1++){


                        }
                    }
                }

            }
        }
    }

     */

    public static void updateEarliest(int earliest, int indexInRoute, int routeIndex,
                                      int [][][] TimeVesselUseOnOperation, int[] startNodes, int[][][][] SailingTimes,
                                      List<List<OperationInRoute>> vesselroutes, int[][] twIntervals, String method){
        int lastEarliest=earliest;
        //System.out.println("WITHIN UPDATE EARLIEST");
        //System.out.println("Last earliest time: " + lastEarliest);
        for(int f=indexInRoute+1;f<vesselroutes.get(routeIndex).size();f++){
            //System.out.println("Index updating: "+f);
            OperationInRoute objectFMinus1=vesselroutes.get(routeIndex).get(f-1);
            OperationInRoute objectF=vesselroutes.get(routeIndex).get(f);
            int opTimeFMinus1=TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1]
                    [Math.min(SailingTimes[0].length-1,objectFMinus1.getEarliestTime())];
            //System.out.println("ID operation "+ vesselroutes.get(routeIndex).get(f).getID() + " , " +"Route: "+ routeIndex);
            //System.out.println("Earliest already assigned F: "+ objectF.getEarliestTime() + " , " + "Potential update earliest F: "+
            //        (lastEarliest+
            //                SailingTimes[routeIndex][objectFMinus1.getEarliestTime()+opTimeFMinus1-1][objectFMinus1.getID()-1][objectF.getID()-1]
            //                +TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1][objectFMinus1.getEarliestTime()-1]));
            int newTime=Math.max(lastEarliest+
                            SailingTimes[routeIndex][Math.min(SailingTimes[0].length-1,objectFMinus1.getEarliestTime()+opTimeFMinus1-1)][objectFMinus1.getID()-1][objectF.getID()-1]
                            +TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1][Math.min(SailingTimes[0].length-1,objectFMinus1.getEarliestTime())],
                    twIntervals[objectF.getID()-startNodes.length-1][0]);

            //System.out.println("New time: "+ newTime + " , " + "ID F: " +objectF.getID());
            if(!method.equals("oneexchange") && newTime==objectF.getEarliestTime()){
                break;
            }
            vesselroutes.get(routeIndex).get(f).setEarliestTime(newTime);
            //System.out.println("Setting earliest time of operation: " + vesselroutes.get(routeIndex).get(f).getID() + " to " + vesselroutes.get(routeIndex).get(f).getEarliestTime());
            //System.out.println(objectF.getEarliestTime());
            lastEarliest=newTime;

        }
    }

    public static void updateLatest(int latest, int indexInRoute, int routeIndex,
                                    int [][][] TimeVesselUseOnOperation, int[] startNodes, int[][][][] SailingTimes,
                                    List<List<OperationInRoute>> vesselroutes, int[][] twIntervals, String method){
        int lastLatest=latest;
        //System.out.println("WITHIN UPDATE LATEST");
        //System.out.println("Last latest time: " + lastLatest);
        for(int k=indexInRoute-1;k>-1;k--){
            //System.out.println("Index updating: "+k);
            OperationInRoute objectK=vesselroutes.get(routeIndex).get(k);
            int opTimeK=TimeVesselUseOnOperation[routeIndex][objectK.getID()-startNodes.length-1]
                    [objectK.getLatestTime()-1];
            int updateSailingTime=0;
            //System.out.println("ID operation "+ vesselroutes.get(routeIndex).get(k).getID() + " , " +"Route: "+ routeIndex);

            if(k==vesselroutes.get(routeIndex).size()-2){
                updateSailingTime=objectK.getLatestTime();
            }
            if(k<vesselroutes.get(routeIndex).size()-2){
                updateSailingTime=objectK.getLatestTime()+opTimeK;
            }
            //System.out.println("Latest already assigned K: "+ objectK.getLatestTime() + " , " + "Potential update latest K: "+
            //        (lastLatest- SailingTimes[routeIndex][updateSailingTime-1][objectK.getID()-1]
            //                [vesselroutes.get(routeIndex).get(k+1).getID()-1] -opTimeK)) ;
            int newTime=Math.min(lastLatest-
                            SailingTimes[routeIndex][Math.min(SailingTimes[0].length-1,updateSailingTime-1)][objectK.getID()-1]
                                    [vesselroutes.get(routeIndex).get(k+1).getID()-1] -opTimeK,
                    twIntervals[objectK.getID()-startNodes.length-1][1]);
            //System.out.println("New time: "+ newTime + " , " + "ID K: " +objectK.getID());

            if(!method.equals("oneexchange") && newTime==objectK.getLatestTime()){
                break;
            }
            objectK.setLatestTime(newTime);
            //System.out.println(objectK.getLatestTime());
            lastLatest=newTime;
        }
    }

    public static void updateSimultaneous(List<Map<Integer, ConnectedValues>> simOpRoutes, int routeIndex,
                                          Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, PrecedenceValues>> precedenceOverRoutes,
                                          List<Map<Integer, PrecedenceValues>> precedenceOfRoutes, int[][][] TimeVesselUseOnOperation,
                                          int[] startNodes, int[][][][] SailingTimes, Map<Integer, PrecedenceValues> precedenceOverOperations,
                                          Map<Integer, PrecedenceValues> precedenceOfOperations, List<List<OperationInRoute>> vesselroutes,
                                          int pos1, int pos2, String function) {

        if(simOpRoutes.get(routeIndex)!=null){
            for (ConnectedValues sValues : simOpRoutes.get(routeIndex).values()) {
                //System.out.println("Update caused by simultaneous " + sValues.getOperationObject().getID() + " in route " + routeIndex);
                int cur_earliestTemp = sValues.getOperationObject().getEarliestTime();
                int cur_latestTemp = sValues.getOperationObject().getLatestTime();
                //System.out.println(cur_earliestTemp + " cur earliest temp of " + sValues.getOperationObject().getID());

                int sIndex = sValues.getIndex();
                if(function.equals("onerelocate")) {
                    if (pos1 < sIndex && pos2 > sIndex) {
                        sValues.setIndex(sIndex - 1);
                    } else if (pos1 > sIndex && pos2 <= sIndex) {
                        sValues.setIndex(sIndex + 1);
                    }
                }

                if(sValues.getConnectedOperationObject() != null) {
                    ConnectedValues simOp = simultaneousOp.get(sValues.getConnectedOperationObject().getID());

                    int earliestPO;
                    int latestPO;
                    if(simOp.getIndex() == 0){
                        earliestPO = SailingTimes[simOp.getRoute()][0][startNodes[simOp.getRoute()]-1][simOp.getOperationObject().getID()-1];
                    }else {
                        int prevOpTime = TimeVesselUseOnOperation[simOp.getRoute()][vesselroutes.get(simOp.getRoute()).get(simOp.getIndex()-1).getID()-startNodes.length-1]
                                [Math.min(SailingTimes[0].length-1,vesselroutes.get(simOp.getRoute()).get(simOp.getIndex()-1).getEarliestTime())];
                        int SailingPrevOptoOp = SailingTimes[simOp.getRoute()][Math.min(SailingTimes[0].length-1,vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getEarliestTime()+prevOpTime)]
                                [vesselroutes.get(simOp.getRoute()).get(simOp.getIndex()-1).getID()-1][simOp.getOperationObject().getID() - 1];
                        earliestPO = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getEarliestTime() + prevOpTime + SailingPrevOptoOp;
                    }
                    int earliestTemp = Math.max(cur_earliestTemp, earliestPO);
                    if(simOp.getIndex()==vesselroutes.get(simOp.getRoute()).size()-1){
                        latestPO = SailingTimes[0].length;
                    }else {
                        int nextOpTime = TimeVesselUseOnOperation[simOp.getRoute()][simOp.getOperationObject().getID() - startNodes.length - 1]
                                [Math.min(SailingTimes[0].length-1,simOp.getOperationObject().getLatestTime())];
                        System.out.println(simOp.getOperationObject().getLatestTime() + " latest time of operation " + simOp.getOperationObject().getID());
                        int SailingOptoNextOp = SailingTimes[simOp.getRoute()][Math.min(SailingTimes[0].length-1,simOp.getOperationObject().getLatestTime()+nextOpTime)]
                                [simOp.getOperationObject().getID() - 1][vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() + 1).getID()];
                        latestPO = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() + 1).getLatestTime() - nextOpTime - SailingOptoNextOp;
                    }
                    int latestTemp = Math.min(cur_latestTemp, latestPO);


                    if (earliestTemp > cur_earliestTemp) {
                        cur_earliestTemp = earliestTemp;
                        sValues.getOperationObject().setEarliestTime(cur_earliestTemp);
                        //System.out.println("Setting earliest time of operation: " + sValues.getOperationObject().getID() + " to " + sValues.getOperationObject().getEarliestTime() +
                        //        " with index " + sValues.getIndex() + " index in route: " + indexInRoute);
                        ConstructionHeuristic.updateEarliest(cur_earliestTemp,sValues.getIndex(),routeIndex,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes);
                    }else if(earliestTemp>earliestPO){
                        simOp.getOperationObject().setEarliestTime(cur_earliestTemp);
                        //System.out.println("Setting earliest time of operation: " + simOp.getOperationObject().getID() + " to " + simOp.getOperationObject().getEarliestTime());
                        //System.out.println(simOp.getOperationObject().getEarliestTime() + " setting the earliest time of " + simOp.getOperationObject().getID() );
                        ConstructionHeuristic.updateEarliest(cur_earliestTemp,simOp.getIndex(),simOp.getRoute(), TimeVesselUseOnOperation, startNodes,SailingTimes, vesselroutes);

                        ConstructionHeuristic.updateSimultaneous(simOpRoutes,simOp.getRoute(),simOp.getIndex(),simultaneousOp,precedenceOverRoutes,
                                precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,precedenceOfOperations,vesselroutes);



                        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(simOp.getRoute()),simOp.getIndex(),simOpRoutes,precedenceOfOperations,
                                precedenceOverOperations,TimeVesselUseOnOperation,startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselroutes,SailingTimes);


                    }
                    if (latestTemp < cur_latestTemp) {
                        cur_latestTemp = latestTemp;
                        sValues.getOperationObject().setLatestTime(cur_latestTemp);
                        //System.out.println(sValues.getOperationObject().getLatestTime() + " setting the latest time of " + sValues.getOperationObject().getID() );
                        ConstructionHeuristic.updateLatest(cur_latestTemp,sValues.getIndex(),routeIndex,TimeVesselUseOnOperation, startNodes,SailingTimes, vesselroutes);
                    }else if(latestTemp<latestPO){
                        //System.out.println(latestTemp + " , " +latestPO + " , " + cur_latestTemp);
                        simOp.getOperationObject().setLatestTime(cur_latestTemp);
                        //System.out.println(simOp.getOperationObject().getLatestTime() + " setting the latest time of " + simOp.getOperationObject().getID() );

                        ConstructionHeuristic.updateLatest(cur_latestTemp,simOp.getIndex(),simOp.getRoute(),TimeVesselUseOnOperation, startNodes,SailingTimes, vesselroutes);
                        ConstructionHeuristic.updateSimultaneous(simOpRoutes,simOp.getRoute(),simOp.getIndex(),
                                simultaneousOp,precedenceOverRoutes, precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                                precedenceOfOperations,vesselroutes);
                        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(simOp.getRoute()),simOp.getIndex(),TimeVesselUseOnOperation,startNodes,simOpRoutes,
                                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselroutes,simultaneousOp,SailingTimes);


                    }
                }
            }
        }
    }


    public static void updateConRoutes(List<Map<Integer,ConnectedValues>> simOpRoutes, List<Map<Integer,PrecedenceValues>> precedenceOfRoutes,
                                List<Map<Integer,PrecedenceValues>> precedenceOverRoutes, int v,
                                List<List<OperationInRoute>> vesselroutes, List<Integer> updatedRoutes, Map<Integer,PrecedenceValues> precedenceOverOperations,
                                       Map<Integer,PrecedenceValues> precedenceOfOperations, Map<Integer,ConnectedValues> simultaneousOp,
                                       int[][][][] SailingTimes, int[][] twIntervals, int[] EarliestStartingTimeForVessel,
                                       int [] startNodes, int[][][] TimeVesselUseOnOperation){
        List<Integer> routesToUpdate = new ArrayList<>();
        /*
        if(!simOpRoutes.get(v).isEmpty()){
            for(ConnectedValues op : simOpRoutes.get(v).values()){
                int conRoute = op.getConnectedRoute();
                if(!routesToUpdate.contains(conRoute) && !vesselroutes.get(conRoute).isEmpty()) {
                    routesToUpdate.add(conRoute);
                }
            }
        }
        if(!precedenceOfRoutes.get(v).isEmpty()){
            for(PrecedenceValues op : precedenceOfRoutes.get(v).values()){
                int conRoute = op.getConnectedRoute();
                if(!routesToUpdate.contains(conRoute) && !vesselroutes.get(conRoute).isEmpty()) {
                    routesToUpdate.add(conRoute);
                }
            }
        }
        if(!precedenceOverRoutes.get(v).isEmpty()){
            for(PrecedenceValues op : precedenceOverRoutes.get(v).values()){
                 OperationInRoute conRoute = op.getConnectedOperationObject();
                 int conVessel = op.getConnectedRoute();
                if(conRoute != null && !routesToUpdate.contains(conVessel) && !vesselroutes.get(conVessel).isEmpty()) {
                    routesToUpdate.add(conVessel);
                }
            }
        }*/
        for(int route = 0; route<vesselroutes.size()-1;route++) {
            if(!simOpRoutes.get(route).isEmpty() || !precedenceOfRoutes.get(route).isEmpty() || !precedenceOverRoutes.get(route).isEmpty()){
                routesToUpdate.add(route);
            }
        }
        if(updatedRoutes != null) {
            for (int route : updatedRoutes) {
                if (routesToUpdate.contains(route)) {
                    routesToUpdate.remove(Integer.valueOf(route));
                }
            }
        }
        for(int route : routesToUpdate) {
            if (vesselroutes.get(route) != null && vesselroutes.get(route).size() != 0) {
                System.out.println("Updating route: " + route);
                int earliest = Math.max(SailingTimes[route][EarliestStartingTimeForVessel[route]][startNodes[route] - 1]
                        [vesselroutes.get(route).get(0).getID() - 1] + 1, twIntervals[vesselroutes.get(route).get(0).getID() - 1][0]);
                int latest = Math.min(SailingTimes[0].length, twIntervals
                        [vesselroutes.get(route).get(vesselroutes.get(route).size() - 1).getID() - 1 - startNodes.length][1]);
                vesselroutes.get(route).get(0).setEarliestTime(earliest);
                vesselroutes.get(route).get(vesselroutes.get(route).size() - 1).setLatestTime(latest);
                updateEarliest(earliest, 0, route, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes, twIntervals, "oneexchange");
                updateLatest(latest, vesselroutes.get(route).size() - 1, route, TimeVesselUseOnOperation,
                        startNodes, SailingTimes, vesselroutes, twIntervals, "oneexchange");
            }
        }
        for(int route : routesToUpdate) {
            if (vesselroutes.get(route) != null && vesselroutes.get(route).size() != 0) {
                ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(route), 0, simOpRoutes, precedenceOfOperations, precedenceOverOperations, TimeVesselUseOnOperation, startNodes, precedenceOverRoutes,
                        precedenceOfRoutes, simultaneousOp, vesselroutes, SailingTimes);
                ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(route), vesselroutes.get(route).size() - 1, TimeVesselUseOnOperation, startNodes, simOpRoutes, precedenceOverOperations, precedenceOfOperations,
                        precedenceOfRoutes, precedenceOverRoutes, vesselroutes, simultaneousOp, SailingTimes);
                ConstructionHeuristic.updateSimultaneous(simOpRoutes,route,0,simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,precedenceOfOperations,vesselroutes);
            }
        }
    }


    public Boolean checkCrosSyncSim(Map<Integer,ConnectedValues> simOps, int o, int v, int insertIndex){
        if(!simOps.isEmpty()) {
            for(ConnectedValues op : simOps.values()) {
                ConnectedValues conOp = simultaneousOp.get(op.getConnectedOperationID());
                if (simALNS[o - startNodes.length - 1][0] != 0 &&
                        simultaneousOp.get(simALNS[o - startNodes.length - 1][0]).getRoute() == conOp.getRoute()) {
                    if ((simultaneousOp.get(simALNS[o - startNodes.length - 1][0]).getIndex() - conOp.getIndex() > 0 &&
                            insertIndex - op.getIndex() < 0) || (simultaneousOp.get(simALNS[o - startNodes.length - 1][0]).getIndex() -
                            conOp.getIndex() < 0 && insertIndex - op.getIndex() > 0)) {
                        return false;
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
                        System.out.println("Earliest time is larger than latest time, infeasible move");
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
                    System.out.println("Earliest and/or latest time for simultaneous op do not match, infeasible move");
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

    public static List<List<OperationInRoute>> retainOldSolution(List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks,
                                                                 Map<Integer, ConnectedValues> simultaneousOp, Map<Integer, PrecedenceValues> precedenceOfOperations,
                                                                 Map<Integer, PrecedenceValues> precedenceOverOperations){
        if(!simultaneousOp.isEmpty()){
            for(ConnectedValues op : simultaneousOp.values()){
                //if(vesselroutes.get(op.getRoute()).get(op.getIndex()).getID() == op.getOperationObject().getID()) {
                    int currentEarliest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getEarliestTime();
                    int currentLatest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getLatestTime();
                    vesselroutes.get(op.getRoute()).remove(op.getIndex());
                    op.getOperationObject().setLatestTime(currentLatest);
                    op.getOperationObject().setEarliestTime(currentEarliest);
                    vesselroutes.get(op.getRoute()).add(op.getIndex(), op.getOperationObject());
                //}
            }
        }
        if(!precedenceOfOperations.isEmpty()){
            for(PrecedenceValues op : precedenceOfOperations.values()){
                if(vesselroutes.get(op.getRoute()).get(op.getIndex()).getID() == op.getOperationObject().getID()) {
                    int currentEarliest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getEarliestTime();
                    int currentLatest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getLatestTime();
                    vesselroutes.get(op.getRoute()).remove(op.getIndex());
                    op.getOperationObject().setLatestTime(currentLatest);
                    op.getOperationObject().setEarliestTime(currentEarliest);
                    vesselroutes.get(op.getRoute()).add(op.getIndex(), op.getOperationObject());
                }
            }
        }
        if(!precedenceOverOperations.isEmpty()){
            for(PrecedenceValues op : precedenceOverOperations.values()){
                if(vesselroutes.get(op.getRoute()).get(op.getIndex()).getID() == op.getOperationObject().getID()) {
                    int currentEarliest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getEarliestTime();
                    int currentLatest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getLatestTime();
                    vesselroutes.get(op.getRoute()).remove(op.getIndex());
                    op.getOperationObject().setLatestTime(currentLatest);
                    op.getOperationObject().setEarliestTime(currentEarliest);
                    vesselroutes.get(op.getRoute()).add(op.getIndex(), op.getOperationObject());
                }
            }
        }
        return vesselroutes;
    }

    public List<List<OperationInRoute>> searchAll (List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks){
        for(int vessel=0; vessel<vesselroutes.size();vessel++){
            if (vesselroutes.get(vessel) != null) {
                for (int vessel2 = 0; vessel2 < vesselroutes.size(); vessel2++) {
                    if (vesselroutes.get(vessel2) == null) {
                        int position = 0;
                        for (int task = 0; task < vesselroutes.get(vessel).size(); task++) {
                            System.out.println(vessel + " , " + vessel2 + " , " + task + " , " + position);
                            vesselroutes = two_relocate(vesselroutes, vessel, vessel2, task, position, startNodes, unroutedTasks);
                            //printLSOSolution(vesseltypes,vesselroutes,unroutedTasks);
                        }
                    }else{
                        for (int position = 0; position < vesselroutes.get(vessel2).size()+1; position++) {
                            for (int task = 0; task < vesselroutes.get(vessel).size(); task++) {
                                System.out.println(vessel + " , " + vessel2 + " , " + task + " , " + position);
                                //vesselroutes = two_exchange(vesselroutes, vessel, vessel2, task, position, startNodes, unroutedTasks);
                                vesselroutes = two_relocate(vesselroutes, vessel, vessel2, task, position, startNodes, unroutedTasks);
                                //vesselroutes = one_relocate(vesselroutes,vessel,task,position,startNodes,unroutedTasks);
                                //printLSOSolution(vesseltypes,vesselroutes,unroutedTasks);
                            }
                        }
                    }
                }
            }
        }
        return vesselroutes;
    }

    public List<List<OperationInRoute>> insertAll (List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks){
        for(int vessel=1; vessel<vesselroutes.size();vessel++){
            if (!unroutedTasks.isEmpty()) {
                for (OperationInRoute task : unroutedTasks) {
                    for (int position = 0; position < vesselroutes.get(vessel).size(); position++) {
                        System.out.println(vessel + " , " + task.getID() + " , " + position);
                        insert(vesselroutes,task,vessel,position,startNodes,unroutedTasks);
                        //printLSOSolution(vesseltypes,vesselroutes,unroutedTasks);
                    }
                }
            }
        }
        return vesselroutes;
    }



    public void printLSOSolution(int[] vessseltypes, List<List<OperationInRoute>> vesselroutes,
                                 List<OperationInRoute> unroutedTasks){
        //PrintData.timeVesselUseOnOperations(TimeVesselUseOnOperation,startNodes.length);
        //PrintData.printSailingTimes(SailingTimes,3,nOperations-2*startNodes.length,startNodes.length);
        //PrintData.printOperationsForVessel(OperationsForVessel);
        //PrintData.printPrecedenceALNS(precedenceALNS);
        //PrintData.printSimALNS(simALNS);
        //PrintData.printTimeWindows(timeWindowsForOperations);
        //PrintData.printTimeWindowsIntervals(twIntervals);

        System.out.println("Sailing cost per route: "+ Arrays.toString(routeSailingCost));
        System.out.println("Operation gain per route: "+Arrays.toString(routeOperationGain));
        System.out.println("Objective value: "+objValue);
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

        for(Map.Entry<Integer, ConnectedValues> entry : simultaneousOp.entrySet()){
            ConnectedValues simOp = entry.getValue();
            System.out.println("Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                    simOp.getRoute() + " with index: " + simOp.getIndex());
        }

    }



    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes = new int[]{1,2,4,5};
        int[] startnodes=new int[]{1,2,3,4};
        DataGenerator dg = new DataGenerator(vesseltypes, 5, startnodes,
                "test_instances/20-5_e_m.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),startnodes.length);
        //PrintData.printOperationGain(dg.getOperationGain(), startnodes.length);
        PrintData.printSailingTimes(dg.getSailingTimes(),2,27, 4);
        //PrintData.printSailingTimes(dg.getSailingTimes(),3,23, 4);
        ConstructionHeuristic a = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(),dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations());
        a.createSortedOperations();
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);
        LS_operators LSO = new LS_operators(dg.getOperationsForVessel(),vesseltypes,dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),
                dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),dg.getTwIntervals(),a.getRouteSailingCost(),a.getRouteOperationGain(),
                dg.getStartNodes(), dg.getSimultaneousALNS(),dg.getBigTasksALNS(), a.getOperationGain(),
                a.getPrecedenceOverOperations(), a.getPrecedenceOfOperations(),a.getSimultaneousOp(),
                a.getSimOpRoutes(),a.getPrecedenceOfRoutes(), a.getPrecedenceOverRoutes(), a.getConsolidatedOperations());
        //List<List<OperationInRoute>> new_vesselroutes = LSO.two_relocate(a.vesselroutes,1,3,4,0,startnodes,a.getUnroutedTasks());
        List<List<OperationInRoute>> new_vesselroutes = LSO.searchAll(a.vesselroutes, a.getUnroutedTasks());
        //List<List<OperationInRoute>> new_vesselroutes = LSO.insert(a.vesselroutes,a.unroutedTasks.get(2),1,1, startnodes,a.getUnroutedTasks());
        LSO.printLSOSolution(vesseltypes,new_vesselroutes,a.getUnroutedTasks());
    }

}