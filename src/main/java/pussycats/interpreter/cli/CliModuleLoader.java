package pussycats.interpreter.cli;

import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.error.ErrorReporter;
import pussycats.interpreter.error.SourceManager;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.parser.Parser;
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
            throw new RuntimeException(
                    "Cannot read module '" + normalizedPath + "': " + e.getMessage(), e
            );
        }
        String sourceName = normalizedPath.toString();
        sourceManager.register(sourceName, source);
        ErrorReporter reporter = new ErrorReporter(source, sourceName, sourceManager);

        // ---- Lexer ----
        List<Token> tokens = new Lexer(source, sourceName).scan();

        // ---- Parser ----
        Parser parser = new Parser(tokens, source, sourceName);
        Node ast = parser.parse();
        if (parser.hasErrors() || ast == null) {
            throw new RuntimeException(parser.formatErrors());
        }

        // ---- Analyzer ----
        Analyzer analyzer = new Analyzer(reporter);
        analyzer.analyze(ast);
        if (analyzer.hasErrors()) {
            throw new RuntimeException(analyzer.formatErrors());
        }

        // ---- Optimizer ----
        Node optimized = new Optimizer().runFullOptimization(ast, false);
        if (!(optimized instanceof Statement.Block block)) {
            throw new RuntimeException("Module must contain a block");
        }

        return block;
    }
}
