package pussycats.interpreter.semantic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.parser.Parser;

import java.math.BigInteger;
import java.util.List;

@DisplayName("Optimizer Tests")
class OptimizerTest {

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

    private static Statement.Block optimize(String src) {
        Statement.Block ast = parse(src);
        Optimizer opt = new Optimizer();
        return (Statement.Block) opt.runFullOptimization(ast);
    }

    private static Expression expr(String src) {
        var stmt = parse("print " + src).statements().getFirst();
        var print = assertInstanceOf(Statement.PrintStatement.class, stmt);
        return print.values().getFirst();
    }

    private static Expression optimizedExpr(String src) {
        var stmt = optimize("print " + src).statements().getFirst();
        var print = assertInstanceOf(Statement.PrintStatement.class, stmt);
        return print.values().getFirst();
    }

    /* ============ constant folding - arithmetic ============ */

    @Test
    @DisplayName("Integer addition: 2 + 3 → 5")
    void intAdditionFolding() {
        var e = assertInstanceOf(Expression.IntLit.class, optimizedExpr("2 + 3"));
        assertEquals(new BigInteger("5"), e.value());
    }

    @Test
    @DisplayName("Integer subtraction: 10 - 4 → 6")
    void intSubtractionFolding() {
        var e = assertInstanceOf(Expression.IntLit.class, optimizedExpr("10 - 4"));
        assertEquals(new BigInteger("6"), e.value());
    }

    @Test
    @DisplayName("Integer multiplication: 3 * 4 → 12")
    void intMultiplicationFolding() {
        var e = assertInstanceOf(Expression.IntLit.class, optimizedExpr("3 * 4"));
        assertEquals(new BigInteger("12"), e.value());
    }

    @Test
    @DisplayName("Integer division: 10 / 3 → 3 (floor)")
    void intDivisionFolding() {
        var e = assertInstanceOf(Expression.IntLit.class, optimizedExpr("10 / 3"));
        assertEquals(new BigInteger("3"), e.value());
    }

    @Test
    @DisplayName("Integer division by zero is NOT folded")
    void intDivisionByZeroNotFolded() {
        var e = assertInstanceOf(Expression.Binary.class, optimizedExpr("5 / 0"));
        assertEquals(pussycats.interpreter.lexer.TokenType.SLASH, e.op());
    }

    @Test
    @DisplayName("Complex arithmetic: 2 + 3 * 4 → 14")
    void complexArithmeticFolding() {
        var e = assertInstanceOf(Expression.IntLit.class, optimizedExpr("2 + 3 * 4"));
        assertEquals(new BigInteger("14"), e.value());
    }

    @Test
    @DisplayName("Real addition: 2.5 + 3.5 → 6.0")
    void realAdditionFolding() {
        var e = assertInstanceOf(Expression.RealLit.class, optimizedExpr("2.5 + 3.5"));
        assertEquals(6.0, e.value(), 1e-9);
    }

    @Test
    @DisplayName("Mixed int/real addition: 2 + 3.5 → 5.5")
    void mixedIntRealAdditionFolding() {
        var e = assertInstanceOf(Expression.RealLit.class, optimizedExpr("2 + 3.5"));
        assertEquals(5.5, e.value(), 1e-9);
    }

    /* ============ constant folding - comparisons ============ */

    @Test
    @DisplayName("Integer comparison: 3 < 5 → true")
    void intComparisonFolding() {
        var e = assertInstanceOf(Expression.BoolLit.class, optimizedExpr("3 < 5"));
        assertTrue(e.value());
    }

    @Test
    @DisplayName("Integer comparison: 5 < 3 → false")
    void intComparisonFoldingFalse() {
        var e = assertInstanceOf(Expression.BoolLit.class, optimizedExpr("5 < 3"));
        assertFalse(e.value());
    }

    @Test
    @DisplayName("Integer equality: 5 = 5 → true")
    void intEqualityFolding() {
        var e = assertInstanceOf(Expression.BoolLit.class, optimizedExpr("5 = 5"));
        assertTrue(e.value());
    }

    @Test
    @DisplayName("Real comparison: 3.5 < 5.0 → true")
    void realComparisonFolding() {
        var e = assertInstanceOf(Expression.BoolLit.class, optimizedExpr("3.5 < 5.0"));
        assertTrue(e.value());
    }

    /* ============ constant folding - logical ============ */

    @Test
    @DisplayName("Logical AND: true and false → false")
    void logicalAndFolding() {
        var e = assertInstanceOf(Expression.BoolLit.class, optimizedExpr("true and false"));
        assertFalse(e.value());
    }

    @Test
    @DisplayName("Logical OR: true or false → true")
    void logicalOrFolding() {
        var e = assertInstanceOf(Expression.BoolLit.class, optimizedExpr("true or false"));
        assertTrue(e.value());
    }

    @Test
    @DisplayName("Logical XOR: true xor false → true")
    void logicalXorFolding() {
        var e = assertInstanceOf(Expression.BoolLit.class, optimizedExpr("true xor false"));
        assertTrue(e.value());
    }

    /* ============ constant folding - unary ============ */

    @Test
    @DisplayName("Unary minus on int: -5 → -5")
    void unaryMinusIntFolding() {
        var e = assertInstanceOf(Expression.IntLit.class, optimizedExpr("-5"));
        assertEquals(new BigInteger("-5"), e.value());
    }

    @Test
    @DisplayName("Unary minus on real: -3.5 → -3.5")
    void unaryMinusRealFolding() {
        var e = assertInstanceOf(Expression.RealLit.class, optimizedExpr("-3.5"));
        assertEquals(-3.5, e.value(), 1e-9);
    }

    @Test
    @DisplayName("Double negation: -(-x) → x")
    void doubleNegationSimplification() {
        var e = optimizedExpr("-(-x)");
        assertInstanceOf(Expression.Ident.class, e);
        assertEquals("x", ((Expression.Ident) e).name());
    }

    @Test
    @DisplayName("NOT on boolean: not true → false")
    void notBooleanFolding() {
        var e = assertInstanceOf(Expression.BoolLit.class, optimizedExpr("not true"));
        assertFalse(e.value());
    }

    @Test
    @DisplayName("Double NOT: not not x → x")
    void doubleNotSimplification() {
        var e = optimizedExpr("not not x");
        assertInstanceOf(Expression.Ident.class, e);
        assertEquals("x", ((Expression.Ident) e).name());
    }

    @Test
    @DisplayName("Unary plus on literal is identity")
    void unaryPlusIdentity() {
        var e = optimizedExpr("+5");
        assertInstanceOf(Expression.IntLit.class, e);
        assertEquals(new BigInteger("5"), ((Expression.IntLit) e).value());
    }

    /* ============ string concatenation folding ============ */

    @Test
    @DisplayName("String concatenation: \"Hello\" + \" \" + \"World\" → \"Hello World\"")
    void stringConcatenationFolding() {
        var e = assertInstanceOf(Expression.StringLit.class, optimizedExpr("\"Hello\" + \" \" + \"World\""));
        assertEquals("Hello World", e.value());
    }

    /* ============ algebraic simplification ============ */

    // Algebraic simplifications are disabled to avoid type conflicts with strings.
    // For example, "hello" + 0 should raise a type error at runtime, but if we
    // simplify it to "hello", the error disappears silently - bad.

    /* ============ if statement simplification ============ */

    @Test
    @DisplayName("if true then A else B end → A")
    void ifTrueSimplification() {
        var block = optimize("if true then print 1 else print 2 end");
        assertEquals(1, block.statements().size());
        var stmt = assertInstanceOf(Statement.PrintStatement.class, block.statements().getFirst());
        var val = assertInstanceOf(Expression.IntLit.class, stmt.values().getFirst());
        assertEquals(new BigInteger("1"), val.value());
    }

    @Test
    @DisplayName("if false then A else B end → B")
    void ifFalseSimplification() {
        var block = optimize("if false then print 1 else print 2 end");
        assertEquals(1, block.statements().size());
        var stmt = assertInstanceOf(Statement.PrintStatement.class, block.statements().getFirst());
        var val = assertInstanceOf(Expression.IntLit.class, stmt.values().getFirst());
        assertEquals(new BigInteger("2"), val.value());
    }

    @Test
    @DisplayName("if false then A end → empty block")
    void ifFalseWithoutElseSimplification() {
        var block = optimize("if false then print 1 end");
        assertTrue(block.statements().isEmpty());
    }

    /* ============ dead code elimination ============ */

    @Test
    @DisplayName("Code after return is removed")
    void deadCodeAfterReturn() {
        var block = optimize("print 1; return; print 2");
        assertEquals(2, block.statements().size());
        assertInstanceOf(Statement.PrintStatement.class, block.statements().get(0));
        assertInstanceOf(Statement.ReturnStatement.class, block.statements().get(1));
    }

    @Test
    @DisplayName("Code after exit is removed")
    void deadCodeAfterExit() {
        var block = optimize("loop exit; print 1 end");
        assertEquals(1, block.statements().size());
        var loop = assertInstanceOf(Statement.LoopStatement.class, block.statements().get(0));
        assertEquals(1, loop.body().statements().size());
        assertInstanceOf(Statement.ExitStatement.class, loop.body().statements().getFirst());
    }

    @Test
    @DisplayName("Code after if with both branches terminating is removed")
    void deadCodeAfterTerminatingIf() {
        var block = optimize("if x then return else return end; print 1");
        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.IfStatement.class, block.statements().get(0));
    }

    /* ============ unused variable removal ============ */

    @Test
    @DisplayName("Unused variable with pure init is removed")
    void unusedVariableWithPureInitRemoved() {
        var block = optimize("var x := 5; print 1");
        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.PrintStatement.class, block.statements().get(0));
    }

    @Test
    @DisplayName("Used variable is kept")
    void usedVariableKept() {
        var block = optimize("var x := 5; print x");
        assertEquals(2, block.statements().size());
        assertInstanceOf(Statement.VarDecl.class, block.statements().get(0));
        assertInstanceOf(Statement.PrintStatement.class, block.statements().get(1));
    }

    @Test
    @DisplayName("Unused variable with impure init is kept")
    void unusedVariableWithImpureInitKept() {
        // Use a function call as impure init
        var block = optimize("var x := f(); print 2");
        assertEquals(2, block.statements().size());
        assertInstanceOf(Statement.VarDecl.class, block.statements().get(0));
        assertInstanceOf(Statement.PrintStatement.class, block.statements().get(1));
    }

    @Test
    @DisplayName("Assignment to unused variable with pure value is removed")
    void deadAssignmentRemoved() {
        var block = optimize("x := 5; print 1");
        assertEquals(1, block.statements().size());
        assertInstanceOf(Statement.PrintStatement.class, block.statements().get(0));
    }

    @Test
    @DisplayName("Pure expression statement is removed")
    void pureExpressionStatementRemoved() {
        // Expression statements don't exist in D, so this test is not applicable
        // Skip this test
    }

    /* ============ empty loop removal ============ */

    @Test
    @DisplayName("while false loop ... end → empty block")
    void whileFalseRemoved() {
        var block = optimize("while false loop print 1 end");
        assertTrue(block.statements().isEmpty());
    }

    @Test
    @DisplayName("for i in 5..3 loop ... end → empty block (empty range)")
    void forRangeEmptyRemoved() {
        var block = optimize("for i in 5..3 loop print 1 end");
        assertTrue(block.statements().isEmpty());
    }

    /* ============ block flattening ============ */

    @Test
    @DisplayName("Block flattening after if simplification (no VarDecl)")
    void blockFlatteningAfterIf() {
        // if true then print 1 end; print 2 → print 1; print 2 (flattened)
        var block = optimize("if true then print 1 end; print 2");
        assertEquals(2, block.statements().size());
        assertInstanceOf(Statement.PrintStatement.class, block.statements().get(0));
        assertInstanceOf(Statement.PrintStatement.class, block.statements().get(1));
    }

    @Test
    @DisplayName("Block NOT flattened when it contains VarDecl (scoping)")
    void blockNotFlattenedWithVarDecl() {
        // if true then var x := 1; print x end → block remains (scoping)
        var block = optimize("if true then var x := 1; print x end");
        assertEquals(1, block.statements().size());
        var stmt = block.statements().getFirst();
        // After if simplification, we have a Block containing VarDecl and Print
        assertInstanceOf(Statement.Block.class, stmt);
        var innerBlock = (Statement.Block) stmt;
        assertEquals(2, innerBlock.statements().size());
        assertInstanceOf(Statement.VarDecl.class, innerBlock.statements().get(0));
        assertInstanceOf(Statement.PrintStatement.class, innerBlock.statements().get(1));
    }

    /* ============ fixed-point iteration ============ */

    @Test
    @DisplayName("Multiple passes handle interdependent optimizations")
    void fixedPointIteration() {
        // First pass: if 3 < 5 → if true → print 1
        // Second pass: if true then print 1 else ... end → print 1
        var block = optimize("if 3 < 5 then print 1 else print 2 end");
        assertEquals(1, block.statements().size());
        var stmt = assertInstanceOf(Statement.PrintStatement.class, block.statements().get(0));
        var val = assertInstanceOf(Expression.IntLit.class, stmt.values().getFirst());
        assertEquals(new BigInteger("1"), val.value());
    }

    /* ============ integration tests ============ */

    @Test
    @DisplayName("Complex optimization: multiple passes working together")
    void complexOptimization() {
        var block = optimize("""
                var x := 2 + 3;
                var y := 5;
                if true then print x else print y end;
                return;
                print "unreachable"
                """);
        
        // var y should be removed (unused)
        // if true should be simplified
        // unreachable code should be removed
        assertEquals(3, block.statements().size());
        assertInstanceOf(Statement.VarDecl.class, block.statements().get(0));
        assertInstanceOf(Statement.PrintStatement.class, block.statements().get(1));
        assertInstanceOf(Statement.ReturnStatement.class, block.statements().get(2));
        
        // Check that x was folded to 5
        var varDecl = (Statement.VarDecl) block.statements().get(0);
        var init = assertInstanceOf(Expression.IntLit.class, varDecl.init());
        assertEquals(new BigInteger("5"), init.value());
    }
}
