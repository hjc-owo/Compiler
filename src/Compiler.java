import error.ErrorHandler;
import frontend.Lexer;
import frontend.Parser;
import ir.IRModule;
import ir.LLVMGenerator;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class Compiler {
    private static final int stage = 3;
    /**
     * 0 - 不做优化处理
     * 1 - 变成字符串输出
     */
    private static final int optimizationLevel = 1;

    public static void main(String[] args) throws IOException {
        IOUtils.delete("output.txt");
        IOUtils.delete("error.txt");
        IOUtils.delete("llvm_ir.txt");
        String content = IOUtils.read("testfile.txt");

        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.analyze(content);
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
        if (!errorHandler.getErrors().isEmpty()) {
            errorHandler.printErrors();
            return;
        }

        LLVMGenerator generator = new LLVMGenerator(optimizationLevel);
        generator.visitCompUnit(parser.getCompUnitNode());
        if (stage == 3) {
            IOUtils.write(IRModule.getInstance().toString(), "llvm_ir.txt");
        }
    }
}