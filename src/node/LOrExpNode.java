package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class LOrExpNode {
    // LOrExp -> LAndExp | LOrExp '||' LAndExp

    private List<LAndExpNode> lAndExpNodes;
    private List<Token> orTokens;

    public LOrExpNode(List<LAndExpNode> lAndExpNodes, List<Token> orTokens) {
        this.lAndExpNodes = lAndExpNodes;
        this.orTokens = orTokens;
    }

    public void print() {
        for (int i = 0; i < lAndExpNodes.size(); i++) {
            lAndExpNodes.get(i).print();
            IOUtils.write(Parser.nodeType.get(NodeType.LOrExp));
            if (i < orTokens.size()) {
                IOUtils.write(orTokens.get(i).toString());
            }
        }
    }
}
