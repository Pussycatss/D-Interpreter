package pussycats.interpreter.runtime;

import pussycats.interpreter.SourceSpan;

/**
 * Represents a single frame in the interpreter's call stack.
 * <p>
 * Stores information about where in the source code execution is currently located.
 */
public record CallStackFrame(
        String functionName,
        SourceSpan span
) {
    public CallStackFrame {
        if (functionName == null) {
            functionName = "<anonymous>";
        }

        if (span == null) {
            span = SourceSpan.UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return "  File, line " + span.start().line()
                + ", in " + functionName;
    }
}
