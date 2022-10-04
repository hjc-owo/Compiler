package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

public class PrimaryExpNode {
    // PrimaryExp -> '(' Exp ')' | LVal | Number

    private Token leftParentToken = null;
    private ExpNode expNode = null;
    private Token rightParentToken = null;
    private LValNode lValNode = null;
    private NumberNode numberNode = null;

    public PrimaryExpNode(Token leftParentToken, ExpNode expNode, Token rightParentToken) {
        this.leftParentToken = leftParentToken;
        this.expNode = expNode;
        this.rightParentToken = rightParentToken;
    }

    public PrimaryExpNode(LValNode lValNode) {
        this.lValNode = lValNode;
    }

    public PrimaryExpNode(NumberNode numberNode) {
        this.numberNode = numberNode;
    }

    public void print() {
        if (expNode != null) {
            IOUtils.write(leftParentToken.toString());
            expNode.print();
            IOUtils.write(rightParentToken.toString());
        } else if (lValNode != null) {
            lValNode.print();
        } else {
            numberNode.print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.PrimaryExp));
    }
}
