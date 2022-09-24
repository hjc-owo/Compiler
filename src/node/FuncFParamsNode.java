package node;

import frontend.Parser;
import token.Token;
import token.TokenType;
import utils.IOUtils;

import java.util.List;

public class FuncFParamsNode {
    // FuncFParams -> FuncFParam { ',' FuncFParam }

    private List<FuncFParamNode> funcFParamNodes;

    public FuncFParamsNode(List<FuncFParamNode> funcFParamNodes) {
        this.funcFParamNodes = funcFParamNodes;
    }

    public void print() {
        funcFParamNodes.get(0).print();
        for (int i = 1; i < funcFParamNodes.size(); i++) {
            IOUtils.write(Token.constTokens.get(TokenType.COMMA).toString());
            funcFParamNodes.get(i).print();
        }
        IOUtils.write(Parser.nodeType.get(NodeType.FuncFParams));
    }
}
