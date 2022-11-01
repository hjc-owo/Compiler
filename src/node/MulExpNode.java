package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

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

    public String getStr() {
        return unaryExpNode.getStr() + (operator == null ? "" : operator.getContent() + mulExpNode.getStr());
    }
}
