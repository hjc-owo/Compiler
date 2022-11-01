import error.ErrorHandler;
import frontend.Lexer;
import frontend.Parser;
//import ir.IRModule;
//import ir.LLVMGenerator;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Compiler {
    private static List<Token> tokens = new ArrayList<>();
    public static final int stage = 3;

    public static int getStage() {
        return stage;
    }

    public static void main(String[] args) throws IOException {
        final boolean error = true;
        IOUtils.delete("output.txt");
        IOUtils.delete("error.txt");
        IOUtils.delete("llvm_ir.txt");
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

        ErrorHandler errorHandler = new ErrorHandler();
        errorHandler.compUnitError(parser.getCompUnitNode());
        if (error) {
            ErrorHandler.printErrors();
        }

//        LLVMGenerator generator = new LLVMGenerator();
//        generator.visitCompUnit(parser.getCompUnitNode());
//        if (stage == 3) {
//            IOUtils.write(IRModule.getInstance().toString(), "llvm_ir.txt");
//        }
    }
}