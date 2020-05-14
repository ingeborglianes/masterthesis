public class draftFirstTrySyncCopy {
    /*
        public static List<List<OperationInRoute>> retainOldSolution(List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks,
                                                                 Map<Integer, ConnectedValues> simultaneousOp, Map<Integer, PrecedenceValues> precedenceOfOperations,
                                                                 Map<Integer, PrecedenceValues> precedenceOverOperations){
        if(!simultaneousOp.isEmpty()){
            for(ConnectedValues op : simultaneousOp.values()){
                //System.out.println(op.getOperationObject().getID() +" with index " + op.getIndex() + " in route " + op.getRoute());
                //System.out.println(vesselroutes.get(op.getRoute()).size());
                if(vesselroutes.get(op.getRoute()).get(op.getIndex()).getID() == op.getOperationObject().getID()) {
                    int currentEarliest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getEarliestTime();
                    int currentLatest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getLatestTime();
                    vesselroutes.get(op.getRoute()).remove(op.getIndex());
                    op.getOperationObject().setLatestTime(currentLatest);
                    op.getOperationObject().setEarliestTime(currentEarliest);
                    vesselroutes.get(op.getRoute()).add(op.getIndex(), op.getOperationObject());
                }
            }
        }
        if(!precedenceOfOperations.isEmpty()){
            for(PrecedenceValues op : precedenceOfOperations.values()){
                //System.out.println(op.getOperationObject().getID());
                if(vesselroutes.get(op.getRoute()).get(op.getIndex()).getID() == op.getOperationObject().getID()) {
                    int currentEarliest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getEarliestTime();
                    int currentLatest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getLatestTime();
                    vesselroutes.get(op.getRoute()).remove(op.getIndex());
                    op.getOperationObject().setLatestTime(currentLatest);
                    op.getOperationObject().setEarliestTime(currentEarliest);
                    vesselroutes.get(op.getRoute()).add(op.getIndex(), op.getOperationObject());
                }
            }
        }
        if(!precedenceOverOperations.isEmpty()){
            for(PrecedenceValues op : precedenceOverOperations.values()){
                //System.out.println(op.getOperationObject().getID());
                if(vesselroutes.get(op.getRoute()).get(op.getIndex()).getID() == op.getOperationObject().getID()) {
                    int currentEarliest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getEarliestTime();
                    int currentLatest = vesselroutes.get(op.getRoute()).get(op.getIndex()).getLatestTime();
                    vesselroutes.get(op.getRoute()).remove(op.getIndex());
                    op.getOperationObject().setLatestTime(currentLatest);
                    op.getOperationObject().setEarliestTime(currentEarliest);
                    vesselroutes.get(op.getRoute()).add(op.getIndex(), op.getOperationObject());
                }
            }
        }
        return vesselroutes;
    }
         */
}
