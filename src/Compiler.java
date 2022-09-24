import frontend.Lexer;
import token.Token;
import utils.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Compiler {
    private static List<Token> tokens = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        final int stage = 1;
        String content = IOUtils.read("testfile.txt");

        Lexer lexer = new Lexer(tokens);
        lexer.analyze(content);
        if (stage == 1) {
            IOUtils.write(lexer.getLexAns(), "output.txt");
        }
    }
}