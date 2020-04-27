import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import java.util.stream.IntStream;

public class LargeNeighboorhoodSearchInsertCopy {
    private Map<Integer, PrecedenceValues> precedenceOverOperations;
    private Map<Integer, PrecedenceValues> precedenceOfOperations;
    //map for operations that are connected as simultaneous operations. ID= operation number. Value= Simultaneous value.
    private Map<Integer, ConnectedValues> simultaneousOp;
    private List<Map<Integer, ConnectedValues>> simOpRoutes;
    private List<Map<Integer, PrecedenceValues>> precedenceOfRoutes;
    private List<Map<Integer, PrecedenceValues>> precedenceOverRoutes;
    private Map<Integer, ConsolidatedValues> consolidatedOperations;
    private List<OperationInRoute> unroutedTasks;
    private List<List<OperationInRoute>> vesselRoutes;
    private int[][] twIntervals;
    private int[][] precedenceALNS;
    private int[][] simALNS;
    private int[][] bigTasksALNS;
    private int[] startNodes;
    private int[][][][] SailingTimes;
    private int[][][] TimeVesselUseOnOperation;
    private int[] SailingCostForVessel;
    private int[] EarliestStartingTimeForVessel;
    private int[][][] operationGain;
    private int[] routeSailingCost;
    private int[] routeOperationGain;
    private int objValue;
    private int nVessels;
    private int nTimePeriods;
    private int [][] OperationsForVessel;
    Random generator;
    ArrayList<Integer> removedOperations;
    Map<Integer,List<InsertionValues>> allFeasibleInsertions = new HashMap<>();

    public LargeNeighboorhoodSearchInsertCopy(Map<Integer, PrecedenceValues> precedenceOverOperations, Map<Integer, PrecedenceValues> precedenceOfOperations,
                                          Map<Integer, ConnectedValues> simultaneousOp, List<Map<Integer, ConnectedValues>> simOpRoutes,
                                          List<Map<Integer, PrecedenceValues>> precedenceOfRoutes, List<Map<Integer, PrecedenceValues>> precedenceOverRoutes,
                                          Map<Integer, ConsolidatedValues> consolidatedOperations, List<OperationInRoute> unroutedTasks,
                                          List<List<OperationInRoute>> vesselRoutes,ArrayList<Integer> removedOperations, int[][] twIntervals,
                                          int[][] precedenceALNS, int[][] simALNS, int[] startNodes, int[][][][] SailingTimes,
                                          int[][][] TimeVesselUseOnOperation, int[] SailingCostForVessel, int[] EarliestStartingTimeForVessel,
                                          int[][][] operationGain, int[][] bigTasksALNS, int[][] OperationsForVessel, int randomSeed) {
        this.precedenceOverOperations = precedenceOverOperations;
        this.precedenceOfOperations = precedenceOfOperations;
        this.simultaneousOp = simultaneousOp;
        this.simOpRoutes = simOpRoutes;
        this.precedenceOfRoutes = precedenceOfRoutes;
        this.precedenceOverRoutes = precedenceOverRoutes;
        this.unroutedTasks = unroutedTasks;
        this.vesselRoutes = vesselRoutes;
        this.consolidatedOperations = consolidatedOperations;
        this.twIntervals = twIntervals;
        this.precedenceALNS = precedenceALNS;
        this.simALNS = simALNS;
        this.startNodes = startNodes;
        this.SailingTimes = SailingTimes;
        this.TimeVesselUseOnOperation = TimeVesselUseOnOperation;
        this.SailingCostForVessel = SailingCostForVessel;
        this.EarliestStartingTimeForVessel = EarliestStartingTimeForVessel;
        this.operationGain = operationGain;
        this.routeSailingCost = new int[vesselRoutes.size()];
        this.routeOperationGain = new int[vesselRoutes.size()];
        this.objValue = 0;
        this.bigTasksALNS = bigTasksALNS;
        this.generator = new Random(randomSeed);
        this.nVessels=vesselRoutes.size();
        this.nTimePeriods=TimeVesselUseOnOperation[0][0].length;
        this.OperationsForVessel=OperationsForVessel;
        this.removedOperations=removedOperations;
        Collections.sort(removedOperations);
        unroutedTasks.sort(Comparator.comparing(OperationInRoute::getID));
        for (int i=0;i<this.unroutedTasks.size();i++){
            System.out.println(unroutedTasks.get(i).getID());
        }
    }

    public void switchConsolidated(){
        //1. remove consolidated or small tasks
        //2. find best position for both alternatives, for the small tasks this will be the operation gain and sailing
        //cost of both tasks divided by two
        //3. insert the best alternative
        for (Map.Entry<Integer, ConsolidatedValues> entry : consolidatedOperations.entrySet()) {
            int bigTask = entry.getKey();
            int small1= bigTasksALNS[bigTask-1-startNodes.length][1];
            int small2= bigTasksALNS[bigTask-1-startNodes.length][2];
            ConsolidatedValues conVals = entry.getValue();
            if (!(!conVals.getConsolidated() && !conVals.getSmallTasks())){
                if(conVals.getConsolidated()){

                }
            }

        }
    }

    public int calculateInsertionValuesRegretInsertion(){
        int prevID=-1;
        int prevRegretValue=-100000;
        int currentID=-1;
        int currentRegretValue=-100000;
        allFeasibleInsertions = new HashMap<>();
        for(OperationInRoute our : unroutedTasks){
            System.out.println("OPERATION "+our.getID());
            int ourID=our.getID();
            if(simALNS[ourID-startNodes.length-1][1]!=0){
                int simA=simALNS[ourID-startNodes.length-1][1];
                int presOfOp=precedenceALNS[ourID-1-startNodes.length][1];
                for (int i=0;i<allFeasibleInsertions.get(simA).size();i++){
                    InsertionValues option =allFeasibleInsertions.get(simA).get(i);
                    if(option.getBenenefitIncrease()==-100000){
                        if(allFeasibleInsertions.get(ourID)==null){
                            allFeasibleInsertions.put(ourID, new ArrayList<>() {{
                                add(new InsertionValues(-100000, -1, -1, -1, -1));
                            }});
                        }
                        else{
                            allFeasibleInsertions.get(ourID).
                                    add(new InsertionValues(-100000, -1, -1, -1, -1));
                        }
                    }
                    else{
                        PrecedenceValues pv = precedenceOverOperations.get(presOfOp);
                        int precedenceIndex=-1;
                        int precedenceRoute=-1;
                        int earliestP=-1;
                        if(pv!=null){
                            precedenceIndex=pv.getIndex();
                            precedenceRoute=pv.getRoute();
                            earliestP=pv.getOperationObject().getEarliestTime();
                        }
                        List<InsertionValues> precedenceIVList=allFeasibleInsertions.get(presOfOp);
                        if(precedenceIVList!=null){
                            precedenceIndex=precedenceIVList.get(i).getIndexInRoute();
                            precedenceRoute=precedenceIVList.get(i).getRouteIndex();
                            earliestP=precedenceIVList.get(i).getEarliest();
                        }
                        findInsertionCosts(our,option.getEarliest(),option.getLatest(),earliestP,precedenceRoute,option.getRouteIndex(),precedenceIndex,option.getIndexInRoute());
                        int size=allFeasibleInsertions.get(ourID).size();
                        InsertionValues ourValues=allFeasibleInsertions.get(ourID).get(size-1);
                        int ourBenefitIncrease=ourValues.getBenenefitIncrease();
                        if(ourBenefitIncrease==-100000) {
                            option.setBenenefitIncrease(-100000);
                            if(currentID==simA){
                                currentRegretValue=-100000;
                            }
                            if(prevID==simA){
                                prevRegretValue=-100000;
                            }
                        }
                        else{
                            if(allFeasibleInsertions.get(presOfOp)==null) {
                                int newBenefitIncrease = (option.getBenenefitIncrease() + ourBenefitIncrease) / 2;
                                ourValues.setBenenefitIncrease(newBenefitIncrease);
                                option.setBenenefitIncrease(newBenefitIncrease);
                                if (i > 0) {
                                    if (newBenefitIncrease >= allFeasibleInsertions.get(simA).get(0).getBenenefitIncrease()) {
                                        allFeasibleInsertions.get(simA).remove(i);
                                        allFeasibleInsertions.get(simA).add(0, option);
                                        allFeasibleInsertions.get(ourID).remove(size - 1);
                                        allFeasibleInsertions.get(ourID).add(0, option);
                                    } else if (!(newBenefitIncrease < allFeasibleInsertions.get(simA).get(allFeasibleInsertions.size() - 1).getBenenefitIncrease())) {
                                        for (int s = 1; s < allFeasibleInsertions.get(simA).size(); s++) {
                                            if (newBenefitIncrease < allFeasibleInsertions.get(simA).get(s - 1).getBenenefitIncrease() &&
                                                    newBenefitIncrease >= allFeasibleInsertions.get(simA).get(s).getBenenefitIncrease()) {
                                                allFeasibleInsertions.get(simA).remove(i);
                                                allFeasibleInsertions.get(simA).add(s, option);
                                                allFeasibleInsertions.get(ourID).remove(size - 1);
                                                allFeasibleInsertions.get(ourID).add(s, option);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if(allFeasibleInsertions.get(presOfOp)!=null) {
                    int regretValueTemp;
                    if(allFeasibleInsertions.get(presOfOp).size()==1 && allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease()!=-100000){
                        regretValueTemp=allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease();
                    }
                    else if(allFeasibleInsertions.get(presOfOp).size()==1 && allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease()==-100000){
                        regretValueTemp=-100000;
                    }
                    else{
                        int ourValue1=allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease();
                        int ourValue2=allFeasibleInsertions.get(presOfOp).get(1).getBenenefitIncrease();
                        regretValueTemp=ourValue1-ourValue2;
                        if(allFeasibleInsertions.get(presOfOp).get(1).getBenenefitIncrease()==-100000){
                            regretValueTemp=allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease();
                        }
                    }
                    if (regretValueTemp > currentRegretValue && !(precedenceALNS[presOfOp-1-startNodes.length][1]!=0 &&
                            allFeasibleInsertions.get(precedenceALNS[presOfOp-1-startNodes.length][1])!=null)) {
                        prevRegretValue = currentRegretValue;
                        currentRegretValue = regretValueTemp;
                        prevID = currentID;
                        currentID = presOfOp;
                    }
                }
                else {
                    int regretValueTemp;
                    if (allFeasibleInsertions.get(simA).size() == 1 && allFeasibleInsertions.get(simA).get(0).getBenenefitIncrease() != -100000) {
                        regretValueTemp = allFeasibleInsertions.get(simA).get(0).getBenenefitIncrease();
                    } else if (allFeasibleInsertions.get(simA).size() == 1 && allFeasibleInsertions.get(simA).get(0).getBenenefitIncrease() == -100000) {
                        regretValueTemp = -100000;
                    } else {
                        int ourValue1 = allFeasibleInsertions.get(simA).get(0).getBenenefitIncrease();
                        int ourValue2 = allFeasibleInsertions.get(simA).get(1).getBenenefitIncrease();
                        regretValueTemp = ourValue1 - ourValue2;
                        if (allFeasibleInsertions.get(simA).get(1).getBenenefitIncrease() == -100000) {
                            regretValueTemp = allFeasibleInsertions.get(simA).get(0).getBenenefitIncrease();
                        }
                    }
                    if (simA == currentID && regretValueTemp < currentRegretValue && prevRegretValue > regretValueTemp) {
                        if(prevID==simA){
                            currentRegretValue=regretValueTemp;
                        }
                        else{
                            currentRegretValue = prevRegretValue;
                            currentID = prevID;
                        }
                    } else if (simA == currentID && regretValueTemp < currentRegretValue && prevRegretValue < regretValueTemp) {
                        currentRegretValue = regretValueTemp;
                    } else if (regretValueTemp > currentRegretValue) {
                        prevRegretValue = currentRegretValue;
                        currentRegretValue = regretValueTemp;
                        prevID = currentID;
                        currentID = simA;
                    }
                }
            }
            else if (precedenceALNS[ourID-startNodes.length-1][1]!=0) {
                int presOfOp=precedenceALNS[ourID-startNodes.length-1][1];
                if(allFeasibleInsertions.get(presOfOp)!=null) {
                    if(allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease()!=-100000) {
                        ArrayList<Integer> removeIndexes = new ArrayList<>();
                        for (int i = 0; i < allFeasibleInsertions.get(presOfOp).size(); i++) {
                            InsertionValues option = allFeasibleInsertions.get(presOfOp).get(i);
                            findInsertionCosts(our, -1, -1, option.getEarliest(), option.getRouteIndex(), -1, option.getIndexInRoute(), -1);
                        }
                        int regretValueTemp;
                        if (allFeasibleInsertions.get(presOfOp).size() == 1 && allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease() != -100000) {
                            regretValueTemp = allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease();
                        } else if (allFeasibleInsertions.get(presOfOp).size() == 1 && allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease() == -100000) {
                            regretValueTemp = -100000;
                        } else {
                            int ourValue1 = allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease();
                            int ourValue2 = allFeasibleInsertions.get(presOfOp).get(1).getBenenefitIncrease();
                            regretValueTemp = ourValue1 - ourValue2;
                            if (allFeasibleInsertions.get(presOfOp).get(1).getBenenefitIncrease() == -100000) {
                                regretValueTemp = allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease();
                            }
                        }
                        if (regretValueTemp > currentRegretValue && !(precedenceALNS[presOfOp - 1 - startNodes.length][1] != 0 &&
                                allFeasibleInsertions.get(precedenceALNS[presOfOp - 1 - startNodes.length][1]) != null)) {
                            prevRegretValue = currentRegretValue;
                            currentRegretValue = regretValueTemp;
                            prevID = currentID;
                            currentID = presOfOp;
                        }
                    }
                    else{
                        allFeasibleInsertions.put(ourID, new ArrayList<>() {{
                            add(new InsertionValues(-100000, -1, -1, -1, -1));
                        }});
                    }
                }
                else{
                    PrecedenceValues pv = precedenceOverOperations.get(presOfOp);
                    findInsertionCosts(our,-1,-1,pv.getOperationObject().getEarliestTime(),pv.getRoute(),-1,pv.getIndex(),-1);
                    int regretValueTemp;
                    if(allFeasibleInsertions.get(ourID).size()==1 && allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease()!=-100000){
                        regretValueTemp=allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease();
                    }
                    else if(allFeasibleInsertions.get(ourID).size()==1 && allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease()==-100000){
                        regretValueTemp=-100000;
                    }
                    else{
                        int ourValue1=allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease();
                        int ourValue2=allFeasibleInsertions.get(ourID).get(1).getBenenefitIncrease();
                        regretValueTemp=ourValue1-ourValue2;
                        if(allFeasibleInsertions.get(ourID).get(1).getBenenefitIncrease()==-100000){
                            regretValueTemp=allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease();
                        }
                    }
                    if(regretValueTemp>currentRegretValue){
                        prevRegretValue=currentRegretValue;
                        prevID=currentID;
                        currentRegretValue=regretValueTemp;
                        currentID=ourID;
                    }
                }
            }
            else{
                findInsertionCosts(our,-1,-1,-1,-1,-1,-1,-1);
                int regretValueTemp;
                if(allFeasibleInsertions.get(ourID).size()==1 && allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease()!=-100000){
                    regretValueTemp=allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease();
                }
                else if(allFeasibleInsertions.get(ourID).size()==1 && allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease()==-100000){
                    regretValueTemp=-100000;
                }
                else{
                    int ourValue1=allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease();
                    int ourValue2=allFeasibleInsertions.get(ourID).get(1).getBenenefitIncrease();
                    regretValueTemp=ourValue1-ourValue2;
                    if(allFeasibleInsertions.get(ourID).get(1).getBenenefitIncrease()==-100000){
                        regretValueTemp=allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease();
                    }
                }
                if(regretValueTemp>currentRegretValue){
                    prevRegretValue=currentRegretValue;
                    prevID=currentID;
                    currentRegretValue=regretValueTemp;
                    currentID=ourID;
                }
            }
            System.out.println("ONE ITERATION calculate insertion values:");
            System.out.println("current best ID "+currentID);
            System.out.println("current best value "+currentRegretValue);
            if(currentID!=-1){
                System.out.println("Current route: "+allFeasibleInsertions.get(currentID).get(0).getRouteIndex());
                System.out.println("Current index: "+allFeasibleInsertions.get(currentID).get(0).getIndexInRoute());
            }
            System.out.println("previous best ID "+prevID);
            System.out.println("previous best value "+prevRegretValue);
        }
        if(currentRegretValue==-100000){
            currentID=-1;
        }
        return currentID;
    }

    public int calculateInsertionValuesBestInsertion(){
        int prevID=-1;
        int prevBestValue=-100000;
        int currentID=-1;
        int currentBestValue=-100000;
        allFeasibleInsertions = new HashMap<>();
        for(OperationInRoute our : unroutedTasks){
            System.out.println("OPERATION "+our.getID());
            int ourID=our.getID();
            if(simALNS[ourID-startNodes.length-1][1]!=0){
                if(!(currentID==simALNS[ourID-startNodes.length-1][1] || currentID==precedenceALNS[ourID-startNodes.length-1][1])){
                    continue;
                }
                int simA=simALNS[ourID-startNodes.length-1][1];
                int presOfOp=precedenceALNS[ourID-1-startNodes.length][1];
                for (int i=0;i<allFeasibleInsertions.get(simA).size();i++){
                    InsertionValues option =allFeasibleInsertions.get(simA).get(i);
                    if(option.getBenenefitIncrease()==-100000){
                        if(allFeasibleInsertions.get(ourID)==null){
                            allFeasibleInsertions.put(ourID, new ArrayList<>() {{
                                add(new InsertionValues(-100000, -1, -1, -1, -1));
                            }});
                        }
                        else{
                            allFeasibleInsertions.get(ourID).
                                    add(new InsertionValues(-100000, -1, -1, -1, -1));
                        }
                    }
                    else{
                        PrecedenceValues pv = precedenceOverOperations.get(presOfOp);
                        int precedenceIndex=-1;
                        int precedenceRoute=-1;
                        int earliestP=-1;
                        if(pv!=null){
                            precedenceIndex=pv.getIndex();
                            precedenceRoute=pv.getRoute();
                            earliestP=pv.getOperationObject().getEarliestTime();
                        }
                        List<InsertionValues> precedenceIVList=allFeasibleInsertions.get(presOfOp);
                        if(precedenceIVList!=null){
                            precedenceIndex=precedenceIVList.get(i).getIndexInRoute();
                            precedenceRoute=precedenceIVList.get(i).getRouteIndex();
                            earliestP=precedenceIVList.get(i).getEarliest();
                        }
                        findInsertionCosts(our,option.getEarliest(),option.getLatest(),earliestP,precedenceRoute,option.getRouteIndex(),precedenceIndex,option.getIndexInRoute());
                        int size=allFeasibleInsertions.get(ourID).size();
                        InsertionValues ourValues=allFeasibleInsertions.get(ourID).get(size-1);
                        int ourBenefitIncrease=ourValues.getBenenefitIncrease();
                        if(ourBenefitIncrease==-100000) {
                            option.setBenenefitIncrease(-100000);
                            if(currentID==simA){
                                currentBestValue=-100000;
                            }
                            if(prevID==simA){
                                prevBestValue=-100000;
                            }
                        }
                        else {
                            if (allFeasibleInsertions.get(presOfOp) == null) {
                                int newBenefitIncrease = (option.getBenenefitIncrease() + ourBenefitIncrease) / 2;
                                ourValues.setBenenefitIncrease(newBenefitIncrease);
                                option.setBenenefitIncrease(newBenefitIncrease);
                                if (i > 0) {
                                    if (newBenefitIncrease >= allFeasibleInsertions.get(simA).get(0).getBenenefitIncrease()) {
                                        allFeasibleInsertions.get(simA).remove(i);
                                        allFeasibleInsertions.get(simA).add(0, option);
                                        allFeasibleInsertions.get(ourID).remove(size - 1);
                                        allFeasibleInsertions.get(ourID).add(0, option);
                                    } else if (!(newBenefitIncrease < allFeasibleInsertions.get(simA).get(allFeasibleInsertions.size() - 1).getBenenefitIncrease())) {
                                        for (int s = 1; s < allFeasibleInsertions.get(simA).size(); s++) {
                                            if (newBenefitIncrease < allFeasibleInsertions.get(simA).get(s - 1).getBenenefitIncrease() &&
                                                    newBenefitIncrease >= allFeasibleInsertions.get(simA).get(s).getBenenefitIncrease()) {
                                                allFeasibleInsertions.get(simA).remove(i);
                                                allFeasibleInsertions.get(simA).add(s, option);
                                                allFeasibleInsertions.get(ourID).remove(size - 1);
                                                allFeasibleInsertions.get(ourID).add(s, option);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if(allFeasibleInsertions.get(presOfOp)!=null) {
                    int optionValue = allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease();
                    if (currentID == presOfOp) {
                        if (optionValue < currentBestValue && prevBestValue > optionValue) {
                            currentBestValue = prevBestValue;
                            currentID = prevID;
                        } else {
                            currentBestValue = optionValue;
                        }
                    }
                }
                int optionValue=allFeasibleInsertions.get(simA).get(0).getBenenefitIncrease();
                if(currentID==simA) {
                    if (optionValue < currentBestValue && prevBestValue > optionValue) {
                        currentBestValue = prevBestValue;
                        currentID = prevID;
                    } else {
                        currentBestValue = optionValue;
                    }
                }
            }
            else if (precedenceALNS[ourID-startNodes.length-1][1]!=0) {
                int presOfOp=precedenceALNS[ourID-startNodes.length-1][1];
                if(allFeasibleInsertions.get(presOfOp)!=null && currentID!=presOfOp){
                    continue;
                }
                if(allFeasibleInsertions.get(presOfOp)!=null) {
                    for (int i=0; i < allFeasibleInsertions.get(presOfOp).size();i++) {
                        InsertionValues option =allFeasibleInsertions.get(presOfOp).get(i);
                        findInsertionCosts(our, -1, -1, option.getEarliest(), option.getRouteIndex(), -1,option.getIndexInRoute(),-1);
                        int size = allFeasibleInsertions.get(ourID).size();
                        InsertionValues ourValues = allFeasibleInsertions.get(ourID).get(size - 1);
                        int ourBenefitIncrease = ourValues.getBenenefitIncrease();
                    }
                    int optionValue=allFeasibleInsertions.get(presOfOp).get(0).getBenenefitIncrease();
                    if(optionValue<currentBestValue && prevBestValue>optionValue){
                        currentBestValue=prevBestValue;
                        currentID=prevID;
                    }
                    else{
                        currentBestValue=optionValue;
                    }
                }
                else{
                    PrecedenceValues pv = precedenceOverOperations.get(presOfOp);
                    findInsertionCosts(our,-1,-1,pv.getOperationObject().getEarliestTime(),pv.getRoute(),-1,pv.getIndex(),-1);
                    int ourValue=allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease();
                    if(ourValue>currentBestValue){
                        prevBestValue=currentBestValue;
                        prevID=currentID;
                        currentBestValue=ourValue;
                        currentID=ourID;
                    }
                }
            }
            else{
                findInsertionCosts(our,-1,-1,-1,-1,-1,-1,-1);
                int ourValue=allFeasibleInsertions.get(ourID).get(0).getBenenefitIncrease();
                if(ourValue>currentBestValue){
                    prevBestValue=currentBestValue;
                    prevID=currentID;
                    currentBestValue=ourValue;
                    currentID=ourID;
                }
            }
            /*
            for (Map.Entry<Integer, List<InsertionValues>> entry : allFeasibleInsertions.entrySet()) {
                int evaluatedOperation=
                if (precedenceALNS[][0]!= 0)
            }

             */
            System.out.println("ONE ITERATION calculate insertion values:");
            System.out.println("current best ID "+currentID);
            System.out.println("current best value "+currentBestValue);
            if(currentID!=-1){
                System.out.println("Current route: "+allFeasibleInsertions.get(currentID).get(0).getRouteIndex());
                System.out.println("Current index: "+allFeasibleInsertions.get(currentID).get(0).getIndexInRoute());
            }
            System.out.println("previous best ID "+prevID);
            System.out.println("previous best value "+prevBestValue);
        }
        if(currentBestValue==-100000){
            currentID=-1;
        }
        return currentID;
    }

    public void insertionByMethod(String method){
        //husk å oppdatere unrouted her
        Boolean continueInsert=true;
        for (Map.Entry<Integer, ConsolidatedValues> entry : consolidatedOperations.entrySet()) {
            int bigTask = entry.getKey();
            int small1= bigTasksALNS[bigTask-1-startNodes.length][1];
            int small2= bigTasksALNS[bigTask-1-startNodes.length][2];
            ConsolidatedValues conVals = entry.getValue();
            if (!conVals.getConsolidated() && !conVals.getSmallTasks()){
                unroutedTasks.add(new OperationInRoute(bigTask,0,0));
                unroutedTasks.add(new OperationInRoute(small1,0,0));
                unroutedTasks.add(new OperationInRoute(small2,0,0));
            }

        }
        while(continueInsert){
            int insertID=0;
            if(method=="best"){
                insertID= calculateInsertionValuesBestInsertion();
            }
            else if(method=="regret"){
                insertID= calculateInsertionValuesRegretInsertion();
            }
            if(insertID!=-1 && unroutedTasks.size()!=0){
                System.out.println("UNROUTED ALL FEASIBLE DICTIONARY AFTER ONE ROUTE OF CALCULATIONS");
                for (Map.Entry<Integer, List<InsertionValues>> entry : allFeasibleInsertions.entrySet()) {
                    int key = entry.getKey();
                    System.out.println("Evaluating operation "+key+" precedenceOver: "+precedenceALNS[key-1-startNodes.length][0]
                            +" precedenceOf: "+precedenceALNS[key-1-startNodes.length][1]+" simOver: "+simALNS[key-1-startNodes.length][0]
                            +" simOf: "+precedenceALNS[key-1-startNodes.length][1]);
                    List<InsertionValues> iValues = entry.getValue();
                    for (InsertionValues iv : iValues){
                        System.out.println("Benefit "+iv.getBenenefitIncrease());
                    }
                    System.out.println(" ");
                }

                OperationInRoute insertOR=null;
                for (int i=0;i<unroutedTasks.size();i++){
                    if (unroutedTasks.get(i).getID()==insertID){
                        insertOR=unroutedTasks.get(i);
                    }
                }
                if(insertOR!=null){
                    System.out.println("Operation chosen to insert: "+insertOR.getID());
                    unroutedTasks.remove(insertOR);
                    InsertionValues insertValuesOR=allFeasibleInsertions.get(insertOR.getID()).get(0);
                    if(bigTasksALNS[insertID-1-startNodes.length]!=null){
                        if(insertID==bigTasksALNS[insertID-1-startNodes.length][0]){
                            unroutedTasks.remove(bigTasksALNS[insertID-1-startNodes.length][1]);
                            unroutedTasks.remove(bigTasksALNS[insertID-1-startNodes.length][2]);
                            consolidatedOperations.put(bigTasksALNS[insertID-1-startNodes.length][0],new ConsolidatedValues(true,false,0,0,
                                    insertValuesOR.getRouteIndex()));
                        }
                    }
                    insertOperation(insertOR.getID(), insertValuesOR.getEarliest(), insertValuesOR.getLatest(), insertValuesOR.getIndexInRoute(), insertValuesOR.getRouteIndex());
                    if(simALNS[insertID-1-startNodes.length][0]!=0){
                        int connectedSimID=simALNS[insertID-1-startNodes.length][0];
                        OperationInRoute connectedOR=null;
                        for (int i=0;i<unroutedTasks.size();i++){
                            if (unroutedTasks.get(i).getID()==connectedSimID){
                                connectedOR=unroutedTasks.get(i);
                            }
                        }
                        unroutedTasks.remove(connectedOR);
                        InsertionValues insertValuesORConnected=allFeasibleInsertions.get(connectedOR.getID()).get(0);
                        if(bigTasksALNS[insertID-1-startNodes.length]!=null){
                            if(insertID==bigTasksALNS[insertID-1-startNodes.length][1] ||
                                    insertID==bigTasksALNS[insertID-1-startNodes.length][2]){
                                unroutedTasks.remove(bigTasksALNS[insertID-1-startNodes.length][0]);
                                consolidatedOperations.put(bigTasksALNS[insertID-1-startNodes.length][0],new ConsolidatedValues(
                                        false,true,insertValuesOR.getRouteIndex(),insertValuesORConnected.getRouteIndex(),
                                        0));
                            }
                        }
                        insertOperation(connectedSimID, insertValuesORConnected.getEarliest(), insertValuesORConnected.getLatest(), insertValuesORConnected.getIndexInRoute(), insertValuesORConnected.getRouteIndex());
                    }
                }
            }
            if(insertID==-1 || unroutedTasks.size()==0){
                continueInsert=false;
            }
        }
    }

    public int checkprecedenceOfEarliestLNS(int o, int earliestTemp, int earliestPO, int routeConnectedPrecedence){
        int precedenceOf=precedenceALNS[o-1-startNodes.length][1];
        if(precedenceOf!=0) {
            earliestTemp = Math.max(earliestTemp, earliestPO + TimeVesselUseOnOperation[routeConnectedPrecedence][precedenceOf - 1 - startNodes.length][earliestPO-1]);
        }
        return earliestTemp;
    }

    public int[] checkSimultaneousOfTimesLNS(int o, int earliestTemp, int latestTemp, int earliestSO, int latestSO){
        int simultaneousOf=simALNS[o-1-startNodes.length][1];
        //System.out.println("Within check simultaneous of times");
        if(simultaneousOf!=0) {
            earliestTemp = Math.max(earliestTemp, earliestSO);
            latestTemp = Math.min(latestTemp, latestSO);
            //System.out.println("earliest and latest dependent on sim of operation");
        }
        return new int[]{earliestTemp,latestTemp};
    }

    public void insertFeasibleDict(int o,InsertionValues iv, int benefitIncreaseTemp){
        if (allFeasibleInsertions.get(o) == null) {
            allFeasibleInsertions.put(o, new ArrayList<>() {{
                add(iv);
            }});
        }
        else {
            if(benefitIncreaseTemp>=allFeasibleInsertions.get(o).get(0).getBenenefitIncrease()){
                allFeasibleInsertions.get(o).add(0,iv);
            }
            else if(benefitIncreaseTemp<allFeasibleInsertions.get(o).get(allFeasibleInsertions.get(o).size()-1).getBenenefitIncrease()){
                allFeasibleInsertions.get(o).add(allFeasibleInsertions.get(o).size(),iv);
            }
            else{
                for (int i=1;i<allFeasibleInsertions.get(o).size();i++){
                    if(benefitIncreaseTemp<allFeasibleInsertions.get(o).get(i-1).getBenenefitIncrease()&&
                            benefitIncreaseTemp>=allFeasibleInsertions.get(o).get(i).getBenenefitIncrease()){
                        allFeasibleInsertions.get(o).add(i,iv);
                        break;
                    }
                }
            }
        }
    }

    public boolean checkPPlacementLNS(int o, int n, int v, int pOFRoute, int pOFIndex){
        int precedenceOf=precedenceALNS[o-startNodes.length-1][1];
        if(precedenceOf!=0){
            if(pOFRoute==v){
                if(pOFIndex>=n){
                    return false;
                }
            }
        }
        return true;
    }

    public void findInsertionCosts(OperationInRoute operationToInsert, int earliestSO, int latestSO, int earliestPO,
                                   int routeConnectedPrecedence, int routeConnectedSimultaneous, int pOFIndex, int simAIndex){
        //WHAT TO DO WITH CONSOLIDATED?
        int o=operationToInsert.getID();
        int benefitIncrease=-100000;
        int indexInRoute=0;
        int routeIndex=0;
        int earliest=0;
        int latest=nTimePeriods-1;
        System.out.println("On operation: "+o);
        for (int v = 0; v < nVessels; v++) {
            boolean precedenceOverFeasible;
            boolean precedenceOfFeasible;
            boolean simultaneousFeasible;
            System.out.println("ROUTE CONNECTED SIM: "+routeConnectedSimultaneous);
            if (DataGenerator.containsElement(o, OperationsForVessel[v]) && v!= routeConnectedSimultaneous) {
                System.out.println("Try vessel "+v);
                if (vesselRoutes.get(v) == null || vesselRoutes.get(v).isEmpty()) {
                    //System.out.println("Empty route");
                    //insertion into empty route
                    int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                    int sailingCost=sailingTimeStartNodeToO*SailingCostForVessel[v];
                    int earliestTemp=Math.max(EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO+1,twIntervals[o-startNodes.length-1][0]);
                    int latestTemp=Math.min(nTimePeriods,twIntervals[o-startNodes.length-1][1]);
                    int precedenceOfValuesEarliest=checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence);
                    earliestTemp=precedenceOfValuesEarliest;
                    int [] simultaneousTimesValues = checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO);
                    //System.out.println(simultaneousTimesValues[0] + "," + simultaneousTimesValues[1]+ " sim time");
                    earliestTemp=simultaneousTimesValues[0];
                    latestTemp=simultaneousTimesValues[1];
                    if(earliestTemp<=latestTemp) {
                        System.out.println("Feasible for empty route");
                        int benefitIncreaseTemp=operationGain[v][o-startNodes.length-1][earliestTemp-1]-sailingCost;
                        if(!(simALNS[o-startNodes.length-1][1]!=0 || precedenceALNS[o-startNodes.length-1][1]!=0)) {
                            InsertionValues iv = new InsertionValues(benefitIncreaseTemp, 0, v, earliestTemp, latestTemp);
                            insertFeasibleDict(o,iv, benefitIncreaseTemp);
                        }
                        else{
                            if(benefitIncreaseTemp>0 && benefitIncreaseTemp>benefitIncrease) {
                                benefitIncrease = benefitIncreaseTemp;
                                routeIndex = v;
                                indexInRoute = 0;
                                earliest = earliestTemp;
                                latest = latestTemp;
                            }
                        }
                    }
                }
                else{
                    for(int n=0;n<vesselRoutes.get(v).size();n++){
                        System.out.println("On index "+n);
                        if(n==0) {
                            //check insertion in first position
                            int sailingTimeStartNodeToO=SailingTimes[v][EarliestStartingTimeForVessel[v]][v][o - 1];
                            int earliestTemp = Math.max(EarliestStartingTimeForVessel[v] + sailingTimeStartNodeToO + 1, twIntervals[o - startNodes.length - 1][0]);
                            int opTime=TimeVesselUseOnOperation[v][o - 1 - startNodes.length][EarliestStartingTimeForVessel[v]+sailingTimeStartNodeToO];
                            int precedenceOfValuesEarliest=checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence);
                            earliestTemp=precedenceOfValuesEarliest;
                            if(earliestTemp<=60) {
                                int sailingTimeOToNext = SailingTimes[v][earliestTemp - 1][o - 1][vesselRoutes.get(v).get(0).getID() - 1];
                                int latestTemp = Math.min(vesselRoutes.get(v).get(0).getLatestTime() - sailingTimeOToNext - opTime,
                                        twIntervals[o - startNodes.length - 1][1]);
                                int [] simultaneousTimesValues = checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO);
                                earliestTemp = simultaneousTimesValues[0];
                                latestTemp = simultaneousTimesValues[1];
                                int timeIncrease = sailingTimeStartNodeToO + sailingTimeOToNext
                                        - SailingTimes[v][EarliestStartingTimeForVessel[v]][v][vesselRoutes.get(v).get(0).getID() - 1];
                                int sailingCost = timeIncrease * SailingCostForVessel[v];
                                Boolean pPlacementFeasible = checkPPlacementLNS(o, n, v,routeConnectedPrecedence,pOFIndex);
                                if (earliestTemp <= latestTemp && pPlacementFeasible) {
                                    OperationInRoute lastOperation = vesselRoutes.get(v).get(vesselRoutes.get(v).size() - 1);
                                    int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                    int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, 0, v, o,vesselRoutes,
                                            TimeVesselUseOnOperation,startNodes,SailingTimes);
                                    int deltaOperationGainLastOperation = 0;
                                    if (changedTime > 0) {
                                        deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                    }
                                    int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                    if (benefitIncreaseTemp > 0) {
                                        precedenceOverFeasible = checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, 0, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods,
                                                SailingTimes, vesselRoutes, precedenceOfOperations, precedenceOverRoutes);
                                        precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, 0, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                vesselRoutes, precedenceOverOperations, precedenceOfRoutes);
                                        simultaneousFeasible = checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, 0, earliestTemp, latestTemp, simultaneousOp, simALNS,
                                                startNodes, SailingTimes, TimeVesselUseOnOperation,vesselRoutes,routeConnectedSimultaneous,simAIndex);
                                        if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                            System.out.println("Feasible for position n=0");
                                            if(!(simALNS[o-startNodes.length-1][1]!=0 || precedenceALNS[o-startNodes.length-1][1]!=0)) {
                                                InsertionValues iv = new InsertionValues(benefitIncreaseTemp, 0, v, earliestTemp, latestTemp);
                                                insertFeasibleDict(o,iv, benefitIncreaseTemp);
                                            }
                                            else{
                                                if(benefitIncreaseTemp>benefitIncrease) {
                                                    benefitIncrease = benefitIncreaseTemp;
                                                    routeIndex = v;
                                                    indexInRoute = 0;
                                                    earliest = earliestTemp;
                                                    latest = latestTemp;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (n==vesselRoutes.get(v).size()-1){
                            //check insertion in last position
                            //System.out.println("Checking this position");
                            int earliestN=vesselRoutes.get(v).get(n).getEarliestTime();
                            int operationTimeN=TimeVesselUseOnOperation[v][vesselRoutes.get(v).get(n).getID()-1-startNodes.length][earliestN-1];
                            int startTimeSailingTimePrevToO=earliestN+operationTimeN;
                            if(startTimeSailingTimePrevToO >= nTimePeriods){
                                continue;
                            }
                            int sailingTimePrevToO=SailingTimes[v][startTimeSailingTimePrevToO-1]
                                    [vesselRoutes.get(v).get(n).getID()-1][o - 1];
                            int earliestTemp=Math.max(earliestN + operationTimeN + sailingTimePrevToO
                                    ,twIntervals[o-startNodes.length-1][0]);
                            int latestTemp=twIntervals[o-startNodes.length-1][1];
                            int precedenceOfValuesEarliest=checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence);
                            earliestTemp=precedenceOfValuesEarliest;
                            if(earliestTemp<=60) {
                                System.out.println("Time feasible");
                                int [] simultaneousTimesValues = checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO);
                                earliestTemp = simultaneousTimesValues[0];
                                latestTemp = simultaneousTimesValues[1];
                                int timeIncrease = sailingTimePrevToO;
                                int sailingCost = timeIncrease * SailingCostForVessel[v];
                                if (earliestTemp <= latestTemp) {
                                    System.out.println("p placement feasible and time feasible");
                                    OperationInRoute lastOperation = vesselRoutes.get(v).get(vesselRoutes.get(v).size() - 1);
                                    int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                    int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute, n + 1, v, o,vesselRoutes,
                                            TimeVesselUseOnOperation,startNodes,SailingTimes);
                                    int deltaOperationGainLastOperation = 0;
                                    if (changedTime > 0) {
                                        deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                    }
                                    int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                    if(o==19){
                                        System.out.println("DELTA OPERATION GAIN LAST OPERATION: "+deltaOperationGainLastOperation);
                                        System.out.println("benefit increase 19: "+benefitIncreaseTemp);
                                    }
                                    if (benefitIncreaseTemp > 0) {
                                        precedenceOverFeasible = checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation, nTimePeriods, SailingTimes,
                                                vesselRoutes, precedenceOfOperations, precedenceOverRoutes);
                                        precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                vesselRoutes, precedenceOverOperations, precedenceOfRoutes);
                                        simultaneousFeasible = checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp, simultaneousOp,
                                                simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation,vesselRoutes,routeConnectedSimultaneous,simAIndex);
                                        if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                            System.out.println("Feasible for last position in route");
                                            if(!(simALNS[o-startNodes.length-1][1]!=0 || precedenceALNS[o-startNodes.length-1][1]!=0)) {
                                                InsertionValues iv=new InsertionValues(benefitIncreaseTemp,n+1,v,earliestTemp,latestTemp);
                                                insertFeasibleDict(o,iv,benefitIncreaseTemp);
                                            }
                                            else{
                                                System.out.println("Benefit increase temp: "+benefitIncreaseTemp);
                                                if(benefitIncreaseTemp>benefitIncrease) {
                                                    benefitIncrease = benefitIncreaseTemp;
                                                    routeIndex = v;
                                                    indexInRoute = n + 1;
                                                    earliest = earliestTemp;
                                                    latest = latestTemp;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if(n<vesselRoutes.get(v).size()-1) {
                            //check insertion for all other positions in the route
                            int earliestN = vesselRoutes.get(v).get(n).getEarliestTime();
                            int operationTimeN = TimeVesselUseOnOperation[v][vesselRoutes.get(v).get(n).getID() - 1 - startNodes.length][earliestN - 1];
                            int startTimeSailingTimePrevToO = earliestN + operationTimeN;
                            int sailingTimePrevToO = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselRoutes.get(v).get(n).getID() - 1][o - 1];
                            int earliestTemp = Math.max(earliestN + sailingTimePrevToO + operationTimeN, twIntervals[o - startNodes.length - 1][0]);
                            int precedenceOfValuesEarliest=checkprecedenceOfEarliestLNS(o,earliestTemp,earliestPO,routeConnectedPrecedence);
                            earliestTemp=precedenceOfValuesEarliest;
                            if(earliestTemp<=60) {
                                if (earliestTemp - 1 < nTimePeriods) {
                                    int opTime = TimeVesselUseOnOperation[v][o - 1 - startNodes.length][earliestTemp - 1];
                                    int sailingTimeOToNext = SailingTimes[v][Math.min(earliestTemp + opTime - 1, nTimePeriods - 1)][o - 1][vesselRoutes.get(v).get(n + 1).getID() - 1];

                                    int latestTemp = Math.min(vesselRoutes.get(v).get(n + 1).getLatestTime() -
                                            sailingTimeOToNext - opTime, twIntervals[o - startNodes.length - 1][1]);
                                    int [] simultaneousTimesValues = checkSimultaneousOfTimesLNS(o, earliestTemp, latestTemp, earliestSO, latestSO);
                                    earliestTemp = simultaneousTimesValues[0];
                                    latestTemp = simultaneousTimesValues[1];
                                    int sailingTimePrevToNext = SailingTimes[v][startTimeSailingTimePrevToO - 1][vesselRoutes.get(v).get(n).getID() - 1][vesselRoutes.get(v).get(n + 1).getID() - 1];
                                    int timeIncrease = sailingTimePrevToO + sailingTimeOToNext - sailingTimePrevToNext;
                                    int sailingCost = timeIncrease * SailingCostForVessel[v];
                                    Boolean pPlacementFeasible = checkPPlacementLNS(o, n+1, v,routeConnectedPrecedence,pOFIndex);
                                    if (earliestTemp <= latestTemp && pPlacementFeasible) {
                                        OperationInRoute lastOperation = vesselRoutes.get(v).get(vesselRoutes.get(v).size() - 1);
                                        int earliestTimeLastOperationInRoute = lastOperation.getEarliestTime();
                                        int changedTime = ConstructionHeuristic.checkChangeEarliestLastOperation(earliestTemp, earliestTimeLastOperationInRoute,
                                                n + 1, v, o,vesselRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes);
                                        int deltaOperationGainLastOperation = 0;
                                        if (changedTime > 0) {
                                            deltaOperationGainLastOperation = operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute - 1] -
                                                    operationGain[v][lastOperation.getID() - 1 - startNodes.length][earliestTimeLastOperationInRoute + changedTime - 1];
                                        }
                                        int benefitIncreaseTemp = operationGain[v][o - startNodes.length - 1][earliestTemp - 1] - sailingCost - deltaOperationGainLastOperation;
                                        if (benefitIncreaseTemp > 0) {
                                            int currentLatest = vesselRoutes.get(v).get(n).getLatestTime();
                                            simultaneousFeasible = ConstructionHeuristic.checkSOfFeasible(o, v, currentLatest, startNodes, simALNS, simultaneousOp);
                                            if (simultaneousFeasible) {
                                                simultaneousFeasible = checkSimultaneousFeasibleLNS(simOpRoutes.get(v), o, v, n + 1, earliestTemp, latestTemp,
                                                        simultaneousOp, simALNS, startNodes, SailingTimes, TimeVesselUseOnOperation,
                                                        vesselRoutes,routeConnectedSimultaneous,simAIndex);
                                            }
                                            precedenceOverFeasible = checkPOverFeasibleLNS(precedenceOverRoutes.get(v), o, n + 1, earliestTemp, startNodes, TimeVesselUseOnOperation,
                                                    nTimePeriods, SailingTimes, vesselRoutes, precedenceOfOperations, precedenceOverRoutes);
                                            precedenceOfFeasible = ConstructionHeuristic.checkPOfFeasible(precedenceOfRoutes.get(v), o, n + 1, latestTemp, startNodes, TimeVesselUseOnOperation, SailingTimes,
                                                    vesselRoutes, precedenceOverOperations, precedenceOfRoutes);
                                            if (precedenceOverFeasible && precedenceOfFeasible && simultaneousFeasible) {
                                                System.out.println("Feasible for index: "+(n+1));
                                                if(!(simALNS[o-startNodes.length-1][1]!=0 || precedenceALNS[o-startNodes.length-1][1]!=0)) {
                                                    InsertionValues iv=new InsertionValues(benefitIncreaseTemp,n+1,v,earliestTemp,latestTemp);
                                                    insertFeasibleDict(o,iv,benefitIncreaseTemp);
                                                }
                                                else{
                                                    if(benefitIncreaseTemp>benefitIncrease) {
                                                        benefitIncrease = benefitIncreaseTemp;
                                                        routeIndex = v;
                                                        indexInRoute = n + 1;
                                                        earliest = earliestTemp;
                                                        latest = latestTemp;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if((simALNS[o-startNodes.length-1][1]!=0 || precedenceALNS[o-startNodes.length-1][1]!=0)){
            System.out.println("Benefit increase for presOf or simOf "+benefitIncrease);
            int finalBenefitIncrease = benefitIncrease;
            int finalIndexInRoute = indexInRoute;
            int finalRouteIndex = routeIndex;
            int finalEarliest = earliest;
            int finalLatest = latest;
            if(allFeasibleInsertions.get(o)==null){
                allFeasibleInsertions.put(o, new ArrayList<>() {{
                    add(new InsertionValues(finalBenefitIncrease, finalIndexInRoute, finalRouteIndex, finalEarliest, finalLatest));
                }});
            }
            else{
                allFeasibleInsertions.get(o).add(new InsertionValues(finalBenefitIncrease, finalIndexInRoute, finalRouteIndex, finalEarliest, finalLatest));
            }
        }
        else{
            int finalBenefitIncrease = benefitIncrease;
            int finalIndexInRoute = indexInRoute;
            int finalRouteIndex = routeIndex;
            int finalEarliest = earliest;
            int finalLatest = latest;
            if (allFeasibleInsertions.get(o)==null){
                allFeasibleInsertions.put(o, new ArrayList<>() {{
                    add(new InsertionValues(finalBenefitIncrease, finalIndexInRoute, finalRouteIndex, finalEarliest, finalLatest));
                }});
            }
        }
    }

    public void insertOperation(int o, int earliest, int latest, int indexInRoute, int routeIndex){
        //=ConstructionHeuristic.updateConsolidatedOperations(o,routeIndex,removeConsolidatedSmallTasks,bigTasksALNS,
        //        startNodes, consolidatedOperations);
        OperationInRoute newOr=new OperationInRoute(o, earliest, latest);
        int presOver=precedenceALNS[o-1-startNodes.length][0];
        int presOf=precedenceALNS[o-1-startNodes.length][1];
        if (presOver!=0){
            //System.out.println(o+" added in precedence operations dictionary 0 "+presOver);
            PrecedenceValues pValues= new PrecedenceValues(newOr,null,presOver,indexInRoute,routeIndex,0);
            precedenceOverOperations.put(o,pValues);
            precedenceOverRoutes.get(routeIndex).put(o,pValues);
        }
        if (presOf!=0){
            //System.out.println("Operation precedence of: "+presOf);
            PrecedenceValues pValues= precedenceOverOperations.get(presOf);
            PrecedenceValues pValuesReplace=new PrecedenceValues(pValues.getOperationObject(),
                    newOr,pValues.getConnectedOperationID(),pValues.getIndex(),pValues.getRoute(),routeIndex);
            PrecedenceValues pValuesPut=new PrecedenceValues(newOr,pValues.getOperationObject(),presOf,indexInRoute,routeIndex,pValues.getRoute());
            precedenceOverOperations.put(presOf,pValuesReplace);
            precedenceOfOperations.put(o, pValuesPut);
            precedenceOverRoutes.get(pValues.getRoute()).put(presOf,pValuesReplace);
            precedenceOfRoutes.get(routeIndex).put(o, pValuesPut);
        }
        //while((o-1-startNodes.length+k) < simALNS.length && simALNS[o-1-startNodes.length+k][0]!=0 ){
        int simA = simALNS[o-1-startNodes.length][1];
        int simB = simALNS[o-1-startNodes.length][0];
        if(simB != 0 && simA == 0) {
            ConnectedValues sValue = new ConnectedValues(newOr, null,simB,indexInRoute,routeIndex, 0);
            simultaneousOp.put(o,sValue);
            simOpRoutes.get(routeIndex).put(o,sValue);
        }
        else if (simA != 0){
            ConnectedValues sValues = simultaneousOp.get(simA);
            if(sValues.getConnectedOperationObject() == null){
                ConnectedValues cValuesReplace=new ConnectedValues(sValues.getOperationObject(), newOr, sValues.getConnectedOperationID(),
                        sValues.getIndex(), sValues.getRoute(), routeIndex);
                simultaneousOp.replace(simA, sValues, cValuesReplace);
                simOpRoutes.get(sValues.getRoute()).replace(simA, sValues, cValuesReplace);
            }
            else{
                ConnectedValues cValuesPut1=new ConnectedValues(sValues.getOperationObject(), newOr, sValues.getConnectedOperationID(),
                        sValues.getIndex(), sValues.getRoute(), routeIndex);
                simultaneousOp.put(simA, cValuesPut1);
                simOpRoutes.get(sValues.getRoute()).put(simA, cValuesPut1);
            }
            ConnectedValues sim2=new ConnectedValues(newOr,sValues.getOperationObject(),simA,indexInRoute,routeIndex,sValues.getRoute());
            simultaneousOp.put(o, sim2);
            simOpRoutes.get(routeIndex).put(o, sim2);
        }
        System.out.println("INSERTION IS PERFORMED");
        System.out.println("NEW ADD: Vessel route "+routeIndex);
        System.out.println("Operation "+o);
        System.out.println("Earliest time "+ earliest);
        System.out.println("Latest time "+ latest);
        System.out.println("Route index "+indexInRoute);
        System.out.println(" ");
        if (vesselRoutes.get(routeIndex) == null) {
            int finalIndexInRoute = indexInRoute;
            vesselRoutes.set(routeIndex, new ArrayList<>() {{
                add(finalIndexInRoute, newOr);
            }});
        }
        else {
            vesselRoutes.get(routeIndex).add(indexInRoute,newOr);
        }
        ConstructionHeuristic.updateIndexesInsertion(routeIndex,indexInRoute, vesselRoutes,simultaneousOp,precedenceOverOperations,precedenceOfOperations);
        //Update all earliest starting times forward
        ConstructionHeuristic.updateEarliest(earliest,indexInRoute,routeIndex, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes);
        ConstructionHeuristic.updateLatest(latest,indexInRoute,routeIndex, TimeVesselUseOnOperation, startNodes, SailingTimes, vesselRoutes);
        ConstructionHeuristic.updatePrecedenceOver(precedenceOverRoutes.get(routeIndex),indexInRoute,simOpRoutes,precedenceOfOperations,precedenceOverOperations,
                TimeVesselUseOnOperation, startNodes,precedenceOverRoutes,precedenceOfRoutes,simultaneousOp,vesselRoutes,SailingTimes);
        ConstructionHeuristic.updatePrecedenceOf(precedenceOfRoutes.get(routeIndex),indexInRoute,TimeVesselUseOnOperation,startNodes,simOpRoutes,
                precedenceOverOperations,precedenceOfOperations,precedenceOfRoutes,precedenceOverRoutes,vesselRoutes,simultaneousOp,SailingTimes);
        ConstructionHeuristic.updateSimultaneous(simOpRoutes,routeIndex,indexInRoute,simultaneousOp,precedenceOverRoutes,precedenceOfRoutes,TimeVesselUseOnOperation,
                startNodes,SailingTimes,precedenceOverOperations,precedenceOfOperations,vesselRoutes);
        /*
        for(int r=0;r<nVessels;r++) {
            System.out.println("VESSEL " + r);
            if(vesselRoutes.get(r) != null) {
                for (int n = 0; n < vesselRoutes.get(r).size(); n++) {
                    System.out.println("Number in order: " + n);
                    System.out.println("ID " + vesselRoutes.get(r).get(n).getID());
                    System.out.println("Earliest starting time " + vesselRoutes.get(r).get(n).getEarliestTime());
                    System.out.println("latest starting time " + vesselRoutes.get(r).get(n).getLatestTime());
                    System.out.println(" ");
                }
            }
        }

         */
    }

    public Boolean checkPOverFeasibleLNS(Map<Integer,PrecedenceValues> precedenceOver, int o, int insertIndex,int earliest,
                                         int []startNodes, int[][][] TimeVesselUseOnOperation, int nTimePeriods,
                                         int [][][][] SailingTimes, List<List<OperationInRoute>> vesselroutes,
                                         Map<Integer,PrecedenceValues> precedenceOfOperations,
                                         List<Map<Integer,PrecedenceValues>> precedenceOverRoutes) {
        if(precedenceOver!=null) {
            for (PrecedenceValues pValues : precedenceOver.values()) {
                int route = pValues.getRoute();
                OperationInRoute firstOr = pValues.getOperationObject();
                OperationInRoute secondOr = pValues.getConnectedOperationObject();
                if (secondOr != null && !unroutedTasks.contains(secondOr)) {
                    PrecedenceValues connectedOpPValues = precedenceOfOperations.get(secondOr.getID());
                    System.out.println("first operation: "+firstOr.getID());
                    int routeConnectedOp = connectedOpPValues.getRoute();
                    if (routeConnectedOp == pValues.getRoute()) {
                        continue;
                    }
                    int precedenceIndex = pValues.getIndex();
                    if (insertIndex <= precedenceIndex) {
                        int change = ConstructionHeuristic.checkChangeEarliest(earliest, insertIndex, route, precedenceIndex, pValues.getOperationObject().getEarliestTime(), o,
                                startNodes,TimeVesselUseOnOperation,SailingTimes, vesselroutes);
                        if (change!=0) {
                            int t= firstOr.getEarliestTime()+change-1;
                            if(t>nTimePeriods-1){
                                t=nTimePeriods-1;
                            }
                            int newESecondOr=firstOr.getEarliestTime() + TimeVesselUseOnOperation[route][firstOr.getID() - startNodes.length - 1]
                                    [t] + change;
                            if(newESecondOr>secondOr.getEarliestTime()) {
                                if (newESecondOr > secondOr.getLatestTime()) {
                                    //System.out.println("NOT PRECEDENCE OVER FEASIBLE EARLIEST/LATEST P-OPERATION "+firstOr.getID());
                                    //System.out.println("Precedence over infeasible");
                                    return false;
                                }
                                else{
                                    int secondOrIndex= connectedOpPValues.getIndex();
                                    if (!checkPOverFeasibleLNS(precedenceOverRoutes.get(routeConnectedOp),secondOr.getID(),secondOrIndex,newESecondOr,
                                            startNodes,TimeVesselUseOnOperation,nTimePeriods, SailingTimes, vesselroutes, precedenceOfOperations,precedenceOverRoutes)){
                                        //System.out.println("Precedence over infeasible");
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }


    public void runLNSInsert(){
        insertionByMethod("best");
        ConstructionHeuristic.calculateObjective(vesselRoutes,TimeVesselUseOnOperation,startNodes,SailingTimes,SailingCostForVessel,
                EarliestStartingTimeForVessel, operationGain, routeSailingCost,routeOperationGain,objValue);
    }

    public void printLNSInsertSolution(int[] vessseltypes){
        //PrintData.timeVesselUseOnOperations(TimeVesselUseOnOperation,startNodes.length);
        //PrintData.printSailingTimes(SailingTimes,3,nOperations-2*startNodes.length,startNodes.length);
        //PrintData.printOperationsForVessel(OperationsForVessel);
        //PrintData.printPrecedenceALNS(precedenceALNS);
        //PrintData.printSimALNS(simALNS);
        //PrintData.printTimeWindows(timeWindowsForOperations);
        //PrintData.printTimeWindowsIntervals(twIntervals);

        System.out.println("SOLUTION AFTER LNS INSERT");

        System.out.println("Sailing cost per route: "+ Arrays.toString(routeSailingCost));
        System.out.println("Operation gain per route: "+Arrays.toString(routeOperationGain));
        int obj= IntStream.of(routeOperationGain).sum()-IntStream.of(routeSailingCost).sum();
        System.out.println("Objective value: "+obj);
        for (int i=0;i<vesselRoutes.size();i++){
            int totalTime=0;
            System.out.println("VESSELINDEX "+i+" VESSELTYPE "+vessseltypes[i]);
            if (vesselRoutes.get(i)!=null) {
                for (int o=0;o<vesselRoutes.get(i).size();o++) {
                    System.out.println("Operation number: "+vesselRoutes.get(i).get(o).getID() + " Earliest start time: "+
                            vesselRoutes.get(i).get(o).getEarliestTime()+ " Latest Start time: "+ vesselRoutes.get(i).get(o).getLatestTime());
                    if (o==0){
                        totalTime+=SailingTimes[i][0][i][vesselRoutes.get(i).get(o).getID()-1];
                        totalTime+=TimeVesselUseOnOperation[i][vesselRoutes.get(i).get(o).getID()-startNodes.length-1][0];
                        //System.out.println("temp total time: "+totalTime);
                    }
                    else{
                        totalTime+=SailingTimes[i][0][vesselRoutes.get(i).get(o-1).getID()-1][vesselRoutes.get(i).get(o).getID()-1];
                        if(o!=vesselRoutes.get(i).size()-1) {
                            totalTime += TimeVesselUseOnOperation[i][vesselRoutes.get(i).get(o).getID() - startNodes.length - 1][0];
                        }
                        //System.out.println("temp total time: "+totalTime);
                    }
                }
            }
            System.out.println("TOTAL DURATION FOR ROUTE: "+totalTime);
        }
        if(!unroutedTasks.isEmpty()){
            System.out.println("UNROUTED TASKS");
            for(int n=0;n<unroutedTasks.size();n++) {
                System.out.println(unroutedTasks.get(n).getID());
            }
        }
        System.out.println(" ");
        System.out.println("SIMULTANEOUS DICTIONARY");
        for(Map.Entry<Integer, ConnectedValues> entry : simultaneousOp.entrySet()){
            ConnectedValues simOp = entry.getValue();
            System.out.println("Simultaneous operation: " + simOp.getOperationObject().getID() + " in route: " +
                    simOp.getRoute() + " with index: " + simOp.getIndex());
        }
        System.out.println("PRECEDENCE OVER DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOverOperations.entrySet()){
            PrecedenceValues presOverOp = entry.getValue();
            System.out.println("Precedence over operation: " + presOverOp.getOperationObject().getID() + " in route: " +
                    presOverOp.getRoute() + " with index: " + presOverOp.getIndex());
        }
        System.out.println("PRECEDENCE OF DICTIONARY");
        for(Map.Entry<Integer, PrecedenceValues> entry : precedenceOfOperations.entrySet()){
            PrecedenceValues presOfOp = entry.getValue();
            System.out.println("Precedence of operation: " + presOfOp.getOperationObject().getID() + " in route: " +
                    presOfOp.getRoute() + " with index: " + presOfOp.getIndex());
        }
        System.out.println("Is solution feasible? "+checkSolution(vesselRoutes));
    }

    public Boolean checkSolution(List<List<OperationInRoute>> vesselroutes){
        for(List<OperationInRoute> route : vesselroutes){
            if(route != null) {
                for (OperationInRoute operation : route) {
                    if (operation.getEarliestTime() > operation.getLatestTime()) {
                        System.out.println("Earliest time is larger than latest time, infeasible move");
                        return false;
                    }
                }
            }
        }
        if(!simultaneousOp.isEmpty()){
            for(ConnectedValues op : simultaneousOp.values()){
                OperationInRoute conOp = op.getConnectedOperationObject();
                if(op.getOperationObject().getEarliestTime() != conOp.getEarliestTime() ||
                        op.getOperationObject().getLatestTime() != conOp.getLatestTime()){
                    System.out.println("Earliest and/or latest time for simultaneous op do not match, infeasible move");
                    return false;
                }
            }
        }
        if(!precedenceOverOperations.isEmpty()){
            for(PrecedenceValues op : precedenceOverOperations.values()){
                OperationInRoute conop = op.getConnectedOperationObject();
                if(conop != null) {
                    if (conop.getEarliestTime() < op.getOperationObject().getEarliestTime() + TimeVesselUseOnOperation[op.getRoute()]
                            [op.getOperationObject().getID() - 1-startNodes.length][op.getOperationObject().getEarliestTime()]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Boolean checkSimultaneousFeasibleLNS(Map<Integer,ConnectedValues> simOps, int o, int v, int insertIndex, int earliestTemp,
                                                int latestTemp, Map<Integer, ConnectedValues> simultaneousOp, int [][] simALNS,
                                                int [] startNodes, int [][][][] SailingTimes, int [][][] TimeVesselUseOnOperation,
                                                List<List<OperationInRoute>> vesselroutes, int simARoute, int simAIndex){
        if(simOps!=null) {
            for (ConnectedValues op : simOps.values()) {
                //System.out.println("trying to insert operation " + o + " in position " + insertIndex+ " , " +op.getOperationObject().getID() + " simultaneous operation in route " +v);
                ArrayList<ArrayList<Integer>> earliest_change = ConstructionHeuristic.checkChangeEarliestSim(earliestTemp,insertIndex,v,o,op.getOperationObject().getID(),startNodes,
                        SailingTimes, TimeVesselUseOnOperation, simultaneousOp, vesselroutes);
                if (!earliest_change.isEmpty()) {
                    for (ArrayList<Integer> connectedTimes : earliest_change) {
                        //System.out.println(connectedTimes.get(0) + " , " + connectedTimes.get(1) + " earliest change");
                        if (connectedTimes.get(0) > connectedTimes.get(1)) {
                            //System.out.println("Sim infeasible");
                            return false;
                        }
                        ConnectedValues conOp = simultaneousOp.get(op.getConnectedOperationID());
                        //System.out.println(conOp.getOperationObject().getID() + " Con op operation ID " + conOp.getRoute() + " route index");
                        if(simALNS[o-startNodes.length-1][1] != 0 &&
                                simARoute == conOp.getRoute()){
                            //System.out.println(simultaneousOp.get(simALNS[o-startNodes.length-1][1]).getRoute() + " Con op of o ID" );
                            //System.out.println(conOp.getIndex());
                            if((simAIndex - conOp.getIndex() > 0 &&
                                    insertIndex - op.getIndex() < 0) || (simAIndex -
                                    conOp.getIndex() < 0 && insertIndex - op.getIndex() > 0)){
                                //System.out.println("Sim infeasible");
                                return false;
                            }
                        }
                    }
                }
                ArrayList<ArrayList<Integer>> latest_change = ConstructionHeuristic.checkChangeLatestSim(latestTemp,insertIndex,v,o,op.getOperationObject().getID(),startNodes,
                        SailingTimes,TimeVesselUseOnOperation,simultaneousOp,vesselroutes);
                if(!latest_change.isEmpty()){
                    for(ArrayList<Integer> connectedTimes : latest_change){
                        //System.out.println(connectedTimes.get(0) + " , " + connectedTimes.get(1) + " latest change");
                        if(connectedTimes.get(0) > connectedTimes.get(1)){
                            //System.out.println("Sim infeasible");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static void main(String[] args) throws FileNotFoundException {
        int[] vesseltypes = new int[]{1,2,3,4,5};
        int[] startnodes=new int[]{1,2,3,4,5};
        DataGenerator dg = new DataGenerator(vesseltypes, 5,startnodes ,
                "test_instances/test_instance_15_locations_PRECEDENCEtest4.txt",
                "results.txt", "weather_files/weather_normal.txt");
        dg.generateData();
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),startnodes.length);
        //PrintData.printSailingTimes(dg.getSailingTimes(),2,17, 4);
        //PrintData.printSailingTimes(dg.getSailingTimes(),3,23, 4);
        ConstructionHeuristic a = new ConstructionHeuristic(dg.getOperationsForVessel(), dg.getTimeWindowsForOperations(), dg.getEdges(),
                dg.getSailingTimes(), dg.getTimeVesselUseOnOperation(), dg.getEarliestStartingTimeForVessel(),
                dg.getSailingCostForVessel(), dg.getOperationGain(), dg.getPrecedence(), dg.getSimultaneous(),
                dg.getBigTasksArr(), dg.getConsolidatedTasks(), dg.getEndNodes(), dg.getStartNodes(), dg.getEndPenaltyForVessel(),dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getBigTasksALNS(),dg.getTimeWindowsForOperations());
        a.createSortedOperations();
        a.constructionHeuristic();
        a.printInitialSolution(vesseltypes);
        LargeNeighboorhoodSearchRemoval LNSR = new LargeNeighboorhoodSearchRemoval(a.getPrecedenceOverOperations(),a.getPrecedenceOfOperations(),
                a.getSimultaneousOp(),a.getSimOpRoutes(),a.getPrecedenceOfRoutes(),a.getPrecedenceOverRoutes(),
                a.getConsolidatedOperations(),a.getUnroutedTasks(),a.getVesselroutes(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(), dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),15,21,dg.getDistOperationsInInstance(),
                0.08,0.5,0.01,0.1,
                0.1,0.1);
        LNSR.runLNSRemoval();
        System.out.println("-----------------");
        LNSR.printLNSSolution(vesseltypes);
        //PrintData.printSailingTimes(dg.getSailingTimes(),4,dg.getSimultaneousALNS().length,a.getVesselroutes().size());
        //PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),dg.getStartNodes().length);
        LargeNeighboorhoodSearchInsertCopy LNSI = new LargeNeighboorhoodSearchInsertCopy(LNSR.getPrecedenceOverOperations(),LNSR.getPrecedenceOfOperations(),
                LNSR.getSimultaneousOp(),LNSR.getSimOpRoutes(),LNSR.getPrecedenceOfRoutes(),LNSR.getPrecedenceOverRoutes(),
                LNSR.getConsolidatedOperations(),LNSR.getUnroutedTasks(),LNSR.getVesselRoutes(),LNSR.getRemovedOperations(), dg.getTwIntervals(),
                dg.getPrecedenceALNS(),dg.getSimultaneousALNS(),dg.getStartNodes(),
                dg.getSailingTimes(),dg.getTimeVesselUseOnOperation(),dg.getSailingCostForVessel(),dg.getEarliestStartingTimeForVessel(),
                dg.getOperationGain(),dg.getBigTasksALNS(),dg.getOperationsForVessel(),5);
        System.out.println("-----------------");
        PrintData.printPrecedenceALNS(dg.getPrecedenceALNS());
        PrintData.printSimALNS(dg.getSimultaneousALNS());
        //PrintData.printSailingTimes(dg.getSailingTimes(),2,dg.getSimultaneousALNS().length,dg.getStartNodes().length);
        PrintData.timeVesselUseOnOperations(dg.getTimeVesselUseOnOperation(),startnodes.length);
        LNSI.runLNSInsert();
        LNSI.printLNSInsertSolution(vesseltypes);
    }
}