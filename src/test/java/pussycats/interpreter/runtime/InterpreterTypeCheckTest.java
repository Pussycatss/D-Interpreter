package pussycats.interpreter.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterTypeCheckTest extends InterpreterTestBase {

    @Test
    void shouldRecognizeInteger() {
        execute("print 42 is int");
        assertOutput("true");
    }

    @Test
    void shouldRecognizeReal() {
        execute("print 3.14 is real");
        assertOutput("true");
    }

    @Test
    void shouldRecognizeString() {
        execute("print \"hello\" is string");
        assertOutput("true");
    }

    @Test
    void shouldRecognizeBoolean() {
        execute("print true is bool");
        assertOutput("true");
    }

    @Test
    void shouldRecognizeNone() {
        execute("print none is none");
        assertOutput("true");
    }

    @Test
    void shouldRecognizeArray() {
        execute("print [1, 2] is []");
        assertOutput("true");
    }

    @Test
    void shouldRecognizeTuple() {
        execute("print {1, 2} is {}");
        assertOutput("true");
    }

    @Test
    void shouldRecognizeFunction() {
        execute("""
            var f := func() is
                return 1
            end

            print f is func
            """);

        assertOutput("true");
    }

    @Test
    void shouldReturnFalseForDifferentType() {
        execute("print 42 is string");
        assertOutput("false");
    }
}