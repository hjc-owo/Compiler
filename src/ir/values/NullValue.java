package ir.values;

import ir.types.VoidType;

public class NullValue extends Value {
    public NullValue() {
        super("null", VoidType.voidType);
    }
}