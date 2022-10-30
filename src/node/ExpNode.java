package node;

import frontend.Parser;
import symbol.FuncParam;
import symbol.SymbolTable;
import utils.IOUtils;

public class ExpNode {
    // Exp -> AddExp

    private AddExpNode addExpNode;

    public ExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public void print() {
        addExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.Exp));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        addExpNode.fillSymbolTable(currentSymbolTable);
    }

    public FuncParam getFuncParam() {
        return addExpNode.getFuncParam();
    }

    public String getStr() {
        return addExpNode.getStr();
    }
}
