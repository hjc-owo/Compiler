package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class VarDefNode {
    // VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    private Token ident;
    private List<ConstExpNode> constExpNodes;
    private InitValNode initValNode;

    public VarDefNode(Token ident, List<ConstExpNode> constExpNodes, InitValNode initValNode) {
        this.ident = ident;
        this.constExpNodes = constExpNodes;
        this.initValNode = initValNode;
    }

    public void print() {
        IOUtils.write(ident.toString());
        for (ConstExpNode constExpNode : constExpNodes) {
            IOUtils.write(Token.constTokens.get(TokenType.LBRACK).toString());
            constExpNode.print();
            IOUtils.write(Token.constTokens.get(TokenType.RBRACK).toString());
        }
        if (initValNode != null) {
            IOUtils.write(Token.constTokens.get(TokenType.ASSIGN).toString());
            initValNode.print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.VarDef));
    }
}
