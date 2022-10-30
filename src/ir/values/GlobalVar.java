package ir.values;

import ir.IRModule;
import ir.types.PointerType;
import ir.types.Type;

public class GlobalVar extends User {

    private boolean isConst; // 是否是常量
    private Value value;

    public GlobalVar(String name, Type type, boolean isConst, Value value) {
        super("@" + name, new PointerType(type));
        this.isConst = isConst;
        this.value = value;
        IRModule.getInstance().addGlobalVar(this);
    }

    public boolean isConst() {
        return isConst;
    }

    public void setConst(boolean aConst) {
        isConst = aConst;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getName()).append(" = ");
        if (isConst) {
            sb.append("constant ");
        } else {
            sb.append("global ");
        }
        if (value != null) {
            sb.append(value);
        }
        return sb.toString();
    }
}
