package node;

import frontend.Parser;
import symbol.FuncParam;
import symbol.SymbolTable;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class MulExpNode {
    // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp

    private List<UnaryExpNode> unaryExpNodes;
    private List<Token> operations;

    public MulExpNode(List<UnaryExpNode> unaryExpNodes, List<Token> operations) {
        this.unaryExpNodes = unaryExpNodes;
        this.operations = operations;
    }

    public int getValue() {
        int value = unaryExpNodes.get(0).getValue();
        for (int i = 1; i < unaryExpNodes.size(); i++) {
            if (operations.get(i - 1).getType() == TokenType.MULT) {
                value *= unaryExpNodes.get(i).getValue();
            } else if (operations.get(i - 1).getType() == TokenType.DIV) {
                value /= unaryExpNodes.get(i).getValue();
            } else {
                value %= unaryExpNodes.get(i).getValue();
            }
        }
        return value;
    }

    public void print() {
        for (int i = 0; i < unaryExpNodes.size(); i++) {
            unaryExpNodes.get(i).print();
            IOUtils.write(Parser.nodeType.get(NodeType.MulExp));
            if (i < operations.size()) {
                IOUtils.write(operations.get(i).toString());
            }
        }
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        for (UnaryExpNode unaryExpNode : unaryExpNodes) {
            unaryExpNode.fillSymbolTable(currentSymbolTable);
        }
    }

    public FuncParam getFuncParam() {
        return unaryExpNodes.get(0).getFuncParam();
    }
}
