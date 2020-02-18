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

        int cur_pos = pos1;
        int new_pos = pos2;
        if(pos2<pos1){
            cur_pos = pos2;
            new_pos = pos1;
        }

        OperationInRoute toMove = vesselroutes.get(vessel).get(cur_pos);
        vesselroutes.get(vessel).remove(cur_pos);
        vesselroutes.get(vessel).add(new_pos, toMove);

        // Nå funker oppdatering av tider ved endring med denne metoden. Men TimeVesselUseOnOperation skaper også noen indexproblemer, se linje 36 og 45
/*
        int cum_time= 0;
        for(int i = 0; i < vesselroutes.get(vessel).size(); i++ ) {

            if (i == 0) {
                int sail_time = SailingTimes[vessel][0][startnodes[vessel]][vesselroutes.get(vessel).get(i).getID()];
                cum_time = cum_time + sail_time;
                //Knot med indexer
                int op_time = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(i).getID()-startnodes.length+1][cum_time];
                vesselroutes.get(vessel).get(i).setTimeperiod(cum_time);
                cum_time = cum_time + op_time;

            }
            else {
                int sail_time = SailingTimes[vessel][cum_time][vesselroutes.get(vessel).get(i-1).getID()][vesselroutes.get(vessel).get(i).getID()];
                cum_time = cum_time + sail_time;
                //Knot med indexer
                int op_time = TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(i).getID()-startnodes.length+1][cum_time];
                vesselroutes.get(vessel).get(i).setTimeperiod(cum_time);
                cum_time = cum_time + op_time;

            }
        }
 */
        // Nytt forsøk på oppdatering av tid

        // Calculate old time :
        int old_first_dist;
        int old_second_dist;
        int new_first_dist;
        int new_second_dist;
        System.out.println(vessel + " vessel");
        System.out.println(startnodes[vessel] + " startnode");
        System.out.println(vesselroutes.get(vessel).get(cur_pos).getID() + " ID");
        System.out.println(SailingTimes[vessel][0][startnodes[vessel]][vesselroutes.get(vessel).get(cur_pos).getID()] + " første seiling");
        if(cur_pos==0){
            System.out.println(SailingTimes[vessel][0][startnodes[vessel]][vesselroutes.get(vessel).get(new_pos).getID()]);
            old_first_dist = SailingTimes[vessel][0][startnodes[vessel]][vesselroutes.get(vessel).get(new_pos).getID()] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(cur_pos).getTimeperiod()][vesselroutes.get(vessel).get(cur_pos).getID()][vesselroutes.get(vessel).get(cur_pos+1).getID()] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(cur_pos).getID()][vesselroutes.get(vessel).get(cur_pos).getTimeperiod()] ;
            new_first_dist = SailingTimes[vessel][0][startnodes[vessel]][vesselroutes.get(vessel).get(new_pos).getID()];
            System.out.println(vesselroutes.get(vessel).get(new_pos).getID() + " her");
        } else{
            old_first_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(cur_pos-1).getTimeperiod()][vesselroutes.get(vessel).get(cur_pos-1).getID()][vesselroutes.get(vessel).get(new_pos).getID()] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(cur_pos).getTimeperiod()][vesselroutes.get(vessel).get(cur_pos).getID()][vesselroutes.get(vessel).get(cur_pos+1).getID()] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(cur_pos).getID()][vesselroutes.get(vessel).get(cur_pos).getTimeperiod()] ;
            new_first_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(cur_pos-1).getTimeperiod()][vesselroutes.get(vessel).get(cur_pos-1).getID()][vesselroutes.get(vessel).get(new_pos).getID()];
        }

        if(new_pos == vesselroutes.get(vessel).size()-1){
            old_second_dist = 0;
            new_second_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(new_pos-1).getTimeperiod()][vesselroutes.get(vessel).get(new_pos-1).getID()][vesselroutes.get(vessel).get(cur_pos).getID()] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(cur_pos).getID()][vesselroutes.get(vessel).get(new_pos).getTimeperiod()] ;
        } else {
            old_second_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(new_pos).getTimeperiod()][vesselroutes.get(vessel).get(new_pos).getID()][vesselroutes.get(vessel).get(new_pos + 1).getID()];
            new_second_dist = SailingTimes[vessel][vesselroutes.get(vessel).get(new_pos-1).getTimeperiod()][vesselroutes.get(vessel).get(new_pos-1).getID()][vesselroutes.get(vessel).get(cur_pos).getID()] +
                    SailingTimes[vessel][vesselroutes.get(vessel).get(new_pos).getTimeperiod()][vesselroutes.get(vessel).get(cur_pos).getID()][vesselroutes.get(vessel).get(new_pos+1).getID()] +
                    TimeVesselUseOnOperation[vessel][vesselroutes.get(vessel).get(cur_pos).getID()][vesselroutes.get(vessel).get(new_pos).getTimeperiod()] ;

        }

        int fist_delta = (old_first_dist) - (new_first_dist);
        int second_delta = (old_first_dist+old_second_dist) - (new_first_dist+new_second_dist);

        for(int i = 0; i < vesselroutes.get(vessel).size(); i++ ) {
            if (i<cur_pos){
                continue;
            }
            else if (i >= cur_pos && i < new_pos){
                vesselroutes.get(vessel).get(i).setTimeperiod(vesselroutes.get(vessel).get(i).getTimeperiod()+fist_delta);
            }
            else{
                vesselroutes.get(vessel).get(i).setTimeperiod(vesselroutes.get(vessel).get(i).getTimeperiod()+fist_delta+second_delta);
            }
        }

        return vesselroutes;
    }

    public List<List<OperationInRoute>> two_relocate(List<List<OperationInRoute>> vesselroutes, int cur_vessel, int new_vessel, int cur_pos, int new_pos){
        if (cur_vessel == new_vessel){
            return vesselroutes;
        }

        if(ALNS.containsElement (vesselroutes.get(cur_vessel).get(cur_pos).getID()+1, OperationsForVessel[new_vessel])) {
            OperationInRoute toMove = vesselroutes.get(cur_vessel).get(cur_pos);
            vesselroutes.get(cur_vessel).remove(cur_pos);
            vesselroutes.get(new_vessel).add(new_pos, toMove);
        }
        return vesselroutes;
    }

    public List<List<OperationInRoute>> one_exchange(List<List<OperationInRoute>> vesselroutes, int vessel, int pos1, int pos2){
        if (pos1 == pos2){
            return vesselroutes;
        }
        OperationInRoute toMove1 = vesselroutes.get(vessel).get(pos1);
        OperationInRoute toMove2 = vesselroutes.get(vessel).get(pos2);
        vesselroutes.get(vessel).remove(pos1);
        vesselroutes.get(vessel).add(pos2, toMove1);
        if (pos1 < pos2){
            vesselroutes.get(vessel).remove(pos2-1);
            vesselroutes.get(vessel).add(pos1, toMove2);
        }
        else{
            vesselroutes.get(vessel).remove(pos2+1);
            vesselroutes.get(vessel).add(pos1, toMove2);
        }
        return vesselroutes;
    }

    public List<List<OperationInRoute>> two_exchange(List<List<OperationInRoute>> vesselroutes, int vessel1, int vessel2, int pos1, int pos2){
        if (vessel1 == vessel2){
            return vesselroutes;
        }
        if((ALNS.containsElement (vesselroutes.get(vessel1).get(pos1).getID()+1, OperationsForVessel[vessel2])) &&
                ALNS.containsElement(vesselroutes.get(vessel2).get(pos2).getID()+1, OperationsForVessel[vessel1])) {

            OperationInRoute toMove1 = vesselroutes.get(vessel1).get(pos1);
            OperationInRoute toMove2 = vesselroutes.get(vessel2).get(pos2);
            vesselroutes.get(vessel1).remove(pos1);
            vesselroutes.get(vessel2).remove(pos2);
            vesselroutes.get(vessel2).add(pos2, toMove1);
            vesselroutes.get(vessel1).add(pos1, toMove2);
        }
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
                    System.out.println("Operation number: "+opInRoute.getID() +" Time: "+opInRoute.getTimeperiod());
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
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);
        LS_operators LSO = new LS_operators(dg.getOperationsForVessel(), vesseltypes, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation());
        List<List<OperationInRoute>> new_vesselroutes = LSO.one_relocate(a.getVesselroutes(),0,0,1, startnodes);
        LSO.printLSOSolution(vesseltypes, new_vesselroutes);
    }
}