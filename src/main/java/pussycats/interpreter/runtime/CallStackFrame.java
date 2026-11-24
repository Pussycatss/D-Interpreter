package pussycats.interpreter.runtime;

import pussycats.interpreter.SourceSpan;

/**
 * Represents a single frame in the interpreter's call stack.
 * <p>
 * Stores information about where in the source code execution is currently located.
 */
public record CallStackFrame(String functionName, SourceSpan span, String sourceName) {
    public static final String UNKNOWN_SOURCE = "<unknown>";

    public CallStackFrame {
        if (functionName == null) {
            functionName = "<anonymous>";
        }
        if (span == null) {
            span = SourceSpan.UNKNOWN;
        }
        if (sourceName == null || sourceName.isEmpty()) {
            sourceName = UNKNOWN_SOURCE;
        }
    }

    @Override
    public String toString() {
        return "  File \"" + sourceName + "\", line " + span.start().line()
                + ", in " + functionName;
    }
}
