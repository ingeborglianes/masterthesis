public class ParameterFileGurobi {
    //Insert your parameters and then run with the basic model from the BasicModel class

    //Insert the number of locations for the test instance file used
    public static int loc=30;

    //Insert the filepath of the weatherfile, if you want no weather impact, use the file path for the
    //normal weather file
    public static String weatherFile= "weather_files/weather_normal.txt";

    //Insert the filepath of the testInstance. If you want to create a new test instance, use the OperationGenerator class
    public static String testInstance ="test_instances/30_3_locations_normalOpGenerator.txt";

    // Insert the name of your result-routing-file. In this file the different variables are printed for the results
    // for each test instance you run on one of the models
    // Choose which name you prefer, the file will be generated automatically
    public static String nameResultFile ="resultsGurobi.txt";

    //Insert the  number of days in the planning horizon
    public static int days=5;


    //Insert the filepath of the positions.csv file which has all instances between all locations used
    public static String filePathPositionFile= "DistanceMatrixMowiNorway.csv";

}
