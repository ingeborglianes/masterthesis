import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class draftRetainOld {
    /*
    public static CopyValues retainOldSolution(List<List<OperationInRoute>> vesselroutes, Map<Integer,ConsolidatedValues> consolidatedOperations,
                                               int[][] simALNS, int[][] precALNS, int[][] bigTaskALNS, int[] startNodes){

    List<List<OperationInRoute>> old_vesselroutes = new ArrayList<>();
    Map<Integer, PrecedenceValues> precedenceOverOperationsCopy=new HashMap<>();
    Map<Integer, PrecedenceValues> precedenceOfOperationsCopy=new HashMap<>();
    Map<Integer, ConnectedValues> simultaneousOpCopy=new HashMap<>();
    List<Map<Integer, PrecedenceValues>> precedenceOverRoutesCopy=new ArrayList<>();
    List<Map<Integer, PrecedenceValues>> precedenceOfRoutesCopy=new ArrayList<>();
    List<Map<Integer, ConnectedValues>> simOpRoutesCopy=new ArrayList<>();
        for(int i=0;i<vesselroutes.size();i++){
        precedenceOverRoutesCopy.add(new HashMap<>());
        precedenceOfRoutesCopy.add(new HashMap<>());
        simOpRoutesCopy.add(new HashMap<>());
    }
        for (int vessel=0; vessel<vesselroutes.size();vessel++) {
        List<OperationInRoute> route = new ArrayList<>();
        if (vesselroutes.get(vessel) != null) {
            for (int s=0;s<vesselroutes.get(vessel).size();s++) {
                OperationInRoute operationInRoute = vesselroutes.get(vessel).get(s);
                int ID = operationInRoute.getID();
                int matrixIndex=ID-1-startNodes.length;
                OperationInRoute op = new OperationInRoute(ID, operationInRoute.getEarliestTime(), operationInRoute.getLatestTime());
                route.add(op);
                if(simALNS[matrixIndex][0]!=0){
                    int connectedID = simALNS[matrixIndex][0];
                    OperationInRoute connectedObject;
                    int connectedRoute;
                    if(simultaneousOpCopy.get(connectedID)!=null){
                        connectedObject=simultaneousOpCopy.get(connectedID).getOperationObject();
                        connectedRoute=simultaneousOpCopy.get(connectedID).getRoute();
                        simultaneousOpCopy.get(connectedID).setConnectedOperationObject(op);
                        simultaneousOpCopy.get(connectedID).setConnectedRoute(vessel);
                    }
                    else{
                        connectedObject=null;
                        connectedRoute=0;
                    }
                    ConnectedValues newCV=new ConnectedValues(op,connectedObject,connectedID,s,vessel,connectedRoute);
                    simultaneousOpCopy.put(ID,newCV);
                    simOpRoutesCopy.get(vessel).put(ID,newCV);
                }
                if(simALNS[matrixIndex][1]!=0){
                    int connectedID = simALNS[matrixIndex][1];
                    OperationInRoute connectedObject;
                    int connectedRoute;
                    if(simultaneousOpCopy.get(connectedID)!=null){
                        connectedObject=simultaneousOpCopy.get(connectedID).getOperationObject();
                        connectedRoute=simultaneousOpCopy.get(connectedID).getRoute();
                        simultaneousOpCopy.get(connectedID).setConnectedOperationObject(op);
                        simultaneousOpCopy.get(connectedID).setConnectedRoute(vessel);
                    }
                    else{
                        connectedObject=null;
                        connectedRoute=0;
                    }
                    ConnectedValues newCV=new ConnectedValues(op,connectedObject,connectedID,s,vessel,connectedRoute);
                    simultaneousOpCopy.put(ID,newCV);
                    simOpRoutesCopy.get(vessel).put(ID,newCV);
                }
                if(precALNS[matrixIndex][0]!=0){
                    int connectedID = precALNS[matrixIndex][0];
                    OperationInRoute connectedObject;
                    int connectedRoute;
                    if(precedenceOfOperationsCopy.get(connectedID)!=null){
                        connectedObject=precedenceOfOperationsCopy.get(connectedID).getOperationObject();
                        connectedRoute=precedenceOfOperationsCopy.get(connectedID).getRoute();
                        precedenceOfOperationsCopy.get(connectedID).setConnectedOperationObject(op);
                        precedenceOfOperationsCopy.get(connectedID).setConnectedRoute(vessel);
                    }
                    else{
                        connectedObject=null;
                        connectedRoute=0;
                    }
                    PrecedenceValues newPV=new PrecedenceValues(op,connectedObject,connectedID,s,vessel,connectedRoute);
                    precedenceOverOperationsCopy.put(ID,newPV);
                    precedenceOverRoutesCopy.get(vessel).put(ID,newPV);
                }
                if(precALNS[matrixIndex][1]!=0){
                    int connectedID = precALNS[matrixIndex][1];
                    OperationInRoute connectedObject;
                    int connectedRoute;
                    if(precedenceOverOperationsCopy.get(connectedID)!=null){
                        connectedObject=precedenceOverOperationsCopy.get(connectedID).getOperationObject();
                        connectedRoute=precedenceOverOperationsCopy.get(connectedID).getRoute();
                        precedenceOverOperationsCopy.get(connectedID).setConnectedOperationObject(op);
                        precedenceOverOperationsCopy.get(connectedID).setConnectedRoute(vessel);
                    }
                    else{
                        connectedObject=null;
                        connectedRoute=0;
                    }
                    PrecedenceValues newPV=new PrecedenceValues(op,connectedObject,connectedID,s,vessel,connectedRoute);
                    precedenceOfOperationsCopy.put(ID,newPV);
                    precedenceOfRoutesCopy.get(vessel).put(ID,newPV);
                }
                if (bigTaskALNS[matrixIndex] != null) {
                    if (bigTaskALNS[matrixIndex][0] == ID) {
                        consolidatedOperations.get(ID).setConsolidatedRoute(vessel);
                        consolidatedOperations.get(ID).setConnectedRoute1(0);
                        consolidatedOperations.get(ID).setConnectedRoute2(0);
                    } else if (bigTaskALNS[matrixIndex][1] == ID) {
                        consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConnectedRoute1(vessel);
                        consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConsolidatedRoute(0);
                    } else if (bigTaskALNS[matrixIndex][2] == ID) {
                        consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConnectedRoute2(vessel);
                        consolidatedOperations.get(bigTaskALNS[matrixIndex][0]).setConsolidatedRoute(0);
                    }
                }
            }
        }
        old_vesselroutes.add(route);
    }
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfOperationsCopy.entrySet()){
        int taskID=entry.getKey();
        PrecedenceValues pvOf=entry.getValue();
        int connectedPOverTaskID=precALNS[taskID-1-startNodes.length][1];
        if(pvOf.getConnectedOperationObject()==null){
            PrecedenceValues pvOver = precedenceOverOperationsCopy.get(connectedPOverTaskID);
            pvOf.setConnectedOperationObject(pvOver.getOperationObject());
            pvOf.setConnectedRoute(pvOver.getRoute());
        }
    }
        return new CopyValues(old_vesselroutes,precedenceOverOperationsCopy,precedenceOfOperationsCopy,simultaneousOpCopy,
                              precedenceOverRoutesCopy,precedenceOfRoutesCopy,simOpRoutesCopy);
}
        */
}
