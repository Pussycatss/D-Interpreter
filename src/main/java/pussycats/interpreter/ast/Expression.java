package pussycats.interpreter.ast;

import pussycats.interpreter.lexer.TokenType;

import java.math.BigInteger;
import java.util.List;

public sealed interface Expression extends Node {

    record IntLit(BigInteger value)                                 implements Expression {}
    record RealLit(double value)                                    implements Expression {}
    record StringLit(String value)                                  implements Expression {}
    record BoolLit(boolean value)                                   implements Expression {}
    record NoneLit()                                                implements Expression {}
    record Ident(String name)                                       implements Expression {}
    record ArrayLit(List<Expression> elements)                      implements Expression {}
    record TupleLit(List<TupleEntry> entries)                       implements Expression {}
    record FuncLit(List<String> params, Node body, boolean isBlock) implements Expression {}

    record Unary(TokenType op, Expression operand)                  implements Expression {}
    record Binary(Expression left, TokenType op, Expression right)  implements Expression {}
    record Index(Expression target, Expression index)               implements Expression {}
    record Call(Expression callee, List<Expression> args)           implements Expression {}
    record Field(Expression target, String name)                    implements Expression {}
    record FieldIndex(Expression target, int index)                 implements Expression {}
    record IsCheck(Expression target, TypeIndicator type)           implements Expression {}

    record TupleEntry(String name, Expression value) {}
}
