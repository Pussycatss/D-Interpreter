package pussycats.interpreter.semantic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.parser.Parser;

import java.util.List;

@DisplayName("Semantic Analyzer Tests")
class AnalyzerTest {

    /* ============ helpers ============ */

    private static Analyzer analyze(String src) {
        List<Token> tokens = Lexer.scan(src);
        Parser p = new Parser(tokens, src, "<test>");
        Node ast = p.parse();
        if (ast == null || p.hasErrors()) {
            throw new AssertionError(
                    "parse failed for: <" + src + ">\n" + p.formatErrors());
        }
        Analyzer analyzer = new Analyzer(src, "<test>");
        analyzer.analyze(ast);
        return analyzer;
    }

    private static void assertNoErrors(String src) {
        Analyzer analyzer = analyze(src);
        assertFalse(analyzer.hasErrors(),
                "Expected no semantic errors, but got:\n" + analyzer.formatErrors());
    }

    private static void assertHasError(String src, String expectedMessagePart) {
        Analyzer analyzer = analyze(src);
        assertTrue(analyzer.hasErrors(),
                "Expected a semantic error but got none for: <" + src + ">");

        String errors = analyzer.formatErrors();
        assertTrue(errors.contains(expectedMessagePart),
                "Expected error containing '" + expectedMessagePart + "', but got:\n" + errors);
    }

    /* ============ variable declarations & scopes ============ */

    @Test
    @DisplayName("Valid: variable declared and used")
    void validVariableUsage() {
        assertNoErrors("var x := 5\nprint x");
    }

    @Test
    @DisplayName("Error: variable used before declaration")
    void undeclaredVariable() {
        assertHasError("print x", "Undeclared variable: 'x'");
    }

    @Test
    @DisplayName("Error: duplicate declaration in same scope")
    void duplicateVariableDeclaration() {
        assertHasError("var x := 1\nvar x := 2", "Duplicate declaration");
    }

    @Test
    @DisplayName("Valid: shadowing a variable in a nested block")
    void validVariableShadowing() {
        assertNoErrors("""
                var x := 1
                if true then
                    var x := 2
                    print x
                end
                print x
                """);
    }

    @Test
    @DisplayName("Error: variable declared in inner block used outside")
    void scopeBoundaryViolation() {
        assertHasError("""
                if true then
                    var x := 2
                end
                print x
                """, "Undeclared variable: 'x'");
    }

    @Test
    @DisplayName("Valid: loop variable is in scope inside loop body")
    void validLoopVariableScope() {
        assertNoErrors("for i in 1..3 loop\n    print i\nend");
    }

    @Test
    @DisplayName("Error: loop variable used after loop ends")
    void loopVariableOutOfScope() {
        assertHasError("for i in 1..3 loop\n    print i\nend\nprint i",
                "Undeclared variable: 'i'");
    }

    /* ============ assignments ============ */

    @Test
    @DisplayName("Valid: assigning to identifier, index, and field")
    void validAssignmentTargets() {
        assertNoErrors("""
                var x := 1
                var arr := [1, 2]
                var tup := {a := 1}
                x := 2
                arr[1] := 3
                tup.a := 4
                """);
    }

    @Test
    @DisplayName("Error: assigning to a literal")
    void invalidAssignmentTargetLiteral() {
        assertHasError("4 := 4", "Invalid assignment target");
    }

    @Test
    @DisplayName("Error: assigning to a binary expression")
    void invalidAssignmentTargetExpression() {
        assertHasError("1 + 3 := 4", "Invalid assignment target");
    }

    /* ============ control flow context (exit/return) ============ */

    @Test
    @DisplayName("Valid: 'exit' inside loop")
    void validExitInsideLoop() {
        assertNoErrors("loop\n    exit\nend");
    }

    @Test
    @DisplayName("Error: 'exit' outside loop")
    void invalidExitOutsideLoop() {
        assertHasError("exit", "'exit' can only appear inside a loop");
    }

    @Test
    @DisplayName("Valid: 'return' inside function")
    void validReturnInsideFunction() {
        assertNoErrors("var f := func() is\n    return 1\nend");
    }

    @Test
    @DisplayName("Error: 'return' outside function")
    void invalidReturnOutsideFunction() {
        assertHasError("return 1", "'return' can only appear inside a function");
    }

    @Test
    @DisplayName("Error: 'exit' inside function inside loop should fail")
    void exitInsideFunctionDoesNotInheritLoopDepth() {
        // A function creates a new scope for loop depth.
        // The 'exit' cannot break the outer loop.
        assertHasError("""
                loop
                    var f := func() is
                        exit
                    end
                end
                """, "'exit' can only appear inside a loop");
    }

    /* ============ functions ============ */

    @Test
    @DisplayName("Valid: function parameters are in scope in body")
    void validFunctionParametersInScope() {
        assertNoErrors("var f := func(x, y) => x + y");
    }

    @Test
    @DisplayName("Error: function parameter used outside function")
    void functionParameterOutOfScope() {
        assertHasError("""
                var f := func(x) is
                    print x
                end
                print x
                """, "Undeclared variable: 'x'");
    }

    @Test
    @DisplayName("Error: duplicate function parameters")
    void duplicateFunctionParameters() {
        assertHasError("var f := func(x, y, x) => x",
                "Duplicate function parameter");
    }

    /* ============ tuples ============ */

    @Test
    @DisplayName("Valid: tuple with unique keys")
    void validTupleKeys() {
        assertNoErrors("var t := {a := 1, b := 2, 3}");
    }

    @Test
    @DisplayName("Error: duplicate tuple keys")
    void duplicateTupleKeys() {
        assertHasError("var t := {a := 1, b := 2, a := 3}",
                "Duplicate tuple key");
    }
}