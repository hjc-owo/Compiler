package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

public class PrimaryExpNode {
    // PrimaryExp -> '(' Exp ')' | LVal | Number

    private ExpNode expNode;
    private LValNode lValNode;
    private NumberNode numberNode;

    public PrimaryExpNode(ExpNode expNode, LValNode lValNode, NumberNode numberNode) {
        this.expNode = expNode;
        this.lValNode = lValNode;
        this.numberNode = numberNode;
    }

    public void print() {
        if (expNode != null) {
            IOUtils.write(Token.constTokens.get(TokenType.LPARENT).toString());
            expNode.print();
            IOUtils.write(Token.constTokens.get(TokenType.RPARENT).toString());
        } else if (lValNode != null) {
            lValNode.print();
        } else {
            numberNode.print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.PrimaryExp));
    }
}
