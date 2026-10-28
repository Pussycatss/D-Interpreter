package pussycats.interpreter.ast;

/**
 * Root of the D abstract syntax tree.
 * <p>
 * Every node is either an {@link Expression} or a {@link Statement}.
 */
public sealed interface Node permits Expression, Statement {}