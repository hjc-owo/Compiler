package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

public class UnaryExpNode {
    // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private PrimaryExpNode primaryExpNode;
    private Token ident;
    private FuncRParamsNode funcRParamsNode;
    private UnaryOpNode unaryOpNode;
    private UnaryExpNode unaryExpNode;

    public UnaryExpNode(PrimaryExpNode primaryExpNode, Token ident, FuncRParamsNode funcRParamsNode, UnaryOpNode unaryOpNode, UnaryExpNode unaryExp) {
        this.primaryExpNode = primaryExpNode;
        this.ident = ident;
        this.funcRParamsNode = funcRParamsNode;
        this.unaryOpNode = unaryOpNode;
        this.unaryExpNode = unaryExp;
    }

    public void print() {
        if (primaryExpNode != null) {
            primaryExpNode.print();
        } else if (ident != null) {
            IOUtils.write(ident.toString());
            IOUtils.write(Token.constTokens.get(TokenType.LPARENT).toString());
            if (funcRParamsNode != null) {
                funcRParamsNode.print();
            }
            IOUtils.write(Token.constTokens.get(TokenType.RPARENT).toString());
        } else {
            unaryOpNode.print();
            unaryExpNode.print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.UnaryExp));
    }
}
