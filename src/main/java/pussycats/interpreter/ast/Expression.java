package pussycats.interpreter.ast;

import pussycats.interpreter.lexer.TokenType;

import java.math.BigInteger;
import java.util.List;

/**
 * An AST node that produces a value, such as a literal, a variable
 * or an operation on other expressions.
 */
public sealed interface Expression extends Node {

    // --- Literals ---
    record IntLit(BigInteger value, SourceLocation loc)                                 implements Expression {}
    record RealLit(double value, SourceLocation loc)                                    implements Expression {}
    record StringLit(String value, SourceLocation loc)                                  implements Expression {}
    record BoolLit(boolean value, SourceLocation loc)                                   implements Expression {}
    record NoneLit(SourceLocation loc)                                                  implements Expression {}

    // --- References ---
    record Ident(String name, SourceLocation loc)                                       implements Expression {}
    /** Element access by an expression: {@code a[i]}. */
    record Index(Expression target, Expression index, SourceLocation loc)               implements Expression {}
    /** Tuple field access by name: {@code p.x}. */
    record Field(Expression target, String name, SourceLocation loc)                    implements Expression {}
    /** Tuple field access by position: {@code t.2}. */
    record FieldIndex(Expression target, int index, SourceLocation loc)                 implements Expression {}

    // --- Operators ---
    record Call(Expression callee, List<Expression> args, SourceLocation loc)           implements Expression {}
    /** Operation with one operand, e.g. {@code -x} or {@code not x}. */
    record Unary(TokenType op, Expression operand, SourceLocation loc)                  implements Expression {}
    record Binary(Expression left, TokenType op, Expression right, SourceLocation loc)  implements Expression {}
    /** Type check: {@code x is int}. */
    record IsCheck(Expression target, TypeIndicator type, SourceLocation loc)           implements Expression {}

    // --- Composite ---
    record ArrayLit(List<Expression> elements, SourceLocation loc)                      implements Expression {}
    record TupleLit(List<TupleEntry> entries, SourceLocation loc)                       implements Expression {}
    /**
     * Function literal.
     *
     * @param body    an {@link Expression} for {@code => expr}, or a block of statements
     * @param isBlock true if the body is a statement block, false if it is a single expression
     */
    record FuncLit(List<Parameter> params, Node body, boolean isBlock, SourceLocation loc) implements Expression {}
    record Parameter(String name, SourceLocation loc) {}

    /**
     * One element of a tuple literal.
     *
     * @param name  field name, or {@code null} if the element is unnamed
     * @param value field value
     */
    record TupleEntry(String name, Expression value, SourceLocation loc) {}
}
