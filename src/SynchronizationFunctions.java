public class SynchronizationFunctions {
    /*
    public Boolean checkCrossSim(int o, int v, int index){
        int simOf=simALNS[o-startNodes.length-1][1];
        if(simOpRoutes.get(v).size() != 0 &&  simOf!= 0){
            for (ConnectedValues cValue:simOpRoutes.get(v).values()){
                ConnectedValues simOp=simultaneousOp.get(simOf);
                if(simOp.getRoute()==cValue.getConnectedRoute()){
                    if (cValue.getIndex()<index){
                        if(crossSimultaneous[simOp.getOperationObject().getID()-1-startNodes.length][cValue.getConnectedOperationID()-1-startNodes.length]==1){
                            System.out.println("Not valid insertion because of cross syn");
                            return true;
                        }
                    }
                    if (cValue.getIndex()>=index){
                        if(crossSimultaneous[cValue.getConnectedOperationID()-1-startNodes.length][simOp.getOperationObject().getID()-1-startNodes.length]==1){
                            System.out.println("Not valid insertion because of cross syn");
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void updateCrossSimultaneousInsertion(int routeIndex, ConnectedValues sValues, int o, int indexInRoute){
        for(ConnectedValues valuesConnectedRoute: simOpRoutes.get(sValues.getRoute()).values()){
            if (valuesConnectedRoute.getOperationObject().getID() != sValues.getOperationObject().getID() &&
                    crossSimultaneous[valuesConnectedRoute.getOperationObject().getID()-1-startNodes.length][sValues.getOperationObject().getID()-1-startNodes.length]==1){
                System.out.println("index 1: "+valuesConnectedRoute.getOperationObject().getID()+" index 2: "+o);
                crossSimultaneous[valuesConnectedRoute.getOperationObject().getID()-1-startNodes.length][o-1-startNodes.length]=1;
            }
            else if (valuesConnectedRoute.getOperationObject().getID() != sValues.getOperationObject().getID() &&
                    crossSimultaneous[sValues.getOperationObject().getID()-1-startNodes.length][valuesConnectedRoute.getOperationObject().getID()-1-startNodes.length]==1){
                System.out.println("index 1: "+o+" index 2: "+valuesConnectedRoute.getOperationObject().getID());
                crossSimultaneous[o-1-startNodes.length][valuesConnectedRoute.getOperationObject().getID()-1-startNodes.length]=1;
            }
        }
        for(ConnectedValues cV: simOpRoutes.get(routeIndex).values()){
            if(cV.getIndex()<indexInRoute){
                crossSimultaneous[cV.getOperationObject().getID()-startNodes.length-1][o-startNodes.length-1]=1;
                if(cV.getConnectedRoute()==sValues.getRoute() && cV.getConnectedOperationObject().getID()!=sValues.getOperationObject().getID()){
                    System.out.println("index 1: "+cV.getConnectedOperationObject().getID()+" index 2: "+sValues.getOperationObject().getID());
                    crossSimultaneous[cV.getConnectedOperationObject().getID()-1-startNodes.length][sValues.getOperationObject().getID()-1-startNodes.length]=1;
                }
            }
            else{
                crossSimultaneous[o-1-startNodes.length][cV.getOperationObject().getID()-1-startNodes.length]=1;
                if(cV.getConnectedRoute()==sValues.getRoute() && cV.getConnectedOperationObject().getID()!=sValues.getOperationObject().getID()){
                    System.out.println("index 1: "+sValues.getOperationObject().getID()+" index 2: "+cV.getConnectedOperationObject().getID());
                    crossSimultaneous[sValues.getOperationObject().getID()-1-startNodes.length][cV.getConnectedOperationObject().getID()-1-startNodes.length]=1;
                }
            }
        }
    }

    if(simALNS[o-startNodes.length-1][1] != 0 ) {
                    ConnectedValues simOp = simultaneousOp.get(simALNS[o - startNodes.length - 1][1]);
                    int prevEarliest=0;
                    if(simOp.getIndex() > 0) {
                        prevEarliest = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex() - 1).getEarliestTime();
                    }
                    OperationInRoute firstOp = vesselroutes.get(simOp.getRoute()).get(vesselroutes.get(simOp.getRoute()).size() - 1);
                    if (simOp.getIndex() == 0) {
                        prevEarliest = twIntervals[firstOp.getID() - startNodes.length - 1][0];
                    }
                    firstOp.setEarliestTime(prevEarliest);
                    unroutedTasks.add(simOp.getOperationObject());
                    vesselroutes.get(simOp.getRoute()).remove(simOp.getIndex());
                    simultaneousOp.remove(simOp.getOperationObject().getID());
                    simOpRoutes.get(simOp.getRoute()).remove(simOp.getOperationObject().getID());
                    int nextLatest = 0;
                    if (vesselroutes.get(simOp.getRoute()).size() > simOp.getIndex()) {
                        nextLatest = vesselroutes.get(simOp.getRoute()).get(simOp.getIndex()).getLatestTime();
                    }
                    OperationInRoute lastOp = vesselroutes.get(simOp.getRoute()).get(vesselroutes.get(simOp.getRoute()).size() - 1);
                    if (simOp.getIndex() == vesselroutes.get(simOp.getRoute()).size()) {
                        nextLatest = twIntervals[lastOp.getID() - startNodes.length - 1][1];
                    }
                    lastOp.setLatestTime(nextLatest);
                    updateEarliest(prevEarliest, Math.max(simOp.getIndex()-1, 0), simOp.getRoute(), TimeVesselUseOnOperation, startNodes, SailingTimes, vesselroutes);
                    updateLatestAfterRemoval(nextLatest, Math.min(simOp.getIndex(), vesselroutes.get(simOp.getRoute()).size() - 1), simOp.getRoute());
                    updatePrecedenceOver(precedenceOverRoutes.get(routeIndex), simOp.getIndex(),simOpRoutes,precedenceOfOperations,precedenceOverOperations,TimeVesselUseOnOperation,
                            startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselroutes,SailingTimes);
                    updatePrecedenceOf(precedenceOverRoutes.get(routeIndex), simOp.getIndex(),TimeVesselUseOnOperation,startNodes,simOpRoutes,
                            precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselroutes,simultaneousOp,SailingTimes);
                    updateSimultaneousAfterRemoval(simOpRoutes.get(routeIndex), simOp.getRoute(), simOp.getIndex() - 1, o);

     */
}
