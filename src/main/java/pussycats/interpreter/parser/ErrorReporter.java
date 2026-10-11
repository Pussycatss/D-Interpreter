package pussycats.interpreter.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Accumulates {@link ParseError}s across an entire parse */
public final class ErrorReporter {
    private static final int MAX_ERRORS = 200;

    private final List<ParseError> errors = new ArrayList<>();
    private final String source;
    private final String filename;

    public ErrorReporter(String source, String filename) {
        this.source = source;
        this.filename = filename;
    }

    public void report(String message, int line, int column) {
        report(message, null, line, column);
    }

    public void report(String message, String suggestion, int line, int column) {
        if (tooManyErrors()) {
            return;
        }
        errors.add(new ParseError(message, suggestion, source, filename, line, column));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean tooManyErrors() {
        return errors.size() >= MAX_ERRORS;
    }

    public List<ParseError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /** Renders every collected error, in the order they were found. */
    public String formatAll() {
        StringBuilder sb = new StringBuilder();
        for (ParseError e : errors) {
            sb.append(e.format()).append("\n\n");
        }
        return sb.toString();
    }
}
