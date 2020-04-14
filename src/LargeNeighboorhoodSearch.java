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
            if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 || simALNS[selectedTaskID-startNodes.length-1][0] != 0) {
                removeSynchronizedOp(simultaneousOp.get(selectedTaskID),null,0);
                if(simALNS[selectedTaskID-startNodes.length-1][0] != 0 ) {
                    removeSynchronizedOp(simultaneousOp.get(simALNS[selectedTaskID-startNodes.length-1][0]),null,0);
                }
                if(simALNS[selectedTaskID-startNodes.length-1][1] != 0 ) {
                    removeSynchronizedOp(simultaneousOp.get(simALNS[selectedTaskID-startNodes.length-1][1]),null,0);
                }
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

    public void removeSynchronizedOp(ConnectedValues simOp, PrecedenceValues pOp, int presType){
        int route;
        int index;
        if(simOp!=null){
            route=simOp.getRoute();
            index=simOp.getIndex();
        }
        else{
            route=pOp.getRoute();
            index=pOp.getIndex();
        }
        int prevEarliest=0;
        if(index - 1!=-1){
            prevEarliest = vesselRoutes.get(route).get(index - 1).getEarliestTime();
        }
        if(index - 1==-1){
            OperationInRoute firstOp = vesselRoutes.get(route).get(0);
            if(twIntervals[firstOp.getID()-startNodes.length-1][0]==0){
                prevEarliest = 1;
            }
            else{
                prevEarliest=twIntervals[firstOp.getID()-startNodes.length-1][0];
            }
            firstOp.setEarliestTime(prevEarliest);
        }
        unroutedTasks.add(simOp.getOperationObject());
        vesselRoutes.get(route).remove(index);
        if(simOp!=null){
            simultaneousOp.remove(simOp.getOperationObject().getID());
            simOpRoutes.get(simOp.getRoute()).remove(simOp.getOperationObject().getID());
        }
        if(pOp!=null){
            if(presType==0) {
                precedenceOverOperations.remove(pOp.getOperationObject().getID());
                precedenceOverRoutes.get(route).remove(pOp.getOperationObject().getID());
            }
            else{
                precedenceOfOperations.remove(pOp.getOperationObject().getID());
                precedenceOfRoutes.get(route).remove(pOp.getOperationObject().getID());
            }
        }
        int nextLatest = 0;
        if (vesselRoutes.get(simOp.getRoute()).size() > simOp.getIndex()) {
            nextLatest = vesselRoutes.get(simOp.getRoute()).get(simOp.getIndex()).getLatestTime();
        }
        if (simOp.getIndex() == vesselRoutes.get(simOp.getRoute()).size()) {
            OperationInRoute lastOp = vesselRoutes.get(simOp.getRoute()).get(vesselRoutes.get(simOp.getRoute()).size() - 1);
            nextLatest = twIntervals[lastOp.getID() - startNodes.length - 1][1];
            lastOp.setLatestTime(nextLatest);
        }
        ConstructionHeuristic.updateEarliest(prevEarliest, Math.max(simOp.getIndex() - 1,0), simOp.getRoute(), TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes);
        ConstructionHeuristic.updateLatestAfterRemoval(nextLatest, Math.min(simOp.getIndex(), vesselRoutes.get(simOp.getRoute()).size() - 1), simOp.getRoute(),vesselRoutes, TimeVesselUseOnOperation, startNodes,
                SailingTimes, twIntervals);
        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(simOp.getRoute()), simOp.getIndex(),simOpRoutes,precedenceOfOperations,precedenceOverOperations,TimeVesselUseOnOperation,
                startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselRoutes,SailingTimes);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOverRoutes.get(simOp.getRoute()), simOp.getIndex(),TimeVesselUseOnOperation,startNodes,simOpRoutes,
                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselRoutes,simultaneousOp,SailingTimes);
        ConstructionHeuristic.updateSimultaneousAfterRemoval(simOpRoutes.get(simOp.getRoute()), simOp.getRoute(), simOp.getIndex() - 1,
                simultaneousOp, vesselRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes);
    }

    //remember to change argument to int operationNumber
    public void removeNormalOp(ConnectedValues simOp){
        int prevEarliest=0;
        if(simOp.getIndex() - 1!=-1){
            prevEarliest = vesselRoutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getEarliestTime();
        }
        if(simOp.getIndex() - 1==-1){
            OperationInRoute firstOp = vesselRoutes.get(simOp.getRoute()).get(0);
            if(twIntervals[firstOp.getID()-startNodes.length-1][0]==0){
                prevEarliest = 1;
            }
            else{
                prevEarliest=twIntervals[firstOp.getID()-startNodes.length-1][0];
            }
            firstOp.setEarliestTime(prevEarliest);
        }
        unroutedTasks.add(simOp.getOperationObject());
        vesselRoutes.get(simOp.getRoute()).remove(simOp.getIndex());
        simultaneousOp.remove(simOp.getOperationObject().getID());
        simOpRoutes.get(simOp.getRoute()).remove(simOp.getOperationObject().getID());
        int nextLatest = 0;
        if (vesselRoutes.get(simOp.getRoute()).size() > simOp.getIndex()) {
            nextLatest = vesselRoutes.get(simOp.getRoute()).get(simOp.getIndex()).getLatestTime();
        }
        if (simOp.getIndex() == vesselRoutes.get(simOp.getRoute()).size()) {
            OperationInRoute lastOp = vesselRoutes.get(simOp.getRoute()).get(vesselRoutes.get(simOp.getRoute()).size() - 1);
            nextLatest = twIntervals[lastOp.getID() - startNodes.length - 1][1];
            lastOp.setLatestTime(nextLatest);
        }
        ConstructionHeuristic.updateEarliest(prevEarliest, Math.max(simOp.getIndex() - 1,0), simOp.getRoute(), TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes);
        ConstructionHeuristic.updateLatestAfterRemoval(nextLatest, Math.min(simOp.getIndex(), vesselRoutes.get(simOp.getRoute()).size() - 1), simOp.getRoute(),vesselRoutes, TimeVesselUseOnOperation, startNodes,
                SailingTimes, twIntervals);
        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(simOp.getRoute()), simOp.getIndex(),simOpRoutes,precedenceOfOperations,precedenceOverOperations,TimeVesselUseOnOperation,
                startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselRoutes,SailingTimes);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOverRoutes.get(simOp.getRoute()), simOp.getIndex(),TimeVesselUseOnOperation,startNodes,simOpRoutes,
                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselRoutes,simultaneousOp,SailingTimes);
        ConstructionHeuristic.updateSimultaneousAfterRemoval(simOpRoutes.get(simOp.getRoute()), simOp.getRoute(), simOp.getIndex() - 1,
                simultaneousOp, vesselRoutes, TimeVesselUseOnOperation, startNodes, SailingTimes);
    }

}
