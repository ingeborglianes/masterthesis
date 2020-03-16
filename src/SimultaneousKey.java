public class SimultaneousKey {
    private int simType;
    private int connectedOp;

    public SimultaneousKey(int simType, int connectedOp) {
        this.simType = simType;
        this.connectedOp = connectedOp;
    }

    @Override
    public int hashCode() {
        if(simType==0){
            return simType+connectedOp;
        }
        else{
            return simType-connectedOp;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SimultaneousKey) && ((SimultaneousKey) obj).getSimType()==simType
                && ((SimultaneousKey) obj).getConnectedOp()==connectedOp;
    }

    public int getSimType() {
        return simType;
    }

    public int getConnectedOp() {
        return connectedOp;
    }
}
