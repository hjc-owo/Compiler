package node;

import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class FuncFParamsNode {
    // FuncFParams -> FuncFParam { ',' FuncFParam }

    private List<FuncFParamNode> funcFParamNodes;
    private List<Token> commas;

    public FuncFParamsNode(List<FuncFParamNode> funcFParamNodes, List<Token> commas) {
        this.funcFParamNodes = funcFParamNodes;
        this.commas = commas;
    }

    public void print() {
        funcFParamNodes.get(0).print();
        for (int i = 1; i < funcFParamNodes.size(); i++) {
            IOUtils.write(commas.get(i - 1).toString());
            funcFParamNodes.get(i).print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.FuncFParams));
    }
}
