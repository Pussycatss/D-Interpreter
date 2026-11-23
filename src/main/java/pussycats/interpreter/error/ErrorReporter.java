package pussycats.interpreter.error;

import pussycats.interpreter.SourceLocation;
import pussycats.interpreter.SourceSpan;
import pussycats.interpreter.runtime.InterpretationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Accumulates {@link DiagnosticError}s across an entire parse */
public final class ErrorReporter {
    private static final int MAX_ERRORS = 200;

    private final List<DiagnosticError> errors = new ArrayList<>();
    private final String source;
    private final String filename;
    private final SourceManager sourceManager;

    public ErrorReporter(String source, String filename) {
        this(source, filename, null);
    }

    public ErrorReporter(String source, String filename, SourceManager sourceManager) {
        this.source = source;
        this.filename = filename;
        this.sourceManager = sourceManager;
    }

    public void report(String errorType, String message, SourceLocation loc) {
        report(errorType, message, null, loc.line(), loc.column(), 1);
    }

    public void report(String errorType, String message, SourceLocation loc, int length) {
        report(errorType, message, null, loc.line(), loc.column(), length);
    }

    public void report(String errorType, String message, String suggestion, SourceLocation loc, int length) {
        report(errorType, message, suggestion, loc.line(), loc.column(), length);
    }

    public void report(String errorType, String message, String suggestion, SourceLocation loc) {
        report(errorType, message, suggestion, loc.line(), loc.column(), 1);
    }

    public void report(String errorType, String message, String suggestion, int line, int column, int length) {
        if (tooManyErrors()) {
            return;
        }
        errors.add(new DiagnosticError(
                message,
                suggestion,
                source,
                filename,
                line,
                column,
                length,
                errorType,
                null,
                "<module>",
                sourceManager
        ));
    }

    public void report(InterpretationException exception) {
        if (tooManyErrors()) {
            return;
        }
        SourceSpan span = exception.getSpan();
        errors.add(new DiagnosticError(
                exception.getMessage(),
                null,
                source,
                filename,
                span.start().line(),
                span.start().column(),
                span.length(),
                "RuntimeError",
                exception.getCallStack(),
                exception.getFunctionName(),
                sourceManager
        ));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean tooManyErrors() {
        return errors.size() >= MAX_ERRORS;
    }

    public List<DiagnosticError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /** Renders every collected error, in the order they were found. */
    public String formatAll() {
        StringBuilder sb = new StringBuilder();
        for (DiagnosticError e : errors) {
            sb.append(e.format()).append("\n\n");
        }
        return sb.toString();
    }
}
