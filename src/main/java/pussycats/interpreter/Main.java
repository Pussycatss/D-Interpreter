package pussycats.interpreter;

import pussycats.interpreter.ast.AstPrinter;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.cli.REPL;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Entry point of the D interpreter.
 * <p>
 * <i><b>NOT FINISHED</b></i>
 * <p>
 * SUPPORTED LANGUAGE FEATURES:
 * <ul>
 *   <li>Lexical analysis (tokenization)
 * </ul>
 * </i>
 * <p>
 * Usage:
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
        boolean dumpTokens = false;
        boolean dumpAst = false;
        String code = null;
        String file = null;

        int i = 0;

        while (i < args.length) {
            String a = args[i];
            switch (a) {
                case "-h", "--help" -> {
                    printHelp();
                    System.exit(EXIT_OK);
                    return;
                }
                case "-t", "--tokens" -> dumpTokens = true;
                case "-a", "--ast"    -> dumpAst = true;
                case "-ta", "--all"   -> { dumpTokens = true; dumpAst = true; }
                case "-c" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: -c requires an argument");
                        System.exit(EXIT_ERROR);
                        return;
                    }
                    code = args[++i];
                }
                default -> {
                    if (a.startsWith("-")) {
                        System.err.println("Unknown option: " + a);
                        System.err.println("Try '--help' for usage.");
                        System.exit(EXIT_ERROR);
                        return;
                    }
                    if (file != null) {
                        System.err.println("Error: only one input file is allowed");
                        System.exit(EXIT_ERROR);
                        return;
                    }
                    file = a;
                }
            }
            i++;
        }

        if (!dumpTokens && !dumpAst) {
            dumpAst = true;
        }

        if (code != null) {
            run(code, "<string>", dumpTokens, dumpAst);
            return;
        }

        if (file != null) {
            runFile(file, dumpTokens, dumpAst);
            return;
        }

        // No file, no -c: drop into the REPL.
        new REPL("lexer");
        System.exit(EXIT_OK);
    }

    private static void runFile(String path, boolean dumpTokens, boolean dumpAst) {
        String source;
        try {
            source = Files.readString(Path.of(path));
        } catch (IOException e) {
            System.err.println("Cannot read file '" + path + "': " + e.getMessage());
            System.exit(EXIT_ERROR);
            return;
        }
        run(source, path, dumpTokens, dumpAst);
    }

    private static void run(String source, String sourceName,
                            boolean dumpTokens, boolean dumpAst) {
        // ---- Lexer ----
        List<Token> tokens = new Lexer(source, sourceName).scan();

        if (dumpTokens) {
            System.out.println("=== Tokens ===");
            for (Token token : tokens) {
                System.out.println(token);
            }
            System.out.println();
        }

        // ---- Parser ----
        Parser parser = new Parser(tokens, source, sourceName);
        Node ast = parser.parse();

        if (parser.hasErrors() || ast == null) {
            System.err.print(parser.formatErrors());
            System.exit(EXIT_ERROR);
            return;
        }

        if (dumpAst) {
            System.out.println("=== AST ===");
            System.out.println(new AstPrinter().print(ast));
        }

        // TODO: semantic analysis + interpretation
    }

    private static void printHelp() {
        System.out.println("D Interpreter");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java pussycats.interpreter.Main [options] [file]");
        System.out.println();
        System.out.println("Input:");
        System.out.println("  -c <code>       Execute program passed as a string");
        System.out.println("  <file>          Execute program from a file");
        System.out.println();
        System.out.println("Output (default: --ast):");
        System.out.println("  -t, --tokens    Dump the lexer's token stream");
        System.out.println("  -a, --ast       Dump the parser's abstract syntax tree");
        System.out.println("  -ta, --all      Dump both tokens and AST");
        System.out.println();
        System.out.println("Other:");
        System.out.println("  -h, --help      Show this help message");
        System.out.println();
        System.out.println("With no file and no -c, starts a REPL.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java pussycats.interpreter.Main script.d");
        System.out.println("  java pussycats.interpreter.Main -c \"print 42\"");
        System.out.println("  java pussycats.interpreter.Main -t script.d");
        System.out.println("  java pussycats.interpreter.Main --all -c \"var x := 1\"");
    }

    private Main() {
    }
}