import java.util.List;

public class LS_operators {
    List<List<OperationInRoute>> vesselroutes;

    public LS_operators(List<List<OperationInRoute>> vesselroutes){
        this.vesselroutes = vesselroutes;
    }


    public List<List<OperationInRoute>> one_relocate(List<List<OperationInRoute>> vesselroutes, int vessel, int cur_por, int new_pos){
        vesselroutes.get(vessel).remove(cur_por);
        vesselroutes.get(vessel).add(new_pos,);

    }

}
