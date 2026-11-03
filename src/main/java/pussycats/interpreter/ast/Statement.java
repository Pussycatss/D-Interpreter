package pussycats.interpreter.ast;

import java.util.List;

public sealed interface Statement extends Node {
    record VarDecl(String name, Expression init, SourceLocation loc)                            implements Statement {}
    record VarDeclGroup(List<VarDecl> decls, SourceLocation loc)                                implements Statement {}
    record Assign(Expression target, Expression value, SourceLocation loc)                      implements Statement {}
    record ExpressionStatement(Expression expression, SourceLocation loc)                       implements Statement {}
    record PrintStatement(List<Expression> values, SourceLocation loc)                          implements Statement {}

    record IfStatement(Expression cond, Statement then, Statement otherwise, SourceLocation loc) implements Statement {}
            // otherwise may be null
    record WhileStatement(Expression cond, Statement.Block body, SourceLocation loc)            implements Statement {}
    record LoopStatement(Statement.Block body, SourceLocation loc)                              implements Statement {}
    record ForRangeStatement(String var, Expression from, Expression to, Statement.Block body, SourceLocation loc)
            implements Statement {} // var may be null
    record ForInStatement(String var, Expression iterable, Statement.Block body, SourceLocation loc)
            implements Statement {}
    record ExitStatement(SourceLocation loc)                                                    implements Statement {}
    record ReturnStatement(Expression value, SourceLocation loc)                                implements Statement {}
            // value may be null

    record Block(List<Statement> statements, SourceLocation loc)                                implements Statement {}
}