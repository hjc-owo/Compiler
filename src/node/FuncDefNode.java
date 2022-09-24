package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

public class FuncDefNode {
    // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block

    private FuncTypeNode funcTypeNode;
    private Token ident;
    private FuncFParamsNode funcFParamsNode;
    private BlockNode blockNode;

    public FuncDefNode(FuncTypeNode funcTypeNode, Token ident, FuncFParamsNode funcFParamsNode, BlockNode blockNode) {
        this.funcTypeNode = funcTypeNode;
        this.ident = ident;
        this.funcFParamsNode = funcFParamsNode;
        this.blockNode = blockNode;
    }

    public void print() {
        funcTypeNode.print();
        IOUtils.write(ident.toString());
        IOUtils.write(Token.constTokens.get(TokenType.LPARENT).toString());
        if (funcFParamsNode != null) {
            funcFParamsNode.print();
        }
        IOUtils.write(Token.constTokens.get(TokenType.RPARENT).toString());
        blockNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.FuncDef));
    }
}
