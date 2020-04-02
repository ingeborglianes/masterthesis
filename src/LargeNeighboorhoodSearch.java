import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class LargeNeighboorhoodSearch {
    private Map<Integer,PrecedenceValues> precedenceOverOperations;
    private Map<Integer,PrecedenceValues> precedenceOfOperations;
    //map for operations that are connected as simultaneous operations. ID= operation number. Value= Simultaneous value.
    private Map<Integer, ConnectedValues> simultaneousOp;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;
    private List<Map<Integer,PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer,PrecedenceValues>> precedenceOverRoutes;
    private Map<Integer, ConsolidatedValues> consolidatedOperations;
    private List<OperationInRoute> unroutedTasks;
    private List<List<OperationInRoute>> vesselRoutes;

    public LargeNeighboorhoodSearch(Map<Integer,PrecedenceValues> precedenceOverOperations, Map<Integer,PrecedenceValues> precedenceOfOperations,
                                    Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, ConnectedValues>> simOpRoutes,
                                    List<Map<Integer,PrecedenceValues>> precedenceOfRoutes, List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                                    Map<Integer, ConsolidatedValues> consolidatedOperations, List<OperationInRoute> unroutedTasks,
                                    List<List<OperationInRoute>> vesselRoutes){
        this.precedenceOverOperations=precedenceOverOperations;
        this.precedenceOfOperations=precedenceOfOperations;
        this.simultaneousOp=simultaneousOp;
        this.simOpRoutes=simOpRoutes;
        this.precedenceOfRoutes=precedenceOfRoutes;
        this.precedenceOverRoutes=precedenceOverRoutes;
        this.unroutedTasks=unroutedTasks;
        this.vesselRoutes=vesselRoutes;
        this.consolidatedOperations=consolidatedOperations;
    }
    //removal methods

    public void randomRemoval(){
        int randomRoute = ThreadLocalRandom.current().nextInt(0, vesselRoutes.size());
        int randomIndex = ThreadLocalRandom.current().nextInt(0, vesselRoutes.get(randomRoute).size());
        OperationInRoute selectedTask=vesselRoutes.get(randomRoute).get(randomIndex);
        //sim task
        if(simultaneousOp.get(selectedTask.getID())!=null){

        }
        //precedence of task
        else if(simultaneousOp.get(selectedTask.getID())!=null){

        }
        //precedence over task
        else if(simultaneousOp.get(selectedTask.getID())!=null){

        }
        //normal task


    }



    public void worstRemoval(){

    }

    public void randomRemovalWRandomness(){

    }

    public void worstRemovalWRandomness(){

    }

    public void synchronizedRemoval(){

    }

    public void routeRemoval(){

    }

}
