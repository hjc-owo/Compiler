package node;

import error.Error;
import error.ErrorHandler;
import error.ErrorType;
import frontend.Parser;
import symbol.*;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UnaryExpNode {
    // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private PrimaryExpNode primaryExpNode = null;
    private Token ident = null;
    private Token leftParentToken = null;
    private FuncRParamsNode funcRParamsNode = null;
    private Token rightParentToken = null;
    private UnaryOpNode unaryOpNode = null;
    private UnaryExpNode unaryExpNode = null;

    public UnaryExpNode(PrimaryExpNode primaryExpNode) {
        this.primaryExpNode = primaryExpNode;
    }

    public UnaryExpNode(Token ident, Token leftParentToken, FuncRParamsNode funcRParamsNode, Token rightParentToken) {
        this.ident = ident;
        this.leftParentToken = leftParentToken;
        this.funcRParamsNode = funcRParamsNode;
        this.rightParentToken = rightParentToken;
    }

    public UnaryExpNode(UnaryOpNode unaryOpNode, UnaryExpNode unaryExpNode) {
        this.unaryOpNode = unaryOpNode;
        this.unaryExpNode = unaryExpNode;
    }

    public int getValue() {
        if (primaryExpNode != null) {
            return primaryExpNode.getValue();
        } else if (ident != null) {
            // TODO: function call
            return 0;
        } else {
            if (unaryOpNode.getToken().getType() == TokenType.PLUS) {
                return unaryExpNode.getValue();
            } else if (unaryOpNode.getToken().getType() == TokenType.MINU) {
                return -unaryExpNode.getValue();
            } else {
                return ~unaryExpNode.getValue();
            }
        }
    }

    public void print() {
        if (primaryExpNode != null) {
            primaryExpNode.print();
        } else if (ident != null) {
            IOUtils.write(ident.toString());
            IOUtils.write(leftParentToken.toString());
            if (funcRParamsNode != null) {
                funcRParamsNode.print();
            }
            IOUtils.write(rightParentToken.toString());
        } else {
            unaryOpNode.print();
            unaryExpNode.print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.UnaryExp));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        if (primaryExpNode != null) {
            primaryExpNode.fillSymbolTable(currentSymbolTable);
        } else if (ident != null) {
            if (!currentSymbolTable.contains(ident.getContent())) {
                ErrorHandler.addError(new Error(ident.getLineNumber(), ErrorType.c));
            }
            Symbol symbol = currentSymbolTable.get(ident.getContent());
            if (symbol instanceof FuncSymbol) {
                FuncSymbol funcSymbol = (FuncSymbol) symbol;
                if (funcRParamsNode == null) {
                    if (funcSymbol.getFuncParams().size() != 0) {
                        ErrorHandler.addError(new Error(ident.getLineNumber(), ErrorType.d));
                    }
                } else {
                    if (funcSymbol.getFuncParams().size() != funcRParamsNode.getExpNodes().size()) {
                        ErrorHandler.addError(new Error(ident.getLineNumber(), ErrorType.d));
                    }
                    List<Integer> funcFParamDimensions = new ArrayList<>();
                    for (FuncParam funcParam : funcSymbol.getFuncParams()) {
                        funcFParamDimensions.add(funcParam.getDimension());
                    }
                    List<Integer> funcRParamDimensions = new ArrayList<>();
                    if (funcRParamsNode != null) {
                        for (ExpNode expNode : funcRParamsNode.getExpNodes()) {
                            FuncParam funcRParam = expNode.getFuncParam();
                            if (funcRParam != null) {
                                if (funcRParam.getName() != null) {
                                    Symbol symbol2 = currentSymbolTable.get(funcRParam.getName());
                                    if (symbol2 instanceof ArraySymbol) {
                                        funcRParamDimensions.add(((ArraySymbol) symbol2).getDimension() - funcRParam.getDimension());
                                    } else if (symbol2 instanceof FuncSymbol) {
                                        funcRParamDimensions.add(((FuncSymbol) symbol2).getType() == FuncType.VOID ? -1 : 0);
                                    }
                                } else {
                                    funcRParamDimensions.add(funcRParam.getDimension());
                                }
                            }
                        }
                    }
                    if (!Objects.equals(funcFParamDimensions, funcRParamDimensions)) {
                        ErrorHandler.addError(new Error(ident.getLineNumber(), ErrorType.e));
                    }
                }
            } else {
                ErrorHandler.addError(new Error(ident.getLineNumber(), ErrorType.c));
            }
        } else {
            unaryExpNode.fillSymbolTable(currentSymbolTable);
        }
    }

    public FuncParam getFuncParam() {
        if (primaryExpNode != null) {
            return primaryExpNode.getFuncParam();
        } else if (ident != null) {
            return null;
        } else {
            return unaryExpNode.getFuncParam();
        }
    }
}
