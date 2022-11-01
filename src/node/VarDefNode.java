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

public class VarDefNode {
    // VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    private Token ident;
    private List<Token> leftBrackets;
    private List<ConstExpNode> constExpNodes;
    private List<Token> rightBrackets;
    private Token assign;
    private InitValNode initValNode;

    public VarDefNode(Token ident, List<Token> leftBrackets, List<ConstExpNode> constExpNodes, List<Token> rightBrackets, Token assign, InitValNode initValNode) {
        this.ident = ident;
        this.leftBrackets = leftBrackets;
        this.constExpNodes = constExpNodes;
        this.rightBrackets = rightBrackets;
        this.assign = assign;
        this.initValNode = initValNode;
    }

    public Token getIdent() {
        return ident;
    }

    public List<ConstExpNode> getConstExpNodes() {
        return constExpNodes;
    }

    public InitValNode getInitValNode() {
        return initValNode;
    }

    public void print() {
        IOUtils.write(ident.toString());
        for (int i = 0; i < leftBrackets.size(); i++) {
            IOUtils.write(leftBrackets.get(i).toString());
            constExpNodes.get(i).print();
            IOUtils.write(rightBrackets.get(i).toString());
        }
        if (initValNode != null) {
            IOUtils.write(assign.toString());
            initValNode.print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.VarDef));
    }
}
