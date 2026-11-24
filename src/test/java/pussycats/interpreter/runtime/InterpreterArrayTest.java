package pussycats.interpreter.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterArrayTest extends InterpreterTestBase {

    @Test
    void shouldCreateAndPrintArray() {
        execute("var a := [1, 2, 3]\nprint a");

        assertOutput("[1, 2, 3]");
    }

    @Test
    void shouldAccessArrayElement() {
        execute("""
            var a := [10, 20, 30]
            print a[1]
            print a[2]
            print a[3]
            """);

        assertEquals(
                "10" + System.lineSeparator()
                        + "20" + System.lineSeparator()
                        + "30" + System.lineSeparator(),
                output()
        );
    }

    @Test
    void shouldSupportNestedArrays() {
        execute("""
            var a := [[1, 2], [3, 4]]
            print a[1][2]
            print a[2][1]
            """);

        assertEquals(
                "2" + System.lineSeparator()
                        + "3" + System.lineSeparator(),
                output()
        );
    }

    @Test
    void shouldConcatenateArrays() {
        execute("""
            var a := [1, 2]
            var b := [3, 4]
            print a + b
            """);

        assertOutput("[1, 2, 3, 4]");
    }

    @Test
    void shouldCreateEmptyArray() {
        execute("""
            var a := []
            print a
            """);

        assertOutput("[]");
    }

    @Test
    void shouldFailWhenIndexingNonArray() {
        assertRuntimeError(
                "var x := 10\nprint x[1]",
                "Cannot index this type"
        );
    }

    @Test
    void shouldFailWhenIndexIsNotInteger() {
        assertRuntimeError(
                "var a := [1, 2]\nprint a[1.5]",
                "Index must be an integer"
        );
    }

    @Test
    void shouldFailWhenIndexIsZero() {
        assertRuntimeError(
                "var a := [1, 2]\nprint a[0]",
                "Index must be >= 1"
        );
    }
}
