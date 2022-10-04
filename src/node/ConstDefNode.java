package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class ConstDefNode {
    // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    private Token ident;
    private List<Token> leftBrackets;
    private List<ConstExpNode> constExpNodes;
    private List<Token> rightBrackets;
    private Token equalToken;
    private ConstInitValNode constInitValNode;

    public ConstDefNode(Token ident, List<Token> leftBrackets, List<ConstExpNode> constExpNodes, List<Token> rightBrackets, Token equalToken, ConstInitValNode constInitValNode) {
        this.ident = ident;
        this.leftBrackets = leftBrackets;
        this.constExpNodes = constExpNodes;
        this.rightBrackets = rightBrackets;
        this.equalToken = equalToken;
        this.constInitValNode = constInitValNode;
    }

    public void print() {
        IOUtils.write(ident.toString());
        for (int i = 0; i < constExpNodes.size(); i++) {
            IOUtils.write(leftBrackets.get(i).toString());
            constExpNodes.get(i).print();
            IOUtils.write(rightBrackets.get(i).toString());
        }
        IOUtils.write(equalToken.toString());
        constInitValNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.ConstDef));
    }
}
