package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class FuncFParamNode {
    // FuncFParam -> BType Ident [ '[' ']' { '[' ConstExp ']' }]

    private BTypeNode bTypeNode;
    private Token ident;
    private Token lbrack;
    private List<ConstExpNode> constExpNodes;

    public FuncFParamNode(BTypeNode bTypeNode, Token ident, Token lbrack, List<ConstExpNode> constExpNodes) {
        this.bTypeNode = bTypeNode;
        this.ident = ident;
        this.lbrack = lbrack;
        this.constExpNodes = constExpNodes;
    }

    public void print() {
        bTypeNode.print();
        IOUtils.write(ident.toString());
        if (lbrack != null) {
            IOUtils.write(lbrack.toString());
            IOUtils.write(Token.constTokens.get(TokenType.RBRACK).toString());
            for (ConstExpNode constExpNode : constExpNodes) {
                IOUtils.write(Token.constTokens.get(TokenType.LBRACK).toString());
                constExpNode.print();
                IOUtils.write(Token.constTokens.get(TokenType.RBRACK).toString());
            }
        }
        IOUtils.write(Parser.nodeType.get(NodeType.FuncFParam));
    }
}
