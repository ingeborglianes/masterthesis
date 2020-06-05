public class ParameterFile {
    //Insert your parameters and then run with the basic model from the BasicModel class

    //Insert the number of locations for the test instance file used
    public static int loc=20;

    //Insert the filepath of the weatherfile, if you want no weather impact, use the file path for the
    //normal weather file
    public static String weatherFile= "weather_files/weather_september_scaled.txt";
    public static String weatherFile2= "weather_files/weather_januar_scaled.txt";

    //Insert the filepath of the testInstance. If you want to create a new test instance, use the OperationGenerator class
    public static String testInstance ="tuning_instances/60_1_locations(81_140)_.txt";

    // Insert the name of your result-routing-file. In this file the different variables are printed for the results
    // for each test instance you run on one of the models
    // Choose which name you prefer, the file will be generated automatically
    public static String nameResultFile ="results/ALNSroutes/weather_september/";//+testInstance;

    public  static String nameResultFileGurobi = "results/GurobiRoutes/weather_september/";//+testInstance;
    public  static String nameResultFileGurobi2 = "results/GurobiRoutes/weather_january/";//+testInstance;

    //Insert the  number of days in the planning horizon
    public static int days=5;


    //Insert the filepath of the positions.csv file which has all instances between all locations used
    public static String filePathPositionFile= "DistanceMatrixMowiNorway_sorted.csv";


    public static double noiseControlParameter=0.125;
    public static double randomnessParameterRemoval=5;
    public static int numberOfRemoval=15;
    public static double[] removalInterval = new double[]{0.15,0.5};
    public static int randomSeed=50;
    public static double relatednessWeightDistance=0.2;        // a0
    public static double relatednessWeightDuration=1;        // a1
    public static double relatednessWeightTimewindows=0.01;     //Brukes ikke
    public static double relatednessWeightPrecedenceOver=0.1;   //Brukes ikke
    public static double relatednessWeightPrecedenceOf=0.1;     //Brukes ikke
    public static double relatednessWeightSimultaneous=0.1;     //Brukes ikke
    public static int numberOfIterations = 100000;
    public static int numberOfSegmentIterations=100;
    public static double controlParameter=0.5;                  // reaction parameter
    public static int reward1=33;                               // sigma1
    public static int reward2=9;                                // sigma2
    public static int reward3=9;                               // sigma3
    public static double lowerThresholdWeights=0.2;
    public static int earlyPrecedenceFactor=20;                 //I konstruksjonsheuristikken belønnes precedence
    public static int localOptimumIterations=1;
    public static int IterationsWithoutAcceptance=ParameterFile.numberOfIterations+1;
    public static int numberOfILSIterations=10000;
}
