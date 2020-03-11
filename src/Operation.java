public class Operation {
    private  int number;
    private int[] vessels;
    private int location;
    private int simultaneous;
    private int[] bigTaskSet;
    private int precedence;
    private int[] timeWindow;
    private int duration;
    private int type;
    private int operationGain;
    private String name;

    public Operation(int num, int [] vessels,int location, int simultaneous,
                     int[] bigTaskSet,
                     int precedence,
                     int []timeWindow,
                     int duration,
                     int type,
                     int operationGain, String name){
        this.number=num;
        this.vessels=vessels;
        this.location=location;
        this.simultaneous=simultaneous;
        this.bigTaskSet=bigTaskSet;
        this.precedence=precedence;
        this.timeWindow=timeWindow;
        this.duration=duration;
        this.type=type;
        this.operationGain=operationGain;
        this.name=name;
    }

    public int getNumber() {
        return number;
    }

    public int[] getVessels() {
        return vessels;
    }

    public int getLocation() {
        return location;
    }

    public int getSimultaneous() {
        return simultaneous;
    }

    public int[] getBigTaskSet() {
        return bigTaskSet;
    }

    public int getPrecedence() {
        return precedence;
    }

    public int[] getTimeWindow() {
        return timeWindow;
    }

    public int getDuration() {
        return duration;
    }

    public int getType() {
        return type;
    }

    public int getOperationGain() {
        return operationGain;
    }

    public String getName() {
        return name;
    }
}
