package pussycats.interpreter.runtime;

import java.util.Iterator;
import java.util.List;

/**
 * Runtime array. Holds cells (Cell), not raw cells, so elements are assignable:
 * `a[0] = x` works because indexing returns the cell, not just its contents.
 * <p>
 * This is NOT a Cell — it's the *contents* of a cell. An array literal evaluates
 * to `ImmutableCell(new ArrayCell(...), loc)`; a variable holding an array is
 * `MutableCell(new ArrayCell(...), loc)`.
 */
public record ArrayCell(List<Cell> elements) implements Iterable<Cell> {

    public int size() {
        return elements.size();
    }

    public Cell get(int i) {
        return elements.get(i);
    }

    @Override
    public Iterator<Cell> iterator() {
        return elements.iterator();
    }
}