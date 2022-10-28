package node;

import frontend.Parser;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class ConstInitValNode {
    // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private ConstExpNode constExpNode;
    private Token leftBraceToken;
    private List<ConstInitValNode> constInitValNodes;
    private List<Token> commas;
    private Token rightBraceToken;

    public ConstInitValNode(ConstExpNode constExpNode, Token leftBraceToken, List<ConstInitValNode> constInitValNodes, List<Token> commas, Token rightBraceToken) {
        this.constExpNode = constExpNode;
        this.leftBraceToken = leftBraceToken;
        this.constInitValNodes = constInitValNodes;
        this.commas = commas;
        this.rightBraceToken = rightBraceToken;
    }

    public ConstExpNode getConstExpNode() {
        return constExpNode;
    }

    public Token getLeftBraceToken() {
        return leftBraceToken;
    }

    public List<ConstInitValNode> getConstInitValNodes() {
        return constInitValNodes;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public Token getRightBraceToken() {
        return rightBraceToken;
    }

    public void print() {
        if (constExpNode != null) {
            constExpNode.print();
        } else {
            IOUtils.write(leftBraceToken.toString());
            if (constInitValNodes.size() > 0) {
                constInitValNodes.get(0).print();
                for (int i = 1; i < constInitValNodes.size(); i++) {
                    IOUtils.write(commas.get(i - 1).toString());
                    constInitValNodes.get(i).print();
                }
            }
            IOUtils.write(rightBraceToken.toString());
        }
        IOUtils.write(Parser.nodeType.get(NodeType.ConstInitVal));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        if (constExpNode != null) {
            constExpNode.fillSymbolTable(currentSymbolTable);
        } else {
            for (ConstInitValNode constInitValNode : constInitValNodes) {
                constInitValNode.fillSymbolTable(currentSymbolTable);
            }
        }
    }
}
