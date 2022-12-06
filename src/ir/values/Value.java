package ir.values;

import ir.IRModule;
import ir.types.Type;

import java.util.ArrayList;
import java.util.List;

public class Value {
    private final IRModule module = IRModule.getInstance();
    private String name;
    private Type type;
    private List<Use> usesList; // 使用了这个 Value 的 User 列表，这对应着 def-use 关系
    public static int REG_NUMBER = 0; // LLVM 中的寄存器编号
    private final String id; // LLVM 中的 Value 的唯一编号

    public Value(String name, Type type) {
        this.name = name;
        this.type = type;
        this.id = UniqueIdGen.getInstance().getUniqueId();
        this.usesList = new ArrayList<>();
    }

    public IRModule getModule() {
        return module;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<Use> getUsesList() {
        return usesList;
    }

    public void setUsesList(List<Use> usesList) {
        this.usesList = usesList;
    }

    public void addUse(Use use) {
        this.usesList.add(use);
    }

    public void removeUseByUser(User user) {
        usesList.removeIf(use -> use.getUser() == user);
    }

    public void removeFromUseList(Use use) {
        usesList.remove(use);
    }

    public String getId() {
        return id;
    }

    public String getUniqueName() {
        if (isNumber()) return getName();
        if (isGlobal()) return getGlobalName();
        return getName() + "_" + getId();
    }

    public String getGlobalName() {
        return name.replaceAll("@", "");
    }

    public boolean isNumber() {
        return this instanceof ConstInt;
    }

    public int getNumber() {
        return Integer.parseInt(name);
    }

    public boolean isGlobal() {
        return name.startsWith("@");
    }

    @Override
    public String toString() {
        return type.toString() + " " + name;
    }

    // 将该 this 的所有被用到的 user 中的操作数都替换成 value
    public void replaceUsedWith(Value value) {
        List<Use> tmp = new ArrayList<>(usesList);
        for (Use use : tmp) {
            use.getUser().setOperands(use.getPosOfOperand(), value);
        }
        this.usesList.clear();
    }
}