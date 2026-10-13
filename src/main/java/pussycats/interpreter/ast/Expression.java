package pussycats.interpreter.ast;

import pussycats.interpreter.lexer.TokenType;

import java.math.BigInteger;
import java.util.List;

/**
 * An AST node that produces a value, such as a literal, a variable
 * or an operation on other expressions.
 */
public sealed interface Expression extends Node {

    record IntLit(BigInteger value)                                 implements Expression {}
    record RealLit(double value)                                    implements Expression {}
    record StringLit(String value)                                  implements Expression {}
    record BoolLit(boolean value)                                   implements Expression {}
    record NoneLit()                                                implements Expression {}
    record Ident(String name)                                       implements Expression {}
    record ArrayLit(List<Expression> elements)                      implements Expression {}
    record TupleLit(List<TupleEntry> entries)                       implements Expression {}

    /**
     * Function literal.
     *
     * @param body    an {@link Expression} for {@code => expr}, or a block of statements
     * @param isBlock true if the body is a statement block, false if it is a single expression
     */
    record FuncLit(List<String> params, Node body, boolean isBlock) implements Expression {}

    /** Operation with one operand, e.g. {@code -x} or {@code not x}. */
    record Unary(TokenType op, Expression operand)                  implements Expression {}
    record Binary(Expression left, TokenType op, Expression right)  implements Expression {}
    /** Element access by an expression: {@code a[i]}. */
    record Index(Expression target, Expression index)               implements Expression {}
    record Call(Expression callee, List<Expression> args)           implements Expression {}
    /** Tuple field access by name: {@code p.x}. */
    record Field(Expression target, String name)                    implements Expression {}
    /** Tuple field access by position: {@code t.2}. */
    record FieldIndex(Expression target, int index)                 implements Expression {}
    /** Type check: {@code x is int}. */
    record IsCheck(Expression target, TypeIndicator type)           implements Expression {}

    /**
     * One element of a tuple literal.
     *
     * @param name  field name, or {@code null} if the element is unnamed
     * @param value field value
     */
    record TupleEntry(String name, Expression value) {}
}
