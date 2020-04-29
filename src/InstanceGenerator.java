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
        this.excludedOperations= new int[]{1,2,10};
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
        int[] loc = new int[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37};
        InstanceGenerator ig = new InstanceGenerator(5,loc);
        ig.generateInstanceFromDistribution();
        ig.writeToFile("test_instances/37_locations_normalOpGenerator.txt");
    }
}
