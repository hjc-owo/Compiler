package node;

import error.Error;
import error.ErrorHandler;
import error.ErrorType;
import frontend.Parser;
import symbol.FuncSymbolTable;
import symbol.FuncType;
import symbol.SymbolTable;
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

    public List<BlockItemNode> getBlockItemNodes() {
        return blockItemNodes;
    }

    public void print() {
        IOUtils.write(leftBraceToken.toString());
        for (BlockItemNode blockItemNode : blockItemNodes) {
            blockItemNode.print();
        }
        IOUtils.write(rightBraceToken.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.Block));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        for (BlockItemNode blockItemNode : blockItemNodes) {
            blockItemNode.fillSymbolTable(currentSymbolTable);
        }
        if (currentSymbolTable instanceof FuncSymbolTable) {
            FuncSymbolTable funcSymbolTable = (FuncSymbolTable) currentSymbolTable;
            if (blockItemNodes.size() > 0) {
                if (funcSymbolTable.getType() == FuncType.INT) {
                    if (blockItemNodes.get(blockItemNodes.size() - 1).getStmtNode() == null) {
                        ErrorHandler.addError(new Error(rightBraceToken.getLineNumber(), ErrorType.g));
                    } else if (blockItemNodes.get(blockItemNodes.size() - 1).getStmtNode().getReturnToken() == null) {
                        ErrorHandler.addError(new Error(rightBraceToken.getLineNumber(), ErrorType.g));
                    }
                }
            } else {
                if (funcSymbolTable.getType() == FuncType.INT) {
                    ErrorHandler.addError(new Error(rightBraceToken.getLineNumber(), ErrorType.g));
                }
            }

        }
    }
}
