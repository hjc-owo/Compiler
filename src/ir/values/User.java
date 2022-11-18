package ir.values;

import ir.types.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class User extends Value {
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

    public void setOperands(int pos, Value operand) {
        if (pos >= operands.size()) {
            return;
        }
        this.operands.set(pos, operand);
        if (operand != null) {
            operand.addUse(new Use(operand, this, pos));
        }
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

    public void removeNumberOperand(HashSet<Integer> idx) {
        removeUseFromOperands();
        List<Value> tmp = new ArrayList<>(operands);
        operands.clear();
        for (int i = 0; i < tmp.size(); i++) {
            //不在就把这个Value加回operands里
            if (!idx.contains(i)) {
                tmp.get(i).addUse(new Use(tmp.get(i), this, operands.size()));
                this.operands.add(tmp.get(i));
            }
        }
    }

}
