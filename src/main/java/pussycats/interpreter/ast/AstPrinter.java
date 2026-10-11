package pussycats.interpreter.ast;

public class AstPrinter {
    public String print(Node n) {
        StringBuilder sb = new StringBuilder();
        print(n, sb, 0);
        return sb.toString();
    }

    private void print(Node n, StringBuilder sb, int depth) {
        sb.repeat("  ", depth);
        switch (n) {
            case Statement.Block b -> {
                sb.append("Block\n");
                for (Statement s : b.statements()) print(s, sb, depth + 1);
            }
            case Statement.VarDecl v -> sb.append("VarDecl(").append(v.name()).append(v.init() == null ? "" : " := " + abbrev(v.init())).append(")\n");
            case Statement.PrintStatement p -> {
                sb.append("Print\n");
                for (Expression e : p.values()) {
                    print(e, sb, depth + 1);
                }
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
            case Expression.StringLit s -> sb.append("String(").append(s.value()).append(")\n");
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
            }
            case Expression.Call c -> {
                sb.append("Call\n");
                print(c.callee(), sb, depth + 1);
                for (Expression e : c.args()) {
                    print(e, sb, depth + 1);
                }
            }
            case Expression.Field f -> sb.append("Field(").append(f.name()).append(")\n");
            case Expression.FieldIndex f -> sb.append("FieldIndex(.").append(f.index()).append(")\n");
            case Expression.ArrayLit a -> {
                sb.append("Array\n");
                for (Expression e : a.elements()) {
                    print(e, sb, depth + 1);
                }
            }
            case Expression.TupleLit t -> {
                sb.append("Tuple\n");
                for (Expression.TupleEntry e : t.entries()) {
                    sb.repeat("  ", depth + 1).append("Entry(").append(e.name()).append(")\n");
                    print(e.value(), sb, depth + 2);
                }
            }
            default -> sb.append(n.getClass().getSimpleName()).append("\n");
        }
    }

    private String abbrev(Expression e) {
        return switch (e) {
            case Expression.IntLit i -> i.value().toString();
            case Expression.RealLit r -> Double.toString(r.value());
            case Expression.StringLit s -> "\"" + s.value() + "\"";
            case Expression.BoolLit b -> Boolean.toString(b.value());
            case Expression.Ident i -> i.name();
            case Expression.NoneLit ignored -> "none";
            default -> "<expression>";
        };
    }
}
