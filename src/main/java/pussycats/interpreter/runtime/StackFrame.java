package pussycats.interpreter.runtime;

import pussycats.interpreter.ast.SourceLocation;

import java.util.HashMap;
import java.util.Map;

public class StackFrame {
    private final Map<String, Cell> variables;
    private final StackFrame parent;

    public StackFrame(StackFrame parent, SourceLocation loc) {
        this.parent = parent;
        this.variables = new HashMap<>();
    }

    public StackFrame(SourceLocation loc) {
        this(null, loc);
    }
    
    public Cell getVariable(String name) {
        Cell cell = variables.get(name);
        if (cell != null) {
            return cell;
        }
        if (parent != null) {
            return parent.getVariable(name);
        }
        throw new RuntimeException("Undefined variable: " + name);
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
