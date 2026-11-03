package pussycats.interpreter.semantic;

import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.SourceLocation;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.error.ErrorReporter;

import java.util.*;

public class Analyzer {

    public final ErrorReporter reporter;
    private final Deque<Scope> scopes = new ArrayDeque<>();
    private int loopDepth = 0;
    private int functionDepth = 0;

    private static final class Scope {
        final Set<String> names = new HashSet<>();
        final Map<String, Integer> tupleSizes = new HashMap<>();
        final Map<String, Set<String>> tupleFields = new HashMap<>();
    }

    public Analyzer(String source, String filename) {
        this.reporter = new ErrorReporter(source, filename);
        scopes.push(new Scope());
    }

    public boolean hasErrors() {
        return reporter.hasErrors();
    }

    public String formatErrors() {
        return reporter.formatAll();
    }

    public void analyze(Node node) {
        if (node == null) {
            return;
        }
        switch (node) {
            case Statement.Block b -> {
                scopes.push(new Scope());
                b.statements().forEach(this::analyze);
                scopes.pop();
            }
            case Statement.VarDecl v -> {
                analyze(v.init());
                checkVariableDeclaration(v);
            }
            case Statement.VarDeclGroup group -> group.decls().forEach(this::analyze);
            case Statement.ExitStatement e -> checkExitStatement(e);
            case Statement.ReturnStatement r -> {
                checkReturnStatement(r);
                analyze(r.value());
            }
            case Statement.ForInStatement f -> {
                analyze(f.iterable());
                loopDepth++;
                scopes.push(new Scope());
                if (f.var() != null) {
                    currentScope().names.add(f.var());
                }
                f.body().statements().forEach(this::analyze);
                scopes.pop();
                loopDepth--;
            }
            case Statement.ForRangeStatement f -> {
                analyze(f.from());
                analyze(f.to());
                loopDepth++;
                scopes.push(new Scope());
                if (f.var() != null) {
                    currentScope().names.add(f.var());
                }
                f.body().statements().forEach(this::analyze);
                scopes.pop();
                loopDepth--;
            }
            case Statement.LoopStatement l -> {
                loopDepth++;
                analyze(l.body());
                loopDepth--;
            }
            case Statement.WhileStatement w -> {
                analyze(w.cond());
                loopDepth++;
                analyze(w.body());
                loopDepth--;
            }
            case Expression.Ident i -> checkVariableUsage(i);
            case Statement.IfStatement i -> {
                analyze(i.cond());
                scopes.push(new Scope());
                analyze(i.then());
                scopes.pop();
                if (i.otherwise() != null) {
                    scopes.push(new Scope());
                    analyze(i.otherwise());
                    scopes.pop();
                }
            }
            case Statement.Assign a -> {
                checkAssignmentTarget(a.target(), a.loc());
                if (a.target() instanceof Expression.Ident i) {
                    for (Scope scope : scopes) {
                        scope.tupleSizes.remove(i.name());
                        scope.tupleFields.remove(i.name());
                    }
                }
                analyze(a.target());
                analyze(a.value());
            }
            case Statement.ExpressionStatement es -> analyze(es.expression());
            case Statement.PrintStatement p -> p.values().forEach(this::analyze);
            case Expression.Binary b -> {
                analyze(b.left());
                analyze(b.right());
            }
            case Expression.Unary u -> analyze(u.operand());
            case Expression.Call c -> {
                analyze(c.callee());
                c.args().forEach(this::analyze);
            }
            case Expression.ArrayLit a -> a.elements().forEach(this::analyze);
            case Expression.TupleLit t -> {
                checkTupleKeys(t);
                t.entries().forEach(e -> analyze(e.value()));
            }
            case Expression.FuncLit f -> {
                checkFunctionParameters(f);

                int savedLoopDepth = loopDepth;
                loopDepth = 0;
                functionDepth++;

                scopes.push(new Scope());
                if (f.params() != null) {
                    f.params().forEach(param -> currentScope().names.add(param.name()));
                }
                analyze(f.body());
                scopes.pop();

                functionDepth--;
                loopDepth = savedLoopDepth;
            }
            case Expression.Index idx -> {
                analyze(idx.target());
                analyze(idx.index());
            }
            case Expression.Field fld -> {
                analyze(fld.target());
                checkTupleFieldAccess(fld);
            }
            case Expression.FieldIndex fld -> {
                analyze(fld.target());
                checkTupleFieldIndex(fld);
            }
            case Expression.IsCheck isc -> analyze(isc.target());
            default -> {
            }
        }
    }

    private Scope currentScope() {
        return scopes.getFirst();
    }

    private Scope findScope(String name) {
        for (Scope scope : scopes) {
            if (scope.names.contains(name)) {
                return scope;
            }
        }
        return null;
    }

    private Integer findTupleSize(String name) {
        for (Scope scope : scopes) {
            if (scope.tupleSizes.containsKey(name)) {
                return scope.tupleSizes.get(name);
            }
        }
        return null;
    }

    // ==========================================
    // Semantic Checks
    // ==========================================

    private void checkVariableDeclaration(Statement.VarDecl v) {
        Scope scope = currentScope();
        if (scope.names.contains(v.name())) {
            SourceLocation loc = v.loc() != null ? v.loc() : SourceLocation.UNKNOWN;
            reporter.report("Declaration error", "Duplicate declaration", "Variable '" + v.name() +
                            "' is already defined in this scope", loc, v.name().length());
            return;
        }
        scope.names.add(v.name());
        if (v.init() instanceof Expression.TupleLit t) {
            scope.tupleSizes.put(v.name(), t.entries().size());
            Set<String> fields = new HashSet<>();
            for (Expression.TupleEntry entry : t.entries()) {
                if (entry.name() != null) {
                    fields.add(entry.name());
                }
            }
            scope.tupleFields.put(v.name(), fields);
        }
    }

    private void checkVariableUsage(Expression.Ident i) {
        if (findScope(i.name()) == null) {
            SourceLocation loc = i.loc() != null ? i.loc() : SourceLocation.UNKNOWN;
            reporter.report("NameError", "Undeclared variable: '" + i.name() + "'", loc, i.name().length());
        }
    }

    private void checkExitStatement(Statement.ExitStatement e) {
        if (loopDepth <= 0) {
            SourceLocation loc = e.loc() != null ? e.loc() : SourceLocation.UNKNOWN;
            reporter.report("ContextError", "'exit' can only appear inside a loop", loc, 4);
        }
    }

    private void checkReturnStatement(Statement.ReturnStatement r) {
        if (functionDepth <= 0) {
            SourceLocation loc = r.loc() != null ? r.loc() : SourceLocation.UNKNOWN;
            reporter.report("ContextError", "'return' can only appear inside a function", loc, 6);
        }
    }

    private void checkAssignmentTarget(Expression target, SourceLocation loc) {
        if (!(target instanceof Expression.Ident
                || target instanceof Expression.Index
                || target instanceof Expression.Field
                || target instanceof Expression.FieldIndex)) {
            reporter.report("ReferenceError", "Invalid assignment target",
                    "the left side of ':=' must be a variable, array element, or tuple field", loc);
        }
    }

    private void checkFunctionParameters(Expression.FuncLit f) {
        if (f.params() == null) {
            return;
        }
        Set<String> seenParams = new HashSet<>();
        for (Expression.Parameter param : f.params()) {
            if (!seenParams.add(param.name())) {
                SourceLocation loc = param.loc();
                reporter.report("DeclarationError", "Duplicate function parameter", "'" + param.name() +
                                "' is defined multiple times", loc, param.name().length());
            }
        }
    }

    private void checkTupleKeys(Expression.TupleLit t) {
        if (t.entries() == null) {
            return;
        }
        Set<String> seenKeys = new HashSet<>();
        for (Expression.TupleEntry entry : t.entries()) {
            if (entry.name() != null && !seenKeys.add(entry.name())) {
                SourceLocation loc = entry.loc();
                reporter.report("DeclarationError", "Duplicate tuple key", "'" + entry.name() +
                        "' is defined multiple times", loc, entry.name().length());
            }
        }
    }

    private void checkTupleFieldAccess(Expression.Field fld) {
        if (fld.target() instanceof Expression.TupleLit tupleLit) {
            boolean found = tupleLit.entries().stream().anyMatch(e -> fld.name().equals(e.name()));
            if (!found) {
                reporter.report("IndexError", "Tuple does not have a field named '" + fld.name() + "'", fld.loc());
            }
        }
        else if (fld.target() instanceof Expression.Ident ident) {
            Set<String> fields = findTupleFields(ident.name());
            if (fields != null && !fields.contains(fld.name())) {
                reporter.report("IndexError", "Tuple '" + ident.name() + "' does not have a field named '" + fld.name() +
                        "'", fld.loc());
            }
        }
    }

    private Set<String> findTupleFields(String name) {
        for (Scope scope : scopes) {
            if (scope.tupleFields.containsKey(name)) {
                return scope.tupleFields.get(name);
            }
        }
        return null;
    }

    private void checkTupleFieldIndex(Expression.FieldIndex fld) {
        if (fld.target() instanceof Expression.TupleLit tupleLit) {
            int size = tupleLit.entries().size();
            if (fld.index() < 1 || fld.index() > size) {
                reporter.report("IndexError", "Tuple index out of bounds","size is " + size +
                        ", but index is " + fld.index(), fld.loc());
            }
        }
        if (fld.target() instanceof Expression.Ident i) {
            Integer size = findTupleSize(i.name());
            if (size != null && (fld.index() < 1 || fld.index() > size)) {
                reporter.report("IndexError", "Tuple '" + i.name() + "' index out of bounds", "size is " + size +
                        ", but index is " + fld.index(), fld.loc());
            }
        }
    }
}
