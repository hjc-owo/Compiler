package node;

import frontend.Parser;
import utils.IOUtils;

public class ExpNode {
    // Exp -> AddExp

    private AddExpNode addExpNode;

    public ExpNode(AddExpNode addExpNode) {
        this.addExpNode = addExpNode;
    }

    public void print() {
        addExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.Exp));
    }
}
