
package pussycats.interpreter.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterArithmeticTest extends InterpreterTestBase {

    @Test
    void shouldAddIntegers() {
        execute("""
                print 10 + 20
                """);

        assertOutput("30");
    }

    @Test
    void shouldSubtractIntegers() {
        execute("""
                print 20 - 8
                """);

        assertOutput("12");
    }

    @Test
    void shouldMultiplyIntegers() {
        execute("""
                print 6 * 7
                """);

        assertOutput("42");
    }

    @Test
    void shouldDivideIntegers() {
        execute("""
                print 20 / 4
                """);

        assertOutput("5");
    }

    @Test
    void shouldPreserveIntegerDivision() {
        execute("""
                print 7 / 2
                """);

        assertOutput("3");
    }

    @Test
    void shouldSupportMixedArithmetic() {
        execute("""
                print 10 + 2.5
                """);

        assertOutput("12.5");
    }

    @Test
    void shouldRespectOperatorPrecedence() {
        execute("""
                print 2 + 3 * 4
                """);

        assertOutput("14");
    }

    @Test
    void shouldRespectParentheses() {
        execute("""
                print (2 + 3) * 4
                """);

        assertOutput("20");
    }

    @Test
    void shouldSupportUnaryMinus() {
        execute("""
                print -10
                """);

        assertOutput("-10");
    }

    @Test
    void shouldSupportUnaryPlus() {
        execute("""
                print +10
                """);

        assertOutput("10");
    }

    @Test
    void shouldThrowOnDivisionByZero() {
        assertRuntimeError("""
                print 10 / 0
                """, "Division by zero");
    }

    @Test
    void shouldThrowWhenAddingIncompatibleTypes() {
        assertRuntimeError("""
                print 10 + true
                """, "Cannot apply PLUS to these types");
    }

    @Test
    void shouldSupportStringConcatenation() {
        execute("""
                print "Hello, " + "world!"
                """);

        assertOutput("Hello, world!");
    }
}
