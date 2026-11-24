package pussycats.interpreter.runtime;

import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.SourceLocation;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.lexer.TokenType;

import java.math.BigInteger;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Interpreter {
    private final StackFrame globalFrame;
    private StackFrame currentFrame;
    private final Deque<ExecutionContext> executionContexts;
    private final ModuleLoader moduleLoader;
    private final Map<Path, StackFrame> modules = new HashMap<>();
    private final Set<Path> loadingModules = new HashSet<>();
    private static long executionSteps = 0;

    private static final int MAX_CALL_DEPTH = 256;
    private static final long MAX_EXECUTION_STEPS = 10_000_000;

    public Interpreter(String filename, ModuleLoader moduleLoader) {
        this.globalFrame = new StackFrame();
        this.currentFrame = globalFrame;
        this.executionContexts = new ArrayDeque<>();
        this.executionContexts.addLast(new ExecutionContext("<module>", captureSourceName(filename), null));
        this.moduleLoader = Objects.requireNonNull(moduleLoader);
    }

    public Object interpret(Node node) {
        if (node == null) {
            return null;
        }

        if (++executionSteps > MAX_EXECUTION_STEPS) {
            return fail(node, "Maximum execution steps exceeded: " + MAX_EXECUTION_STEPS);
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
        Cell v = currentFrame.getVariable(id.name());
        return (v == null)
                ? fail(id, "Undefined variable: " + id.name())
                : v;
    }

    private Object interpretIndex(Expression.Index idx) {
        Cell targetCell = evaluate(idx.target());
        if (!(targetCell.get() instanceof ArrayCell arr)) {
            return fail(idx, "Cannot index this type");
        }
        int i = evaluateIndex(idx);
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
                String name, List<Expression.Parameter> params, Node body, StackFrame closure, String sourceName))) {
            return fail(c, "Cannot call this type");
        }
        if (params.size() != c.args().size()) {
            return fail(c, "Argument count mismatch");
        }
        if (executionContexts.size() >= MAX_CALL_DEPTH) {
            return fail(c, "Maximum function call depth exceeded: " + MAX_CALL_DEPTH);
        }

        List<Cell> argCells = new ArrayList<>(params.size());
        for (Expression arg : c.args()) {
            Cell argValue = evaluate(arg);
            argCells.add(new MutableCell(argValue.get(), arg.loc()));
        }
        StackFrame previous = currentFrame;
        currentFrame = new StackFrame(closure);
        CallStackFrame callSite = new CallStackFrame(
                currentContext().functionName(),
                c.span(),
                currentContext().sourceName()
        );
        pushContext(name, sourceName, callSite);
        try {
            for (int i = 0; i < params.size(); i++) {
                currentFrame.setVariable(params.get(i).name(), argCells.get(i));
            }
            Object result = interpret(body);
            if (result instanceof Signal.ReturnSignal(Cell valueCell, SourceLocation _)) {
                return new ImmutableCell(valueCell.get(), c.loc());
            }
            Object raw = (result instanceof Cell cell) ? cell.get() : result;
            return new ImmutableCell(raw, c.loc());
        } finally {
            popContext();
            currentFrame = previous;
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
            if (left instanceof TupleCell l && right instanceof TupleCell r) {
                List<TupleCell.Entry> merged = new ArrayList<>(l.size() + r.size());
                Set<String> names = new HashSet<>();
                for (TupleCell.Entry entry : l.entries()) {
                    if (entry.name() != null && !names.add(entry.name())) {
                        return fail(b, "Duplicate tuple field: " + entry.name());
                    }
                    merged.add(entry);
                }
                for (TupleCell.Entry entry : r.entries()) {
                    if (entry.name() != null && !names.add(entry.name())) {
                        return fail(b, "Duplicate tuple field: " + entry.name());
                    }
                    merged.add(entry);
                }
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
        if (left == null || right == null) {
            return fail(node, "Value 'none' cannot be used as an operand of equality");
        }
        if (left instanceof BigInteger l && right instanceof BigInteger r) {
            return l.equals(r);
        }
        if (left instanceof Number l && right instanceof Number r) {
            return l.doubleValue() == r.doubleValue();
        }
        return fail(node, "Equality comparison is only supported for numbers");
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
        return interpretFuncLit(f, "<anonymous>");
    }

    private Object interpretFuncLit(Expression.FuncLit f, String name) {
        StackFrame closure = currentFrame;
        String definingSource = currentContext().sourceName();
        return new ImmutableCell(new FunctionCell(name, f.params(), f.body(), closure, definingSource), f.loc());
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
            case Statement.Import i -> interpretImport(i);
        };
    }

    private Object interpretVarDecl(Statement.VarDecl vd) {
        StackFrame frame = currentFrame;
        if (frame.containsLocal(vd.name())) {
            return fail(vd, "Variable already declared in this scope: " + vd.name());
        }

        Object value = null;
        SourceLocation valueLoc = vd.loc();
        if (vd.init() != null) {
            Cell initCell;
            if (vd.init() instanceof Expression.FuncLit f) {
                initCell = (ImmutableCell) interpretFuncLit(f, vd.name());
            } else {
                initCell = evaluate(vd.init());
            }
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
        Cell target = evaluateAssignmentTarget(a.target());
        Cell rhs = evaluate(a.value());
        if (!target.isMutable()) {
            return fail(a, "Cannot assign to an immutable value");
        }
        target.set(rhs.get());
        return null;
    }

    private Cell evaluateAssignmentTarget(Expression expr) {
        if (!(expr instanceof Expression.Index idx)) {
            return evaluate(expr);
        }
        Cell targetCell = resolveIndexTarget(idx.target());
        if (!(targetCell.get() instanceof ArrayCell arr)) {
            return fail(idx, "Cannot index this type");
        }
        int i = evaluateIndex(idx);
        if (i > arr.size()) {
            arr.growTo(i, idx.loc());
        }
        return arr.get(i - 1);
    }

    private Cell resolveIndexTarget(Expression expr) {
        if (!(expr instanceof Expression.Index)) {
            return evaluate(expr);
        }
        Cell cell = evaluateAssignmentTarget(expr);
        if (cell.get() == null) {
            if (!cell.isMutable()) {
                return fail(expr, "Cannot create an array in an immutable value");
            }
            cell.set(new ArrayCell(new ArrayList<>()));
        }
        if (!(cell.get() instanceof ArrayCell)) {
            return fail(expr, "Cannot index this type");
        }
        return cell;
    }

    private int evaluateIndex(Expression.Index idx) {
        Cell indexCell = evaluate(idx.index());
        Object indexObj = indexCell.get();
        int i;
        try {
            i = switch (indexObj) {
                case BigInteger b -> b.intValueExact();
                case Integer n -> n;
                case null, default -> throw new ArithmeticException();
            };
        } catch (ArithmeticException e) {
            return fail(idx, "Index must be an integer");
        }
        if (i < 1) {
            return fail(idx, "Index must be >= 1");
        }
        return i;
    }

    private Object interpretPrintStatement(Statement.PrintStatement p) {
        List<Expression> values = p.values();
        StringJoiner sj = new StringJoiner(" ");
        for (Expression expr : values) {
            Cell cell = evaluate(expr);
            sj.add(formatValue(cell.get(), new IdentityHashMap<>()));
        }
        System.out.println(sj);
        return null;
    }

    private String formatValue(Object value, IdentityHashMap<Object, Boolean> active) {
        return switch (value) {
            case null -> "none";
            case FunctionCell _ -> "<function>";
            case ArrayCell arr -> {
                if (active.containsKey(arr)) {
                    yield "<cycle>";
                }
                active.put(arr, Boolean.TRUE);
                try {
                    StringJoiner sj = new StringJoiner(", ", "[", "]");
                    for (Cell cell : arr.elements()) {
                        sj.add(formatValue(cell.get(), active));
                    }
                    yield sj.toString();
                } finally {
                    active.remove(arr);
                }
            }
            case TupleCell tuple -> {
                if (active.containsKey(tuple)) {
                    yield "<cycle>";
                }
                active.put(tuple, Boolean.TRUE);
                try {
                    StringJoiner sj = new StringJoiner(", ", "{", "}");
                    for (TupleCell.Entry entry : tuple.entries()) {
                        String v = formatValue(entry.cell().get(), active);
                        sj.add(entry.name() == null
                                ? v
                                : entry.name() + " := " + v);
                    }
                    yield sj.toString();
                } finally {
                    active.remove(tuple);
                }
            }
            case String s -> s;
            default -> String.valueOf(value);
        };
    }

    private Object interpretIfStatement(Statement.IfStatement i) {
        Cell condCell = evaluate(i.cond());
        if (!(condCell.get() instanceof Boolean cond)) {
            return fail(i.cond(), "Non-boolean condition in if statement");
        }

        if (cond) {
            return interpret(i.then());
        }
        if (i.otherwise() != null) {
            return interpret(i.otherwise());
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
        StackFrame outer = currentFrame;
        for (BigInteger i = from; i.compareTo(to) <= 0; i = i.add(BigInteger.ONE)) {
            StackFrame previous = currentFrame;
            currentFrame = new StackFrame(outer);
            try {
                currentFrame.setVariable(f.var(), new MutableCell(i, f.loc()));
                Object result = interpretBlock(f.body());
                if (result instanceof Signal.ExitSignal) {
                    break;
                }
                if (result instanceof Signal) {
                    return result;
                }
            } finally {
                currentFrame = previous;
            }
        }
        return null;
    }

    private Object interpretForInStatement(Statement.ForInStatement f) {
        Cell iterableCell = evaluate(f.iterable());
        if (!(iterableCell.get() instanceof Iterable<?> elements)) {
            return fail(f, "Expression in for-in statement must be iterable");
        }

        List<Object> snapshot = new ArrayList<>();
        elements.forEach(snapshot::add);

        StackFrame outer = currentFrame;
        for (Object element : snapshot) {
            StackFrame previous = currentFrame;
            currentFrame = new StackFrame(outer);
            try {
                Object value = switch (element) {
                    case Cell cell -> cell.get();
                    case TupleCell.Entry entry -> entry.cell().get();
                    default -> element;
                };
                currentFrame.setVariable(f.var(), new MutableCell(value, f.loc()));

                Object result = interpretBlock(f.body());
                if (result instanceof Signal.ExitSignal) break;
                if (result instanceof Signal) return result;
            } finally {
                currentFrame = previous;
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
        StackFrame previous = currentFrame;
        currentFrame = new StackFrame(previous);
        try {
            for (Statement statement : b.statements()) {
                Object result = interpret(statement);
                if (result instanceof Signal) {
                    return result;
                }
            }
            return null;
        } finally {
            currentFrame = previous;
        }
    }

    private Object interpretImport(Statement.Import i) {
        Path path = resolveModulePath(i.path());
        StackFrame moduleFrame = modules.get(path);
        if (moduleFrame == null) {
            if (!loadingModules.add(path)) {
                return fail(i, "Circular module import: " + path);
            }
            try {
                Statement.Block program = moduleLoader.load(path);
                StackFrame previous = currentFrame;
                moduleFrame = new StackFrame();
                currentFrame = moduleFrame;
                pushContext("<module>", path.toString(), null);
                try {
                    for (Statement statement : program.statements()) {
                        if (!(statement instanceof Statement.VarDecl
                                || statement instanceof Statement.VarDeclGroup
                                || statement instanceof Statement.Import)) {
                            continue;
                        }
                        Object result = interpret(statement);
                        if (result instanceof Signal) {
                            return fail(i, "Unexpected control flow in module: " + path);
                        }
                    }
                } finally {
                    popContext();
                    currentFrame = previous;
                }
                modules.put(path, moduleFrame);
            } catch (ModuleLoadException e) {
                throw e.wrap(new CallStackFrame(
                        currentContext().functionName(),
                        i.span(),
                        currentContext().sourceName()));
            } catch (InterpretationException e) {
                List<CallStackFrame> traceback = new ArrayList<>(e.getCallStack().size() + 1);
                traceback.add(new CallStackFrame(
                        currentContext().functionName(),
                        i.span(),
                        currentContext().sourceName()));
                traceback.addAll(e.getCallStack());
                throw new InterpretationException(e.getSpan(), e.getMessage(), traceback);
            } catch (RuntimeException e) {
                fail(i, e.getMessage());
            } finally {
                loadingModules.remove(path);
            }
        }
        StackFrame current = currentFrame;
        for (String name : i.names()) {
            if (current.containsLocal(name)) {
                return fail(i, "Variable already declared in this scope: " + name);
            }
            Cell imported = moduleFrame.getLocalVariable(name);
            if (imported == null) {
                return fail(i, "Module '" + path + "' does not define '" + name + "'");
            }
            current.setVariable(name, imported);
        }
        return null;
    }

    private Path resolveModulePath(String rawPath) {
        Path path = Paths.get(rawPath);
        if (!path.isAbsolute()) {
            String currentSource = currentContext().sourceName();
            if (!currentSource.equals(CallStackFrame.UNKNOWN_SOURCE)) {
                Path currentFile = Paths.get(currentSource);
                Path parent = currentFile.getParent();
                if (parent != null) {
                    path = parent.resolve(path);
                }
            }
        }
        return path.toAbsolutePath().normalize();
    }

    static String captureSourceName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return CallStackFrame.UNKNOWN_SOURCE;
        }
        if (raw.startsWith("<") && raw.endsWith(">")) {
            return raw;
        }
        boolean windowsDrive = raw.length() >= 3
                && Character.isLetter(raw.charAt(0))
                && raw.charAt(1) == ':'
                && (raw.charAt(2) == '\\' || raw.charAt(2) == '/');
        if (!windowsDrive && raw.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*")) {
            return raw;
        }
        try {
            return Paths.get(raw).toAbsolutePath().normalize().toString();
        } catch (InvalidPathException e) {
            return raw;
        }
    }

    private record ExecutionContext(
            String functionName,
            String sourceName,
            CallStackFrame callSite
    ) {}

    private ExecutionContext currentContext() {
        return executionContexts.getLast();
    }

    private void pushContext(String functionName, String sourceName, CallStackFrame callSite) {
        executionContexts.addLast(
                new ExecutionContext(functionName, sourceName, callSite)
        );
    }

    private void popContext() {
        executionContexts.removeLast();
    }

    private Cell evaluate(Expression expr) {
        Object result = interpret(expr);
        return (Cell) result;
    }

    private <T> T fail(Node n, String message) {
        List<CallStackFrame> traceback = new ArrayList<>();
        Iterator<ExecutionContext> iterator = executionContexts.iterator();
        if (iterator.hasNext()) {
            iterator.next();
        }
        while (iterator.hasNext()) {
            CallStackFrame callSite = iterator.next().callSite();
            if (callSite != null) {
                traceback.add(callSite);
            }
        }
        ExecutionContext context = currentContext();
        traceback.add(new CallStackFrame(
                context.functionName(),
                n.span(),
                context.sourceName()
        ));
        throw new InterpretationException(n.span(), message, traceback);
    }
}
