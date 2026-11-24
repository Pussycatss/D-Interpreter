package pussycats.interpreter.cli;

import pussycats.interpreter.SourceLocation;
import pussycats.interpreter.SourceSpan;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.error.DiagnosticError;
import pussycats.interpreter.error.ErrorReporter;
import pussycats.interpreter.error.SourceManager;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.LexerException;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.parser.Parser;
import pussycats.interpreter.runtime.ModuleLoadException;
import pussycats.interpreter.runtime.ModuleLoader;
import pussycats.interpreter.semantic.Analyzer;
import pussycats.interpreter.semantic.Optimizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CliModuleLoader implements ModuleLoader {
    private final SourceManager sourceManager;

    public CliModuleLoader(SourceManager sourceManager) {
        this.sourceManager = sourceManager;
    }

    @Override
    public Statement.Block load(Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        String source;
        try {
            source = Files.readString(normalizedPath);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read module '" + normalizedPath + "': " + e.getMessage(), e);
        }
        String sourceName = normalizedPath.toString();
        sourceManager.register(sourceName, source);
        ErrorReporter reporter = new ErrorReporter(source, sourceName, sourceManager);

        // ---- Lexer ----
        List<Token> tokens;
        try {
            tokens = new Lexer(source).scan();
        } catch (LexerException e) {
            throw new ModuleLoadException(List.of(), "SyntaxError", e.getMessage(), null, e.getSpan(), sourceName);
        }

        // ---- Parser ----
        Parser parser = new Parser(tokens, source, sourceName);
        Node ast = parser.parse();
        if (parser.hasErrors() || ast == null) {
            DiagnosticError first = parser.getErrors().getFirst();
            throw new ModuleLoadException(
                    List.of(), first.errorType(), first.message(), first.suggestion(), spanOf(first), sourceName);
        }

        // ---- Analyzer ----
        Analyzer analyzer = new Analyzer(reporter);
        analyzer.analyze(ast);
        if (analyzer.hasErrors()) {
            DiagnosticError first = parser.getErrors().getFirst();
            throw new ModuleLoadException(
                    List.of(), first.errorType(), first.message(), first.suggestion(), spanOf(first), sourceName);
        }

        // ---- Optimizer ----
        Node optimized = new Optimizer().runFullOptimization(ast, false);
        if (!(optimized instanceof Statement.Block block)) {
            throw new ModuleLoadException(
                    List.of(), "InternalError", "Module must contain a block", null,
                    new SourceSpan(new SourceLocation(1, 1), new SourceLocation(1, 1)), sourceName);
        }

        return block;
    }

    private SourceSpan spanOf(DiagnosticError d) {
        return new SourceSpan(new SourceLocation(d.line(), d.column()),
                              new SourceLocation(d.line(), d.column() + Math.max(1, d.length())));
    }
}
