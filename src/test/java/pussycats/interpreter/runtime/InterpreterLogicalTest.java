package pussycats.interpreter.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterLogicalTest extends InterpreterTestBase {

    @Test
    void shouldEvaluateAnd() {
        execute("""
            print true and true
            print true and false
            print false and true
            """);

        assertEquals(
                "true" + System.lineSeparator()
                        + "false" + System.lineSeparator()
                        + "false" + System.lineSeparator(),
                output()
        );
    }

    @Test
    void shouldEvaluateOr() {
        execute("""
            print true or false
            print false or true
            print false or false
            """);

        assertEquals(
                "true" + System.lineSeparator()
                        + "true" + System.lineSeparator()
                        + "false" + System.lineSeparator(),
                output()
        );
    }

    @Test
    void shouldEvaluateXor() {
        execute("""
            print true xor false
            print false xor true
            print true xor true
            """);

        assertEquals(
                "true" + System.lineSeparator()
                        + "true" + System.lineSeparator()
                        + "false" + System.lineSeparator(),
                output()
        );
    }

    @Test
    void shouldSupportShortCircuitAnd() {
        execute("""
            print false and (1 / 0 = 0)
            """);

        assertOutput("false");
    }

    @Test
    void shouldSupportShortCircuitOr() {
        execute("""
            print true or (1 / 0 = 0)
            """);

        assertOutput("true");
    }

    @Test
    void shouldFailWhenLogicalOperandIsNotBoolean() {
        assertRuntimeError(
                "print 10 and true",
                "Cannot apply AND to these types"
        );
    }
}