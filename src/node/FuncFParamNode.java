package node;

import error.Error;
import error.ErrorHandler;
import error.ErrorType;
import frontend.Parser;
import symbol.ArraySymbol;
import symbol.FuncParam;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

public class FuncFParamNode {
    // FuncFParam -> BType Ident [ '[' ']' { '[' ConstExp ']' }]

    private BTypeNode bTypeNode;
    private Token ident;
    private List<Token> leftBrackets;
    private List<Token> rightBrackets;
    private List<ConstExpNode> constExpNodes;

    public FuncFParamNode(BTypeNode bTypeNode, Token ident, List<Token> leftBrackets, List<Token> rightBrackets, List<ConstExpNode> constExpNodes) {
        this.bTypeNode = bTypeNode;
        this.ident = ident;
        this.leftBrackets = leftBrackets;
        this.rightBrackets = rightBrackets;
        this.constExpNodes = constExpNodes;
    }

    public void print() {
        bTypeNode.print();
        IOUtils.write(ident.toString());
        if (leftBrackets.size() > 0) {
            IOUtils.write(leftBrackets.get(0).toString());
            IOUtils.write(rightBrackets.get(0).toString());
            for (int i = 1; i < leftBrackets.size(); i++) {
                IOUtils.write(leftBrackets.get(i).toString());
                constExpNodes.get(i - 1).print();
                IOUtils.write(rightBrackets.get(i).toString());
            }
        }
        IOUtils.write(Parser.nodeType.get(NodeType.FuncFParam));
    }

    public FuncParam getParam() {
        return new FuncParam(ident.getContent(), leftBrackets.size());
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        if (currentSymbolTable.containsInCurrent(ident.getContent())) {
            ErrorHandler.addError(new Error(ident.getLineNumber(), ErrorType.b));
        }
        int dimension = leftBrackets.size();
        if (dimension == 0) {
            currentSymbolTable.put(ident.getContent(), new ArraySymbol(ident.getContent(), false, 0, new ArrayList<>()));
        } else {
            List<Integer> dimLengths = new ArrayList<>();
            for (ConstExpNode constExpNode : constExpNodes) {
                dimLengths.add(constExpNode.getValue());
            }
            currentSymbolTable.put(ident.getContent(), new ArraySymbol(ident.getContent(), false, dimension, dimLengths));
        }
    }
}
