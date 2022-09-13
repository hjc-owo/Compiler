package frontend;

import token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        }

    }

    public String getTK(String word) {
        if (Objects.equals(word, "main")) return "MAINTK";
        else if (Objects.equals(word, "const")) return "CONSTTK";
        else if (Objects.equals(word, "int")) return "INTTK";
        else if (Objects.equals(word, "break")) return "BREAKTK";
        else if (Objects.equals(word, "continue")) return "CONTINUETK";
        else if (Objects.equals(word, "if")) return "IFTK";
        else if (Objects.equals(word, "else")) return "ELSETK";
        else if (Objects.equals(word, "while")) return "WHILETK";
        else if (Objects.equals(word, "getint")) return "SCANFTK";
        else if (Objects.equals(word, "printf")) return "PRINTFTK";
        else if (Objects.equals(word, "return")) return "RETURNTK";
        else if (Objects.equals(word, "void")) return "VOIDTK";
        else return "IDENFR";
    }
}
