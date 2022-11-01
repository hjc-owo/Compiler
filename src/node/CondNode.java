package node;

import frontend.Parser;
import utils.IOUtils;

public class CondNode {
    // Cond -> LOrExp

    private LOrExpNode lOrExpNode;

    public CondNode(LOrExpNode lOrExpNode) {
        this.lOrExpNode = lOrExpNode;
    }

    public LOrExpNode getLOrExpNode() {
        return lOrExpNode;
    }

    public void print() {
        lOrExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.Cond));
    }
}
