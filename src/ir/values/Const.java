package ir.values;

import ir.types.Type;

public abstract class Const extends Value {
    public Const(String name, Type type) {
        super(name, type);
    }
}
