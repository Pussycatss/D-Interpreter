package pussycats.interpreter.runtime;

import java.util.Iterator;
import java.util.List;

/**
 * Runtime tuple. Named and/or positional fields. Each entry holds a cell
 * (Cell), not a raw object, so a tuple field can be an l-cell
 * (subject to whatever mutability policy interpretAssign adopts — see note).
 */
public record TupleCell(List<Entry> entries) implements Iterable<TupleCell.Entry> {
    /**
     * name may be null for positional-only entries.
     */
    public record Entry(String name, Cell cell) {
    }

    public int size() {
        return entries.size();
    }

    public Entry get(int i) {
        return entries.get(i);
    }

    public Entry getByName(String name) {
        for (Entry e : entries) {
            if (name.equals(e.name())) return e;
        }
        return null;
    }

    @Override
    public Iterator<Entry> iterator() {
        return entries.iterator();
    }

}