import backend.MipsGenModule;
import config.Config;
import error.ErrorHandler;
import frontend.Lexer;
import frontend.Parser;
import ir.IRModule;
import ir.LLVMGenerator;
import pass.PassModule;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.List;

public class Compiler {
    public static void main(String[] args) throws IOException {
        Config.init();
        String content = IOUtils.read(Config.fileInPath);

        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.analyze(content);
        if (Config.lexer) {
            lexer.printLexAns();
        }

        Parser parser = new Parser(tokens);
        parser.analyze();
        if (Config.parser) {
            parser.printParseAns();
        }

        if (Config.error) {
            ErrorHandler errorHandler = new ErrorHandler();
            errorHandler.compUnitError(parser.getCompUnitNode());
            errorHandler.printErrors();
            return;
        }

        if (Config.ir) {
            LLVMGenerator generator = new LLVMGenerator();
            generator.visitCompUnit(parser.getCompUnitNode());
            IOUtils.llvm_ir_raw(IRModule.getInstance().toString());
            PassModule.getInstance().runIRPasses();
            IOUtils.llvm_ir(IRModule.getInstance().toString());
        }

        if (Config.mips) {
            MipsGenModule.getInstance().loadIR();
            MipsGenModule.getInstance().genMips();
        }
    }
}