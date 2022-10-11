package symbol;

public class FuncSymbolTable extends SymbolTable {

    private FuncType type; // 函数返回类型

    public FuncSymbolTable() {
        super();
    }

    public FuncSymbolTable(SymbolTable parentSymbolTable) {
        super(parentSymbolTable);
    }

    public FuncSymbolTable(SymbolTable parentSymbolTable, FuncType type) {
        super(parentSymbolTable);
        this.type = type;
    }

    public FuncType getType() {
        return type;
    }


}
