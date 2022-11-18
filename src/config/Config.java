package config;

import utils.IOUtils;

public class Config {
    /**
     * stages of compilation
     */
    public static boolean lexer = false;
    public static boolean parser = false;
    public static boolean error = false;
    public static boolean ir = true;
    public static boolean mips = true;

    /**
     * optimization level
     */
    public static boolean chToStr = true;
    public static boolean addToMul = true;
    public static boolean DeadCodeElimination = true;
    public static boolean GlobalVarLocalize = true;
    public static boolean Mem2Reg = true;

    public static void init() {
        IOUtils.delete("output.txt");
        IOUtils.delete("error.txt");
        IOUtils.delete("llvm_ir_raw.txt");
        IOUtils.delete("llvm_ir.txt");
        IOUtils.delete("llvm_ir_op.txt");
        IOUtils.delete("mips.txt");
    }
}
