package ir.values.instructions;

import ir.types.IntegerType;
import ir.types.PointerType;
import ir.types.VoidType;
import ir.values.BasicBlock;
import ir.values.Value;

public class ConvInst extends Instruction {
    public ConvInst(BasicBlock basicBlock, Operator op, Value value) {
        super(VoidType.voidType, op, basicBlock);
        this.setName("%" + REG_NUMBER++);
        if (op == Operator.Zext) {
            setType(IntegerType.i32);
        } else if (op == Operator.Bitcast) {
            setType(new PointerType(IntegerType.i32));
        }
        addOperand(value);
    }

    @Override
    public String toString() {
        if (getOperator() == Operator.Zext) {
            return getName() + " = zext i1 " + getOperands().get(0).getName() + " to i32";
        } else if (getOperator() == Operator.Bitcast) {
            return getName() + " = bitcast " + getOperands().get(0).getType() + getOperands().get(0).getName() + " to i32*";
        } else {
            return null;
        }
    }
}
