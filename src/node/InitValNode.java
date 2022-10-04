package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class InitValNode {
    // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private ExpNode expNode;
    private Token leftBraceToken;
    private List<InitValNode> initValNodes;
    private List<Token> commas;
    private Token rightBraceToken;

    public InitValNode(ExpNode expNode, Token leftBraceToken, List<InitValNode> initValNodes, List<Token> commas, Token rightBraceToken) {
        this.expNode = expNode;
        this.leftBraceToken = leftBraceToken;
        this.initValNodes = initValNodes;
        this.commas = commas;
        this.rightBraceToken = rightBraceToken;
    }

    public void print() {
        if (expNode != null) {
            expNode.print();
        } else {
            IOUtils.write(leftBraceToken.toString());
            if (initValNodes.size() > 0) {
                for (int i = 0; i < initValNodes.size(); i++) {
                    initValNodes.get(i).print();
                    if (i != initValNodes.size() - 1) {
                        IOUtils.write(commas.get(i).toString());
                    }
                }
            }
            IOUtils.write(rightBraceToken.toString());
        }
        IOUtils.write(Parser.nodeType.get(NodeType.InitVal));
    }
}
