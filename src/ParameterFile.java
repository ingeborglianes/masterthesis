public class ParameterFile {
    //Insert your parameters and then run with the basic model from the BasicModel class

    //Insert the number of locations for the test instance file used
    public static int loc=25;

    //Insert the filepath of the weatherfile, if you want no weather impact, use the file path for the
    //normal weather file
    public static String weatherFile= "weather_files/weather_normal.txt";

    //Insert the filepath of the testInstance. If you want to create a new test instance, use the OperationGenerator class
    public static String testInstance ="test_instances/25_4_locations_normalOpGenerator.txt";

    // Insert the name of your result-routing-file. In this file the different variables are printed for the results
    // for each test instance you run on one of the models
    // Choose which name you prefer, the file will be generated automatically
    public static String nameResultFile ="results.txt";

    //Insert the  number of days in the planning horizon
    public static int days=5;


    //Insert the filepath of the positions.csv file which has all instances between all locations used
    public static String filePathPositionFile= "Positions.csv";

    public static int numberOfRemoval=15;
    public static int randomSeed=50;
    public static double relatednessWeightDistance=0.08;
    public static double relatednessWeightDuration=0.5;
    public static double relatednessWeightTimewindows=0.01;
    public static double relatednessWeightPrecedenceOver=0.1;
    public static double relatednessWeightPrecedenceOf=0.1;
    public static double relatednessWeightSimultaneous=0.1;
    public static int numberOfIterations = 10000;
    public static int numberOfSegmentIterations=100;
    public static double controlParameter=0.1;
    public static int reward1=33;
    public static int reward2=9;
    public static int reward3=13;
    public static double lowerThresholdWeights=1.0;
    public static int iterationsWithoutImprovementParameter=50;
    public static int earlyPrecedenceFactor=20;
    public static int localOptimumIterations=20;
}
