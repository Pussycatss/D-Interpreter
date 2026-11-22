package pussycats.interpreter.runtime;

import java.util.HashMap;
import java.util.Map;

public class StackFrame {
    private final Map<String, Cell> variables;
    private final StackFrame parent;

    public StackFrame(StackFrame parent) {
        this.parent = parent;
        this.variables = new HashMap<>();
    }

    public StackFrame() {
        this(null);
    }
    
    public Cell getVariable(String name) {
        Cell cell = variables.get(name);
        if (cell != null) {
            return cell;
        }
        return parent != null
                ? parent.getVariable(name)
                : null;
    }

    public boolean containsLocal(String name) {
        return variables.containsKey(name);
    }

    public void setVariable(String name, Cell cell) {
        variables.put(name, cell);
    }

    public StackFrame getParent() {
        return parent;
    }
}
