package pussycats.interpreter.cli;

import pussycats.interpreter.ast.AstPrinter;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.parser.Parser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import pussycats.interpreter.semantic.Optimizer;
import pussycats.interpreter.semantic.Analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "d-interpreter", mixinStandardHelpOptions = true, version = "D Interpreter 1.0",
        description = "Interpreter for the D programming language")
public class CommandLineArgs implements Callable<Integer> {

    @Option(names = {"-t", "--tokens"}, description = "Dump the lexer's token stream")
    private boolean dumpTokens = false;

    @Option(names = {"-a", "--ast"}, description = "Dump the parser's abstract syntax tree")
    private boolean dumpAst = false;

    @Option(names = {"-ta", "--all"}, description = "Dump both tokens and AST")
    private boolean dumpAll = false;

    @Option(names = {"-c", "--code"}, description = "Execute program passed as a string")
    private String code = null;

    @Option(names = {"-o", "--optimizer"}, description = "Run REPL with optimizer (shows before/after AST)")
    private boolean optimizerMode = false;

    @Parameters(index = "0", description = "Script file to execute", arity = "0..1")
    private String file = null;

    @Override
    public Integer call() {
        if (dumpAll) {
            dumpTokens = true;
            dumpAst = true;
        }

        if (!dumpTokens && !dumpAst) {
            dumpAst = true;
        }

        if (code != null) {
            run(code, "<string>", dumpTokens, dumpAst);
            return 0;
        }

        if (file != null) {
            runFile(file, dumpTokens, dumpAst);
            return 0;
        }

        // No file, no -c: drop into the REPL.
        new REPL(optimizerMode ? "optimizer" : "parser");
        return 0;
    }

    private void runFile(String path, boolean dumpTokens, boolean dumpAst) {
        String source;
        try {
            source = Files.readString(Path.of(path));
        } catch (IOException e) {
            System.err.println("Cannot read file '" + path + "': " + e.getMessage());
            System.exit(1);
            return;
        }
        run(source, path, dumpTokens, dumpAst);
    }

    private void run(String source, String sourceName, boolean dumpTokens, boolean dumpAst) {
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
            System.exit(1);
            return;
        }

        if (dumpAst) {
            System.out.println("=== AST ===");
            System.out.println(new AstPrinter().print(ast));
        }

        // ---- Analyzer ----
        Analyzer analyzer = new Analyzer();
        analyzer.analyze(ast);

        if (!analyzer.errors.isEmpty()) {
            System.err.println("=== Semantic Errors ===");
            for (String error : analyzer.errors) {
                System.err.println(error);
            }
            System.exit(1);
            return;
        }

        // ---- Optimizer ----
        Optimizer optimizer = new Optimizer();
        Node optimizedAst = optimizer.runFullOptimization(ast);

        if (optimizedAst == null) {
            System.err.println("Optimizer failed to optimize AST");
            System.exit(1);
            return;
        }

        if (dumpAst) {
            System.out.println("=== Optimized AST ===");
            System.out.println(new AstPrinter().print(optimizedAst));
        }

        // TODO: interpretation
    }
}
