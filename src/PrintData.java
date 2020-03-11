import java.util.Arrays;

public class PrintData {

    //Matrix with dimensions (all nodes x all nodes), where all nodes both includes operations, start nodes and end nodes
    public static void printPrecedence(int[][] precedence){
        System.out.println("PRECEDENCE");
        printGrid(precedence[0].length,precedence);
    }

    //Matrix with dimensions (all nodes x all nodes), where all nodes both includes operations, start nodes and end nodes
    public static void printSimultaneous(int[][] simultaneous){
        System.out.println("SIMULTANEOUS");
        printGrid(simultaneous[0].length,simultaneous);
    }

    //Three dimensional list, this function prints the gain a vessels receive for starting an operation in each time period
    // the number of rows is the number of operations, not including start nodes and end nodes, columns represent the time periods.
    public static void printOperationGain(int[][][] operationGain, int nStartNodes){
        System.out.println("POperation gain");
        for(int v=0;v<nStartNodes;v++) {
            System.out.println("VESSEL "+v);
            printGrid2(operationGain[v][0].length,operationGain[v].length,
                    operationGain[v]);
        }
    }

    //3 dimensional list, this function prints the time vessels use on operations in each time period.
    // For each vessel that is printed, operations will be the outer list and time will be the inner list.
    //Only operations are included, not start nodes and end nodes
    public static void timeVesselUseOnOperations(int[][][] timeVesselUseOnOperation, int nStartNodes){
        System.out.println("TIME VESSEL USE ON OPERATIONS");
        for(int v=0;v<nStartNodes;v++){
            System.out.println("VESSEL "+v);
            printGrid2(timeVesselUseOnOperation[v][0].length,timeVesselUseOnOperation[v].length,
                    timeVesselUseOnOperation[v]);
        }
    }

    //The length of the list is the number of vessels
    public static void printSailingCostForVessel(int [] sailingCostForVessel){
        System.out.println(" SailingCostForVessel: "+Arrays.toString(sailingCostForVessel));
    }

    //The length of the list is the number of vessels
    public static void printEarliestStartingTimes(int [] earliestStartingTimeForVessel){
        System.out.println(" earliestStartingTimes: "+Arrays.toString(earliestStartingTimeForVessel));
    }

    //The length of the list is the number of vessels
    public static void printEndPenaltyForVessel (double [] endPenaltyForVessel){
        System.out.println(" endPenalty: "+Arrays.toString(endPenaltyForVessel));
    }

    //Matrix. Outer list: vessels, Inner list: operations. Only operations included, not start and end nodes
    // Each vessel list consists of the number of the operations the vessel can perform
    // The operations is not null indexed, for example, the first operation will have the number 4 if we have three
    //start nodes
    public static void printOperationsForVessel (int [][] operationsForVessel){
        System.out.println("Operations for vessels");
        printGrid2(operationsForVessel[0].length,operationsForVessel.length, operationsForVessel);
    }

    //Chosen vessel is the number of the vessel you want to print sailing times for. The number of the chosen vessel is not null
    //indexed, hence the first vessel in the fleet has number 1.
    //print the sailing times for the chosen vessel in each time period, start nodes and end nodes are also included
    public static void printSailingTimes(int[][][][] sailingTimes, int chosenVessel, int numberOfOperations, int numberOfVessels){
        int index=1;
        for(int[][] vessel:sailingTimes[chosenVessel-1]) {
            System.out.println("Vessel "+chosenVessel+" in time period: " + String.valueOf(index));
            printGrid(numberOfOperations + 2*numberOfVessels, vessel);
            index += 1;
        }
    }

    //Start and end nodes included, outer list: nodes, inner list: time
    //All times where an operation can be performed, is included in the list for that operation
    public static void printTimeWindows(int[][] timeWindows){
        System.out.println("TIMEWINDOWS");
        printGrid2(timeWindows[0].length,timeWindows.length,timeWindows);
    }

    //earliest and and latest time
    public static void printTimeWindowsIntervals(int[][] twIntervals){
        System.out.println("TIMEWINDOWS INTERVALS");
        printGrid2(twIntervals[0].length,twIntervals.length,twIntervals);
    }

    //Outer list: length is the number of operations, not including start and end nodes
    //Inner list: first entry is the number of the operation this operation has precedence over, second entry is the number of
    //the operation that has precedence over this operation. Operation numbers goes from 1 to n.
    //Tasks without precedence has a null entry
    public static void printPrecedenceALNS(int[][] precedenceALNS){
        System.out.println("Precedence ALNS");
        printGrid2(precedenceALNS[0].length,precedenceALNS.length,precedenceALNS);
    }

    //Length is the number of operations, not including start and end nodes. Each entry corresponds to one operation and the
    //value of that entry is the operation that has to be performed simultaneously
    public static void printSimALNS(int[] simALNS){
        System.out.println("Simultaneous ALNS");
        System.out.println(Arrays.toString(simALNS));
    }

    //Outer list: length is the number of operations, not including start and end nodes
    //Inner list: first entry is the big task, second and third entry is partial tasks that consolidate the big task.
    //All operations that are either partial or big has this entry, others has null
    public static void printBigTasksALNS(int[][] bigTasksALNS, int nOperations){
        System.out.println("Big tasks ALNS");
        for (int[] inner: bigTasksALNS){
            if (inner==null){
                System.out.println("null");
            }
            else{
                System.out.println(inner[0]+" "+inner[1]+" "+inner[2]);
            }
        }
    }

     /*
    Other information about data

    1. The text files where the operations generated are printed, prints operation null indexed and also includes
    start and end nodes

    2. Static functions that are used by several classes are placed in the end of DataGenerator, as for example
    contains element

    3. Sailingtimes[v][i][j][t] is changed to Sailingtimes[v][t][i][j]
     */

    public static void printGrid(int dim,int[][] matrix)
    {
        for(int i = 0; i < dim; i++)
        {
            for(int j = 0; j < dim; j++)
            {
                System.out.printf("%5d ", matrix[i][j]);
            }
            System.out.println();
        }
    }

    public static void printGrid2(int dim1,int dim2, int[][] matrix)
    {
        for(int i = 0; i < dim2; i++)
        {
            for(int j = 0; j < dim1; j++)
            {
                System.out.printf("%5d ", matrix[i][j]);
            }
            System.out.println();
        }
    }

}
