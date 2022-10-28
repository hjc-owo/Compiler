package node;

import frontend.Parser;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

public class EqExpNode {
    // RelExp | EqExp ('==' | '!=') RelExp
    private RelExpNode relExpNode;
    private Token operator;
    private EqExpNode eqExpNode;

    public EqExpNode(RelExpNode relExpNode, Token operator, EqExpNode eqExpNode) {
        this.relExpNode = relExpNode;
        this.operator = operator;
        this.eqExpNode = eqExpNode;
    }

    public RelExpNode getRelExpNode() {
        return relExpNode;
    }

    public Token getOperator() {
        return operator;
    }

    public EqExpNode getEqExpNode() {
        return eqExpNode;
    }

    public void print() {
        relExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.EqExp));
        if (operator != null) {
            IOUtils.write(operator.toString());
            eqExpNode.print();
        }
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        relExpNode.fillSymbolTable(currentSymbolTable);
        if (eqExpNode != null) {
            eqExpNode.fillSymbolTable(currentSymbolTable);
        }
    }
}
