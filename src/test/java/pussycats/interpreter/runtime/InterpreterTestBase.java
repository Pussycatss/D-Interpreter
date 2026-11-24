package pussycats.interpreter.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.parser.Parser;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

abstract class InterpreterTestBase {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream output;

    protected ModuleLoader moduleLoader = path -> {
        throw new AssertionError("Unexpected module load: " + path);
    };

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    protected Object execute(String source) {
        return execute(source, moduleLoader);
    }

    protected Object execute(String source, ModuleLoader loader) {
        var tokens = Lexer.scan(source);

        var parser = new Parser(tokens, source, "<test>");

        Node ast = parser.parse();

        assertFalse(
                parser.hasErrors(),
                () -> "Unexpected parser errors:\n" + parser.formatErrors()
        );

        assertNotNull(ast, "Parser returned null");

        Interpreter interpreter = new Interpreter("<test>", loader);

        return interpreter.interpret(ast);
    }

    protected String output() {
        return output.toString();
    }

    protected void assertOutput(String expected) {
        assertEquals(
                expected + System.lineSeparator(),
                output()
        );
    }

    protected InterpretationException assertRuntimeError(
            String source,
            String expectedMessage
    ) {
        InterpretationException exception = assertThrows(
                InterpretationException.class,
                () -> execute(source)
        );

        assertEquals(expectedMessage, exception.getMessage());

        return exception;
    }
}
