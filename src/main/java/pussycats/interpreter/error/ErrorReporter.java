package pussycats.interpreter.error;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Accumulates {@link DiagnosticError}s across an entire parse */
public final class ErrorReporter {
    private static final int MAX_ERRORS = 200;

    private final List<DiagnosticError> errors = new ArrayList<>();
    private final String source;
    private final String filename;
    private final String errorType;

    public ErrorReporter(String source, String filename, String errorType) {
        this.source = source;
        this.filename = filename;
        this.errorType = errorType;
    }

    public void report(String message, int line, int column) {
        report(message, null, line, column, 1);
    }

    public void report(String message, String suggestion, int line, int column, int length) {
        if (tooManyErrors()) {
            return;
        }
        errors.add(new DiagnosticError(message, suggestion, source, filename, line, column, length, errorType));
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
