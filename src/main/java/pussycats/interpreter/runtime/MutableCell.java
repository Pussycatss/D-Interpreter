package pussycats.interpreter.runtime;

import pussycats.interpreter.SourceLocation;

public final class MutableCell implements Cell {
    private Object value;
    private final SourceLocation loc;

    public MutableCell(Object value, SourceLocation loc) {
        this.value = value;
        this.loc = loc;
    }

    @Override
    public Object get() {
        return value;
    }

    @Override
    public void set(Object value) {
        this.value = value;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    public Object value() {
        return value;
    }

    public SourceLocation loc() {
        return loc;
    }
}