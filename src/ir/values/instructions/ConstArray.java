package ir.values.instructions;

import ir.types.ArrayType;
import ir.types.Type;
import ir.values.Const;
import ir.values.ConstInt;
import ir.values.Value;

import java.util.ArrayList;
import java.util.List;

public class ConstArray extends Const {
    // 高维数组应该往下递归
    private Type elementType;
    private List<Value> array;
    private int capacity;

    public ConstArray(Type type, Type elementType, int capacity) {
        super("", type);
        this.elementType = elementType;
        this.array = new ArrayList<>();
        this.capacity = capacity;
        if (elementType instanceof ArrayType) {
            for (int i = 0; i < ((ArrayType) type).getLength(); i++) {
                array.add(new ConstArray(elementType, ((ArrayType) elementType).getElementType(), ((ArrayType) elementType).getCapacity()));
            }
        } else {
            for (int i = 0; i < ((ArrayType) type).getLength(); i++) {
                array.add(ConstInt.ZERO);
            }
        }
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
        // recursion
        if (elementType instanceof ArrayType) {
            ((ConstArray) (array.get(offset / ((ArrayType) elementType).getCapacity()))).storeValue(offset % ((ArrayType) elementType).getCapacity(), value);
        } else {
            array.set(offset, value);
        }
    }

    private boolean allZero() {
        for (Value value : array) {
            if (value instanceof ConstInt) {
                if (((ConstInt) value).getValue() != 0) {
                    return false;
                }
            } else {
                if (!((ConstArray) value).allZero()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        if (allZero()) {
            return this.getType().toString() + " " + "zeroinitializer";
        } else {
            // use recursion to output llvm global array
            // like this:
            // [3 x [2 x i32]] [[2 x i32] [i32 1, i32 2], [i32 3, i32 4], [i32 5, i32 6]]
            // [4 x i32] [i32 1, i32 2, i32 3, i32 4]
            // [2 x [3 x [2 x i32]]] [[3 x [2 x i32]] [[2 x i32] [i32 1, i32 2], [i32 3, i32 4], [i32 5, i32 6]], [3 x [2 x i32]] [[2 x i32] [i32 7, i32 8], [i32 9, i32 10], [i32 11, i32 12]]]
            StringBuilder sb = new StringBuilder();
            sb.append(this.getType().toString()).append(" ").append("[");
            for (int i = 0; i < array.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(array.get(i).toString());
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
