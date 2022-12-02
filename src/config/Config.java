package config;

import utils.IOUtils;

import java.io.IOException;
import java.io.PrintStream;

public class Config {
    /**
     * The path of files.
     */
    public static String fileInPath = "testfile.txt";
    public static String fileOutPath = "output.txt";
    public static String fileErrorPath = "error.txt";
    public static String fileLlvmIRPath = "llvm_ir.txt";
    public static String fileMipsPath = "mips.txt";
    public static String stdOutPath = "stdout.txt";
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

    public static void init() throws IOException {
        IOUtils.delete(fileOutPath);
        IOUtils.delete(fileErrorPath);
        IOUtils.delete(fileLlvmIRPath);
        IOUtils.delete(fileMipsPath);
        System.setOut(new PrintStream(stdOutPath));
    }
}
