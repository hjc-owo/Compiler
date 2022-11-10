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
    private static final int stage = 4;
    /**
     * 0 - 不做优化处理
     * 1 - 连续加法变成乘法
     */
    private static final int optimizationLevel = 1;
    private static final boolean chToStr = true;

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

        if (stage == 3) {
            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.compUnitError(parser.getCompUnitNode());
            errorHandler.printErrors();
            return;
        }

        if (stage >= 4) {
            LLVMGenerator generator = new LLVMGenerator(optimizationLevel, chToStr);
            generator.visitCompUnit(parser.getCompUnitNode());
            IOUtils.write(IRModule.getInstance().toString(), "llvm_ir.txt");
        }
    }
}