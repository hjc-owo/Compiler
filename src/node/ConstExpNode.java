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

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public void print() {
        addExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.ConstExp));
    }
}
