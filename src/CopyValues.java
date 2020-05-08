import java.util.List;
import java.util.Map;

public class CopyValues {
    private List<List<OperationInRoute>> vesselRoutes;
    private Map<Integer, PrecedenceValues> precedenceOverOperations;
    private Map<Integer, PrecedenceValues> precedenceOfOperations;
    private Map<Integer, ConnectedValues> simultaneousOp;
    private List<Map<Integer, PrecedenceValues>> precedenceOverRoutes;
    private List<Map<Integer, PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;

    public CopyValues(List<List<OperationInRoute>> vesselRoutes,Map<Integer, PrecedenceValues> precedenceOverOperations, Map<Integer, PrecedenceValues> precedenceOfOperations,
            Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, PrecedenceValues>> precedenceOverRoutes, List<Map<Integer, PrecedenceValues>> precedenceOfRoutes,
            List<Map<Integer, ConnectedValues>> simOpRoutes){
        this.precedenceOverOperations=precedenceOverOperations;
        this.precedenceOfOperations=precedenceOfOperations;
        this.simultaneousOp=simultaneousOp;
        this.precedenceOverRoutes=precedenceOverRoutes;
        this.precedenceOfRoutes=precedenceOfRoutes;
        this.simOpRoutes=simOpRoutes;
        this.vesselRoutes=vesselRoutes;
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

    public List<Map<Integer, PrecedenceValues>> getPrecedenceOverRoutes() {
        return precedenceOverRoutes;
    }

    public List<Map<Integer, PrecedenceValues>> getPrecedenceOfRoutes() {
        return precedenceOfRoutes;
    }

    public List<Map<Integer, ConnectedValues>> getSimOpRoutes() {
        return simOpRoutes;
    }

    public List<List<OperationInRoute>> getVesselRoutes() {
        return vesselRoutes;
    }
}
