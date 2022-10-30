package node;

import frontend.Parser;
import symbol.FuncParam;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

public class PrimaryExpNode {
    // PrimaryExp -> '(' Exp ')' | LVal | Number

    private Token leftParentToken = null;
    private ExpNode expNode = null;
    private Token rightParentToken = null;
    private LValNode lValNode = null;
    private NumberNode numberNode = null;

    public PrimaryExpNode(Token leftParentToken, ExpNode expNode, Token rightParentToken) {
        this.leftParentToken = leftParentToken;
        this.expNode = expNode;
        this.rightParentToken = rightParentToken;
    }

    public PrimaryExpNode(LValNode lValNode) {
        this.lValNode = lValNode;
    }

    public PrimaryExpNode(NumberNode numberNode) {
        this.numberNode = numberNode;
    }

    public Token getLeftParentToken() {
        return leftParentToken;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public Token getRightParentToken() {
        return rightParentToken;
    }

    public LValNode getLValNode() {
        return lValNode;
    }

    public NumberNode getNumberNode() {
        return numberNode;
    }

    public void print() {
        if (expNode != null) {
            IOUtils.write(leftParentToken.toString());
            expNode.print();
            IOUtils.write(rightParentToken.toString());
        } else if (lValNode != null) {
            lValNode.print();
        } else {
            numberNode.print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.PrimaryExp));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        if (expNode != null) {
            expNode.fillSymbolTable(currentSymbolTable);
        } else if (lValNode != null) {
            lValNode.fillSymbolTable(currentSymbolTable);
        }
    }

    public FuncParam getFuncParam() {
        if (expNode != null) {
            return expNode.getFuncParam();
        } else if (lValNode != null) {
            return lValNode.getFuncParam();
        } else {
            return new FuncParam(null, 0);
        }
    }

    public String getStr() {
        if (expNode != null) {
            return "(" + expNode.getStr() + ")";
        } else if (lValNode != null) {
            return lValNode.getStr();
        } else {
            return numberNode.getStr();
        }
    }
}
