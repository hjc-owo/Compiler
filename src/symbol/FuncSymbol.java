package symbol;

import java.util.List;

public class FuncSymbol extends Symbol { // 函数符号

    private FuncType type; // 函数返回类型
    private List<FuncParam> funcParams; // 函数参数表

    public FuncSymbol(String name, FuncType type, List<FuncParam> funcParams) {
        super(name);
        this.type = type;
        this.funcParams = funcParams;
    }

    public FuncType getType() {
        return type;
    }

    public List<FuncParam> getFuncParams() {
        return funcParams;
    }
}
