package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class ConstDeclNode {
    // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'

    private BTypeNode bTypeNode;
    private List<ConstDefNode> constDefNodes;

    public ConstDeclNode(BTypeNode bTypeNode, List<ConstDefNode> constDefNodes) {
        this.bTypeNode = bTypeNode;
        this.constDefNodes = constDefNodes;
    }

    public void print() {
        IOUtils.write(Token.constTokens.get(TokenType.CONSTTK).toString());
        bTypeNode.print();
        constDefNodes.get(0).print();
        for (int i = 1; i < constDefNodes.size(); i++) {
            IOUtils.write(Token.constTokens.get(TokenType.COMMA).toString());
            constDefNodes.get(i).print();
        }
        IOUtils.write(Token.constTokens.get(TokenType.SEMICN).toString());
        IOUtils.write(Parser.nodeType.get(NodeType.ConstDecl));
    }
}
