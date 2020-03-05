import java.io.*;
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
    private Map<Integer, List<OperationType>> logOperations=new HashMap<Integer, List<OperationType>>();
    private int [][] simultaneous;
    private int [][] precedence;
    private Map<Integer, List<Integer>> consolidatedTasks=new HashMap<Integer, List<Integer>>();
    private List<Integer> bigTasks=new ArrayList<Integer>();
    private int[] bigTasksArr;
    private int [] penalty;
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
    private Double [] weatherPenaltyOperations;
    private int [] weatherPenaltySpeed;
    private String weatherFile;
    private int[][] twIntervals;
    private int[][] precedenceALNS;
    private int[] simultaneousALNS;
    private int[][] bigTasksALNS;

    public DataGenerator(int[] vessels, int days, int[] locationsStartNodes, String filePath, String fileNameRouting, String weatherFile){
        this.vesselsInput=vessels;
        this.days=days;
        this.nStartNodes=this.vesselsInput.length;
        this.nEndNodes=this.vesselsInput.length;
        this.locationsStartNodes=locationsStartNodes;
        this.filePath=filePath;
        this.fileNameRouting=fileNameRouting;
        this.weatherFile=weatherFile;
        this.weatherPenaltyOperations=new Double[days*12];
        this.weatherPenaltySpeed=new int[days*12];
        twIntervals=new int[60][2];
        for (int[] operation: twIntervals){
            operation[0]=0;
            operation[1]=days*12;
        }
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
            else{
                weatherPenaltyOperations[wh] = 0.0;
                weatherPenaltySpeed[wh]=3;
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
                        System.out.println(countInner+" and "+countOuter);
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
        //this.operationTypes=og.createOperationTypes();
        this.operationTypes=og.createOperationTypesTest();
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

    public void generateOperations(){
        List<int[]> precedenceALNSTemp= new ArrayList<>();
        List<Integer> simultaneousALNSTemp= new ArrayList<>();
        List<int[]> bigTasksALNSTemp= new ArrayList<>();
        List<Operation> operations=new ArrayList<Operation>();
        int opNumber=1;
        int[] tw= IntStream.rangeClosed(1, this.days*12).toArray();
        List<String> routing = new ArrayList<>();
        for (Integer location : this.logOperations.keySet()) {
            for (OperationType opType : this.logOperations.get(location)){
                if (opType.getVessel2()==null && opType.getVesselBigTask()==null){
                    Operation op=new Operation(opNumber, opType.getVessel1(), location, 0, null,
                            opType.getPrecedenceOver(), tw, opType.getDuration(),
                            opType.getNumber(), opType.getPenalty(), opType.getName());
                    if (opType.getNumber()==11){
                        int[] keyTime=new int[]{location,opType.getNumber()};
                        int t=returnTimeOfOperation(keyTime);
                        if(t>this.days*12){
                            t = this.days*12;
                        }
                        int[] tw11= new int[this.days*12];
                        tw11[t-1]=t;
                        twIntervals[opNumber-1][0]=t;
                        twIntervals[opNumber-1][1]=t;
                        op = new Operation(opNumber, opType.getVessel1(), location, 0, null,
                                opType.getPrecedenceOver(), tw11, opType.getDuration(),
                                opType.getNumber(), opType.getPenalty(), opType.getName());
                    }
                    operations.add(op);
                    simultaneousALNSTemp.add(0);
                    bigTasksALNSTemp.add(null);
                    this.consolidatedTasks.put(op.getNumber()+nStartNodes, new ArrayList<Integer>(){{}});
                    this.bigTasks.add(op.getNumber()+nStartNodes);
                    routing.add("Operation: "+String.valueOf(opNumber+nStartNodes)+
                            " Precedence: "+String.valueOf(op.getPrecedence())+" Location: "+ String.valueOf(op.getLocation())+
                            " optype: "+String.valueOf(op.getType())+" bigTaskSet: "+String.valueOf(op.getBigTaskSet())+
                            "Sim: "+String.valueOf(op.getSimultaneous())+" Vessels: "+Arrays.toString(op.getVessels())
                            +" Duration: "+op.getDuration()+" Task description: "+op.getName());
                    opNumber+=1;
                }
                else if(opType.getVessel2()!=null && opType.getVesselBigTask() ==null){
                    Operation op1= new Operation(opNumber, opType.getVessel1(), location, opNumber+1, null,
                            opType.getPrecedenceOver(),tw, opType.getDuration(),
                            opType.getNumber(),opType.getPenalty(),opType.getName()+" Part 1");
                    operations.add(op1);
                    simultaneousALNSTemp.add(opNumber+nStartNodes+1);
                    bigTasksALNSTemp.add(null);
                    routing.add("Operation: "+String.valueOf(opNumber+nStartNodes)+
                            " Precedence: "+String.valueOf(op1.getPrecedence())+" Location: "+ String.valueOf(op1.getLocation())+
                            " optype: "+String.valueOf(op1.getType())+" bigTaskSet: "+String.valueOf(op1.getBigTaskSet())+
                            "Sim: "+String.valueOf(op1.getSimultaneous()+nStartNodes)+" Vessels: "+Arrays.toString(op1.getVessels())+" Duration: "+op1.getDuration()+" Task description: "+op1.getName());
                    this.consolidatedTasks.put(op1.getNumber()+nStartNodes, new ArrayList<Integer>(){{}});
                    this.bigTasks.add(op1.getNumber()+nStartNodes);
                    opNumber+=1;
                    Operation op2=new Operation(opNumber, opType.getVessel2(), location, opNumber-1, null,
                            opType.getPrecedenceOver(),tw, opType.getDuration(),
                            opType.getNumber(),opType.getPenalty(),opType.getName()+" Part 2");
                    operations.add(op2);
                    simultaneousALNSTemp.add(opNumber+nStartNodes-1);
                    bigTasksALNSTemp.add(null);
                    routing.add("Operation: "+String.valueOf(opNumber+nStartNodes)+
                            " Precedence: "+String.valueOf(op2.getPrecedence())+" Location: "+ String.valueOf(op2.getLocation())+
                            " optype: "+String.valueOf(op2.getType())+" bigTaskSet: "+String.valueOf(op2.getBigTaskSet())+
                            "Sim: "+String.valueOf(op2.getSimultaneous()+nStartNodes)+" Vessels: "+Arrays.toString(op2.getVessels())+" Duration: "+op2.getDuration()+" Task description: "+op2.getName());
                    this.consolidatedTasks.put(op2.getNumber()+nStartNodes, new ArrayList<Integer>(){{}});
                    this.bigTasks.add(op2.getNumber()+nStartNodes);
                    opNumber+=1;
                }
                else if(opType.getVesselBigTask()!=null && opType.getVessel2()!=null){
                    Operation opSmall1=new Operation(opNumber, opType.getVessel1(), location, opNumber+1, null,
                            opType.getPrecedenceOver(),tw, opType.getDuration(),
                            opType.getNumber(),opType.getPenalty(),opType.getName()+" Part 1 of big task operation");
                    operations.add(opSmall1);
                    simultaneousALNSTemp.add(opNumber+nStartNodes+1);
                    bigTasksALNSTemp.add(new int[]{opNumber+nStartNodes+2,opNumber+nStartNodes,opNumber+nStartNodes+1});
                    routing.add("Operation: "+String.valueOf(opNumber+nStartNodes)+
                            " Precedence: "+String.valueOf(opSmall1.getPrecedence())+" Location: "+ String.valueOf(opSmall1.getLocation())+
                            " optype: "+String.valueOf(opSmall1.getType())+" bigTaskSet: "+String.valueOf(opSmall1.getBigTaskSet())+
                            "Sim: "+String.valueOf(opSmall1.getSimultaneous()+nStartNodes)+" Vessels: "+Arrays.toString(opSmall1.getVessels())+" Duration: "+opSmall1.getDuration()+" Task description: "+opSmall1.getName());
                    opNumber+=1;
                    Operation opSmall2=new Operation(opNumber, opType.getVessel2(), location, opNumber-1, null,
                            opType.getPrecedenceOver(),tw, opType.getDuration(),
                            opType.getNumber(),opType.getPenalty(),opType.getName()+" Part 2 of big task operation");
                    operations.add(opSmall2);
                    simultaneousALNSTemp.add(opNumber+nStartNodes-1);
                    bigTasksALNSTemp.add(new int[]{opNumber+1+nStartNodes,opNumber+nStartNodes,opNumber-1+nStartNodes});
                    routing.add("Operation: "+String.valueOf(opNumber+nStartNodes)+
                            " Precedence: "+String.valueOf(opSmall2.getPrecedence())+" Location: "+ String.valueOf(opSmall1.getLocation())+
                            " optype: "+String.valueOf(opSmall2.getType())+" bigTaskSet: "+ Arrays.toString(opSmall2.getBigTaskSet()) +
                            " Sim: "+String.valueOf(opSmall2.getSimultaneous()+nStartNodes)+" Vessels: "+Arrays.toString(opSmall2.getVessels())+" Duration: "+opSmall2.getDuration()+" Task description: "+opSmall2.getName());
                    opNumber+=1;
                    int [] bigTasksArray= new int[]{opSmall1.getNumber(),opSmall2.getNumber()};
                    Operation opBig=new Operation(opNumber, opType.getVesselBigTask(), location, 0, bigTasksArray,
                            opType.getPrecedenceOver(),tw, opType.getDuration(),
                            opType.getNumber(),opType.getPenalty(),opType.getName()+" Big task operation");
                    operations.add(opBig);
                    simultaneousALNSTemp.add(0);
                    bigTasksALNSTemp.add(new int[]{opNumber+nStartNodes,opNumber-1+nStartNodes,opNumber-2+nStartNodes});
                    routing.add("Operation: "+String.valueOf(opNumber+nStartNodes)+
                            " Precedence: "+String.valueOf(opBig.getPrecedence())+" Location: "+ String.valueOf(opBig.getLocation())+
                            " optype: "+String.valueOf(opBig.getType())+" bigTaskSet: "+ Arrays.toString(opBig.getBigTaskSet()) +
                            "Sim: "+String.valueOf(opBig.getSimultaneous())+" Vessels: "+Arrays.toString(opBig.getVessels())+" Duration: "+opBig.getDuration()+" Task description: "+opBig.getName());
                    this.consolidatedTasks.put(opBig.getNumber()+nStartNodes, new ArrayList<Integer>(){{add(opSmall1.getNumber()+nStartNodes); add(opSmall2.getNumber()+nStartNodes);}});
                    this.bigTasks.add(opBig.getNumber()+nStartNodes);
                    opNumber+=1;
                }
            }
        }
        Operation[] operations2=new Operation[operations.size()];
        precedenceALNS=new int[precedenceALNSTemp.size()][2];
        simultaneousALNS=new int[simultaneousALNSTemp.size()];
        bigTasksALNS=new int[bigTasksALNSTemp.size()][3];
        for (int p=0;p<precedenceALNSTemp.size();p++){
            precedenceALNS[p]=new int[]{precedenceALNSTemp.get(p)[0],precedenceALNSTemp.get(p)[1]};
        }
        for (int s=0;s<simultaneousALNSTemp.size();s++){
            simultaneousALNS[s]=simultaneousALNSTemp.get(s);
        }
        for (int b=0;b<bigTasksALNSTemp.size();b++){
            if(bigTasksALNSTemp.get(b)!=null) {
                bigTasksALNS[b] = new int[]{bigTasksALNSTemp.get(b)[0], bigTasksALNSTemp.get(b)[1], bigTasksALNSTemp.get(b)[2]};
            }
            else{
                bigTasksALNS[b] =null;
            }
        }
        int addIndex=0;
        for (Operation op : operations){
            operations2[addIndex]=op;
            addIndex+=1;
        }
        routing.add(" ");
        for (Vessel v:vessels){
            routing.add("Vessel "+ v.getNum()+" is vessel type "+v.getVesselType());
        }
        routing.add(" ");
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
                    precedenceALNS[op1.getNumber()-1][0]=num2+1;
                    precedenceALNS[op2.getNumber()-1][1]=num1+1;
                    System.out.println("Operation "+num1+" has precedence over operation "+num2);
                }
            }
        }
        this.precedence=precedence;
    }

    public void createSimultaneous(){
        int nOperations=this.operations.length;
        int nVessels=this.vessels.length;
        int[][] sim=new int[nOperations+2*nVessels][nOperations+2*nVessels];
        for (Operation op1 : this.operations){
            for (Operation op2 : this.operations){
                if(op1.getSimultaneous()==op2.getNumber()){
                    int num1=nStartNodes + (int) (op1.getNumber()) - 1;
                    int num2=nStartNodes + (int)(op2.getNumber()) - 1;
                    sim[num1][num2]=1;
                    System.out.println("Operation "+num1+" is simultaneous with operation "+num2);
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

    public void createPenalty(){
        int[] penList= new int[this.operations.length];
        int addIndex=0;
        for(Operation op : this.operations){
            penList[addIndex]=op.getPenalty();
            addIndex+=1;
        }
        this.penalty=penList;
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
        int [][][] timeOpVessel=new int [this.vessels.length][this.operations.length][this.weatherPenaltySpeed.length];
        for (Vessel vessel : this.vessels){
            for (Operation op : this.operations){
                for(int t=0;t<weatherPenaltySpeed.length;t++) {
                    if (containsElement(op.getNumber() + nStartNodes, this.operationsForVessel[vessel.getNum() - 1])) {
                        if (op.getSimultaneous() == 0) {
                            timeOpVessel[vessel.getNum() - 1][op.getNumber() - 1][t] = (int) Math.round(vessel.getTimePenalty() * op.getDuration()*weatherPenaltyOperations[t]);
                            //System.out.println("vessel "+vessel.getNum()+ " op "+op.getNumber()+" time "+t+" penaltyV: "+vessel.getTimePenalty()+" penaltyW "+weatherPenaltyOperations[t] +
                            //      " Duration "+op.getDuration());
                        } else {
                            int[] v1 = findOperationType(op.getNumber()).getVessel1();
                            int[] v2 = findOperationType(op.getNumber()).getVessel1();
                            double maxV1 = findVesselObject(v1[v1.length - 1]).getTimePenalty();
                            double maxV2 = findVesselObject(v2[v2.length - 1]).getTimePenalty();
                            double penalty = Math.max(maxV1, maxV2);
                            timeOpVessel[vessel.getNum() - 1][op.getNumber() - 1][t] = (int) Math.round(penalty * op.getDuration()*weatherPenaltyOperations[t]);
                        }
                    }
                }
            }
        }
        this.timeVesselUseOnOperation=timeOpVessel;

    }

    public void createTimeWindows(){
        int [][] timeWindows= new int[this.operations.length+nStartNodes+nEndNodes][this.days*12];
        for (Operation op:this.operations){
            for(int t=0;t<op.getTimeWindow().length;t++){
                if(weatherPenaltyOperations[t]!=0.0){
                    timeWindows[op.getNumber()+nStartNodes-1][t]=op.getTimeWindow()[t];
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
                    if(!containsElement(n+1,startNodes)&&!containsElement(n+1,endNodes)&&!containsElement(i+1,startNodes)
                            &&!containsElement(i+1,endNodes)) {
                        if (containsElement(n+1,operationsForVessel[v]) && containsElement(i+1,operationsForVessel[v]) && n != i) {
                            edges[v][n][i] = 1;
                        }
                    }
                    else if(n==v){
                        if (containsElement(i+1,operationsForVessel[v]) || i == (nStartNodes+nEndNodes+nOperations-1-v)) {
                            edges[v][n][i] = 1;
                        }
                    }
                    else if(i==(nStartNodes+nEndNodes+nOperations-1-v)) {
                        if(containsElement(n+1,operationsForVessel[v])){
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
        int[][][][] sailingTimes = new int[nStartNodes][weatherPenaltySpeed.length][operations.length+nStartNodes+nEndNodes][operations.length+nStartNodes+nEndNodes];
        for(Vessel v:this.vessels) {
            for (int t = 0; t < weatherPenaltySpeed.length; t++) {
                for (Operation o1 : this.operations) {
                    for (Operation o2 : this.operations) {
                        sailingTimes[v.getNum() - 1][t][o1.getNumber() + nStartNodes - 1][o2.getNumber() + nStartNodes - 1] =
                                (int) Math.ceil(distanceArray[o1.getLocation() - 1][o2.getLocation() - 1] / (v.getSpeed() - weatherPenaltySpeed[t]));
                    }
                }
                for (int n = 0; n < nStartNodes; n++) {
                    for (Operation o : this.operations) {

                        sailingTimes[v.getNum() - 1][t][n][o.getNumber() + nStartNodes - 1]
                                = (int) Math.ceil(distanceArray[locationsStartNodes[n] - 1][o.getLocation() - 1] / (v.getSpeed() - weatherPenaltySpeed[t]));
                    }
                }
            }
        }
        this.sailingTimes=sailingTimes;
        int index=0;
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
        this.createPenalty();
        this.createVesselData();
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
        PrintData.printPrecedence(this.precedence);
        PrintData.printSimultaneous(this.simultaneous);
        PrintData.printOperationpenalty(this.penalty);
        //PrintData.timeVesselUseOnOperations(this.timeVesselUseOnOperation, this.nStartNodes);
        PrintData.printSailingCostForVessel(this.sailingCostForVessel);
        PrintData.printEarliestStartingTimes(this.earliestStartingTimeForVessel);
        PrintData.printEndPenaltyForVessel(this.endPenaltyforVessel);
        PrintData.printOperationsForVessel (this.operationsForVessel);
        //PrintData.printSailingTimes(this.sailingTimes, 1, this.operations.length, nStartNodes);
        PrintData.printTimeWindows(this.timeWindowsForOperations);
        PrintData.printTimeWindowsIntervals(this.twIntervals);
        PrintData.printPrecedenceALNS(this.precedenceALNS);
        PrintData.printSimALNS(this.simultaneousALNS);
        PrintData.printBigTasksALNS(this.bigTasksALNS,operations.length);
    }

    public static void main(String[] args) throws FileNotFoundException {
        int[] loc = new int[]{1,2,9,10,11,12,13,14,15};
        int[] vessels=new int[]{2,3,5};
        int[] locStart = new int[]{1,2,3};
        DataGenerator dg=new DataGenerator(vessels,5,locStart,
                "test_instances/test_instance_25_locations.txt",
                "routing","weather_files/weather_september.txt");
        dg.generateData();
        dg.printAllData();

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

    public int[] getPenalty() {
        return penalty;
    }

    public int[] getSailingCostForVessel() {
        return sailingCostForVessel;
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

    public int[] getSimultaneousALNS() {
        return simultaneousALNS;
    }

    public int[][] getBigTasksALNS() {
        return bigTasksALNS;
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



