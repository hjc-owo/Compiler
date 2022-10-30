package ir.values;

import ir.types.Type;

public class Argument extends Value {
    private int index;

    public Argument(String name, Type type, int index) {
        super(name, type);
        this.index = index;
    }

    public Argument(Type type, int index, boolean isLibraryFunction) {
        super(isLibraryFunction ? "" : "%" + REG_NUMBER++, type);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return this.getType().toString() + " " + this.getName();
    }

}
