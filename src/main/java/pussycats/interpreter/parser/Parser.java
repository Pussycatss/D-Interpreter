package pussycats.interpreter.parser;

import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.lexer.Keywords;

import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private final ErrorReporter reporter;
    private int current = 0;
    private int loopDepth = 0;


    /** Internal control-flow signal only. */
    private static final class ParsePanic extends RuntimeException {
        ParsePanic() {
            super(null, null, false, false); // no message/stack trace: pure signal
        }
    }

    public Parser(List<Token> tokens, String source, String filename) {
        this.tokens = tokens;
        this.reporter = new ErrorReporter(source, filename);
    }

    public void parse() {
        parseProgram();
    }

    private void parseProgram() {

    }

    private void parseStatement() {

    }

    private void parseExpression() {

    }

    private void parseRelation() {

    }

    private void parseFactor() {

    }

    private void parseTerm() {

    }

    private void parseUnary() {

    }

    private void parsePrimary() {

    }
}
