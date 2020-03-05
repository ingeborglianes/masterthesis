public class PrecedenceKey {

    private int presType;
    private int connectedOp;

    public PrecedenceKey(int presType, int connectedOp) {
        this.presType = presType;
        this.connectedOp = connectedOp;
    }

    @Override
    public int hashCode() {
        if(presType==0){
            return presType+connectedOp;
        }
        else{
            return presType-connectedOp;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof PrecedenceKey) && ((PrecedenceKey) obj).getPresType()==presType
                && ((PrecedenceKey) obj).getConnectedOp()==connectedOp;
    }

    public int getPresType() {
        return presType;
    }

    public int getConnectedOp() {
        return connectedOp;
    }
}
