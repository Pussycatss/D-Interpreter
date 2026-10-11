package pussycats.interpreter.parser;

import pussycats.interpreter.InterpreterException;

public class ParserException extends InterpreterException {
    public ParserException(String message, String source, int line, int column) {
        super(message, source, line, column);
    }

    public ParserException(String message, String source, String filename, int line, int column) {
        super(message, source, filename, line, column);
    }

    @Override
    protected String getErrorType() {
        return "SyntaxError";
    }
}
