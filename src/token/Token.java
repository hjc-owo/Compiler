package token;

import java.util.HashMap;
import java.util.Map;

public class Token {
    private TokenType type;
    private int lineNumber;
    private String content;

    public Token(TokenType type, int lineNumber, String content) {
        this.type = type;
        this.lineNumber = lineNumber;
        this.content = content;
    }

    public static Map<TokenType, Token> constTokens = new HashMap<TokenType, Token>() {{
        put(TokenType.MAINTK, new Token(TokenType.MAINTK, 0, "main"));
        put(TokenType.CONSTTK, new Token(TokenType.CONSTTK, 0, "const"));
        put(TokenType.INTTK, new Token(TokenType.INTTK, 0, "int"));
        put(TokenType.BREAKTK, new Token(TokenType.BREAKTK, 0, "break"));
        put(TokenType.CONTINUETK, new Token(TokenType.CONTINUETK, 0, "continue"));
        put(TokenType.IFTK, new Token(TokenType.IFTK, 0, "if"));
        put(TokenType.ELSETK, new Token(TokenType.ELSETK, 0, "else"));
        put(TokenType.NOT, new Token(TokenType.NOT, 0, "!"));
        put(TokenType.AND, new Token(TokenType.AND, 0, "&&"));
        put(TokenType.OR, new Token(TokenType.OR, 0, "||"));
        put(TokenType.WHILETK, new Token(TokenType.WHILETK, 0, "while"));
        put(TokenType.GETINTTK, new Token(TokenType.GETINTTK, 0, "getint"));
        put(TokenType.PRINTFTK, new Token(TokenType.PRINTFTK, 0, "printf"));
        put(TokenType.RETURNTK, new Token(TokenType.RETURNTK, 0, "return"));
        put(TokenType.PLUS, new Token(TokenType.PLUS, 0, "+"));
        put(TokenType.MINU, new Token(TokenType.MINU, 0, "-"));
        put(TokenType.VOIDTK, new Token(TokenType.VOIDTK, 0, "void"));
        put(TokenType.MULT, new Token(TokenType.MULT, 0, "*"));
        put(TokenType.DIV, new Token(TokenType.DIV, 0, "/"));
        put(TokenType.MOD, new Token(TokenType.MOD, 0, "%"));
        put(TokenType.LSS, new Token(TokenType.LSS, 0, "<"));
        put(TokenType.LEQ, new Token(TokenType.LEQ, 0, "<="));
        put(TokenType.GRE, new Token(TokenType.GRE, 0, ">"));
        put(TokenType.GEQ, new Token(TokenType.GEQ, 0, ">="));
        put(TokenType.EQL, new Token(TokenType.EQL, 0, "=="));
        put(TokenType.NEQ, new Token(TokenType.NEQ, 0, "!="));
        put(TokenType.ASSIGN, new Token(TokenType.ASSIGN, 0, "="));
        put(TokenType.SEMICN, new Token(TokenType.SEMICN, 0, ";"));
        put(TokenType.COMMA, new Token(TokenType.COMMA, 0, ","));
        put(TokenType.LPARENT, new Token(TokenType.LPARENT, 0, "("));
        put(TokenType.RPARENT, new Token(TokenType.RPARENT, 0, ")"));
        put(TokenType.LBRACK, new Token(TokenType.LBRACK, 0, "["));
        put(TokenType.RBRACK, new Token(TokenType.RBRACK, 0, "]"));
        put(TokenType.LBRACE, new Token(TokenType.LBRACE, 0, "{"));
        put(TokenType.RBRACE, new Token(TokenType.RBRACE, 0, "}"));
    }};

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return type.toString() + " " + content + "\n";
    }
}
