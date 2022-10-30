package node;

import frontend.Parser;
import symbol.FuncType;
import token.Token;
import utils.IOUtils;

public class FuncTypeNode {
    // FuncType -> 'void' | 'int'

    private Token token;

    public FuncTypeNode(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public void print() {
        IOUtils.write(token.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.FuncType));
    }

    public FuncType getType() {
        if (token.getContent().equals("void")) {
            return FuncType.VOID;
        } else {
            return FuncType.INT;
        }
    }
}
