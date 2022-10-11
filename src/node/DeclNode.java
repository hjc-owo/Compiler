package node;

import symbol.SymbolTable;

public class DeclNode {
    // Decl -> ConstDecl | VarDecl
    private ConstDeclNode constDecl;
    private VarDeclNode varDecl;

    public DeclNode(ConstDeclNode constDecl, VarDeclNode varDecl) {
        this.constDecl = constDecl;
        this.varDecl = varDecl;
    }

    public void print() {
        if (constDecl != null) {
            constDecl.print();
        } else {
            varDecl.print();
        }
    }

    public void fillSymbolTable(SymbolTable currentSymbolTable) {
        if (constDecl != null) {
            constDecl.fillSymbolTable(currentSymbolTable);
        } else {
            varDecl.fillSymbolTable(currentSymbolTable);
        }
    }
}
