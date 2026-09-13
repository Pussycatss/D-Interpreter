package pussycats.interpreter.lexer;

/**
 * Custom exception.
 *
 * <p>Thrown when the lexer encounters invalid input.</p>
 */
public class LexerException extends RuntimeException {

    private final int line;
    private final int column;

    public LexerException(String message, int line, int column) {
        super("Lex error at " + line + ":" + column + " —— " + message);
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}