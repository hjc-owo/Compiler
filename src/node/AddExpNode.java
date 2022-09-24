package node;

import frontend.Parser;
import token.Token;
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

    public void print() {
        for (int i = 0; i < mulExpNodes.size(); i++) {
            mulExpNodes.get(i).print();
            IOUtils.write(Parser.nodeType.get(NodeType.AddExp));
            if (i < operations.size()) {
                IOUtils.write(operations.get(i).toString());
            }
        }
    }
}
