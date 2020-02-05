
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class Result {
    //all attributes of the class must be defined
    //en variabel for model type --> indekseres med pathflow/arcflow
    //ta inn

    //main info
    public Double runTime;
    public Double objective;
    public Double MIPGap;
    public Double objBound;
    public Double objBoundC;
    public String instanceName;
    public String filename = "result_instances";
    public int isOptimal;
    public int optimistatus;

    //solution info
    public List<Integer> vesselsNotUsed;
    public List<Integer> operationsNotCompleted;
    public int planningDays;
    public Date todaysDate = new Date();  //Todo; check if correct date



    //Model specific info
    public int numRows;
    public int numCol;
    public int continuousVariables;
    public int binaryVariables;
    public int nonZeros;
    public int solCount;
    public double nodeCount;
    public double rootNodeSolution;

    // instance specific
    public int numOperations;
    public int numPeriods;
    public int numVessels;
    public String[][] vesselPath;
    //public double preProcessTime;


    public String filePath;

    // constructor
    public Result(Double runTime, Double objective, Double MIPGap, Double objBound, Double objBoundC, String instanceName, int isOptimal,
                  int optimistatus,
                  List<Integer> vesselsNotUsed, List<Integer> operationsNotCompleted, int planningDays,
                  int numRows, int numCol, int continuousVariables, int binaryVariables, int nonZeros, int solCount,
                  int numOperations, String[][] vesselPath, int numVessels, int numPeriods, double nodeCount) {

        this.runTime  =runTime;
        this.objective = objective;
        this.MIPGap = MIPGap;
        this.objBound = objBound;
        this.objBoundC = objBoundC;
        this.optimistatus = optimistatus;

        this.instanceName = instanceName;
        this.isOptimal = isOptimal;

        this.vesselsNotUsed=vesselsNotUsed;
        this.operationsNotCompleted=operationsNotCompleted;
        this.planningDays=planningDays;

        this.numRows = numRows;
        this.numCol = numCol;
        this.continuousVariables = continuousVariables;
        this.binaryVariables = binaryVariables;
        this.nonZeros = nonZeros;
        this.solCount = solCount;

        this.numOperations=numOperations;
        this.numVessels=numVessels;
        this.vesselPath=vesselPath;
        this.numPeriods = numPeriods;

        this.filePath = "results/result-files/" + filename  + ".csv";
        this.nodeCount = nodeCount;
        //this.preProcessTime = preProcessTime;
        //this.rootNodeSolution = rootNodeSolution;
    }
    //write all attributes of the object to results to a csv file





    public void store() throws IOException {

        File newFile = new File(filePath);
        Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        CSVWriter csvWriter = new CSVWriter(writer, ';', CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);
        NumberFormat formatter = new DecimalFormat("#0.00000000");
        SimpleDateFormat date_formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        if (newFile.length() == 0){
            String[] CSV_COLUMNS = {"Instance", "Objective Value", "Runtime", "Gap", "Objective Bound", "Objective Bound C",
                    "is Optimal", "Optimization Status Code",
                    "Date",
                    "Vessels not Used", "Operations not Completed",
                    "Planning days",
                    "Rows", "Columns", "Continuous Variables", "Binary Variables", "NonZeros", "Solution count",
                    "Number of Tasks", "Number of Vessels", "Path of Vessel" ,"Number of periods", "Node Count"};
            csvWriter.writeNext(CSV_COLUMNS, false);

        }
        String[] results = {instanceName, formatter.format(objective),
                formatter.format(runTime),
                formatter.format(MIPGap), formatter.format(objBound), formatter.format(objBoundC),
                formatter.format(isOptimal), formatter.format(optimistatus),
                date_formatter.format(todaysDate),
                String.valueOf(vesselsNotUsed) , String.valueOf(operationsNotCompleted),
                formatter.format(planningDays) ,  formatter.format(numRows),
                formatter.format(numCol), formatter.format(continuousVariables), formatter.format(binaryVariables),
                formatter.format(nonZeros),formatter.format(solCount),formatter.format(numOperations),
                formatter.format(numVessels), Arrays.deepToString(vesselPath),
                formatter.format(numPeriods), formatter.format(nodeCount)};
        csvWriter.writeNext(results, false);
        csvWriter.close();
        writer.close();

    }


    public static void main(String[] args) throws FileNotFoundException {

    }





}
