package pussycats.interpreter.lexer;

import pussycats.interpreter.InterpreterException;

/**
 * Thrown when the lexer encounters text that cannot be turned into a token
 * TODO: ENUMERATE POSSIBLE ERRORS
 */
public final class LexerException extends InterpreterException {

    public LexerException(String message, String source, int line, int column) {
        super(message, source, line, column);
    }

    public LexerException(String message, String source, String filename, int line, int column) {
        super(message, source, filename, line, column);
    }

    @Override
    protected String getErrorType() {
        return "SyntaxError";
    }
}