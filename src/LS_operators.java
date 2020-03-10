import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class LS_operators {
    private int [][] OperationsForVessel;
    private int[] vesseltypes;
    private int[][][][] SailingTimes;
    private  int [][][] TimeVesselUseOnOperation;
    private int [][] twIntervals;


    public LS_operators(int [][] OperationsForVessel, int[] vesseltypes, int[][][][] SailingTimes, int [][][] TimeVesselUseOnOperation, int [][] twIntervals){
        this.OperationsForVessel = OperationsForVessel;
        this.vesseltypes = vesseltypes;
        this.SailingTimes = SailingTimes;
        this.TimeVesselUseOnOperation = TimeVesselUseOnOperation;
        this.twIntervals = twIntervals;
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



        int nStartnodes = startnodes.length;
        int old_pos1_dist;
        int old_pos2_dist;

        //Tracker gammel tid for både pos 1 og pos 2 (21.02)
        if(pos1==0) {

            old_pos1_dist = SailingTimes[vessel][0][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(old_pos1_dist + " Old pos 1 dist");

        } else if(pos1==vesselroutes.get(vessel).size()-1){
            old_pos1_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(old_pos1_dist + " Old pos 1 dist");


        } else{
            old_pos1_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
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
                            [vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(new_pos2_dist + " New pos 2 dist");
        } else if (pos2==vesselroutes.get(vessel).size()-1){
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist =  new_second_sailing +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()];
            System.out.println(new_pos2_dist + " New pos 2 dist");

        } else{
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist = new_second_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
            System.out.println(new_pos2_dist + " New pos 2 dist");
        }

        //Updated method for setting new times (21.2) - tested
        int first_delta;
        int second_delta;
        if (pos1<pos2){
            first_delta = -(old_pos1_dist) + (new_pos1_dist);
            second_delta = -(old_pos2_dist) + (new_pos2_dist);
        }else{
            first_delta = -(old_pos2_dist) + (new_pos2_dist);
            second_delta = -(old_pos1_dist) + (new_pos1_dist);
        }
        System.out.println(first_delta);
        System.out.println(second_delta);


        for(int i = 0; i < vesselroutes.get(vessel).size(); i++ ) {
            if (i<pos1 && i<pos2){
                int new_latesttime = vesselroutes.get(vessel).get(i).getLatestTime()-(first_delta+second_delta);
                if(new_latesttime > vesselroutes.get(vessel).get(i).getEarliestTime() && new_latesttime <= twIntervals[vesselroutes.get(vessel).get(i).getID()-nStartnodes-1][1]){
                    vesselroutes.get(vessel).get(i).setLatestTime(new_latesttime );
                }

            }
            else if(i == pos1 || i == pos2){
            }
            else if ((i > pos1 && i < pos2) || (i > pos2 && i < pos1)){
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(i).getEarliestTime()+first_delta);
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(i).getLatestTime()-second_delta);
            }
            else{
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(i).getEarliestTime()+first_delta+second_delta);
            }
        }
        vesselroutes.get(vessel).get(pos1).setEarliestTime(vesselroutes.get(vessel).get(pos1).getEarliestTime()+first_delta);
        vesselroutes.get(vessel).get(pos1).setLatestTime(vesselroutes.get(vessel).get(pos1).getLatestTime()-second_delta);

        if(pos2==0){
            vesselroutes.get(vessel).get(pos2).setEarliestTime(new_second_sailing);
            int last_sail = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel).get(pos2).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()];
            vesselroutes.get(vessel).get(pos2).setLatestTime(vesselroutes.get(vessel).get(pos2+1).getLatestTime()-last_sail-this_op);

        }
        else if(pos2==vesselroutes.get(vessel).size()-1){
            int prev_op = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()];
            vesselroutes.get(vessel).get(pos2).setEarliestTime(vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+new_second_sailing+prev_op);
            vesselroutes.get(vessel).get(pos2).setLatestTime(TimeVesselUseOnOperation[vessel].length);
        }
        else{
            int prev_op = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()];
            vesselroutes.get(vessel).get(pos2).setEarliestTime(vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+new_second_sailing+prev_op);
            int last_sail = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel).get(pos2).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()];
            vesselroutes.get(vessel).get(pos2).setLatestTime(vesselroutes.get(vessel).get(pos2+1).getLatestTime()-last_sail-this_op);
        }

        if(vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1).getEarliestTime()>60 ||
                vesselroutes.get(vessel).get(0).getLatestTime()<0){
            return old_vesselroutes;
        }

        return vesselroutes;
    }


    public List<List<OperationInRoute>> two_relocate(List<List<OperationInRoute>> vesselroutes, int vessel1, int vessel2, int pos1, int pos2, int[] startnodes){
        if ((vessel1 == vessel2) ||
                !(ConstructionHeuristic.containsElement (vesselroutes.get(vessel1).get(pos1).getID(), OperationsForVessel[vessel2]))){
                return vesselroutes;
            }

        int nStartnodes = startnodes.length;
        int old_vessel1_dist;
        int old_vessel2_dist;

        //Tracker gammel tid for vessel 1 - testet for metoden 2_exchange skal fungere riktig.
        if(pos1==0) {
            old_vessel1_dist = SailingTimes[vessel1][0][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else{
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()] ;
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");
        }


        if(pos2==0) {
            old_vessel2_dist = SailingTimes[vessel2][0][startnodes[vessel2]-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if(pos2==vesselroutes.get(vessel2).size()-1){
            old_vessel2_dist = 0;
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else{
            old_vessel2_dist = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];
            System.out.println(vesselroutes.get(vessel2).get(pos2).getID()-1);
            System.out.println(vesselroutes.get(vessel2).get(pos2+1).getID()-1);
            System.out.println(old_vessel2_dist + " Old vessel2 dist her");
        }


        //Track new time for vessel 1 - Completed 20.2, not tested
        int new_vessel1_dist;

        if(pos1==0) {
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


        if(pos2==0) {
            new_second_sailing = SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1]+
                            TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1]]]
                            [vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1]];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2==vesselroutes.get(vessel2).size()-1){
            new_second_sailing = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else{
            new_second_sailing = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing] ;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        }


        //Mangler metode for oppdatering av tidene
        int vessel1_delta = -old_vessel1_dist + new_vessel1_dist;
        System.out.println(vessel1_delta);
        int vessel2_delta = -old_vessel2_dist + new_vessel2_dist;
        System.out.println(vessel2_delta);

        for (int i = 0; i < vesselroutes.get(vessel1).size(); i++) {
            if (i < pos1) {
                vesselroutes.get(vessel1).get(i).setLatestTime(vesselroutes.get(vessel1).get(i).getLatestTime() - vessel1_delta);
            }else if (i == pos1){
                vesselroutes.get(vessel1).get(i).setEarliestTime(vesselroutes.get(vessel1).get(i).getEarliestTime() + vessel1_delta);
                vesselroutes.get(vessel1).get(i).setLatestTime(vesselroutes.get(vessel1).get(i).getLatestTime() - vessel1_delta);
            }
            else {
                vesselroutes.get(vessel1).get(i).setEarliestTime(vesselroutes.get(vessel1).get(i).getEarliestTime() + vessel1_delta);
            }
        }
        for (int i = 0; i < vesselroutes.get(vessel2).size(); i++) {
            if (i < pos2) {
                vesselroutes.get(vessel2).get(i).setLatestTime(vesselroutes.get(vessel2).get(i).getLatestTime() - vessel2_delta);
            } else if (i>pos2){
                vesselroutes.get(vessel2).get(i).setEarliestTime(vesselroutes.get(vessel2).get(i).getEarliestTime() + vessel2_delta);
            }
        }

        if(pos2==0){
            vesselroutes.get(vessel2).get(pos2).setEarliestTime(new_second_sailing);
            int second_sail = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            vesselroutes.get(vessel2).get(pos2).setLatestTime(vesselroutes.get(vessel2).get(pos2+1).getLatestTime()-second_sail-this_op);
        }
        else if(pos2==vesselroutes.get(vessel2).size()-1){
            int prev_op = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()];
            vesselroutes.get(vessel2).get(pos2).setEarliestTime(vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing+prev_op);
            vesselroutes.get(vessel2).get(pos2).setLatestTime(TimeVesselUseOnOperation[vessel2].length);
        }
        else{
            int prev_op = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()];
            vesselroutes.get(vessel2).get(pos2).setEarliestTime(vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing+prev_op);
            int last_sail = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            vesselroutes.get(vessel2).get(pos2).setLatestTime(vesselroutes.get(vessel2).get(pos2+1).getLatestTime()-last_sail-this_op);
        }


        if(vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size()-1).getEarliestTime()>60 || vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getEarliestTime()>60 ||
                vesselroutes.get(vessel1).get(0).getLatestTime()<0 || vesselroutes.get(vessel2).get(0).getLatestTime()<0){
            return old_vesselroutes;
        }

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
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];

        } else{
            old_pos1_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;

        }
        System.out.println(old_pos1_dist + " Old pos 1 dist");


        if(pos2==vesselroutes.get(vessel).size()-1){
            old_pos2_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()];
        } else{
            old_pos2_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos2).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()] ;
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
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()] ;
            System.out.println(new_first_dist + " new first dist");

        } else{
            new_first_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_first_dist = new_first_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()+
                            TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()] ;
            System.out.println(new_first_dist + " new first dist");
        }


        //Assumption: We can use old Timeperiod for the time used on the operation and time used for sailing
        if(pos2 == vesselroutes.get(vessel).size()-1){
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_second_dist =  new_second_sailing +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
            System.out.println(new_second_dist + " new second dist");
        } else {
            new_second_sailing = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_second_dist = new_second_sailing +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;

            System.out.println(new_second_dist + " new second dist");
        }

        // Oppdatert til dette punktet 21.2 og testet


        //Updated method for setting new times (20.2) - not tested
        int first_delta = -(old_pos1_dist) + (new_first_dist);
        int second_delta = -(old_pos2_dist) + (new_second_dist);

        for(int i = 0; i < vesselroutes.get(vessel).size(); i++ ) {
            if (i<pos1 && i<pos2){
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(i).getLatestTime()-(first_delta+second_delta));
            }
            else if(i == pos1 || i == pos2){
            }
            else if ((i > pos1 && i < pos2) || (i > pos2 && i < pos1)){
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(i).getEarliestTime()+first_delta);
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(i).getLatestTime()-second_delta);
            }
            else{
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(i).getEarliestTime()+first_delta+second_delta);
            }
        }
        if(pos1==0){
            vesselroutes.get(vessel).get(pos1).setEarliestTime(new_first_sailing);
            int first_sail = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            vesselroutes.get(vessel).get(pos1).setLatestTime(vesselroutes.get(vessel).get(pos1+1).getLatestTime()-first_sail-this_op);

        }
        else{
            int prev_op = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()];
            vesselroutes.get(vessel).get(pos1).setEarliestTime(vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+new_first_sailing+prev_op);
            int first_sail = SailingTimes[vessel][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            vesselroutes.get(vessel).get(pos1).setLatestTime(vesselroutes.get(vessel).get(pos1+1).getLatestTime()-first_sail-this_op);
        }


        if(pos2==vesselroutes.get(vessel).size()-1){
            int prev_op = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()];
            vesselroutes.get(vessel).get(pos2).setEarliestTime(vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+new_second_sailing+prev_op);
            vesselroutes.get(vessel).get(pos2).setLatestTime(TimeVesselUseOnOperation[vessel].length);
        }
        else{
            int prev_op = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()];
            vesselroutes.get(vessel).get(pos2).setEarliestTime(vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+new_second_sailing+prev_op);
            int last_sail = SailingTimes[vessel][vesselroutes.get(vessel).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel).get(pos2).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()];
            vesselroutes.get(vessel).get(pos2).setLatestTime(vesselroutes.get(vessel).get(pos2+1).getLatestTime()-last_sail-this_op);
        }

        if(vesselroutes.get(vessel).get(vesselroutes.get(vessel).size()-1).getEarliestTime()>60 ||
                vesselroutes.get(vessel).get(0).getLatestTime()<0 ){
            return old_vesselroutes;
        }


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


        if(pos1==0) {

            old_vessel1_dist = SailingTimes[vessel1][0][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else{
            old_vessel1_dist = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()] ;
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        }



        if(pos2==0) {
            old_vessel2_dist = SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1 - nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if(pos2==vesselroutes.get(vessel2).size()-1){
            old_vessel2_dist = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else{
            old_vessel2_dist = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()] ;
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

        if(pos1==0) {
            new_first_sailing = SailingTimes[vessel1][0][startnodes[vessel1]-1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist = new_first_sailing  +
                    SailingTimes[vessel1][vesselroutes.get(vessel2).get(pos2).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1- nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1 - nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            new_first_sailing = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist =  new_first_sailing +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(new_vessel1_dist + " New vessel1 dist");

        } else{
            new_first_sailing = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1];
            new_vessel1_dist = new_first_sailing +
                    SailingTimes[vessel1][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()] ;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        }

        if(pos2==0) {
            new_second_sailing = SailingTimes[vessel2][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2==vesselroutes.get(vessel2).size()-1){
            new_second_sailing = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else{
            new_second_sailing = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1];
            new_vessel2_dist = new_second_sailing +
                    SailingTimes[vessel2][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()] ;
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        }


        int vessel1_delta = -old_vessel1_dist + new_vessel1_dist;
        int vessel2_delta = -old_vessel2_dist + new_vessel2_dist;


        for (int i = 0; i < vesselroutes.get(vessel1).size(); i++) {
            if (i < pos1) {
                vesselroutes.get(vessel1).get(i).setLatestTime(vesselroutes.get(vessel1).get(i).getLatestTime() - vessel1_delta);
            } else if (i > pos1) {
                vesselroutes.get(vessel1).get(i).setEarliestTime(vesselroutes.get(vessel1).get(i).getEarliestTime() + vessel1_delta);
            }
        }
        for (int i = 0; i < vesselroutes.get(vessel2).size(); i++) {
            if (i < pos2) {
                vesselroutes.get(vessel2).get(i).setLatestTime(vesselroutes.get(vessel2).get(i).getLatestTime() - vessel2_delta);
            } else if (i>pos2){
                vesselroutes.get(vessel2).get(i).setEarliestTime(vesselroutes.get(vessel2).get(i).getEarliestTime() + vessel2_delta);
            }
        }
        if(pos1==0){
            vesselroutes.get(vessel1).get(pos1).setEarliestTime(new_first_sailing);
            int first_sail = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            vesselroutes.get(vessel1).get(pos1).setLatestTime(vesselroutes.get(vessel1).get(pos1+1).getLatestTime()-first_sail-this_op);
        }
        else if(pos2==vesselroutes.get(vessel2).size()-1){
            int prev_op = TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()];
            vesselroutes.get(vessel1).get(pos1).setEarliestTime(vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+new_first_sailing+prev_op);
            vesselroutes.get(vessel1).get(pos1).setLatestTime(TimeVesselUseOnOperation[vessel1].length);
        }
        else{
            int prev_op = TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()];
            vesselroutes.get(vessel1).get(pos1).setEarliestTime(vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+new_first_sailing+prev_op);
            int first_sail = SailingTimes[vessel1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            vesselroutes.get(vessel1).get(pos1).setLatestTime(vesselroutes.get(vessel1).get(pos1+1).getLatestTime()-first_sail-this_op);
        }

        if(pos2==0){
            vesselroutes.get(vessel2).get(pos2).setEarliestTime(new_second_sailing);
            int second_sail = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            vesselroutes.get(vessel2).get(pos2).setLatestTime(vesselroutes.get(vessel2).get(pos2+1).getLatestTime()-second_sail-this_op);
        }
        else if(pos2==vesselroutes.get(vessel2).size()-1){
            int prev_op = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()];
            vesselroutes.get(vessel2).get(pos2).setEarliestTime(vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing+prev_op);
            vesselroutes.get(vessel2).get(pos2).setLatestTime(TimeVesselUseOnOperation[vessel2].length);
        }
        else{
            int prev_op = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()];
            vesselroutes.get(vessel2).get(pos2).setEarliestTime(vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+new_second_sailing+prev_op);
            int last_sail = SailingTimes[vessel2][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                    [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1];
            int this_op = TimeVesselUseOnOperation[vessel2][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            vesselroutes.get(vessel2).get(pos2).setLatestTime(vesselroutes.get(vessel2).get(pos2+1).getLatestTime()-last_sail-this_op);
        }

        if(vesselroutes.get(vessel1).get(vesselroutes.get(vessel1).size()-1).getEarliestTime()>60 || vesselroutes.get(vessel2).get(vesselroutes.get(vessel2).size()-1).getEarliestTime()>60 ||
                vesselroutes.get(vessel1).get(0).getLatestTime()<0 || vesselroutes.get(vessel2).get(0).getLatestTime()<0){
            return old_vesselroutes;
        }
        return vesselroutes;

    }




    public void insert(List<List<OperationInRoute>> vesselroutes, OperationInRoute unrouted_operation, int vessel, int pos1) {
        // Calculate old time

        //Perform change

        //Calculate new time

        //Update times

        //Feasibility
        return ;
    }

    public void printLSOSolution(int[] vesseltypes, List<List<OperationInRoute>> vesselroutes){
        for (int i=0;i<vesselroutes.size();i++){
            System.out.println("VESSELINDEX "+i+" VESSELTYPE "+vesseltypes[i]);
            if (vesselroutes.get(i)!=null) {
                for (OperationInRoute opInRoute : vesselroutes.get(i)) {
                    System.out.println("Operation number: "+opInRoute.getID() +" Earliest start time: "+
                    (opInRoute).getEarliestTime()+ " Latest Start time: "+ (opInRoute).getLatestTime());
                }
            }
        }
        /*
        if(!unroutedTasks.isEmpty()){
            System.out.println("UNROUTED TASKS");
            for(int n=0;n<unroutedTasks.size();n++) {
                System.out.println(unroutedTasks.get(n).getID());
            }
        }*/
    }



    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes = new int[]{1,2,4,5};
        int[] startnodes=new int[]{1,2,3,4};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes ,
                "test_instances/test_instance_15_locations_first_test.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),startnodes.length);
        //PrintData.printSailingTimes(dg.getSailingTimes(),2,23, 4);
        PrintData.printSailingTimes(dg.getSailingTimes(),3,23, 4);
        ConstructionHeuristic a = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getPenalty(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(),dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations());
        a.createSortedOperations();
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);
        LS_operators LSO = new LS_operators(dg.getOperationsForVessel(), vesseltypes, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),dg.getTwIntervals());
        List<List<OperationInRoute>> new_vesselroutes = LSO.two_relocate(a.getVesselroutes(),1,2,3,1,  startnodes);
        LSO.printLSOSolution(vesseltypes, new_vesselroutes);
    }
}