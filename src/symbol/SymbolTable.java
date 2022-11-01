package symbol;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    public Map<String, Symbol> symbols = new HashMap<>(); // 查找表
    private SymbolTable parent = null; // 父符号表

    public SymbolTable() {
    }

    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
    }

    public Symbol get(String ident) {
        Symbol symbol = symbols.get(ident);
        if (symbol != null) {
            return symbol;
        } else if (parent != null) {
            return parent.get(ident);
        }
        return null;
    }

    public boolean contains(String ident) {
        if (symbols.containsKey(ident)) {
            return true;
        } else if (parent != null) {
            return parent.contains(ident);
        }
        return false;
    }

    public boolean containsInCurrent(String ident) {
        return symbols.containsKey(ident);
    }

    public void put(String ident, Symbol symbol) {
        symbols.put(ident, symbol);
    }
}
