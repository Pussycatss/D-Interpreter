package pussycats.interpreter.runtime;

import pussycats.interpreter.SourceSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when an imported module cannot be compiled.
 * <p>
 * Carries the *deepest* failure (errorType/message/suggestion/span/sourceName)
 * plus the chain of import sites that led to it (outermost first).
 */
public class ModuleLoadException extends RuntimeException {
    private final List<CallStackFrame> frames;
    private final String errorType;
    private final String suggestion;
    private final SourceSpan span;
    private final String sourceName;

    public ModuleLoadException(List<CallStackFrame> frames,
                               String errorType,
                               String message,
                               String suggestion,
                               SourceSpan span,
                               String sourceName) {
        super(message);
        this.frames = List.copyOf(frames);
        this.errorType = errorType;
        this.suggestion = suggestion;
        this.span = span;
        this.sourceName = sourceName;
    }

    public List<CallStackFrame> getFrames() {
        return frames;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public SourceSpan getSpan() {
        return span;
    }

    public String getSourceName() {
        return sourceName;
    }

    public ModuleLoadException wrap(CallStackFrame site) {
        List<CallStackFrame> newFrames = new ArrayList<>(frames.size() + 1);
        newFrames.add(site);
        newFrames.addAll(frames);
        return new ModuleLoadException(newFrames, errorType, getMessage(), suggestion, span, sourceName);
    }
}