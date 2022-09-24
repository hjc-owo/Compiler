package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class ConstInitValNode {
    // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private ConstExpNode constExpNode;
    private List<ConstInitValNode> constInitValNodes;

    public ConstInitValNode(ConstExpNode constExpNode, List<ConstInitValNode> constInitValNodes) {
        this.constExpNode = constExpNode;
        this.constInitValNodes = constInitValNodes;
    }

    public void print() {
        if (constExpNode != null) {
            constExpNode.print();
        } else {
            IOUtils.write(Token.constTokens.get(TokenType.LBRACE).toString());
            for (int i = 0; i < constInitValNodes.size(); i++) {
                constInitValNodes.get(i).print();
                if (i < constInitValNodes.size() - 1) {
                    IOUtils.write(Token.constTokens.get(TokenType.COMMA).toString());
                }
            }
            IOUtils.write(Token.constTokens.get(TokenType.RBRACE).toString());
        }
        IOUtils.write(Parser.nodeType.get(NodeType.ConstInitVal));
    }
}
