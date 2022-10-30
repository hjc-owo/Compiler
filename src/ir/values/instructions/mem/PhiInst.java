package ir.values.instructions.mem;

import ir.types.Type;
import ir.values.BasicBlock;
import ir.values.Value;
import ir.values.instructions.Operator;

import java.util.List;

public class PhiInst extends MemInst {
    public PhiInst(BasicBlock basicBlock, Type type, List<Value> values) {
        super(type, Operator.Phi, basicBlock);
        for (Value value : values) {
            this.addOperand(value);
        }
        this.setName("%" + REG_NUMBER++);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(getName() + " = phi " + getType() + " ");
        for (int i = 0; i < getOperands().size(); i++) {
            if (i != 0) {
                s.append(", ");
            }
            s.append("[ ").append(getOperands().get(i).getName()).append(", %").append(getNode().getParent().getValue().getPredecessors().get(i).getName()).append(" ]");
        }
        return s.toString();
    }
}
