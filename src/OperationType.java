public class OperationType {
    private int number;
    private int[] vessel1;
    private int[] vessel2;
    private int[] vesselBigTask;
    private int precedenceOver;
    private int mFrequency;
    private int sdFrequency;
    private int precedenceOf;
    private int duration;
    private int operationGain;
    private String name;

    //number,vessel1,vessel2,vesselBigTask, presedensOver, presedensAv, mHyppighet, sdHyppighet,
    //varighet, penalty

    public OperationType(int number, int[] vessel1, int[] vessel2,int [] vesselBigTask,int precedenceOver,int precedenceOf
            ,int mFrequency,int sdFrequency,int duration,int operationGain, String name){
        this.number=number;
        this.vessel1=vessel1;
        this.vessel2=vessel2;
        this.vesselBigTask=vesselBigTask;
        this.precedenceOver=precedenceOver;
        this.precedenceOf=precedenceOf;
        this.mFrequency=mFrequency;
        this.sdFrequency=sdFrequency;
        this.duration=duration;
        this.operationGain=operationGain;
        this.name=name;
    }

    public int getNumber() {
        return number;
    }

    public int[] getVessel1() {
        return vessel1;
    }

    public int[] getVessel2() {
        return vessel2;
    }

    public int []getVesselBigTask() {
        return vesselBigTask;
    }

    public int getmFrequency() {
        return mFrequency;
    }

    public int getSdFrequency() {
        return sdFrequency;
    }

    public int getPrecedenceOf() {
        return precedenceOf;
    }

    public int getDuration() {
        return duration;
    }

    public int getPrecedenceOver() {
        return precedenceOver;
    }

    public int getOperationGain() {
        return operationGain;
    }

    public String getName() {
        return name;
    }

    public int getLimit(){
        return this.mFrequency-this.sdFrequency;
    }
}
