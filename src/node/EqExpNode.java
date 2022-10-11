package node;

import frontend.Parser;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class EqExpNode {
    // RelExp | EqExp ('==' | '!=') RelExp
    private List<RelExpNode> relExpNodes;
    private List<Token> operations;

    public EqExpNode(List<RelExpNode> relExpNodes, List<Token> operations) {
        this.relExpNodes = relExpNodes;
        this.operations = operations;
    }

    public void print() {
        for (int i = 0; i < relExpNodes.size(); i++) {
            relExpNodes.get(i).print();
            IOUtils.write(Parser.nodeType.get(NodeType.EqExp));
            if (i < operations.size()) {
                IOUtils.write(operations.get(i).toString());
            }
        }
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        for (RelExpNode relExpNode : relExpNodes) {
            relExpNode.fillSymbolTable(currentSymbolTable);
        }
    }
}
