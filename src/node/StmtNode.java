package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class StmtNode {
    // Stmt -> LVal '=' Exp ';'
    //	| [Exp] ';'
    //	| Block
    //	| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    //	| 'while' '(' Cond ')' Stmt
    //	| 'break' ';' | 'continue' ';'
    //	| 'return' [Exp] ';'
    //	| LVal '=' 'getint' '(' ')' ';'
    //	| 'printf' '(' FormatString { ',' Exp } ')' ';'
    private Parser.StmtType type;
    private LValNode lValNode;
    private ExpNode expNode;
    private BlockNode blockNode;
    private CondNode condNode;
    private List<StmtNode> stmtNodes;
    private Token formatString;
    private List<ExpNode> expNodes;

    public StmtNode(Parser.StmtType type, LValNode lValNode, ExpNode expNode, BlockNode blockNode, CondNode condNode, List<StmtNode> stmtNodes, Token formatString, List<ExpNode> expNodes) {
        this.type = type;
        this.lValNode = lValNode;
        this.expNode = expNode;
        this.blockNode = blockNode;
        this.condNode = condNode;
        this.stmtNodes = stmtNodes;
        this.formatString = formatString;
        this.expNodes = expNodes;
    }

    public void print() {
        switch (type) {
            case LValAssignExp:
                lValNode.print();
                IOUtils.write(Token.constTokens.get(TokenType.ASSIGN).toString());
                expNode.print();
                IOUtils.write(Token.constTokens.get(TokenType.SEMICN).toString());
                break;
            case Exp:
                if (expNode != null) expNode.print();
                IOUtils.write(Token.constTokens.get(TokenType.SEMICN).toString());
                break;
            case Block:
                blockNode.print();
                break;
            case If:
                IOUtils.write(Token.constTokens.get(TokenType.IFTK).toString());
                IOUtils.write(Token.constTokens.get(TokenType.LPARENT).toString());
                condNode.print();
                IOUtils.write(Token.constTokens.get(TokenType.RPARENT).toString());
                stmtNodes.get(0).print();
                if (stmtNodes.size() == 2) {
                    IOUtils.write(Token.constTokens.get(TokenType.ELSETK).toString());
                    stmtNodes.get(1).print();
                }
                break;
            case While:
                IOUtils.write(Token.constTokens.get(TokenType.WHILETK).toString());
                IOUtils.write(Token.constTokens.get(TokenType.LPARENT).toString());
                condNode.print();
                IOUtils.write(Token.constTokens.get(TokenType.RPARENT).toString());
                stmtNodes.get(0).print();
                break;
            case Break:
                IOUtils.write(Token.constTokens.get(TokenType.BREAKTK).toString());
                IOUtils.write(Token.constTokens.get(TokenType.SEMICN).toString());
                break;
            case Continue:
                IOUtils.write(Token.constTokens.get(TokenType.CONTINUETK).toString());
                IOUtils.write(Token.constTokens.get(TokenType.SEMICN).toString());
                break;
            case Return:
                IOUtils.write(Token.constTokens.get(TokenType.RETURNTK).toString());
                if (expNode != null) {
                    expNode.print();
                }
                IOUtils.write(Token.constTokens.get(TokenType.SEMICN).toString());
                break;
            case LValAssignGetint:
                lValNode.print();
                IOUtils.write(Token.constTokens.get(TokenType.ASSIGN).toString());
                IOUtils.write(Token.constTokens.get(TokenType.GETINTTK).toString());
                IOUtils.write(Token.constTokens.get(TokenType.LPARENT).toString());
                IOUtils.write(Token.constTokens.get(TokenType.RPARENT).toString());
                IOUtils.write(Token.constTokens.get(TokenType.SEMICN).toString());
                break;
            case Printf:
                IOUtils.write(Token.constTokens.get(TokenType.PRINTFTK).toString());
                IOUtils.write(Token.constTokens.get(TokenType.LPARENT).toString());
                IOUtils.write(formatString.toString());
                for (ExpNode expNode : expNodes) {
                    IOUtils.write(Token.constTokens.get(TokenType.COMMA).toString());
                    expNode.print();
                }
                IOUtils.write(Token.constTokens.get(TokenType.RPARENT).toString());
                IOUtils.write(Token.constTokens.get(TokenType.SEMICN).toString());
                break;
        }
        IOUtils.write(Parser.nodeType.get(NodeType.Stmt));
    }
}
