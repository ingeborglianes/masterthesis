import java.io.FileNotFoundException;
import java.util.List;

public class LS_operators {
    private int [][] OperationsForVessel;
    private int[] vesseltypes;
    private int[][][][] SailingTimes;
    private  int [][][] TimeVesselUseOnOperation;


    public LS_operators(int [][] OperationsForVessel, int[] vesseltypes, int[][][][] SailingTimes, int [][][] TimeVesselUseOnOperation){
        this.OperationsForVessel = OperationsForVessel;
        this.vesseltypes = vesseltypes;
        this.SailingTimes=SailingTimes;
        this.TimeVesselUseOnOperation=TimeVesselUseOnOperation;
    }

    public List<List<OperationInRoute>> one_relocate(List<List<OperationInRoute>> vesselroutes, int vessel, int pos1, int pos2, int[] startnodes){
        if (pos1 == pos2){
            return vesselroutes;
        }

        int nStartnodes = startnodes.length;
        int old_pos1_dist;
        int old_pos2_dist;

        //Tracker gammel tid for både pos 1 og pos 2 (20.02) - ikke testet
        if(pos1==0) {

            old_pos1_dist = SailingTimes[vessel+1][0][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(old_pos1_dist + " Old pos 1 dist");

        } else if(pos1==vesselroutes.get(vessel).size()-1){
            old_pos1_dist = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(old_pos1_dist + " Old pos 1 dist");

        } else{
            old_pos1_dist = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
            System.out.println(old_pos1_dist + " Old pos 1 dist");

        }

        if(pos2 == vesselroutes.get(vessel).size()-1){
            old_pos2_dist = 0;
        } else {
            old_pos2_dist = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos2).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1];
            System.out.println(old_pos2_dist + " old pos 2 dist");

        }

        // Commit changes in route
        OperationInRoute toMove = vesselroutes.get(vessel).get(pos1);
        vesselroutes.get(vessel).remove(pos1);
        vesselroutes.get(vessel).add(pos2, toMove);


        //Track new times - oppdatert 20.02 - ikke testet

        int new_pos1_dist;
        int new_pos2_dist;
        int new_second_sailing;

        if(pos1==0){
            //System.out.println("Sailing from: " + vesselroutes.get(vessel).get(new_pos).getID() + " sailing to: " + vesselroutes.get(vessel).get(cur_pos+1).getID() +
            //        " it takes time periods: " + SailingTimes[vessel][vesselroutes.get(vessel).get(new_pos).getTimeperiod()][vesselroutes.get(vessel).get(new_pos).getID()][vesselroutes.get(vessel).get(cur_pos+1).getID()]
            //        + " in time peiod: " + vesselroutes.get(vessel).get(new_pos).getTimeperiod());

            new_pos1_dist = SailingTimes[vessel+1][0][startnodes[vessel]-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            System.out.println(new_pos1_dist + " new pos 1 dist");
        } else{
            new_pos1_dist = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            System.out.println(new_pos1_dist + " new pos 1 dist");
        }

        if(pos2==0) {
            new_second_sailing = SailingTimes[vessel+1][0][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist = new_second_sailing +
                    SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];
            System.out.println(new_pos2_dist + " New pos 2 dist");
        } else if (pos2==vesselroutes.get(vessel).size()-1){
            new_second_sailing = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist =  new_second_sailing +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(vessel).getEarliestTime()];

        } else{
            new_second_sailing = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_pos2_dist = new_second_sailing +
                    SailingTimes[vessel+1][vesselroutes.get(pos1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
        }

        //Updated method for setting new times (20.2) - not tested
        int first_delta = (old_pos1_dist) - (new_pos1_dist);
        int second_delta = (old_pos2_dist) - (new_pos2_dist);

        for(int i = 0; i < vesselroutes.get(vessel).size(); i++ ) {
            if (i<pos1 && i<pos2){
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(i).getLatestTime()+first_delta+second_delta);
            }
            else if(i == pos1){
                if(pos1 == 0){
                    vesselroutes.get(vessel).get(i).setEarliestTime(new_pos1_dist);
                }else {
                    int prev_op = TimeVesselUseOnOperation[vessel + 1][vesselroutes.get(vessel).get(pos1).getID() - 1 - nStartnodes][vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime()];
                    vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(pos2 - 1).getEarliestTime() + new_pos1_dist + prev_op);
                }
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(pos2).getLatestTime() + second_delta);
            }
            else if (i == pos2){
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(pos1).getEarliestTime()+first_delta);
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(pos1).getLatestTime()+second_delta);
            }
            else if ((i > pos1 && i < pos2) || (i > pos2 && i < pos1)){
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(i).getEarliestTime()+first_delta);
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(i).getLatestTime()+second_delta);
            }
            else{
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(i).getEarliestTime()+first_delta+second_delta);
            }
        }
        return vesselroutes;
    }




    public List<List<OperationInRoute>> two_relocate(List<List<OperationInRoute>> vesselroutes, int vessel1, int vessel2, int pos1, int pos2, int[] startnodes){
        if ((vessel1 == vessel2) ||
                !(ALNS.containsElement (vesselroutes.get(vessel1).get(pos1).getID(), OperationsForVessel[vessel2]))){
                return vesselroutes;
            }

        int nStartnodes = startnodes.length;
        int old_vessel1_dist;
        int old_vessel2_dist;

        //Tracker gammel tid for vessel 1 - testet for metoden 2_exchange skal fungere riktig.
        if(pos1==0) {

            old_vessel1_dist = SailingTimes[vessel1+1][0][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            old_vessel1_dist = SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else{
            old_vessel1_dist = SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()] ;
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        }


        //Track new time for vessel 1 - Completed 20.2, not tested
        int new_vessel1_dist;

        if(pos1==0) {
            new_vessel1_dist = SailingTimes[vessel1+1][0][startnodes[vessel1]-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] ;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            new_vessel1_dist = 0;
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else{
            new_vessel1_dist = SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1];
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        }


        //Commit change of position
        OperationInRoute toMove = vesselroutes.get(vessel1).get(pos1);
        vesselroutes.get(vessel1).remove(pos1);
        vesselroutes.get(vessel2).add(pos2, toMove);


        //Use method for insertion to update time of vessel 2

        //Mangler metode for oppdatering av tidene

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

            old_pos1_dist = SailingTimes[vessel+1][0][startnodes[vessel] - 1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()];

        } else{
            old_pos1_dist = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1] +
                    SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;

        }
        System.out.println(old_pos1_dist + " Old pos 1 dist");


        if(pos2==vesselroutes.get(vessel).size()-1){
            old_pos2_dist = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()];
        } else{
            old_pos2_dist = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1] +
                    SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos2).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()] ;
        }
        System.out.println(old_pos2_dist + " Old pos 2 dist");

        // Oppdatert til dette punktet 20.2 - gjort med samme indexer som i testet metode for 2exchange - men ikke testet.

        // Commit the change in the routes

        OperationInRoute toMove1 = vesselroutes.get(vessel).get(pos1);
        OperationInRoute toMove2 = vesselroutes.get(vessel).get(pos2);
        vesselroutes.get(vessel).remove(pos1);
        vesselroutes.get(vessel).add(pos2, toMove1);
        vesselroutes.get(vessel).remove(pos2-1);
        vesselroutes.get(vessel).add(pos1, toMove2);


        // Track new time usage - updated 20.2 - not tested

        int new_first_dist;
        int new_second_dist;
        int new_first_sailing;
        int new_second_sailing;

        if(pos1==0){
            //System.out.println("Sailing from: " + vesselroutes.get(vessel).get(new_pos).getID() + " sailing to: " + vesselroutes.get(vessel).get(cur_pos+1).getID() +
            //        " it takes time periods: " + SailingTimes[vessel][vesselroutes.get(vessel).get(new_pos).getTimeperiod()][vesselroutes.get(vessel).get(new_pos).getID()][vesselroutes.get(vessel).get(cur_pos+1).getID()]
            //        + " in time peiod: " + vesselroutes.get(vessel).get(new_pos).getTimeperiod());

            new_first_sailing = SailingTimes[vessel+1][0][startnodes[vessel]-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_first_dist = new_first_sailing +
                    SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos2).getEarliestTime()+
                            TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()] ;
            System.out.println(new_first_dist + " new first dist");

        } else{
            new_first_sailing = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos1-1).getID()-1][vesselroutes.get(vessel).get(pos1).getID()-1];
            new_first_dist = new_first_sailing +
                    SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos2).getEarliestTime()+
                            TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel).get(pos1).getID()-1][vesselroutes.get(vessel).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2).getEarliestTime()] ;
            System.out.println(new_first_dist + " new first dist");
        }



        //Assumption: We can use old Timeperiod for the time used on the operation and time used for sailing
        if(pos2 == vesselroutes.get(vessel).size()-1){
            new_second_sailing = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_second_dist =  new_second_sailing +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;
            System.out.println(new_second_dist + " new second dist");
        } else {
            new_second_sailing = SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel).get(pos2-1).getID()-1][vesselroutes.get(vessel).get(pos2).getID()-1];
            new_second_dist = new_second_sailing +
                    SailingTimes[vessel+1][vesselroutes.get(vessel).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel).get(pos1).getEarliestTime()]][vesselroutes.get(vessel).get(pos2).getID()-1][vesselroutes.get(vessel).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel+1][vesselroutes.get(vessel).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel).get(pos1).getEarliestTime()] ;

            System.out.println(new_second_dist + " new second dist");
        }

        // Mangler oppdatering av tider:

        //Updated method for setting new times (20.2) - not tested
        int first_delta = (old_pos1_dist) - (new_first_dist);
        int second_delta = (old_pos2_dist) - (new_second_dist);

        for(int i = 0; i < vesselroutes.get(vessel).size(); i++ ) {
            if (i<pos1 && i<pos2){
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(i).getLatestTime()+first_delta+second_delta);
            }
            else if(i == pos1){
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(pos2).getEarliestTime()+first_delta);
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(pos2).getLatestTime()+second_delta);
            }
            else if (i == pos2){
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(pos1).getEarliestTime()+first_delta);
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(pos1).getLatestTime()+second_delta);
            }
            else if ((i > pos1 && i < pos2) || (i > pos2 && i < pos1)){
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(i).getEarliestTime()+first_delta);
                vesselroutes.get(vessel).get(i).setLatestTime(vesselroutes.get(vessel).get(i).getLatestTime()+second_delta);
            }
            else{
                vesselroutes.get(vessel).get(i).setEarliestTime(vesselroutes.get(vessel).get(i).getEarliestTime()+first_delta+second_delta);
            }
        }




        return vesselroutes;
    }



    public List<List<OperationInRoute>> two_exchange(List<List<OperationInRoute>> vesselroutes, int vessel1, int vessel2, int pos1, int pos2, int[] startnodes){

        if ((vessel1 == vessel2) ||
                !((ALNS.containsElement (vesselroutes.get(vessel1).get(pos1).getID(), OperationsForVessel[vessel2])) &&
                        ALNS.containsElement(vesselroutes.get(vessel2).get(pos2).getID(), OperationsForVessel[vessel1]))){
            return vesselroutes;
        }


        int nStartnodes = startnodes.length;
        int old_vessel1_dist;
        int old_vessel2_dist;


        if(pos1==0) {

            old_vessel1_dist = SailingTimes[vessel1+1][0][startnodes[vessel1] - 1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            old_vessel1_dist = SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        } else{
            old_vessel1_dist = SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()] ;
            System.out.println(old_vessel1_dist + " Old vessel 1 dist");

        }

        //Til dette punktet stemmer funksjonen - testet 18.02.


        if(pos2==0) {
            old_vessel2_dist = SailingTimes[vessel2+1][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    SailingTimes[vessel2+1][vesselroutes.get(vessel2).get(pos2).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2).getID()-1 - nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else if(pos2==vesselroutes.get(vessel2).size()-1){
            old_vessel2_dist = SailingTimes[vessel2+1][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        } else{
            old_vessel2_dist = SailingTimes[vessel2+1][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    SailingTimes[vessel2+1][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()] ;
            System.out.println(old_vessel2_dist + " Old vessel2 dist");
        }

        // Skrevet ferdig til dette punktet 19.02 - ikke testet pga. uoverenstemmelser med ALNS.
        // I tro om at dette stemmer, da det er helt likt det over.


        //Commit exchange - this works as supposed to
        OperationInRoute toMove1 = vesselroutes.get(vessel1).get(pos1);
        OperationInRoute toMove2 = vesselroutes.get(vessel2).get(pos2);
        vesselroutes.get(vessel1).remove(pos1);
        vesselroutes.get(vessel2).remove(pos2);
        vesselroutes.get(vessel2).add(pos2, toMove1);
        vesselroutes.get(vessel1).add(pos1, toMove2);


        // Track new time usage
        //Oppdatert 19.02 - ikke testet pga. oppdateringer fra ALNS, gjort likt som over, og bør derfor være riktig.

        int new_vessel1_dist;
        int new_vessel2_dist;

        if(pos1==0) {
            new_vessel1_dist = SailingTimes[vessel1+1][0][startnodes[vessel1]-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1+1][vesselroutes.get(vessel2).get(pos2).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1- nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()]]
                            [vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1 - nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];
            System.out.println(new_vessel1_dist + " New vessel1 dist");
        } else if(pos1==vesselroutes.get(vessel1).size()-1){
            new_vessel1_dist = SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()];

        } else{
            new_vessel1_dist = SailingTimes[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1-1).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1-1).getEarliestTime()]]
                    [vesselroutes.get(vessel1).get(pos1-1).getID()-1][vesselroutes.get(vessel1).get(pos1).getID()-1] +
                    SailingTimes[vessel1+1][vesselroutes.get(vessel2).get(pos2).getEarliestTime()+TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel2).get(pos2).getEarliestTime()]][vesselroutes.get(vessel1).get(pos1).getID()-1][vesselroutes.get(vessel1).get(pos1+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel1+1][vesselroutes.get(vessel1).get(pos1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2).getEarliestTime()] ;
        }

        if(pos2==0) {
            new_vessel2_dist = SailingTimes[vessel2+1][0][startnodes[vessel2] - 1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    SailingTimes[vessel2+1][vesselroutes.get(vessel1).get(pos1).getEarliestTime() +
                            TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()]]
                            [vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2 + 1).getID()-1] +
                    TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2).getID()-1- nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()];
            System.out.println(new_vessel2_dist + " New vessel2 dist");
        } else if (pos2==vesselroutes.get(vessel2).size()-1){
            new_vessel2_dist = SailingTimes[vessel2+1][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(vessel1).getEarliestTime()];

        } else{
            new_vessel2_dist = SailingTimes[vessel2+1][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()+
                    TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2-1).getID()-1-nStartnodes][vesselroutes.get(vessel2).get(pos2-1).getEarliestTime()]]
                    [vesselroutes.get(vessel2).get(pos2-1).getID()-1][vesselroutes.get(vessel2).get(pos2).getID()-1] +
                    SailingTimes[vessel2+1][vesselroutes.get(pos1).get(pos1).getEarliestTime()+TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes]
                            [vesselroutes.get(vessel1).get(pos1).getEarliestTime()]][vesselroutes.get(vessel2).get(pos2).getID()-1][vesselroutes.get(vessel2).get(pos2+1).getID()-1] +
                    TimeVesselUseOnOperation[vessel2+1][vesselroutes.get(vessel2).get(pos2).getID()-1-nStartnodes][vesselroutes.get(vessel1).get(pos1).getEarliestTime()] ;
        }


        //Her mangler oppdatering av tidene basert på de nye deltaene.



        return vesselroutes;

    }



    /*
    public List<List<OperationInRoute>> insert(List<List<OperationInRoute>> vesselroutes, List<Integer> unroutedTasks, int vessel, int pos1, int pos2){
        OperationInRoute toMove1 = vesselroutes.get(vessel).get(pos1);
    }


     */

    public void printLSOSolution(int[] vesseltypes, List<List<OperationInRoute>> vesselroutes){
        for (int i=0;i<vesselroutes.size();i++){
            System.out.println("VESSELINDEX "+i+" VESSELTYPE "+vesseltypes[i]);
            if (vesselroutes.get(i)!=null) {
                for (OperationInRoute opInRoute : vesselroutes.get(i)) {
                    System.out.println("Operation number: "+opInRoute.getID() +" Earliest Time: "+opInRoute.getEarliestTime());
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
        ALNS a = new ALNS(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getPenalty(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(),dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS());
        a.createSortedOperations();
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);
        LS_operators LSO = new LS_operators(dg.getOperationsForVessel(), vesseltypes, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation());
        List<List<OperationInRoute>> new_vesselroutes = LSO.two_exchange(a.getVesselroutes(),1,2,1,3,  startnodes);
        LSO.printLSOSolution(vesseltypes, new_vesselroutes);
    }
}