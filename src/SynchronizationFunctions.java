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

     */
}
