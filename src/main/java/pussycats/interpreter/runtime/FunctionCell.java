package pussycats.interpreter.runtime;

import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;

import java.util.List;

/**
 * Runtime function / closure. Holds the parameter list, body, and the
 * environment (frame) that was active when the function literal was
 * evaluated. The captured frame's parent chain gives the lexical scope.
 * <p>
 * This is NOT a Cell — it's the *contents* of a cell. A function literal
 * evaluates to ImmutableCell(new FunctionCell(...)); a variable bound to
 * a function is MutableCell(new FunctionCell(...)).
 */
public record FunctionCell(
        String name,
        List<Expression.Parameter> params,
        Node body,
        StackFrame closure,
        String SourceName
) {
}