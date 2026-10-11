package pussycats.interpreter.ast;

import java.util.List;

public sealed interface Statement extends Node {
    record VarDecl(String name, Expression init)                                               implements Statement {}
    record VarDeclGroup(List<VarDecl> decls)                                                   implements Statement {}
    record Assign(Expression target, Expression value)                                         implements Statement {}
    record ExpressionStatement(Expression expression)                                          implements Statement {}
    record PrintStatement(List<Expression> values)                                             implements Statement {}

    record IfStatement(Expression cond, Statement then, Statement otherwise)                   implements Statement {} // otherwise may be null
    record WhileStatement(Expression cond, List<Statement> body)                               implements Statement {}
    record LoopStatement(List<Statement> body)                                                 implements Statement {}
    record ForRangeStatement(String var, Expression from, Expression to, List<Statement> body) implements Statement {} // var may be null
    record ForInStatement(String var, Expression iterable, List<Statement> body)               implements Statement {}
    record ExitStatement()                                                                     implements Statement {}
    record ReturnStatement(Expression value)                                                   implements Statement {} // value may be null

    record Block(List<Statement> statements)                                                   implements Statement {}
}