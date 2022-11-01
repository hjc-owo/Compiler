package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

public class FuncDefNode {
    // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block

    private FuncTypeNode funcTypeNode;
    private Token ident;
    private Token leftParentToken;
    private FuncFParamsNode funcFParamsNode;
    private Token rightParentToken;
    private BlockNode blockNode;

    public FuncDefNode(FuncTypeNode funcTypeNode, Token ident, Token leftParentToken, FuncFParamsNode funcFParamsNode, Token rightParentToken, BlockNode blockNode) {
        this.funcTypeNode = funcTypeNode;
        this.ident = ident;
        this.leftParentToken = leftParentToken;
        this.funcFParamsNode = funcFParamsNode;
        this.rightParentToken = rightParentToken;
        this.blockNode = blockNode;
    }

    public FuncTypeNode getFuncTypeNode() {
        return funcTypeNode;
    }

    public Token getIdent() {
        return ident;
    }

    public FuncFParamsNode getFuncFParamsNode() {
        return funcFParamsNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public void print() {
        funcTypeNode.print();
        IOUtils.write(ident.toString());
        IOUtils.write(leftParentToken.toString());
        if (funcFParamsNode != null) {
            funcFParamsNode.print();
        }
        IOUtils.write(rightParentToken.toString());
        blockNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.FuncDef));
    }
}
