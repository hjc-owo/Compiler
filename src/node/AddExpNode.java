package node;

import frontend.Parser;
import symbol.FuncRParam;
import symbol.SymbolTable;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class AddExpNode {
    // AddExp -> MulExp | AddExp ('+' | '−') MulExp
    private List<MulExpNode> mulExpNodes;
    private List<Token> operations;

    public AddExpNode(List<MulExpNode> mulExpNodes, List<Token> operations) {
        this.mulExpNodes = mulExpNodes;
        this.operations = operations;
    }

    public int getValue() {
        int value = mulExpNodes.get(0).getValue();
        for (int i = 1; i < mulExpNodes.size(); i++) {
            if (operations.get(i - 1).getType() == TokenType.PLUS) {
                value += mulExpNodes.get(i).getValue();
            } else {
                value -= mulExpNodes.get(i).getValue();
            }
        }
        return value;
    }

    public void print() {
        for (int i = 0; i < mulExpNodes.size(); i++) {
            mulExpNodes.get(i).print();
            IOUtils.write(Parser.nodeType.get(NodeType.AddExp));
            if (i < operations.size()) {
                IOUtils.write(operations.get(i).toString());
            }
        }
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        for (MulExpNode mulExpNode : mulExpNodes) {
            mulExpNode.fillSymbolTable(currentSymbolTable);
        }
    }

    public FuncRParam getFuncRParam() {
        return mulExpNodes.get(0).getFuncRParam();
    }
}
