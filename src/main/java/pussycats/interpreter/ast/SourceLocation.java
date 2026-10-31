package pussycats.interpreter.ast;

public record SourceLocation(int line, int column) {
    public static final SourceLocation UNKNOWN = new SourceLocation(-1, -1);
}