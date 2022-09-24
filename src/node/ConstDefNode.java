package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class ConstDefNode {
    // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    private Token ident;
    private List<ConstExpNode> constExpNodes;
    private ConstInitValNode constInitValNode;

    public ConstDefNode(Token ident, List<ConstExpNode> constExpNodes, ConstInitValNode constInitValNode) {
        this.ident = ident;
        this.constExpNodes = constExpNodes;
        this.constInitValNode = constInitValNode;
    }

    public void print() {
        IOUtils.write(ident.toString());
        for (ConstExpNode constExpNode : constExpNodes) {
            IOUtils.write(Token.constTokens.get(TokenType.LBRACK).toString());
            constExpNode.print();
            IOUtils.write(Token.constTokens.get(TokenType.RBRACK).toString());
        }
        IOUtils.write(Token.constTokens.get(TokenType.ASSIGN).toString());
        constInitValNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.ConstDef));
    }
}
