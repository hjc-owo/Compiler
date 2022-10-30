package ir.values.instructions.mem;

import ir.types.ArrayType;
import ir.types.PointerType;
import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.instructions.Operator;

public class AllocaInst extends MemInst {
    private boolean isConst;
    private Type allocaType;

    public AllocaInst(BasicBlock basicBlock, boolean isConst, Type allocaType) {
        super(new PointerType(allocaType), Operator.Alloca, basicBlock);
        this.setName("%" + REG_NUMBER++);
        this.isConst = isConst;
        this.allocaType = allocaType;
        if (allocaType instanceof ArrayType) {
            if (((ArrayType) allocaType).getLength() == -1) {
                this.allocaType = new PointerType(((ArrayType) allocaType).getElementType());
                setType(new PointerType(this.allocaType));
            }
        }
    }

    public boolean isConst() {
        return isConst;
    }

    public void setConst(boolean aConst) {
        isConst = aConst;
    }

    public Type getAllocaType() {
        return allocaType;
    }

    public void setAllocaType(Type allocaType) {
        this.allocaType = allocaType;
    }

    @Override
    public String toString() {
        return this.getName() + " = alloca " + this.getAllocaType();
    }
}
