package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class LOrExpNode {
    // LOrExp -> LAndExp | LOrExp '||' LAndExp

    private List<LAndExpNode> lAndExpNodes;

    public LOrExpNode(List<LAndExpNode> lAndExpNodes) {
        this.lAndExpNodes = lAndExpNodes;
    }

    public void print() {
        for (int i = 0; i < lAndExpNodes.size(); i++) {
            lAndExpNodes.get(i).print();
            IOUtils.write(Parser.nodeType.get(NodeType.LOrExp));
            if (i < lAndExpNodes.size() - 1) {
                IOUtils.write(Token.constTokens.get(TokenType.OR).toString());
            }
        }
    }
}
