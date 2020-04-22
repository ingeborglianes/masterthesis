public class ConsolidatedValues {
    private Boolean consolidated;
    private Boolean smallTasks;
    private int connectedRoute1;
    private int connectedRoute2;
    private int consolidatedRoute;

    ConsolidatedValues(Boolean consolidated, Boolean smallTasks, int connectedRoute1,
                       int connectedRoute2, int consolidatedRoute){
        this.consolidated=consolidated;
        this.smallTasks=smallTasks;
        this.connectedRoute1=connectedRoute1;
        this.connectedRoute2=connectedRoute2;
        this.consolidatedRoute=consolidatedRoute;
    }

    public Boolean getConsolidated() {
        return consolidated;
    }

    public Boolean getSmallTasks() {
        return smallTasks;
    }

    public int getConnectedRoute1() {
        return connectedRoute1;
    }

    public int getConnectedRoute2() {
        return connectedRoute2;
    }

    public int getConsolidatedRoute() {
        return consolidatedRoute;
    }

    public void setConsolidatedRoute(int consolidatedRoute) {
        this.consolidatedRoute = consolidatedRoute;
    }
}
