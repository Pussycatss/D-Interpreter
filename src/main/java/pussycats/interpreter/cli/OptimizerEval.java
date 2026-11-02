package pussycats.interpreter.cli;

import pussycats.interpreter.ast.AstPrinter;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.parser.Parser;
import pussycats.interpreter.semantic.Optimizer;

import java.util.List;

public class OptimizerEval implements Evaluable {
    private final AstPrinter astPrinter = new AstPrinter();
    private final Optimizer optimizer = new Optimizer();

    public String eval(String line) {
        List<Token> tokens = new Lexer(line, "<stdin>").scan();
        Parser parser = new Parser(tokens, line, "<stdin>");
        Node ast = parser.parse();

        if (parser.hasErrors() || ast == null) {
            return parser.formatErrors();
        }

        // In REPL mode, don't remove unused variables - user may want to use them later
        Node optimized = optimizer.runFullOptimization(ast, false);

        return "Original AST:\n" +
                astPrinter.print(ast) +
                "\n\nOptimized AST:\n" +
                astPrinter.print(optimized);
    }
}
