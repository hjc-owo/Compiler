package frontend;

import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static token.TokenType.*;

public class Lexer {
    private final String content; // 源代码

    public static List<Token> tokens = new ArrayList<>(); // 词法分析结果

    public Lexer(String content) {
        this.content = content;
    }

    public void analyze() {
        int lineNumber = 1; // 当前所在行数
        int contentLength = content.length(); // 源代码长度

        for (int i = 0; i < contentLength; i++) {
            char c = content.charAt(i);
            // System.out.print(c);
            if (c == '\n') lineNumber++;
                // else if (isWhitespace(c)) continue; // 跳过空白字符
            else if (c == '_' || isLetter(c)) { // 标识符
                String s = "";
                for (int j = i; j < content.length(); j++) {
                    char d = content.charAt(j);
                    if (d == '_' || isLetter(d) || isDigit(d)) s += d;
                    else {
                        i = j - 1;
                        break;
                    }
                }
                tokens.add(new Token(getTK(s), lineNumber, s));
            } else if (isDigit(c)) { // 数字
                String s = "";
                for (int j = i; j < content.length(); j++) {
                    char d = content.charAt(j);
                    if (isDigit(d)) s += d;
                    else {
                        i = j - 1;
                        break;
                    }
                }
                tokens.add(new Token(INTCON, lineNumber, s));
            } else if (c == '\"') { // 字符串
                String s = "\"";
                for (int j = i + 1; j < content.length(); j++) {
                    char d = content.charAt(j);
                    if (d != '\"') s += d;
                    else {
                        i = j;
                        s += "\"";
                        break;
                    }
                }
                tokens.add(new Token(STRCON, lineNumber, s));
            } else if (c == '!') { // ! 或 !=
                if (content.charAt(i + 1) != '=') tokens.add(new Token(NOT, lineNumber, "!"));
                else {
                    tokens.add(new Token(NEQ, lineNumber, "!="));
                    i++;
                }
            } else if (c == '&') { // &&
                if (content.charAt(i + 1) == '&') {
                    tokens.add(new Token(AND, lineNumber, "&&"));
                    i++;
                }
            } else if (c == '|') { // ||
                if (content.charAt(i + 1) == '|') {
                    tokens.add(new Token(OR, lineNumber, "||"));
                    i++;
                }
            } else if (c == '+') { // +
                tokens.add(new Token(PLUS, lineNumber, "+"));
            } else if (c == '-') { // -
                tokens.add(new Token(MINU, lineNumber, "-"));
            } else if (c == '*') { // *
                tokens.add(new Token(MULT, lineNumber, "*"));
            } else if (c == '/') { // / 或 // 或 /*
                char d = content.charAt(i + 1);
                if (d == '/') { // //
                    int j = content.indexOf('\n', i + 2);
                    i = j - 1;
                } else if (d == '*') { // /* */
                    int j = content.indexOf("*/", i + 2);
                    i = j + 1;
                } else tokens.add(new Token(DIV, lineNumber, "/"));
            } else if (c == '%') { // %
                tokens.add(new Token(MOD, lineNumber, "%"));
            } else if (c == '<') { // < 或 <=
                if (content.charAt(i + 1) != '=') { // <
                    tokens.add(new Token(LSS, lineNumber, "<"));
                } else {
                    tokens.add(new Token(LEQ, lineNumber, "<="));
                    i++;
                }
            } else if (c == '>') { // > 或 >=
                if (content.charAt(i + 1) != '=') { // >
                    tokens.add(new Token(GRE, lineNumber, ">"));
                } else {
                    tokens.add(new Token(GEQ, lineNumber, ">="));
                    i++;
                }
            } else if (c == '=') { // = 或 ==
                if (content.charAt(i + 1) != '=') tokens.add(new Token(ASSIGN, lineNumber, "="));
                else {
                    tokens.add(new Token(EQL, lineNumber, "=="));
                    i++;
                }
            } else if (c == ';') tokens.add(new Token(SEMICN, lineNumber, ";"));
            else if (c == ',') tokens.add(new Token(COMMA, lineNumber, ","));
            else if (c == '(') tokens.add(new Token(LPARENT, lineNumber, "("));
            else if (c == ')') tokens.add(new Token(RPARENT, lineNumber, ")"));
            else if (c == '[') tokens.add(new Token(LBRACK, lineNumber, "["));
            else if (c == ']') tokens.add(new Token(RBRACK, lineNumber, "]"));
            else if (c == '{') tokens.add(new Token(LBRACE, lineNumber, "{"));
            else if (c == '}') tokens.add(new Token(RBRACE, lineNumber, "}"));
        }

    }

    public TokenType getTK(String word) {
        if (Objects.equals(word, "main")) return MAINTK;
        else if (Objects.equals(word, "const")) return CONSTTK;
        else if (Objects.equals(word, "int")) return INTTK;
        else if (Objects.equals(word, "break")) return BREAKTK;
        else if (Objects.equals(word, "continue")) return CONTINUETK;
        else if (Objects.equals(word, "if")) return IFTK;
        else if (Objects.equals(word, "else")) return ELSETK;
        else if (Objects.equals(word, "while")) return WHILETK;
        else if (Objects.equals(word, "getint")) return GETINTTK;
        else if (Objects.equals(word, "printf")) return PRINTFTK;
        else if (Objects.equals(word, "return")) return RETURNTK;
        else if (Objects.equals(word, "void")) return VOIDTK;
        else return IDENFR;
    }

    public String getLexAns() {
        StringBuilder ans = new StringBuilder();
        for (Token token : tokens) {
            ans.append(token.toString());
        }
        return ans.toString();
    }
}
