import java.lang.reflect.Array;
import java.util.List;

public class LS_operators {
    List<List<OperationInRoute>> vesselroutes;
    private int [][] OperationsForVessel;
    private int [][] TimeWindowsForOperations;
    private int [][][] Edges;
    private int [][][][] SailingTimes;
    private int [][][] TimeVesselUseOnOperation;
    private int [] EarliestStartingTimeForVessel;


    public LS_operators(List<List<OperationInRoute>> vesselroutes, int [][] OperationsForVessel, int [][] TimeWindowsForOperations, int [][][] Edges,
                        int [][][][] SailingTimes, int [][][] TimeVesselUseOnOperation, int [] EarliestStartingTimeForVessel){
        this.vesselroutes = vesselroutes;
        this.OperationsForVessel = OperationsForVessel;
        this.TimeWindowsForOperations = TimeWindowsForOperations;
        this.Edges = Edges;
        this.SailingTimes = SailingTimes;
        this.TimeVesselUseOnOperation = TimeVesselUseOnOperation;
        this.EarliestStartingTimeForVessel = EarliestStartingTimeForVessel;

    }

    public List<List<OperationInRoute>> one_relocate(List<List<OperationInRoute>> vesselroutes, int vessel, int cur_pos, int new_pos){
        if (cur_pos == new_pos){
            return vesselroutes;
        }
        OperationInRoute toMove = vesselroutes.get(vessel).get(cur_pos);
        vesselroutes.get(vessel).remove(cur_pos);
        vesselroutes.get(vessel).add(new_pos, toMove);
        return vesselroutes;
    }

    public List<List<OperationInRoute>> two_relocate(List<List<OperationInRoute>> vesselroutes, int cur_vessel, int new_vessel, int cur_pos, int new_pos){
        if (cur_vessel == new_vessel){
            return vesselroutes;
        }
        OperationInRoute toMove = vesselroutes.get(cur_vessel).get(cur_pos);
        vesselroutes.get(cur_vessel).remove(cur_pos);
        vesselroutes.get(new_vessel).add(new_pos, toMove);
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
        OperationInRoute toMove1 = vesselroutes.get(vessel1).get(pos1);
        OperationInRoute toMove2 = vesselroutes.get(vessel2).get(pos2);
        vesselroutes.get(vessel1).remove(pos1);
        vesselroutes.get(vessel2).remove(pos2);
        vesselroutes.get(vessel2).add(pos2, toMove1);
        vesselroutes.get(vessel1).add(pos1, toMove2);
        return vesselroutes;
    }


    /*
    public List<List<OperationInRoute>> insert(List<List<OperationInRoute>> vesselroutes, List<Integer> unroutedTasks, int vessel, int pos1, int pos2){
        OperationInRoute toMove1 = vesselroutes.get(vessel).get(pos1);
    }


     */



}
