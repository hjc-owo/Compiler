package node;

import token.Token;
import utils.IOUtils;

public class BTypeNode {
    // BType -> 'int'
    private Token token;

    public BTypeNode(Token token) {
        this.token = token;
    }

    public void print() {
        IOUtils.write(token.toString());
    }
}
