package ir.values;

import ir.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class User extends Value {
    private final List<Value> operands;

    public User(String name, Type type) {
        super(name, type);
        this.operands = new ArrayList<>();
    }

    public List<Value> getOperands() {
        return operands;
    }

    public Value getOperand(int index) {
        return operands.get(index);
    }

    public void addOperand(Value operand) {
        this.operands.add(operand);
        if (operand != null) {
            operand.addUse(new Use(operand, this, operands.size() - 1));
        }
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

    // 从 this 的所有 operands 对应的 value 删除
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

    // 把 operands 中的操作数替换成 value， 并更新 def-use 关系
    public void replaceOperands(int indexOf, Value value) {
        Value operand = operands.get(indexOf);
        this.setOperands(indexOf, value);
        if (operand != null && !this.operands.contains(value)) {
            operand.removeUseByUser(this);
        }
    }

    // 把oldOperand转换为newOperand
    public void replaceOperands(Value oldValue, Value newValue) {
        oldValue.removeUseByUser(this);
        for (int i = 0; i < operands.size(); i++) {
            if (operands.get(i) == oldValue) {
                replaceOperands(i, newValue);
            }
        }
    }

    public void removeNumberOperand(Set<Integer> idx) {
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
