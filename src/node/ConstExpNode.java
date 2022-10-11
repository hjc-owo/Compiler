package node;

import frontend.Parser;
import symbol.SymbolTable;
import utils.IOUtils;

public class ConstExpNode {
    // ConstExp -> AddExp

    private AddExpNode addExpNode;

    public ConstExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public int getValue() {
        return addExpNode.getValue();
    }

    public void print() {
        addExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.ConstExp));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        addExpNode.fillSymbolTable(currentSymbolTable);
    }
}
