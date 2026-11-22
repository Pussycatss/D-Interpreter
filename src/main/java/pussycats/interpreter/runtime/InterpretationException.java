package pussycats.interpreter.runtime;

import pussycats.interpreter.InterpreterException;
import pussycats.interpreter.SourceSpan;

import java.util.List;

public class InterpretationException extends InterpreterException {
    private final List<CallStackFrame> callStack;
    private final String functionName;
    
    public InterpretationException(
            SourceSpan span,
            String message,
            List<CallStackFrame> callStack,
            String functionName) {
        super(message, null, null, span);
        this.callStack = List.copyOf(callStack);
        this.functionName = functionName;
    }

    public List<CallStackFrame> getCallStack() {
        return callStack;
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    protected String getErrorType() {
        return "RuntimeError";
    }
}
