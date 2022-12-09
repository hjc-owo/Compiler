package ir;

import config.Config;
import ir.types.*;
import ir.values.*;
import ir.values.instructions.ConstArray;
import ir.values.instructions.Operator;
import node.*;
import token.TokenType;
import utils.Pair;

import java.util.*;

public class LLVMGenerator {
    private static final LLVMGenerator instance = new LLVMGenerator();

    public static LLVMGenerator getInstance() {
        return instance;
    }

    private BuildFactory buildFactory = BuildFactory.getInstance();

    private BasicBlock curBlock = null;
    private BasicBlock curTrueBlock = null;
    private BasicBlock curFalseBlock = null;
    private BasicBlock continueBlock = null;
    private BasicBlock curWhileFinalBlock = null;
    private Function curFunction = null;

    /**
     * 计算时需要保留的
     */
    private Integer saveValue = null;
    private Operator saveOp = null;
    private int tmpIndex = 0;
    private Operator tmpOp = null;
    private Type tmpType = null;
    private Value tmpValue = null;
    private List<Value> tmpList = null;
    private List<Type> tmpTypeList = null;
    private List<Value> funcArgsList = null;

    private boolean isGlobal = true;
    private boolean isConst = false;
    private boolean isArray = false;
    private boolean isRegister = false;

    /**
     * 数组相关
     */
    private Value curArray = null;
    private String tmpName = null;
    private int tmpDepth = 0;
    private int tmpOffset = 0;
    private List<Integer> tmpDims = null;


    /**
     * 新符号表系统和常量表
     */
    private List<Pair<Map<String, Value>, Map<String, Integer>>> symbolTable = new ArrayList<>();

    public Map<String, Value> getCurSymbolTable() {
        return symbolTable.get(symbolTable.size() - 1).getFirst();
    }

    public void addSymbol(String name, Value value) {
        getCurSymbolTable().put(name, value);
    }

    public void addGlobalSymbol(String name, Value value) {
        symbolTable.get(0).getFirst().put(name, value);
    }

    public Value getValue(String name) {
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            if (symbolTable.get(i).getFirst().containsKey(name)) {
                return symbolTable.get(i).getFirst().get(name);
            }
        }
        return null;
    }

    /**
     * 常量表
     */

    public Map<String, Integer> getCurConstTable() {
        return symbolTable.get(symbolTable.size() - 1).getSecond();
    }

    public void addConst(String name, Integer value) {
        getCurConstTable().put(name, value);
    }

    public Integer getConst(String name) {
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            if (symbolTable.get(i).getSecond().containsKey(name)) {
                return symbolTable.get(i).getSecond().get(name);
            }
        }
        return 0;
    }

    public void changeConst(String name, Integer value) {
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            if (symbolTable.get(i).getSecond().containsKey(name)) {
                symbolTable.get(i).getSecond().put(name, value);
                return;
            }
        }
    }

    /**
     * 添加和删除当前块符号表和常量表
     */
    public void addSymbolAndConstTable() {
        symbolTable.add(new Pair<>(new HashMap<>(), new HashMap<>()));
    }

    public void removeSymbolAndConstTable() {
        symbolTable.remove(symbolTable.size() - 1);
    }

    public int calculate(Operator op, int a, int b) {
        switch (op) {
            case Add:
                return a + b;
            case Sub:
                return a - b;
            case Mul:
                return a * b;
            case Div:
                return a / b;
            case Mod:
                return a % b;
            default:
                return 0;
        }
    }

    /**
     * 字符串相关
     */
    private List<String> stringList = new ArrayList<>();

    private int getStringIndex(String str) {
        for (int i = 0; i < stringList.size(); i++) {
            if (stringList.get(i).equals(str)) {
                return i;
            }
        }
        stringList.add(str);
        Type type = buildFactory.getArrayType(IntegerType.i8, str.length() + 1);
        Value value = buildFactory.buildGlobalVar(getStringName(str), type, true, buildFactory.getConstString(str));
        addGlobalSymbol(getStringName(str), value);
        return stringList.size() - 1;
    }

    private String getStringName(int index) {
        return "_str_" + index;
    }

    private String getStringName(String str) {
        return getStringName(getStringIndex(str));
    }

    /**
     * 遍历语法树
     */
    public void visitCompUnit(CompUnitNode compUnitNode) {
        addSymbolAndConstTable();
        addSymbol("getint", buildFactory.buildLibraryFunction("getint", IntegerType.i32, new ArrayList<>()));
        addSymbol("putint", buildFactory.buildLibraryFunction("putint", VoidType.voidType, new ArrayList<>(Collections.singleton(IntegerType.i32))));
        addSymbol("putch", buildFactory.buildLibraryFunction("putch", VoidType.voidType, new ArrayList<>(Collections.singleton(IntegerType.i32))));
        addSymbol("putstr", buildFactory.buildLibraryFunction("putstr", VoidType.voidType, new ArrayList<>(Collections.singleton(new PointerType(IntegerType.i8)))));

        // CompUnit -> {Decl} {FuncDef} MainFuncDef
        for (DeclNode declNode : compUnitNode.getDeclNodes()) {
            visitDecl(declNode);
        }
        for (FuncDefNode funcDefNode : compUnitNode.getFuncDefNodes()) {
            visitFuncDef(funcDefNode);
        }
        visitMainFuncDef(compUnitNode.getMainFuncDefNode());
    }

    public void visitDecl(DeclNode declNode) {
        // Decl -> ConstDecl | VarDecl
        if (declNode.getConstDecl() != null) {
            visitConstDecl(declNode.getConstDecl());
        } else {
            visitVarDecl(declNode.getVarDecl());
        }
    }

    public void visitConstDecl(ConstDeclNode constDeclNode) {
        // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        tmpType = IntegerType.i32;
        for (ConstDefNode constDefNode : constDeclNode.getConstDefNodes()) {
            visitConstDef(constDefNode);
        }
    }

    private void visitConstDef(ConstDefNode constDefNode) {
        // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        String name = constDefNode.getIdent().getContent();
        if (constDefNode.getConstExpNodes().isEmpty()) {
            // is not an array
            visitConstInitVal(constDefNode.getConstInitValNode());
            tmpValue = buildFactory.getConstInt(saveValue == null ? 0 : saveValue);
            addConst(name, saveValue);
            if (isGlobal) {
                tmpValue = buildFactory.buildGlobalVar(name, tmpType, true, tmpValue);
                addSymbol(name, tmpValue);
            } else {
                tmpValue = buildFactory.buildVar(curBlock, tmpValue, true, tmpType);
                addSymbol(name, tmpValue);
            }
        } else {
            // is an array
            List<Integer> dims = new ArrayList<>();
            for (ConstExpNode constExpNode : constDefNode.getConstExpNodes()) {
                visitConstExp(constExpNode);
                dims.add(saveValue);
            }
            tmpDims = new ArrayList<>(dims);
            Type type = null;
            for (int i = dims.size() - 1; i >= 0; i--) {
                if (type == null) {
                    type = buildFactory.getArrayType(tmpType, dims.get(i));
                } else {
                    type = buildFactory.getArrayType(type, dims.get(i));
                }
            }
            if (isGlobal) {
                tmpValue = buildFactory.buildGlobalArray(name, type, true);
                ((ConstArray) ((GlobalVar) tmpValue).getValue()).setInit(true);
            } else {
                tmpValue = buildFactory.buildArray(curBlock, true, type);
            }
            addSymbol(name, tmpValue);
            curArray = tmpValue;
            isArray = true;
            tmpName = name;
            tmpDepth = 0;
            tmpOffset = 0;
            visitConstInitVal(constDefNode.getConstInitValNode());
            isArray = false;
        }
    }

    private void visitConstInitVal(ConstInitValNode constInitValNode) {
        // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        if (constInitValNode.getConstExpNode() != null && !isArray) {
            // is not an array
            visitConstExp(constInitValNode.getConstExpNode());
        } else {
            // '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
            if (constInitValNode.getConstExpNode() != null) {
                tmpValue = null;
                visitConstExp(constInitValNode.getConstExpNode());
                tmpDepth = 1;
                tmpValue = buildFactory.getConstInt(saveValue);
                if (isGlobal) {
                    buildFactory.buildInitArray(curArray, tmpOffset, tmpValue);
                } else {
                    buildFactory.buildStore(curBlock, buildFactory.buildGEP(curBlock, curArray, tmpOffset), tmpValue);
                }
                StringBuilder name = new StringBuilder(tmpName);
                List<Value> args = ((ArrayType) ((PointerType) curArray.getType()).getTargetType()).offset2Index(tmpOffset);
                for (Value v : args) {
                    name.append(((ConstInt) v).getValue()).append(";");
                }
                addConst(name.toString(), saveValue);
                tmpOffset++;
            } else if (!constInitValNode.getConstInitValNodes().isEmpty()) {
                int depth = 0, offset = tmpOffset;
                for (ConstInitValNode constInitValNode1 : constInitValNode.getConstInitValNodes()) {
                    visitConstInitVal(constInitValNode1);
                    depth = Math.max(depth, tmpDepth);
                }
                depth++;
                int size = 1;
                for (int i = 1; i < depth; i++) {
                    size *= tmpDims.get(tmpDims.size() - i);
                }
                tmpOffset = Math.max(tmpOffset, offset + size);
                tmpDepth = depth;
            }
        }
    }

    private void visitVarDecl(VarDeclNode varDeclNode) {
        // VarDecl -> BType VarDef { ',' VarDef } ';'
        tmpType = IntegerType.i32;
        for (VarDefNode varDefNode : varDeclNode.getVarDefNodes()) {
            visitVarDef(varDefNode);
        }
    }

    private void visitVarDef(VarDefNode varDefNode) {
        // VarDef -> Ident { '[' ConstExp ']' } [ '=' InitVal ]
        String name = varDefNode.getIdent().getContent();
        if (varDefNode.getConstExpNodes().isEmpty()) {
            // is not an array
            if (varDefNode.getInitValNode() != null) {
                tmpValue = null;
                if (isGlobal) {
                    isConst = true;
                    saveValue = null;
                }
                visitInitVal(varDefNode.getInitValNode());
                isConst = false;
            } else {
                tmpValue = null;
                if (isGlobal) {
                    saveValue = null;
                }
            }
            if (isGlobal) {
                tmpValue = buildFactory.buildGlobalVar(name, tmpType, false, buildFactory.getConstInt(saveValue == null ? 0 : saveValue));
                addSymbol(name, tmpValue);
            } else {
                tmpValue = buildFactory.buildVar(curBlock, tmpValue, isConst, tmpType);
                addSymbol(name, tmpValue);
            }
        } else {
            // is an array
            isConst = true;
            List<Integer> dims = new ArrayList<>();
            for (ConstExpNode constExpNode : varDefNode.getConstExpNodes()) {
                visitConstExp(constExpNode);
                dims.add(saveValue);
            }
            isConst = false;
            tmpDims = new ArrayList<>(dims);
            Type type = null;
            for (int i = dims.size() - 1; i >= 0; i--) {
                if (type == null) {
                    type = buildFactory.getArrayType(tmpType, dims.get(i));
                } else {
                    type = buildFactory.getArrayType(type, dims.get(i));
                }
            }
            if (isGlobal) {
                tmpValue = buildFactory.buildGlobalArray(name, type, false);
                if (varDefNode.getInitValNode() != null) {
                    ((ConstArray) ((GlobalVar) tmpValue).getValue()).setInit(true);
                }
            } else {
                tmpValue = buildFactory.buildArray(curBlock, false, type);
            }
            addSymbol(name, tmpValue);
            curArray = tmpValue;
            if (varDefNode.getInitValNode() != null) {
                isArray = true;
                tmpName = name;
                tmpDepth = 0;
                tmpOffset = 0;
                visitInitVal(varDefNode.getInitValNode());
                isArray = false;
            }
            isConst = false;
        }
    }

    private void visitInitVal(InitValNode initValNode) {
        // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
        if (initValNode.getExpNode() != null && !isArray) {
            // Exp
            visitExp(initValNode.getExpNode());
        } else {
            // '{' [ InitVal { ',' InitVal } ] '}'
            if (initValNode.getExpNode() != null) {
                if (isGlobal) {
                    isConst = true;
                }
                saveValue = null;
                tmpValue = null;
                visitExp(initValNode.getExpNode());
                isConst = false;
                tmpDepth = 1;
                if (isGlobal) {
                    tmpValue = buildFactory.getConstInt(saveValue);
                    buildFactory.buildInitArray(curArray, tmpOffset, tmpValue);
                } else {
                    buildFactory.buildStore(curBlock, buildFactory.buildGEP(curBlock, curArray, tmpOffset), tmpValue);
                }
                tmpOffset++;
            } else if (!initValNode.getInitValNodes().isEmpty()) {
                int depth = 0, offset = tmpOffset;
                for (InitValNode initValNode1 : initValNode.getInitValNodes()) {
                    visitInitVal(initValNode1);
                    depth = Math.max(depth, tmpDepth);
                }
                depth++;
                int size = 1;
                for (int i = 1; i < depth; i++) {
                    size *= tmpDims.get(tmpDims.size() - i);
                }
                tmpOffset = Math.max(tmpOffset, offset + size);
                tmpDepth = depth;
            }
        }
    }

    private void visitFuncDef(FuncDefNode funcDefNode) {
        // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        isGlobal = false;
        String funcName = funcDefNode.getIdent().getContent();
        Type type = funcDefNode.getFuncTypeNode().getToken().getType() == TokenType.INTTK ? IntegerType.i32 : VoidType.voidType;
        tmpTypeList = new ArrayList<>();
        if (funcDefNode.getFuncFParamsNode() != null) {
            visitFuncFParams(funcDefNode.getFuncFParamsNode());
        }
        Function function = buildFactory.buildFunction(funcName, type, tmpTypeList);
        curFunction = function;
        addSymbol(funcName, function);
        addSymbolAndConstTable();
        addSymbol(funcName, function);
        curBlock = buildFactory.buildBasicBlock(curFunction);
        funcArgsList = buildFactory.getFunctionArguments(curFunction);
        isRegister = true;
        if (funcDefNode.getFuncFParamsNode() != null) {
            visitFuncFParams(funcDefNode.getFuncFParamsNode());
        }
        isRegister = false;
        visitBlock(funcDefNode.getBlockNode());
        isGlobal = true;
        removeSymbolAndConstTable();
        buildFactory.checkBlockEnd(curBlock);
    }

    private void visitMainFuncDef(MainFuncDefNode mainFuncDefNode) {
        // MainFuncDef -> 'int' 'main' '(' ')' Block
        isGlobal = false;
        Function function = buildFactory.buildFunction("main", IntegerType.i32, new ArrayList<>());
        curFunction = function;
        addSymbol("main", function);
        addSymbolAndConstTable();
        addSymbol("main", function);
        curBlock = buildFactory.buildBasicBlock(curFunction);
        funcArgsList = buildFactory.getFunctionArguments(curFunction);
        visitBlock(mainFuncDefNode.getBlockNode());
        isGlobal = true;
        removeSymbolAndConstTable();
        buildFactory.checkBlockEnd(curBlock);
    }

    private void visitFuncFParams(FuncFParamsNode funcFParamsNode) {
        // FuncFParams -> FuncFParam { ',' FuncFParam }
        if (isRegister) {
            tmpIndex = 0;
            for (FuncFParamNode funcFParamNode : funcFParamsNode.getFuncFParamNodes()) {
                visitFuncFParam(funcFParamNode);
                tmpIndex++;
            }
        } else {
            tmpTypeList = new ArrayList<>();
            for (FuncFParamNode funcFParamNode : funcFParamsNode.getFuncFParamNodes()) {
                visitFuncFParam(funcFParamNode);
                tmpTypeList.add(tmpType);
            }
        }
    }

    private void visitFuncFParam(FuncFParamNode funcFParamNode) {
        // BType Ident [ '[' ']' { '[' ConstExp ']' }]
        if (isRegister) {
            int i = tmpIndex;
            Value value = buildFactory.buildVar(curBlock, funcArgsList.get(i), false, tmpTypeList.get(i));
            addSymbol(funcFParamNode.getIdent().getContent(), value);
        } else {
            if (funcFParamNode.getLeftBrackets().isEmpty()) {
                tmpType = IntegerType.i32;
            } else {
                List<Integer> dims = new ArrayList<>();
                dims.add(-1);
                if (!funcFParamNode.getConstExpNodes().isEmpty()) {
                    for (ConstExpNode constExpNode : funcFParamNode.getConstExpNodes()) {
                        isConst = true;
                        visitConstExp(constExpNode);
                        dims.add(saveValue);
                        isConst = false;
                    }
                }
                tmpType = null;
                for (int i = dims.size() - 1; i >= 0; i--) {
                    if (tmpType == null) {
                        tmpType = IntegerType.i32;
                    }
                    tmpType = buildFactory.getArrayType(tmpType, dims.get(i));
                }
            }
        }
    }

    private void visitBlock(BlockNode blockNode) {
        // Block -> '{' { BlockItem } '}'
        for (BlockItemNode blockItemNode : blockNode.getBlockItemNodes()) {
            visitBlockItem(blockItemNode);
        }
    }

    private void visitBlockItem(BlockItemNode blockItemNode) {
        // BlockItem -> Decl | Stmt
        if (blockItemNode.getDeclNode() != null) {
            visitDecl(blockItemNode.getDeclNode());
        } else {
            visitStmt(blockItemNode.getStmtNode());
        }
    }

    private void visitStmt(StmtNode stmtNode) {
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
            case LValAssignExp:
                if (stmtNode.getLValNode().getExpNodes().isEmpty()) {
                    // is not an array
                    Value input = getValue(stmtNode.getLValNode().getIdent().getContent());
                    visitExp(stmtNode.getExpNode());
                    tmpValue = buildFactory.buildStore(curBlock, input, tmpValue);
                } else {
                    // is an array
                    List<Value> indexList = new ArrayList<>();
                    for (ExpNode expNode : stmtNode.getLValNode().getExpNodes()) {
                        visitExp(expNode);
                        indexList.add(tmpValue);
                    }
                    tmpValue = getValue(stmtNode.getLValNode().getIdent().getContent());
                    Value addr;
                    Type type = tmpValue.getType(), targetType = ((PointerType) type).getTargetType();
                    if (targetType instanceof PointerType) {
                        // arr[][3]
                        tmpValue = buildFactory.buildLoad(curBlock, tmpValue);
                    } else {
                        // arr[3][2]
                        indexList.add(0, ConstInt.ZERO);
                    }
                    addr = buildFactory.buildGEP(curBlock, tmpValue, indexList);
                    visitExp(stmtNode.getExpNode());
                    tmpValue = buildFactory.buildStore(curBlock, addr, tmpValue);
                }
                break;
            case Exp:
                if (stmtNode.getExpNode() != null) {
                    visitExp(stmtNode.getExpNode());
                }
                break;
            case Block:
                addSymbolAndConstTable();
                visitBlock(stmtNode.getBlockNode());
                removeSymbolAndConstTable();
                break;
            case If:
                if (stmtNode.getElseToken() == null) {
                    // basicBlock;
                    // if (...) {
                    //    trueBlock;
                    // }
                    // finalBlock;
                    BasicBlock basicBlock = curBlock;

                    BasicBlock trueBlock = buildFactory.buildBasicBlock(curFunction);
                    curBlock = trueBlock;
                    visitStmt(stmtNode.getStmtNodes().get(0));
                    BasicBlock finalBlock = buildFactory.buildBasicBlock(curFunction);
                    buildFactory.buildBr(curBlock, finalBlock);

                    curTrueBlock = trueBlock;
                    curFalseBlock = finalBlock;
                    curBlock = basicBlock;
                    visitCond(stmtNode.getCondNode());

                    curBlock = finalBlock;
                } else {
                    // basicBlock;
                    // if (...) {
                    //    trueBlock;
                    //    ...
                    //    trueEndBlock;
                    // } else {
                    //    falseBlock;
                    //    ...
                    //    falseEndBlock;
                    // }
                    // finalBlock;
                    BasicBlock basicBlock = curBlock;

                    BasicBlock trueBlock = buildFactory.buildBasicBlock(curFunction);
                    curBlock = trueBlock;
                    visitStmt(stmtNode.getStmtNodes().get(0));
                    BasicBlock trueEndBlock = curBlock;

                    BasicBlock falseBlock = buildFactory.buildBasicBlock(curFunction);
                    curBlock = falseBlock;
                    visitStmt(stmtNode.getStmtNodes().get(1));
                    BasicBlock falseEndBlock = curBlock;

                    curBlock = basicBlock;
                    curTrueBlock = trueBlock;
                    curFalseBlock = falseBlock;
                    visitCond(stmtNode.getCondNode());

                    BasicBlock finalBlock = buildFactory.buildBasicBlock(curFunction);
                    buildFactory.buildBr(trueEndBlock, finalBlock);
                    buildFactory.buildBr(falseEndBlock, finalBlock);
                    curBlock = finalBlock;
                }
                break;
            case While:
                // basicBlock;
                // while (judgeBlock) {
                //    whileBlock;
                // }
                // whileFinalBlock;
                BasicBlock basicBlock = curBlock;
                BasicBlock tmpContinueBlock = continueBlock;
                BasicBlock tmpWhileFinalBlock = curWhileFinalBlock;

                BasicBlock judgeBlock = buildFactory.buildBasicBlock(curFunction);
                buildFactory.buildBr(basicBlock, judgeBlock);

                BasicBlock whileBlock = buildFactory.buildBasicBlock(curFunction);
                curBlock = whileBlock;
                continueBlock = judgeBlock;

                BasicBlock whileFinalBlock = buildFactory.buildBasicBlock(curFunction);
                curWhileFinalBlock = whileFinalBlock;

                visitStmt(stmtNode.getStmtNodes().get(0));
                buildFactory.buildBr(curBlock, judgeBlock);

                continueBlock = tmpContinueBlock;
                curWhileFinalBlock = tmpWhileFinalBlock;

                curTrueBlock = whileBlock;
                curFalseBlock = whileFinalBlock;
                curBlock = judgeBlock;
                visitCond(stmtNode.getCondNode());

                curBlock = whileFinalBlock;
                break;
            case Break:
                buildFactory.buildBr(curBlock, curWhileFinalBlock);
                break;
            case Continue:
                buildFactory.buildBr(curBlock, continueBlock);
                break;
            case Return:
                if (stmtNode.getExpNode() == null) {
                    buildFactory.buildRet(curBlock);
                } else {
                    visitExp(stmtNode.getExpNode());
                    buildFactory.buildRet(curBlock, tmpValue);
                }
                break;
            case LValAssignGetint:
                if (stmtNode.getLValNode().getExpNodes().isEmpty()) {
                    Value input = getValue(stmtNode.getLValNode().getIdent().getContent());
                    tmpValue = buildFactory.buildCall(curBlock, (Function) getValue("getint"), new ArrayList<>());
                    buildFactory.buildStore(curBlock, input, tmpValue);
                } else {
                    List<Value> indexList = new ArrayList<>();
                    for (ExpNode expNode : stmtNode.getLValNode().getExpNodes()) {
                        visitExp(expNode);
                        indexList.add(tmpValue);
                    }
                    tmpValue = getValue(stmtNode.getLValNode().getIdent().getContent());
                    Value addr;
                    Type type = tmpValue.getType(), targetType = ((PointerType) type).getTargetType();
                    if (targetType instanceof PointerType) {
                        // arr[][3]
                        tmpValue = buildFactory.buildLoad(curBlock, tmpValue);
                    } else {
                        // arr[3][2]
                        indexList.add(0, ConstInt.ZERO);
                    }
                    addr = buildFactory.buildGEP(curBlock, tmpValue, indexList);
                    Value input = buildFactory.buildCall(curBlock, (Function) getValue("getint"), new ArrayList<>());
                    tmpValue = buildFactory.buildStore(curBlock, addr, input);
                }
                break;
            case Printf:
                String formatStrings = stmtNode.getFormatString().getContent().replace("\\n", "\n").replace("\"", "");
                List<Value> args = new ArrayList<>();
                for (ExpNode expNode : stmtNode.getExpNodes()) {
                    visitExp(expNode);
                    args.add(tmpValue);
                }
                for (int i = 0; i < formatStrings.length(); i++) {
                    if (formatStrings.charAt(i) == '%') {
                        buildFactory.buildCall(curBlock, (Function) getValue("putint"), new ArrayList<Value>() {{
                            add(args.remove(0));
                        }});
                        i++;
                    } else {
                        if (Config.chToStr) {
                            int j = i;
                            while (j < formatStrings.length() && formatStrings.charAt(j) != '%') {
                                j++;
                            }
                            String str = formatStrings.substring(i, j);
                            if (str.length() == 1) {
                                buildFactory.buildCall(curBlock, (Function) getValue("putch"), new ArrayList<Value>() {{
                                    add(buildFactory.getConstInt(str.charAt(0)));
                                }});
                            } else {
                                Value strAddr = buildFactory.buildGEP(curBlock, getValue(getStringName(str)), new ArrayList<Value>() {{
                                    add(ConstInt.ZERO);
                                    add(ConstInt.ZERO);
                                }});
                                buildFactory.buildCall(curBlock, (Function) getValue("putstr"), new ArrayList<Value>() {{
                                    add(strAddr);
                                }});
                                i = j - 1;
                            }
                        } else {
                            int finalI = i;
                            buildFactory.buildCall(curBlock, (Function) getValue("putch"), new ArrayList<Value>() {{
                                add(buildFactory.getConstInt(formatStrings.charAt(finalI)));
                            }});
                        }
                    }
                }
                break;
            default:
                throw new RuntimeException("Unknown StmtNode type: " + stmtNode.getType());
        }
    }

    private void visitExp(ExpNode expNode) {
        // Exp -> AddExp
        tmpValue = null;
        saveValue = null;
        visitAddExp(expNode.getAddExpNode());
    }

    private void visitCond(CondNode condNode) {
        // Cond -> LOrExp
        visitLOrExp(condNode.getLOrExpNode());
    }

    private void visitLVal(LValNode lValNode) {
        // LVal -> Ident {'[' Exp ']'}
        if (isConst) {
            StringBuilder name = new StringBuilder(lValNode.getIdent().getContent());
            if (!lValNode.getExpNodes().isEmpty()) {
                name.append("0;");
                for (ExpNode expNode : lValNode.getExpNodes()) {
                    visitExp(expNode);
                    name.append(buildFactory.getConstInt(saveValue == null ? 0 : saveValue).getValue()).append(";");
                }
            }
            saveValue = getConst(name.toString());
        } else {
            if (lValNode.getExpNodes().isEmpty()) {
                // is not an array
                Value addr = getValue(lValNode.getIdent().getContent());
                tmpValue = addr;
                Type type = addr.getType();

                if (!(((PointerType) type).getTargetType() instanceof ArrayType)) {
                    tmpValue = buildFactory.buildLoad(curBlock, tmpValue);
                } else {
                    List<Value> indexList = new ArrayList<>();
                    indexList.add(ConstInt.ZERO);
                    indexList.add(ConstInt.ZERO);
                    tmpValue = buildFactory.buildGEP(curBlock, tmpValue, indexList);
                }
            } else {
                // is an array, maybe arr[1][2] or arr[][2]
                List<Value> indexList = new ArrayList<>();
                for (ExpNode expNode : lValNode.getExpNodes()) {
                    visitExp(expNode);
                    indexList.add(tmpValue);
                }
                tmpValue = getValue(lValNode.getIdent().getContent());
                Value addr;
                Type type = tmpValue.getType(), targetType = ((PointerType) type).getTargetType();
                if (targetType instanceof PointerType) {
                    // arr[][3]
                    tmpValue = buildFactory.buildLoad(curBlock, tmpValue);
                } else {
                    // arr[1][2]
                    indexList.add(0, ConstInt.ZERO);
                }
                addr = buildFactory.buildGEP(curBlock, tmpValue, indexList);
                if (((PointerType) addr.getType()).getTargetType() instanceof ArrayType) {
                    List<Value> indexList2 = new ArrayList<>();
                    indexList2.add(ConstInt.ZERO);
                    indexList2.add(ConstInt.ZERO);
                    tmpValue = buildFactory.buildGEP(curBlock, addr, indexList2);
                } else {
                    tmpValue = buildFactory.buildLoad(curBlock, addr);
                }
            }
        }
    }

    private void visitPrimaryExp(PrimaryExpNode primaryExpNode) {
        // PrimaryExp -> '(' Exp ')' | LVal | Number
        if (primaryExpNode.getExpNode() != null) {
            visitExp(primaryExpNode.getExpNode());
        } else if (primaryExpNode.getLValNode() != null) {
            visitLVal(primaryExpNode.getLValNode());
        } else {
            visitNumber(primaryExpNode.getNumberNode());
        }
    }

    private void visitNumber(NumberNode numberNode) {
        // Number -> IntConst
        if (isConst) {
            saveValue = Integer.parseInt(numberNode.getToken().getContent());
        } else {
            tmpValue = buildFactory.getConstInt(Integer.parseInt(numberNode.getToken().getContent()));
        }
    }


    private void visitUnaryExp(UnaryExpNode unaryExpNode) {
        // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        if (unaryExpNode.getPrimaryExpNode() != null) {
            visitPrimaryExp(unaryExpNode.getPrimaryExpNode());
        } else if (unaryExpNode.getIdent() != null) {
            // Ident '(' [FuncRParams] ')'
            tmpList = new ArrayList<>();
            if (unaryExpNode.getFuncRParamsNode() != null) {
                visitFuncRParams(unaryExpNode.getFuncRParamsNode());
            }
            tmpValue = buildFactory.buildCall(curBlock, (Function) getValue(unaryExpNode.getIdent().getContent()), tmpList);
        } else {
            // UnaryOp UnaryExp
            // UnaryOp 直接在这里处理即可
            if (unaryExpNode.getUnaryOpNode().getToken().getType() == TokenType.PLUS) {
                visitUnaryExp(unaryExpNode.getUnaryExpNode());
            } else if (unaryExpNode.getUnaryOpNode().getToken().getType() == TokenType.MINU) {
                visitUnaryExp(unaryExpNode.getUnaryExpNode());
                if (isConst) {
                    saveValue = -saveValue;
                } else {
                    tmpValue = buildFactory.buildBinary(curBlock, Operator.Sub, ConstInt.ZERO, tmpValue);
                }
            } else {
                visitUnaryExp(unaryExpNode.getUnaryExpNode());
                tmpValue = buildFactory.buildNot(curBlock, tmpValue);
            }
        }
    }

    private void visitFuncRParams(FuncRParamsNode funcRParamsNode) {
        // FuncRParams -> Exp { ',' Exp }
        List<Value> args = new ArrayList<>();
        for (ExpNode expNode : funcRParamsNode.getExpNodes()) {
            visitExp(expNode);
            args.add(tmpValue);
        }
        tmpList = args;
    }


    private void visitMulExp(MulExpNode mulExpNode) {
        // UnaryExp | UnaryExp ('*' | '/' | '%') MulExp
        if (isConst) {
            Integer value = saveValue;
            Operator op = saveOp;
            saveValue = null;
            visitUnaryExp(mulExpNode.getUnaryExpNode());
            if (value != null) {
                saveValue = calculate(op, value, saveValue);
            }
            if (mulExpNode.getMulExpNode() != null) {
                switch (mulExpNode.getOperator().getType()) {
                    case MULT:
                        saveOp = Operator.Mul;
                        break;
                    case DIV:
                        saveOp = Operator.Div;
                        break;
                    case MOD:
                        saveOp = Operator.Mod;
                        break;
                    default:
                        throw new RuntimeException("unknown operator");
                }
                visitMulExp(mulExpNode.getMulExpNode());
            }
        } else {
            Value value = tmpValue;
            Operator op = tmpOp;
            tmpValue = null;
            visitUnaryExp(mulExpNode.getUnaryExpNode());
            if (value != null) {
                tmpValue = buildFactory.buildBinary(curBlock, op, value, tmpValue);
            }
            if (mulExpNode.getMulExpNode() != null) {
                if (mulExpNode.getOperator().getType() == TokenType.MULT) {
                    tmpOp = Operator.Mul;
                } else if (mulExpNode.getOperator().getType() == TokenType.DIV) {
                    tmpOp = Operator.Div;
                } else {
                    tmpOp = Operator.Mod;
                }
                visitMulExp(mulExpNode.getMulExpNode());
            }
        }
    }

    private void visitAddExp(AddExpNode addExpNode) {
        // AddExp -> MulExp | MulExp ('+' | '−') AddExp
        if (isConst) {
            Integer value = saveValue;
            Operator op = saveOp;
            saveValue = null;
            visitMulExp(addExpNode.getMulExpNode());
            if (value != null) {
                saveValue = calculate(op, value, saveValue);
            }
            if (addExpNode.getAddExpNode() != null) {
                saveOp = addExpNode.getOperator().getType() == TokenType.PLUS ? Operator.Add : Operator.Sub;
                visitAddExp(addExpNode.getAddExpNode());
            }
        } else {
            Value value = tmpValue;
            Operator op = tmpOp;
            if (Config.addToMul) {
                if (tmpValue == null && addExpNode.getAddExpNode() != null) {
                    // 试图把连加变成乘法
                    if (Objects.equals(addExpNode.getMulExpNode().getStr(), addExpNode.getAddExpNode().getMulExpNode().getStr())) {
                        int times = 1;
                        MulExpNode mulExpNode = addExpNode.getMulExpNode();
                        AddExpNode now = addExpNode;
                        AddExpNode next = addExpNode.getAddExpNode();
                        while (next != null &&
                                next.getMulExpNode() != null &&
                                Objects.equals(mulExpNode.getStr(), next.getMulExpNode().getStr()) &&
                                now.getOperator() != null) {
                            times += now.getOperator().getType() == TokenType.PLUS ? 1 : -1;
                            now = next;
                            next = next.getAddExpNode();
                        }
                        tmpValue = null;
                        visitMulExp(mulExpNode);
                        if (times == 2) {
                            tmpValue = buildFactory.buildBinary(curBlock, Operator.Add, tmpValue, tmpValue);
                        } else if (times == 1) {
                            // do nothing
                        } else if (times == 0) {
                            tmpValue = buildFactory.getConstInt(0);
                        } else if (times == -1) {
                            tmpValue = buildFactory.buildBinary(curBlock, Operator.Sub, ConstInt.ZERO, tmpValue);
                        } else {
                            tmpValue = buildFactory.buildBinary(curBlock, Operator.Mul, tmpValue, buildFactory.getConstInt(times));
                        }
                        tmpOp = now.getOperator() == null ? Operator.Add : now.getOperator().getType() == TokenType.PLUS ? Operator.Add : Operator.Sub;
                        if (next != null) {
                            visitAddExp(next);
                        }
                        return;
                    }
                }
            }
            tmpValue = null;
            visitMulExp(addExpNode.getMulExpNode());
            if (value != null) {
                tmpValue = buildFactory.buildBinary(curBlock, op, value, tmpValue);
            }
            if (addExpNode.getAddExpNode() != null) {
                tmpOp = addExpNode.getOperator().getType() == TokenType.PLUS ? Operator.Add : Operator.Sub;
                visitAddExp(addExpNode.getAddExpNode());
            }
        }

    }

    private void visitRelExp(RelExpNode relExpNode) {
        // RelExp -> AddExp | AddExp ('<' | '>' | '<=' | '>=') RelExp
        Value value = tmpValue;
        Operator op = tmpOp;
        tmpValue = null;
        visitAddExp(relExpNode.getAddExpNode());
        if (value != null) {
            tmpValue = buildFactory.buildBinary(curBlock, op, value, tmpValue);
        }
        if (relExpNode.getRelExpNode() != null) {
            switch (relExpNode.getOperator().getType()) {
                case LSS:
                    tmpOp = Operator.Lt;
                    break;
                case LEQ:
                    tmpOp = Operator.Le;
                    break;
                case GRE:
                    tmpOp = Operator.Gt;
                    break;
                case GEQ:
                    tmpOp = Operator.Ge;
                    break;
                default:
                    throw new RuntimeException("Unknown operator");
            }
            visitRelExp(relExpNode.getRelExpNode());
        }
    }

    private void visitEqExp(EqExpNode eqExpNode) {
        // EqExp -> RelExp | RelExp ('==' | '!=') EqExp
        Value value = tmpValue;
        Operator op = tmpOp;
        tmpValue = null;
        visitRelExp(eqExpNode.getRelExpNode());
        if (value != null) {
            tmpValue = buildFactory.buildBinary(curBlock, op, value, tmpValue);
        }
        if (eqExpNode.getEqExpNode() != null) {
            tmpOp = eqExpNode.getOperator().getType() == TokenType.EQL ? Operator.Eq : Operator.Ne;
            visitEqExp(eqExpNode.getEqExpNode());
        }
    }

    private void visitLAndExp(LAndExpNode lAndExpNode) {
        // LAndExp -> EqExp | EqExp '&&' LAndExp
        BasicBlock trueBlock = curTrueBlock;
        BasicBlock falseBlock = curFalseBlock;
        BasicBlock tmpTrueBlock = curTrueBlock;
        BasicBlock thenBlock = null;
        if (lAndExpNode.getLAndExpNode() != null) {
            thenBlock = buildFactory.buildBasicBlock(curFunction);
            tmpTrueBlock = thenBlock;
        }
        curTrueBlock = tmpTrueBlock;
        tmpValue = null;
        visitEqExp(lAndExpNode.getEqExpNode());
        buildFactory.buildBr(curBlock, tmpValue, curTrueBlock, curFalseBlock);
        curTrueBlock = trueBlock;
        curFalseBlock = falseBlock;
        if (lAndExpNode.getLAndExpNode() != null) {
            curBlock = thenBlock;
            visitLAndExp(lAndExpNode.getLAndExpNode());
        }
    }

    private void visitLOrExp(LOrExpNode lOrExpNode) {
        // LOrExp -> LAndExp | LAndExp '||' LOrExp
        BasicBlock trueBlock = curTrueBlock;
        BasicBlock falseBlock = curFalseBlock;
        BasicBlock tmpFalseBlock = curFalseBlock;
        BasicBlock thenBlock = null;
        if (lOrExpNode.getLOrExpNode() != null) {
            thenBlock = buildFactory.buildBasicBlock(curFunction);
            tmpFalseBlock = thenBlock;
        }
        curFalseBlock = tmpFalseBlock;
        visitLAndExp(lOrExpNode.getLAndExpNode());
        curTrueBlock = trueBlock;
        curFalseBlock = falseBlock;
        if (lOrExpNode.getLOrExpNode() != null) {
            curBlock = thenBlock;
            visitLOrExp(lOrExpNode.getLOrExpNode());
        }
    }

    private void visitConstExp(ConstExpNode constExpNode) {
        // ConstExp -> AddExp
        isConst = true;
        saveValue = null;
        visitAddExp(constExpNode.getAddExpNode());
        isConst = false;
    }
}
