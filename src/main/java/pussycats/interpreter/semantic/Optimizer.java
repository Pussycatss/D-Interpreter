package pussycats.interpreter.semantic;

import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Optimizer {

    public Node optimize(Node node) {
        if (node == null) {
            return null;
        }

        return switch (node) {
            case Expression.Binary b -> optimizeBinary(b);
            case Expression.Unary u -> optimizeUnary(u);
            case Statement.IfStatement i -> optimizeIf(i);
            case Statement.Block b -> optimizeBlock(b);
            case Statement.VarDecl v -> new Statement.VarDecl(v.name(),
                    (Expression) optimize(v.init()));
            case Statement.Assign a -> new Statement.Assign((Expression) optimize(a.target()),
                    (Expression) optimize(a.value()));
            case Statement.PrintStatement p -> new Statement.PrintStatement(p.values().stream().map(v ->
                    (Expression) optimize(v)).toList());
            case Statement.WhileStatement w -> new Statement.WhileStatement((Expression) optimize(w.cond()),
                    (Statement.Block) optimize(w.body()));
            case Statement.ForInStatement f -> new Statement.ForInStatement(f.var(),
                    (Expression) optimize(f.iterable()), (Statement.Block) optimize(f.body()));
            case Statement.ForRangeStatement f -> new Statement.ForRangeStatement(f.var(),
                    (Expression) optimize(f.from()), (Expression) optimize(f.to()),
                    (Statement.Block) optimize(f.body()));
            case Statement.LoopStatement l -> new Statement.LoopStatement((Statement.Block) optimize(l.body()));
            case Statement.ReturnStatement r -> new Statement.ReturnStatement((Expression) optimize(r.value()));
            case Expression.Call c -> new Expression.Call((Expression) optimize(c.callee()),
                    c.args().stream().map(a -> (Expression) optimize(a)).toList());
            case Expression.ArrayLit a -> new Expression.ArrayLit(a.elements().stream().map((e) ->
                    (Expression) optimize(e)).toList());
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
        Expression result = new Expression.Binary(left, b.op(), right);
        // TODO: Constant Folding
        //       If left is IntLit and right is IntLit:
        //          Perform math
        //       Don't forget about exceptions (for example, division by 0)
        return result;
    }

    private Node optimizeUnary(Expression.Unary u) {
        Expression operand = (Expression) optimize(u.operand());
        Expression result = new Expression.Unary(u.op(), operand);
        // TODO: Constant Folding
        //       -(-5) -> 5; not true -> false
        return result;
    }

    private Node optimizeIf(Statement.IfStatement i) {
        Expression cond = (Expression) optimize(i.cond());
        // TODO: If cond is a BoolLit(true), return i.then()
        //       If cond is a BoolLit(false), return i.otherwise() (or an empty block)
        //       And not just true/false, also comparison of constants
        return new Statement.IfStatement(cond, (Statement) optimize(i.then()), (Statement) optimize(i.otherwise()));
    }

    private Node optimizeBlock(Statement.Block b) {
        List<Statement> optimizedStatements = new ArrayList<>();

        for (Statement stmt : b.statements()) {
            Statement optStmt = (Statement) optimize(stmt);
            optimizedStatements.add(optStmt);
            // TODO: Dead Code Elimination (Unreachable code)
            //       If optStmt is a ReturnStatement or ExitStatement, break the loop
        }

        return new Statement.Block(optimizedStatements);
    }

    public Node removeUnusedVariables(Node node, Set<String> usedVariables) {
        // TODO: Traverse the tree. If you see a VarDecl, and v.name() is NOT in usedVariables, drop it from the Block.
        //       (Make sure you don't accidentally drop it if it has a side effect, like var x := print("hi"))
        return node;
    }
}
