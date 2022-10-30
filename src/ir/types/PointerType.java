package ir.types;

public class PointerType implements Type {
    private final Type targetType;

    public PointerType(Type targetType) {
        this.targetType = targetType;
    }

    public Type getTargetType() {
        return targetType;
    }

    @Override
    public String toString() {
        return targetType.toString() + "*";
    }
}
