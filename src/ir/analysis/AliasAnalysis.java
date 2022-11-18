package ir.analysis;

import ir.types.PointerType;
import ir.values.GlobalVar;
import ir.values.Use;
import ir.values.Value;
import ir.values.instructions.mem.AllocaInst;
import ir.values.instructions.mem.GEPInst;
import ir.values.instructions.mem.LoadInst;
import ir.values.instructions.mem.StoreInst;

public class AliasAnalysis {
    public static Value getArrayValue(Value pointer) {
        Value pt = pointer;
        while (pt instanceof GEPInst || pt instanceof LoadInst) {
            if (pt instanceof GEPInst) {
                pt = ((GEPInst) pt).getPointer();
            } else {
                pt = ((LoadInst) pt).getPointer();
            }
        }
        if (pt instanceof AllocaInst || pt instanceof GlobalVar) {
            if (pt instanceof AllocaInst && ((AllocaInst) pt).getAllocaType() instanceof PointerType) {
                for (Use use : pt.getUsesList()) {
                    if (use.getUser() instanceof StoreInst) {
                        pt = use.getUser().getOperands().get(1);
                    }
                }
            }
            return pt;
        } else {
            return null;
        }
    }

    public static boolean isGlobal(Value array) {
        return array instanceof GlobalVar;
    }

    public static boolean isParam(Value array) {
        if (array instanceof AllocaInst) {
            return ((AllocaInst) array).getAllocaType() instanceof PointerType;
        } else {
            return false;
        }
    }
}
