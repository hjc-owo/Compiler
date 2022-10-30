package ir.types;

public class LabelType implements Type {
    private final int handler;
    private static int HANDLER = 0;

    public LabelType() {
        handler = HANDLER++;
    }

    @Override
    public String toString() {
        return "label_" + handler;
    }
}
