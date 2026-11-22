package pussycats.interpreter;

/**
 * Base exception class for all errors.
 * <p>
 * Provides source code context and error formatting.
 */
public abstract class InterpreterException extends RuntimeException {

    private final String source;
    private final String filename;
    private final SourceSpan span;

    public InterpreterException(String message, String source, String filename, int line, int column) {
        super(message);
        this.source = source;
        this.filename = filename;
        this.span = new SourceSpan(new SourceLocation(line, column), new SourceLocation(line, column));
    }

    public InterpreterException(String message, String source, int line, int column) {
        this(message, source, "<unknown>", line, column);
    }

    public InterpreterException(String message, String source, String filename, SourceSpan span) {
        super(message);
        this.source = source;
        this.filename = filename;
        this.span = span;
    }

    public String getSource() {
        return source;
    }

    public String getFilename() {
        return filename;
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

    /**
     * Formats the error in style:
     * <pre>
     *   File "script.d", line 10, in main
     *     result = divide(x, 0)
     *   File "script.d", line 5, in divide
     *     return a / b
     * ZeroDivisionError: division by zero
     * </pre>
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("  File \"").append(getFilename()).append("\", line ").append(getSpan().start().line()).append("\n");
        
        String sourceLine = getSourceLine();
        if (sourceLine != null) {
            sb.append("    ").append(sourceLine).append("\n");
            
            int markerPosition = getSpan().start().column() - 1;
            if (markerPosition >= 0 && markerPosition <= sourceLine.length()) {
                sb.append("    ");
                sb.repeat(" ", markerPosition);
                sb.append("^\n");
            }
        }
        
        sb.append(getErrorType()).append(": ").append(getMessage());
        
        return sb.toString();
    }

    private String getSourceLine() {
        return getFrameSourceLine(getSource(), getSpan().start().line());
    }

    private String getFrameSourceLine(String frameSource, int frameLine) {
        if (frameSource == null) {
            return null;
        }
        
        String[] lines = frameSource.split("\n", -1);
        if (frameLine >= 1 && frameLine <= lines.length) {
            return lines[frameLine - 1];
        }
        return null;
    }

    protected abstract String getErrorType();
}
