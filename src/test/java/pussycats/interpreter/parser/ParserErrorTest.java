package pussycats.interpreter.parser;

import org.junit.jupiter.api.Test;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.lexer.Lexer;

import static org.junit.jupiter.api.Assertions.*;

class ParserErrorTest {

    /** Parses and asserts that at least one error was reported. */
    private static Parser failing(String src) {
        var p = new Parser(Lexer.scan(src), src, "<test>");
        Node ast = p.parse();
        assertTrue(p.hasErrors() || ast == null,
                "expected a parse error for: <" + src + ">");
        return p;
    }

    private static String errors(String src) {
        return failing(src).formatErrors();
    }

    @Test void varWithoutName() {
        assertTrue(errors("var").contains("variable name"));
    }

    @Test void varWithEqualsInsteadOfAssign() {
        var out = errors("var x = 1");
        // the reporter should hint at :=
        assertTrue(out.contains(":="),
                "expected a hint about ':=' but got:\n" + out);
    }

    @Test void ifWithoutThen() {
        assertTrue(errors("if x print 1 end").contains("then"));
    }

    @Test void ifWithoutEnd() {
        assertTrue(errors("if x then print 1").contains("end"));
    }

    @Test void whileWithoutLoop() {
        assertTrue(errors("while x print 1 end").contains("loop"));
    }

    @Test void exitOutsideLoop() {
        assertTrue(errors("exit").toLowerCase().contains("loop"));
    }

    @Test void forWithoutLoop() {
        assertTrue(errors("for 1..3 print 1 end").contains("loop"));
    }

    @Test void funcWithoutBody() {
        assertTrue(errors("func(x)").contains("=>"));
    }

    @Test void missingSeparatorBetweenStatements() {
        assertTrue(errors("print 1 print 2").toLowerCase().contains("separator"));
    }

    @Test void relationIsNotAssociative() {
        // 1 < 2 < 3 should fail — the spec allows only one comparison
        failing("1 < 2 < 3");
    }

    @Test void unclosedParenthesis() {
        failing("print (1 + 2");
    }

    @Test void unclosedArray() {
        failing("print [1, 2, 3");
    }

    @Test void emptyExpressionAfterAssign() {
        failing("var x :=");
    }

    @Test void errorReportsLineAndColumn() {
        var p = failing("print 1\nvar");
        var err = p.formatErrors();
        assertTrue(err.contains("line 2"),
                "expected an error on line 2 but got:\n" + err);
    }

    @Test void misspelledPrintAtStatementStart() {
        var out = errors("prknt 42");
        assertTrue(out.contains("print"),
                "expected a suggestion for 'print' but got:\n" + out);
    }

    @Test void shortIdentifierDoesNotTriggerKeywordSuggestion() {
        var out = errors("c y");
        assertFalse(out.contains("did you mean 'if'"),
                "false positive suggestion for 'if':\n" + out);
    }

    @Test void bareIdentifierIsNotAStatement() {
        var out = errors("x");
        assertTrue(out.contains(":="),
                "expected a ':=' error for a bare identifier but got:\n" + out);
    }

    @Test void bareLiteralIsNotAStatement() {
        var out = errors("42");
        assertTrue(out.contains(":="),
                "expected a ':=' error for a bare literal but got:\n" + out);
    }
}