package node;

import error.Error;
import error.ErrorHandler;
import error.ErrorType;
import frontend.Parser;
import symbol.ArraySymbol;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

public class ConstDefNode {
    // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    private Token ident;
    private List<Token> leftBrackets;
    private List<ConstExpNode> constExpNodes;
    private List<Token> rightBrackets;
    private Token equalToken;
    private ConstInitValNode constInitValNode;

    public ConstDefNode(Token ident, List<Token> leftBrackets, List<ConstExpNode> constExpNodes, List<Token> rightBrackets, Token equalToken, ConstInitValNode constInitValNode) {
        this.ident = ident;
        this.leftBrackets = leftBrackets;
        this.constExpNodes = constExpNodes;
        this.rightBrackets = rightBrackets;
        this.equalToken = equalToken;
        this.constInitValNode = constInitValNode;
    }

    public Token getIdent() {
        return ident;
    }

    public List<Token> getLeftBrackets() {
        return leftBrackets;
    }

    public List<ConstExpNode> getConstExpNodes() {
        return constExpNodes;
    }

    public List<Token> getRightBrackets() {
        return rightBrackets;
    }

    public Token getEqualToken() {
        return equalToken;
    }

    public ConstInitValNode getConstInitValNode() {
        return constInitValNode;
    }

    public void print() {
        IOUtils.write(ident.toString());
        for (int i = 0; i < constExpNodes.size(); i++) {
            IOUtils.write(leftBrackets.get(i).toString());
            constExpNodes.get(i).print();
            IOUtils.write(rightBrackets.get(i).toString());
        }
        IOUtils.write(equalToken.toString());
        constInitValNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.ConstDef));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        if (currentSymbolTable.containsInCurrent(ident.getContent())) {
            ErrorHandler.addError(new Error(ident.getLineNumber(), ErrorType.b));
        }
        int dimension = constExpNodes.size();
        if (dimension == 0) {
            currentSymbolTable.put(ident.getContent(), new ArraySymbol(ident.getContent(), true, 0, new ArrayList<>()));
        } else {
            List<Integer> dimLengths = getDimLengths(currentSymbolTable);
            currentSymbolTable.put(ident.getContent(), new ArraySymbol(ident.getContent(), true, dimension, dimLengths));
        }
        constInitValNode.fillSymbolTable(currentSymbolTable);
    }

    private List<Integer> getDimLengths(SymbolTable currentSymbolTable) {
        List<Integer> dimLengths = new ArrayList<>();
        for (ConstExpNode constExpNode : constExpNodes) {
            dimLengths.add(1);
            constExpNode.fillSymbolTable(currentSymbolTable);
        }
        return dimLengths;
    }
}
