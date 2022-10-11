package node;

import error.Error;
import error.ErrorHandler;
import error.ErrorType;
import frontend.Parser;
import symbol.FuncSymbol;
import symbol.FuncSymbolTable;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

import java.util.ArrayList;

public class FuncDefNode {
    // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block

    private FuncTypeNode funcTypeNode;
    private Token ident;
    private Token leftParentToken;
    private FuncFParamsNode funcFParamsNode;
    private Token rightParentToken;
    private BlockNode blockNode;

    public FuncDefNode(FuncTypeNode funcTypeNode, Token ident, Token leftParentToken, FuncFParamsNode funcFParamsNode, Token rightParentToken, BlockNode blockNode) {
        this.funcTypeNode = funcTypeNode;
        this.ident = ident;
        this.leftParentToken = leftParentToken;
        this.funcFParamsNode = funcFParamsNode;
        this.rightParentToken = rightParentToken;
        this.blockNode = blockNode;
    }

    public void print() {
        funcTypeNode.print();
        IOUtils.write(ident.toString());
        IOUtils.write(leftParentToken.toString());
        if (funcFParamsNode != null) {
            funcFParamsNode.print();
        }
        IOUtils.write(rightParentToken.toString());
        blockNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.FuncDef));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        if (currentSymbolTable.containsInCurrent(ident.getContent())) {
            ErrorHandler.addError(new Error(ident.getLineNumber(), ErrorType.b));
        }
        if (funcFParamsNode == null) {
            currentSymbolTable.put(ident.getContent(), new FuncSymbol(ident.getContent(), funcTypeNode.getType(), new ArrayList<>()));
        } else {
            currentSymbolTable.put(ident.getContent(), new FuncSymbol(ident.getContent(), funcTypeNode.getType(), funcFParamsNode.getParams()));
        }
        currentSymbolTable = new FuncSymbolTable(currentSymbolTable, funcTypeNode.getType());
        if (funcFParamsNode != null) {
            funcFParamsNode.fillSymbolTable(currentSymbolTable);
        }
        blockNode.fillSymbolTable(currentSymbolTable);
    }
}
