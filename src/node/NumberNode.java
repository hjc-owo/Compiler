package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

public class NumberNode {
    // Number -> IntConst

    Token token;

    public NumberNode(Token token) {
        this.token = token;
    }

    public void print() {
        IOUtils.write(token.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.Number));
    }
}
