package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class BlockNode {
    // Block -> '{' { BlockItem } '}'

    private List<BlockItemNode> blockItemNodes;

    public BlockNode(List<BlockItemNode> blockItemNodes) {
        this.blockItemNodes = blockItemNodes;
    }

    public void print() {
        IOUtils.write(Token.constTokens.get(TokenType.LBRACE).toString());
        for (BlockItemNode blockItemNode : blockItemNodes) {
            blockItemNode.print();
        }
        IOUtils.write(Token.constTokens.get(TokenType.RBRACE).toString());
        IOUtils.write(Parser.nodeType.get(NodeType.Block));
    }
}
