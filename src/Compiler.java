import backend.MipsGenModule;
import config.Config;
import error.ErrorHandler;
import frontend.Lexer;
import frontend.Parser;
import ir.IRModule;
import ir.LLVMGenerator;
import utils.IOUtils;

import java.io.IOException;

public class Compiler {
    public static void main(String[] args) throws IOException {
        Config.init();

        Lexer.getInstance().analyze(IOUtils.read(Config.fileInPath));
        if (Config.lexer) {
            Lexer.getInstance().printLexAns();
        }

        Parser.getInstance().setTokens(Lexer.getInstance().getTokens());
        Parser.getInstance().analyze();
        if (Config.parser) {
            Parser.getInstance().printParseAns();
        }

        ErrorHandler.getInstance().compUnitError(Parser.getInstance().getCompUnitNode());

        if (Config.error) {
            ErrorHandler.getInstance().printErrors();
        }

        if (!ErrorHandler.getInstance().getErrors().isEmpty()) {
            return;
        }

        if (Config.ir) {
            LLVMGenerator.getInstance().visitCompUnit(Parser.getInstance().getCompUnitNode());
            IOUtils.llvm_ir_raw(IRModule.getInstance().toString());
            IOUtils.llvm_ir(IRModule.getInstance().toString());
        }

        if (Config.mips) {
            MipsGenModule.getInstance().loadIR();
            MipsGenModule.getInstance().genMips();
        }
    }
}