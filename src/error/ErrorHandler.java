package error;

import node.*;
import symbol.*;
import utils.IOUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ErrorHandler {

    public static List<Error> errors = new ArrayList<>();

    public static int loopCount = 0;

    private SymbolTable symbolTable = new SymbolTable();
    private SymbolTable currentSymbolTable = symbolTable;

    public List<Error> getErrors() {
        return errors;
    }

    public void printErrors() {
        errors.sort(Error::compareTo);
        for (Error error : errors) {
            IOUtils.error(error.toString());
        }
    }

    public static void addError(Error newError) {
        for (Error error : errors) {
            if (error.equals(newError)) {
                return;
            }
        }
        errors.add(newError);
    }

    public void compUnitError(CompUnitNode compUnitNode) {
        // CompUnit -> {Decl} {FuncDef} MainFuncDef
        for (DeclNode decl : compUnitNode.getDeclNodes()) {
            declError(decl);
        }
        for (FuncDefNode funcDef : compUnitNode.getFuncDefNodes()) {
            funcDefError(funcDef);
        }
        mainFuncDefError(compUnitNode.getMainFuncDefNode());
    }

    private void declError(DeclNode declNode) {
        // Decl -> ConstDecl | VarDecl
        if (declNode.getConstDecl() != null) {
            constDeclError(declNode.getConstDecl());
        } else {
            varDeclError(declNode.getVarDecl());
        }
    }

    private void constDeclError(ConstDeclNode constDeclNode) {
        // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        for (ConstDefNode constDef : constDeclNode.getConstDefNodes()) {
            constDefError(constDef);
        }
    }

    private void constDefError(ConstDefNode constDefNode) {
        // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        if (currentSymbolTable.containsInCurrent(constDefNode.getIdent().getContent())) {
            ErrorHandler.addError(new Error(constDefNode.getIdent().getLineNumber(), ErrorType.b));
        }
        int dimension = constDefNode.getConstExpNodes().size();
        if (dimension == 0) {
            currentSymbolTable.put(constDefNode.getIdent().getContent(), new ArraySymbol(constDefNode.getIdent().getContent(), true, 0));
        } else {
            for (ConstExpNode constExpNode : constDefNode.getConstExpNodes()) {
                constExpError(constExpNode);
            }
            currentSymbolTable.put(constDefNode.getIdent().getContent(), new ArraySymbol(constDefNode.getIdent().getContent(), true, constDefNode.getConstExpNodes().size()));
        }
        constInitValError(constDefNode.getConstInitValNode());
    }

    private void constInitValError(ConstInitValNode constInitValNode) {
        // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        if (constInitValNode.getConstExpNode() != null) {
            constExpError(constInitValNode.getConstExpNode());
        } else {
            for (ConstInitValNode constInitVal : constInitValNode.getConstInitValNodes()) {
                constInitValError(constInitVal);
            }
        }
    }

    private void varDeclError(VarDeclNode varDeclNode) {
        // VarDecl -> BType VarDef { ',' VarDef } ';'
        for (VarDefNode varDef : varDeclNode.getVarDefNodes()) {
            varDefError(varDef);
        }
    }

    private void varDefError(VarDefNode varDefNode) {
        // VarDef -> Ident { '[' ConstExp ']' } [ '=' InitVal ]
        if (currentSymbolTable.containsInCurrent(varDefNode.getIdent().getContent())) {
            ErrorHandler.addError(new Error(varDefNode.getIdent().getLineNumber(), ErrorType.b));
        }
        int dimension = varDefNode.getConstExpNodes().size();
        if (dimension == 0) {
            currentSymbolTable.put(varDefNode.getIdent().getContent(), new ArraySymbol(varDefNode.getIdent().getContent(), false, 0));
        } else {
            for (ConstExpNode constExpNode : varDefNode.getConstExpNodes()) {
                constExpError(constExpNode);
            }
            currentSymbolTable.put(varDefNode.getIdent().getContent(), new ArraySymbol(varDefNode.getIdent().getContent(), false, varDefNode.getConstExpNodes().size()));
        }
        if (varDefNode.getInitValNode() != null) {
            initValError(varDefNode.getInitValNode());
        }
    }

    private void initValError(InitValNode initValNode) {
        // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
        if (initValNode.getExpNode() != null) {
            expError(initValNode.getExpNode());
        } else {
            for (InitValNode initVal : initValNode.getInitValNodes()) {
                initValError(initVal);
            }
        }
    }

    private void funcDefError(FuncDefNode funcDefNode) {
        // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        if (currentSymbolTable.containsInCurrent(funcDefNode.getIdent().getContent())) {
            ErrorHandler.addError(new Error(funcDefNode.getIdent().getLineNumber(), ErrorType.b));
        }
        if (funcDefNode.getFuncFParamsNode() == null) {
            currentSymbolTable.put(funcDefNode.getIdent().getContent(), new FuncSymbol(funcDefNode.getIdent().getContent(), funcDefNode.getFuncTypeNode().getType(), new ArrayList<>()));
        } else {
            List<FuncParam> params = new ArrayList<>();
            for (FuncFParamNode funcFParamNode : funcDefNode.getFuncFParamsNode().getFuncFParamNodes()) {
                params.add(new FuncParam(funcFParamNode.getIdent().getContent(), funcFParamNode.getLeftBrackets().size()));
            }
            currentSymbolTable.put(funcDefNode.getIdent().getContent(), new FuncSymbol(funcDefNode.getIdent().getContent(), funcDefNode.getFuncTypeNode().getType(), params));
        }
        SymbolTable table = currentSymbolTable;
        currentSymbolTable = new FuncSymbolTable(currentSymbolTable, funcDefNode.getFuncTypeNode().getType());
        if (funcDefNode.getFuncFParamsNode() != null) {
            funcFParamsError(funcDefNode.getFuncFParamsNode());
        }
        blockError(funcDefNode.getBlockNode());
        currentSymbolTable = table;
    }

    private void mainFuncDefError(MainFuncDefNode mainFuncDefNode) {
        // MainFuncDef -> 'int' 'main' '(' ')' Block
        SymbolTable table = currentSymbolTable;
        currentSymbolTable = new FuncSymbolTable(currentSymbolTable, FuncType.INT);
        blockError(mainFuncDefNode.getBlockNode());
        currentSymbolTable = table;
    }

    private void funcFParamsError(FuncFParamsNode funcFParamsNode) {
        // FuncFParams -> FuncFParam { ',' FuncFParam }
        for (FuncFParamNode funcFParam : funcFParamsNode.getFuncFParamNodes()) {
            funcFParamError(funcFParam);
        }
    }

    private void funcFParamError(FuncFParamNode funcFParamNode) {
        // FuncFParam -> BType Ident [ '[' ']' { '[' ConstExp ']' }]
        if (currentSymbolTable.containsInCurrent(funcFParamNode.getIdent().getContent())) {
            ErrorHandler.addError(new Error(funcFParamNode.getIdent().getLineNumber(), ErrorType.b));
        }
        currentSymbolTable.put(funcFParamNode.getIdent().getContent(), new ArraySymbol(funcFParamNode.getIdent().getContent(), false, funcFParamNode.getLeftBrackets().size()));
    }

    private void blockError(BlockNode blockNode) {
        // Block -> '{' { BlockItem } '}'
        for (BlockItemNode blockItemNode : blockNode.getBlockItemNodes()) {
            blockItemNodeError(blockItemNode);
        }
        if (currentSymbolTable instanceof FuncSymbolTable) {
            FuncSymbolTable funcSymbolTable = (FuncSymbolTable) currentSymbolTable;
            if (blockNode.getBlockItemNodes().size() > 0) {
                if (funcSymbolTable.getType() == FuncType.INT) {
                    if (blockNode.getBlockItemNodes().get(blockNode.getBlockItemNodes().size() - 1).getStmtNode() == null) {
                        ErrorHandler.addError(new Error(blockNode.getRightBraceToken().getLineNumber(), ErrorType.g));
                    } else if (blockNode.getBlockItemNodes().get(blockNode.getBlockItemNodes().size() - 1).getStmtNode().getReturnToken() == null) {
                        ErrorHandler.addError(new Error(blockNode.getRightBraceToken().getLineNumber(), ErrorType.g));
                    }
                }
            } else {
                if (funcSymbolTable.getType() == FuncType.INT) {
                    ErrorHandler.addError(new Error(blockNode.getRightBraceToken().getLineNumber(), ErrorType.g));
                }
            }
        }
    }

    private void blockItemNodeError(BlockItemNode blockItemNode) {
        // BlockItem -> Decl | Stmt
        if (blockItemNode.getDeclNode() != null) {
            declError(blockItemNode.getDeclNode());
        } else {
            stmtError(blockItemNode.getStmtNode());
        }
    }

    private void stmtError(StmtNode stmtNode) {
        // Stmt -> LVal '=' Exp ';'
        //	| [Exp] ';'
        //	| Block
        //	| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        //	| 'while' '(' Cond ')' Stmt
        //	| 'break' ';' | 'continue' ';'
        //	| 'return' [Exp] ';'
        //	| LVal '=' 'getint' '(' ')' ';'
        //	| 'printf' '(' FormatString { ',' Exp } ')' ';'
        switch (stmtNode.getType()) {
            case Exp:
                // [Exp] ';'
                if (stmtNode.getExpNode() != null) expError(stmtNode.getExpNode());
                break;
            case Block:
                // Block
                SymbolTable table = currentSymbolTable;
                currentSymbolTable = new SymbolTable(currentSymbolTable);
                blockError(stmtNode.getBlockNode());
                currentSymbolTable = table;
                break;
            case If:
                // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                condError(stmtNode.getCondNode());
                stmtError(stmtNode.getStmtNodes().get(0));
                if (stmtNode.getElseToken() != null) {
                    stmtError(stmtNode.getStmtNodes().get(1));
                }
                break;
            case While:
                // 'while' '(' Cond ')' Stmt
                ErrorHandler.loopCount++;
                stmtError(stmtNode.getStmtNodes().get(0));
                ErrorHandler.loopCount--;
                break;
            case Break:
                // 'break' ';'
            case Continue:
                // 'continue' ';'
                if (ErrorHandler.loopCount == 0) {
                    ErrorHandler.addError(new Error(stmtNode.getBreakOrContinueToken().getLineNumber(), ErrorType.m));
                }
                break;
            case Return:
                // 'return' [Exp] ';'
                if (currentSymbolTable instanceof FuncSymbolTable) {
                    FuncSymbolTable funcSymbolTable = (FuncSymbolTable) currentSymbolTable;
                    if (funcSymbolTable.getType() == FuncType.VOID && stmtNode.getExpNode() != null) {
                        ErrorHandler.addError(new Error(stmtNode.getReturnToken().getLineNumber(), ErrorType.f));
                    }
                }
                break;
            case LValAssignExp:
                // LVal '=' Exp ';'
            case LValAssignGetint:
                // LVal '=' 'getint' '(' ')' ';'
                if (currentSymbolTable.get(stmtNode.getLValNode().getIdent().getContent()) instanceof ArraySymbol) {
                    ArraySymbol arraySymbol = (ArraySymbol) currentSymbolTable.get(stmtNode.getLValNode().getIdent().getContent());
                    if (arraySymbol.isConst()) {
                        ErrorHandler.addError(new Error(stmtNode.getLValNode().getIdent().getLineNumber(), ErrorType.h));
                    }
                }
                lValError(stmtNode.getLValNode());
                break;
            case Printf:
                // 'printf' '(' FormatString { ',' Exp } ')' ';'
                int numOfExp = stmtNode.getExpNodes().size();
                int numOfFormatString = 0;
                for (int i = 0; i < stmtNode.getFormatString().toString().length(); i++) {
                    if (stmtNode.getFormatString().toString().charAt(i) == '%') {
                        if (stmtNode.getFormatString().toString().charAt(i + 1) == 'd') {
                            numOfFormatString++;
                        }
                    }
                }
                if (numOfExp != numOfFormatString) {
                    ErrorHandler.addError(new Error(stmtNode.getPrintfToken().getLineNumber(), ErrorType.l));
                }
                break;
        }
    }

    private void expError(ExpNode expNode) {
        // Exp -> AddExp
        addExpError(expNode.getAddExpNode());
    }

    private FuncParam getFuncParamInExp(ExpNode expNode) {
        // Exp -> AddExp
        return getFuncParamInAddExp(expNode.getAddExpNode());
    }

    private void condError(CondNode condNode) {
        // Cond -> LOrExp
        lOrExpError(condNode.getLOrExpNode());
    }

    private void lValError(LValNode lValNode) {
        // LVal -> Ident {'[' Exp ']'}
        if (!currentSymbolTable.contains(lValNode.getIdent().getContent())) {
            ErrorHandler.addError(new Error(lValNode.getIdent().getLineNumber(), ErrorType.c));
        }
        for (ExpNode expNode : lValNode.getExpNodes()) {
            expError(expNode);
        }
    }

    private FuncParam getFuncParamInLVal(LValNode lValNode) {
        return new FuncParam(lValNode.getIdent().getContent(), lValNode.getExpNodes().size());
    }

    private void primaryExpError(PrimaryExpNode primaryExpNode) {
        // PrimaryExp -> '(' Exp ')' | LVal | Number
        if (primaryExpNode.getExpNode() != null) {
            expError(primaryExpNode.getExpNode());
        } else if (primaryExpNode.getLValNode() != null) {
            lValError(primaryExpNode.getLValNode());
        }
    }

    private FuncParam getFuncParamInPrimaryExp(PrimaryExpNode primaryExpNode) {
        // PrimaryExp -> '(' Exp ')' | LVal | Number
        if (primaryExpNode.getExpNode() != null) {
            return getFuncParamInExp(primaryExpNode.getExpNode());
        } else if (primaryExpNode.getLValNode() != null) {
            return getFuncParamInLVal(primaryExpNode.getLValNode());
        } else {
            return new FuncParam(null, 0);
        }
    }

    private void unaryExpError(UnaryExpNode unaryExpNode) {
        // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        if (unaryExpNode.getPrimaryExpNode() != null) {
            primaryExpError(unaryExpNode.getPrimaryExpNode());
        } else if (unaryExpNode.getIdent() != null) {
            if (!currentSymbolTable.contains(unaryExpNode.getIdent().getContent())) {
                ErrorHandler.addError(new Error(unaryExpNode.getIdent().getLineNumber(), ErrorType.c));
            }
            Symbol symbol = currentSymbolTable.get(unaryExpNode.getIdent().getContent());
            if (symbol instanceof FuncSymbol) {
                FuncSymbol funcSymbol = (FuncSymbol) symbol;
                if (unaryExpNode.getFuncRParamsNode() == null) {
                    if (funcSymbol.getFuncParams().size() != 0) {
                        ErrorHandler.addError(new Error(unaryExpNode.getIdent().getLineNumber(), ErrorType.d));
                    }
                } else {
                    if (funcSymbol.getFuncParams().size() != unaryExpNode.getFuncRParamsNode().getExpNodes().size()) {
                        ErrorHandler.addError(new Error(unaryExpNode.getIdent().getLineNumber(), ErrorType.d));
                    }
                    List<Integer> funcFParamDimensions = new ArrayList<>();
                    for (FuncParam funcParam : funcSymbol.getFuncParams()) {
                        funcFParamDimensions.add(funcParam.getDimension());
                    }
                    List<Integer> funcRParamDimensions = new ArrayList<>();
                    if (unaryExpNode.getFuncRParamsNode() != null) {
                        for (ExpNode expNode : unaryExpNode.getFuncRParamsNode().getExpNodes()) {
                            FuncParam funcRParam = getFuncParamInExp(expNode);
                            if (funcRParam != null) {
                                if (funcRParam.getName() == null) {
                                    funcRParamDimensions.add(funcRParam.getDimension());
                                } else {
                                    Symbol symbol2 = currentSymbolTable.get(funcRParam.getName());
                                    if (symbol2 instanceof ArraySymbol) {
                                        funcRParamDimensions.add(((ArraySymbol) symbol2).getDimension() - funcRParam.getDimension());
                                    } else if (symbol2 instanceof FuncSymbol) {
                                        funcRParamDimensions.add(((FuncSymbol) symbol2).getType() == FuncType.VOID ? -1 : 0);
                                    }
                                }
                            }
                        }
                    }
                    if (!Objects.equals(funcFParamDimensions, funcRParamDimensions)) {
                        ErrorHandler.addError(new Error(unaryExpNode.getIdent().getLineNumber(), ErrorType.e));
                    }
                }
            } else {
                ErrorHandler.addError(new Error(unaryExpNode.getIdent().getLineNumber(), ErrorType.c));
            }
        } else {
            unaryExpError(unaryExpNode.getUnaryExpNode());
        }
    }

    private FuncParam getFuncParamInUnaryExp(UnaryExpNode unaryExpNode) {
        if (unaryExpNode.getPrimaryExpNode() != null) {
            return getFuncParamInPrimaryExp(unaryExpNode.getPrimaryExpNode());
        } else if (unaryExpNode.getIdent() != null) {
            return currentSymbolTable.get(unaryExpNode.getIdent().getContent()) instanceof FuncSymbol ? new FuncParam(unaryExpNode.getIdent().getContent(), 0) : null;
        } else {
            return getFuncParamInUnaryExp(unaryExpNode.getUnaryExpNode());
        }
    }

    private void mulExpError(MulExpNode mulExpNode) {
        // MulExp -> UnaryExp | UnaryExp ('\*' | '/' | '%') MulExp
        unaryExpError(mulExpNode.getUnaryExpNode());
        if (mulExpNode.getMulExpNode() != null) {
            mulExpError(mulExpNode.getMulExpNode());
        }
    }

    private FuncParam getFuncParamInMulExp(MulExpNode mulExpNode) {
        return getFuncParamInUnaryExp(mulExpNode.getUnaryExpNode());
    }

    private void addExpError(AddExpNode addExpNode) {
        // AddExp -> MulExp | MulExp ('+' | '-') AddExp
        mulExpError(addExpNode.getMulExpNode());
        if (addExpNode.getAddExpNode() != null) {
            addExpError(addExpNode.getAddExpNode());
        }
    }

    private FuncParam getFuncParamInAddExp(AddExpNode addExpNode) {
        // AddExp -> MulExp | MulExp ('+' | '-') AddExp
        return getFuncParamInMulExp(addExpNode.getMulExpNode());
    }

    private void relExpError(RelExpNode relExpNode) {
        // RelExp -> AddExp | AddExp ('<' | '>' | '<=' | '>=') RelExp
        addExpError(relExpNode.getAddExpNode());
        if (relExpNode.getRelExpNode() != null) {
            relExpError(relExpNode.getRelExpNode());
        }
    }

    private void eqExpError(EqExpNode eqExpNode) {
        // EqExp -> RelExp | RelExp ('==' | '!=') EqExp
        relExpError(eqExpNode.getRelExpNode());
        if (eqExpNode.getEqExpNode() != null) {
            eqExpError(eqExpNode.getEqExpNode());
        }
    }

    private void lAndExpError(LAndExpNode lAndExpNode) {
        // LAndExp -> EqExp | EqExp '&&' LAndExp
        eqExpError(lAndExpNode.getEqExpNode());
        if (lAndExpNode.getLAndExpNode() != null) {
            lAndExpError(lAndExpNode.getLAndExpNode());
        }
    }

    private void lOrExpError(LOrExpNode lOrExpNode) {
        // LOrExp -> LAndExp | LAndExp '||' LOrExp
        lAndExpError(lOrExpNode.getLAndExpNode());
        if (lOrExpNode.getLOrExpNode() != null) {
            lOrExpError(lOrExpNode.getLOrExpNode());
        }
    }

    private void constExpError(ConstExpNode constExpNode) {
        // ConstExp -> AddExp
        addExpError(constExpNode.getAddExpNode());
    }

}
