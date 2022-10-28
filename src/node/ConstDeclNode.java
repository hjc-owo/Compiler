package node;

import frontend.Parser;
import symbol.SymbolTable;
import token.Token;
import utils.IOUtils;

import java.util.List;

public class ConstDeclNode {
    // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    private Token constToken;
    private BTypeNode bTypeNode;
    private List<ConstDefNode> constDefNodes;
    private List<Token> commas;
    private Token semicnToken;

    public ConstDeclNode(Token constToken, BTypeNode bTypeNode, List<ConstDefNode> constDefNodes, List<Token> commas, Token semicnToken) {
        this.constToken = constToken;
        this.bTypeNode = bTypeNode;
        this.constDefNodes = constDefNodes;
        this.commas = commas;
        this.semicnToken = semicnToken;
    }

    public Token getConstToken() {
        return constToken;
    }

    public BTypeNode getbTypeNode() {
        return bTypeNode;
    }

    public List<ConstDefNode> getConstDefNodes() {
        return constDefNodes;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public Token getSemicnToken() {
        return semicnToken;
    }

    public void print() {
        IOUtils.write(constToken.toString());
        bTypeNode.print();
        constDefNodes.get(0).print();
        for (int i = 1; i < constDefNodes.size(); i++) {
            IOUtils.write(commas.get(i - 1).toString());
            constDefNodes.get(i).print();
        }
        IOUtils.write(semicnToken.toString());
        IOUtils.write(Parser.nodeType.get(NodeType.ConstDecl));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        for (ConstDefNode constDefNode : constDefNodes) {
            constDefNode.fillSymbolTable(currentSymbolTable);
        }
    }
}
