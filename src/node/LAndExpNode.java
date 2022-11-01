package node;

import frontend.Parser;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class LAndExpNode {
    // LAndExp -> EqExp | LAndExp '&&' EqExp
    private EqExpNode eqExpNode;
    private Token andToken;
    private LAndExpNode lAndExpNode;

    public LAndExpNode(EqExpNode eqExpNode, Token operator, LAndExpNode lAndExpNode) {
        this.eqExpNode = eqExpNode;
        this.andToken = operator;
        this.lAndExpNode = lAndExpNode;
    }

    public EqExpNode getEqExpNode() {
        return eqExpNode;
    }

    public Token getAndToken() {
        return andToken;
    }

    public LAndExpNode getLAndExpNode() {
        return lAndExpNode;
    }

    public void print() {
        eqExpNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.LAndExp));
        if (andToken != null) {
            IOUtils.write(andToken.toString());
            lAndExpNode.print();
        }
    }
}
