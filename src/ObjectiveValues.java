public class ObjectiveValues {
    private int objvalue;
    private int[] routeSailingCost;
    private int[] routeBenefitGain;

    public ObjectiveValues(int objvalue, int[] routeSailingCost, int[] routeBenefitGain){
        this.objvalue=objvalue;
        this.routeSailingCost=routeSailingCost;
        this.routeBenefitGain=routeBenefitGain;
    }

    public int getObjvalue() {
        return objvalue;
    }

    public int[] getRouteSailingCost() {
        return routeSailingCost;
    }

    public int[] getRouteBenefitGain() {
        return routeBenefitGain;
    }
}
