package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class BlockNode {
    // Block -> '{' { BlockItem } '}'

    private Token leftBraceToken;
    private List<BlockItemNode> blockItemNodes;
    private Token rightBraceToken;

    public BlockNode(Token leftBraceToken, List<BlockItemNode> blockItemNodes, Token rightBraceToken) {
        this.leftBraceToken = leftBraceToken;
        this.blockItemNodes = blockItemNodes;
        this.rightBraceToken = rightBraceToken;
    }

    public void print() {
        IOUtils.write(leftBraceToken.toString());
        for (BlockItemNode blockItemNode : blockItemNodes) {
            blockItemNode.print();
        }
        IOUtils.write(rightBraceToken.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.Block));
    }
}
