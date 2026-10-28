package pussycats.interpreter.cli;

import pussycats.interpreter.ast.AstPrinter;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.lexer.Lexer;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.parser.Parser;

import java.util.List;

public class ParserEval implements Evaluable {
    private final AstPrinter astPrinter = new AstPrinter();

    public String eval(String line) {
        List<Token> tokens = new Lexer(line, "<stdin>").scan();
        Parser parser = new Parser(tokens, line, "<stdin>");
        Node ast = parser.parse();

        if (parser.hasErrors() || ast == null) {
            return parser.formatErrors();
        }

        return astPrinter.print(ast);
    }
}
