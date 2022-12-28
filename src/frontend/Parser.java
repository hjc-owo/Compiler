package frontend;

import error.Error;
import error.ErrorHandler;
import error.ErrorType;
import node.*;
import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private static final Parser instance = new Parser();

    public static Parser getInstance() {
        return instance;
    }

    private List<Token> tokens;
    private int index = 0;
    private Token now;
    private CompUnitNode compUnitNode;

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
        now = tokens.get(index);
    }

    public CompUnitNode getCompUnitNode() {
        return compUnitNode;
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
        if (now.getType() == TokenType.CONSTTK) {
            constDeclNode = ConstDecl();
        } else {
            varDeclNode = VarDecl();
        }
        return new DeclNode(constDeclNode, varDeclNode);
    }

    private ConstDeclNode ConstDecl() {
        // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        Token constToken = match(TokenType.CONSTTK);
        BTypeNode bTypeNode = BType();
        List<ConstDefNode> constDefNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        Token semicnToken;
        constDefNodes.add(ConstDef());
        while (now.getType() == TokenType.COMMA) {
            commas.add(match(TokenType.COMMA));
            constDefNodes.add(ConstDef());
        }
        semicnToken = match(TokenType.SEMICN);
        return new ConstDeclNode(constToken, bTypeNode, constDefNodes, commas, semicnToken);
    }

    private BTypeNode BType() {
        // BType -> 'int'
        Token bTypeToken = match(TokenType.INTTK);
        return new BTypeNode(bTypeToken);
    }

    private ConstDefNode ConstDef() {
        // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        Token ident = match(TokenType.IDENFR);
        List<Token> leftBrackets = new ArrayList<>();
        List<ConstExpNode> constExpNodes = new ArrayList<>();
        List<Token> rightBrackets = new ArrayList<>();
        Token equalToken;
        while (now.getType() == TokenType.LBRACK) {
            leftBrackets.add(match(TokenType.LBRACK));
            constExpNodes.add(ConstExp());
            rightBrackets.add(match(TokenType.RBRACK));
        }
        equalToken = match(TokenType.ASSIGN);
        ConstInitValNode constInitValNode = ConstInitVal();
        return new ConstDefNode(ident, leftBrackets, constExpNodes, rightBrackets, equalToken, constInitValNode);
    }

    private ConstInitValNode ConstInitVal() {
        // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        ConstExpNode constExpNode = null;
        Token leftBraceToken = null;
        List<ConstInitValNode> constInitValNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        Token rightBraceToken = null;
        if (now.getType() == TokenType.LBRACE) {
            leftBraceToken = match(TokenType.LBRACE);
            if (now.getType() != TokenType.RBRACE) {
                constInitValNodes.add(ConstInitVal());
                while (now.getType() != TokenType.RBRACE) {
                    commas.add(match(TokenType.COMMA));
                    constInitValNodes.add(ConstInitVal());
                }
            }
            rightBraceToken = match(TokenType.RBRACE);
        } else {
            constExpNode = ConstExp();
        }
        return new ConstInitValNode(constExpNode, leftBraceToken, constInitValNodes, commas, rightBraceToken);
    }

    private VarDeclNode VarDecl() {
        // VarDecl -> BType VarDef { ',' VarDef } ';'
        BTypeNode bTypeNode = BType();
        List<VarDefNode> varDefNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        Token semicnToken;
        varDefNodes.add(VarDef());
        while (now.getType() == TokenType.COMMA) {
            commas.add(match(TokenType.COMMA));
            varDefNodes.add(VarDef());
        }
        semicnToken = match(TokenType.SEMICN);
        return new VarDeclNode(bTypeNode, varDefNodes, commas, semicnToken);
    }

    private VarDefNode VarDef() {
        // VarDef -> Ident ({ '[' ConstExp ']' } [ '=' InitVal ] | '=' 'getint' '(' ')')
        Token ident = match(TokenType.IDENFR);
        List<Token> leftBrackets = new ArrayList<>();
        List<ConstExpNode> constExpNodes = new ArrayList<>();
        List<Token> rightBrackets = new ArrayList<>();
        Token equalToken = null;
        InitValNode initValNode = null;
        while (now.getType() == TokenType.LBRACK) {
            leftBrackets.add(match(TokenType.LBRACK));
            constExpNodes.add(ConstExp());
            rightBrackets.add(match(TokenType.RBRACK));
        }
        if (now.getType() == TokenType.ASSIGN) {
            equalToken = match(TokenType.ASSIGN);
            if (now.getType() == TokenType.GETINTTK) {
                Token getIntToken = match(TokenType.GETINTTK);
                Token leftParenToken = match(TokenType.LPARENT);
                Token rightParenToken = match(TokenType.RPARENT);
                return new VarDefNode(ident);
            } else {
                initValNode = InitVal();
            }
        }
        return new VarDefNode(ident, leftBrackets, constExpNodes, rightBrackets, equalToken, initValNode);
    }

    private InitValNode InitVal() {
        // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
        ExpNode expNode = null;
        Token leftBraceToken = null;
        List<InitValNode> initValNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        Token rightBraceToken = null;
        if (now.getType() == TokenType.LBRACE) {
            leftBraceToken = match(TokenType.LBRACE);
            if (now.getType() != TokenType.RBRACE) {
                initValNodes.add(InitVal());
                while (now.getType() != TokenType.RBRACE) {
                    commas.add(match(TokenType.COMMA));
                    initValNodes.add(InitVal());
                }
            }
            rightBraceToken = match(TokenType.RBRACE);
        } else {
            expNode = Exp();
        }
        return new InitValNode(expNode, leftBraceToken, initValNodes, commas, rightBraceToken);
    }

    private FuncDefNode FuncDef() {
        // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        FuncTypeNode funcTypeNode = FuncType();
        Token ident = match(TokenType.IDENFR);
        Token leftParentToken = match(TokenType.LPARENT);
        FuncFParamsNode funcParamsNode = null;
        if (now.getType() == TokenType.INTTK) {
            funcParamsNode = FuncFParams();
        }
        Token rightParentToken = match(TokenType.RPARENT);
        BlockNode blockNode = Block();
        return new FuncDefNode(funcTypeNode, ident, leftParentToken, funcParamsNode, rightParentToken, blockNode);
    }

    private MainFuncDefNode MainFuncDef() {
        // MainFuncDef -> 'int' 'main' '(' ')' Block
        Token intToken = match(TokenType.INTTK);
        Token mainToken = match(TokenType.MAINTK);
        Token leftParentToken = match(TokenType.LPARENT);
        Token rightParentToken = match(TokenType.RPARENT);
        BlockNode blockNode = Block();
        return new MainFuncDefNode(intToken, mainToken, leftParentToken, rightParentToken, blockNode);
    }

    private FuncTypeNode FuncType() {
        // FuncType -> 'void' | 'int'
        if (now.getType() == TokenType.VOIDTK) {
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
        List<Token> commas = new ArrayList<>();
        funcFParamNodes.add(FuncFParam());
        while (now.getType() == TokenType.COMMA) {
            commas.add(match(TokenType.COMMA));
            funcFParamNodes.add(FuncFParam());
        }
        return new FuncFParamsNode(funcFParamNodes, commas);
    }

    private FuncFParamNode FuncFParam() {
        // FuncFParam -> BType Ident [ '[' ']' { '[' ConstExp ']' }]
        BTypeNode bTypeNode = BType();
        Token ident = match(TokenType.IDENFR);
        List<Token> leftBrackets = new ArrayList<>();
        List<Token> rightBrackets = new ArrayList<>();
        List<ConstExpNode> constExpNodes = new ArrayList<>();
        if (now.getType() == TokenType.LBRACK) {
            leftBrackets.add(match(TokenType.LBRACK));
            rightBrackets.add(match(TokenType.RBRACK));
            while (now.getType() == TokenType.LBRACK) {
                leftBrackets.add(match(TokenType.LBRACK));
                constExpNodes.add(ConstExp());
                rightBrackets.add(match(TokenType.RBRACK));
            }
        }
        return new FuncFParamNode(bTypeNode, ident, leftBrackets, rightBrackets, constExpNodes);
    }

    private BlockNode Block() {
        // Block -> '{' { BlockItem } '}'
        Token leftBraceToken = match(TokenType.LBRACE);
        List<BlockItemNode> blockItemNodes = new ArrayList<>();
        while (now.getType() != TokenType.RBRACE) {
            blockItemNodes.add(BlockItem());
        }
        Token rightBraceToken = match(TokenType.RBRACE);
        return new BlockNode(leftBraceToken, blockItemNodes, rightBraceToken);
    }

    private BlockItemNode BlockItem() {
        // BlockItem -> Decl | Stmt
        DeclNode declNode = null;
        StmtNode stmtNode = null;
        if (now.getType() == TokenType.CONSTTK || now.getType() == TokenType.INTTK) {
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
        if (now.getType() == TokenType.LBRACE) {
            // Block
            BlockNode blockNode = Block();
            return new StmtNode(StmtNode.StmtType.Block, blockNode);
        } else if (now.getType() == TokenType.PRINTFTK) {
            // 'printf' '(' FormatString { ',' Exp } ')' ';'
            Token printfToken = match(TokenType.PRINTFTK);
            Token leftParentToken = match(TokenType.LPARENT);
            Token formatString = match(TokenType.STRCON);
            List<Token> commas = new ArrayList<>();
            List<ExpNode> expNodes = new ArrayList<>();
            while (now.getType() == TokenType.COMMA) {
                commas.add(match(TokenType.COMMA));
                expNodes.add(Exp());
            }
            Token rightParentToken = match(TokenType.RPARENT);
            Token semicnToken = match(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.Printf, printfToken, leftParentToken, formatString, commas, expNodes, rightParentToken, semicnToken);
        } else if (now.getType() == TokenType.IFTK) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            Token ifToken = match(TokenType.IFTK);
            Token leftParentToken = match(TokenType.LPARENT);
            CondNode condNode = Cond();
            Token rightParentToken = match(TokenType.RPARENT);
            List<StmtNode> stmtNodes = new ArrayList<>();
            stmtNodes.add(Stmt());
            Token elseToken = null;
            if (now.getType() == TokenType.ELSETK) {
                elseToken = match(TokenType.ELSETK);
                stmtNodes.add(Stmt());
            }
            return new StmtNode(StmtNode.StmtType.If, ifToken, leftParentToken, condNode, rightParentToken, stmtNodes, elseToken);
        } else if (now.getType() == TokenType.WHILETK) {
            // 'while' '(' Cond ')' Stmt
            Token whileToken = match(TokenType.WHILETK);
            Token leftParentToken = match(TokenType.LPARENT);
            CondNode condNode = Cond();
            Token rightParentToken = match(TokenType.RPARENT);
            List<StmtNode> stmtNodes = new ArrayList<>();
            stmtNodes.add(Stmt());
            return new StmtNode(StmtNode.StmtType.While, whileToken, leftParentToken, condNode, rightParentToken, stmtNodes);
        } else if (now.getType() == TokenType.BREAKTK) {
            // 'break' ';'
            Token breakToken = match(TokenType.BREAKTK);
            Token semicnToken = match(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.Break, breakToken, semicnToken);
        } else if (now.getType() == TokenType.CONTINUETK) {
            // 'continue' ';'
            Token continueToken = match(TokenType.CONTINUETK);
            Token semicnToken = match(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.Continue, continueToken, semicnToken);
        } else if (now.getType() == TokenType.RETURNTK) {
            // 'return' [Exp] ';'
            Token returnToken = match(TokenType.RETURNTK);
            ExpNode expNode = null;
            if (isExp()) {
                expNode = Exp();
            }
            Token semicnToken = match(TokenType.SEMICN);
            return new StmtNode(StmtNode.StmtType.Return, returnToken, expNode, semicnToken);
        } else {
            int assign = index;
            for (int i = index; i < tokens.size() && tokens.get(i).getLineNumber() == now.getLineNumber(); i++) {
                if (tokens.get(i).getType() == TokenType.ASSIGN) {
                    assign = i;
                }
            }
            if (assign > index) {
                // LVal '=' Exp ';'
                // LVal '=' 'getint' '(' ')' ';'
                LValNode lValNode = LVal();
                Token assignToken = match(TokenType.ASSIGN);
                if (now.getType() == TokenType.GETINTTK) {
                    Token getintToken = match(TokenType.GETINTTK);
                    Token leftParentToken = match(TokenType.LPARENT);
                    Token rightParentToken = match(TokenType.RPARENT);
                    Token semicnToken = match(TokenType.SEMICN);
                    return new StmtNode(StmtNode.StmtType.LValAssignGetint, lValNode, assignToken, getintToken, leftParentToken, rightParentToken, semicnToken);
                } else {
                    ExpNode expNode = Exp();
                    Token semicnToken = match(TokenType.SEMICN);
                    return new StmtNode(StmtNode.StmtType.LValAssignExp, lValNode, assignToken, expNode, semicnToken);
                }
            } else {
                // [Exp] ';'
                ExpNode expNode = null;
                if (isExp()) {
                    expNode = Exp();
                }
                Token semicnToken = match(TokenType.SEMICN);
                return new StmtNode(StmtNode.StmtType.Exp, expNode, semicnToken);
            }
        }
    }

    private boolean isExp() {
        return now.getType() == TokenType.IDENFR ||
                now.getType() == TokenType.PLUS ||
                now.getType() == TokenType.MINU ||
                now.getType() == TokenType.NOT ||
                now.getType() == TokenType.LPARENT ||
                now.getType() == TokenType.INTCON;
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
        List<Token> leftBrackets = new ArrayList<>();
        List<ExpNode> expNodes = new ArrayList<>();
        List<Token> rightBrackets = new ArrayList<>();
        while (now.getType() == TokenType.LBRACK) {
            leftBrackets.add(match(TokenType.LBRACK));
            expNodes.add(Exp());
            rightBrackets.add(match(TokenType.RBRACK));
        }
        return new LValNode(ident, leftBrackets, expNodes, rightBrackets);
    }

    private PrimaryExpNode PrimaryExp() {
        // PrimaryExp -> '(' Exp ')' | LVal | Number
        if (now.getType() == TokenType.LPARENT) {
            Token leftParentToken = match(TokenType.LPARENT);
            ExpNode expNode = Exp();
            Token rightParentToken = match(TokenType.RPARENT);
            return new PrimaryExpNode(leftParentToken, expNode, rightParentToken);
        } else if (now.getType() == TokenType.INTCON) {
            NumberNode numberNode = Number();
            return new PrimaryExpNode(numberNode);
        } else {
            LValNode lValNode = LVal();
            return new PrimaryExpNode(lValNode);
        }
    }

    private NumberNode Number() {
        // Number -> IntConst
        return new NumberNode(match(TokenType.INTCON));
    }

    private UnaryExpNode UnaryExp() {
        // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        if (now.getType() == TokenType.IDENFR && tokens.get(index + 1).getType() == TokenType.LPARENT) {
            Token ident = match(TokenType.IDENFR);
            Token leftParentToken = match(TokenType.LPARENT);
            FuncRParamsNode funcRParamsNode = null;
            if (isExp()) {
                funcRParamsNode = FuncRParams();
            }
            Token rightParentToken = match(TokenType.RPARENT);
            return new UnaryExpNode(ident, leftParentToken, funcRParamsNode, rightParentToken);
        } else if (now.getType() == TokenType.PLUS || now.getType() == TokenType.MINU || now.getType() == TokenType.NOT) {
            UnaryOpNode unaryOpNode = UnaryOp();
            UnaryExpNode unaryExpNode = UnaryExp();
            return new UnaryExpNode(unaryOpNode, unaryExpNode);
        } else {
            PrimaryExpNode primaryExpNode = PrimaryExp();
            return new UnaryExpNode(primaryExpNode);
        }
    }

    private UnaryOpNode UnaryOp() {
        // UnaryOp -> '+' | '−' | '!'
        Token token;
        if (now.getType() == TokenType.PLUS) {
            token = match(TokenType.PLUS);
        } else if (now.getType() == TokenType.MINU) {
            token = match(TokenType.MINU);
        } else {
            token = match(TokenType.NOT);
        }
        return new UnaryOpNode(token);
    }

    private FuncRParamsNode FuncRParams() {
        // FuncRParams -> Exp { ',' Exp }
        List<ExpNode> expNodes = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        expNodes.add(Exp());
        while (now.getType() == TokenType.COMMA) {
            commas.add(match(TokenType.COMMA));
            expNodes.add(Exp());
        }
        return new FuncRParamsNode(expNodes, commas);
    }

    private MulExpNode MulExp() {
        // MulExp -> UnaryExp | MulExp ('*' | '/' | '%' | 'bitand' ) UnaryExp
        UnaryExpNode unaryExpNode = UnaryExp();
        Token operator = null;
        MulExpNode mulExpNode = null;
        if (now.getType() == TokenType.MULT) {
            operator = match(TokenType.MULT);
            mulExpNode = MulExp();
        } else if (now.getType() == TokenType.DIV) {
            operator = match(TokenType.DIV);
            mulExpNode = MulExp();
        } else if (now.getType() == TokenType.MOD) {
            operator = match(TokenType.MOD);
            mulExpNode = MulExp();
        } else if (now.getType() == TokenType.BITAND) {
            operator = match(TokenType.BITAND);
            mulExpNode = MulExp();
        }
        return new MulExpNode(unaryExpNode, operator, mulExpNode);
    }

    private AddExpNode AddExp() {
        // AddExp -> MulExp | AddExp ('+' | '−') MulExp
        MulExpNode mulExpNode = MulExp();
        Token operator = null;
        AddExpNode addExpNode = null;
        if (now.getType() == TokenType.PLUS) {
            operator = match(TokenType.PLUS);
            addExpNode = AddExp();
        } else if (now.getType() == TokenType.MINU) {
            operator = match(TokenType.MINU);
            addExpNode = AddExp();
        }
        return new AddExpNode(mulExpNode, operator, addExpNode);
    }

    private RelExpNode RelExp() {
        // RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        AddExpNode addExpNode = AddExp();
        Token operator = null;
        RelExpNode relExpNode = null;
        if (now.getType() == TokenType.LSS) {
            operator = match(TokenType.LSS);
            relExpNode = RelExp();
        } else if (now.getType() == TokenType.GRE) {
            operator = match(TokenType.GRE);
            relExpNode = RelExp();
        } else if (now.getType() == TokenType.LEQ) {
            operator = match(TokenType.LEQ);
            relExpNode = RelExp();
        } else if (now.getType() == TokenType.GEQ) {
            operator = match(TokenType.GEQ);
            relExpNode = RelExp();
        }
        return new RelExpNode(addExpNode, operator, relExpNode);
    }

    private EqExpNode EqExp() {
        // EqExp -> RelExp | EqExp ('==' | '!=') RelExp
        RelExpNode relExpNode = RelExp();
        Token operator = null;
        EqExpNode eqExpNode = null;
        if (now.getType() == TokenType.EQL) {
            operator = match(TokenType.EQL);
            eqExpNode = EqExp();
        } else if (now.getType() == TokenType.NEQ) {
            operator = match(TokenType.NEQ);
            eqExpNode = EqExp();
        }
        return new EqExpNode(relExpNode, operator, eqExpNode);
    }

    private LAndExpNode LAndExp() {
        // LAndExp -> EqExp | LAndExp '&&' EqExp
        EqExpNode eqExpNode = EqExp();
        Token operator = null;
        LAndExpNode lAndExpNode = null;
        if (now.getType() == TokenType.AND) {
            operator = match(TokenType.AND);
            lAndExpNode = LAndExp();
        }
        return new LAndExpNode(eqExpNode, operator, lAndExpNode);
    }

    private LOrExpNode LOrExp() {
        // LOrExp -> LAndExp | LOrExp '||' LAndExp
        LAndExpNode lAndExpNode = LAndExp();
        Token operator = null;
        LOrExpNode lOrExpNode = null;
        if (now.getType() == TokenType.OR) {
            operator = match(TokenType.OR);
            lOrExpNode = LOrExp();
        }
        return new LOrExpNode(lAndExpNode, operator, lOrExpNode);
    }

    private ConstExpNode ConstExp() {
        // ConstExp -> AddExp
        return new ConstExpNode(AddExp());
    }

    private Token match(TokenType tokenType) {
        if (now.getType() == tokenType) {
            Token tmpNow = now;
            if (index < tokens.size() - 1) {
                now = tokens.get(++index);
            }
            return tmpNow;
        } else if (tokenType == TokenType.SEMICN) {
            ErrorHandler.getInstance().addError(new Error(tokens.get(index - 1).getLineNumber(), ErrorType.i));
            return new Token(TokenType.SEMICN, tokens.get(index - 1).getLineNumber(), ";");
        } else if (tokenType == TokenType.RPARENT) {
            ErrorHandler.getInstance().addError(new Error(tokens.get(index - 1).getLineNumber(), ErrorType.j));
            return new Token(TokenType.RPARENT, tokens.get(index - 1).getLineNumber(), ")");
        } else if (tokenType == TokenType.RBRACK) {
            ErrorHandler.getInstance().addError(new Error(tokens.get(index - 1).getLineNumber(), ErrorType.k));
            return new Token(TokenType.RBRACK, tokens.get(index - 1).getLineNumber(), "]");
        } else {
            throw new RuntimeException("Syntax error at line " + now.getLineNumber() + ": " + now.getContent() + " is not " + tokenType);
        }
    }

    public void printParseAns() {
        compUnitNode.print();
    }
}
