package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class InitValNode {
    // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private ExpNode expNode;
    private List<InitValNode> initValNodes;

    public InitValNode(ExpNode expNode, List<InitValNode> initValNodes) {
        this.expNode = expNode;
        this.initValNodes = initValNodes;
    }

    public void print() {
        if (expNode != null) {
            expNode.print();
        } else {
            IOUtils.write(Token.constTokens.get(TokenType.LBRACE).toString());
            for (int i = 0; i < initValNodes.size(); i++) {
                initValNodes.get(i).print();
                if (i < initValNodes.size() - 1) {
                    IOUtils.write(Token.constTokens.get(TokenType.COMMA).toString());
                }
            }
            IOUtils.write(Token.constTokens.get(TokenType.RBRACE).toString());
        }
        IOUtils.write(Parser.nodeType.get(NodeType.InitVal));
    }
}
