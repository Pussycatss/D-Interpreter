package pussycats.interpreter.semantic;

import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;

import java.util.*;

public class Analyzer {

    public final List<String> errors = new ArrayList<>();
    private final Deque<Scope> scopes = new ArrayDeque<>();
    private int loopDepth = 0;
    private int functionDepth = 0;

    private static final class Scope {
        final Set<String> names = new HashSet<>();
    }

    public Analyzer() {
        scopes.push(new Scope());
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
                checkAssignmentTarget(a.target());
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
                    f.params().forEach(param -> currentScope().names.add(param));
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
            case Expression.Field fld -> analyze(fld.target());
            case Expression.FieldIndex fld -> analyze(fld.target());
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

    private void checkVariableDeclaration(Statement.VarDecl v) {
        Scope scope = currentScope();
        if (scope.names.contains(v.name())) {
            errors.add("Duplicate declaration: Variable '" + v.name() + "' is already defined in this scope.");
            return;
        }
        scope.names.add(v.name());
    }

    private void checkVariableUsage(Expression.Ident i) {
        if (findScope(i.name()) == null) {
            errors.add("Semantic error: Identifier '" + i.name() + "' is used before declaration.");
        }
    }

    private void checkExitStatement(Statement.ExitStatement e) {
        if (loopDepth <= 0) {
            errors.add("Invalid usage: 'exit' statement is only allowed inside loops.");
        }
    }

    private void checkReturnStatement(Statement.ReturnStatement r) {
        if (functionDepth <= 0) {
            errors.add("Invalid usage: 'return' statement is only allowed inside functions.");
        }
    }

    private void checkAssignmentTarget(Expression target) {
        if (!(target instanceof Expression.Ident
                || target instanceof Expression.Index
                || target instanceof Expression.Field
                || target instanceof Expression.FieldIndex)) {
            errors.add("Invalid assignment target: Left-hand side of assignment must be a reference.");
        }
    }

    private void checkFunctionParameters(Expression.FuncLit f) {
        if (f.params() == null) {
            return;
        }
        Set<String> seenParams = new HashSet<>();
        for (String param : f.params()) {
            if (!seenParams.add(param)) {
                errors.add("Duplicate function parameter: '" + param + "' is defined multiple times.");
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
                errors.add("Duplicate tuple key: '" + entry.name() + "' is defined multiple times.");
            }
        }
    }
}
