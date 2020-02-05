public class Vessel {
    private double speed;
    private int number;
    private int sailingCost;
    private int earliestStartingTime;
    private double timePenalty;
    private double endPenalty;
    private int vesselType;
    private int[] operationCost;

    public Vessel(int num, double speed, int sailingCost, int earliestStartingTime,
                  double timePenalty, int vesselType, double endPenalty){
        this.number=num;
        this.sailingCost=sailingCost;
        this.earliestStartingTime=earliestStartingTime;
        this.timePenalty=timePenalty;
        this.vesselType=vesselType;
        this.speed=speed;
        this.endPenalty=endPenalty;
    }

    public double getSpeed() {
        return speed;
    }

    public int getNum() {
        return number;
    }

    public int getSailingCost() {
        return sailingCost;
    }

    public int getEarliestStartingTime() {
        return earliestStartingTime;
    }

    public double getTimePenalty() {
        return timePenalty;
    }

    public int getVesselType() {
        return vesselType;
    }

    public double getEndPenalty() {
        return endPenalty;
    }
}

