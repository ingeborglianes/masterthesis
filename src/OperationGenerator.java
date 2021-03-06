public class OperationGenerator {

    public OperationType[] createOperationTypes() {
        //number,vessel1,vessel2,vesselBigTask, presedensOver, presedensAv, mHyppighet, sdHyppighet,
        //varighet, penalty
        int[] op1v1 = new int[]{2, 3, 4, 6};
        OperationType op1 = new OperationType(1, op1v1, null, null, 2
                , 0, 17520, 730, 8, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,
                0,"Transport net before operation");
        int[] op2v1 = new int[]{2, 3, 4, 6};
        int[] op2v2 = new int[]{2, 3, 4, 6};
        OperationType op2 = new OperationType(2, op2v1, op2v2, null,
                3, 1, 17520, 730, 4, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Install net");
        int[] op3v1 = new int[]{2, 3, 4, 6};
        OperationType op3 = new OperationType(3, op3v1, null,
                null, 0, 2, 17520, 730, 8, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Transport net after operation");
        int[] op4v1 = new int[]{5};
        int[] op4v2 = new int[]{2, 3, 4, 6};
        OperationType op4 = new OperationType(4, op4v1, op4v2, null,
                0, 0, 1152, 192, 5, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 8,0, "Delousing");
        int[] op5v1 = new int[]{2, 3, 4};
        int[] op5v2 = new int[]{2, 3, 4};
        int[] op5BT = new int[]{6};
        OperationType op5 = new OperationType(5, op5v1, op5v2, op5BT,
                0, 0, 8760, 360, 20, DataGenerator.costPenalty * DataGenerator.maxSailingTime*2,0, "Large inspection of the facility");
        int[] op6v1 = new int[]{2};
        OperationType op6 = new OperationType(6, op6v1, null, null,
                0, 0, 5110, 730, 5, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Wash the net");
        int[] op7v1 = new int[]{4, 6};
        OperationType op7 = new OperationType(7, op7v1, null, null,
                0, 0, 8760, 360, 48, DataGenerator.costPenalty * DataGenerator.maxSailingTime*2,0, "tightening anchor lines");
        int[] op8v1 = new int[]{2, 3, 4, 6};
        OperationType op8 = new OperationType(8, op8v1, null, null,
                9, 0, 8760, 360, 6, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Easy transport of equipment to facility");
        int[] op9v1 = new int[]{2, 3, 4, 6};
        int[] op9v2 = new int[]{2, 3, 4, 6};
        OperationType op9 = new OperationType(9, op9v1, op9v2, null,
                0, 8, 8760, 360, 3, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Small installation facility");
        int[] op10v1 = new int[]{2, 3, 4, 6};
        OperationType op10 = new OperationType(10, op10v1, null, null,
                0, 0, 720, 100, 2, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Remove dead fish");
        int[] op11v1 = new int[]{2, 3, 4, 6};
        OperationType op11 = new OperationType(11, op11v1, null, null,
                0, 0, 720, 100, 4, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 8,1, "Support wellboat");
        int[] op12v1 = new int[]{1, 2, 3, 4, 6};
        OperationType op12 = new OperationType(12, op12v1, null, null,
                0, 0, 720, 100, 5, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Inspect net ROV");
        int[] op13v1 = new int[]{1, 3};
        OperationType op13 = new OperationType(13, op13v1, null, null,
                0, 0, 8760, 360, 4, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Inspect net diver");
        int[] op14v1 = new int[]{2};
        OperationType op14 = new OperationType(14, op14v1, null, null,
                0, 0, 8760, 360, 4, DataGenerator.costPenalty * DataGenerator.maxSailingTime*2,0, "Wash bottom ring and floating collar");
        int[] op15v1 = new int[]{2, 3, 4, 6};
        OperationType op15 = new OperationType(15, op15v1, null, null,
                0, 0, 168, 24, 3, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Support working boat");
        return new OperationType[]{op1, op2, op3, op4, op5, op6, op7, op8, op9, op10, op11, op12, op13, op14, op15};
    }

    public OperationType[] createOperationTypesTest() {
        int[] op9v1 = new int[]{2, 3, 4, 6};
        int[] op9v2 = new int[]{2, 3, 4, 6};
        OperationType op9 = new OperationType(9, op9v1, op9v2, null,
                0, 8, 700, 160, 3, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Small installation facility");
        int[] op8v1 = new int[]{2, 3, 4, 6};
        OperationType op8 = new OperationType(8, op9v1, null, null,
                9, 0, 700, 160, 6, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Easy transport of equipment to facility");
        //int[] op8v1 = new int[]{2, 3, 4, 6};
        //int[] op8v2 = new int[]{2, 3, 4, 6};
        //OperationType op8 = new OperationType(8, op8v1, op8v2, null,
        //        0, 0, 600, 30, 3, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4, "Small installation facility");
        int[] op10v1 = new int[]{2, 3, 4, 6};
        OperationType op10 = new OperationType(10, op10v1, null, null,
                0, 0, 400, 100, 2, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Remove dead fish");
        int[] op12v1 = new int[]{1, 2, 3, 4, 6};
        OperationType op12 = new OperationType(12, op12v1, null, null,
                0, 0, 700, 100, 5, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Inspect net ROV");
        int[] op13v1 = new int[]{1, 3};
        OperationType op13 = new OperationType(13, op13v1, null, null,
                0, 0, 700, 36, 4, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Inspect net diver");
        int[] op14v1 = new int[]{2};
        OperationType op14 = new OperationType(14, op14v1, null, null,
                0, 0, 700, 36, 4, DataGenerator.costPenalty * DataGenerator.maxSailingTime,0, "Wash bottom ring and floating collar");
        int[] op11v1 = new int[]{2, 3, 4, 6};
        OperationType op11 = new OperationType(11, op11v1, null, null,
                0, 0, 700, 73, 4, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 8,1, "Support wellboat");
        int[] op15v1 = new int[]{2, 3, 4, 6};
        OperationType op15 = new OperationType(15, op15v1, null, null,
                0, 0, 700, 24, 3, DataGenerator.costPenalty * DataGenerator.maxSailingTime * 4,0, "Support working boat");
        int[] op5v1 = new int[]{2, 3, 4};
        int[] op5v2 = new int[]{2, 3, 4};
        int[] op5BT = new int[]{6};
        OperationType op5 = new OperationType(5, op5v1, op5v2, op5BT,
                0, 0, 700, 24, 10, DataGenerator.costPenalty * DataGenerator.maxSailingTime,0, "Large inspection of the facility");

        return new OperationType[]{op8,op9,op10,op11,op12,op13,op14,op15};
    }
}


