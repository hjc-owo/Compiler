package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

public class MainFuncDefNode {
    // MainFuncDef -> 'int' 'main' '(' ')' Block

    private Token intToken;
    private Token mainToken;
    private Token leftParentToken;
    private Token rightParentToken;
    private BlockNode blockNode;

    public MainFuncDefNode(Token intToken, Token mainToken, Token leftParentToken, Token rightParentToken, BlockNode blockNode) {
        this.intToken = intToken;
        this.mainToken = mainToken;
        this.leftParentToken = leftParentToken;
        this.rightParentToken = rightParentToken;
        this.blockNode = blockNode;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public void print() {
        IOUtils.write(intToken.toString());
        IOUtils.write(mainToken.toString());
        IOUtils.write(leftParentToken.toString());
        IOUtils.write(rightParentToken.toString());
        blockNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.MainFuncDef));
    }
}
