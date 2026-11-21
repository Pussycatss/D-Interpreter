package pussycats.interpreter.runtime;

import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.SourceLocation;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.lexer.TokenType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class Interpreter {
    private final StackFrame globalFrame;
    private final List<StackFrame> frameStack;

    public Interpreter() {
        this.globalFrame = new StackFrame(null, SourceLocation.UNKNOWN);
        this.frameStack = new ArrayList<>();
        this.frameStack.add(globalFrame);
    }

    public Object interpret(Node node) {
        if (node == null) {
            return null;
        }

        return switch (node) {
            case Expression expr -> interpretExpression(expr);
            case Statement stmt -> interpretStatement(stmt);
        };
    }

    // Type-specific interpretation methods
    private Object interpretExpression(Expression expr) {
        return switch (expr) {
            case Expression.IntLit i          -> new ImmutableCell(i.value(), i.loc());
            case Expression.RealLit r         -> new ImmutableCell(r.value(), r.loc());
            case Expression.StringLit s       -> new ImmutableCell(s.value(), s.loc());
            case Expression.BoolLit b         -> new ImmutableCell(b.value(), b.loc());
            case Expression.NoneLit n         -> new ImmutableCell(null, n.loc());
            case Expression.Ident id          -> getVariable(id);
            case Expression.Index idx         -> interpretIndex(idx);
            case Expression.Field fld         -> interpretField(fld);
            case Expression.FieldIndex fldIdx -> interpretFieldIndex(fldIdx);
            case Expression.Call c            -> interpretCall(c);
            case Expression.Unary u           -> interpretUnary(u);
            case Expression.Binary b          -> interpretBinary(b);
            case Expression.IsCheck c         -> interpretIsCheck(c);
            case Expression.ArrayLit arr      -> interpretArrayLit(arr);
            case Expression.TupleLit t        -> interpretTupleLit(t);
            case Expression.FuncLit f         -> interpretFuncLit(f);
        };
    }

    private Object getVariable(Expression.Ident id) {
        Cell v = frameStack.getLast().getVariable(id.name());
        return (v == null)
                ? fail(id, "Undefined variable: " + id.name())
                : v;
    }

    private Object interpretIndex(Expression.Index idx) {
        Cell targetCell = evaluate(idx.target());
        Cell indexCell = evaluate(idx.index());
        if (!(targetCell.get() instanceof ArrayCell arr)) {
            return fail(idx, "Cannot index this type");
        }

        Object indexObj = indexCell.get();
        int i;
        try {
            i = switch (indexObj) {
                case BigInteger b  -> b.intValueExact();
                case Integer n     -> n;
                case null, default -> throw new ArithmeticException();
            };
        } catch (ArithmeticException e) {
            return fail(idx, "Index must be an integer");
        }
        if (i < 1) {
            return fail(idx, "Index must be >= 1");
        }
        if (i > arr.size()) {
            arr.growTo(i, idx.loc());
        }
        return arr.get(i - 1);
    }

    private Object interpretField(Expression.Field fld) {
        Cell targetCell = evaluate(fld.target());
        if (!(targetCell.get() instanceof TupleCell tuple)) {
            return fail(fld, "Cannot access field on this type");
        }

        TupleCell.Entry entry = tuple.getByName(fld.name());
        if (entry == null) {
            return fail(fld, "Field not found: " + fld.name());
        }
        return entry.cell();
    }

    private Object interpretFieldIndex(Expression.FieldIndex fldIdx) {
        Cell targetCell = evaluate(fldIdx.target());
        if (!(targetCell.get() instanceof TupleCell tuple)) {
            return fail(fldIdx, "Cannot index this type");
        }

        int i = fldIdx.index();
        if (i < 1 || i > tuple.size()) {
            return fail(fldIdx, "Index out of bounds");
        }
        return tuple.get(i - 1).cell();
    }

    private Object interpretCall(Expression.Call c) {
        Cell calleeCell = evaluate(c.callee());
        if (!(calleeCell.get() instanceof FunctionCell(
                List<Expression.Parameter> params, Node body, StackFrame closure
        ))) {
            return fail(c, "Cannot call this type");
        }
        if (params.size() != c.args().size()) {
            return fail(c, "Argument count mismatch");
        }

        List<Cell> argCells = new ArrayList<>(params.size());
        for (Expression arg : c.args()) {
            Cell argValue = evaluate(arg);
            argCells.add(new MutableCell(argValue.get(), arg.loc()));
        }
        StackFrame newFrame = new StackFrame(closure, c.loc());
        frameStack.add(newFrame);
        try {
            for (int i = 0; i < params.size(); i++) {
                newFrame.setVariable(params.get(i).name(), argCells.get(i));
            }
            Object result = interpret(body);
            if (result instanceof Signal.ReturnSignal(Cell valueCell, SourceLocation _)) {
                return new ImmutableCell(valueCell.get(), c.loc());
            }
            Object raw = (result instanceof Cell cell) ? cell.get() : result;
            return new ImmutableCell(raw, c.loc());
        } finally {
            frameStack.removeLast();
        }
    }

    private Object interpretUnary(Expression.Unary u) {
        Cell operandCell = evaluate(u.operand());
        Object operand = operandCell.get();

        Object result = switch (u.op()) {
            case MINUS -> switch (operand) {
                case BigInteger n -> n.negate();
                case Double d -> -d;
                case null, default -> fail(u, "Cannot negate this type");
            };
            case PLUS -> switch (operand) {
                case BigInteger n -> n;
                case Double d -> d;
                case null, default -> fail(u, "Cannot apply unary plus to this type");
            };
            case NOT -> switch (operand) {
                case Boolean b -> !b;
                default -> fail(u, "Cannot apply not to this type");
            };
            default -> fail(u, "Unknown unary operator");
        };
        return new ImmutableCell(result, u.loc());
    }

    private Object interpretBinary(Expression.Binary b) {
        Cell leftCell = evalOperand(b.left());
        Object left = leftCell.get();

        if (b.op() == TokenType.AND || b.op() == TokenType.OR) {
            if (!(left instanceof Boolean lb)) {
                return fail(b, "Cannot apply " + b.op() + " to these types");
            }
            if (b.op() == TokenType.AND && !lb) {
                return new ImmutableCell(Boolean.FALSE, b.loc());
            }
            if (b.op() == TokenType.OR && lb) {
                return new ImmutableCell(Boolean.TRUE, b.loc());
            }
        }

        Cell rightCell = evalOperand(b.right());
        Object right = rightCell.get();

        Object result = switch (b.op()) {
            case PLUS, MINUS, ASTERISK, SLASH -> arithmetic(b, left, right);
            case LESS, LESS_EQUAL, GREATER, GREATER_EQUAL -> compare(b, left, right);
            case EQUAL -> valuesEqual(left, right, b);
            case NOT_EQUAL -> !valuesEqual(left, right, b);
            case AND, OR, XOR -> logic(b, left, right);
            default -> fail(b, "Unknown binary operator");
        };
        return new ImmutableCell(result, b.loc());
    }

    private Cell evalOperand(Expression e) {
        return evaluate(e);
    }

    private Object arithmetic(Expression.Binary b, Object left, Object right) {
        TokenType op = b.op();
        if (op == TokenType.PLUS) {
            if (left instanceof String l && right instanceof String r) {
                return l + r;
            }
            if (left instanceof ArrayCell l && right instanceof ArrayCell r) {
                List<Cell> merged = new ArrayList<>(l.size() + r.size());
                merged.addAll(l.elements());
                merged.addAll(r.elements());
                return new ArrayCell(merged);
                // the merged ArrayCell shares the element cells with l and r.
                // Given c = a + b, c[0] = 99 would also change a[0].
            }
            if (left instanceof TupleCell(List<TupleCell.Entry> entries1) &&
                    right instanceof TupleCell(List<TupleCell.Entry> entries)) {
                List<TupleCell.Entry> merged = new ArrayList<>(entries1.size() + entries.size());
                merged.addAll(entries1);
                merged.addAll(entries);
                return new TupleCell(merged);
            }
        }
        if (!(left instanceof Number l) || !(right instanceof Number r)) {
            return fail(b, "Cannot apply " + op + " to these types");
        }
        boolean divisionByZero = switch (right) {
            case BigInteger n -> n.signum() == 0;
            case Double d -> d == 0.0;
            default -> false;
        };
        if (op == TokenType.SLASH && divisionByZero) {
            return fail(b, "Division by zero");
        }
        if (l instanceof BigInteger x && r instanceof BigInteger y) {
            return switch (op) {
                case PLUS -> x.add(y);
                case MINUS -> x.subtract(y);
                case ASTERISK -> x.multiply(y);
                default -> x.divide(y);
            };
        }
        double x = l.doubleValue();
        double y = r.doubleValue();
        return switch (op) {
            case PLUS -> x + y;
            case MINUS -> x - y;
            case ASTERISK -> x * y;
            default -> x / y;
        };
    }

    private Object compare(Expression.Binary b, Object left, Object right) {
        int cmp;
        switch (left) {
            case BigInteger l when right instanceof BigInteger r -> cmp = l.compareTo(r);
            case Number l when right instanceof Number r -> cmp = Double.compare(l.doubleValue(), r.doubleValue());
            case String l when right instanceof String r -> cmp = l.compareTo(r);
            case null, default -> {
                return fail(b, "Cannot compare these types");
            }
        }
        return switch (b.op()) {
            case LESS -> cmp < 0;
            case LESS_EQUAL -> cmp <= 0;
            case GREATER -> cmp > 0;
            default -> cmp >= 0;
        };
    }

    private boolean valuesEqual(Object left, Object right, Node node) {
        // TODO: Decide if we throw an error or not
        if (left == null || right == null) {
            return fail(node, "Value 'none' cannot be used as an operand of equality");
        }
        if (left instanceof FunctionCell || right instanceof FunctionCell) {
            return fail(node, "Function cannot be used as an operand of equality");
        }
        if (left instanceof BigInteger l && right instanceof BigInteger r) {
            return l.equals(r);
        }
        if (left instanceof Number l && right instanceof Number r) {
            return l.doubleValue() == r.doubleValue();
        }
        return Objects.equals(left, right);
    }

    private Object logic(Expression.Binary b, Object left, Object right) {
        if (left instanceof Boolean l && right instanceof Boolean r) {
            return switch (b.op()) {
                case AND -> l && r;
                case OR -> l || r;
                default -> l ^ r;
            };
        }
        return fail(b, "Cannot apply " + b.op() + " to these types");
    }

    private Object interpretIsCheck(Expression.IsCheck c) {
        Cell targetCell = evaluate(c.target());
        Object target = targetCell.get();

        Boolean result = switch (c.type()) {
            case NONE   -> target == null;
            case BOOL   -> target instanceof Boolean;
            case INT    -> target instanceof BigInteger;
            case REAL   -> target instanceof Double;
            case STRING -> target instanceof String;
            case VECTOR  -> target instanceof ArrayCell;
            case TUPLE  -> target instanceof TupleCell;
            case FUNC   -> target instanceof FunctionCell;
        };
        return new ImmutableCell(result, c.loc());
    }

    private Object interpretArrayLit(Expression.ArrayLit arr) {
        List<Cell> elements = new ArrayList<>(arr.elements().size());
        for (Expression elem : arr.elements()) {
            Cell elemCell = evaluate(elem);
            elements.add(new MutableCell(elemCell.get(), elem.loc()));
        }
        return new ImmutableCell(new ArrayCell(elements), arr.loc());
    }

    private Object interpretTupleLit(Expression.TupleLit t) {
        List<TupleCell.Entry> entries = new ArrayList<>(t.entries().size());
        for (Expression.TupleEntry entry : t.entries()) {
            Cell cell = evaluate(entry.value());
            entries.add(new TupleCell.Entry(
                    entry.name(),
                    new MutableCell(cell.get(), entry.value().loc())));
        }
        return new ImmutableCell(new TupleCell(entries), t.loc());
    }

    private Object interpretFuncLit(Expression.FuncLit f) {
        StackFrame closure = frameStack.getLast();
        return new ImmutableCell(new FunctionCell(f.params(), f.body(), closure), f.loc());
    }

    private Object interpretStatement(Statement stmt) {
        return switch (stmt) {
            case Statement.VarDecl vd -> interpretVarDecl(vd);
            case Statement.VarDeclGroup vg -> interpretVarDeclGroup(vg);
            case Statement.Assign a -> interpretAssign(a);
            case Statement.PrintStatement p -> interpretPrintStatement(p);
            case Statement.IfStatement i -> interpretIfStatement(i);
            case Statement.WhileStatement w -> interpretWhileStatement(w);
            case Statement.LoopStatement l -> interpretLoopStatement(l);
            case Statement.ForRangeStatement f -> interpretForRangeStatement(f);
            case Statement.ForInStatement f -> interpretForInStatement(f);
            case Statement.ExitStatement _ -> interpretExitStatement();
            case Statement.ReturnStatement r -> interpretReturnStatement(r);
            case Statement.Block b -> interpretBlock(b);
        };
    }

    private Object interpretVarDecl(Statement.VarDecl vd) {
        StackFrame frame = frameStack.getLast();
        if (frame.containsLocal(vd.name())) {
            return fail(vd, "Variable already declared in this scope: " + vd.name());
        }

        Object value = null;
        SourceLocation valueLoc = vd.loc();
        if (vd.init() != null) {
            Cell initCell = evaluate(vd.init());
            value = initCell.get();
            valueLoc = vd.init().loc();
        }

        frame.setVariable(vd.name(), new MutableCell(value, valueLoc));
        return new ImmutableCell(null, vd.loc());
    }

    private Object interpretVarDeclGroup(Statement.VarDeclGroup vg) {
        for (Statement.VarDecl vd : vg.decls()) {
            interpretVarDecl(vd);
        }
        return new ImmutableCell(null, vg.loc());
    }

    private Object interpretAssign(Statement.Assign a) {
        if (a.target() instanceof Expression.Call) {
            return fail(a.target(), "Cannot assign to function call result");
        }
        Cell target = evaluate(a.target());
        Cell rhs = evaluate(a.value());
        if (!target.isMutable()) {
            return fail(a, "Cannot assign to an immutable value");
        }
        target.set(rhs.get());
        return null;
    }

    private Object interpretPrintStatement(Statement.PrintStatement p) {
        List<Expression> values = p.values();
        StringJoiner sj = new StringJoiner(" ");
        for (Expression expr : values) {
            Cell cell = evaluate(expr);
            sj.add(formatValue(cell.get()));
        }
        System.out.println(sj);
        return null;
    }

    private String formatValue(Object value) {
        return switch (value) {
            case null -> "none";
            case FunctionCell _ -> "<function>";
            case ArrayCell arr -> {
                StringJoiner sj = new StringJoiner(", ", "[", "]");
                for (Cell cell : arr.elements()) {
                    sj.add(formatValue(cell.get()));
                }
                yield sj.toString();
            }
            case TupleCell tuple -> {
                StringJoiner sj = new StringJoiner(", ", "{", "}");
                for (TupleCell.Entry entry : tuple.entries()) {
                    String v = formatValue(entry.cell().get());
                    sj.add(entry.name() == null
                            ? v
                            : entry.name() + " := " + v);
                }
                yield sj.toString();
            }
            case String s -> "\"" + s + "\"";
            default -> String.valueOf(value);
        };
    }

    private Object interpretIfStatement(Statement.IfStatement i) {
        Cell condCell = evaluate(i.cond());
        if (!(condCell.get() instanceof Boolean cond)) {
            return fail(i.cond(), "Non-boolean condition in if statement");
        }

        if (cond) {
            return interpretStatement(i.then());
        }
        if (i.otherwise() != null) {
            return interpretStatement(i.otherwise());
        }
        return null;
    }

    private Object interpretWhileStatement(Statement.WhileStatement w) {
        while (true) {
            Cell condCell = evaluate(w.cond());
            if (!(condCell.get() instanceof Boolean cond)) {
                return fail(w.cond(), "Non-boolean condition in while statement");
            }
            if (!cond) {
                break;
            }

            Object result = interpretBlock(w.body());
            if (result instanceof Signal.ExitSignal) {
                break;
            }
            if (result instanceof Signal) {
                return result;
            }
        }
        return null;
    }

    private Object interpretLoopStatement(Statement.LoopStatement l) {
        while (true) {
            Object result = interpretBlock(l.body());
            if (result instanceof Signal.ExitSignal) {
                break;
            }
            if (result instanceof Signal) {
                return result;
            }
        }
        return null;
    }

    private Object interpretForRangeStatement(Statement.ForRangeStatement f) {
        Cell fromCell = evaluate(f.from());
        Cell toCell = evaluate(f.to());
        if (!(fromCell.get() instanceof BigInteger from) ||
                !(toCell.get() instanceof BigInteger to)) {
            return fail(f, "For range bounds must be integers");
        }
        StackFrame outer = frameStack.getLast();
        for (BigInteger i = from; i.compareTo(to) <= 0; i = i.add(BigInteger.ONE)) {
            StackFrame loopFrame = new StackFrame(outer, f.loc());
            frameStack.add(loopFrame);
            try {
                loopFrame.setVariable(f.var(), new MutableCell(i, f.loc()));
                Object result = interpretBlock(f.body());
                if (result instanceof Signal.ExitSignal) {
                    break;
                }
                if (result instanceof Signal) {
                    return result;
                }
            } finally {
                frameStack.removeLast();
            }
        }
        return null;
    }

    private Object interpretForInStatement(Statement.ForInStatement f) {
        Cell iterableCell = evaluate(f.iterable());
        if (!(iterableCell.get() instanceof Iterable<?> elements)) {
            return fail(f, "Expression in for-in statement must be iterable");
        }

        StackFrame outer = frameStack.getLast();
        for (Object element : elements) {
            StackFrame loopFrame = new StackFrame(outer, f.loc());
            frameStack.add(loopFrame);
            try {
                Object value = element instanceof Cell cell ? cell.get() : element;
                loopFrame.setVariable(f.var(), new MutableCell(value, f.loc()));

                Object result = interpretBlock(f.body());
                if (result instanceof Signal.ExitSignal) break;
                if (result instanceof Signal) return result;
            } finally {
                frameStack.removeLast();
            }
        }
        return null;
    }

    private Object interpretExitStatement() {
        return new Signal.ExitSignal();
    }

    private Object interpretReturnStatement(Statement.ReturnStatement r) {
        Expression value = r.value();
        SourceLocation loc = r.loc();
        if (value == null) {
            return new Signal.ReturnSignal(new ImmutableCell(null, loc), loc);
        }

        Cell valueCell = evaluate(value);
        return new Signal.ReturnSignal(valueCell, loc);
    }

    private Object interpretBlock(Statement.Block b) {
        StackFrame newFrame = new StackFrame(frameStack.getLast(), b.loc());
        frameStack.add(newFrame);
        try {
            for (Statement statement : b.statements()) {
                Object result = interpretStatement(statement);
                if (result instanceof Signal) {
                    return result;
                }
            }
            return null;
        } finally {
            frameStack.removeLast();
        }
    }

    private Cell evaluate(Expression expr) {
        Object result = interpretExpression(expr);
        return (Cell) result;
    }

    private <T> T fail(Node n, String message) {
        throw new InterpreterException(n.loc(), message);
    }
}
