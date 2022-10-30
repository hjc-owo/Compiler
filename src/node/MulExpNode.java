package node;

import frontend.Parser;
import symbol.FuncParam;
import symbol.SymbolTable;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class MulExpNode {
    // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp

    private UnaryExpNode unaryExpNode;
    private Token operator;
    private MulExpNode mulExpNode;

    public MulExpNode(UnaryExpNode unaryExpNode, Token operator, MulExpNode mulExpNode) {
        this.unaryExpNode = unaryExpNode;
        this.operator = operator;
        this.mulExpNode = mulExpNode;
    }

    public UnaryExpNode getUnaryExpNode() {
        return unaryExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public void print() {
        unaryExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.MulExp));
        if (operator != null) {
            IOUtils.write(operator.toString());
            mulExpNode.print();
        }
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        unaryExpNode.fillSymbolTable(currentSymbolTable);
        if (mulExpNode != null) {
            mulExpNode.fillSymbolTable(currentSymbolTable);
        }
    }

    public FuncParam getFuncParam() {
        return unaryExpNode.getFuncParam();
    }

    public String getStr() {
        return unaryExpNode.getStr() + (operator == null ? "" : operator.getContent() + mulExpNode.getStr());
    }
}
