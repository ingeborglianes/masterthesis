public class PrecedenceValues {
    private OperationInRoute operation;
    private OperationInRoute connectedOperation;
    private int connectedRoute;
    private int route;

    PrecedenceValues(OperationInRoute operation, OperationInRoute connectedOperation,int route,  int connectedRoute){
        this.operation=operation;
        this.connectedOperation=connectedOperation;
        this.route=route;
        this.connectedRoute=connectedRoute;
    }

    public OperationInRoute getOperation() {
        return operation;
    }

    public void setOperation(OperationInRoute operation) {
        this.operation = operation;
    }

    public OperationInRoute getConnectedOperation() {
        return connectedOperation;
    }

    public void setConnectedOperation(OperationInRoute connectedOperation) {
        this.connectedOperation = connectedOperation;
    }

    public int getConnectedRoute() {
        return connectedRoute;
    }

    public void setConnectedRoute(int connectedRoute) {
        this.connectedRoute = connectedRoute;
    }

    public int getRoute() {
        return route;
    }
}
