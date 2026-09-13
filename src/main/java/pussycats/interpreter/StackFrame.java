package pussycats.interpreter;

/**
 * Represents a single frame in the interpreter's call stack.
 * Stores information about where in the source code execution is currently located.
 */
public record StackFrame(
    String functionName,  // Name of the function being executed
    String source,        // Source code of the function
    String filename,      // Filename where the function is defined
    int line,             // Current line in the function
    int column            // Current column in the function
) {
    public StackFrame {
        if (functionName == null) {
            functionName = "<top-level>";
        }
        if (filename == null) {
            filename = "<string>";
        }
    }

    @Override
    public String toString() {
        return String.format("  File \"%s\", line %d, in %s", filename, line, functionName);
    }
}
