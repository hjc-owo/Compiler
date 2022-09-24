package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class FuncRParamsNode {
    // FuncRParams -> Exp { ',' Exp }

    private List<ExpNode> expNodes;

    public FuncRParamsNode(List<ExpNode> expNodes) {
        this.expNodes = expNodes;
    }

    public void print() {
        for (int i = 0; i < expNodes.size(); i++) {
            expNodes.get(i).print();
            if (i < expNodes.size() - 1) {
                IOUtils.write(Token.constTokens.get(TokenType.COMMA).toString());
            }
        }
        IOUtils.write(Parser.nodeType.get(NodeType.FuncRParams));
    }
}
