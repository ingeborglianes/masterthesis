public class InsertionValues {
    int indexInRoute=0;
    int routeIndex=0;
    int earliest=0;
    int latest=59;
    int benenefitIncrease=0;

    public InsertionValues(int benenefitIncrease, int indexInRoute, int routeIndex, int earliest, int latest){
        this.indexInRoute=indexInRoute;
        this.routeIndex=routeIndex;
        this.earliest=earliest;
        this.latest=latest;
        this.benenefitIncrease=benenefitIncrease;
    }

    public int getIndexInRoute() {
        return indexInRoute;
    }

    public int getRouteIndex() {
        return routeIndex;
    }

    public int getEarliest() {
        return earliest;
    }

    public int getLatest() {
        return latest;
    }

    public int getBenenefitIncrease() {
        return benenefitIncrease;
    }

    public void setBenenefitIncrease(int benenefitIncrease) {
        this.benenefitIncrease = benenefitIncrease;
    }
}
