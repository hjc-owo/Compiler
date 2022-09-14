import frontend.Lexer;
import utils.IOUtils;

import java.io.IOException;

public class Compiler {
    public static void main(String[] args) throws IOException {
        final int stage = 1;
        String content = IOUtils.read("testfile.txt");

        Lexer lexer = new Lexer(content);
        lexer.analyze();
        if (stage == 1) {
            IOUtils.write(lexer.getLexAns(), "output.txt");
        }
    }
}