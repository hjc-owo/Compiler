package node;

import frontend.Parser;
import symbol.FuncRParam;
import symbol.SymbolTable;
import utils.IOUtils;

public class ExpNode {
    // Exp -> AddExp

    private AddExpNode addExpNode;

    public ExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public int getValue() {
        return addExpNode.getValue();
    }

    public void print() {
        addExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.Exp));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        addExpNode.fillSymbolTable(currentSymbolTable);
    }

    public FuncRParam getFuncRParam() {
        return addExpNode.getFuncRParam();
    }
}
