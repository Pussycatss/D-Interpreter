package pussycats.interpreter.runtime;

import pussycats.interpreter.InterpreterException;
import pussycats.interpreter.SourceSpan;

import java.util.List;

public class InterpretationException extends InterpreterException {
    private final List<CallStackFrame> callStack;
    
    public InterpretationException(
            SourceSpan span,
            String message,
            List<CallStackFrame> callStack) {
        super(message, span);
        this.callStack = List.copyOf(callStack);
    }

    public List<CallStackFrame> getCallStack() {
        return callStack;
    }
}
