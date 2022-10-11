import error.ErrorHandler;
import frontend.Lexer;
import frontend.Parser;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Compiler {
    private static List<Token> tokens = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        final int stage = 2;
        final boolean error = true;
        IOUtils.delete("output.txt");
        IOUtils.delete("error.txt");
        String content = IOUtils.read("testfile.txt");

        Lexer lexer = new Lexer(tokens);
        lexer.analyze(content);
        if (stage == 1) {
            lexer.printLexAns();
        }

        Parser parser = new Parser(tokens);
        parser.analyze();
        if (stage == 2) {
            parser.printParseAns();
        }

        parser.fillSymbolTable();

        if (error) {
            ErrorHandler.printErrors();
        }
    }
}