package pussycats.interpreter;

/**
 * Base exception class for all errors.
 * <p>
 * Provides source code context and error formatting.
 */
public abstract class InterpreterException extends RuntimeException {
    private final SourceSpan span;

    public InterpreterException(String message, int line, int column) {
        super(message);
        this.span = new SourceSpan(new SourceLocation(line, column), new SourceLocation(line, column));
    }

    public InterpreterException(String message, SourceSpan span) {
        super(message);
        this.span = span;
    }

    public int getLine() {
        return span.start().line();
    }

    public int getColumn() {
        return span.start().column();
    }

    public SourceSpan getSpan() {
        return span;
    }
}
