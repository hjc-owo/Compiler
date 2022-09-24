package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

public class FuncTypeNode {
    // FuncType -> 'void' | 'int'

    private Token token;

    public FuncTypeNode(Token token) {
        this.token = token;
    }

    public void print() {
        IOUtils.write(token.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.FuncType));
    }
}
