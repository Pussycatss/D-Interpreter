package pussycats.interpreter.parser;

import org.junit.jupiter.api.Test;
import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.Token;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    /* ============ helpers ============ */

    private static Statement.Block parse(String src) {
        List<Token> tokens = Lexer.scan(src);
        Parser p = new Parser(tokens, src, "<test>");
        Node ast = p.parse();
        if (ast == null || p.hasErrors()) {
            throw new AssertionError(
                    "parse failed for: <" + src + ">\n" + p.formatErrors());
        }
        return (Statement.Block) ast;
    }

    private static List<Statement> stmts(String src) {
        return parse(src).statements();
    }

    private static Statement first(String src) {
        return stmts(src).getFirst();
    }

    /**
     * Parses a single expression by wrapping it in {@code print} and pulling
     * the value back out. Bare expressions are not statements in D, so this
     * is the simplest way to reuse the statement-parsing path.
     */
    private static Expression expr(String src) {
        var stmt = first("print " + src);
        var print = assertInstanceOf(Statement.PrintStatement.class, stmt,
                "expected a print statement, got " + stmt);
        assertEquals(1, print.values().size(),
                "expected one print value for: <" + src + ">");
        return print.values().getFirst();
    }

    /* ============ program structure ============ */

    @Test void emptySourceProducesEmptyBlock() {
        assertTrue(parse("").statements().isEmpty());
    }

    @Test void singleStatement() {
        assertEquals(1, stmts("print 42").size());
    }

    @Test void twoStatementsSeparatedByNewline() {
        assertEquals(2, stmts("print 1\nprint 2").size());
    }

    @Test void twoStatementsSeparatedBySemicolon() {
        assertEquals(2, stmts("print 1; print 2").size());
    }

    @Test void blankLinesBetweenStatements() {
        assertEquals(2, stmts("print 1\n\n\nprint 2").size());
    }

    @Test void trailingNewlineIsFine() {
        assertEquals(1, stmts("print 1\n").size());
    }

    /* ============ literals ============ */

    @Test void integerLiteral() {
        var e = assertInstanceOf(Expression.IntLit.class, expr("42"));
        assertEquals(new BigInteger("42"), e.value());
    }

    @Test void realLiteral() {
        var e = assertInstanceOf(Expression.RealLit.class, expr("3.14"));
        assertEquals(3.14, e.value(), 1e-9);
    }

    @Test void stringLiteral() {
        var e = assertInstanceOf(Expression.StringLit.class, expr("\"hi\""));
        assertEquals("hi", e.value());
    }

    @Test void trueAndFalse() {
        var t = assertInstanceOf(Expression.BoolLit.class, expr("true"));
        assertTrue(t.value());
        var f = assertInstanceOf(Expression.BoolLit.class, expr("false"));
        assertFalse(f.value());
    }

    @Test void noneLiteral() {
        assertInstanceOf(Expression.NoneLit.class, expr("none"));
    }

    @Test void identifier() {
        var e = assertInstanceOf(Expression.Ident.class, expr("x"));
        assertEquals("x", e.name());
    }

    @Test void parenthesizedExpression() {
        assertEquals(new BigInteger("1"), ((Expression.IntLit) expr("(1)")).value());
    }

    /* ============ arrays and tuples ============ */

    @Test void emptyArray() {
        var e = assertInstanceOf(Expression.ArrayLit.class, expr("[]"));
        assertTrue(e.elements().isEmpty());
    }

    @Test void arrayWithElements() {
        var e = assertInstanceOf(Expression.ArrayLit.class, expr("[1, 2, 3]"));
        assertEquals(3, e.elements().size());
    }

    @Test void tupleWithNamedAndUnnamed() {
        var e = assertInstanceOf(Expression.TupleLit.class, expr("{a := 1, 2.5}"));
        assertEquals(2, e.entries().size());
        assertEquals("a", e.entries().get(0).name());
        assertNull(e.entries().get(1).name());
    }

    /* ============ precedence and associativity ============ */

    @Test void multiplicationBindsTighterThanAddition() {
        // 1 + 2 * 3  ==  Binary(+, Int(1), Binary(*, Int(2), Int(3)))
        var top = assertInstanceOf(Expression.Binary.class, expr("1 + 2 * 3"));
        assertEquals(pussycats.interpreter.lexer.TokenType.PLUS, top.op());
        assertInstanceOf(Expression.IntLit.class, top.left());
        var right = assertInstanceOf(Expression.Binary.class, top.right());
        assertEquals(pussycats.interpreter.lexer.TokenType.ASTERISK, right.op());
    }

    @Test void additionAndSubtractionAreLeftAssociative() {
        // 1 - 2 - 3  ==  Binary(-, Binary(-, Int(1), Int(2)), Int(3))
        var top = assertInstanceOf(Expression.Binary.class, expr("1 - 2 - 3"));
        assertInstanceOf(Expression.Binary.class, top.left());
        assertInstanceOf(Expression.IntLit.class, top.right());
    }

    @Test void andOrXorAreAtTheSameLevel() {
        // "a and b or c" is left-associative: ((a and b) or c)
        var top = assertInstanceOf(Expression.Binary.class, expr("a and b or c"));
        assertEquals(pussycats.interpreter.lexer.TokenType.OR, top.op());
        var left = assertInstanceOf(Expression.Binary.class, top.left());
        assertEquals(pussycats.interpreter.lexer.TokenType.AND, left.op());
    }

    @Test void unaryMinus() {
        var e = assertInstanceOf(Expression.Unary.class, expr("-x"));
        assertEquals(pussycats.interpreter.lexer.TokenType.MINUS, e.op());
    }

    @Test void notIsUnary() {
        assertInstanceOf(Expression.Unary.class, expr("not a"));
    }

    @Test void comparisonBindsLooserThanArithmetic() {
        // 1 + 2 < 3 * 4  ==  Binary(<, Binary(+, 1, 2), Binary(*, 3, 4))
        var top = assertInstanceOf(Expression.Binary.class, expr("1 + 2 < 3 * 4"));
        assertEquals(pussycats.interpreter.lexer.TokenType.LESS, top.op());
    }

    @Test void isCheck() {
        var e = assertInstanceOf(Expression.IsCheck.class, expr("x is int"));
        assertEquals(pussycats.interpreter.ast.TypeIndicator.INT, e.type());
    }

    /* ============ reference chain (postfix) ============ */

    @Test void index() {
        var e = assertInstanceOf(Expression.Index.class, expr("t[1]"));
        assertInstanceOf(Expression.Ident.class, e.target());
    }

    @Test void callWithNoArgs() {
        var e = assertInstanceOf(Expression.Call.class, expr("f()"));
        assertTrue(e.args().isEmpty());
    }

    @Test void callWithArgs() {
        var e = assertInstanceOf(Expression.Call.class, expr("f(1, 2)"));
        assertEquals(2, e.args().size());
    }

    @Test void fieldAccess() {
        var e = assertInstanceOf(Expression.Field.class, expr("t.a"));
        assertEquals("a", e.name());
    }

    @Test void fieldIndex() {
        var e = assertInstanceOf(Expression.FieldIndex.class, expr("t.2"));
        assertEquals(2, e.index());
    }

    @Test void chainedPostfix() {
        // t.a.b[1](x, 2)
        var call = assertInstanceOf(Expression.Call.class, expr("t.a.b[1](x, 2)"));
        var idx  = assertInstanceOf(Expression.Index.class, call.callee());
        var b    = assertInstanceOf(Expression.Field.class, idx.target());
        var a    = assertInstanceOf(Expression.Field.class, b.target());
        var t    = assertInstanceOf(Expression.Ident.class, a.target());
        assertEquals("t", t.name());
        assertEquals("a", a.name());
        assertEquals("b", b.name());
    }

    /* ============ var declarations ============ */

    @Test void varWithoutInit() {
        var d = assertInstanceOf(Statement.VarDecl.class, first("var x"));
        assertEquals("x", d.name());
        assertNull(d.init());
    }

    @Test void varWithInit() {
        var d = assertInstanceOf(Statement.VarDecl.class, first("var x := 1"));
        assertInstanceOf(Expression.IntLit.class, d.init());
    }

    @Test void varGroup() {
        var g = assertInstanceOf(Statement.VarDeclGroup.class, first("var a, b := 2, c"));
        assertEquals(3, g.decls().size());
        assertNull(g.decls().get(0).init());
        assertInstanceOf(Expression.IntLit.class, g.decls().get(1).init());
        assertNull(g.decls().get(2).init());
    }

    /* ============ assignment ============ */

    @Test void assignment() {
        var a = assertInstanceOf(Statement.Assign.class, first("x := 1"));
        assertInstanceOf(Expression.Ident.class, a.target());
        assertInstanceOf(Expression.IntLit.class, a.value());
    }

    @Test void assignmentToIndex() {
        var a = assertInstanceOf(Statement.Assign.class, first("t[1] := 2"));
        assertInstanceOf(Expression.Index.class, a.target());
    }

    @Test void assignmentToField() {
        var a = assertInstanceOf(Statement.Assign.class, first("t.a := 5"));
        assertInstanceOf(Expression.Field.class, a.target());
    }

    /* ============ if ============ */

    @Test void shortIf() {
        var i = assertInstanceOf(Statement.IfStatement.class, first("if x => print 1"));
        assertInstanceOf(Expression.Ident.class, i.cond());
        assertNull(i.otherwise());
    }

    @Test void shortIfBodyIsWrappedInBlock() {
        var i = assertInstanceOf(Statement.IfStatement.class, first("if x => print 1"));
        assertInstanceOf(Statement.Block.class, i.then());
    }

    @Test void longIfWithoutElse() {
        var i = assertInstanceOf(Statement.IfStatement.class,
                first("if x then print 1 end"));
        assertInstanceOf(Statement.Block.class, i.then());
        assertNull(i.otherwise());
    }

    @Test void longIfWithElse() {
        var i = assertInstanceOf(Statement.IfStatement.class,
                first("if x then print 1 else print 2 end"));
        assertNotNull(i.otherwise());
    }

    /* ============ loops ============ */

    @Test void whileLoop() {
        var w = assertInstanceOf(Statement.WhileStatement.class,
                first("while x < 10 loop print x end"));
        assertEquals(1, w.body().statements().size());
    }

    @Test void bareLoop() {
        var l = assertInstanceOf(Statement.LoopStatement.class,
                first("loop print 1; exit end"));
        assertEquals(2, l.body().statements().size());
    }

    @Test void exitInsideLoopIsFine() {
        assertInstanceOf(Statement.LoopStatement.class, first("loop exit end"));
    }

    /* ============ for ============ */

    @Test void forRangeWithVar() {
        var f = assertInstanceOf(Statement.ForRangeStatement.class,
                first("for i in 1..3 loop print i end"));
        assertEquals("i", f.var());
        assertInstanceOf(Expression.IntLit.class, f.from());
        assertInstanceOf(Expression.IntLit.class, f.to());
    }

    @Test void forRangeWithoutVar() {
        var f = assertInstanceOf(Statement.ForRangeStatement.class,
                first("for 1..3 loop print 1 end"));
        assertNull(f.var());
    }

    @Test void forInWithVar() {
        var f = assertInstanceOf(Statement.ForInStatement.class,
                first("for i in [1, 2, 3] loop print i end"));
        assertEquals("i", f.var());
        assertInstanceOf(Expression.ArrayLit.class, f.iterable());
    }

    @Test void bareForIsInfiniteLoop() {
        // `for loop ... end` parses as a LoopStatement (same as `loop`)
        assertInstanceOf(Statement.LoopStatement.class,
                first("for loop exit end"));
    }

    /* ============ functions ============ */

    @Test void funcWithArrowBody() {
        var f = assertInstanceOf(Expression.FuncLit.class, expr("func(x) => x + 1"));
        assertEquals(List.of("x"), f.params());
        assertFalse(f.isBlock());
        assertInstanceOf(Expression.Binary.class, f.body());
    }

    @Test void funcNoParams() {
        var f = assertInstanceOf(Expression.FuncLit.class, expr("func() => 1"));
        assertTrue(f.params().isEmpty());
    }

    @Test void funcWithBlockBody() {
        var f = assertInstanceOf(Expression.FuncLit.class,
                expr("func(n) is print n end"));
        assertTrue(f.isBlock());
        assertInstanceOf(Statement.Block.class, f.body());
    }

    /* ============ return and print ============ */

    @Test void returnWithoutValue() {
        var r = assertInstanceOf(Statement.ReturnStatement.class, first("return"));
        assertNull(r.value());
    }

    @Test void returnWithValue() {
        var r = assertInstanceOf(Statement.ReturnStatement.class, first("return 42"));
        assertInstanceOf(Expression.IntLit.class, r.value());
    }

    @Test void printSingle() {
        var p = assertInstanceOf(Statement.PrintStatement.class, first("print 1"));
        assertEquals(1, p.values().size());
    }

    @Test void printMultiple() {
        var p = assertInstanceOf(Statement.PrintStatement.class, first("print 1, 2, 3"));
        assertEquals(3, p.values().size());
    }
}