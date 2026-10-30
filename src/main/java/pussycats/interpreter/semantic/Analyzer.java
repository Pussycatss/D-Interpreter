package pussycats.interpreter.semantic;

import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;

import java.util.*;

public class Analyzer {

    public final List<String> errors = new ArrayList<>();
    private final Deque<Set<String>> scopes = new ArrayDeque<>();
    private int loopDepth = 0;

    public Analyzer() {
        scopes.push(new HashSet<>());
    }

    public void analyze(Node node) {
        if (node == null) {
            return;
        }
        switch (node) {
            case Statement.Block b -> {
                scopes.push(new HashSet<>());
                b.statements().forEach(this::analyze);
                scopes.pop();
            }
            case Statement.VarDecl v -> {
                checkVariableDeclaration(v);
                analyze(v.init());
            }
            case Statement.ExitStatement e -> checkExitStatement(e);
            case Statement.ReturnStatement r -> {
                checkReturnStatement(r);
                analyze(r.value());
            }
            case Statement.ForInStatement f -> {
                loopDepth++;
                scopes.push(new HashSet<>());
                if (f.var() != null) {
                    scopes.getFirst().add(f.var());
                }
                analyze(f.iterable());
                analyze(f.body());
                scopes.pop();
                loopDepth--;
            }
            case Statement.ForRangeStatement f -> {
                loopDepth++;
                scopes.push(new HashSet<>());
                if (f.var() != null) {
                    scopes.getFirst().add(f.var());
                }
                analyze(f.from());
                analyze(f.to());
                analyze(f.body());
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
                analyze(i.then());
                analyze(i.otherwise());
            }
            case Statement.Assign a -> {
                analyze(a.target());
                analyze(a.value());
            }
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
            case Expression.TupleLit t -> t.entries().forEach(e -> analyze(e.value()));
            case Expression.FuncLit f -> analyze(f.body());
            case Expression.Index idx -> {
                analyze(idx.target());
                analyze(idx.index());
            }
            case Expression.Field fld -> analyze(fld.target());
            case Expression.FieldIndex fld -> analyze(fld.target());
            case Expression.IsCheck isc -> analyze(isc.target());
            default -> {}
        }
    }

    private void checkVariableDeclaration(Statement.VarDecl v) {
        // TODO: Check if v.name() already exists in current scope (top of the scopes stack)
    }

    private void checkVariableUsage(Expression.Ident i) {
        // TODO: Check if i.name() exists in ANY scope in the scopes stack
    }

    private void checkExitStatement(Statement.ExitStatement e) {
        // TODO: Check if loopDepth == 0, then throw an error
    }

    private void checkReturnStatement(Statement.ReturnStatement r) {
        // TODO: Check if loopDepth == 0, then throw an error
    }
}
