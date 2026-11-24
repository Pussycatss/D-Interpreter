package pussycats.interpreter.cli;

import pussycats.interpreter.ast.AstPrinter;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.error.DiagnosticError;
import pussycats.interpreter.error.ErrorReporter;
import pussycats.interpreter.error.SourceManager;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.LexerException;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.parser.Parser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import pussycats.interpreter.runtime.CallStackFrame;
import pussycats.interpreter.runtime.Interpreter;
import pussycats.interpreter.runtime.InterpretationException;
import pussycats.interpreter.runtime.ModuleLoadException;
import pussycats.interpreter.semantic.Optimizer;
import pussycats.interpreter.semantic.Analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    @Option(names = {"-i", "--interpreter"}, description = "Run REPL that executes code")
    private boolean interpreterMode = false;

    @Parameters(index = "0", description = "Script file to execute", arity = "0..1")
    private String file = null;

    @Override
    public Integer call() {
        if (dumpAll) {
            dumpTokens = true;
            dumpAst = true;
            optimizerMode = true;
        }

        if (code != null) {
            run(code, "<string>");
            return 0;
        }

        if (file != null) {
            runFile(file);
            return 0;
        }

        // No file, no -c: drop into the REPL.
        String replMode = interpreterMode ? "interpreter" : optimizerMode ? "optimizer" : "parser";
        new REPL(replMode);
        return 0;
    }

    private void runFile(String path) {
        String source;
        try {
            source = Files.readString(Path.of(path));
        } catch (IOException e) {
            System.err.println("Cannot read file '" + path + "': " + e.getMessage());
            System.exit(1);
            return;
        }
        run(source, path);
    }

    private void run(String source, String sourceName) {
        SourceManager sourceManager = new SourceManager();
        sourceManager.register(sourceName, source);
        ErrorReporter reporter = new ErrorReporter(source, sourceName, sourceManager);

        CompilationResult result = compile(source, sourceName, reporter);
        // ---- Lexer ----
        if (dumpTokens && result.tokens() != null) {
            System.out.println("=== Tokens ===");
            result.tokens().forEach(System.out::println);
            System.out.println();
        }

        // ---- Parser ----
        if (dumpAst && result.ast() != null) {
            System.out.println("=== AST ===");
            System.out.println(new AstPrinter().print(result.ast()));
        }

        // ---- Analyzer ----
        if (result.error() != null) {
            System.err.print(result.error());
            System.exit(1);
            return;
        }

        // ---- Optimizer ----
        if (optimizerMode && result.optimizedAst() != null) {
            System.out.println("=== Optimized AST ===");
            System.out.println(new AstPrinter().print(result.optimizedAst()));
        }

        // ---- Interpreter ----
        if (dumpTokens || dumpAst || optimizerMode) {
            System.out.println("=== Execution ===");
        }

        Interpreter interpreter = new Interpreter(sourceName, new CliModuleLoader(sourceManager));

        try {
            interpreter.interpret(result.optimizedAst());
        } catch (ModuleLoadException e) {
            printModuleError(e, sourceManager);
            System.exit(1);
        } catch (InterpretationException e) {
            reporter.report(e);
            System.err.print(reporter.formatAll());
            System.exit(1);
        }
    }

    private CompilationResult compile(String source, String sourceName, ErrorReporter reporter) {
        // ---- Lexer ----
        List<Token> tokens;
        try {
            tokens = new Lexer(source).scan();
        } catch (LexerException e) {
            reporter.report(e);
            System.err.print(reporter.formatAll());
            System.exit(1);
            return new CompilationResult(null, null, null, reporter.formatAll());
        }

        // ---- Parser ----
        Parser parser = new Parser(tokens, source, sourceName);
        Node ast = parser.parse();
        if (parser.hasErrors() || ast == null) {
            return new CompilationResult(tokens, null, null, parser.formatErrors());
        }

        // ---- Analyzer ----
        Analyzer analyzer = new Analyzer(reporter);
        analyzer.analyze(ast);
        if (analyzer.hasErrors()) {
            return new CompilationResult(tokens, ast, null, analyzer.formatErrors());
        }

        // ---- Optimizer ----
        Node optimizedAst = new Optimizer().runFullOptimization(ast);
        if (optimizedAst == null) {
            return new CompilationResult(tokens, ast, null, "Optimizer failed to optimize AST");
        }
        return new CompilationResult(tokens, ast, optimizedAst, null);
    }

    private record CompilationResult(
            List<Token> tokens,
            Node ast,
            Node optimizedAst,
            String error
    ) {
    }

    private static void printModuleError(ModuleLoadException e, SourceManager sourceManager) {
        // Append the deepest failing location as the final frame.
        List<CallStackFrame> chain = new ArrayList<>(e.getFrames());
        chain.add(new CallStackFrame("<module>", e.getSpan(), e.getSourceName()));

        DiagnosticError diag = new DiagnosticError(
                e.getMessage(),
                e.getSuggestion(),
                sourceManager.get(e.getSourceName()),
                e.getSourceName(),
                e.getSpan().start().line(),
                e.getSpan().start().column(),
                e.getSpan().length(),
                e.getErrorType(),
                chain,
                sourceManager);
        System.err.print(diag.format());
    }
}
