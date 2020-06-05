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

public class ALNS_weights {

    //all attributes of the class must be defined
    //en variabel for model type --> indekseres med pathflow/arcflow
    //ta inn

    //main info
    public int iteration;
    public int objective;
    public int bestObjective;
    public String instanceName;
    public String filename = "ALNS_weights_results";

    //solution info
    public Date todaysDate = new Date();  //Todo; check if correct date
    public double[] removalWeights;
    public double[] insertionWeights;
    public String filePath;

    // constructor
    public ALNS_weights(int iteration, int objective, int bestObjective, String instanceName, double[] removalWeights, double[]insertionWeights) {
        this.iteration = iteration;
        this.objective = objective;
        this.bestObjective = bestObjective;
        this.instanceName = instanceName;
        this.removalWeights = removalWeights;
        this.insertionWeights =insertionWeights;
        this.filePath = "results/result-files/" + filename  + ".csv";

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
            String[] CSV_COLUMNS = {"Iteration","Instance", "Current Objective Value","Best Objective",
                    "Date", "Insertion weight 1", "Insertion weight 2", "Insertion weight 3", "Removal weight 1", "Removal weight 2",
                    "Removal weight 3", "Removal weight 4", "Removal weight 5", "Removal weight 6"};
            csvWriter.writeNext(CSV_COLUMNS, false);

        }
        String[] results = {formatter.format(iteration), instanceName, formatter.format(objective), formatter.format(bestObjective),
                date_formatter.format(todaysDate), formatter.format(insertionWeights[0]),formatter.format(insertionWeights[1]),formatter.format(insertionWeights[2]),
                formatter.format(removalWeights[0]), formatter.format(removalWeights[1]),formatter.format(removalWeights[2]),formatter.format(removalWeights[3]),
                formatter.format(removalWeights[4]), formatter.format(removalWeights[5])};
        csvWriter.writeNext(results, false);
        csvWriter.close();
        writer.close();

    }

    public static void main(String[] args) throws FileNotFoundException {

    }





}

