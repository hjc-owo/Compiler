package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class VarDeclNode {
    // VarDecl -> BType VarDef { ',' VarDef } ';'
    private BTypeNode bTypeNode;
    private List<VarDefNode> varDefNodes;

    public VarDeclNode(BTypeNode bTypeNode, List<VarDefNode> varDefNodes) {
        this.bTypeNode = bTypeNode;
        this.varDefNodes = varDefNodes;
    }

    public void print() {
        bTypeNode.print();
        varDefNodes.get(0).print();
        for (int i = 1; i < varDefNodes.size(); i++) {
            IOUtils.write(Token.constTokens.get(TokenType.COMMA).toString());
            varDefNodes.get(i).print();
        }
        IOUtils.write(Token.constTokens.get(TokenType.SEMICN).toString());
        IOUtils.write(Parser.nodeType.get(NodeType.VarDecl));
    }
}
