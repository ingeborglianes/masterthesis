public class ConnectedValues {
    private OperationInRoute operationObject;
    private OperationInRoute connectedOperationObject;
    private int connectedOperationID;
    private int index;
    private int connectedRoute;
    private int route;

    ConnectedValues(OperationInRoute operationObject, OperationInRoute connectedOperationObject, int connectedOperationID,
                    int index, int route, int connectedRoute){
        this.operationObject=operationObject;
        this.connectedOperationObject=connectedOperationObject;
        this.connectedOperationID=connectedOperationID;
        this.index=index;
        this.route=route;
        this.connectedRoute=connectedRoute;
    }

    public OperationInRoute getOperationObject() {
        return operationObject;
    }

    public void setOperationObject(OperationInRoute operationObject) {
        this.operationObject = operationObject;
    }

    public OperationInRoute getConnectedOperationObject() {
        return connectedOperationObject;
    }

    public void setConnectedOperationObject(OperationInRoute connectedOperationObject) {
        this.connectedOperationObject = connectedOperationObject;
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

    public void setRoute(int route) {
        this.route = route;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getConnectedOperationID() {
        return connectedOperationID;
    }

    public void setConnectedOperationID(int connectedOperationID) {
        this.connectedOperationID = connectedOperationID;
    }
}

