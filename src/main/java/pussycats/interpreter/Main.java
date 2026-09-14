package pussycats.interpreter;

import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.Token;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Entry point of the D interpreter.
 * <i><p><b>NOT FINISHED</b>
 *
 * <p>SUPPORTED LANGUAGE FEATURES:
 * <ul>
 *   <li>Lexical analysis (tokenization)</li>
 * </ul>
 * </i>
 * <p>Usage:
 * <pre>
 *   # Run a script file
 *   java pussycats.interpreter.Main script.d
 *
 *   # Execute code from string
 *   java pussycats.interpreter.Main -c "print 67"
 *
 *   # Show help
 *   java pussycats.interpreter.Main --help
 * </pre>
 */
public final class Main {

    private static final int EXIT_OK = 0;
    private static final int EXIT_ERROR = 1;

    static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("REPL not implemented.");
            System.out.println("Usage: java pussycats.interpreter.Main [options] [file]");
            System.exit(EXIT_OK);
            return;
        }

        if (args[0].equals("--help") || args[0].equals("-h")) {
            printHelp();
            System.exit(EXIT_OK);
            return;
        }

        if (args[0].equals("-c")) {
            if (args.length < 2) {
                System.err.println("Error: -c requires an argument");
                System.exit(EXIT_ERROR);
                return;
            }
            runString(args[1]);
            return;
        }

        runFile(args[0]);
    }

    private static void runFile(String path) {
        String source;
        try {
            source = Files.readString(Path.of(path));
        } catch (IOException e) {
            System.err.println("Cannot read file '" + path + "': " + e.getMessage());
            System.exit(EXIT_ERROR);
            return;
        }
        run(source, path);
    }

    private static void runString(String code) {
        run(code, "<string>");
    }

    private static void run(String source, String sourceName) {
        List<Token> tokens;
        tokens = new Lexer(source, sourceName).scan();

        for (Token token : tokens) {
            System.out.println(token);
        }

        // TODO: once a parser exists, feed `tokens` into it here and print/execute
    }

    private static void printHelp() {
        System.out.println("D Interpreter");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java pussycats.interpreter.Main [options] [file]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -c <code>   Execute program passed as string");
        System.out.println("  -h, --help  Show this help message");
        System.out.println();
        System.out.println("If no file is specified, reads from stdin.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java pussycats.interpreter.Main script.d");
        System.out.println("  java pussycats.interpreter.Main -c \"print 42\"");
    }

    private Main() {
    }
}