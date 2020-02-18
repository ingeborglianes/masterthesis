public class OperationInRoute {
    private int ID;
    private int earliestTime;
    private int latestTime;

    public OperationInRoute(int ID, int earliestTime,int latestTime) {
        this.ID=ID;
        this.earliestTime=earliestTime;
        this.latestTime=latestTime;
    }

    public int getID() {
        return ID;
    }

    public int getEarliestTime() {
        return earliestTime;
    }

    public void setEarliestTime(int earliestTime) {
        this.earliestTime = earliestTime;
    }

    public int getLatestTime() {
        return latestTime;
    }

    public void setLatestTime(int latestTime) {
        this.latestTime = latestTime;
    }
}
