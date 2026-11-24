package pussycats.interpreter.runtime;

import org.junit.jupiter.api.Test;

class InterpreterFunctionTest extends InterpreterTestBase {

    @Test
    void shouldCallFunctionWithOneArgument() {
        execute("""
            var identity := func(x) is
                return x
            end

            print identity(42)
            """);

        assertOutput("42");
    }

    @Test
    void shouldCallFunctionWithMultipleArguments() {
        execute("""
            var add := func(a, b) is
                return a + b
            end

            print add(10, 20)
            """);

        assertOutput("30");
    }

    @Test
    void shouldSupportNestedFunctionCalls() {
        execute("""
            var addOne := func(n) is
                return n + 1
            end

            var addTwo := func(n) is
                return addOne(addOne(n))
            end

            print addTwo(10)
            """);

        assertOutput("12");
    }

    @Test
    void shouldReturnNoneWhenFunctionHasNoReturnStatement() {
        execute("""
            var doNothing := func() is
                var x := 10
            end

            print doNothing()
            """);

        assertOutput("none");
    }

    @Test
    void shouldReturnNoneWhenReturnHasNoValue() {
        execute("""
            var empty := func() is
                return
            end

            print empty()
            """);

        assertOutput("none");
    }

    @Test
    void shouldCaptureVariablesFromClosure() {
        execute("""
            var multiplier := 10

            var multiply := func(n) is
                return n * multiplier
            end

            print multiply(5)
            """);

        assertOutput("50");
    }

    @Test
    void shouldSupportRecursiveFunctions() {
        execute("""
            var factorial := func(n) is
                if n <= 1 then
                    return 1
                end

                return n * factorial(n - 1)
            end

            print factorial(5)
            """);

        assertOutput("120");
    }

    @Test
    void shouldFailWhenCallingNonFunction() {
        assertRuntimeError(
                "var x := 10\nprint x()",
                "Cannot call this type"
        );
    }

    @Test
    void shouldFailWhenArgumentCountDoesNotMatch() {
        assertRuntimeError("""
            var add := func(a, b) is
                return a + b
            end

            print add(1)
            """, "Argument count mismatch");
    }
}
