package frontend;

import node.*;
import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private List<Token> tokens;
    private int index = 0;
    private CompUnitNode compUnitNode;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void analyze() {
        this.compUnitNode = CompUnit();
    }

    public static Map<NodeType, String> nodeType = new HashMap<NodeType, String>() {{
        put(NodeType.CompUnit, "<CompUnit>\n");
        put(NodeType.Decl, "<Decl>\n");
        put(NodeType.ConstDecl, "<ConstDecl>\n");
        put(NodeType.BType, "<BType>\n");
        put(NodeType.ConstDef, "<ConstDef>\n");
        put(NodeType.ConstInitVal, "<ConstInitVal>\n");
        put(NodeType.VarDecl, "<VarDecl>\n");
        put(NodeType.VarDef, "<VarDef>\n");
        put(NodeType.InitVal, "<InitVal>\n");
        put(NodeType.FuncDef, "<FuncDef>\n");
        put(NodeType.MainFuncDef, "<MainFuncDef>\n");
        put(NodeType.FuncType, "<FuncType>\n");
        put(NodeType.FuncFParams, "<FuncFParams>\n");
        put(NodeType.FuncFParam, "<FuncFParam>\n");
        put(NodeType.Block, "<Block>\n");
        put(NodeType.BlockItem, "<BlockItem>\n");
        put(NodeType.Stmt, "<Stmt>\n");
        put(NodeType.Exp, "<Exp>\n");
        put(NodeType.Cond, "<Cond>\n");
        put(NodeType.LVal, "<LVal>\n");
        put(NodeType.PrimaryExp, "<PrimaryExp>\n");
        put(NodeType.Number, "<Number>\n");
        put(NodeType.UnaryExp, "<UnaryExp>\n");
        put(NodeType.UnaryOp, "<UnaryOp>\n");
        put(NodeType.FuncRParams, "<FuncRParams>\n");
        put(NodeType.MulExp, "<MulExp>\n");
        put(NodeType.AddExp, "<AddExp>\n");
        put(NodeType.RelExp, "<RelExp>\n");
        put(NodeType.EqExp, "<EqExp>\n");
        put(NodeType.LAndExp, "<LAndExp>\n");
        put(NodeType.LOrExp, "<LOrExp>\n");
        put(NodeType.ConstExp, "<ConstExp>\n");
    }};

    public enum StmtType {
        LValAssignExp, Exp, Block, If, While, Break, Continue, Return, LValAssignGetint, Printf
    }

    private CompUnitNode CompUnit() {
        // CompUnit -> {Decl} {FuncDef} MainFuncDef
        List<DeclNode> declNodes = new ArrayList<>();
        List<FuncDefNode> funcDefNodes = new ArrayList<>();
        MainFuncDefNode mainFuncDefNode;
        while (tokens.get(index + 1).getType() != TokenType.MAINTK && tokens.get(index + 2).getType() != TokenType.LPARENT) {
            DeclNode declNode = Decl();
            declNodes.add(declNode);
        }
        while (tokens.get(index + 1).getType() != TokenType.MAINTK) {
            FuncDefNode funcDefNode = FuncDef();
            funcDefNodes.add(funcDefNode);
        }
        mainFuncDefNode = MainFuncDef();
        return new CompUnitNode(declNodes, funcDefNodes, mainFuncDefNode);
    }

    private DeclNode Decl() {
        // Decl -> ConstDecl | VarDecl
        ConstDeclNode constDeclNode = null;
        VarDeclNode varDeclNode = null;
        if (tokens.get(index).getType() == TokenType.CONSTTK) {
            constDeclNode = ConstDecl();
        } else {
            varDeclNode = VarDecl();
        }
        return new DeclNode(constDeclNode, varDeclNode);
    }

    private ConstDeclNode ConstDecl() {
        // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        match(TokenType.CONSTTK);
        BTypeNode bTypeNode = BType();
        List<ConstDefNode> constDefNodes = new ArrayList<>();
        constDefNodes.add(ConstDef());
        while (tokens.get(index).getType() != TokenType.SEMICN) {
            match(TokenType.COMMA);
            constDefNodes.add(ConstDef());
        }
        match(TokenType.SEMICN);
        return new ConstDeclNode(bTypeNode, constDefNodes);
    }

    private BTypeNode BType() {
        // BType -> 'int'
        Token bTypeToken = match(TokenType.INTTK);
        return new BTypeNode(bTypeToken);
    }

    private ConstDefNode ConstDef() {
        // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        Token ident = match(TokenType.IDENFR);
        List<ConstExpNode> constExpNodes = new ArrayList<>();
        while (tokens.get(index).getType() != TokenType.ASSIGN) {
            match(TokenType.LBRACK);
            constExpNodes.add(ConstExp());
            match(TokenType.RBRACK);
        }
        match(TokenType.ASSIGN);
        ConstInitValNode constInitValNode = ConstInitVal();
        return new ConstDefNode(ident, constExpNodes, constInitValNode);
    }

    private ConstInitValNode ConstInitVal() {
        // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        ConstExpNode constExpNode = null;
        List<ConstInitValNode> constInitValNodes = new ArrayList<>();
        if (tokens.get(index).getType() == TokenType.LBRACE) {
            match(TokenType.LBRACE);
            if (tokens.get(index).getType() != TokenType.RBRACE) {
                constInitValNodes.add(ConstInitVal());
                while (tokens.get(index).getType() != TokenType.RBRACE) {
                    match(TokenType.COMMA);
                    constInitValNodes.add(ConstInitVal());
                }
            }
            match(TokenType.RBRACE);
        } else {
            constExpNode = ConstExp();
        }
        return new ConstInitValNode(constExpNode, constInitValNodes);
    }

    private VarDeclNode VarDecl() {
        // VarDecl -> BType VarDef { ',' VarDef } ';'
        BTypeNode bTypeNode = BType();
        List<VarDefNode> varDefNodes = new ArrayList<>();
        varDefNodes.add(VarDef());
        while (tokens.get(index).getType() != TokenType.SEMICN) {
            match(TokenType.COMMA);
            varDefNodes.add(VarDef());
        }
        match(TokenType.SEMICN);
        return new VarDeclNode(bTypeNode, varDefNodes);
    }

    private VarDefNode VarDef() {
        // VarDef -> Ident { '[' ConstExp ']' } [ '=' InitVal ]
        Token ident = match(TokenType.IDENFR);
        List<ConstExpNode> constExpNodes = new ArrayList<>();
        InitValNode initValNode = null;
        while (tokens.get(index).getType() == TokenType.LBRACK) {
            match(TokenType.LBRACK);
            constExpNodes.add(ConstExp());
            match(TokenType.RBRACK);
        }
        if (tokens.get(index).getType() == TokenType.ASSIGN) {
            match(TokenType.ASSIGN);
            initValNode = InitVal();
        }
        return new VarDefNode(ident, constExpNodes, initValNode);
    }

    private InitValNode InitVal() {
        // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
        ExpNode expNode = null;
        List<InitValNode> initValNodes = new ArrayList<>();
        if (tokens.get(index).getType() == TokenType.LBRACE) {
            match(TokenType.LBRACE);
            initValNodes.add(InitVal());
            while (tokens.get(index).getType() != TokenType.RBRACE) {
                match(TokenType.COMMA);
                initValNodes.add(InitVal());
            }
            match(TokenType.RBRACE);
        } else {
            expNode = Exp();
        }
        return new InitValNode(expNode, initValNodes);
    }

    private FuncDefNode FuncDef() {
        // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        FuncTypeNode funcTypeNode = FuncType();
        Token ident = match(TokenType.IDENFR);
        FuncFParamsNode funcParamsNode = null;
        match(TokenType.LPARENT);
        if (tokens.get(index).getType() != TokenType.RPARENT) {
            funcParamsNode = FuncFParams();
        }
        match(TokenType.RPARENT);
        BlockNode blockNode = Block();
        return new FuncDefNode(funcTypeNode, ident, funcParamsNode, blockNode);
    }

    private MainFuncDefNode MainFuncDef() {
        // MainFuncDef -> 'int' 'main' '(' ')' Block
        match(TokenType.INTTK);
        match(TokenType.MAINTK);
        match(TokenType.LPARENT);
        match(TokenType.RPARENT);
        BlockNode blockNode = Block();
        return new MainFuncDefNode(blockNode);
    }

    private FuncTypeNode FuncType() {
        // FuncType -> 'void' | 'int'
        if (tokens.get(index).getType() == TokenType.VOIDTK) {
            Token voidToken = match(TokenType.VOIDTK);
            return new FuncTypeNode(voidToken);
        } else {
            Token intToken = match(TokenType.INTTK);
            return new FuncTypeNode(intToken);
        }
    }

    private FuncFParamsNode FuncFParams() {
        // FuncFParams -> FuncFParam { ',' FuncFParam }
        List<FuncFParamNode> funcFParamNodes = new ArrayList<>();
        funcFParamNodes.add(FuncFParam());
        while (tokens.get(index).getType() == TokenType.COMMA) {
            match(TokenType.COMMA);
            funcFParamNodes.add(FuncFParam());
        }
        return new FuncFParamsNode(funcFParamNodes);
    }

    private FuncFParamNode FuncFParam() {
        // FuncFParam -> BType Ident [ '[' ']' { '[' ConstExp ']' }]
        BTypeNode bTypeNode = BType();
        Token ident = match(TokenType.IDENFR);
        Token lbrack = null;
        List<ConstExpNode> constExpNodes = new ArrayList<>();
        if (tokens.get(index).getType() == TokenType.LBRACK) {
            lbrack = match(TokenType.LBRACK);
            match(TokenType.RBRACK);
            while (tokens.get(index).getType() == TokenType.LBRACK) {
                match(TokenType.LBRACK);
                constExpNodes.add(ConstExp());
                match(TokenType.RBRACK);
            }
        }
        return new FuncFParamNode(bTypeNode, ident, lbrack, constExpNodes);
    }

    private BlockNode Block() {
        // Block -> '{' { BlockItem } '}'
        List<BlockItemNode> blockItemNodes = new ArrayList<>();
        match(TokenType.LBRACE);
        if (tokens.get(index).getType() == TokenType.RBRACE) {
            match(TokenType.RBRACE);
        } else {
            while (tokens.get(index).getType() != TokenType.RBRACE) {
                blockItemNodes.add(BlockItem());
            }
            match(TokenType.RBRACE);
        }
        return new BlockNode(blockItemNodes);
    }

    private BlockItemNode BlockItem() {
        // BlockItem -> Decl | Stmt
        DeclNode declNode = null;
        StmtNode stmtNode = null;
        if (tokens.get(index).getType() == TokenType.CONSTTK || tokens.get(index).getType() == TokenType.INTTK) {
            declNode = Decl();
        } else {
            stmtNode = Stmt();
        }
        return new BlockItemNode(declNode, stmtNode);
    }

    private StmtNode Stmt() {
        // Stmt -> LVal '=' Exp ';'
        //	| [Exp] ';'
        //	| Block
        //	| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        //	| 'while' '(' Cond ')' Stmt
        //	| 'break' ';' | 'continue' ';'
        //	| 'return' [Exp] ';'
        //	| LVal '=' 'getint' '(' ')' ';'
        //	| 'printf' '(' FormatString { ',' Exp } ')' ';'
        StmtType stmtType = null;
        LValNode lValNode = null;
        ExpNode expNode = null;
        BlockNode blockNode = null;
        CondNode condNode = null;
        List<StmtNode> stmtNodes = new ArrayList<>();
        Token formatString = null;
        List<ExpNode> expNodes = new ArrayList<>();
        if (tokens.get(index).getType() == TokenType.LBRACE) {
            // Block
            stmtType = StmtType.Block;
            blockNode = Block();
        } else if (tokens.get(index).getType() == TokenType.PRINTFTK) {
            // 'printf' '(' FormatString { ',' Exp } ')' ';'
            stmtType = StmtType.Printf;
            match(TokenType.PRINTFTK);
            match(TokenType.LPARENT);
            formatString = match(TokenType.STRCON);
            while (tokens.get(index).getType() == TokenType.COMMA) {
                match(TokenType.COMMA);
                expNodes.add(Exp());
            }
            match(TokenType.RPARENT);
            match(TokenType.SEMICN);
        } else if (tokens.get(index).getType() == TokenType.IFTK) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            stmtType = StmtType.If;
            match(TokenType.IFTK);
            match(TokenType.LPARENT);
            condNode = Cond();
            match(TokenType.RPARENT);
            stmtNodes.add(Stmt());
            if (tokens.get(index).getType() == TokenType.ELSETK) {
                match(TokenType.ELSETK);
                stmtNodes.add(Stmt());
            }
        } else if (tokens.get(index).getType() == TokenType.WHILETK) {
            // 'while' '(' Cond ')' Stmt
            stmtType = StmtType.While;
            match(TokenType.WHILETK);
            match(TokenType.LPARENT);
            condNode = Cond();
            match(TokenType.RPARENT);
            stmtNodes.add(Stmt());
        } else if (tokens.get(index).getType() == TokenType.BREAKTK) {
            // 'break' ';'
            stmtType = StmtType.Break;
            match(TokenType.BREAKTK);
            match(TokenType.SEMICN);
        } else if (tokens.get(index).getType() == TokenType.CONTINUETK) {
            // 'continue' ';'
            stmtType = StmtType.Continue;
            match(TokenType.CONTINUETK);
            match(TokenType.SEMICN);
        } else if (tokens.get(index).getType() == TokenType.RETURNTK) {
            // 'return' [Exp] ';'
            stmtType = StmtType.Return;
            match(TokenType.RETURNTK);
            if (tokens.get(index).getType() != TokenType.SEMICN) {
                expNode = Exp();
            }
            match(TokenType.SEMICN);
        } else {
            int assign = index, semicn = index;
            for (int i = index; i < tokens.size(); i++) {
                if (tokens.get(i).getType() == TokenType.ASSIGN) {
                    assign = i;
                }
                if (tokens.get(i).getType() == TokenType.SEMICN) {
                    semicn = i;
                    break;
                }
            }
            if (assign < semicn && assign > index) {
                // LVal '=' Exp ';'
                // LVal '=' 'getint' '(' ')' ';'
                lValNode = LVal();
                match(TokenType.ASSIGN);
                if (tokens.get(index).getType() == TokenType.GETINTTK) {
                    stmtType = StmtType.LValAssignGetint;
                    match(TokenType.GETINTTK);
                    match(TokenType.LPARENT);
                    match(TokenType.RPARENT);
                } else {
                    stmtType = StmtType.LValAssignExp;
                    expNode = Exp();
                }
                match(TokenType.SEMICN);
            } else {
                // [Exp] ';'
                stmtType = StmtType.Exp;
                if (tokens.get(index).getType() != TokenType.SEMICN) {
                    expNode = Exp();
                }
                match(TokenType.SEMICN);
            }
        }
        return new StmtNode(stmtType, lValNode, expNode, blockNode, condNode, stmtNodes, formatString, expNodes);
    }

    private ExpNode Exp() {
        // Exp -> AddExp
        return new ExpNode(AddExp());
    }

    private CondNode Cond() {
        // Cond -> LOrExp
        return new CondNode(LOrExp());
    }

    private LValNode LVal() {
        // LVal -> Ident {'[' Exp ']'}
        Token ident = match(TokenType.IDENFR);
        List<ExpNode> expNodes = new ArrayList<>();
        while (tokens.get(index).getType() == TokenType.LBRACK) {
            match(TokenType.LBRACK);
            expNodes.add(Exp());
            match(TokenType.RBRACK);
        }
        return new LValNode(ident, expNodes);
    }

    private PrimaryExpNode PrimaryExp() {
        // PrimaryExp -> '(' Exp ')' | LVal | Number
        ExpNode expNode = null;
        LValNode lValNode = null;
        NumberNode numberNode = null;
        if (tokens.get(index).getType() == TokenType.LPARENT) {
            match(TokenType.LPARENT);
            expNode = Exp();
            match(TokenType.RPARENT);
        } else if (tokens.get(index).getType() == TokenType.INTCON) {
            numberNode = Number();
        } else {
            lValNode = LVal();
        }
        return new PrimaryExpNode(expNode, lValNode, numberNode);
    }

    private NumberNode Number() {
        // Number -> IntConst
        return new NumberNode(match(TokenType.INTCON));
    }

    private UnaryExpNode UnaryExp() {
        // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        PrimaryExpNode primaryExpNode = null;
        Token ident = null;
        FuncRParamsNode funcRParamsNode = null;
        UnaryOpNode unaryOpNode = null;
        UnaryExpNode unaryExpNode = null;
        if (tokens.get(index).getType() == TokenType.IDENFR && tokens.get(index + 1).getType() == TokenType.LPARENT) {
            ident = match(TokenType.IDENFR);
            match(TokenType.LPARENT);
            if (tokens.get(index).getType() != TokenType.RPARENT) {
                funcRParamsNode = FuncRParams();
            }
            match(TokenType.RPARENT);
        } else if (tokens.get(index).getType() == TokenType.PLUS || tokens.get(index).getType() == TokenType.MINU || tokens.get(index).getType() == TokenType.NOT) {
            unaryOpNode = UnaryOp();
            unaryExpNode = UnaryExp();
        } else {
            primaryExpNode = PrimaryExp();
        }
        return new UnaryExpNode(primaryExpNode, ident, funcRParamsNode, unaryOpNode, unaryExpNode);
    }

    private UnaryOpNode UnaryOp() {
        // UnaryOp -> '+' | '−' | '!'
        Token token = null;
        if (tokens.get(index).getType() == TokenType.PLUS) {
            token = match(TokenType.PLUS);
        } else if (tokens.get(index).getType() == TokenType.MINU) {
            token = match(TokenType.MINU);
        } else {
            token = match(TokenType.NOT);
        }
        return new UnaryOpNode(token);
    }

    private FuncRParamsNode FuncRParams() {
        // FuncRParams -> Exp { ',' Exp }
        List<ExpNode> expNodes = new ArrayList<>();
        expNodes.add(Exp());
        while (tokens.get(index).getType() == TokenType.COMMA) {
            match(TokenType.COMMA);
            expNodes.add(Exp());
        }
        return new FuncRParamsNode(expNodes);
    }

    private MulExpNode MulExp() {
        // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
        List<UnaryExpNode> unaryExpNodes = new ArrayList<>();
        List<Token> operations = new ArrayList<>();
        unaryExpNodes.add(UnaryExp());
        while (tokens.get(index).getType() == TokenType.MULT || tokens.get(index).getType() == TokenType.DIV || tokens.get(index).getType() == TokenType.MOD) {
            if (tokens.get(index).getType() == TokenType.MULT) {
                operations.add(match(TokenType.MULT));
            } else if (tokens.get(index).getType() == TokenType.DIV) {
                operations.add(match(TokenType.DIV));
            } else {
                operations.add(match(TokenType.MOD));
            }
            unaryExpNodes.add(UnaryExp());
        }
        return new MulExpNode(unaryExpNodes, operations);
    }

    private AddExpNode AddExp() {
        // AddExp -> MulExp { ('+' | '−') MulExp }
        List<MulExpNode> mulExpNodes = new ArrayList<>();
        List<Token> operations = new ArrayList<>();
        mulExpNodes.add(MulExp());
        while (tokens.get(index).getType() == TokenType.PLUS || tokens.get(index).getType() == TokenType.MINU) {
            if (tokens.get(index).getType() == TokenType.PLUS) {
                operations.add(match(TokenType.PLUS));
            } else if (tokens.get(index).getType() == TokenType.MINU) {
                operations.add(match(TokenType.MINU));
            }
            mulExpNodes.add(MulExp());
        }
        return new AddExpNode(mulExpNodes, operations);
    }

    private RelExpNode RelExp() {
        // RelExp -> AddExp { ('<' | '>' | '<=' | '>=') AddExp }
        List<AddExpNode> addExpNodes = new ArrayList<>();
        List<Token> operations = new ArrayList<>();
        addExpNodes.add(AddExp());
        while (tokens.get(index).getType() == TokenType.LSS || tokens.get(index).getType() == TokenType.LEQ || tokens.get(index).getType() == TokenType.GRE || tokens.get(index).getType() == TokenType.GEQ) {
            if (tokens.get(index).getType() == TokenType.LSS) {
                operations.add(match(TokenType.LSS));
            } else if (tokens.get(index).getType() == TokenType.LEQ) {
                operations.add(match(TokenType.LEQ));
            } else if (tokens.get(index).getType() == TokenType.GRE) {
                operations.add(match(TokenType.GRE));
            } else {
                operations.add(match(TokenType.GEQ));
            }
            addExpNodes.add(AddExp());
        }
        return new RelExpNode(addExpNodes, operations);
    }

    private EqExpNode EqExp() {
        // EqExp -> RelExp { ('==' | '!=') RelExp }
        List<RelExpNode> relExpNodes = new ArrayList<>();
        List<Token> operations = new ArrayList<>();
        relExpNodes.add(RelExp());
        while (tokens.get(index).getType() == TokenType.EQL || tokens.get(index).getType() == TokenType.NEQ) {
            if (tokens.get(index).getType() == TokenType.EQL) {
                operations.add(match(TokenType.EQL));
            } else {
                operations.add(match(TokenType.NEQ));
            }
            relExpNodes.add(RelExp());
        }
        return new EqExpNode(relExpNodes, operations);
    }

    private LAndExpNode LAndExp() {
        // LAndExp -> EqExp { '&&' EqExp }
        List<EqExpNode> eqExpNodes = new ArrayList<>();
        eqExpNodes.add(EqExp());
        while (tokens.get(index).getType() == TokenType.AND) {
            match(TokenType.AND);
            eqExpNodes.add(EqExp());
        }
        return new LAndExpNode(eqExpNodes);
    }

    private LOrExpNode LOrExp() {
        // LOrExp -> LAndExp { '||' LAndExp }
        List<LAndExpNode> lAndExpNodes = new ArrayList<>();
        lAndExpNodes.add(LAndExp());
        while (tokens.get(index).getType() == TokenType.OR) {
            match(TokenType.OR);
            lAndExpNodes.add(LAndExp());
        }
        return new LOrExpNode(lAndExpNodes);
    }

    private ConstExpNode ConstExp() {
        // ConstExp -> AddExp
        return new ConstExpNode(AddExp());
    }

    private Token match(TokenType tokenType) {
        if (tokens.get(index).getType() == tokenType) {
            return tokens.get(index++);
        } else {
            throw new RuntimeException("Syntax error: " + tokens.get(index).toString() + " at line " + tokens.get(index).getLineNumber());
        }
    }

    public void printParseAns() {
        compUnitNode.print();
    }
}
