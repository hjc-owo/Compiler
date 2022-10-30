package ir.values.instructions;

import ir.types.Type;
import ir.values.Const;
import ir.values.ConstInt;
import ir.values.Value;

import java.util.ArrayList;
import java.util.List;

public class ConstArray extends Const {
    private Type elementType;
    private List<Value> array;
    private int capacity;

    public ConstArray(Type type, Type elementType, int capacity) {
        super("", type);
        this.elementType = elementType;
        this.array = new ArrayList<>();
        this.capacity = capacity;
    }

    public Type getElementType() {
        return elementType;
    }

    public void setElementType(Type elementType) {
        this.elementType = elementType;
    }

    public List<Value> getArray() {
        return array;
    }

    public void setArray(List<Value> array) {
        this.array = array;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void storeValue(int offset, Value value) {
        if (offset == array.size()) {
            array.add(value);
        } else if (offset > array.size()) {
            int size = array.size();
            while (offset != size) {
                array.add(ConstInt.ZERO);
                size = array.size();
            }
            array.add(value);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getType().toString()).append("[");
        for (Value value : array) {
            sb.append(value.toString()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }
}
