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

public class ILSResult {

    //all attributes of the class must be defined
    //en variabel for model type --> indekseres med pathflow/arcflow
    //ta inn

    //main info
    public long runTime;
    public long timeInSec;
    public int objective;
    public int heuristicObjective;
    public String instanceName;
    public String weatherfile;
    public String filename = "ILS_results";

    //solution info
    public List<Integer> operationsNotCompleted;
    public List<Integer> operationsNotCompletedAfterHeuristic;
    public Date todaysDate = new Date();  //Todo; check if correct date

    //Model specific info
    public double noiseControlParameter;
    public double randomnessParameterRemoval;
    public double[] removalInterval;
    public int randomSeed;
    public double relatednessWeightDistance;        // a0
    public double relatednessWeightDuration;         // a1
    public int numberOfIterations;
    public int numberOfSegmentIterations;
    public double controlParameter;                  // reaction parameter
    public int reward1;                               // sigma1
    public int reward2;                                // sigma2
    public int reward3;                               // sigma3
    public double lowerThresholdWeights;
    public int earlyPrecedenceFactor;                 //I konstruksjonsheuristikken belønnes precedence
    public int localOptimumIterations;

    // instance specific
    public int numOperations;
    public int numPeriods;
    public int numVessels;
    public int numLocations;
    public String filePath;
    public int iterationsWithoutAcceptance;
    public int ILSIterations;
    public int numberOfImprovementsLocal;
    public int ALNSObj;
    public Boolean infeasibleSearch;


    // constructor
        public ILSResult(long runTime,long timeInSec, int objective, int heuristicObjective, String instanceName, String weatherfile,
                      List<Integer> operationsNotCompleted, List<Integer> operationsNotCompletedAfterHeuristic,
                      double noiseControlParameter,double randomnessParameterRemoval, double[] removalInterval, int randomSeed,
                      double relatednessWeightDistance, double relatednessWeightDuration, int numberOfIterations, int numberOfSegmentIterations,
                      double controlParameter, int reward1, int reward2, int reward3, double lowerThresholdWeights, int earlyPrecedenceFactor, int localOptimumIterations,
                      int numOperations, int numVessels, int numPeriods, int numLocations, int iterationsWithoutAcceptance,
                         int ILSIterations,int numberOfImprovementsLocal,int ALNSObj, Boolean infeasibleSearch) {

        this.runTime  =runTime;
        this.timeInSec = timeInSec;
        this.objective = objective;
        this.heuristicObjective = heuristicObjective;
        this.instanceName = instanceName;
        this.weatherfile = weatherfile;
        this.operationsNotCompleted=operationsNotCompleted;
        this.operationsNotCompletedAfterHeuristic = operationsNotCompletedAfterHeuristic;
        this.noiseControlParameter = noiseControlParameter;
        this.randomnessParameterRemoval = randomnessParameterRemoval;
        this.removalInterval = removalInterval;
        this.randomSeed = randomSeed;
        this.relatednessWeightDistance =relatednessWeightDistance;
        this.relatednessWeightDuration = relatednessWeightDuration;
        this.numberOfIterations = numberOfIterations;
        this.numberOfSegmentIterations = numberOfSegmentIterations;
        this.controlParameter = controlParameter;
        this.reward1 = reward1;
        this.reward2 = reward2;
        this.reward3 = reward3;
        this.lowerThresholdWeights = lowerThresholdWeights;
        this.earlyPrecedenceFactor = earlyPrecedenceFactor;
        this.localOptimumIterations = localOptimumIterations;
        this.iterationsWithoutAcceptance = iterationsWithoutAcceptance;
        this.numOperations=numOperations;
        this.numVessels=numVessels;
        this.numPeriods = numPeriods;
        this.numLocations = numLocations;
        this.filePath = "results/result-files/" + filename  + ".csv";
        this.ILSIterations=ILSIterations;
        this.numberOfImprovementsLocal=numberOfImprovementsLocal;
        this.ALNSObj=ALNSObj;
        this.infeasibleSearch=infeasibleSearch;

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
            String[] CSV_COLUMNS = {"Instance", "Objective Value","Heuristic Objective", "Time in seconds","NanoTime",
                    "Unrouted", "Unrouted after Heuristic", "Weather File",
                    "Date", "noiseControlParameter", "randomnessParameterRemoval", "Removal interval","randomSeed",
                    "relatednessWeightDistance", "relatednessWeightDuration", "numberOfIterations", "numberOfSegmentIterations",
                    "controlParameter", "reward1", "reward2", "reward3", "Num iterations before acceptance","lowerThresholdWeights", "earlyPrecedenceFactor", "localOptimumIterations",
                    "Number of Tasks", "Number of Vessels", "Number of periods", "Number of Locations","ILS Iterations","number Of improvements local","ALNS Objective","Infeasible search"};
            csvWriter.writeNext(CSV_COLUMNS, false);

        }
        String[] results = {instanceName, formatter.format(objective), formatter.format(heuristicObjective), formatter.format(timeInSec),
                formatter.format(runTime), String.valueOf(operationsNotCompleted), String.valueOf(operationsNotCompletedAfterHeuristic),
                weatherfile, date_formatter.format(todaysDate), formatter.format(noiseControlParameter),
                formatter.format(randomnessParameterRemoval), Arrays.toString(removalInterval), formatter.format(randomSeed),
                formatter.format(relatednessWeightDistance), formatter.format(relatednessWeightDuration), formatter.format(numberOfIterations),
                formatter.format(numberOfSegmentIterations), formatter.format(controlParameter), formatter.format(reward1), formatter.format(reward2),
                formatter.format(reward3), formatter.format(iterationsWithoutAcceptance),formatter.format(lowerThresholdWeights),
                formatter.format(earlyPrecedenceFactor), formatter.format(localOptimumIterations),
                formatter.format(numOperations), formatter.format(numVessels), formatter.format(numPeriods), formatter.format(numLocations)
                , formatter.format(ILSIterations), formatter.format(numberOfImprovementsLocal), formatter.format(ALNSObj)
                , formatter.format(infeasibleSearch)};
        csvWriter.writeNext(results, false);
        csvWriter.close();
        writer.close();

    }

    public static void main(String[] args) throws FileNotFoundException {

    }





}

