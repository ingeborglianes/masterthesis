import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.IntStream;

public class DataGenerator {
    private int[]vesselsInput;
    private Vessel[] vessels;
    private int days;
    private OperationType[] operationTypes;
    private Vessel[] vesselTypes;
    private Operation[] operations;
    private int[][][][] sailingTimes;
    private int[][] distOperationsInInstance;
    private Map<Integer, List<OperationType>> logOperations=new HashMap<Integer, List<OperationType>>();
    private int [][] simultaneous;
    private int [][] precedence;
    private Map<Integer, List<Integer>> consolidatedTasks=new HashMap<Integer, List<Integer>>();
    private List<Integer> bigTasks=new ArrayList<Integer>();
    private int[] bigTasksArr;
    private int [][][] operationGain;
    private int [][][] operationGainGurobi;
    private int [] sailingCostForVessel;
    private int [] earliestStartingTimeForVessel;
    private int[][][] edges;
    private int[][] operationsForVessel;
    private int[][] timeWindowsForOperations;
    private int[][][]timeVesselUseOnOperation;
    private int[][] distanceArray;
    private int nStartNodes;
    private int nEndNodes;
    private int[] locationsStartNodes;
    private int[] endNodes;
    private int[] startNodes;
    private double[] endPenaltyforVessel;
    private List<List<Integer>> distanceMatrix= new ArrayList<>();
    private String filePath;
    private Map<int[],Integer> operationTime= new HashMap<>();
    private int maxDistance=0;
    public static int maxSailingTime=0;
    public static int costPenalty=66;
    private String fileNameRouting;
    private Double [][] weatherPenaltyOperations;
    private int [][] weatherPenaltySpeed;
    private String weatherFile;
    private int[][] twIntervals;
    private int[][] precedenceALNS;
    private int[][] simultaneousALNS;
    private int[][] bigTasksALNS;
    private int simNumber;
    private int presNumber;
    private String fileTasktypes ="tasktypes.txt";

    public DataGenerator(int[] vessels, int days, int[] locationsStartNodes, String filePath, String fileNameRouting, String weatherFile){
        this.vesselsInput=vessels;
        this.days=days;
        this.nStartNodes=this.vesselsInput.length;
        this.nEndNodes=this.vesselsInput.length;
        this.locationsStartNodes=locationsStartNodes;
        this.filePath=filePath;
        this.fileNameRouting=fileNameRouting;
        this.weatherFile=weatherFile;
        this.weatherPenaltyOperations=new Double[vessels.length][days*12];
        this.weatherPenaltySpeed=new int[vessels.length][days*12];
        this.simNumber=0;
        this.presNumber=0;
    }

    public void importWeather() throws FileNotFoundException {
        Scanner s = new Scanner(new File(this.weatherFile));
        double[] weatherValues = new double[60];
        int count=0;
        while (s.hasNextLine()){
            List<String> weather = new ArrayList<String>();
            weather.add(s.nextLine());
            String[] weatherStr = weather.get(0).split("\\s+");
            weatherValues[count]=Double.parseDouble(weatherStr[1]);
            count+=1;
        }
        s.close();
        /*
        for (int wh=0;wh<weatherValues.length;wh++){
            if(weatherValues[wh]<=1.5){
                weatherPenaltyOperations[wh] = 1.0;
                weatherPenaltySpeed[wh]=0;
            }
            else if(weatherValues[wh]>1.5 && weatherValues[wh]<=2.5){
                weatherPenaltyOperations[wh] = 1.2;
                weatherPenaltySpeed[wh]=0;
            }
            else if(weatherValues[wh]>2.5 && weatherValues[wh]<=3.5) {
                weatherPenaltyOperations[wh] = 1.3;
                weatherPenaltySpeed[wh]=2;
            }
            //here we need to change time vessel use on operation, not time window
            else{
                weatherPenaltyOperations[wh] = 0.0;
                weatherPenaltySpeed[wh]=3;
            }
        }*/
        for(int i=0;i<vesselsInput.length;i++) {
            for (int wh=0;wh<weatherValues.length;wh++){
                if(vesselsInput[i]<5) {
                    if (weatherValues[wh] <= 0.25) {
                        weatherPenaltyOperations[i][wh] = 1.0;
                        weatherPenaltySpeed[i][wh] = 0;
                    } else if (weatherValues[wh] > 0.25 && weatherValues[wh] <= 0.5) {
                        weatherPenaltyOperations[i][wh] = 0.8;
                        weatherPenaltySpeed[i][wh] = 0;
                    } else if (weatherValues[wh] > 0.5 && weatherValues[wh] <= 1) {
                        weatherPenaltyOperations[i][wh] = 0.7;
                        weatherPenaltySpeed[i][wh] = 2;
                    }
                    //here we need to change time vessel use on operation, not time window
                    else {
                        weatherPenaltyOperations[i][wh] = 0.0;
                        weatherPenaltySpeed[i][wh] = 3;
                    }
                }
                else{
                    if (weatherValues[wh] <= 0.5) {
                        weatherPenaltyOperations[i][wh] = 1.0;
                        weatherPenaltySpeed[i][wh] = 0;
                    } else if (weatherValues[wh] > 0.5 && weatherValues[wh] <= 1) {
                        weatherPenaltyOperations[i][wh] = 0.8;
                        weatherPenaltySpeed[i][wh] = 0;
                    } else if (weatherValues[wh] > 1 && weatherValues[wh] <= 1.5) {
                        weatherPenaltyOperations[i][wh] = 0.7;
                        weatherPenaltySpeed[i][wh] = 2;
                    }
                    //here we need to change time vessel use on operation, not time window
                    else {
                        weatherPenaltyOperations[i][wh] = 0.0;
                        weatherPenaltySpeed[i][wh] = 3;
                    }
                }
            }
        }
        //for (int d=0;d<weatherValues.length;d++){
        //System.out.println("Wave height: "+weatherValues[d]+" Operation penalty: "+weatherPenaltyOperations[d]+
        //      " Speed penalty: "+weatherPenaltySpeed);
        //}
    }

    public void createDistance() throws FileNotFoundException {
        Scanner s = new Scanner(new File(ParameterFile.filePathPositionFile));
        int countInner=0;
        int countOuter=0;
        while (s.hasNextLine()){
            List<Integer> temp = new ArrayList<Integer>();
            List<String> distance1 = new ArrayList<String>();
            distance1.add(s.nextLine());
            String[] distances = distance1.get(0).split(";");
            for(String dist : distances){
                dist=removeSpace(dist);
                int distInt=0;
                if(dist.contains(",")) {
                    String distReplaced = dist.replace(',', '.');
                    distInt = (int) Math.ceil(Double.parseDouble(distReplaced.trim()));
                    if(distInt>maxDistance){
                        maxDistance=distInt;
                        //System.out.println(countInner+" and "+countOuter);
                    }
                }
                temp.add(distInt);
                countInner+=1;
            }
            distanceMatrix.add(temp);
            countInner=0;
            countOuter+=1;
        }
        s.close();
        this.distanceArray=new int[distanceMatrix.size()][distanceMatrix.size()];
        maxSailingTime=(int) Math.ceil(maxDistance/(10*1.85));
        for (int n =0;n<distanceMatrix.size();n++){
            for (int i =0;i<distanceMatrix.size();i++){
                int distance=distanceMatrix.get(n).get(i);
                this.distanceArray[n][i]=distance;
            }
        }
    }

    public void runOperationGenerator(){
        OperationGenerator og=new OperationGenerator();
        //REMEMBER TO CHOOSE TEST OR NOT HERE
        this.operationTypes=og.createOperationTypes();
        //this.operationTypes=og.createOperationTypesTest();
    }

    public void readInstance() throws FileNotFoundException {
        Scanner s = new Scanner(new File(this.filePath));
        while (s.hasNextLine()){
            List<String> loc = new ArrayList<String>();
            loc.add(s.nextLine());
            String[] locStr = loc.get(0).split("\\s+");
            if (Integer.parseInt(locStr[0]) != 0) {
                for (int n = 1; n < locStr.length; n++) {
                    if (logOperations.get(Integer.parseInt(locStr[0])) != null) {
                        logOperations.get(Integer.parseInt(locStr[0])).add(findOperationType(Integer.parseInt(locStr[n])));
                    } else {
                        int finalN = n;
                        logOperations.put(Integer.parseInt(locStr[0]),new ArrayList<OperationType>(){{add(findOperationType(Integer.parseInt(locStr[finalN])));}});
                    }
                }
            }
            else if (Integer.parseInt(locStr[0])==0){
                int[] keyList=new int[]{Integer.parseInt(locStr[1]),Integer.parseInt(locStr[2])};
                operationTime.put(keyList,Integer.parseInt(locStr[3]));
            }
        }
        s.close();
        for (List<OperationType> opLoc:logOperations.values()){
            //System.out.println("New sorted list");
            int numberOfOperations=opLoc.size();
            opLoc.sort(Comparator.comparing(OperationType::getNumber));
            /*
            for(int i =1;i<numberOfOperations;i++){
                if (opLoc.get(i).getNumber()<opLoc.get(i-1).getNumber()){
                    Collections.swap(opLoc, i-1, i);
                }
            }
             */
            /*
            for(int i =0;i<numberOfOperations;i++){
                System.out.println("Type: "+opLoc.get(i).getNumber());
            }
             */
        }
    }

    public void createVesselTypes(){
        Vessel v1= new Vessel(1,10.0*1.85,66,0,1.0,1, 0.1 );
        Vessel v2= new Vessel(2,9.0*1.85,42,0,1.0,2, 0.1);
        Vessel v3= new Vessel(3,12.0*1.85,55,0,1.1,3, 0.2);
        Vessel v4= new Vessel(4,10.0*1.85,56,0,1.1,4, 0.2);
        Vessel v5= new Vessel(5,8.0*1.85,50,0,1.2,5,0.3);
        Vessel v6= new Vessel(6,10.0*1.85,55,0,1.2,6, 0.3);
        this.vesselTypes=new Vessel[]{v1,v2,v3,v4,v5,v6};
    }

    public void createVesselObjects(){
        Vessel [] vessels = new Vessel[this.vesselsInput.length];
        int appendIndex=0;
        for (int v : this.vesselsInput){
            for (Vessel vType : this.vesselTypes){
                if(v==vType.getNum()){
                    vessels[appendIndex]=new Vessel(appendIndex+1,vType.getSpeed(),vType.getSailingCost(),
                            vType.getEarliestStartingTime(),vType.getTimePenalty(),vType.getNum(), vType.getEndPenalty());
                    appendIndex+=1;
                }
            }
        }
        this.vessels=vessels;
    }

    public OperationType findOperationType(int number){
        OperationType matchOp=operationTypes[0];
        for (OperationType op : operationTypes){
            if (op.getNumber()==number){
                matchOp=op;
            }
        }
        return matchOp;
    }

    public Vessel findVesselObject(int number){
        Vessel matchV=vessels[0];
        for (Vessel v : vessels){
            if (v.getNum()==number){
                matchV=v;
            }
        }
        return matchV;
    }

    public int returnTimeOfOperation(int[] locOpList){
        int t=0;
        for (Map.Entry<int[], Integer> item : operationTime.entrySet()) {
            int[] key =item.getKey();
            int value = item.getValue();
            if (key[0]==locOpList[0] && key[1]==locOpList[1]){
                t=value;
            }
        }
        return t;
    }

    public void generateOperations() {
        List<int[]> bigTasksALNSTemp = new ArrayList<>();
        List<Operation> operations = new ArrayList<Operation>();
        int opNumber = 1;
        int[] tw = IntStream.rangeClosed(1, this.days * 12).toArray();
        List<String> routing = new ArrayList<>();
        int numberOfS = 0;
        int numberOfC = 0;
        int numberOfP = 0;
        ArrayList<List<Integer>> consolidatedTasksNumber = new ArrayList<List<Integer>>();
        ArrayList<List<Integer>> simultaneousTasksNumber = new ArrayList<List<Integer>>();
        for (Integer location : this.logOperations.keySet()) {
            for (OperationType opType : this.logOperations.get(location)) {
                if (opType.getVessel2() == null && opType.getVesselBigTask() == null) {
                    Operation op = new Operation(opNumber, opType.getVessel1(), location, 0, null,
                            opType.getPrecedenceOver(), tw, opType.getDuration(),
                            opType.getNumber(), opType.getOperationGain(), opType.getName());
                    if (opType.getPrecedenceOver() != 0) {
                        numberOfP += 1;
                    } else if (opType.getPrecedenceOf() != 0) {
                        numberOfP += 1;
                    }
                    if (opType.getTimewindowInterval() != 0) {
                        int[] keyTime = new int[]{location, opType.getNumber()};
                        int t = returnTimeOfOperation(keyTime);
                        int startIndex = Math.max(1, t - opType.getTimewindowInterval());
                        int endIndex = Math.min(this.days * 12 - 1, t);
                        if (startIndex == 1) {
                            endIndex = opType.getTimewindowInterval();
                        }
                        if (endIndex == this.days * 12 - 1) {
                            startIndex = this.days * 12 - opType.getTimewindowInterval();
                        }
                        int[] twOp = new int[this.days * 12];
                        for (int i = startIndex; i < endIndex; i++) {
                            twOp[i - 1] = i;
                        }
                        op = new Operation(opNumber, opType.getVessel1(), location, 0, null,
                                opType.getPrecedenceOver(), twOp, opType.getDuration(),
                                opType.getNumber(), opType.getOperationGain(), opType.getName());
                    }
                    operations.add(op);
                    bigTasksALNSTemp.add(null);
                    this.consolidatedTasks.put(op.getNumber() + nStartNodes, new ArrayList<Integer>() {{
                    }});
                    this.bigTasks.add(op.getNumber() + nStartNodes);
                    routing.add("Operation: " + String.valueOf(opNumber + nStartNodes) +
                            " Precedence: " + String.valueOf(op.getPrecedence()) + " Location: " + String.valueOf(op.getLocation()) +
                            " optype: " + String.valueOf(op.getType()) + " bigTaskSet: " + String.valueOf(op.getBigTaskSet()) +
                            "Sim: " + String.valueOf(op.getSimultaneous()) + " Vessels: " + Arrays.toString(op.getVessels())
                            + " Duration: " + op.getDuration() + " Task description: " + op.getName());
                    opNumber += 1;
                } else if (opType.getVessel2() != null && opType.getVesselBigTask() == null) {
                    Operation op1 = new Operation(opNumber, opType.getVessel1(), location, opNumber + 1, null,
                            opType.getPrecedenceOver(), tw, opType.getDuration(),
                            opType.getNumber(), opType.getOperationGain(), opType.getName() + " Part 1");
                    operations.add(op1);
                    if (opType.getPrecedenceOver() != 0) {
                        numberOfP += 2;
                    } else if (opType.getPrecedenceOf() != 0) {
                        numberOfP += 2;
                    }
                    numberOfS += 2;
                    bigTasksALNSTemp.add(null);
                    routing.add("Operation: " + String.valueOf(opNumber + nStartNodes) +
                            " Precedence: " + String.valueOf(op1.getPrecedence()) + " Location: " + String.valueOf(op1.getLocation()) +
                            " optype: " + String.valueOf(op1.getType()) + " bigTaskSet: " + String.valueOf(op1.getBigTaskSet()) +
                            "Sim: " + String.valueOf(op1.getSimultaneous() + nStartNodes) + " Vessels: " + Arrays.toString(op1.getVessels()) + " Duration: " + op1.getDuration() + " Task description: " + op1.getName());
                    this.consolidatedTasks.put(op1.getNumber() + nStartNodes, new ArrayList<Integer>() {{
                    }});
                    this.bigTasks.add(op1.getNumber() + nStartNodes);
                    opNumber += 1;
                    Operation op2 = new Operation(opNumber, opType.getVessel2(), location, opNumber - 1, null,
                            opType.getPrecedenceOver(), tw, opType.getDuration(),
                            opType.getNumber(), opType.getOperationGain(), opType.getName() + " Part 2");
                    operations.add(op2);
                    bigTasksALNSTemp.add(null);
                    routing.add("Operation: " + String.valueOf(opNumber + nStartNodes) +
                            " Precedence: " + String.valueOf(op2.getPrecedence()) + " Location: " + String.valueOf(op2.getLocation()) +
                            " optype: " + String.valueOf(op2.getType()) + " bigTaskSet: " + String.valueOf(op2.getBigTaskSet()) +
                            "Sim: " + String.valueOf(op2.getSimultaneous() + nStartNodes) + " Vessels: " + Arrays.toString(op2.getVessels()) + " Duration: " + op2.getDuration() + " Task description: " + op2.getName());
                    this.consolidatedTasks.put(op2.getNumber() + nStartNodes, new ArrayList<Integer>() {{
                    }});
                    this.bigTasks.add(op2.getNumber() + nStartNodes);
                    opNumber += 1;
                    ArrayList<String> list = new ArrayList<String>() {{
                        add("A");
                        add("B");
                        add("C");
                    }};
                    simultaneousTasksNumber.add(new ArrayList<Integer>() {{
                        add(op1.getNumber());
                        add(op2.getNumber());
                    }});
                } else if (opType.getVesselBigTask() != null && opType.getVessel2() != null) {
                    numberOfC += 1;
                    Operation opSmall1 = new Operation(opNumber, opType.getVessel1(), location, opNumber + 1, null,
                            opType.getPrecedenceOver(), tw, opType.getDuration(),
                            opType.getNumber(), opType.getOperationGain() / 2, opType.getName() + " Part 1 of big task operation");
                    operations.add(opSmall1);
                    bigTasksALNSTemp.add(new int[]{opNumber + nStartNodes + 2, opNumber + nStartNodes, opNumber + nStartNodes + 1});
                    routing.add("Operation: " + String.valueOf(opNumber + nStartNodes) +
                            " Precedence: " + String.valueOf(opSmall1.getPrecedence()) + " Location: " + String.valueOf(opSmall1.getLocation()) +
                            " optype: " + String.valueOf(opSmall1.getType()) + " bigTaskSet: " + String.valueOf(opSmall1.getBigTaskSet()) +
                            "Sim: " + String.valueOf(opSmall1.getSimultaneous() + nStartNodes) + " Vessels: " + Arrays.toString(opSmall1.getVessels()) + " Duration: " + opSmall1.getDuration() + " Task description: " + opSmall1.getName());
                    opNumber += 1;
                    Operation opSmall2 = new Operation(opNumber, opType.getVessel2(), location, opNumber - 1, null,
                            opType.getPrecedenceOver(), tw, opType.getDuration(),
                            opType.getNumber(), opType.getOperationGain() / 2, opType.getName() + " Part 2 of big task operation");
                    operations.add(opSmall2);
                    bigTasksALNSTemp.add(new int[]{opNumber + 1 + nStartNodes, opNumber - 1 + nStartNodes, opNumber + nStartNodes});
                    routing.add("Operation: " + String.valueOf(opNumber + nStartNodes) +
                            " Precedence: " + String.valueOf(opSmall2.getPrecedence()) + " Location: " + String.valueOf(opSmall1.getLocation()) +
                            " optype: " + String.valueOf(opSmall2.getType()) + " bigTaskSet: " + Arrays.toString(opSmall2.getBigTaskSet()) +
                            " Sim: " + String.valueOf(opSmall2.getSimultaneous() + nStartNodes) + " Vessels: " + Arrays.toString(opSmall2.getVessels()) + " Duration: " + opSmall2.getDuration() + " Task description: " + opSmall2.getName());
                    opNumber += 1;
                    int[] bigTasksArray = new int[]{opSmall1.getNumber(), opSmall2.getNumber()};
                    Operation opBig = new Operation(opNumber, opType.getVesselBigTask(), location, 0, bigTasksArray,
                            opType.getPrecedenceOver(), tw, opType.getDuration(),
                            opType.getNumber(), opType.getOperationGain(), opType.getName() + " Big task operation");
                    operations.add(opBig);
                    bigTasksALNSTemp.add(new int[]{opNumber + nStartNodes, opNumber - 2 + nStartNodes, opNumber - 1 + nStartNodes});
                    routing.add("Operation: " + String.valueOf(opNumber + nStartNodes) +
                            " Precedence: " + String.valueOf(opBig.getPrecedence()) + " Location: " + String.valueOf(opBig.getLocation()) +
                            " optype: " + String.valueOf(opBig.getType()) + " bigTaskSet: " + Arrays.toString(opBig.getBigTaskSet()) +
                            "Sim: " + String.valueOf(opBig.getSimultaneous()) + " Vessels: " + Arrays.toString(opBig.getVessels()) + " Duration: " + opBig.getDuration() + " Task description: " + opBig.getName());
                    this.consolidatedTasks.put(opBig.getNumber() + nStartNodes, new ArrayList<Integer>() {{
                        add(opSmall1.getNumber() + nStartNodes);
                        add(opSmall2.getNumber() + nStartNodes);
                    }});
                    this.bigTasks.add(opBig.getNumber() + nStartNodes);
                    opNumber += 1;
                    consolidatedTasksNumber.add(new ArrayList<Integer>() {{
                        add(opSmall1.getNumber());
                        add(opSmall2.getNumber());
                        add(opBig.getNumber());
                    }});
                }
            }
        }
        Operation[] operations2 = new Operation[operations.size()];
        bigTasksALNS = new int[bigTasksALNSTemp.size()][3];
        for (int b = 0; b < bigTasksALNSTemp.size(); b++) {
            if (bigTasksALNSTemp.get(b) != null) {
                bigTasksALNS[b] = new int[]{bigTasksALNSTemp.get(b)[0], bigTasksALNSTemp.get(b)[1], bigTasksALNSTemp.get(b)[2]};
            } else {
                bigTasksALNS[b] = null;
            }
        }
        int addIndex = 0;
        for (Operation op : operations) {
            operations2[addIndex] = op;
            addIndex += 1;
        }
        routing.add(" ");
        for (Vessel v : vessels) {
            //System.out.println("Vessel "+ v.getNum()+" is vessel type "+v.getVesselType());
            routing.add("Vessel " + v.getNum() + " is vessel type " + v.getVesselType());
        }
        routing.add(" ");

        String taskTypes = filePath+" " +String.valueOf(numberOfS)+" "+String.valueOf(numberOfC)+" "+String.valueOf(numberOfP)+" ";

        for (List<Integer> sim : simultaneousTasksNumber){
            int[] simTasks =new int[2];
            simTasks[0]=sim.get(0);
            simTasks[1]=sim.get(1);
            taskTypes+= Arrays.toString(simTasks);
            taskTypes+=" ";
        }

        for (List<Integer> con : consolidatedTasksNumber){
            int[] conTasks =new int[3];
            conTasks[0]=con.get(0);
            conTasks[1]=con.get(1);
            conTasks[2]=con.get(2);
            taskTypes+= Arrays.toString(conTasks);
            taskTypes+=" ";
        }

        try(FileWriter fw = new FileWriter(fileTasktypes, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(taskTypes);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }

        try(FileWriter fw = new FileWriter(fileNameRouting, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            for(String s : routing) {
                out.println(s);
            }
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
        this.operations=operations2;
        this.distOperationsInInstance=new int[this.operations.length][this.operations.length];
    }

    public void createPrecedence(){
        int nOperations=this.operations.length;
        int nVessels=this.vessels.length;
        int[][] precedence=new int[nOperations+2*nVessels][nOperations+2*nVessels];
        precedenceALNS=new int[nOperations][2];
        for (Operation op1 : this.operations){
            for (Operation op2 : this.operations){
                if (op1.getPrecedence()==op2.getType() && op1.getLocation()==op2.getLocation()){
                    int num1 = nStartNodes + (int) (op1.getNumber()) - 1;
                    int num2 = nStartNodes + (int) (op2.getNumber()) - 1;
                    precedence[num1][num2]=1;
                    if(precedenceALNS[op1.getNumber()-1][0]==0 && precedenceALNS[op1.getNumber()-1][1]==0){
                        presNumber+=1;
                    }
                    if(precedenceALNS[op2.getNumber()-1][0]==0 && precedenceALNS[op2.getNumber()-1][1]==0){
                        presNumber+=1;
                    }
                    precedenceALNS[op1.getNumber()-1][0]=num2+1;
                    precedenceALNS[op2.getNumber()-1][1]=num1+1;
                }
            }
        }
        this.precedence=precedence;
    }

    public void createSimultaneous(){
        int nOperations=this.operations.length;
        int nVessels=this.vessels.length;
        int[][] sim=new int[nOperations+2*nVessels][nOperations+2*nVessels];
        simultaneousALNS=new int[nOperations][2];
        for (Operation op1 : this.operations){
            for (Operation op2 : this.operations){
                if(op1.getSimultaneous()==op2.getNumber()){
                    int num1=nStartNodes + (int) (op1.getNumber()) - 1;
                    int num2=nStartNodes + (int)(op2.getNumber()) - 1;
                    sim[num1][num2]=1;
                    if(simultaneousALNS[op1.getNumber()-1][0]==0 && simultaneousALNS[op1.getNumber()-1][1]==0){
                        simNumber+=1;
                    }
                    if(simultaneousALNS[op2.getNumber()-1][0]==0 && simultaneousALNS[op2.getNumber()-1][1]==0){
                        simNumber+=1;
                    }
                    if(op1.getNumber() < op2.getNumber()){
                        simultaneousALNS[op1.getNumber()-1][0]=num2+1;
                        simultaneousALNS[op2.getNumber()-1][1]=num1+1;
                    }
                }
            }
        }
        this.simultaneous=sim;
    }

    public void createBigTasks(){
        int addIndex=0;
        int [] bigTasksArr=new int[this.bigTasks.size()];
        for (Integer i:this.bigTasks){
            bigTasksArr[addIndex]=i;
            addIndex+=1;
        }
        this.bigTasksArr=bigTasksArr;
    }



    public void createVesselData(){
        int[][] opForVessel=new int[this.vessels.length][this.operations.length];
        this.sailingCostForVessel=new int[vessels.length];
        this.earliestStartingTimeForVessel=new int[vessels.length];
        this.endPenaltyforVessel = new double[vessels.length];
        for (Vessel vessel : this.vessels){
            this.sailingCostForVessel[vessel.getNum()-1]=vessel.getSailingCost();
            this.earliestStartingTimeForVessel[vessel.getNum()-1]=vessel.getEarliestStartingTime();
            this.endPenaltyforVessel[vessel.getNum()-1]=vessel.getEndPenalty();
            for(Operation op:this.operations){
                if(containsElement(vessel.getVesselType(),op.getVessels())){
                    opForVessel[vessel.getNum()-1][op.getNumber()-1]=op.getNumber()+nStartNodes;
                }
            }
        }
        this.operationsForVessel=opForVessel;
        int [][][] timeOpVessel=new int [this.vessels.length][this.operations.length][this.weatherPenaltySpeed[0].length];
        for (Vessel vessel : this.vessels){
            for (Operation op : this.operations){
                for(int t=0;t<weatherPenaltySpeed[0].length;t++) {
                    if (containsElement(op.getNumber() + nStartNodes, this.operationsForVessel[vessel.getNum() - 1])) {
                        double opDuration;
                        if (op.getSimultaneous() == 0) {
                            opDuration = vessel.getTimePenalty() * op.getDuration();
                            /*Gammel:
                            timeOpVessel[vessel.getNum() - 1][op.getNumber() - 1][t] = (int) Math.round(vessel.getTimePenalty() * op.getDuration()*weatherPenaltyOperations[t]);
                            //System.out.println("vessel "+vessel.getNum()+ " op "+op.getNumber()+" time "+t+" penaltyV: "+vessel.getTimePenalty()+" penaltyW "+weatherPenaltyOperations[t] +
                            //      " Duration "+op.getDuration());*/
                        } else {
                            int[] v1 = findOperationType(op.getNumber()).getVessel1();
                            int[] v2 = findOperationType(op.getNumber()).getVessel1();
                            double maxV1 = findVesselObject(v1[v1.length - 1]).getTimePenalty();
                            double maxV2 = findVesselObject(v2[v2.length - 1]).getTimePenalty();
                            double penalty = Math.max(maxV1, maxV2);
                            opDuration = penalty*op.getDuration();
                            /*Gammel:
                            int[] v1 = findOperationType(op.getNumber()).getVessel1();
                            int[] v2 = findOperationType(op.getNumber()).getVessel1();
                            double maxV1 = findVesselObject(v1[v1.length - 1]).getTimePenalty();
                            double maxV2 = findVesselObject(v2[v2.length - 1]).getTimePenalty();
                            double penalty = Math.max(maxV1, maxV2);
                            timeOpVessel[vessel.getNum() - 1][op.getNumber() - 1][t] = (int) Math.round(penalty * op.getDuration()*weatherPenaltyOperations[t]);*/
                        }
                        double opTime = 1*weatherPenaltyOperations[vessel.getNum()-1][t];
                        int k = 1;
                        while(opTime < opDuration){
                            opTime = opTime+weatherPenaltyOperations[vessel.getNum()-1][Math.min(this.weatherPenaltySpeed[0].length-1,t+k)];
                            k++;
                            if(t+k > (this.weatherPenaltySpeed[0].length + (2*opDuration))){
                                k = 10000;
                                break;
                            }
                        }
                        timeOpVessel[vessel.getNum() - 1][op.getNumber() - 1][t] = k;
                    }
                }
            }
        }
        this.timeVesselUseOnOperation=timeOpVessel;

    }

    public void createOperationGain(){
        int[][][] gains= new int[vessels.length][this.operations.length][this.days*12];
        int[][][] gainsGurobi = new int[vessels.length][this.operations.length][this.days*12];
        for(int v=0;v<this.vessels.length;v++) {
            for (Operation op : this.operations) {
                int[] keyTime=new int[]{op.getLocation(),op.getType()};
                int drawnTime=returnTimeOfOperation(keyTime);
                for (int t = 0; t < op.getTimeWindow().length; t++) {
                    if (t + timeVesselUseOnOperation[v][op.getNumber()-1][t] < op.getTimeWindow().length) {
                        if(drawnTime < (this.days*12)/3) {
                            gains[v][op.getNumber() - 1][t] = (int) Math.round(op.getOperationGain() * 1.4);
                        }else if(drawnTime < (2*this.days*12)/3){
                            gains[v][op.getNumber() - 1][t] = (int) Math.round(op.getOperationGain() * 1.2);
                        }else if(drawnTime < this.days*12){
                            gains[v][op.getNumber() - 1][t] = op.getOperationGain();
                        }

                        if(simultaneousALNS[op.getNumber()-1][1]!=0 && bigTasksALNS[op.getNumber()-1]==null){
                            gainsGurobi[v][op.getNumber()-1][t] = 0;
                        }else{
                            if(drawnTime < (this.days*12)/3) {
                                gainsGurobi[v][op.getNumber() - 1][t] = (int) Math.round(op.getOperationGain() * 1.4);
                            }else if(drawnTime < (2*this.days*12)/3){
                                gainsGurobi[v][op.getNumber() - 1][t] = (int) Math.round(op.getOperationGain() * 1.2);
                            }else if(drawnTime < this.days*12){
                                gainsGurobi[v][op.getNumber() - 1][t] = op.getOperationGain();
                            }
                        }

                    } else {
                        int work_time = op.getTimeWindow().length - t;
                        int op_duration = timeVesselUseOnOperation[v][op.getNumber()-1][t];
                        if(drawnTime < (this.days*12)/3) {
                            gains[v][op.getNumber()-1][t] = (int) Math.round((op.getOperationGain() / op_duration) * work_time * 1.4);
                        }else if(drawnTime < (2*this.days*12)/3){
                            gains[v][op.getNumber()-1][t] = (int) Math.round((op.getOperationGain() / op_duration) * work_time * 1.2);
                        }else if(drawnTime < this.days*12){
                            gains[v][op.getNumber()-1][t] = Math.round((op.getOperationGain() / op_duration) * work_time);
                        }

                        if(simultaneousALNS[op.getNumber()-1][1]!=0 && bigTasksALNS[op.getNumber()-1]==null){
                            gainsGurobi[v][op.getNumber()-1][t] = 0;
                        }else{
                            if(drawnTime < (this.days*12)/3) {
                                gainsGurobi[v][op.getNumber()-1][t] = (int) Math.round((op.getOperationGain() / op_duration) * work_time * 1.4);
                            }else if(drawnTime < (2*this.days*12)/3){
                                gainsGurobi[v][op.getNumber()-1][t] = (int) Math.round((op.getOperationGain() / op_duration) * work_time * 1.2);
                            }else if(drawnTime < this.days*12){
                                gainsGurobi[v][op.getNumber()-1][t] = Math.round((op.getOperationGain() / op_duration) * work_time);
                            }
                        }
                    }
                }
            }
        }
        this.operationGain=gains;
        this.operationGainGurobi=gainsGurobi;
    }

    public void createTimeWindows(){
        int [][] timeWindows= new int[this.operations.length+nStartNodes+nEndNodes][this.days*12];
        twIntervals = new int[this.operations.length][2];
        for (int[] operation: twIntervals){
            operation[0]=0;
            operation[1]=days*12;
        }
        for (Operation op:this.operations){
            for(int t=0;t<op.getTimeWindow().length;t++){
                timeWindows[op.getNumber()+nStartNodes-1][t]=op.getTimeWindow()[t];
                if(t>0 && op.getTimeWindow()[t-1]==0 && op.getTimeWindow()[t]!=0){
                    twIntervals[op.getNumber()-1][0]=op.getTimeWindow()[t];
                }
                if(t<op.getTimeWindow().length-1 && op.getTimeWindow()[t]!=0 && op.getTimeWindow()[t+1]==0){
                    twIntervals[op.getNumber()-1][1]=op.getTimeWindow()[t];
                }
            }
        }
        this.timeWindowsForOperations=timeWindows;
    }

    public void createEdges(){
        int[][][] edges = new int[vessels.length][this.operations.length + nStartNodes + nEndNodes][this.operations.length + nStartNodes + nEndNodes];
        for(int v=0;v<this.vessels.length;v++) {
            int nOperations = this.operations.length;
            for (int n = 0; n < nOperations + nStartNodes + nEndNodes; n++) {
                for (int i = 0; i < nOperations + nStartNodes + nEndNodes; i++) {
                    if(!BasicModel.containsElement(n+1,startNodes)&&!BasicModel.containsElement(n+1,endNodes)&&!BasicModel.containsElement(i+1,startNodes)
                            &&!BasicModel.containsElement(i+1,endNodes)) {
                        if (BasicModel.containsElement(n+1,operationsForVessel[v]) && BasicModel.containsElement(i+1,operationsForVessel[v]) && n != i) {
                            edges[v][n][i] = 1;
                        }
                    }
                    else if(n==v){
                        if (BasicModel.containsElement(i+1,operationsForVessel[v]) || i == (nStartNodes+nEndNodes+nOperations-1-v)) {
                            edges[v][n][i] = 1;
                        }
                    }
                    else if(i==(nStartNodes+nEndNodes+nOperations-1-v)) {
                        if(BasicModel.containsElement(n+1,operationsForVessel[v])){
                            edges[v][n][i] = 1;

                        }
                    }
                }
            }
        }
        this.edges=edges;
        /*
        for(int v=0;v<this.vessels.length;v++) {
            System.out.println("EDGES for vessel: " + v);
            this.printGrid2(this.edges[0][0].length,edges[0].length,this.edges[v]);
        }
        */
    }

    public void createSailingTimes(){
        int[][][][] sailingTimes = new int[nStartNodes][weatherPenaltySpeed[0].length][operations.length+nStartNodes+nEndNodes][operations.length+nStartNodes+nEndNodes];
        for(Vessel v:this.vessels) {
            for (int t = 0; t < weatherPenaltySpeed[0].length; t++) {
                for (Operation o1 : this.operations) {
                    for (Operation o2 : this.operations) {
                        double sailingDist = distanceArray[o1.getLocation() - 1][o2.getLocation() - 1];
                        double coveredDist = 0;
                        int k=0;
                        while(coveredDist<sailingDist){
                            coveredDist = coveredDist+(v.getSpeed()-weatherPenaltySpeed[v.getNum()-1][Math.min(weatherPenaltySpeed[0].length-1,t+k)]);
                            k++;
                        }
                        sailingTimes[v.getNum() - 1][t][o1.getNumber() + nStartNodes - 1][o2.getNumber() + nStartNodes - 1] = k;
                        /*Gammel:
                        sailingTimes[v.getNum() - 1][t][o1.getNumber() + nStartNodes - 1][o2.getNumber() + nStartNodes - 1] =
                                (int) Math.ceil(distanceArray[o1.getLocation() - 1][o2.getLocation() - 1] / (v.getSpeed() - weatherPenaltySpeed[t]));*/
                        if(t==0){
                            distOperationsInInstance[o1.getNumber()-1][o2.getNumber()-1]=
                                    (int) Math.ceil(distanceArray[o1.getLocation() - 1][o2.getLocation() - 1]);
                        }
                    }
                }
                for (int n = 0; n < nStartNodes; n++) {
                    for (Operation o : this.operations) {
                        double sailingDist = distanceArray[locationsStartNodes[n] - 1][o.getLocation() - 1];
                        double coveredDist = 0;
                        int k=0;
                        while(coveredDist<sailingDist){
                            coveredDist = coveredDist+(v.getSpeed()-weatherPenaltySpeed[v.getNum()-1][Math.min(weatherPenaltySpeed[0].length-1,t+k)]);
                            k++;
                        }
                        sailingTimes[v.getNum() - 1][t][n][o.getNumber() + nStartNodes - 1] = k;
                    }
                }
            }
        }
        this.sailingTimes=sailingTimes;
    }

    public void generateData() throws FileNotFoundException {
        this.importWeather();
        this.createDistance();
        this.runOperationGenerator();
        this.createVesselTypes();
        this.createVesselObjects();
        this.readInstance();
        this.generateOperations();
        this.createSimultaneous();
        this.createPrecedence();
        this.createBigTasks();
        this.createSailingTimes();
        this.createVesselData();
        this.createOperationGain();
        this.createTimeWindows();
        this.endNodes=new int[vessels.length];
        this.startNodes=new int[vessels.length];
        int addIndex=0;
        for (int i=this.vessels.length+this.timeVesselUseOnOperation[0].length+1;i<this.operations.length+nStartNodes+nEndNodes+1;i++){
            endNodes[addIndex]=i;
            addIndex+=1;
        }
        addIndex=0;
        for (int n=1;n<this.vessels.length+1;n++){
            startNodes[addIndex]=n;
            addIndex+=1;
        }
        this.createEdges();
    }

    public void printAllData(){
        //PrintData.printPrecedence(this.precedence);
        //PrintData.printSimultaneous(this.simultaneous);
        System.out.println("Sim B tasks");
        for (Operation op : this.operations){
            if(simultaneousALNS[op.getNumber()-1][1]!= 0 && bigTasksALNS[op.getNumber()-1]==null){
                System.out.println(op.getNumber()+this.vessels.length);
            }
        }
        System.out.println("---------------------------------");
        System.out.println("Normal OP GAIN");
        PrintData.printOperationGain(this.operationGain, this.nStartNodes);
        System.out.println("---------------------------------");
        System.out.println("GUROBI OP GAIN");
        PrintData.printOperationGain(this.operationGainGurobi, this.nStartNodes);
        //PrintData.timeVesselUseOnOperations(this.timeVesselUseOnOperation, this.nStartNodes);
        //PrintData.printSailingCostForVessel(this.sailingCostForVessel);
        //PrintData.printEarliestStartingTimes(this.earliestStartingTimeForVessel);
        //PrintData.printEndPenaltyForVessel(this.endPenaltyforVessel);
        //PrintData.printOperationsForVessel (this.operationsForVessel);
        //PrintData.printSailingTimes(this.sailingTimes, 1, this.operations.length, nStartNodes);
        //PrintData.printTimeWindows(this.timeWindowsForOperations);
        //PrintData.printTimeWindowsIntervals(this.twIntervals);
        //PrintData.printPrecedenceALNS(this.precedenceALNS);
        //PrintData.printSimALNS(this.simultaneousALNS);
        //PrintData.printBigTasksALNS(this.bigTasksALNS,operations.length);
        //PrintData.printEarliestStartingTimes(earliestStartingTimeForVessel);
    }

    public static void main(String[] args) throws FileNotFoundException {
        int[] vessels20 = new int[]{3, 5,6};
        int[] locStart20 = new int[]{94, 95, 96};

        int[] vessels40 = new int[]{1,3,4,5,6};
        int[] locStart40 = new int[]{94,94,96,97,98};

        int[] vessels60 =  new int[]{1,2,3,4,5,6,3,4};
        int[] locStart60 =new int[]{94,95,96,97,98,99,100,101};

        String[] seasons = new String[]{"low", "high"};
        for (String season : seasons) {
            for (int i = 1; i < 6; i++) {
                String instance = "20_" + i + "_" + season + "_locations(94_113)_";
                String testInstance = "technical_test_instances/" + instance + ".txt";
                DataGenerator dg = new DataGenerator(vessels20, 5, locStart20,
                        testInstance,
                        "routing", "weather_files/weather_normal.txt");
                dg.generateData();
            }
            for (int i = 1; i < 6; i++) {
                String instance = "40_" + i + "_" + season + "_locations(94_133)_";
                String testInstance = "technical_test_instances/" + instance + ".txt";
                DataGenerator dg = new DataGenerator(vessels40, 5, locStart40,
                        testInstance,
                        "routing", "weather_files/weather_normal.txt");
                dg.generateData();

            }
            for (int i = 1; i < 6; i++) {
                String instance = "60_" + i + "_" + season + "_locations(81_140)_";
                String testInstance = "technical_test_instances/" + instance + ".txt";
                DataGenerator dg = new DataGenerator(vessels60, 5, locStart60,
                        testInstance,
                        "routing", "weather_files/weather_normal.txt");
                dg.generateData();
            }
        }

    }


    public int getSimNumber() {
        return simNumber;
    }

    public int getPresNumber() {
        return presNumber;
    }

    public int[][] getTwIntervals() {
        return twIntervals;
    }

    public int[][][][] getSailingTimes() {
        return sailingTimes;
    }

    public int[][] getSimultaneous() {
        return simultaneous;
    }

    public int[][] getPrecedence() {
        return precedence;
    }

    public Map<Integer, List<Integer>> getConsolidatedTasks() {
        return consolidatedTasks;
    }

    public List<Integer> getBigTasks() {
        return bigTasks;
    }

    public int[][][] getOperationGain() {
        return operationGain;
    }

    public int[] getSailingCostForVessel() {
        return sailingCostForVessel;
    }

    public int[][][] getOperationGainGurobi() {
        return operationGainGurobi;
    }

    public int[] getBigTasksArr() {
        return bigTasksArr;
    }

    public int[] getEarliestStartingTimeForVessel() {
        return earliestStartingTimeForVessel;
    }

    public double[] getEndPenaltyForVessel() {
        return endPenaltyforVessel;
    }

    public int[][][] getEdges() {
        return edges;
    }

    public int[][] getOperationsForVessel() {
        return operationsForVessel;
    }

    public int[][] getTimeWindowsForOperations() {
        return timeWindowsForOperations;
    }

    public int[][][] getTimeVesselUseOnOperation() {
        return timeVesselUseOnOperation;
    }

    public Operation[] getOperations() {
        return operations;
    }

    public int[] getEndNodes() {
        return endNodes;
    }

    public int[] getStartNodes() {
        return startNodes;
    }

    public int[][] getPrecedenceALNS() {
        return precedenceALNS;
    }

    public int[][] getSimultaneousALNS() {
        return simultaneousALNS;
    }

    public int[][] getBigTasksALNS() {
        return bigTasksALNS;
    }

    public int[][] getDistOperationsInInstance() {
        return distOperationsInInstance;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public static String removeSpace(String str)
    {
        // Count leading zeros
        int i = 0;
        while (str.charAt(i) == ' ')
            i++;

        // Convert str into StringBuffer as Strings
        // are immutable.
        StringBuffer sb = new StringBuffer(str);

        // The  StringBuffer replace function removes
        // i characters from given index (0 here)
        sb.replace(0, i, "");

        return sb.toString();  // return in String
    }

    //Static functions also valuable for other classes

    public static Boolean containsElement(int element, int[] list)   {
        Boolean bol = false;
        for (Integer e: list)     {
            if(element==e){
                return true;
            }
        }
        return bol;
    }
}



