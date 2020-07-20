public class ParameterFile {
    //Insert your parameters and then run with the basic model from the BasicModel class

    //Insert the number of locations for the test instance file used
    public static int loc=20;

    //Insert the  number of days in the planning horizon
    public static int days=5;

    //Insert the filepath of the weatherfile, if you want no weather impact, use the file path for the
    //normal weather file
    public static String weatherFile= "weather_files/weather_perfect.txt";

    //Insert the filepath of the testInstance. If you want to create a new test instance, use the OperationGenerator class
    public static String testInstance ="technical_test_instances/20_1_high_locations(94_113)_.txt";

    //Insert the filepath of the positions.csv file which has all instances between all locations used
    public static String filePathPositionFile= "DistanceMatrixMowiNorway_sorted.csv";

    //File where results such as objective value, computational time etc. are saved
    public static String technicalResultFile="results";

    //ONLY GUROBI
    // Insert the location of your result-routing-file. In this file the routing of the vessels are printed
    // The name of the file will be the name of the test instance
    public static String nameResultFileGurobi = "results/GurobiRoutes/";//+testInstance;
    //The end of the name of the file where we save all gurobi progression, hence each improvement gurobi makes
    public static String logNameAdd="technical_instances";

    //ONLY ALNS, ILS or ALNS+ILS
    // Insert the location of your result-routing-file. In this file the routing of the vessels are printed
    // The name of the file will be the name of the test instance
    public static String nameResultFile ="results/ALNSroutes/";//+testInstance;
    //Local seacrh setup, choose either "ILS" or "LS+ALNS"
    public static String localSearchSetup="ILS";
    //Insert the parameters for the ALNS/ILS/ALNS+ILS, the current values are the tuned values which we used for
    //the runs of the ALNS in the comparison with Gurobi in the master thesis
    public static double noiseControlParameter=0.125;          // noise parameter
    public static double randomnessParameterRemoval=5;         // determinism parameter
    public static double[] removalInterval = new double[]{0.15,0.50};  //removal parameter
    public static double relatednessWeightDistance=0.2;         // a0
    public static double relatednessWeightDuration=1;           // a1
    public static int numberOfIterations = 100000;              // ALNS iterations
    public static int numberOfSegmentIterations=100;            // number of iterations in one segment
    public static double controlParameter=0.5;                  // r, reaction parameter
    public static int reward1=33;                               // sigma1
    public static int reward2=9;                                // sigma2
    public static int reward3=9;                                // sigma3
    public static double lowerThresholdWeights=0.2;             // lower threshold adaptive weights
    public static int earlyPrecedenceFactor=20;                 // early precedence
    public static int numberOfILSIterations=10000;              // ILS iterations

    //Parameters that are not active anymore, do not need to do anything with these
    public static int localOptimumIterations=1;                 //not used
    public static int IterationsWithoutAcceptance=ParameterFile.numberOfIterations+1;    //not used
    public static double relatednessWeightTimewindows=0.01;     //not used
    public static double relatednessWeightPrecedenceOver=0.1;   //not used
    public static double relatednessWeightPrecedenceOf=0.1;     //not used
    public static double relatednessWeightSimultaneous=0.1;     //not used
    public static int randomSeed=50;                            //this is set random for each run in the ALNS and ILS classes

}
