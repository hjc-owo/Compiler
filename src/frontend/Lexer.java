package frontend;

import token.Token;
import token.TokenType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private List<Token> tokens;

    private Map<String, TokenType> keywords = new HashMap<String, TokenType>() {{
        put("main", TokenType.MAINTK);
        put("const", TokenType.CONSTTK);
        put("int", TokenType.INTTK);
        put("break", TokenType.BREAKTK);
        put("continue", TokenType.CONTINUETK);
        put("if", TokenType.IFTK);
        put("else", TokenType.ELSETK);
        put("while", TokenType.WHILETK);
        put("getint", TokenType.GETINTTK);
        put("printf", TokenType.PRINTFTK);
        put("return", TokenType.RETURNTK);
        put("void", TokenType.VOIDTK);
    }};

    public Lexer(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void analyze(String content) {
        // initLexer();

        int lineNumber = 1; // 当前所在行数
        int contentLength = content.length(); // 源代码长度

        for (int i = 0; i < contentLength; i++) {
            char c = content.charAt(i);
            // System.out.print(c);
            if (c == '\n') lineNumber++;
                // else if (isWhitespace(c)) continue; // 跳过空白字符
            else if (c == '_' || Character.isLetter(c)) { // 标识符
                String s = "";
                for (int j = i; j < contentLength; j++) {
                    char d = content.charAt(j);
                    if (d == '_' || Character.isLetter(d) || Character.isDigit(d)) s += d;
                    else {
                        i = j - 1;
                        break;
                    }
                }
                tokens.add(new Token(keywords.getOrDefault(s, TokenType.IDENFR), lineNumber, s));
            } else if (Character.isDigit(c)) { // 数字
                String s = "";
                for (int j = i; j < contentLength; j++) {
                    char d = content.charAt(j);
                    if (Character.isDigit(d)) s += d;
                    else {
                        i = j - 1;
                        break;
                    }
                }
                tokens.add(new Token(TokenType.INTCON, lineNumber, s));
            } else if (c == '\"') { // 字符串
                String s = "\"";
                for (int j = i + 1; j < contentLength; j++) {
                    char d = content.charAt(j);
                    if (d != '\"') s += d;
                    else {
                        i = j;
                        s += "\"";
                        break;
                    }
                }
                tokens.add(new Token(TokenType.STRCON, lineNumber, s));
            } else if (c == '!') { // ! 或 !=
                if (content.charAt(i + 1) != '=') tokens.add(new Token(TokenType.NOT, lineNumber, "!"));
                else {
                    tokens.add(new Token(TokenType.NEQ, lineNumber, "!="));
                    i++;
                }
            } else if (c == '&') { // &&
                if (content.charAt(i + 1) == '&') {
                    tokens.add(new Token(TokenType.AND, lineNumber, "&&"));
                    i++;
                }
            } else if (c == '|') { // ||
                if (content.charAt(i + 1) == '|') {
                    tokens.add(new Token(TokenType.OR, lineNumber, "||"));
                    i++;
                }
            } else if (c == '+') { // +
                tokens.add(new Token(TokenType.PLUS, lineNumber, "+"));
            } else if (c == '-') { // -
                tokens.add(new Token(TokenType.MINU, lineNumber, "-"));
            } else if (c == '*') { // *
                tokens.add(new Token(TokenType.MULT, lineNumber, "*"));
            } else if (c == '/') { // / 或 // 或 /*
                char d = content.charAt(i + 1);
                if (d == '/') { // //
                    int j = content.indexOf('\n', i + 2);
                    lineNumber++;
                    i = j - 1;
                } else if (d == '*') { // /* */
                    for (int j = i + 2; j < contentLength; j++) {
                        char e = content.charAt(j);
                        if (e == '\n') lineNumber++;
                        else if (e == '*' && content.charAt(j + 1) == '/') {
                            i = j + 1;
                            break;
                        }
                    }
                } else tokens.add(new Token(TokenType.DIV, lineNumber, "/"));
            } else if (c == '%') { // %
                tokens.add(new Token(TokenType.MOD, lineNumber, "%"));
            } else if (c == '<') { // < 或 <=
                if (content.charAt(i + 1) != '=') { // <
                    tokens.add(new Token(TokenType.LSS, lineNumber, "<"));
                } else {
                    tokens.add(new Token(TokenType.LEQ, lineNumber, "<="));
                    i++;
                }
            } else if (c == '>') { // > 或 >=
                if (content.charAt(i + 1) != '=') { // >
                    tokens.add(new Token(TokenType.GRE, lineNumber, ">"));
                } else {
                    tokens.add(new Token(TokenType.GEQ, lineNumber, ">="));
                    i++;
                }
            } else if (c == '=') { // = 或 ==
                if (content.charAt(i + 1) != '=') tokens.add(new Token(TokenType.ASSIGN, lineNumber, "="));
                else {
                    tokens.add(new Token(TokenType.EQL, lineNumber, "=="));
                    i++;
                }
            } else if (c == ';') tokens.add(new Token(TokenType.SEMICN, lineNumber, ";"));
            else if (c == ',') tokens.add(new Token(TokenType.COMMA, lineNumber, ","));
            else if (c == '(') tokens.add(new Token(TokenType.LPARENT, lineNumber, "("));
            else if (c == ')') tokens.add(new Token(TokenType.RPARENT, lineNumber, ")"));
            else if (c == '[') tokens.add(new Token(TokenType.LBRACK, lineNumber, "["));
            else if (c == ']') tokens.add(new Token(TokenType.RBRACK, lineNumber, "]"));
            else if (c == '{') tokens.add(new Token(TokenType.LBRACE, lineNumber, "{"));
            else if (c == '}') tokens.add(new Token(TokenType.RBRACE, lineNumber, "}"));
        }

    }

    public String getLexAns() {
        StringBuilder ans = new StringBuilder();
        for (Token token : tokens) {
            ans.append(token.toString());
        }
        return ans.toString();
    }
}
