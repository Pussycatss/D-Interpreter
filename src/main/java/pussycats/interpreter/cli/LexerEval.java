package pussycats.interpreter.cli;

import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.Token;

import java.util.List;

public class LexerEval implements Evaluable {
    public List<Token> eval(String line) {
        List<Token> tokens;
        tokens = new Lexer(line, "<stdin>").scan();

        return tokens;
    }
}
