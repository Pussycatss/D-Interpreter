package pussycats.interpreter.ast;

import pussycats.interpreter.SourceLocation;
import pussycats.interpreter.SourceSpan;
import pussycats.interpreter.lexer.TokenType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * An AST node that produces a value, such as a literal, a variable
 * or an operation on other expressions.
 */
public sealed interface Expression extends Node {

    // --- Literals ---
    record IntLit(BigInteger value, SourceSpan span)                                            implements Expression {}
    record RealLit(BigDecimal value, SourceSpan span)                                           implements Expression {}
    record StringLit(String value, SourceSpan span)                                             implements Expression {}
    record BoolLit(boolean value, SourceSpan span)                                              implements Expression {}
    record NoneLit(SourceSpan span)                                                             implements Expression {}

    // --- References ---
    record Ident(String name, SourceSpan span)                                                  implements Expression {}
    /** Element access by an expression: {@code a[i]}. */
    record Index(Expression target, Expression index, SourceSpan span)                          implements Expression {}
    /** Tuple field access by name: {@code p.x}. */
    record Field(Expression target, String name, SourceSpan span)                               implements Expression {}
    /** Tuple field access by position: {@code t.2}. */
    record FieldIndex(Expression target, int index, SourceSpan span)                            implements Expression {}

    // --- Operators ---
    record Call(Expression callee, List<Expression> args, SourceSpan span)                      implements Expression {
        @Override
        public SourceLocation loc() {
            return span.start();
        }
    }
    /** Operation with one operand, e.g. {@code -x} or {@code not x}. */
    record Unary(TokenType op, Expression operand, SourceSpan span)                             implements Expression {}
    record Binary(Expression left, TokenType op, Expression right, SourceSpan span)             implements Expression {
        @Override
        public SourceLocation loc() {
            return span.start();
        }
    }
    /** Type check: {@code x is int}. */
    record IsCheck(Expression target, TypeIndicator type, SourceSpan span)                      implements Expression {}

    // --- Composite ---
    record ArrayLit(List<Expression> elements, SourceSpan span)                                 implements Expression {}
    record TupleLit(List<TupleEntry> entries, SourceSpan span)                                  implements Expression {}
    /**
     * Function literal.
     *
     * @param body    an {@link Expression} for {@code => expr}, or a block of statements
     * @param isBlock true if the body is a statement block, false if it is a single expression
     */
    record FuncLit(List<Parameter> params, Node body, boolean isBlock, SourceSpan span)         implements Expression {}
    record Parameter(String name, SourceSpan span) {}

    /**
     * One element of a tuple literal.
     *
     * @param name  field name, or {@code null} if the element is unnamed
     * @param value field value
     */
    record TupleEntry(String name, Expression value, SourceSpan span) {}
}
