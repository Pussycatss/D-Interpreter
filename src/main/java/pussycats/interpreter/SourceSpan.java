package pussycats.interpreter;

public record SourceSpan(SourceLocation start, SourceLocation end) {
    public static final SourceSpan UNKNOWN = new SourceSpan(SourceLocation.UNKNOWN, SourceLocation.UNKNOWN);

    public int length() {
        if (start.line() != end.line()) {
            return 1;
        }

        return Math.max(1, end.column() - start.column());
    }
}