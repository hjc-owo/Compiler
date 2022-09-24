package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

public class UnaryOpNode {
    // UnaryOp -> '+' | '−' | '!'

    Token token;

    public UnaryOpNode(Token token) {
        this.token = token;
    }

    public void print() {
        IOUtils.write(token.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.UnaryOp));
    }
}
