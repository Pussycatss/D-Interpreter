package pussycats.interpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * Base exception class for all errors.
 * <p>
 * Provides source code context and error formatting.
 */
public abstract class InterpreterException extends RuntimeException {

    private final String source;
    private final String filename;
    private final int line;
    private final int column;
    private final List<StackFrame> stackTrace = new ArrayList<>();

    public InterpreterException(String message, String source, String filename, int line, int column) {
        super(message);
        this.source = source;
        this.filename = filename;
        this.line = line;
        this.column = column;
    }

    public InterpreterException(String message, String source, int line, int column) {
        this(message, source, "<unknown>", line, column);
    }

    public String getSource() {
        return source;
    }

    public String getFilename() {
        return filename;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public void pushStackFrame(StackFrame frame) {
        stackTrace.add(frame);
    }

    public List<StackFrame> getCallStack() {
        return new ArrayList<>(stackTrace);
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

        // Print call stack if non-empty TODO: implement in runtime (see ARCHITECTURE.md)
        if (!stackTrace.isEmpty()) {
            for (StackFrame frame : stackTrace) {
                sb.append(frame).append("\n");
                String frameLine = getFrameSourceLine(frame);
                if (frameLine != null) {
                    sb.append("    ").append(frameLine).append("\n");
                }
            }
            return sb.toString();
        }
        
        sb.append("  File \"").append(filename).append("\", line ").append(line).append("\n");
        
        String sourceLine = getSourceLine();
        if (sourceLine != null) {
            sb.append("    ").append(sourceLine).append("\n");
            
            int markerPosition = column - 1;
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
        return getFrameSourceLine(source, line);
    }

    private String getFrameSourceLine(StackFrame frame) {
        return getFrameSourceLine(frame.source(), frame.line());
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
