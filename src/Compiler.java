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
    public static final int stage = 2;

    public static int getStage() {
        return stage;
    }

    public static void main(String[] args) throws IOException {
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

//        LLVMGenerator generator = new LLVMGenerator();
//        generator.visitCompUnit(parser.getCompUnitNode());

        if (error) {
            ErrorHandler.printErrors();
        }
    }
}