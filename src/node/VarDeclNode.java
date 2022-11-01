package node;

import frontend.Parser;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class VarDeclNode {
    // VarDecl -> BType VarDef { ',' VarDef } ';'
    private BTypeNode bTypeNode;
    private List<VarDefNode> varDefNodes;
    private List<Token> commas;
    private Token semicn;

    public VarDeclNode(BTypeNode bTypeNode, List<VarDefNode> varDefNodes, List<Token> commas, Token semicn) {
        this.bTypeNode = bTypeNode;
        this.varDefNodes = varDefNodes;
        this.commas = commas;
        this.semicn = semicn;
    }

    public List<VarDefNode> getVarDefNodes() {
        return varDefNodes;
    }

    public void print() {
        bTypeNode.print();
        varDefNodes.get(0).print();
        for (int i = 1; i < varDefNodes.size(); i++) {
            IOUtils.write(commas.get(i - 1).toString());
            varDefNodes.get(i).print();
        }
        IOUtils.write(semicn.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.VarDecl));
    }
}
