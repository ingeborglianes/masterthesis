public class BackUp {
    /*
    if (n==0 && vesselroutes.get(v).size()==1){
        int cost = SailingCostForVessel[v] * SailingTimes[v][EarliestStartingTimeForVessel[v]]
                [vesselroutes.get(v).get(n).getID()-1][o - 1];
        int earliestTemp=Math.max(vesselroutes.get(v).get(n).getEarliestTime()+
                SailingTimes[v][EarliestStartingTimeForVessel[v]]
                        [vesselroutes.get(v).get(n).getID()-1][o - 1]+TimeVesselUseOnOperation[v][vesselroutes.get(v).get(0).getID()-1-startNodes.length]
                [EarliestStartingTimeForVessel[v]],twIntervals[o-startNodes.length-1][0]);
        int latestTemp=twIntervals[o-startNodes.length-1][1];
        int precedenceOf=precedenceALNS[o-1-startNodes.length][1];
        if(precedenceOf!=0) {
            if (precedenceOperations.get(precedenceOf) == null) {
                break;
            }
            int earliestPO = precedenceOperations.get(precedenceOf).getValue().getEarliestTime();
            int latestPO = precedenceOperations.get(precedenceOf).getValue().getLatestTime();
            earliestTemp = Math.max(earliestTemp, earliestPO + TimeVesselUseOnOperation[v][precedenceOf - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]);
            latestTemp = Math.min(latestTemp, latestPO + TimeVesselUseOnOperation[v][precedenceOf - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]);
        }
        if(cost < costAdded && earliestTemp<=latestTemp ) {
            costAdded = cost;
            routeIndex = v;
            indexInRoute = 0;
            earliest = earliestTemp;
            latest = latestTemp;
        }
        //INSERTION IN THE BEGINNING OF THE ROUTE
        cost = SailingCostForVessel[v] * (SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1]
                + SailingTimes[v][EarliestStartingTimeForVessel[v]][o - 1][vesselroutes.get(v).get(0).getID() - 1]
                - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselroutes.get(v).get(0).getID() - 1]);
        earliestTemp = Math.max(EarliestStartingTimeForVessel[v] + SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1] + 1, twIntervals[o - startNodes.length - 1][0]);
        latestTemp = Math.min(vesselroutes.get(v).get(0).getLatestTime() - SailingTimes[v][EarliestStartingTimeForVessel[v]][o - 1]
                        [vesselroutes.get(v).get(0).getID() - 1]
                        - TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]
                , twIntervals[o - startNodes.length - 1][1]);
        if(precedenceOf!=0) {
            int earliestPO = precedenceOperations.get(precedenceOf).getValue().getEarliestTime();
            int latestPO = precedenceOperations.get(precedenceOf).getValue().getLatestTime();
            earliestTemp = Math.max(earliestTemp, earliestPO + TimeVesselUseOnOperation[v][precedenceOf - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]);
            latestTemp = Math.min(latestTemp, latestPO + TimeVesselUseOnOperation[v][precedenceOf - 1 - startNodes.length][EarliestStartingTimeForVessel[v]]);
        }
        if(cost < costAdded && earliestTemp<=latestTemp ) {
            costAdded = cost;
            routeIndex = v;
            indexInRoute = 0;
            earliest = earliestTemp;
            latest = latestTemp;
        }
    }

     */
}
