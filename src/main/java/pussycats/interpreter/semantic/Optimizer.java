package pussycats.interpreter.semantic;

import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.lexer.TokenType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Optimizer {

    private static final int MAX_PASSES = 32;

    public Node optimize(Node node) {
        if (node == null) {
            return null;
        }

        return switch (node) {
            case Expression.Binary b -> optimizeBinary(b);
            case Expression.Unary u -> optimizeUnary(u);
            case Statement.IfStatement i -> optimizeIf(i);
            case Statement.Block b -> optimizeBlock(b);
            case Statement.VarDecl v -> new Statement.VarDecl(v.name(), (Expression) optimize(v.init()));
            case Statement.VarDeclGroup g -> new Statement.VarDeclGroup(
                    g.decls().stream().map(d -> (Statement.VarDecl) optimize(d)).toList());
            case Statement.ExpressionStatement e -> new Statement.ExpressionStatement(
                    (Expression) optimize(e.expression()));
            case Statement.Assign a -> new Statement.Assign((Expression) optimize(a.target()),
                    (Expression) optimize(a.value()));
            case Statement.PrintStatement p -> new Statement.PrintStatement(
                    p.values().stream().map(v -> (Expression) optimize(v)).toList());
            case Statement.WhileStatement w -> {
                Expression cond = (Expression) optimize(w.cond());
                // while false loop ... end -> nothing
                if (cond instanceof Expression.BoolLit(var value) && !value) {
                    yield new Statement.Block(List.of());
                }
                yield new Statement.WhileStatement(cond, (Statement.Block) optimize(w.body()));
            }
            case Statement.ForInStatement f -> new Statement.ForInStatement(f.var(),
                    (Expression) optimize(f.iterable()), (Statement.Block) optimize(f.body()));
            case Statement.ForRangeStatement f -> {
                Expression from = (Expression) optimize(f.from());
                Expression to = (Expression) optimize(f.to());
                // for i in 5..3 loop ... end -> nothing (assumes ranges only go upwards)
                if (from instanceof Expression.IntLit(var lo)
                        && to instanceof Expression.IntLit(var hi)
                        && lo.compareTo(hi) > 0) {
                    yield new Statement.Block(List.of());
                }
                yield new Statement.ForRangeStatement(f.var(), from, to, (Statement.Block) optimize(f.body()));
            }
            case Statement.LoopStatement l -> new Statement.LoopStatement((Statement.Block) optimize(l.body()));
            case Statement.ReturnStatement r -> new Statement.ReturnStatement((Expression) optimize(r.value()));
            case Expression.Call c -> new Expression.Call((Expression) optimize(c.callee()),
                    c.args().stream().map(a -> (Expression) optimize(a)).toList());
            case Expression.ArrayLit a -> new Expression.ArrayLit(
                    a.elements().stream().map(e -> (Expression) optimize(e)).toList());
            case Expression.TupleLit t -> new Expression.TupleLit(
                    t.entries().stream().map(e -> new Expression.TupleEntry(e.name(),
                            (Expression) optimize(e.value()))).toList());
            case Expression.Index idx -> new Expression.Index((Expression) optimize(idx.target()),
                    (Expression) optimize(idx.index()));
            case Expression.Field fld -> new Expression.Field((Expression) optimize(fld.target()), fld.name());
            case Expression.FieldIndex fld -> new Expression.FieldIndex((Expression) optimize(fld.target()),
                    fld.index());
            case Expression.IsCheck isc -> new Expression.IsCheck((Expression) optimize(isc.target()), isc.type());
            case Expression.FuncLit f -> new Expression.FuncLit(f.params(), optimize(f.body()), f.isBlock());
            default -> node;
        };
    }

    private Node optimizeBinary(Expression.Binary b) {
        Expression left = (Expression) optimize(b.left());
        Expression right = (Expression) optimize(b.right());

        Expression folded = foldConstants(b.op(), left, right);
        if (folded != null) {
            return folded;
        }
        Expression simplified = simplifyAlgebraic(b.op(), left, right);
        if (simplified != null) {
            return simplified;
        }
        return new Expression.Binary(left, b.op(), right);
    }

    /** Returns the folded literal, or null if the operands/operator can't be folded. */
    private Expression foldConstants(TokenType op, Expression left, Expression right) {
        if (left instanceof Expression.IntLit(var l) && right instanceof Expression.IntLit(var r)) {
            return foldInt(op, l, r);
        }
        if (left instanceof Expression.BoolLit(var l) && right instanceof Expression.BoolLit(var r)) {
            return foldBool(op, l, r);
        }
        if (left instanceof Expression.StringLit(var l) && right instanceof Expression.StringLit(var r)) {
            return op == TokenType.PLUS ? new Expression.StringLit(l + r) : null;
        }
        // Remaining numeric combinations: Int/Real, Real/Int, Real/Real
        Double l = asDouble(left);
        Double r = asDouble(right);
        if (l != null && r != null) {
            return foldReal(op, l, r);
        }
        return null;
    }

    private Expression foldInt(TokenType op, BigInteger l, BigInteger r) {
        return switch (op) {
            case PLUS -> new Expression.IntLit(l.add(r));
            case MINUS -> new Expression.IntLit(l.subtract(r));
            case ASTERISK -> new Expression.IntLit(l.multiply(r));
            // x / 0 is left for the runtime to report
            case SLASH -> r.signum() == 0 ? null : new Expression.IntLit(floorDiv(l, r));
            case LESS -> new Expression.BoolLit(l.compareTo(r) < 0);
            case LESS_EQUAL -> new Expression.BoolLit(l.compareTo(r) <= 0);
            case GREATER -> new Expression.BoolLit(l.compareTo(r) > 0);
            case GREATER_EQUAL -> new Expression.BoolLit(l.compareTo(r) >= 0);
            case EQUAL -> new Expression.BoolLit(l.equals(r));
            case NOT_EQUAL -> new Expression.BoolLit(!l.equals(r));
            default -> null;
        };
    }

    private Expression foldReal(TokenType op, double l, double r) {
        return switch (op) {
            case PLUS -> realOrNull(l + r);
            case MINUS -> realOrNull(l - r);
            case ASTERISK -> realOrNull(l * r);
            case SLASH -> r == 0.0 ? null : realOrNull(l / r);
            case LESS -> new Expression.BoolLit(l < r);
            case LESS_EQUAL -> new Expression.BoolLit(l <= r);
            case GREATER -> new Expression.BoolLit(l > r);
            case GREATER_EQUAL -> new Expression.BoolLit(l >= r);
            case EQUAL -> new Expression.BoolLit(l == r);
            case NOT_EQUAL -> new Expression.BoolLit(l != r);
            default -> null;
        };
    }

    private Expression foldBool(TokenType op, boolean l, boolean r) {
        return switch (op) {
            case AND -> new Expression.BoolLit(l && r);
            case OR -> new Expression.BoolLit(l || r);
            case XOR -> new Expression.BoolLit(l ^ r);
            default -> null;
        };
    }

    /**
     * Algebraic simplifications are disabled to avoid type conflicts with strings.
     * For example, "hello" + 0 should raise a type error at runtime, but if we
     * simplify it to "hello", the error disappears. Since we don't have type
     * information during optimization, we conservatively skip these transformations.
     */
    private Expression simplifyAlgebraic(TokenType op, Expression left, Expression right) {
        return null;
    }

    private Node optimizeUnary(Expression.Unary u) {
        Expression operand = (Expression) optimize(u.operand());

        if (u.op() == TokenType.PLUS
                && (operand instanceof Expression.IntLit || operand instanceof Expression.RealLit)) {
            return operand;
        }

        if (u.op() == TokenType.MINUS) {
            if (operand instanceof Expression.IntLit(var v)) {
                return new Expression.IntLit(v.negate());
            }
            if (operand instanceof Expression.RealLit(var v)) {
                return new Expression.RealLit(-v);
            }
            // -(-x) -> x
            if (operand instanceof Expression.Unary(var innerOp, var inner) && innerOp == TokenType.MINUS) {
                return inner;
            }
        }

        if (u.op() == TokenType.NOT) {
            if (operand instanceof Expression.BoolLit(var v)) {
                return new Expression.BoolLit(!v);
            }
            // not not x -> x
            if (operand instanceof Expression.Unary(var innerOp, var inner) && innerOp == TokenType.NOT) {
                return inner;
            }
        }

        return new Expression.Unary(u.op(), operand);
    }

    private Node optimizeIf(Statement.IfStatement i) {
        Expression cond = (Expression) optimize(i.cond());

        if (cond instanceof Expression.BoolLit(var value)) {
            if (value) {
                return optimize(i.then());
            }
            return i.otherwise() != null ? optimize(i.otherwise()) : new Statement.Block(List.of());
        }

        return new Statement.IfStatement(cond, (Statement) optimize(i.then()), (Statement) optimize(i.otherwise()));
    }

    private Node optimizeBlock(Statement.Block b) {
        List<Statement> result = new ArrayList<>();

        for (Statement stmt : b.statements()) {
            Statement opt = (Statement) optimize(stmt);

            // Flatten nested blocks, but only if they declare nothing
            if (opt instanceof Statement.Block inner && !declaresVariables(inner)) {
                result.addAll(inner.statements());
            } else {
                result.add(opt);
            }

            // Everything after return/exit is unreachable
            if (terminates(opt)) {
                break;
            }
        }

        return new Statement.Block(result);
    }

    private boolean declaresVariables(Statement.Block block) {
        return block.statements().stream()
                .anyMatch(s -> s instanceof Statement.VarDecl || s instanceof Statement.VarDeclGroup);
    }

    private boolean terminates(Statement s) {
        if (s instanceof Statement.ReturnStatement || s instanceof Statement.ExitStatement) {
            return true;
        }
        return switch (s) {
            case Statement.Block b -> b.statements().stream().anyMatch(this::terminates);
            case Statement.IfStatement i ->
                    i.otherwise() != null && terminates(i.then()) && terminates(i.otherwise());
            default -> false;
        };
    }

    private boolean isPure(Expression e) {
        if (e == null
                || e instanceof Expression.IntLit || e instanceof Expression.RealLit
                || e instanceof Expression.BoolLit || e instanceof Expression.StringLit
                || e instanceof Expression.NoneLit || e instanceof Expression.Ident
                || e instanceof Expression.FuncLit) {
            return true;
        }
        return switch (e) {
            case Expression.Unary u -> isPure(u.operand());
            case Expression.Binary b -> isPure(b.left()) && isPure(b.right())
                    // division may fail with "division by zero"
                    && (b.op() != TokenType.SLASH || isNonZeroNumber(b.right()));
            case Expression.IsCheck isc -> isPure(isc.target());
            case Expression.ArrayLit a -> a.elements().stream().allMatch(this::isPure);
            case Expression.TupleLit t -> t.entries().stream().allMatch(en -> isPure(en.value()));
            default -> false; // may have effects or fail
        };
    }

    public Set<String> collectUsed(Node node) {
        Set<String> used = new HashSet<>();
        collectUsed(node, used);
        return used;
    }

    private void collectUsed(Node node, Set<String> used) {
        if (node == null) return;

        switch (node) {
            case Expression.Ident i -> used.add(i.name());
            case Statement.VarDecl v -> collectUsed(v.init(), used);
            case Statement.VarDeclGroup g -> g.decls().forEach(d -> collectUsed(d, used));
            case Statement.Assign a -> {
                collectUsed(a.value(), used);
                // `x := ...` alone is not a use of x, but `t[i] := ...` / `t.f := ...` do use t (and i)
                if (!(a.target() instanceof Expression.Ident)) {
                    collectUsed(a.target(), used);
                }
            }
            case Expression.Binary b -> {
                collectUsed(b.left(), used);
                collectUsed(b.right(), used);
            }
            case Expression.Unary u -> collectUsed(u.operand(), used);
            case Expression.Call c -> {
                collectUsed(c.callee(), used);
                c.args().forEach(arg -> collectUsed(arg, used));
            }
            case Expression.ArrayLit a -> a.elements().forEach(e -> collectUsed(e, used));
            case Expression.TupleLit t -> t.entries().forEach(e -> collectUsed(e.value(), used));
            case Expression.Index idx -> {
                collectUsed(idx.target(), used);
                collectUsed(idx.index(), used);
            }
            case Expression.Field f -> collectUsed(f.target(), used);
            case Expression.FieldIndex f -> collectUsed(f.target(), used);
            case Expression.IsCheck isc -> collectUsed(isc.target(), used);
            case Expression.FuncLit f -> collectUsed(f.body(), used);
            case Statement.Block b -> b.statements().forEach(s -> collectUsed(s, used));
            case Statement.IfStatement i -> {
                collectUsed(i.cond(), used);
                collectUsed(i.then(), used);
                collectUsed(i.otherwise(), used);
            }
            case Statement.WhileStatement w -> {
                collectUsed(w.cond(), used);
                collectUsed(w.body(), used);
            }
            case Statement.LoopStatement l -> collectUsed(l.body(), used);
            case Statement.ForInStatement f -> {
                collectUsed(f.iterable(), used);
                collectUsed(f.body(), used);
            }
            case Statement.ForRangeStatement f -> {
                collectUsed(f.from(), used);
                collectUsed(f.to(), used);
                collectUsed(f.body(), used);
            }
            case Statement.ReturnStatement r -> collectUsed(r.value(), used);
            case Statement.PrintStatement p -> p.values().forEach(v -> collectUsed(v, used));
            case Statement.ExpressionStatement e -> collectUsed(e.expression(), used);
            default -> {
            }
        }
    }

    public Node removeUnusedVariables(Node node, Set<String> used) {
        if (node == null) return null;

        return switch (node) {
            case Statement.Block b -> {
                List<Statement> kept = new ArrayList<>();
                for (Statement stmt : b.statements()) {
                    Statement s = (Statement) removeUnusedVariables(stmt, used);
                    switch (s) {
                        case Statement.VarDecl v -> {
                            if (isNeeded(v, used)) kept.add(v);
                        }
                        case Statement.VarDeclGroup g -> {
                            List<Statement.VarDecl> keptDecls = g.decls().stream()
                                    .filter(d -> isNeeded(d, used)).toList();
                            if (keptDecls.size() == 1) {
                                kept.add(keptDecls.getFirst());
                            } else if (!keptDecls.isEmpty()) {
                                kept.add(new Statement.VarDeclGroup(keptDecls));
                            }
                        }
                        case Statement.Assign a -> {
                            if (!isDeadAssign(a, used)) kept.add(a);
                        }
                        case Statement.ExpressionStatement e -> {
                            if (!isPure(e.expression())) kept.add(e);
                        }
                        default -> kept.add(s);
                    }
                }
                yield new Statement.Block(kept);
            }
            case Statement.IfStatement i -> new Statement.IfStatement(
                    (Expression) removeUnusedVariables(i.cond(), used),
                    (Statement) removeUnusedVariables(i.then(), used),
                    (Statement) removeUnusedVariables(i.otherwise(), used));
            case Statement.WhileStatement w -> new Statement.WhileStatement(
                    (Expression) removeUnusedVariables(w.cond(), used),
                    (Statement.Block) removeUnusedVariables(w.body(), used));
            case Statement.LoopStatement l -> new Statement.LoopStatement(
                    (Statement.Block) removeUnusedVariables(l.body(), used));
            case Statement.ForInStatement f -> new Statement.ForInStatement(f.var(),
                    (Expression) removeUnusedVariables(f.iterable(), used),
                    (Statement.Block) removeUnusedVariables(f.body(), used));
            case Statement.ForRangeStatement f -> new Statement.ForRangeStatement(f.var(),
                    (Expression) removeUnusedVariables(f.from(), used),
                    (Expression) removeUnusedVariables(f.to(), used),
                    (Statement.Block) removeUnusedVariables(f.body(), used));
            case Statement.VarDecl v -> new Statement.VarDecl(v.name(),
                    (Expression) removeUnusedVariables(v.init(), used));
            case Statement.VarDeclGroup g -> new Statement.VarDeclGroup(
                    g.decls().stream().map(d -> (Statement.VarDecl) removeUnusedVariables(d, used)).toList());
            case Statement.ExpressionStatement e -> new Statement.ExpressionStatement(
                    (Expression) removeUnusedVariables(e.expression(), used));
            case Statement.Assign a -> new Statement.Assign(
                    (Expression) removeUnusedVariables(a.target(), used),
                    (Expression) removeUnusedVariables(a.value(), used));
            case Statement.ReturnStatement r -> new Statement.ReturnStatement(
                    (Expression) removeUnusedVariables(r.value(), used));
            case Statement.PrintStatement p -> new Statement.PrintStatement(
                    p.values().stream().map(v -> (Expression) removeUnusedVariables(v, used)).toList());
            case Expression.Binary b -> new Expression.Binary(
                    (Expression) removeUnusedVariables(b.left(), used), b.op(),
                    (Expression) removeUnusedVariables(b.right(), used));
            case Expression.Unary u -> new Expression.Unary(u.op(),
                    (Expression) removeUnusedVariables(u.operand(), used));
            case Expression.Call c -> new Expression.Call(
                    (Expression) removeUnusedVariables(c.callee(), used),
                    c.args().stream().map(a -> (Expression) removeUnusedVariables(a, used)).toList());
            case Expression.ArrayLit a -> new Expression.ArrayLit(
                    a.elements().stream().map(e -> (Expression) removeUnusedVariables(e, used)).toList());
            case Expression.TupleLit t -> new Expression.TupleLit(
                    t.entries().stream().map(e -> new Expression.TupleEntry(e.name(),
                            (Expression) removeUnusedVariables(e.value(), used))).toList());
            case Expression.Index idx -> new Expression.Index(
                    (Expression) removeUnusedVariables(idx.target(), used),
                    (Expression) removeUnusedVariables(idx.index(), used));
            case Expression.Field f -> new Expression.Field(
                    (Expression) removeUnusedVariables(f.target(), used), f.name());
            case Expression.FieldIndex f -> new Expression.FieldIndex(
                    (Expression) removeUnusedVariables(f.target(), used), f.index());
            case Expression.IsCheck isc -> new Expression.IsCheck(
                    (Expression) removeUnusedVariables(isc.target(), used), isc.type());
            case Expression.FuncLit f -> new Expression.FuncLit(f.params(),
                    removeUnusedVariables(f.body(), used), f.isBlock());
            default -> node;
        };
    }

    /** A declaration stays if the name is read somewhere or the initializer may have side effects. */
    private boolean isNeeded(Statement.VarDecl v, Set<String> used) {
        return used.contains(v.name()) || !isPure(v.init());
    }

    private boolean isDeadAssign(Statement.Assign a, Set<String> used) {
        return a.target() instanceof Expression.Ident(String name) && !used.contains(name) && isPure(a.value());
    }

    /** Runs all passes until the AST stops changing */
    public Node runFullOptimization(Node ast) {
        if (ast == null) return null;

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            Node prev = ast;
            ast = optimize(ast);
            ast = removeUnusedVariables(ast, collectUsed(ast));
            if (ast.equals(prev)) {
                break;
            }
        }
        return ast;
    }

    private static Double asDouble(Expression e) {
        return switch (e) {
            case Expression.IntLit(var v) -> v.doubleValue();
            case Expression.RealLit(var v) -> v;
            default -> null;
        };
    }

    private static boolean isNonZeroNumber(Expression e) {
        Double d = asDouble(e);
        return d != null && d != 0.0;
    }

    private static boolean isInt(Expression e, BigInteger value) {
        return e instanceof Expression.IntLit(var v) && v.equals(value);
    }

    private static Expression realOrNull(double v) {
        return Double.isFinite(v) ? new Expression.RealLit(v) : null;
    }

    /** Integer division rounding down, as the language spec requires (BigInteger.divide truncates). */
    private static BigInteger floorDiv(BigInteger l, BigInteger r) {
        BigInteger[] qr = l.divideAndRemainder(r);
        boolean inexactAndOppositeSigns = qr[1].signum() != 0 && qr[1].signum() != r.signum();
        return inexactAndOppositeSigns ? qr[0].subtract(BigInteger.ONE) : qr[0];
    }
}