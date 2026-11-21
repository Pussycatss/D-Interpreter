package pussycats.interpreter.runtime;

import pussycats.interpreter.ast.SourceLocation;

public class InterpreterException extends RuntimeException {
    private final SourceLocation location;
    
    public InterpreterException(SourceLocation loc, String message) {
        super(message);
        this.location = loc;
    }

    public SourceLocation getLocation() {
        return location;
    }
}
