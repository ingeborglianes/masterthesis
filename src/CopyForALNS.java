public class CopyForALNS {
    /*
    public void runLocalSearchFullEnumeration(){
        Boolean continueLocal=true;
        LargeNeighboorhoodSearchRemoval LNSR = new LargeNeighboorhoodSearchRemoval(precedenceOverOperations,precedenceOfOperations,
                simultaneousOp,simOpRoutes,precedenceOfRoutes,precedenceOverRoutes,
                consolidatedOperations,unroutedTasks,vesselroutes, dg.getTwIntervals(),
                dg.getPrecedenceALNS(), dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),numberOfRemoval,randomSeed,dg.getDistOperationsInInstance(),
                relatednessWeightDistance,relatednessWeightDuration,relatednessWeightTimewindows,relatednessWeightPrecedenceOver,
                relatednessWeightPrecedenceOf,relatednessWeightSimultaneous,dg.getOperationGainGurobi(),vessels);

        //for run removal, insert method, alternatives: worst, synchronized, route, related, random, worst_sailing
        String removalMethod = chooseRemovalMethod();
        LNSR.runLNSRemoval(removalMethod);

        printLNSInsertSolution(vessels, currentRouteSailingCost, currentRouteOperationGain, vesselroutes, dg.getStartNodes(), dg.getSailingTimes(),
                dg.getTimeVesselUseOnOperation(), unroutedTasks, precedenceOverOperations, consolidatedOperations,
                precedenceOfOperations, simultaneousOp, simOpRoutes);
        while(continueLocal) {
            System.out.println("Before locAL SEARCH");
            for (int i = 0; i < vesselroutes.size(); i++) {
                System.out.println("VESSELINDEX " + i);
                if (vesselroutes.get(i) != null) {
                    for (int o = 0; o < vesselroutes.get(i).size(); o++) {
                        System.out.println("Operation number: " + vesselroutes.get(i).get(o).getID() + " Earliest start time: " +
                                vesselroutes.get(i).get(o).getEarliestTime() + " Latest Start time: " + vesselroutes.get(i).get(o).getLatestTime());
                    }
                }
            }
            System.out.println("Unrouted ");
            for (OperationInRoute un : unroutedTasks) {
                System.out.println(un.getID());
            }

            System.out.println("run 1RL");
            LS_operators LSO = new LS_operators(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                    dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                    dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(), vesselroutes, unroutedTasks,
                    precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                    simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, dg.getOperationGainGurobi());
            LSO.runNormalLSO("1RL");

            System.out.println("run 2RL");
            evaluateSolutionLocal(LSO.getRouteOperationGain(), LSO.getRouteSailingCost(), LSO.getVesselroutes(), LSO.getUnroutedTasks());
            LSO.runNormalLSO("2RL");

            System.out.println("run 1EX");
            evaluateSolutionLocal(LSO.getRouteOperationGain(), LSO.getRouteSailingCost(), LSO.getVesselroutes(), LSO.getUnroutedTasks());
            LSO.runNormalLSO("1EX");

            System.out.println("run 2EX");
            evaluateSolutionLocal(LSO.getRouteOperationGain(), LSO.getRouteSailingCost(), LSO.getVesselroutes(), LSO.getUnroutedTasks());
            LSO.runNormalLSO("2EX");

            evaluateSolutionLocal(LSO.getRouteOperationGain(), LSO.getRouteSailingCost(), LSO.getVesselroutes(), LSO.getUnroutedTasks());
            System.out.println("run insert normal");
            LSO.runNormalLSO("insertNormal");
            evaluateSolutionLocal(LSO.getRouteOperationGain(), LSO.getRouteSailingCost(), LSO.getVesselroutes(), LSO.getUnroutedTasks());

            System.out.println("run relocate LSOs");
            RelocateInsert RI = new RelocateInsert(dg.getOperationsForVessel(), vessels, dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(),
                    dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(), dg.getTwIntervals(), currentRouteSailingCost, currentRouteOperationGain,
                    dg.getStartNodes(), dg.getSimultaneousALNS(), dg.getPrecedenceALNS(), dg.getBigTasksALNS(), dg.getOperationGain(),
                    unroutedTasks, vesselroutes, precedenceOverOperations, precedenceOfOperations, simultaneousOp,
                    simOpRoutes, precedenceOfRoutes, precedenceOverRoutes, consolidatedOperations, dg.getOperationGainGurobi());
            RI.runRelocateLSO("relocate");
            evaluateSolutionLocal(RI.getRouteOperationGain(), RI.getRouteSailingCost(), RI.getVesselRoutes(), RI.getUnroutedTasks());

            System.out.println("run precedence");
            RI.runRelocateLSO("precedence");
            evaluateSolutionLocal(RI.getRouteOperationGain(), RI.getRouteSailingCost(), RI.getVesselRoutes(), RI.getUnroutedTasks());

            System.out.println("run simultaneous");
            RI.runRelocateLSO("simultaneous");
            evaluateSolutionLocal(RI.getRouteOperationGain(), RI.getRouteSailingCost(), RI.getVesselRoutes(), RI.getUnroutedTasks());

            SwitchConsolidated sc = new SwitchConsolidated(precedenceOverOperations, precedenceOfOperations,
                    simultaneousOp, simOpRoutes, precedenceOfRoutes, precedenceOverRoutes,
                    consolidatedOperations, unroutedTasks, vesselroutes, dg.getTwIntervals(),
                    dg.getPrecedenceALNS(), dg.getSimultaneousALNS(), dg.getStartNodes(), dg.getSailingTimes(),
                    dg.getTimeVesselUseOnOperation(), dg.getSailingCostForVessel(), dg.getEarliestStartingTimeForVessel(),
                    dg.getOperationGain(), dg.getBigTasksALNS(), dg.getOperationsForVessel(), dg.getOperationGainGurobi(), vessels);
            System.out.println("run consolidated");
            sc.runSwitchConsolidated();
            evaluateSolutionLocal(sc.getRouteOperationGain(), sc.getRouteSailingCost(), sc.getVesselRoutes(), sc.getUnroutedTasks());
            continueLocal=evaluateSolutionLocal(sc.getRouteOperationGain(), sc.getRouteSailingCost(), sc.getVesselRoutes(), sc.getUnroutedTasks());
        }
    }

    public Boolean evaluateSolutionLocal(int[] routeOperationGain, int[] routeSailingCost, List<List<OperationInRoute>> vesselroutes, List<OperationInRoute> unroutedTasks){
        int bestObj= IntStream.of(bestRouteOperationGain).sum()-IntStream.of(bestRouteSailingCost).sum();
        int currentObj= IntStream.of(currentRouteOperationGain).sum()-IntStream.of(currentRouteSailingCost).sum();
        int newObj = IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum();
        if(newObj>bestObj){
            //System.out.println("New best global solution "+newObj);
            bestRouteSailingCost = routeSailingCost;
            bestRouteOperationGain = routeOperationGain;
            bestRoutes = copyVesselRoutes(vesselroutes);
            bestUnrouted = copyUnrouted(unroutedTasks);
            currentRouteSailingCost = routeSailingCost;
            currentRouteOperationGain = routeOperationGain;
            currentRoutes = copyVesselRoutes(vesselroutes);
            currentUnrouted = copyUnrouted(unroutedTasks);
            System.out.println("Do one more iteration local search");
            return true;
        }

        else{
        retainCurrentBestSolution("current");
        return false;
    }
}

public static void main(String[] args) throws FileNotFoundException {
        long startTime = System.nanoTime();
        ALNS alns = new ALNS();
        int constructionObjective=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();
        List<Integer> unroutedList=new ArrayList<>();
        for (OperationInRoute ur:alns.bestUnrouted){
            unroutedList.add(ur.getID());
        }

        //alns.runDestroyRepair();
        alns.retainCurrentBestSolution("best");
        int afterLarge=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();
        List<Integer> unroutedList2=new ArrayList<>();
        for (OperationInRoute ur:alns.bestUnrouted){
            unroutedList2.add(ur.getID());
        }
        alns.runLocalSearchFullEnumeration();
        long endTime   = System.nanoTime();
        int localObjective=IntStream.of(alns.bestRouteOperationGain).sum()-IntStream.of(alns.bestRouteSailingCost).sum();

        alns.printLNSInsertSolution(alns.vessels, alns.bestRouteSailingCost, alns.bestRouteOperationGain, alns.bestRoutes,
                alns.dg.getStartNodes(), alns.dg.getSailingTimes(), alns.dg.getTimeVesselUseOnOperation(), alns.unroutedTasks,
                alns.precedenceOverOperations, alns.consolidatedOperations,
                alns.precedenceOfOperations, alns.simultaneousOp, alns.simOpRoutes);
        System.out.println("Construction Objective "+constructionObjective);
        System.out.println("afterALNS "+afterLarge);
        System.out.println("Local Objective "+localObjective);
        long totalTime = endTime - startTime;
        System.out.println("Time "+totalTime/1000000000);
        //System.out.println(alns.generator.doubles());

        System.out.println("Unrouted construction");
        for (Integer urInt:unroutedList){
            System.out.println(urInt);
        }

        System.out.println("Unrouted after large");
        for (Integer urInt:unroutedList2){
            System.out.println(urInt);
        }

        System.out.println("Unrouted after local search");
        for (OperationInRoute ur:alns.bestUnrouted){
            System.out.println(ur.getID());
        }

    }
     */
}
