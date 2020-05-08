public class draftFirstTrySyncCopy {
    /*
        int[][] simALNS = dg.getSimultaneousALNS();
        int[][] precALNS = dg.getPrecedenceALNS();
        int[][] bigTaskALNS = dg.getBigTasksALNS();
        int nStartnodes = dg.getStartNodes().length;
        for(int vessel = 0; vessel < vesselroutes.size(); vessel++){
            for(int task = 0; task < vesselroutes.get(vessel).size(); task++) {
                int taskID = vesselroutes.get(vessel).get(task).getID();
                if (simALNS[taskID - nStartnodes - 1][0] != 0 || simALNS[taskID - nStartnodes - 1][1] != 0) {
                    if (simultaneousOp.get(taskID) != null) {
                        simOpRoutes.get(simultaneousOp.get(taskID).getRoute()).remove(taskID);
                        simultaneousOp.get(taskID).setIndex(task);
                        simultaneousOp.get(taskID).setRoute(vessel);
                        simultaneousOp.get(taskID).getOperationObject().setEarliestTime(vesselroutes.get(vessel).get(task).getEarliestTime());
                        simultaneousOp.get(taskID).getOperationObject().setLatestTime(vesselroutes.get(vessel).get(task).getLatestTime());
                        simOpRoutes.get(simultaneousOp.get(taskID).getRoute()).put(taskID,simultaneousOp.get(taskID));
                    } else {
                        int simOp = Math.max(simALNS[taskID - 1 - nStartnodes][0], simALNS[taskID - 1 - nStartnodes][1]);
                        if (simultaneousOp.get(simOp) == null) {
                            ConnectedValues sValue = new ConnectedValues(vesselroutes.get(vessel).get(task), null, simOp, task, vessel, -1);
                            simultaneousOp.put(taskID, sValue);
                            simOpRoutes.get(vessel).put(taskID, sValue);
                        } else {
                            ConnectedValues sValues = simultaneousOp.get(simOp);
                            if (sValues.getConnectedOperationObject() == null) {
                                ConnectedValues cValuesReplace = new ConnectedValues(sValues.getOperationObject(), vesselroutes.get(vessel).get(task), sValues.getConnectedOperationID(),
                                        sValues.getIndex(), sValues.getRoute(), vessel);
                                simultaneousOp.replace(simOp, sValues, cValuesReplace);
                                simOpRoutes.get(sValues.getRoute()).replace(simOp, sValues, cValuesReplace);
                            } else {
                                ConnectedValues cValuesPut1 = new ConnectedValues(sValues.getOperationObject(), vesselroutes.get(vessel).get(task), sValues.getConnectedOperationID(),
                                        sValues.getIndex(), sValues.getRoute(), vessel);
                                simultaneousOp.put(simOp, cValuesPut1);
                                simOpRoutes.get(sValues.getRoute()).put(simOp, cValuesPut1);
                            }
                            ConnectedValues sim2 = new ConnectedValues(vesselroutes.get(vessel).get(task), sValues.getOperationObject(), simOp, task, vessel, sValues.getRoute());
                            simultaneousOp.put(taskID, sim2);
                            simOpRoutes.get(vessel).put(taskID, sim2);
                        }
                    }
                }
                if (precALNS[taskID - nStartnodes - 1][0] != 0) {
                    if (precedenceOverOperations.get(taskID) != null) {
                        precedenceOverRoutes.get(precedenceOverOperations.get(taskID).getRoute()).remove(taskID);
                        precedenceOverOperations.get(taskID).setIndex(task);
                        precedenceOverOperations.get(taskID).setRoute(vessel);
                        precedenceOverOperations.get(taskID).getOperationObject().setEarliestTime(vesselroutes.get(vessel).get(task).getEarliestTime());
                        precedenceOverOperations.get(taskID).getOperationObject().setLatestTime(vesselroutes.get(vessel).get(task).getLatestTime());
                        precedenceOverRoutes.get(precedenceOverOperations.get(taskID).getRoute()).put(taskID,precedenceOverOperations.get(taskID));
                    } else {
                        int precOver = precALNS[taskID - nStartnodes - 1][0];
                        if(precedenceOfOperations.get(precOver)==null) {
                            PrecedenceValues pValues = new PrecedenceValues(vesselroutes.get(vessel).get(task), null, precOver, task, vessel, -1);
                            precedenceOverOperations.put(taskID, pValues);
                            precedenceOverRoutes.get(vessel).put(taskID,pValues);
                        }
                        if(precedenceOfOperations.get(precOver) != null) {
                            PrecedenceValues pValues = precedenceOfOperations.get(precOver);
                            PrecedenceValues pValuesReplace = new PrecedenceValues(pValues.getOperationObject(),
                                    vesselroutes.get(vessel).get(task), pValues.getConnectedOperationID(), pValues.getIndex(), pValues.getRoute(), vessel);
                            PrecedenceValues pValuesPut = new PrecedenceValues(vesselroutes.get(vessel).get(task), pValues.getOperationObject(), precOver, task, vessel, pValues.getRoute());
                            precedenceOfOperations.put(precOver, pValuesReplace);
                            precedenceOverOperations.put(taskID, pValuesPut);
                            precedenceOfRoutes.get(pValues.getRoute()).put(precOver, pValuesReplace);
                            precedenceOverRoutes.get(vessel).put(taskID, pValuesPut);

                        }
                    }
                }
                if (precALNS[taskID - nStartnodes - 1][1] != 0) {
                    if (precedenceOfOperations.get(taskID) != null) {
                        precedenceOfRoutes.get(precedenceOfOperations.get(taskID).getRoute()).remove(taskID);
                        precedenceOfOperations.get(taskID).setIndex(task);
                        precedenceOfOperations.get(taskID).setRoute(vessel);
                        precedenceOfOperations.get(taskID).getOperationObject().setEarliestTime(vesselroutes.get(vessel).get(task).getEarliestTime());
                        precedenceOfOperations.get(taskID).getOperationObject().setLatestTime(vesselroutes.get(vessel).get(task).getLatestTime());
                        precedenceOfRoutes.get(precedenceOfOperations.get(taskID).getRoute()).put(taskID,precedenceOfOperations.get(taskID));
                    } else {
                        int precOf = precALNS[taskID - nStartnodes - 1][1];
                        if(precedenceOverOperations.get(precOf) != null) {
                            PrecedenceValues pValues = precedenceOverOperations.get(precOf);
                            PrecedenceValues pValuesReplace = new PrecedenceValues(pValues.getOperationObject(),
                                    vesselroutes.get(vessel).get(task), pValues.getConnectedOperationID(), pValues.getIndex(), pValues.getRoute(), vessel);
                            PrecedenceValues pValuesPut = new PrecedenceValues(vesselroutes.get(vessel).get(task), pValues.getOperationObject(), precOf, task, vessel, pValues.getRoute());
                            precedenceOverOperations.put(precOf, pValuesReplace);
                            precedenceOfOperations.put(taskID, pValuesPut);
                            precedenceOverRoutes.get(pValues.getRoute()).put(precOf, pValuesReplace);
                            precedenceOfRoutes.get(vessel).put(taskID, pValuesPut);
                        }else if(precedenceOverOperations.get(precOf)==null){
                            PrecedenceValues pValues = new PrecedenceValues(vesselroutes.get(vessel).get(task), null, precOf, task, vessel, -1);
                            precedenceOfOperations.put(taskID, pValues);
                            precedenceOfRoutes.get(vessel).put(taskID, pValues);
                        }
                    }
                }
                if (bigTaskALNS[taskID-nStartnodes-1] != null) {
                    if (bigTaskALNS[taskID - nStartnodes - 1][0] == taskID) {
                        consolidatedOperations.get(taskID).setConsolidatedRoute(vessel);
                        consolidatedOperations.get(taskID).setConnectedRoute1(0);
                        consolidatedOperations.get(taskID).setConnectedRoute2(0);
                    } else if (bigTaskALNS[taskID - nStartnodes - 1][1] == taskID) {
                        consolidatedOperations.get(bigTaskALNS[taskID - nStartnodes - 1][0]).setConnectedRoute1(vessel);
                        consolidatedOperations.get(bigTaskALNS[taskID - nStartnodes - 1][0]).setConsolidatedRoute(0);
                    } else if (bigTaskALNS[taskID - nStartnodes - 1][2] == taskID) {
                        consolidatedOperations.get(bigTaskALNS[taskID - nStartnodes - 1][0]).setConnectedRoute2(vessel);
                        consolidatedOperations.get(bigTaskALNS[taskID - nStartnodes - 1][0]).setConsolidatedRoute(0);
                    }
                }
            }
        }
        for(OperationInRoute op : unroutedTasks) {
            if (simultaneousOp.get(op.getID()) != null) {
                simOpRoutes.get(simultaneousOp.get(op.getID()).getRoute()).remove(op.getID());
                simultaneousOp.remove(op.getID());
            }
            if (precedenceOfOperations.get(op.getID()) != null) {
                precedenceOfRoutes.get(precedenceOfOperations.get(op.getID()).getRoute()).remove(op.getID());
                precedenceOfOperations.remove(op.getID());
            }
            if (precedenceOverOperations.get(op.getID()) != null) {
                precedenceOverRoutes.get(precedenceOverOperations.get(op.getID()).getRoute()).remove(op.getID());
                precedenceOverOperations.remove(op.getID());
            }
        }
         */
}
