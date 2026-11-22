package pussycats.interpreter.ast;

import pussycats.interpreter.SourceLocation;
import pussycats.interpreter.SourceSpan;

/**
 * Root of the D abstract syntax tree.
 * <p>
 * Every node is either an {@link Expression} or a {@link Statement}.
 */
public sealed interface Node permits Expression, Statement {
    SourceSpan span();

    default SourceLocation loc() {
        return span().start();
    }
}