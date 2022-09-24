package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

public class MainFuncDefNode {
    // MainFuncDef -> 'int' 'main' '(' ')' Block

    private BlockNode blockNode;

    public MainFuncDefNode(BlockNode blockNode) {
        this.blockNode = blockNode;
    }

    public void print() {
        IOUtils.write(Token.constTokens.get(TokenType.INTTK).toString());
        IOUtils.write(Token.constTokens.get(TokenType.MAINTK).toString());
        IOUtils.write(Token.constTokens.get(TokenType.LPARENT).toString());
        IOUtils.write(Token.constTokens.get(TokenType.RPARENT).toString());
        blockNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.MainFuncDef));
    }

}
