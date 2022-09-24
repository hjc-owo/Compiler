package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class LValNode {
    // LVal -> Ident {'[' Exp ']'}
    private Token ident;
    private List<ExpNode> expNodes;

    public LValNode(Token ident, List<ExpNode> expNodes) {
        this.ident = ident;
        this.expNodes = expNodes;
    }

    public void print() {
        IOUtils.write(ident.toString());
        for (ExpNode expNode : expNodes) {
            IOUtils.write(Token.constTokens.get(TokenType.LBRACK).toString());
            expNode.print();
            IOUtils.write(Token.constTokens.get(TokenType.RBRACK).toString());
        }
        IOUtils.write(Parser.nodeType.get(NodeType.LVal));
    }
}
