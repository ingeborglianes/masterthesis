import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class InstanceGenerator {
    private OperationType[] operationTypes;
    private int days;
    private Map<Integer, List<OperationType>> logOperations=new HashMap<Integer, List<OperationType>>();
    private Map<int[],Integer> operationTime=new HashMap<int[],Integer>();
    private int[] locations;
    private int[] excludedOperations;

    public InstanceGenerator(int days, int[] locations){
        this.locations=locations;
        this.days=days;
        this.excludedOperations= new int[]{1,2,8,10};
    }

    public void OpGen(){
        OperationGenerator og=new OperationGenerator();
        this.operationTypes=og.createOperationTypes();
    }

    public void OpGenTest(){
        OperationGenerator og=new OperationGenerator();
        this.operationTypes=og.createOperationTypesTest();
    }

    public void generateInstanceFromDistribution(){
        OpGen();
        //OpGenTest();
        int period=this.days*24;
        for (int loc : this.locations) {
            for (OperationType opType : this.operationTypes) {
                int mean = opType.getmFrequency();
                int sd=opType.getSdFrequency();
                double t = (double) ThreadLocalRandom.current().nextInt(1, mean+1);
                NormalDistribution nd = new NormalDistribution(mean,sd);
                double samplePoint=nd.sample();
                if(samplePoint>=t && samplePoint<=(t+period) && !DataGenerator.containsElement(opType.getNumber(),excludedOperations)){
                    int time=((int) Math.ceil((samplePoint-t)/2));
                    if(time<10){
                        time=10;
                    }
                    else if(time>55){
                        time=10;
                    }
                    int[] locOp=new int[]{loc,opType.getNumber()};
                    operationTime.put(locOp,time);
                    if (logOperations.get(loc)!=null){
                        logOperations.get(loc).add(opType);
                    }
                    else{
                        logOperations.put(loc,new ArrayList<OperationType>(){{add(opType);}});
                    }
                    int newOp=opType.getPrecedenceOver();
                    int newOp2=opType.getPrecedenceOf();
                    this.findPrecedenceOverDist(loc,newOp);
                    this.findPrecedenceOfDist(loc,newOp2);
                }
            }
        }
    }

    public void findPrecedenceOfDist(int loc,int no2){
        if(no2!=0) {
            OperationType newOp2=findOperationType(no2);
            this.logOperations.get(loc).add(newOp2);
            if (newOp2.getPrecedenceOf() != 0) {
                this.logOperations.get(loc).add(this.findOperationType(newOp2.getPrecedenceOf()));
            }
        }
    }

    public void findPrecedenceOverDist(int loc,int no){
        if(no!=0) {
            OperationType newOp=findOperationType(no);
            this.logOperations.get(loc).add(newOp);
            if (newOp.getPrecedenceOver() != 0) {
                this.logOperations.get(loc).add(this.findOperationType(newOp.getPrecedenceOver()));
            }
        }
    }

    public OperationType findOperationType(int number){
        OperationType matchOp=operationTypes[0];
        for (OperationType op : this.operationTypes){
            if (op.getNumber()==number){
                matchOp=op;
            }
        }
        return matchOp;
    }

    public void writeToFile(String testInstanceName)
            throws IOException {
        FileWriter fileWriter = new FileWriter(testInstanceName);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        String tasksForOperation="";
        for (Map.Entry<Integer, List<OperationType>> item : logOperations.entrySet()) {
            int key = item.getKey();
            //System.out.println("Location");
            //System.out.println(key);
            List<OperationType> value = item.getValue();
            //System.out.println("Operations");
            for (OperationType ot:value){
                //System.out.println(ot.getNumber());
            }
            tasksForOperation+=String.valueOf(key);
            for (OperationType opType : value){
                tasksForOperation+=" ";
                tasksForOperation+=String.valueOf(opType.getNumber());
            }
            tasksForOperation+="\n";
        }
        for(Map.Entry<int[], Integer> item : operationTime.entrySet()){
            int[] key = item.getKey();
            int value=item.getValue();
            tasksForOperation+=String.valueOf(0)+" "+String.valueOf(key[0])+" "+String.valueOf(key[1])+" "+String.valueOf(value);
            tasksForOperation+="\n";
        }
        printWriter.print(tasksForOperation);
        printWriter.close();
    }

    public static void main(String[] args) throws IOException {
        int[] loc = new int[]{
                /* 1 ,2 ,3 ,4 ,5 ,6 ,7 ,8 ,9 ,10 ,11 ,12 ,13 ,14 ,15 ,16 ,17 ,18 ,19 ,20  ,21 ,22 ,23 ,24 ,25 ,26,
                27 ,28 ,29 ,30, 31 ,32 ,33 ,34 ,35 ,36 ,37, 38 ,39 ,40 ,41 ,42 ,43 ,44 ,45 ,46 ,47 ,48 ,49 ,50,
                51 ,52 ,53 ,54 ,55 ,56 ,57 ,58 ,59 ,60 ,61 ,62 ,63 ,64 ,65 ,66 ,67 ,68 ,69 ,70 ,71 ,72 ,73 ,74,
                75 ,76 ,77 ,78 ,79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93,*/ 94, 95, 96, 97, 98, 99, 100, 101, 102,
                103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113}; /*114, 115, 116, 117, 118, 119, 120, 121, 122, 123,
                124, 125, 126, 127, 128, 129, 130, 131, 132, 133}; /*134, 135, 136, 137, 138, 139, 140};  /*141, 142, 143, 144,
                145, 146, 147, 148, 149, 150, 151, 152, 153,154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164,
                165, 166, 167, 168, 169, 170, 171, 172,
        };*/
        InstanceGenerator ig = new InstanceGenerator(5,loc);
        ig.generateInstanceFromDistribution();
        ig.writeToFile("technical_test_instances/20_5_low_locations(94_113)_.txt");
    }
}
