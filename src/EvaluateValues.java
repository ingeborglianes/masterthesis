import java.util.ArrayList;
import java.util.List;

public class EvaluateValues {
    int[] routeOperationGain;
    int[] routeSailingCost;
    List<List<OperationInRoute>> vesselroutes;
    List<OperationInRoute> unrouted;

    public EvaluateValues(int[] routeOperationGain, int[] routeSailingCost, List<List<OperationInRoute>> vesselroutes,
                          List<OperationInRoute> unrouted){
        this.routeOperationGain=routeOperationGain;
        this.routeSailingCost=routeSailingCost;
        this.vesselroutes=vesselroutes;
        this.unrouted=unrouted;
    }

    public int[] getRouteOperationGain() {
        return routeOperationGain;
    }

    public int[] getRouteSailingCost() {
        return routeSailingCost;
    }

    public List<List<OperationInRoute>> getVesselroutes() {
        return vesselroutes;
    }

    public List<OperationInRoute> getUnrouted() {
        return unrouted;
    }
}
