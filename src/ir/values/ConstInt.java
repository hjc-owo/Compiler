package ir.values;

import ir.types.IntegerType;

import java.util.Objects;

public class ConstInt extends Const {
    private int value;
    public static ConstInt ZERO = new ConstInt(0);

    public ConstInt() {
        super("", IntegerType.i32);
        this.value = 0;
    }

    public ConstInt(int value) {
        super(String.valueOf(value), IntegerType.i32);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstInt that = (ConstInt) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "i32 " + this.value;
    }
}
