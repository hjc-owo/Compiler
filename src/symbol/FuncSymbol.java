package symbol;

import java.util.List;

public class FuncSymbol extends Symbol { // 函数符号

    private FuncType type; // 函数返回类型
    private List<FuncFParam> funcFParams; // 函数形参表

    public FuncSymbol(String name, FuncType type, List<FuncFParam> funcFParams) {
        super(name);
        this.type = type;
        this.funcFParams = funcFParams;
    }

    public FuncType getType() {
        return type;
    }

    public List<FuncFParam> getFuncFParams() {
        return funcFParams;
    }
}
