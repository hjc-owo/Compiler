package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class LAndExpNode {
    // LAndExp -> EqExp | LAndExp '&&' EqExp
    private List<EqExpNode> eqExpNodes;
    private List<Token> andTokens;

    public LAndExpNode(List<EqExpNode> eqExpNodes, List<Token> andTokens) {
        this.eqExpNodes = eqExpNodes;
        this.andTokens = andTokens;
    }

    public void print() {
        for (int i = 0; i < eqExpNodes.size(); i++) {
            eqExpNodes.get(i).print();
            IOUtils.write(Parser.nodeType.get(NodeType.LAndExp));
            if (i < andTokens.size()) {
                IOUtils.write(andTokens.get(i).toString());
            }
        }
    }
}
