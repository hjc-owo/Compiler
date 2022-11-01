package node;

import frontend.Parser;
import symbol.FuncParam;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

public class AddExpNode {
    // AddExp -> MulExp | AddExp ('+' | '−') MulExp
    private MulExpNode mulExpNode;
    private Token operator;
    private AddExpNode addExpNode;

    public AddExpNode(MulExpNode mulExpNode, Token operator, AddExpNode addExpNode) {
        this.mulExpNode = mulExpNode;
        this.operator = operator;
        this.addExpNode = addExpNode;
    }

    public MulExpNode getMulExpNode() {
        return mulExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    public AddExpNode getAddExpNode() {
        return addExpNode;
    }

    public void print() {
        mulExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.AddExp));
        if (operator != null) {
            IOUtils.write(operator.toString());
            addExpNode.print();
        }
    }

    public FuncParam getFuncParam() {
        return mulExpNode.getFuncParam();
    }

    public String getStr() {
        return mulExpNode.getStr() + (operator == null ? "" : operator.getContent() + addExpNode.getStr());
    }
}
