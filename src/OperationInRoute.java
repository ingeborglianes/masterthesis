public class OperationInRoute {
    private int ID;
    private int timeperiod;

    public OperationInRoute(int ID, int timeperiod) {
        this.ID=ID;
        this.timeperiod=timeperiod;
    }

    public int getID() {
        return ID;
    }

    public void setTimeperiod(int timeperiod) {
        this.timeperiod = timeperiod;
    }

    public int getTimeperiod() {
        return timeperiod;
    }
}
