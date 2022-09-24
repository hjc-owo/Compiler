package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class RelExpNode {
    // RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private List<AddExpNode> addExpNodes;
    private List<Token> operations;


    public RelExpNode(List<AddExpNode> addExpNodes, List<Token> operations) {
        this.addExpNodes = addExpNodes;
        this.operations = operations;
    }

    public void print() {
        for (int i = 0; i < addExpNodes.size(); i++) {
            addExpNodes.get(i).print();
            IOUtils.write(Parser.nodeType.get(NodeType.RelExp));
            if (i < operations.size()) {
                IOUtils.write(operations.get(i).toString());
            }
        }
    }
}
