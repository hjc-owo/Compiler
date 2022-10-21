package node;

import frontend.Parser;
import symbol.FuncParam;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

import java.util.ArrayList;
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

    public List<FuncParam> getParams() {
        List<FuncParam> params = new ArrayList<>();
        for (FuncFParamNode funcFParamNode : funcFParamNodes) {
            params.add(funcFParamNode.getParam());
        }
        return params;
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        for (FuncFParamNode funcFParamNode : funcFParamNodes) {
            funcFParamNode.fillSymbolTable(currentSymbolTable);
        }
    }
}
