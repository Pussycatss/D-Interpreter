package pussycats.interpreter.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterTupleTest extends InterpreterTestBase {

    @Test
    void shouldCreateAndPrintTuple() {
        execute("var t := {1, 2, 3}\nprint t");

        assertOutput("{1, 2, 3}");
    }

    @Test
    void shouldCreateNamedTupleFields() {
        execute("""
            var person := {
                name := "Alice",
                age := 25
            }

            print person.name
            print person.age
            """);

        assertEquals(
                "Alice" + System.lineSeparator()
                        + "25" + System.lineSeparator(),
                output()
        );
    }

    @Test
    void shouldConcatenateTuples() {
        execute("""
            var a := {1, 2}
            var b := {3, 4}
            print a + b
            """);

        assertOutput("{1, 2, 3, 4}");
    }

    @Test
    void shouldFailWhenAccessingMissingField() {
        assertRuntimeError("""
            var t := {x := 10}
            print t.y
            """, "Field not found: y");
    }

    @Test
    void shouldFailWhenAccessingFieldOnNonTuple() {
        assertRuntimeError(
                "var x := 10\nprint x.y",
                "Cannot access field on this type"
        );
    }
}
