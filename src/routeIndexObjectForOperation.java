public class routeIndexObjectForOperation {
    int route;
    OperationInRoute or;

    routeIndexObjectForOperation(int route, OperationInRoute or){
        this.route=route;
        this.or =or;
    }

    public int getRoute() {
        return route;
    }

    public OperationInRoute getOr() {
        return or;
    }
}
