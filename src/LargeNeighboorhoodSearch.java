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
    private int numberOfRemoval;
    private int [][] twIntervals;
    private int [][] precedenceALNS;
    private int [][] simALNS;
    private int [] startNodes;
    private int [][][][] SailingTimes;
    private int [][][] TimeVesselUseOnOperation;

    public LargeNeighboorhoodSearch(Map<Integer,PrecedenceValues> precedenceOverOperations, Map<Integer,PrecedenceValues> precedenceOfOperations,
                                    Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, ConnectedValues>> simOpRoutes,
                                    List<Map<Integer,PrecedenceValues>> precedenceOfRoutes, List<Map<Integer,PrecedenceValues>> precedenceOverRoutes,
                                    Map<Integer, ConsolidatedValues> consolidatedOperations, List<OperationInRoute> unroutedTasks,
                                    List<List<OperationInRoute>> vesselRoutes, int numberOfRemoval, int [][]twIntervals,
                                    int[][] precedenceALNS,int[][] simALNS,int [] startNodes, int [][][][] SailingTimes,
                                    int [][][] TimeVesselUseOnOperation){
        this.precedenceOverOperations=precedenceOverOperations;
        this.precedenceOfOperations=precedenceOfOperations;
        this.simultaneousOp=simultaneousOp;
        this.simOpRoutes=simOpRoutes;
        this.precedenceOfRoutes=precedenceOfRoutes;
        this.precedenceOverRoutes=precedenceOverRoutes;
        this.unroutedTasks=unroutedTasks;
        this.vesselRoutes=vesselRoutes;
        this.consolidatedOperations=consolidatedOperations;
        this.numberOfRemoval=numberOfRemoval;
        this.twIntervals=twIntervals;
        this.precedenceALNS=precedenceALNS;
        this.simALNS=simALNS;
        this.startNodes=startNodes;
        this.SailingTimes=SailingTimes;
        this.TimeVesselUseOnOperation=TimeVesselUseOnOperation;

    }
    //removal methods

    public void randomRemoval(){
        for (int i=0;i<numberOfRemoval;i++){
            int randomRoute = ThreadLocalRandom.current().nextInt(0, vesselRoutes.size());
            int randomIndex = ThreadLocalRandom.current().nextInt(0, vesselRoutes.get(randomRoute).size());
            OperationInRoute selectedTask=vesselRoutes.get(randomRoute).get(randomIndex);
            int selectedTaskID = selectedTask.getID();
            //sim task
            if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 ) {
                /*
                //simultaneous over removed
                ConstructionHeuristic.updatesAfterRemoval(simultaneousOp.get(simALNS[selectedTaskID-startNodes.length-1][1]),null,simultaneousOp, vesselRoutes, TimeVesselUseOnOperation, startNodes,
                        SailingTimes, twIntervals, unroutedTasks, simOpRoutes, precedenceOverOperations, precedenceOfOperations,
                        precedenceOverRoutes, precedenceOfRoutes);
                // next: remove simultaneous of
                ConstructionHeuristic.updatesAfterRemoval(simultaneousOp.get(selectedTaskID),null,simultaneousOp, vesselRoutes, TimeVesselUseOnOperation, startNodes,
                        SailingTimes, twIntervals, unroutedTasks, simOpRoutes, precedenceOverOperations, precedenceOfOperations,
                        precedenceOverRoutes, precedenceOfRoutes);

                 */
            }
            else if(simALNS[selectedTaskID-startNodes.length-1][0] != 0 ) {
                /*
                //simultaneous of removed
                ConstructionHeuristic.updatesAfterRemoval(simultaneousOp.get(simALNS[selectedTaskID-startNodes.length-1][0]),null,simultaneousOp, vesselRoutes, TimeVesselUseOnOperation, startNodes,
                        SailingTimes, twIntervals, unroutedTasks, simOpRoutes, precedenceOverOperations, precedenceOfOperations,
                        precedenceOverRoutes, precedenceOfRoutes);
                // next: remove simultaneous over
                ConstructionHeuristic.updatesAfterRemoval(simultaneousOp.get(selectedTaskID),null,simultaneousOp, vesselRoutes, TimeVesselUseOnOperation, startNodes,
                        SailingTimes, twIntervals, unroutedTasks, simOpRoutes, precedenceOverOperations, precedenceOfOperations,
                        precedenceOverRoutes, precedenceOfRoutes);
                */
            }
            //precedence of task
            else if(precedenceOfOperations.get(selectedTask.getID())!=null){

            }
            //precedence over task
            else if(precedenceOverOperations.get(selectedTask.getID())!=null){

            }
            //normal task
        }

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
