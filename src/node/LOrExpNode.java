package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

public class LOrExpNode {
    // LOrExp -> LAndExp | LOrExp '||' LAndExp
    private LAndExpNode lAndExpNode;
    private Token orToken;
    private LOrExpNode lOrExpNode;

    public LOrExpNode(LAndExpNode lAndExpNode, Token operator, LOrExpNode lOrExpNode) {
        this.lAndExpNode = lAndExpNode;
        this.orToken = operator;
        this.lOrExpNode = lOrExpNode;
    }

    public LAndExpNode getLAndExpNode() {
        return lAndExpNode;
    }

    public Token getOrToken() {
        return orToken;
    }

    public LOrExpNode getLOrExpNode() {
        return lOrExpNode;
    }

    public void print() {
        lAndExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.LOrExp));
        if (orToken != null) {
            IOUtils.write(orToken.toString());
            lOrExpNode.print();
        }
    }
}
