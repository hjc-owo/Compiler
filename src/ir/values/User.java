package ir.values;

import ir.types.Type;

import java.util.ArrayList;
import java.util.List;

public class User extends Value {
    private List<Value> operands; // 这个 User 使用的 Value 的列表，这对应着 use-def 关系

    public User(String name, Type type) {
        super(name, type);
        this.operands = new ArrayList<>();
    }

    public List<Value> getOperands() {
        return operands;
    }

    public void setOperands(List<Value> operands) {
        this.operands = operands;
    }

    public void addOperand(int posOfOperand, Value operand) {
        if (posOfOperand >= operands.size()) {
            return;
        }
        this.operands.set(posOfOperand, operand);
        if (operand != null) {
            operand.addUse(new Use(operand, this, posOfOperand));
        }
    }

    public void addOperand(Value operand) {
        this.operands.add(operand);
        if (operand != null) {
            operand.addUse(new Use(operand, this, operands.size() - 1));
        }
    }

    public void removeUseFromOperands() {
        if (operands == null) {
            return;
        }
        for (Value operand : operands) {
            if (operand != null) {
                operand.removeUseByUser(this);
            }
        }
    }

}
