package node;

import frontend.Parser;
import symbol.SymbolTable;
import utils.IOUtils;

import java.util.List;

public class CompUnitNode {
    // CompUnit -> {Decl} {FuncDef} MainFuncDef

    private List<DeclNode> declNodes;
    private List<FuncDefNode> funcDefNodes;
    private MainFuncDefNode mainFuncDefNode;

    public CompUnitNode(List<DeclNode> declNodes, List<FuncDefNode> funcDefNodes, MainFuncDefNode mainFuncDefNode) {
        this.declNodes = declNodes;
        this.funcDefNodes = funcDefNodes;
        this.mainFuncDefNode = mainFuncDefNode;
    }

    public void print() {
        for (DeclNode declNode : declNodes) {
            declNode.print();
        }
        for (FuncDefNode funcDefNode : funcDefNodes) {
            funcDefNode.print();
        }
        mainFuncDefNode.print();
        IOUtils.write(Parser.nodeType.get(NodeType.CompUnit));
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        for (DeclNode declNode : declNodes) {
            declNode.fillSymbolTable(currentSymbolTable);
        }
        for (FuncDefNode funcDefNode : funcDefNodes) {
            funcDefNode.fillSymbolTable(currentSymbolTable);
        }
        mainFuncDefNode.fillSymbolTable(currentSymbolTable);
    }
}
