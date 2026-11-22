package pussycats.interpreter.ast;

import pussycats.interpreter.SourceSpan;

import java.util.List;

public sealed interface Statement extends Node {
    record VarDecl(String name, Expression init, SourceSpan span)                                implements Statement {}
    record VarDeclGroup(List<VarDecl> decls, SourceSpan span)                                    implements Statement {}
    record Assign(Expression target, Expression value, SourceSpan span)                          implements Statement {}
    record PrintStatement(List<Expression> values, SourceSpan span)                              implements Statement {}

    record IfStatement(Expression cond, Statement then, Statement otherwise, SourceSpan span)    implements Statement {}
            // otherwise may be null
    record WhileStatement(Expression cond, Statement.Block body, SourceSpan span)                implements Statement {}
    record LoopStatement(Statement.Block body, SourceSpan span)                                  implements Statement {}
    record ForRangeStatement(String var, Expression from, Expression to, Statement.Block body, SourceSpan span)
            implements Statement {} // var may be null
    record ForInStatement(String var, Expression iterable, Statement.Block body, SourceSpan span)
            implements Statement {}
    record ExitStatement(SourceSpan span)                                                        implements Statement {}
    record ReturnStatement(Expression value, SourceSpan span)                                    implements Statement {}
            // value may be null

    record Block(List<Statement> statements, SourceSpan span)                                    implements Statement {}
}