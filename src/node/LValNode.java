package node;

import error.Error;
import error.ErrorHandler;
import error.ErrorType;
import frontend.Parser;
import symbol.FuncRParam;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class LValNode {
    // LVal -> Ident {'[' Exp ']'}
    private Token ident;
    private List<Token> leftBrackets;
    private List<ExpNode> expNodes;
    private List<Token> rightBrackets;

    public LValNode(Token ident, List<Token> leftBrackets, List<ExpNode> expNodes, List<Token> rightBrackets) {
        this.ident = ident;
        this.leftBrackets = leftBrackets;
        this.expNodes = expNodes;
        this.rightBrackets = rightBrackets;
    }

    public Token getIdent() {
        return ident;
    }

    public int getValue() {
        return 0;
        // todo
    }

    public void print() {
        IOUtils.write(ident.toString());
        for (int i = 0; i < leftBrackets.size(); i++) {
            IOUtils.write(leftBrackets.get(i).toString());
            expNodes.get(i).print();
            IOUtils.write(rightBrackets.get(i).toString());
        }
        IOUtils.write(Parser.nodeType.get(NodeType.LVal));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        if (!currentSymbolTable.contains(ident.getContent())) {
            ErrorHandler.addError(new Error(ident.getLineNumber(), ErrorType.c));
        }
        for (ExpNode expNode : expNodes) {
            expNode.fillSymbolTable(currentSymbolTable);
        }
    }

    public FuncRParam getFuncRParam() {
        return new FuncRParam(ident.getContent(), expNodes.size());
    }
}
