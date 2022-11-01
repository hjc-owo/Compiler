package node;

import error.Error;
import error.ErrorHandler;
import error.ErrorType;
import frontend.Parser;
import symbol.ArraySymbol;
import symbol.FuncSymbolTable;
import symbol.FuncType;
import symbol.SymbolTable;
import token.Token;
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
    private Token assignToken;
    private ExpNode expNode;
    private Token semicnToken;
    private BlockNode blockNode;
    private Token ifToken;
    private Token leftParentToken;
    private CondNode condNode;
    private Token rightParentToken;
    private List<StmtNode> stmtNodes;
    private Token elseToken;
    private Token whileToken;
    private Token breakOrContinueToken;
    private Token returnToken;
    private Token getintToken;
    private Token printfToken;
    private Token formatString;
    private List<Token> commas;
    private List<ExpNode> expNodes;

    public StmtNode(Parser.StmtType type, LValNode lValNode, Token assignToken, ExpNode expNode, Token semicnToken) {
        // LVal '=' Exp ';'
        this.type = type;
        this.lValNode = lValNode;
        this.assignToken = assignToken;
        this.expNode = expNode;
        this.semicnToken = semicnToken;
    }

    public StmtNode(Parser.StmtType type, ExpNode expNode, Token semicnToken) {
        // [Exp] ';'
        this.type = type;
        this.expNode = expNode;
        this.semicnToken = semicnToken;
    }

    public StmtNode(Parser.StmtType type, BlockNode blockNode) {
        // Block
        this.type = type;
        this.blockNode = blockNode;
    }

    public StmtNode(Parser.StmtType type, Token ifToken, Token leftParentToken, CondNode condNode, Token rightParentToken, List<StmtNode> stmtNodes, Token elseToken) {
        // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        this.type = type;
        this.ifToken = ifToken;
        this.leftParentToken = leftParentToken;
        this.condNode = condNode;
        this.rightParentToken = rightParentToken;
        this.stmtNodes = stmtNodes;
        this.elseToken = elseToken;
    }

    public StmtNode(Parser.StmtType type, Token whileToken, Token leftParentToken, CondNode condNode, Token rightParentToken, List<StmtNode> stmtNodes) {
        // 'while' '(' Cond ')' Stmt
        this.type = type;
        this.whileToken = whileToken;
        this.leftParentToken = leftParentToken;
        this.condNode = condNode;
        this.rightParentToken = rightParentToken;
        this.stmtNodes = stmtNodes;
    }

    public StmtNode(Parser.StmtType type, Token breakOrContinueToken, Token semicnToken) {
        // 'break' ';'
        this.type = type;
        this.breakOrContinueToken = breakOrContinueToken;
        this.semicnToken = semicnToken;
    }

    public StmtNode(Parser.StmtType type, Token returnToken, ExpNode expNode, Token semicnToken) {
        // 'return' [Exp] ';'
        this.type = type;
        this.returnToken = returnToken;
        this.expNode = expNode;
        this.semicnToken = semicnToken;
    }

    public StmtNode(Parser.StmtType type, LValNode lValNode, Token assignToken, Token getintToken, Token leftParentToken, Token rightParentToken, Token semicnToken) {
        // LVal '=' 'getint' '(' ')' ';'
        this.type = type;
        this.lValNode = lValNode;
        this.assignToken = assignToken;
        this.getintToken = getintToken;
        this.leftParentToken = leftParentToken;
        this.rightParentToken = rightParentToken;
        this.semicnToken = semicnToken;
    }

    public StmtNode(Parser.StmtType type, Token printfToken, Token leftParentToken, Token formatString, List<Token> commas, List<ExpNode> expNodes, Token rightParentToken, Token semicnToken) {
        // 'printf' '(' FormatString { ',' Exp } ')' ';'
        this.type = type;
        this.printfToken = printfToken;
        this.leftParentToken = leftParentToken;
        this.formatString = formatString;
        this.commas = commas;
        this.expNodes = expNodes;
        this.rightParentToken = rightParentToken;
        this.semicnToken = semicnToken;
    }

    public Parser.StmtType getType() {
        return type;
    }

    public LValNode getLValNode() {
        return lValNode;
    }

    public Token getAssignToken() {
        return assignToken;
    }

    public ExpNode getExpNode() {
        return expNode;
    }

    public Token getSemicnToken() {
        return semicnToken;
    }

    public BlockNode getBlockNode() {
        return blockNode;
    }

    public Token getIfToken() {
        return ifToken;
    }

    public Token getLeftParentToken() {
        return leftParentToken;
    }

    public CondNode getCondNode() {
        return condNode;
    }

    public Token getRightParentToken() {
        return rightParentToken;
    }

    public List<StmtNode> getStmtNodes() {
        return stmtNodes;
    }

    public Token getElseToken() {
        return elseToken;
    }

    public Token getWhileToken() {
        return whileToken;
    }

    public Token getBreakOrContinueToken() {
        return breakOrContinueToken;
    }

    public Token getGetintToken() {
        return getintToken;
    }

    public Token getPrintfToken() {
        return printfToken;
    }

    public Token getFormatString() {
        return formatString;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public List<ExpNode> getExpNodes() {
        return expNodes;
    }

    public Token getReturnToken() {
        return returnToken;
    }

    public void print() {
        switch (type) {
            case LValAssignExp:
                // LVal '=' Exp ';'
                lValNode.print();
                IOUtils.write(assignToken.toString());
                expNode.print();
                IOUtils.write(semicnToken.toString());
                break;
            case Exp:
                // [Exp] ';'
                if (expNode != null) expNode.print();
                IOUtils.write(semicnToken.toString());
                break;
            case Block:
                // Block
                blockNode.print();
                break;
            case If:
                // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                IOUtils.write(ifToken.toString());
                IOUtils.write(leftParentToken.toString());
                condNode.print();
                IOUtils.write(rightParentToken.toString());
                stmtNodes.get(0).print();
                if (elseToken != null) {
                    IOUtils.write(elseToken.toString());
                    stmtNodes.get(1).print();
                }
                break;
            case While:
                // 'while' '(' Cond ')' Stmt
                IOUtils.write(whileToken.toString());
                IOUtils.write(leftParentToken.toString());
                condNode.print();
                IOUtils.write(rightParentToken.toString());
                stmtNodes.get(0).print();
                break;
            case Break:
                // 'break' ';'
            case Continue:
                // 'continue' ';'
                IOUtils.write(breakOrContinueToken.toString());
                IOUtils.write(semicnToken.toString());
                break;
            case Return:
                // 'return' [Exp] ';'
                IOUtils.write(returnToken.toString());
                if (expNode != null) {
                    expNode.print();
                }
                IOUtils.write(semicnToken.toString());
                break;
            case LValAssignGetint:
                // LVal '=' 'getint' '(' ')' ';'
                lValNode.print();
                IOUtils.write(assignToken.toString());
                IOUtils.write(getintToken.toString());
                IOUtils.write(leftParentToken.toString());
                IOUtils.write(rightParentToken.toString());
                IOUtils.write(semicnToken.toString());
                break;
            case Printf:
                // 'printf' '(' FormatString { ',' Exp } ')' ';'
                IOUtils.write(printfToken.toString());
                IOUtils.write(leftParentToken.toString());
                IOUtils.write(formatString.toString());
                for (int i = 0; i < commas.size(); i++) {
                    IOUtils.write(commas.get(i).toString());
                    expNodes.get(i).print();
                }
                IOUtils.write(rightParentToken.toString());
                IOUtils.write(semicnToken.toString());
                break;
        }
        IOUtils.write(Parser.nodeType.get(NodeType.Stmt));
    }
}
