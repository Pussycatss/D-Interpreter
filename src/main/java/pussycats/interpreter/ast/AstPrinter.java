package pussycats.interpreter.ast;

import pussycats.interpreter.lexer.TokenType;

import static pussycats.interpreter.lexer.TokenType.PLUS;

/**
 * Prints an AST as an indented tree, one node per line.
 *
 * Used for debugging and tests. Statements are printed as nested nodes,
 * while simple expressions inside them (conditions, ranges, iterables)
 * are shortened to a single line, e.g. {@code cond: (x > 5)}.
 */
public class AstPrinter {

    /**
     * Returns the tree text for the given node and all its children.
     *
     * @param n root node to print
     * @return multi-line string, indented by two spaces per level
     */
    public String print(Node n) {
        StringBuilder sb = new StringBuilder();
        print(n, sb, 0);
        return sb.toString();
    }

    /**
     * Appends one node and, recursively, its children to {@code sb}.
     *
     * @param depth current nesting level, used for indentation
     */
    private void print(Node n, StringBuilder sb, int depth) {
        indent(sb, depth);
        switch (n) {
            case Statement.Block b -> {
                sb.append("Block(").append(b.statements().size()).append(")\n");
                for (Statement s : b.statements()) print(s, sb, depth + 1);
            }
            case Statement.VarDecl v -> {
                sb.append("VarDecl(").append(v.name()).append(")\n");
                if (v.init() != null)
                    print(v.init(), sb, depth + 1);
            }
            case Statement.PrintStatement p -> {
                sb.append("Print(").append(p.values().size()).append(")\n");
                for (Expression e : p.values()) {
                    print(e, sb, depth + 1);
                }
            }
            case Statement.IfStatement s -> {
                sb.append("If\n");
                indent(sb, depth + 1);
                sb.append("cond: ").append(abbrev(s.cond())).append('\n');
                indent(sb, depth + 1);
                sb.append("then:\n");
                print(s.then(), sb, depth + 2);
                if (s.otherwise() != null) {
                    indent(sb, depth + 1); sb.append("else:\n");
                    print(s.otherwise(), sb, depth + 2);
                }
            }
            case Statement.Assign a -> {
                sb.append("Assign\n");
                indent(sb, depth + 1);
                sb.append("target: ").append(abbrev(a.target())).append('\n');
                indent(sb, depth + 1);
                sb.append("value: ").append(abbrev(a.value())).append('\n');
            }
            case Statement.WhileStatement w -> {
                sb.append("While\n");
                indent(sb, depth + 1);
                sb.append("cond: ").append(abbrev(w.cond())).append('\n');
                indent(sb, depth + 1);
                sb.append("body:\n");
                print(w.body(), sb, depth + 2);
            }
            case Statement.LoopStatement l -> {
                sb.append("Loop\n");
                print(l.body(), sb, depth + 1);
            }
            case Statement.ForRangeStatement f -> {
                // A loop without a variable name is printed as "_"
                sb.append("ForRange(").append(f.var() == null ? "_" : f.var()).append(")\n");
                indent(sb, depth + 1);
                sb.append("from: ").append(abbrev(f.from())).append('\n');
                indent(sb, depth + 1);
                sb.append("to:   ").append(abbrev(f.to())).append('\n');
                indent(sb, depth + 1);
                sb.append("body:\n");
                print(f.body(), sb, depth + 2);
            }
            case Statement.ForInStatement s -> {
                sb.append("ForIn(var ").append(s.var()).append(")\n");
                indent(sb, depth + 1);
                sb.append("iterable: ").append(abbrev(s.iterable())).append('\n');
                indent(sb, depth + 1);
                sb.append("body:\n");
                print(s.body(), sb, depth + 2);
            }
            case Expression.Binary b -> {
                sb.append("Binary(").append(b.op()).append(")\n");
                print(b.left(), sb, depth + 1);
                print(b.right(), sb, depth + 1);
            }
            case Expression.Unary u -> {
                sb.append("Unary(").append(u.op()).append(")\n");
                print(u.operand(), sb, depth + 1);
            }
            case Expression.IntLit i -> sb.append("Int(").append(i.value()).append(")\n");
            case Expression.RealLit r -> sb.append("Real(").append(r.value()).append(")\n");
            case Expression.StringLit s -> sb.append("String(\"").append(s.value()).append("\")\n");
            case Expression.BoolLit b -> sb.append("Bool(").append(b.value()).append(")\n");
            case Expression.NoneLit ignored -> sb.append("None\n");
            case Expression.Ident i -> sb.append("Ident(").append(i.name()).append(")\n");
            case Expression.IsCheck c -> {
                sb.append("Is(").append(c.type()).append(")\n");
                print(c.target(), sb, depth + 1);
            }
            case Expression.Index ix -> {
                sb.append("Index\n");
                print(ix.target(), sb, depth + 1);
                print(ix.index(), sb, depth + 1);
            }
            case Expression.Call c -> {
                // The number in brackets is the argument count
                sb.append("Call(").append(c.args().size()).append(")\n");
                print(c.callee(), sb, depth + 1);
                for (Expression e : c.args()) {
                    print(e, sb, depth + 1);
                }
            }
            case Expression.Field f -> {
                sb.append("Field(.").append(f.name()).append(")\n");
                print(f.target(), sb, depth + 1);
            }
            case Expression.FieldIndex f -> {
                sb.append("FieldIndex(.").append(f.index()).append(")\n");
                print(f.target(), sb, depth + 1);
            }
            case Expression.ArrayLit a -> {
                sb.append("Array\n");
                for (Expression e : a.elements()) {
                    print(e, sb, depth + 1);
                }
            }
            case Expression.TupleLit t -> {
                sb.append("Tuple\n");
                for (Expression.TupleEntry e : t.entries()) {
                    indent(sb, depth + 1);
                    sb.append("Entry(").append(e.name()).append(")\n");
                    print(e.value(), sb, depth + 2);
                }
            }
            case Expression.FuncLit f -> {
                sb.append("Func(");
                for (int i = 0; i < f.params().size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(f.params().get(i));
                }
                sb.append(") ");
                // "=> expr" form or "is ... end" block form
                switch (f.body()) {
                    case Expression e -> sb.append("=>\n");
                    case Statement.Block b -> sb.append("is ").append(b.statements().size())
                            .append(b.statements().size() == 1 ? " stmts" : " stmt").append(" end\n");
                    default -> sb.append("???\n");
                }
                print(f.body(), sb, depth + 1);
            }
            case Statement.ExitStatement e -> sb.append("Exit\n");
            case Statement.ExpressionStatement s -> {
                sb.append("ExpressionStatement\n");
                print(s.expression(), sb, depth + 1);
            }
            case Statement.ReturnStatement r -> {
                sb.append("Return\n");
                if (r.value() != null) {
                    print(r.value(), sb, depth + 1);
                }
            }
            case Statement.VarDeclGroup g -> {
                sb.append("VarDeclGroup(").append(g.decls().size()).append(")\n");
                for (Statement.VarDecl d : g.decls()) {
                    print(d, sb, depth + 1);
                }
            }
        }
    }

    /**
     * Converts an expression to a short one-line string, e.g. {@code (a + 1)}
     * or {@code f(x, y)}. Binary expressions are always wrapped in parentheses
     * so that the evaluation order is visible.
     *
     * Function bodies and tuple entries are shown in compact form.
     */
    private String abbrev(Expression e) {
        return switch (e) {
            case Expression.IntLit    i -> i.value().toString();
            case Expression.RealLit   r -> formatReal(r.value());
            case Expression.StringLit s -> "\"" + s.value() + "\"";
            case Expression.BoolLit   b -> Boolean.toString(b.value());
            case Expression.Ident     i -> i.name();
            case Expression.NoneLit   ignored -> "none";
            case Expression.Unary u -> switch (u.op()) {
                case MINUS -> "-" + abbrev(u.operand());
                case PLUS  -> "+" + abbrev(u.operand());
                case NOT   -> "not" + abbrev(u.operand());
                default    -> u.op() + " " + abbrev(u.operand());
            };
            case Expression.Binary b -> "(" + abbrev(b.left()) + " " + opSymbol(b.op()) + " " + abbrev(b.right()) + ")";
            case Expression.Index ix -> abbrev(ix.target()) + "[" + abbrev(ix.index()) + "]";
            case Expression.Call c -> {
                StringBuilder sb = new StringBuilder(abbrev(c.callee())).append('(');
                for (int i = 0; i < c.args().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(abbrev(c.args().get(i)));
                }
                yield sb.append(')').toString();
            }
            case Expression.Field f      -> abbrev(f.target()) + '.' + f.name();
            case Expression.FieldIndex f -> abbrev(f.target()) + '.' + f.index();
            case Expression.IsCheck c    -> "(" + abbrev(c.target()) + " is " + c.type() + ")";
            case Expression.ArrayLit a   -> {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < a.elements().size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(abbrev(a.elements().get(i)));
                }
                yield sb.append(']').toString();
            }
            case Expression.TupleLit t -> {
                StringBuilder sb = new StringBuilder("{");
                for (int i = 0; i < t.entries().size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    var entry = t.entries().get(i);
                    if (entry.name() != null) {
                        sb.append(entry.name()).append(" := ");
                        sb.append(abbrev(entry.value()));
                    }
                }
                yield sb.append('}').toString();
            }
            case Expression.FuncLit f -> {
                StringBuilder sb = new StringBuilder("func(");
                for (int i = 0; i < f.params().size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(f.params().get(i));
                }
                sb.append(") ");

                switch (f.body()) {
                    case Expression ex ->
                            sb.append("=> ").append(abbrev(ex));
                    case Statement.Block b ->
                            sb.append("is {").append(b.statements().size())
                                    .append(b.statements().size() == 1 ? " stmt" : " stmts").append("} end");
                    default -> sb.append("???");
                }
                yield sb.toString();
            }
        };
    }

    /** Formats a real number so that whole values keep a decimal part ({@code 2.0}, not {@code 2}). */
    private static String formatReal(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return d + ".0";
        }
        return Double.toString(d);
    }

    /** Maps an operator token to the symbol used in the source code; unknown operators fall back to the token name. */
    private static String opSymbol(TokenType op) {
        return switch (op) {
            case PLUS          -> "+";
            case MINUS         -> "-";
            case ASTERISK      -> "*";
            case SLASH         -> "/";
            case LESS          -> "<";
            case LESS_EQUAL    -> "<=";
            case GREATER       -> ">";
            case GREATER_EQUAL -> ">=";
            case EQUAL         -> "=";
            case NOT_EQUAL     ->  "/=";
            case AND           -> "and";
            case OR            -> "or";
            case XOR           -> "xor";
            default            -> op.name();
        };
    }

    private static void indent(StringBuilder sb, int depth) {
        sb.repeat("  ", depth);
    }
}
