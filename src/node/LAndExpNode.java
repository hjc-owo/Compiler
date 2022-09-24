package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class LAndExpNode {
    // LAndExp -> EqExp | LAndExp '&&' EqExp
    private List<EqExpNode> eqExpNodes;

    public LAndExpNode(List<EqExpNode> eqExpNodes) {
        this.eqExpNodes = eqExpNodes;
    }

    public void print() {
        for (int i = 0; i < eqExpNodes.size(); i++) {
            eqExpNodes.get(i).print();
            IOUtils.write(Parser.nodeType.get(NodeType.LAndExp));
            if (i < eqExpNodes.size() - 1) {
                IOUtils.write(Token.constTokens.get(TokenType.AND).toString());
            }
        }
    }
}
