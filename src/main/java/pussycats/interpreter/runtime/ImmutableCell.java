package pussycats.interpreter.runtime;

import pussycats.interpreter.ast.SourceLocation;

public record ImmutableCell(Object value, SourceLocation loc) implements Cell {
    @Override
    public Object get() {
        return value;
    }

    @Override
    public void set(Object value) {
        throw new UnsupportedOperationException("ImmutableCell cannot be modified");
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}
