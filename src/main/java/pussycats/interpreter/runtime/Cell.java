package pussycats.interpreter.runtime;

public sealed interface Cell permits ImmutableCell, MutableCell {
    Object get();
    void set(Object value);
    boolean isMutable();
}
