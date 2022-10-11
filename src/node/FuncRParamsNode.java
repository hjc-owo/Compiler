package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class FuncRParamsNode {
    // FuncRParams -> Exp { ',' Exp }

    private List<ExpNode> expNodes;
    private List<Token> commas;

    public FuncRParamsNode(List<ExpNode> expNodes, List<Token> commas) {
        this.expNodes = expNodes;
        this.commas = commas;
    }

    public List<ExpNode> getExpNodes() {
        return expNodes;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public void print() {
        expNodes.get(0).print();
        for (int i = 1; i < expNodes.size(); i++) {
            IOUtils.write(commas.get(i - 1).toString());
            expNodes.get(i).print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.FuncRParams));
    }
}
