package ir.types;

public class PointerType implements Type {
    private final Type targetType;

    public PointerType(Type targetType) {
        this.targetType = targetType;
    }

    public Type getTargetType() {
        return targetType;
    }

    public boolean isString() {
        return targetType instanceof ArrayType && ((ArrayType) targetType).isString();
    }

    @Override
    public String toString() {
        return targetType.toString() + "*";
    }
}
