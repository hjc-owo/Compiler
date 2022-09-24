package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class MulExpNode {
    // MulExp -> UnaryExp | MulExp ('\*' | '/' | '%') UnaryExp

    private List<UnaryExpNode> unaryExpNodes;
    private List<Token> operations;

    public MulExpNode(List<UnaryExpNode> unaryExpNodes, List<Token> operations) {
        this.unaryExpNodes = unaryExpNodes;
        this.operations = operations;
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
}
