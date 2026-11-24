package pussycats.interpreter.cli;

import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.error.ErrorReporter;
import pussycats.interpreter.error.SourceManager;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.LexerException;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.parser.Parser;
import pussycats.interpreter.runtime.InterpretationException;
import pussycats.interpreter.runtime.Interpreter;
import pussycats.interpreter.runtime.ModuleLoadException;
import pussycats.interpreter.semantic.Analyzer;
import pussycats.interpreter.semantic.Optimizer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * REPL handler that executes D code. A single {@link Interpreter} instance is
 * reused across inputs, so variables and functions declared in previous lines
 * stay available. Top-level statements are interpreted one by one in the
 * interpreter's global frame — wrapping them in a {@link Statement.Block} would
 * scope them to a temporary frame that is discarded after each input.
 */
public class InterpreterEval implements Evaluable {
    private static final String SOURCE_NAME = "<stdin>";

    private final SourceManager sourceManager = new SourceManager();
    private final Interpreter interpreter =
            new Interpreter(SOURCE_NAME, new CliModuleLoader(sourceManager));
    private final Optimizer optimizer = new Optimizer();
    private final Set<String> declaredNames = new HashSet<>();

    @Override
    public String eval(String line) {
        // ---- Lexer ----
        List<Token> tokens;
        try {
            tokens = new Lexer(line).scan();
        } catch (LexerException e) {
            return e.getMessage();
        }

        // ---- Parser ----
        Parser parser = new Parser(tokens, line, SOURCE_NAME);
        Node ast = parser.parse();
        if (parser.hasErrors() || ast == null) {
            return parser.formatErrors();
        }
        if (!(ast instanceof Statement.Block block)) {
            return "Unexpected AST root: " + ast.getClass().getSimpleName();
        }

        // ---- Analyzer ----
        // Seed the global scope with names declared by previous inputs.
        ErrorReporter reporter = new ErrorReporter(line, SOURCE_NAME, sourceManager);
        Analyzer analyzer = new Analyzer(reporter);
        declaredNames.forEach(analyzer::declareGlobal);
        for (Statement statement : block.statements()) {
            analyzer.analyze(statement);
        }
        if (analyzer.hasErrors()) {
            return analyzer.formatErrors();
        }

        // ---- Optimizer ----
        Node optimized = optimizer.runFullOptimization(ast, false);
        if (!(optimized instanceof Statement.Block optimizedBlock)) {
            return "Optimizer produced an unexpected AST";
        }

        // ---- Interpreter ----
        collectDeclaredNames(optimizedBlock);
        try {
            interpreter.interpretTopLevel(optimizedBlock);
        } catch (ModuleLoadException e) {
            return "Module error: " + e.getMessage();
        } catch (InterpretationException e) {
            reporter.report(e);
            return reporter.formatAll();
        } catch (RuntimeException e) {
            return "Runtime error: " + e.getMessage();
        }
        return null;
    }

    private void collectDeclaredNames(Statement.Block block) {
        for (Statement statement : block.statements()) {
            switch (statement) {
                case Statement.VarDecl vd -> declaredNames.add(vd.name());
                case Statement.VarDeclGroup group ->
                        group.decls().forEach(decl -> declaredNames.add(decl.name()));
                case Statement.Import imp -> declaredNames.addAll(imp.names());
                default -> {
                }
            }
        }
    }
}
