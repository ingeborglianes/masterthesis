import javax.sound.midi.Soundbank;
import java.io.FileNotFoundException;
import java.util.*;

public class LS_operators {
    private int [][] OperationsForVessel;
    private int[] vesseltypes;
    private int[][][][] SailingTimes;
    private  int [][][] TimeVesselUseOnOperation;
    private int [] startNodes;
    private int [][] twIntervals;
    private int nTimePeriods = 60;
    private int [] routeSailingCost;
    //private int[] routeOperationGain;
    private int objValue;
    private int[][] simALNS;
    //map for operations that are connected with precedence. ID= operation number. Value= Precedence value.
    private Map<Integer,PrecedenceValues> precedenceOverOperations;
    private Map<Integer,PrecedenceValues> precedenceOfOperations;
    //List for operations that are connected as simultaneous operations. ID= operation number. Value= Simultaneous value.
    private Map<Integer, ConnectedValues> simultaneousOp;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;
    private List<Map<Integer,PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer,PrecedenceValues>> precedenceOverRoutes;




    public LS_operators(int [][] OperationsForVessel, int[] vesseltypes, int[][][][] SailingTimes, int [][][] TimeVesselUseOnOperation,
                        int [][] twIntervals, int [] routeSailingCost, int objValue, int[] startNodes, int[][] simALNS,
                        Map<Integer,PrecedenceValues> precedenceOverOperations,Map<Integer,PrecedenceValues> precedenceOfOperations,
                        Map<Integer, ConnectedValues> simultaneousOp,List<Map<Integer, ConnectedValues>> simOpRoutes,
                        List<Map<Integer,PrecedenceValues>> precedenceOfRoutes, List<Map<Integer,PrecedenceValues>> precedenceOverRoutes){
        this.OperationsForVessel = OperationsForVessel;
        this.vesseltypes = vesseltypes;
        this.SailingTimes = SailingTimes;
        this.TimeVesselUseOnOperation = TimeVesselUseOnOperation;
        this.twIntervals = twIntervals;
        this.routeSailingCost = routeSailingCost;
        this.objValue = objValue;
        this.startNodes = startNodes;
        this.simALNS = simALNS;
        this.precedenceOverOperations =precedenceOverOperations;
        this.precedenceOfOperations = precedenceOfOperations;
        this.simultaneousOp = simultaneousOp;
        this.simOpRoutes = simOpRoutes;
        this.precedenceOfRoutes = precedenceOfRoutes;
        this.precedenceOverRoutes = precedenceOverRoutes;
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


    public List<List<OperationInRoute>> one_relocate(List<List<OperationInRoute>> vesselroutes, int vessel, int pos1, int pos2, int[] startnodes){
        if (pos1 == pos2){
            return vesselroutes;
        }

        if(simultaneousOp.get(vesselroutes.get(vessel).get(pos1).getID()) != null){
            System.out.println("Cannot relocate simultaneousOp with this method");
            return vesselroutes;
        }

        int nStartnodes = startnodes.length;
        int old_pos1_dist;
        int old_pos2_dist;

        //Tracker gammel tid for både pos 1 og pos 2 (21.02)
        if(pos1==0) {

            old_pos1_dist = SailingTimes[vessel][0][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1] +
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
            old_pos2_dist = SailingTimes[vessel][0][startnodes[vessel]-1][vesselroutes.get(vessel).get(pos2).getID()-1];
        }
        else if(pos2 == vesselroutes.get(vessel).size()-1){
            old_pos2_dist = 0;
        } else {
            old_pos2_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1];
            System.out.println(old_pos2_dist + " old pos 2 dist");

        }
        // Save old vesselroutes list
        List<List<OperationInRoute>> old_vesselroutes = copyVesselroutes(vesselroutes);

        // Commit changes in route
        OperationInRoute toMove = vesselroutes.get(vessel).get(pos1);
        vesselroutes.get(vessel).remove(pos1);
        vesselroutes.get(vessel).add(pos2, toMove);


        //Track new times - oppdatert 21.02

        int new_pos1_dist;
        int new_pos2_dist;
        int new_second_sailing;

        if(pos1==0){
            //System.out.println("Sailing from: " + vesselroutes.get(vessel).get(new_pos).getID() + " sailing to: " + vesselroutes.get(vessel).get(cur_pos+1).getID() +
            //        " it takes time periods: " + SailingTimes[vessel][vesselroutes.get(vessel).get(new_pos).getTimeperiod()][vesselroutes.get(vessel).get(new_pos).getID()][vesselroutes.get(vessel).get(cur_pos+1).getID()]
            //        + " in time peiod: " + vesselroutes.get(vessel).get(new_pos).getTimeperiod());

            new_pos1_dist = SailingTimes[vessel][0][startnodes[vessel]-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            System.out.println(new_pos1_dist + " new pos 1 dist");
        } else{
            new_pos1_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            System.out.println(new_pos1_dist + " new pos 1 dist");
        }

        if(pos2==0) {
            new_second_sailing = SailingTimes[vessel][0][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist = new_second_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1] ;
                            // +TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(new_pos2_dist + " New pos 2 dist");
        } else if (pos2==vesselroutes.get(vessel).size()-1){
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist =  new_second_sailing ;
                    // + TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()];
            System.out.println(new_pos2_dist + " New pos 2 dist");

        } else{
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist = new_second_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1] ;
                            //+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
            System.out.println(new_pos2_dist + " New pos 2 dist");
        }

        //Calculating differences
        int first_delta;
        int second_delta;
        if (pos1<pos2){
            first_delta = -(old_pos1_dist) + (new_pos1_dist);
            second_delta = -(old_pos2_dist) + (new_pos2_dist);
        }else{
            first_delta = -(old_pos2_dist) + (new_pos2_dist);
            second_delta = -(old_pos1_dist) + (new_pos1_dist);
        }

        int delta = first_delta + second_delta;
        if(delta > 0){
            System.out.println("Sailing time increased, undesirable relocate. Returning old vesselroutes");
            return old_vesselroutes;
        }

        if(pos2 == vesselroutes.get(vessel).size()-1){
            int newEarliestTime = vesselroutes.get(vessel).get(pos1).getEarliestTime() + first_delta + new_second_sailing;
            if(newEarliestTime > nTimePeriods){
                System.out.println("Infeasible one-relocate");
                return old_vesselroutes;
            }
        }else{
            int newEarliestTime = vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1).getEarliestTime() + first_delta + second_delta;
            if(newEarliestTime > nTimePeriods){
                System.out.println("Infeasible one-relocate");
                return old_vesselroutes;
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
            earliest = SailingTimes[vessel][0][startNodes[vessel]-1][vesselroutes.get(vessel).get(earliest_insert).getID()-1]+1;
        }else {
            earliest = vesselroutes.get(vessel).get(earliest_insert - 1).getEarliestTime() +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(earliest_insert - 1).getID() - 1 - startNodes.length][vesselroutes.get(vessel).get(earliest_insert - 1).getEarliestTime()]
                    + SailingTimes[vessel][vesselroutes.get(vessel).get(earliest_insert - 1).getEarliestTime() +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(earliest_insert - 1).getID() - 1 - startNodes.length][vesselroutes.get(vessel).get(earliest_insert - 1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(earliest_insert - 1).getID() - 1][vesselroutes.get(vessel).get(earliest_insert).getID() - 1];
        }
        if(latest_insert == vesselroutes.get(vessel).size()-1){
            latest = nTimePeriods;
        }else {
            latest = vesselroutes.get(vessel).get(latest_insert + 1).getLatestTime() - SailingTimes[vessel][vesselroutes.get(vessel).get(earliest_insert).getLatestTime() +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(latest_insert).getID() - 1 - startnodes.length][vesselroutes.get(vessel).get(earliest_insert).getLatestTime()]]
                    [vesselroutes.get(vessel).get(latest_insert).getID() - 1][vesselroutes.get(vessel).get(latest_insert + 1).getID()-1] -
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(latest_insert).getID() - 1 - startnodes.length][vesselroutes.get(vessel).get(earliest_insert).getLatestTime()];
        }
        vesselroutes.get(vessel).get(earliest_insert).setEarliestTime(earliest);
        vesselroutes.get(vessel).get(latest_insert).setLatestTime(latest);
        updateEarliest(earliest,earliest_insert,vessel,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes);
        updateLatest(latest,latest_insert,vessel,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes);
        updateConRoutes(simOpRoutes,precedenceOfRoutes, precedenceOverRoutes, vessel,vesselroutes);

        //ConstructionHeuristic.updatePrecedenceOver(checkPrecedence(routeIndex,0),indexInRoute);
        //ConstructionHeuristic.updatePrecedenceOf(checkPrecedence(routeIndex,1),indexInRoute);
        updateSimultaneous(simOpRoutes,vessel,pos2,vesselroutes.get(vessel).get(pos2).getID(), simultaneousOp,
                precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                precedenceOfOperations,vesselroutes, pos1,pos2, "onerelocate");
/*
        for(Map<Integer, ConnectedValues> simOpRoute : simOpRoutes){
            for(ConnectedValues simOp : simOpRoute.values()){
                System.out.println(simOp.getOperationObject().getID() + " in route " + simOp.getRoute() + " with index " + simOp.getIndex());
            }
        }
*/
        return vesselroutes;

    }

/*
    public List<List<OperationInRoute>> two_relocate(List<List<OperationInRoute>> vesselroutes, int vessel1, int vessel2, int pos1, int pos2, int[] startnodes){
        if ((vessel1 == vessel2) ||
                !(ConstructionHeuristic.containsElement (vesselroutes.get(vessel1).get(pos1).getID(), OperationsForVessel[vessel2]))){
                return vesselroutes;
            }

        int nStartnodes = startnodes.length;
        int old_vessel1_dist;
        int old_vessel2_dist;

        //Tracker gammel tid for vessel 1 - testet for metoden 2_exchange skal fungere riktig.
        if(vesselroutes.get(vessel1).size() == 1){
            old_vessel1_dist = SailingTimes[vessel1][0][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");
        } else if(pos1==0) {
            old_vessel1_dist = SailingTimes[vessel1][0][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1 + 1).getID()-1];
                            //+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] ;
                    //+ TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else{
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] ;
                    //+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()] ;
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");
        }

        if(vesselroutes.get(vessel2).size() == 0){
            old_vessel2_dist = 0;
            System.out.println( old_vessel2_dist + " Old vessel2 dist");
        } else if(pos2==0) {
            old_vessel2_dist = SailingTimes[vessel2][0][startnodes[vessel2]-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if(pos2==vesselroutes.get(vessel2).size()-1){
            old_vessel2_dist = 0;
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else{
            old_vessel2_dist = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];
            System.out.println(old_vessel2_dist + " Old vessel2 dist her");
        }


        //Track new time for vessel 1 - Completed 20.2, not tested
        int new_vessel1_dist;

        if(vesselroutes.get(vessel1).size() == 0){
            new_vessel1_dist = 0;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1==0) {
            new_vessel1_dist = SailingTimes[vessel1][0][startnodes[vessel1]-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] ;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            new_vessel1_dist = 0;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else{
            new_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1];
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        }


        // Save old vesselroutes list
        List<List<OperationInRoute>> old_vesselroutes = copyVesselroutes(vesselroutes);

        //Commit change of position
        OperationInRoute toMove = vesselroutes.get(vessel1).get(pos1);
        vesselroutes.get(vessel1).remove(pos1);
        vesselroutes.get(vessel2).add(pos2, toMove);


        //Use method for insertion to update time of vessel 2
        int new_second_sailing;
        int new_vessel2_dist;

        if(vesselroutes.get(vessel2).size() == 1){
            new_second_sailing = SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if(pos2==0) {
            new_second_sailing = SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1]+
                            TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1]]]
                            [vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1] ;
                            // +TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1]];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2==vesselroutes.get(vessel2).size()-1){
            new_second_sailing = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing;
                    //+ TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else{
            new_second_sailing = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];
                            //+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing] ;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        }


        //Mangler metode for oppdatering av tidene
        int vessel1_delta = -old_vessel1_dist + new_vessel1_dist;
        System.out.println(vessel1_delta);
        int vessel2_delta = -old_vessel2_dist + new_vessel2_dist;
        System.out.println(vessel2_delta);

        if(pos2 == vesselroutes.get(vessel2).size()-1){
            int newEarliestTime = vesselroutes.get(vessel2).get(pos1).getEarliestTime() + new_second_sailing;
            if(newEarliestTime > nTimePeriods){
                System.out.println("Infeasible two-relocate");
                return old_vesselroutes;
            }
        }else{
            int newEarliestTime = vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getEarliestTime() + vessel2_delta;
            if(newEarliestTime > nTimePeriods){
                System.out.println("Infeasible two-relocate");
                return old_vesselroutes;
            }
        }

        ConstructionHeuristic.updateEarliest(earliest,indexInRoute,routeIndex);
        ConstructionHeuristic.updateLatest(latest,indexInRoute,routeIndex);
        //ConstructionHeuristic.updatePrecedenceOver(checkPrecedence(routeIndex,0),indexInRoute);
        //ConstructionHeuristic.updatePrecedenceOf(checkPrecedence(routeIndex,1),indexInRoute);
        //ConstructionHeuristic.updateSimultaneous(checkSimultaneous(routeIndex),routeIndex,indexInRoute,o);


        return vesselroutes;
    }


    public List<List<OperationInRoute>> one_exchange(List<List<OperationInRoute>> vesselroutes, int vessel, int pos1, int pos2, int[] startnodes){
        if (pos1 == pos2){
            return vesselroutes;
        }
        int cur_pos = pos1;
        int new_pos = pos2;
        if(new_pos<cur_pos){
            pos1 = new_pos;
            pos2 = cur_pos;
        }

        int nStartnodes = startnodes.length;
        int old_pos1_dist;
        int old_pos2_dist;

        if (pos1==0) {

            old_pos1_dist = SailingTimes[vessel][0][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1 + 1).getID()-1];
                            // +TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];

        } else{
            old_pos1_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1];
                    //+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;

        }
        System.out.println(old_pos1_dist + " Old pos 1 dist");


        if(pos2==vesselroutes.get(vessel).size()-1){
            old_pos2_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1] ;
                    //+ TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()];
        } else{
            old_pos2_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos2).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1];
                    // +TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()] ;
        }
        System.out.println(old_pos2_dist + " Old pos 2 dist");

        //Save old vesselroutes list
        List<List<OperationInRoute>> old_vesselroutes = copyVesselroutes(vesselroutes);

        // Commit the change in the routes

        OperationInRoute toMove1 = vesselroutes.get(vessel).get(pos1);
        OperationInRoute toMove2 = vesselroutes.get(vessel).get(pos2);
        vesselroutes.get(vessel).remove(pos1);
        vesselroutes.get(vessel).add(pos2, toMove1);
        vesselroutes.get(vessel).remove(pos2-1);
        vesselroutes.get(vessel).add(pos1, toMove2);


        int new_first_dist;
        int new_second_dist;
        int new_first_sailing;
        int new_second_sailing;

        if(pos1==0){
            //System.out.println("Sailing from: " + vesselroutes.get(vessel).get(new_pos).getID() + " sailing to: " + vesselroutes.get(vessel).get(cur_pos+1).getID() +
            //        " it takes time periods: " + SailingTimes[vessel][vesselroutes.get(vessel).get(new_pos).getTimeperiod()][vesselroutes.get(vessel).get(new_pos).getID()][vesselroutes.get(vessel).get(cur_pos+1).getID()]
            //        + " in time peiod: " + vesselroutes.get(vessel).get(new_pos).getTimeperiod());

            new_first_sailing = SailingTimes[vessel][0][startnodes[vessel]-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_first_dist = new_first_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()+
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1];
                            //+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()] ;
            System.out.println(new_first_dist + " new first dist");

        } else{
            new_first_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_first_dist = new_first_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()+
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] ;
                    //+ TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()] ;
            System.out.println(new_first_dist + " new first dist");
        }


        //Assumption: We can use old Timeperiod for the time used on the operation and time used for sailing
        if(pos2 == vesselroutes.get(vessel).size()-1){
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_second_dist =  new_second_sailing ;
                    // + TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
            System.out.println(new_second_dist + " new second dist");
        } else {
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_second_dist = new_second_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1];
                    //+ TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;

            System.out.println(new_second_dist + " new second dist");
        }

        // Oppdatert til dette punktet 21.2 og testet


        //Updated method for setting new times (20.2) - not tested
        int first_delta = -(old_pos1_dist) + (new_first_dist);
        int second_delta = -(old_pos2_dist) + (new_second_dist);

        if(pos2 == vesselroutes.get(vessel).size()-1) {
            int newEarliestTime = vesselroutes.get(vessel).get(pos1).getEarliestTime() + first_delta + new_second_sailing;
            if (newEarliestTime > nTimePeriods) {
                System.out.println("Infeasible one-exchange");
                return old_vesselroutes;
            }
        }else{
            int newEarliestTime = vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1).getEarliestTime() + first_delta + second_delta;
            if(newEarliestTime > nTimePeriods){
                System.out.println("Infeasible one-exchange");
                return old_vesselroutes;
            }
        }

        ConstructionHeuristic.updateEarliest(earliest,indexInRoute,routeIndex);
        ConstructionHeuristic.updateLatest(latest,indexInRoute,routeIndex);
        //ConstructionHeuristic.updatePrecedenceOver(checkPrecedence(routeIndex,0),indexInRoute);
        //ConstructionHeuristic.updatePrecedenceOf(checkPrecedence(routeIndex,1),indexInRoute);
        //ConstructionHeuristic.updateSimultaneous(checkSimultaneous(routeIndex),routeIndex,indexInRoute,o);


        return vesselroutes;
    }


    public List<List<OperationInRoute>> two_exchange(List<List<OperationInRoute>> vesselroutes, int vessel1, int vessel2, int pos1, int pos2, int[] startnodes){

        if ((vessel1 == vessel2) ||
                !((ConstructionHeuristic.containsElement (vesselroutes.get(vessel1).get(pos1).getID(), OperationsForVessel[vessel2])) &&
                        ConstructionHeuristic.containsElement(vesselroutes.get(vessel2).get(pos2).getID(), OperationsForVessel[vessel1]))){
            return vesselroutes;
        }

        int nStartnodes = startnodes.length;
        int old_vessel1_dist;
        int old_vessel2_dist;


        if(vesselroutes.get(vessel1).size()==1){
            old_vessel1_dist = SailingTimes[vessel1][0][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");
        } else if(pos1==0) {
            old_vessel1_dist = SailingTimes[vessel1][0][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1 + 1).getID()-1];
                            //+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1];
                    //+ TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else{
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1];
                            //+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()] ;
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        }

        if(vesselroutes.get(vessel2).size() == 1){
            old_vessel2_dist = SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if(pos2==0) {
            old_vessel2_dist = SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
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

        //Make copy of old vesselroutes list
        List<List<OperationInRoute>> old_vesselroutes = copyVesselroutes(vesselroutes);

        //Commit exchange - this works as supposed to
        OperationInRoute toMove1 = vesselroutes.get(vessel1).get(pos1);
        OperationInRoute toMove2 = vesselroutes.get(vessel2).get(pos2);
        vesselroutes.get(vessel1).remove(pos1);
        vesselroutes.get(vessel2).remove(pos2);
        vesselroutes.get(vessel2).add(pos2, toMove1);
        vesselroutes.get(vessel1).add(pos1, toMove2);

        // Track new time usage

        int new_vessel1_dist;
        int new_vessel2_dist;
        int new_first_sailing;
        int new_second_sailing;


        if(vesselroutes.get(vessel1).size() == 1){
            new_first_sailing = SailingTimes[vessel1][0][startnodes[vessel1]-1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist = new_first_sailing;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1==0) {
            new_first_sailing = SailingTimes[vessel1][0][startnodes[vessel1]-1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist = new_first_sailing  +
                    SailingTimes[vessel1][vesselroutes.get(vessel2).get(pos2).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1- nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1];
                    // + TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1 - nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            new_first_sailing = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist =  new_first_sailing;
                    // +TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(new_vessel1_dist + " New vessel1 dist");

        } else{
            new_first_sailing = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist = new_first_sailing +
                    SailingTimes[vessel1][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1];
                    // +TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()] ;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        }

        if(vesselroutes.get(vessel2).size() == 1) {
            new_second_sailing = SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID() - 1];
            new_vessel2_dist = new_second_sailing;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if(pos2==0) {
            new_second_sailing = SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2 + 1).getID()-1];
                    //      + TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2==vesselroutes.get(vessel2).size()-1){
            new_second_sailing = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing;
                    // +TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else{
            new_second_sailing = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];
                    // + TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()] ;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        }


        int vessel1_delta = -old_vessel1_dist + new_vessel1_dist;
        int vessel2_delta = -old_vessel2_dist + new_vessel2_dist;


        if(pos2 == vesselroutes.get(vessel2).size()-1) {
            int newEarliestTime = vesselroutes.get(vessel1).get(pos1).getEarliestTime() + new_second_sailing;
            if (newEarliestTime > nTimePeriods) {
                System.out.println("Infeasible two-exchange");
                return old_vesselroutes;
            }
        } else if (pos1 == vesselroutes.get(vessel1).size()-1){
            int newEarliestTime = vesselroutes.get(vessel2).get(pos2).getEarliestTime() + new_first_sailing;
            if (newEarliestTime > nTimePeriods) {
                System.out.println("Infeasible two-exchange");
                return old_vesselroutes;
            }
        }
        else{
            int newEarliestTimeVessel1 = vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size()-1).getEarliestTime() + vessel1_delta;
            int newEarliestTimeVessel2 = vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getEarliestTime() + vessel2_delta;
            if(newEarliestTimeVessel1 > nTimePeriods || newEarliestTimeVessel2 > nTimePeriods){
                System.out.println("Infeasible two-exchange");
                return old_vesselroutes;
            }
        }

        ConstructionHeuristic.updateEarliest(earliest,indexInRoute,routeIndex);
        ConstructionHeuristic.updateLatest(latest,indexInRoute,routeIndex);
        //ConstructionHeuristic.updatePrecedenceOver(checkPrecedence(routeIndex,0),indexInRoute);
        //ConstructionHeuristic.updatePrecedenceOf(checkPrecedence(routeIndex,1),indexInRoute);
        //ConstructionHeuristic.updateSimultaneous(checkSimultaneous(routeIndex),routeIndex,indexInRoute,o);


        return vesselroutes;

    }




    public List<List<OperationInRoute>> insert(List<List<OperationInRoute>> vesselroutes, OperationInRoute unrouted_operation, int vessel, int pos1,
                       int[] routeSailingCost, int[] startnodes ) {
        int nStartnodes = startnodes.length;

        // Calculate old time
        int SailingCost = routeSailingCost[vessel];

        //Make copy of old vesselroutes list
        List<List<OperationInRoute>> old_vesselroutes = copyVesselroutes(vesselroutes);

        //Perform change
        vesselroutes.get(vessel).add(pos1,unrouted_operation);

        //Calculate new time
        int new_second_sailing;
        int new_dist;


        if(vesselroutes.get(vessel).size() == 1){
            new_second_sailing = new_second_sailing = SailingTimes[vessel][0][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_dist = new_second_sailing;
            System.out.println(new_dist + " New dist");
        } else if(pos1==0) {
            new_second_sailing = SailingTimes[vessel][0][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_dist = new_second_sailing +
                    SailingTimes[vessel][new_second_sailing +
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][new_second_sailing]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] ;
            // +TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(new_dist + " New dist");
        } else if (pos1==vesselroutes.get(vessel).size()-1){
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_dist =  new_second_sailing ;
            // + TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()];
            System.out.println(new_dist + " New dist");

        } else{
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_dist = new_second_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1+1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1+1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] ;
            //+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
            System.out.println(new_dist + " New dist");
        }

        //Feasibility
        if(pos1 == vesselroutes.get(vessel).size()-1) {
            int newEarliestTime = vesselroutes.get(vessel).get(pos1-1).getEarliestTime() + new_dist;
            if (newEarliestTime > nTimePeriods) {
                System.out.println("Infeasible insertion");
                return old_vesselroutes;
            }
        }else{
            int newEarliestTime = vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1).getEarliestTime() + new_dist;
            if(newEarliestTime > nTimePeriods){
                System.out.println("Infeasible two-exchange");
                return old_vesselroutes;
            }
        }

        //Update times & Cost
        ConstructionHeuristic.updateEarliest(earliest,indexInRoute,routeIndex);
        ConstructionHeuristic.updateLatest(latest,indexInRoute,routeIndex);
        //ConstructionHeuristic.updatePrecedenceOver(checkPrecedence(routeIndex,0),indexInRoute);
        //ConstructionHeuristic.updatePrecedenceOf(checkPrecedence(routeIndex,1),indexInRoute);
        //ConstructionHeuristic.updateSimultaneous(checkSimultaneous(routeIndex),routeIndex,indexInRoute,o);

        return vesselroutes;
    }

 */

    public static void updateEarliest(int earliest, int indexInRoute, int routeIndex,
                                      int [][][] TimeVesselUseOnOperation, int[] startNodes, int[][][][] SailingTimes,
                                      List<List<OperationInRoute>> vesselroutes){
        int lastEarliest=earliest;
        //System.out.println("WITHIN UPDATE EARLIEST");
        //System.out.println("Last earliest time: " + lastEarliest);
        for(int f=indexInRoute+1;f<vesselroutes.get(routeIndex).size();f++){
            //System.out.println("Index updating: "+f);
            OperationInRoute objectFMinus1=vesselroutes.get(routeIndex).get(f-1);
            OperationInRoute objectF=vesselroutes.get(routeIndex).get(f);
            int opTimeFMinus1=TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1]
                    [objectFMinus1.getLatestTime()-1];
            //System.out.println("ID operation "+ vesselroutes.get(routeIndex).get(f).getID() + " , " +"Route: "+ routeIndex);
            //System.out.println("Latest already assigned F: "+ objectF.getLatestTime() + " , " + "Potential update earliest F: "+
            //        (lastEarliest+
            //                SailingTimes[routeIndex][objectFMinus1.getEarliestTime()+opTimeFMinus1-1][objectFMinus1.getID()-1][objectF.getID()-1]
            //                +TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1][objectFMinus1.getEarliestTime()-1]));
            int newTime=lastEarliest+
                    SailingTimes[routeIndex][objectFMinus1.getEarliestTime()+opTimeFMinus1-1][objectFMinus1.getID()-1][objectF.getID()-1]
                    +TimeVesselUseOnOperation[routeIndex][objectFMinus1.getID()-startNodes.length-1][objectFMinus1.getEarliestTime()-1];

            //System.out.println("New time: "+ newTime + " , " + "ID F: " +objectF.getID());
            if(newTime==objectF.getEarliestTime()){
                break;
            }
            vesselroutes.get(routeIndex).get(f).setEarliestTime(newTime);
            //System.out.println(objectF.getEarliestTime());
            lastEarliest=newTime;

        }
    }

    public static void updateLatest(int latest, int indexInRoute, int routeIndex,
                                    int [][][] TimeVesselUseOnOperation, int[] startNodes, int[][][][] SailingTimes,
                                    List<List<OperationInRoute>> vesselroutes){
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
            int newTime=lastLatest-
                    SailingTimes[routeIndex][updateSailingTime-1][objectK.getID()-1]
                            [vesselroutes.get(routeIndex).get(k+1).getID()-1] -opTimeK;
            //System.out.println("New time: "+ newTime + " , " + "ID K: " +objectK.getID());

            if(newTime==objectK.getLatestTime()){
                break;
            }
            objectK.setLatestTime(newTime);
            //System.out.println(objectK.getLatestTime());
            lastLatest=newTime;
        }
    }

    public static void updateSimultaneous(List<Map<Integer, ConnectedValues>> simOpRoutes, int routeIndex, int indexInRoute, int o,
                                          Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, PrecedenceValues>> precedenceOverRoutes,
                                          List<Map<Integer, PrecedenceValues>> precedenceOfRoutes, int[][][] TimeVesselUseOnOperation,
                                          int [] startNodes, int [][][][] SailingTimes, Map<Integer, PrecedenceValues> precedenceOverOperations,
                                          Map<Integer, PrecedenceValues> precedenceOfOperations, List<List<OperationInRoute>> vesselroutes,
                                          int pos1, int pos2, String function) {

        if(simOpRoutes.get(routeIndex)!=null){
            for (ConnectedValues sValues : simOpRoutes.get(routeIndex).values()) {
                //System.out.println("Update caused by simultaneous " + sValues.getOperationObject().getID() + " in route " + routeIndex);
                int cur_earliestTemp = sValues.getOperationObject().getEarliestTime();
                int cur_latestTemp = sValues.getOperationObject().getLatestTime();
                //System.out.println(routeIndex + " Route index");
                //System.out.println(cur_latestTemp + " cur latest temp of " + sValues.getOperationObject().getID());

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
                        int prevOpTime = TimeVesselUseOnOperation[simOp.getRoute()][simOp.getOperationObject().getID() - startNodes.length - 1]
                                [simOp.getOperationObject().getEarliestTime()];
                        int SailingPrevOptoOp = SailingTimes[simOp.getRoute()][vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getEarliestTime()]
                                [vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getID() - 1][simOp.getOperationObject().getID() - 1];
                        earliestPO = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getEarliestTime() + prevOpTime + SailingPrevOptoOp;

                    }
                    int earliestTemp = Math.max(cur_earliestTemp, earliestPO);
                    if(simOp.getIndex()==vesselroutes.get(simOp.getRoute()).size()-1){
                        latestPO = SailingTimes[0].length;
                    }else {
                        int nextOpTime = TimeVesselUseOnOperation[simOp.getRoute()][simOp.getOperationObject().getID() - startNodes.length - 1]
                                [simOp.getOperationObject().getLatestTime()];
                        int SailingOptoNextOp = SailingTimes[simOp.getRoute()][simOp.getOperationObject().getLatestTime()+nextOpTime]
                                [simOp.getOperationObject().getID() - 1][vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() + 1).getID()];
                        latestPO = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() + 1).getLatestTime() - nextOpTime - SailingOptoNextOp;
                    }
                    int latestTemp = Math.min(cur_latestTemp, latestPO);


                    if (earliestTemp > cur_earliestTemp) {
                        cur_earliestTemp = earliestTemp;
                        sValues.getOperationObject().setEarliestTime(cur_earliestTemp);
                        ConstructionHeuristic.updateEarliest(cur_earliestTemp,indexInRoute,routeIndex,TimeVesselUseOnOperation,startNodes,SailingTimes,vesselroutes);
                    }else if(earliestTemp>earliestPO){
                        simOp.getOperationObject().setEarliestTime(cur_earliestTemp);
                        System.out.println(simOp.getOperationObject().getEarliestTime() + " setting the earliest time of " + simOp.getOperationObject().getID() );
                        ConstructionHeuristic.updateEarliest(cur_earliestTemp,simOp.getIndex(),simOp.getRoute(), TimeVesselUseOnOperation, startNodes,SailingTimes, vesselroutes);

                        ConstructionHeuristic.updateSimultaneous(simOpRoutes,simOp.getRoute(),simOp.getIndex(),simOp.getOperationObject().getID(),simultaneousOp,precedenceOverRoutes,
                                precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,precedenceOfOperations,vesselroutes,false);


                        /*
                        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(simOp.getRoute()),simOp.getIndex(),simOpRoutes,precedenceOfOperations,
                                precedenceOverOperations,TimeVesselUseOnOperation,startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselroutes,SailingTimes);
                        */

                    }
                    if (latestTemp < cur_latestTemp) {
                        cur_latestTemp = latestTemp;
                        sValues.getOperationObject().setLatestTime(cur_latestTemp);
                        System.out.println(sValues.getOperationObject().getLatestTime() + " setting the latest time of " + sValues.getOperationObject().getID() );
                        ConstructionHeuristic.updateLatest(cur_latestTemp,indexInRoute,routeIndex,TimeVesselUseOnOperation, startNodes,SailingTimes, vesselroutes);
                    }else if(latestTemp<latestPO){
                        System.out.println(latestTemp + " , " +latestPO + " , " + cur_latestTemp);
                        simOp.getOperationObject().setLatestTime(cur_latestTemp);
                        System.out.println(simOp.getOperationObject().getLatestTime() + " setting the latest time of " + simOp.getOperationObject().getID() );

                        ConstructionHeuristic.updateLatest(cur_latestTemp,simOp.getIndex(),simOp.getRoute(),TimeVesselUseOnOperation, startNodes,SailingTimes, vesselroutes);
                        ConstructionHeuristic.updateSimultaneous(simOpRoutes,simOp.getRoute(),simOp.getIndex(),simOp.getOperationObject().getID(),
                                simultaneousOp,precedenceOverRoutes, precedenceOfRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,precedenceOverOperations,
                                precedenceOfOperations,vesselroutes,false);

                        /*ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(simOp.getRoute()),simOp.getIndex(),TimeVesselUseOnOperation,startNodes,simOpRoutes,
                                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselroutes,simultaneousOp,SailingTimes);

                         */
                    }
                }
            }
        }
    }

    public void updateConRoutes(List<Map<Integer,ConnectedValues>> simOpRoutes, List<Map<Integer,PrecedenceValues>> precedenceOfRoutes,
                                List<Map<Integer,PrecedenceValues>> precedenceOverRoutes, int v,
                                   List<List<OperationInRoute>> vesselroutes){
        List<Integer> updatedRoutes = new ArrayList<>();
        if(!simOpRoutes.get(v).isEmpty()){
            for(ConnectedValues op : simOpRoutes.get(v).values()){
                int conRoute = op.getConnectedRoute();
                if(!updatedRoutes.contains(conRoute)) {
                    updatedRoutes.add(conRoute);
                }
            }
        }
        if(!precedenceOfRoutes.get(v).isEmpty()){
            for(PrecedenceValues op : precedenceOfRoutes.get(v).values()){
                int conRoute = op.getConnectedRoute();
                if(!updatedRoutes.contains(conRoute)) {
                    updatedRoutes.add(conRoute);
                }
            }
        }
        if(!precedenceOverRoutes.get(v).isEmpty()){
            for(PrecedenceValues op : precedenceOverRoutes.get(v).values()){
                int conRoute = op.getConnectedRoute();
                if(!updatedRoutes.contains(conRoute)) {
                    updatedRoutes.add(conRoute);
                }
            }
        }
        for(int route : updatedRoutes) {
            System.out.println("Updating route: " +route);
            int earliest = SailingTimes[route][0][startNodes[route] - 1][vesselroutes.get(route).get(0).getID() - 1] + 1;
            int latest = SailingTimes[0].length;
            vesselroutes.get(route).get(0).setEarliestTime(earliest);
            vesselroutes.get(route).get(vesselroutes.get(route).size() - 1).setLatestTime(SailingTimes[0].length);
            updateEarliest(earliest, 0, route, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes);
            updateLatest(latest, vesselroutes.get(route).size() - 1, route, TimeVesselUseOnOperation,
                        startNodes, SailingTimes, vesselroutes);
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

    public List<List<OperationInRoute>> searchAll (List<List<OperationInRoute>> vesselroutes){
        for(int vessel=1; vessel<vesselroutes.size()-1;vessel++){
            for(int position=0;position<vesselroutes.get(vessel).size()-1;position++){
                for(int task=0;task<vesselroutes.get(vessel).size()-1;task++) {
                    System.out.println(vessel + " , " + task + " , " + position);
                    vesselroutes = one_relocate(vesselroutes,vessel,task,position,startNodes);
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
        //System.out.println("Operation gain per route: "+Arrays.toString(routeOperationGain));
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
    }



    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes = new int[]{1,2,3,4};
        int[] startnodes=new int[]{1,2,3,4};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes ,
                "test_instances/test_instance_15_locations_simpleTestSIM.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),startnodes.length);
        //PrintData.printSailingTimes(dg.getSailingTimes(),2,12, 4);
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
                                            dg.getTwIntervals(),a.getRouteSailingCost(),a.getObjValue(),dg.getStartNodes(), dg.getSimultaneousALNS(),
                                            a.getPrecedenceOverOperations(), a.getPrecedenceOfOperations(),a.getSimultaneousOp(),
                                            a.getSimOpRoutes(),a.getPrecedenceOfRoutes(), a.getPrecedenceOverRoutes());
        List<List<OperationInRoute>> new_vesselroutes = LSO.one_relocate(a.vesselroutes,1,3,2,startnodes);
        //List<List<OperationInRoute>> new_vesselroutes = LSO.searchAll(a.vesselroutes);
        LSO.printLSOSolution(vesseltypes,new_vesselroutes,a.getUnroutedTasks());
    }

}
