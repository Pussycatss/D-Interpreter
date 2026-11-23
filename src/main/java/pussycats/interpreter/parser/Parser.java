package pussycats.interpreter.parser;

import pussycats.interpreter.SourceLocation;
import pussycats.interpreter.SourceSpan;
import pussycats.interpreter.ast.*;
import pussycats.interpreter.error.ErrorReporter;
import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.lexer.TokenType;
import pussycats.interpreter.lexer.Keywords;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser: turns the token list from the lexer into an AST.
 * <p>
 * Each grammar rule is a {@code parseXxx} method. Expression methods are ordered
 * by precedence, from the lowest ({@code or/and/xor}) to the highest (atoms).
 * <p>
 * Errors do not stop the parser: they are collected in an {@link ErrorReporter},
 * and the parser skips to the next statement ({@link #synchronize()}) and continues,
 * so several errors can be reported in one run.
 */
public final class Parser {

    /** Internal control-flow signal only — never escapes this class. */
    private static final class ParsePanic extends RuntimeException {
        ParsePanic() {
            super(null, null, false, false); // no message/stack trace: pure signal
        }
    }

    private final List<Token> tokens;
    private final ErrorReporter reporter;
    private int current = 0;

    public Parser(List<Token> tokens, String source, String filename) {
        this.tokens = tokens;
        this.reporter = new ErrorReporter(source, filename);
    }

    public boolean hasErrors() {
        return reporter.hasErrors();
    }

    /** Returns all collected errors as one formatted string. */
    public String formatErrors() {
        return reporter.formatAll();
    }

    /**
     * Parses the whole program.
     *
     * @return the root {@link Statement.Block}, or {@code null} if any error was found
     */
    public Node parse() {
        try {
            Node ast = parseProgram();
            return hasErrors() ? null : ast;
        } catch (ParsePanic ignored) {
            return null;
        }
    }

    /**
     * Parses top-level statements. A failed statement is skipped via
     * {@link #synchronize()}, so parsing continues after an error.
     */
    private Statement.Block parseProgram() {
        List<Statement> statements = new ArrayList<>();
        skipSeparators();
        while (!isAtEnd()) {
            try {
                statements.add(parseStatement());
                requireStatementSeparator();
            } catch (ParsePanic ignored) {
                synchronize();
            }
            skipSeparators();
        }
        SourceLocation start = statements.isEmpty()
                ? new SourceLocation(1, 1)
                : statements.getFirst().span().start();

        SourceLocation end = statements.isEmpty()
                ? start
                : statements.getLast().span().end();

        return new Statement.Block(statements, new SourceSpan(start, end));
    }

    /** Parses statements inside a construct, up to (not including) {@code end} or {@code else}. */
    private List<Statement> parseBody() {
        List<Statement> statements = new ArrayList<>();
        skipSeparators();
        while (!isAtEnd() && !check(TokenType.END) && !check(TokenType.ELSE)) {
            try {
                if (check(TokenType.IMPORT)) {
                    throw panic(peek(),
                            "Imports are only allowed at the top level",
                            "move this import declaration outside the current block");
                }
                statements.add(parseStatement());
                if (!isAtEnd() && !check(TokenType.END) && !check(TokenType.ELSE)) {
                    requireStatementSeparator(TokenType.END, TokenType.ELSE);
                }
            } catch (ParsePanic ignored) {
                synchronize();
            }
            skipSeparators();
        }
        return statements;
    }

    private Statement parseStatement() {
        if (match(TokenType.IMPORT)) return parseImport();
        if (match(TokenType.VAR))    return parseVarDecl();
        if (match(TokenType.PRINT))  return parsePrint();
        if (match(TokenType.IF))     return parseIf();
        if (match(TokenType.WHILE))  return parseWhile();
        if (match(TokenType.LOOP))   return parseLoop();
        if (match(TokenType.FOR))    return parseFor();
        if (match(TokenType.EXIT))   return parseExit();
        if (match(TokenType.RETURN)) return parseReturn();
        if (isAtStatementBoundary()) {
            throw panic(peek(),
                    "Expected a statement",
                    "this position needs a statement: try 'print', 'var', 'if', " +
                              "'while', 'loop', 'for', or an assignment using ':='");
        }
        checkKeywordTypo();
        return parseAssignment();
    }

    private void checkKeywordTypo() {
        if (!check(TokenType.IDENTIFIER)) return;
        String close = findClosestKeyword(peek().lexeme());
        if (close == null) return;
        TokenType next = peekAt(1).type();
        if (next == TokenType.ASSIGN
                || next == TokenType.DOT
                || next == TokenType.LBRACKET
                || next == TokenType.LPAREN
                || isExpressionOperator(next)) {
            return;
        }

        throw panic(peek(), "Unknown statement",
                "did you mean '" + close + "'?");
    }

    private static boolean isExpressionOperator(TokenType t) {
        return switch (t) {
            case PLUS, MINUS, ASTERISK, SLASH,
                 LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
                 EQUAL, NOT_EQUAL, AND, OR, XOR, IS -> true;
            default -> false;
        };
    }

    private Statement parseImport() {
        Token open = previous();
        List<String> names = new ArrayList<>();
        do {
            Token name = consume(TokenType.IDENTIFIER, "Expected an import name after 'import'");
            names.add(name.lexeme());
        } while (match(TokenType.COMMA));
        consume(TokenType.FROM, "Expected 'from' after imported names");
        Token path = consume(TokenType.STRING, "Expected a module path after 'from'");
        return new Statement.Import(names, (String) path.literal(),
                span(new SourceLocation(open.line(), open.column()), path));
    }

    /** {@code var a := 1, b, c := 3}: one declaration, or a {@code VarDeclGroup} if there are several. */
    private Statement parseVarDecl() {
        Token open = previous();
        SourceLocation loc = new SourceLocation(open.line(), open.column());
        List<Statement.VarDecl> decls = new ArrayList<>();
        do {
            Token name = consume(TokenType.IDENTIFIER, "Expected a variable name after 'var'");
            Expression init = null;
            if (match(TokenType.ASSIGN)) {
                init = parseExpression();
            } else if (check(TokenType.EQUAL)) {
                throw panic(peek(), "Expected ':=' after variable name", null);
            }
            SourceLocation start = new SourceLocation(name.line(), name.column());
            SourceLocation end = init == null
                    ? new SourceLocation(name.line(), name.column() + name.lexeme().length())
                    : init.span().end();
            decls.add(new Statement.VarDecl(name.lexeme(), init, new SourceSpan(start, end)));
        } while (match(TokenType.COMMA));
        return decls.size() == 1
                ? decls.getFirst()
                : new Statement.VarDeclGroup(decls, new SourceSpan(loc, decls.getLast().span().end()));
    }

    private Statement parsePrint() {
        Token open = previous();
        List<Expression> values = new ArrayList<>();
        do values.add(parseExpression());
        while (match(TokenType.COMMA));
        return new Statement.PrintStatement(values,
                new SourceSpan(new SourceLocation(open.line(), open.column()), values.getLast().span().end()));
    }

    /** Parses an expression; if it is followed by {@code :=}, it becomes an assignment. */
    private Statement parseAssignment() {
        Token open = peek();
        Expression expr = parseExpression();
        if (!match(TokenType.ASSIGN)) {
            throw panic(peek(),
                    "Expected ':=' after reference",
                    "every statement must be one of: var, print, if, while, " +
                              "loop, for, exit, return, or an assignment using ':='");
        }
        Expression value = parseExpression();
        return new Statement.Assign(expr, value,
                new SourceSpan(new SourceLocation(open.line(), open.column()), value.span().end()));
    }

    /** Supports both forms: {@code if c => stmt} and {@code if c then ... [else ...] end}. */
    private Statement parseIf() {
        Token open = previous();
        Expression cond = parseExpression();
        SourceLocation loc = new SourceLocation(open.line(), open.column());
        if (match(TokenType.ARROW)) {
            Statement s = parseStatement();
            return new Statement.IfStatement(cond, new Statement.Block(List.of(s), new SourceSpan(loc, s.span().end())),
                    null, new SourceSpan(loc, s.span().end()));
        }

        consume(TokenType.THEN, "Expected 'then' or '=>' after condition of 'if'");
        List<Statement> body = parseBody();
        Statement then = new Statement.Block(body, new SourceSpan(loc, bodyEnd(body, loc)));
        Statement otherwise = null;
        if (match(TokenType.ELSE)) {
            Token elseToken = previous();
            List<Statement> elseBody = parseBody();
            SourceLocation elseStart =
                    new SourceLocation(elseToken.line(), elseToken.column());
            otherwise = new Statement.Block(
                    elseBody,
                    new SourceSpan(elseStart, bodyEnd(elseBody, elseStart))
            );
        }
        Token endToken = consumeEnd("if", open);
        return new Statement.IfStatement(cond, then, otherwise, span(loc, endToken));
    }

    private Statement parseWhile() {
        Token open = previous();
        Expression cond = parseExpression();
        consume(TokenType.LOOP, "Expected 'loop' after condition of 'while'");
        Statement.Block body = parseBodyBlock(open);
        Token endToken = consumeEnd("while", open);
        return new Statement.WhileStatement(cond, body,
                span(new SourceLocation(open.line(), open.column()), endToken));
    }

    private Statement parseLoop() {
        Token open = previous();
        Statement.Block body = parseBodyBlock(open);
        Token endToken = consumeEnd("loop", open);
        return new Statement.LoopStatement(body, span(new SourceLocation(open.line(), open.column()), endToken));
    }

    /**
     * Parses all forms of {@code for}:
     * <p>
     *   {@code for loop ... end} – infinite loop
     *   {@code for [i in] a..b loop ... end} – range loop
     *   {@code for [i in] iterable loop ... end} – loop over a collection
     *
     */
    private Statement parseFor() {
        Token open = previous();
        SourceLocation loc = new SourceLocation(open.line(), open.column());

        if (check(TokenType.LOOP)) {
            advance();
            Statement.Block body = parseBodyBlock(open);
            Token endToken = consumeEnd("for", open);
            return new Statement.LoopStatement(body, span(loc, endToken));
        }

        String var = null;
        if (check(TokenType.IDENTIFIER) && peekAt(1).type() == TokenType.IN) {
            var = advance().lexeme();
            advance();
        }

        Expression first = parseExpression();

        if (match(TokenType.RANGE)) {
            Expression to = parseExpression();
            consume(TokenType.LOOP, "Expected 'loop' in 'for'");
            Statement.Block body = parseBodyBlock(open);
            Token endToken = consumeEnd("for", open);
            return new Statement.ForRangeStatement(var, first, to, body, span(loc, endToken));
        }

        consume(TokenType.LOOP, "Expected 'loop' in 'for'");
        Statement.Block body = parseBodyBlock(open);
        Token endToken = consumeEnd("for", open);
        return new Statement.ForInStatement(var, first, body, span(loc, endToken));
    }

    /** {@code exit} statement. */
    private Statement parseExit() {
        Token open = previous();
        return new Statement.ExitStatement(span(new SourceLocation(open.line(), open.column()), open));
    }

    /** {@code return} may have no value if the statement ends right after it. */
    private Statement parseReturn() {
        Token open = previous();
        Expression value = isAtStatementBoundary() ? null : parseExpression();
        SourceLocation start = new SourceLocation(open.line(), open.column());
        SourceLocation end = value == null
                ? span(start, open).end()
                : value.span().end();
        return new Statement.ReturnStatement(value, new SourceSpan(start, end));
    }

    // ============================================================
    // Expressions, from lowest to highest precedence:
    // or/and/xor -> relation -> + - -> * / -> unary -> postfix -> atom
    // ============================================================

    private Expression parseExpression() {
        Expression left = parseXor();
        TokenType or = TokenType.OR;
        while (match(or)) {
            Expression right = parseXor();
            left = new Expression.Binary(left, or, right, new SourceSpan(left.span().start(), right.span().end()));
        }
        return left;
    }

    private Expression parseXor() {
        Expression left = parseAnd();
        TokenType xor = TokenType.XOR;
        while (match(xor)) {
            Expression right = parseAnd();
            left = new Expression.Binary(left, xor, right, new SourceSpan(left.span().start(), right.span().end()));
        }
        return left;
    }

    private Expression parseAnd() {
        Expression left = parseRelation();
        TokenType and = TokenType.AND;
        while (match(and)) {
            Expression right = parseXor();
            left = new Expression.Binary(left, and, right, new SourceSpan(left.span().start(), right.span().end()));
        }
        return left;
    }

    /** Comparison operators are not chained: {@code a < b < c} is not allowed. */
    private Expression parseRelation() {
        Expression left = parseIsExpr();
        if (match(TokenType.LESS, TokenType.LESS_EQUAL,
                TokenType.GREATER, TokenType.GREATER_EQUAL,
                TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            TokenType op = previous().type();
            Expression right = parseIsExpr();
            return new Expression.Binary(left, op, right, new SourceSpan(left.span().start(), right.span().end()));
        }
        return left;
    }

    private Expression parseIsExpr() {
        Expression expr = parseTerm();
        if (match(TokenType.IS)) {
            ParsedTypeIndicator parsed = parseTypeIndicator();
            return new Expression.IsCheck(expr, parsed.type(),
                    new SourceSpan(expr.span().start(), parsed.span().end()));
        }
        return expr;
    }

    /** Addition and subtraction (left-associative). */
    private Expression parseTerm() {
        Expression left = parseFactor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            TokenType op = previous().type();
            Expression right = parseFactor();
            left = new Expression.Binary(left, op, right, new SourceSpan(left.span().start(), right.span().end()));
        }
        return left;
    }

    /** Multiplication and division (left-associative). */
    private Expression parseFactor() {
        Expression left = parseUnary();
        while (match(TokenType.ASTERISK, TokenType.SLASH)) {
            TokenType op = previous().type();
            Expression right = parseUnary();
            left = new Expression.Binary(left, op, right, new SourceSpan(left.span().start(), right.span().end()));
        }
        return left;
    }

    /** Prefix {@code + - not}, or a postfix type check {@code expr is type}. */
    private Expression parseUnary() {
        if (match(TokenType.PLUS, TokenType.MINUS, TokenType.NOT)) {
            Token open = previous();
            TokenType op = previous().type();
            Expression operand = parseUnary();
            return new Expression.Unary(op, operand,
                    new SourceSpan(new SourceLocation(open.line(), open.column()), operand.span().end()));
        }
        return parsePrimary();
    }

    /** An atom followed by any number of postfix operations: {@code [i]}, {@code (args)}, {@code .name}, {@code .1}. */
    private Expression parsePrimary() {
        Expression expr = parseAtom();
        while (true) {
            if (match(TokenType.LBRACKET)) {
                Token open = previous();
                Expression index = parseExpression();
                Token close = closeDelimiter(TokenType.RBRACKET, open, "]", "index expression");
                expr = new Expression.Index(expr, index, new SourceSpan(expr.span().start(),
                        span(new SourceLocation(open.line(), open.column()), close).end()));
            } else if (match(TokenType.LPAREN)) {
                Token open = previous();
                List<Expression> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        skipOptionalNewlines();
                        args.add(parseExpression());
                        skipOptionalNewlines();
                    } while (match(TokenType.COMMA));
                }
                Token close = closeDelimiter(TokenType.RPAREN, open, ")", "argument list");
                expr = new Expression.Call(expr, args, new SourceSpan(expr.span().start(), span(expr.loc(), close).end()));
            } else if (match(TokenType.DOT)) {
                Token open = previous();
                SourceLocation loc = new SourceLocation(open.line(), open.column());
                if (match(TokenType.IDENTIFIER)) {
                    Token prev = previous();
                    expr = new Expression.Field(expr, prev.lexeme(),
                            new SourceSpan(expr.span().start(), span(loc, prev).end()));
                } else if (match(TokenType.INTEGER)) {
                    Token prev = previous();
                    BigInteger big = new BigInteger(prev.lexeme());
                    if (big.bitLength() > 31) {
                        throw panic(prev, "Field index is too large",
                                "a tuple field index must fit in a 32-bit signed integer");
                    }
                    int n = big.intValue();
                    expr = new Expression.FieldIndex(expr, n,
                            new SourceSpan(expr.span().start(), span(loc, prev).end()));
                } else {
                    throw error(peek(), "Expected a name or index after '.'");
                }
            } else {
                return expr;
            }
        }
    }

    /** Literals, identifiers, parenthesized expressions, arrays, tuples and functions. */
    private Expression parseAtom() {
        if (match(TokenType.INTEGER)) {
            Token open = previous();
            return new Expression.IntLit(new BigInteger(previous().lexeme()),
                    span(new SourceLocation(open.line(), open.column()), open));
        }
        if (match(TokenType.REAL)) {
            Token open = previous();
            return new Expression.RealLit(Double.parseDouble(previous().lexeme()),
                    span(new SourceLocation(open.line(), open.column()), open));
        }
        if (match(TokenType.STRING)) {
            Token open = previous();
            return new Expression.StringLit((String) previous().literal(),
                    span(new SourceLocation(open.line(), open.column()), open));
        }
        if (match(TokenType.TRUE)) {
            Token open = previous();
            return new Expression.BoolLit(true, span(new SourceLocation(open.line(), open.column()), open));
        }
        if (match(TokenType.FALSE)) {
            Token open = previous();
            return new Expression.BoolLit(false, span(new SourceLocation(open.line(), open.column()), open));
        }
        if (match(TokenType.NONE)) {
            Token open = previous();
            return new Expression.NoneLit(span(new SourceLocation(open.line(), open.column()), open));
        }
        if (match(TokenType.IDENTIFIER)) {
            Token open = previous();
            return new Expression.Ident(previous().lexeme(),
                    span(new SourceLocation(open.line(), open.column()), open));
        }
        if (match(TokenType.LPAREN)) {
            Token open = previous();
            Expression inner = parseExpression();
            closeDelimiter(TokenType.RPAREN, open, ")", "parenthesized expression");
            return inner;
        }
        if (match(TokenType.LBRACKET)) {
            Token open = previous();
            return parseArrayLit(open);
        }
        if (match(TokenType.LBRACE)) {
            Token open = previous();
            return parseTupleLit(open);
        }
        if (match(TokenType.FUNC)) {
            return parseFuncLit();
        }

        throw error(peek(), "Expected an expression");
    }

    private Expression parseArrayLit(Token open) {
        List<Expression> elems = new ArrayList<>();
        if (!check(TokenType.RBRACKET)) {
            do {
                skipOptionalNewlines();
                elems.add(parseExpression());
                skipOptionalNewlines();
            } while (match(TokenType.COMMA));
        }
        Token close = closeDelimiter(TokenType.RBRACKET, open, "]", "array literal");
        return new Expression.ArrayLit(elems, span(new SourceLocation(open.line(), open.column()), close));
    }

    /** Tuple elements may be named ({@code x := 1}) or unnamed ({@code 1}). */
    private Expression parseTupleLit(Token open) {
        List<Expression.TupleEntry> entries = new ArrayList<>();
        SourceLocation loc = new SourceLocation(open.line(), open.column());
        if (!check(TokenType.RBRACE)) {
            do {
                skipOptionalNewlines();
                if (check(TokenType.IDENTIFIER) && peekAt(1).type() == TokenType.ASSIGN) {
                    Token nameTok = advance();
                    advance();
                    Expression value = parseExpression();
                    entries.add(new Expression.TupleEntry(nameTok.lexeme(), value,
                            new SourceSpan(new SourceLocation(nameTok.line(), nameTok.column()), value.span().end())));
                } else if (check(TokenType.IDENTIFIER) && peekAt(1).type() == TokenType.EQUAL) {
                    throw panic(peekAt(1), "Expected ':=' after tuple element name", null);
                } else {
                    Expression value = parseExpression();
                    entries.add(new Expression.TupleEntry(null, value, value.span()));
                }
                skipOptionalNewlines();
            } while (match(TokenType.COMMA));
        }
        Token close = closeDelimiter(TokenType.RBRACE, open, "}", "tuple literal");
        return new Expression.TupleLit(entries, span(loc, close));
    }

    /** {@code func(params) => expr} or {@code func(params) is ... end}; the parameter list is optional. */
    private Expression parseFuncLit() {
        List<Expression.Parameter> params = new ArrayList<>();
        Token funcTok = previous();
        if (match(TokenType.LPAREN)) {
            Token open = previous();
            if (!check(TokenType.RPAREN)) {
                do {
                    skipOptionalNewlines();
                    Token paramTok = consume(TokenType.IDENTIFIER, "Expected parameter name");
                    params.add(new Expression.Parameter(paramTok.lexeme(),
                            span(new SourceLocation(paramTok.line(), paramTok.column()), paramTok)));
                    skipOptionalNewlines();
                } while (match(TokenType.COMMA));
            }
            closeDelimiter(TokenType.RPAREN, open, ")", "parameter list");
        }
        SourceLocation loc = new SourceLocation(funcTok.line(), funcTok.column());
        if (match(TokenType.ARROW)) {
            Expression body = parseExpression();
            return new Expression.FuncLit(params, body, false, new SourceSpan(loc, body.span().end()));
        }
        if (match(TokenType.IS)) {
            List<Statement> bodyList = parseBody();
            Statement.Block body = new Statement.Block(bodyList, new SourceSpan(loc, bodyEnd(bodyList, loc)));
            Token endToken = consumeEnd("func", funcTok);
            return new Expression.FuncLit(params, body, true, span(loc, endToken));
        }
        throw panic(peek(),
                "Expected '=>' or 'is' in function literal",
                "the 'func' at " + pos(funcTok)
                        + " needs a body: either '=> expr' or 'is ... end'");
    }

    /** Parses the type after {@code is}: {@code int, real, bool, string, none, func, [], {}}. */
    private ParsedTypeIndicator parseTypeIndicator() {
        if (match(TokenType.INT_TYPE)) {
            Token token = previous();
            return new ParsedTypeIndicator(TypeIndicator.INT, span(token));
        }
        if (match(TokenType.REAL_TYPE)) {
            Token token = previous();
            return new ParsedTypeIndicator(TypeIndicator.REAL, span(token));
        }
        if (match(TokenType.BOOL_TYPE)) {
            Token token = previous();
            return new ParsedTypeIndicator(TypeIndicator.BOOL, span(token));
        }
        if (match(TokenType.STRING_TYPE)) {
            Token token = previous();
            return new ParsedTypeIndicator(TypeIndicator.STRING, span(token));
        }
        if (match(TokenType.NONE)) {
            Token token = previous();
            return new ParsedTypeIndicator(TypeIndicator.NONE, span(token));
        }
        if (match(TokenType.FUNC)) {
            Token token = previous();
            return new ParsedTypeIndicator(TypeIndicator.FUNC, span(token));
        }
        if (match(TokenType.LBRACKET)) {
            Token open = previous();
            Token close = consume(TokenType.RBRACKET, "Expected ']' after '[' in type indicator");
            return new ParsedTypeIndicator(
                    TypeIndicator.VECTOR,
                    span(new SourceLocation(open.line(), open.column()), close)
            );
        }
        if (match(TokenType.LBRACE)) {
            Token open = previous();
            Token close = consume(TokenType.RBRACE,
                    "Expected '}' after '{' in type indicator");
            return new ParsedTypeIndicator(
                    TypeIndicator.TUPLE,
                    span(new SourceLocation(open.line(), open.column()), close)
            );
        }
        throw panic(peek(),
                "Expected a type indicator (int, real, bool, string, none, [], {}, func)",
                suggestType(peek()));
    }

    /** Consumes the closing token, or reports an error that mentions where the delimiter was opened. */
    private Token closeDelimiter(TokenType expected, Token openTok, String closer, String what) {
        if (match(expected)) {
            return previous();
        }
        Token got = peek();
        throw panic(got,
                "unclosed '" + openTok.lexeme() + "' in " + what,
                "the '" + openTok.lexeme() + "' at " + pos(openTok)
                        + " has no matching '" + closer + "'");
    }

    private static final List<String> TYPE_KEYWORDS = List.of("int", "real", "bool", "string", "none", "func");

    private String suggestType(Token got) {
        if (got.type() != TokenType.IDENTIFIER) return null;
        String close = closestIn(got.lexeme(), TYPE_KEYWORDS);
        return close == null ? null : "did you mean '" + close + "'?";
    }

    private Statement.Block parseBodyBlock(Token open) {
        SourceLocation start = new SourceLocation(open.line(), open.column());
        List<Statement> body = parseBody();
        SourceLocation end = body.isEmpty()
                ? start
                : body.getLast().span().end();
        return new Statement.Block(body, new SourceSpan(start, end));
    }

    /**
     * Checks that two statements are separated by a newline or {@code ;}.
     *
     * @param terminators tokens that may end the statement instead (e.g. {@code end}, {@code else})
     */
    private void requireStatementSeparator(TokenType... terminators) {
        if (isAtEnd() || check(TokenType.NEWLINE) || check(TokenType.SEMICOLON) || checkAny(terminators)) {
            return;
        }
        Token got = peek();
        throw panic(got, "Expected a newline or ';' between statements", null);
    }

    /** True if the current token ends a statement (separator, {@code end}, {@code else} or end of input). */
    private boolean isAtStatementBoundary() {
        return isAtEnd()
                || check(TokenType.NEWLINE)
                || check(TokenType.SEMICOLON)
                || check(TokenType.END)
                || check(TokenType.ELSE);
    }

    /**
     * Consumes {@code end}, or reports an error that points to the line where
     * the construct was opened.
     */
    private Token consumeEnd(String constructName, Token openTok) {
        if (match(TokenType.END)) {
            return previous();
        }
        Token got = peek();
        throw panic(got,
                "unclosed '" + constructName + "'",
                "the '" + constructName + "' at " + pos(openTok)
                        + " needs a matching 'end'");
    }

    private void skipOptionalNewlines() {
        while (check(TokenType.NEWLINE)) {
            advance();
        }
    }

    // ============================================================
    // Token helpers
    // ============================================================

    private static String pos(Token t) {
        return t.line() + ":" + t.column();
    }

    private Token peek() {
        return tokens.get(current);
    }

    /** Looks {@code offset} tokens ahead without consuming; returns EOF if past the end. */
    private Token peekAt(int offset) {
        int idx = current + offset;
        if (idx >= tokens.size()) {
            return tokens.getLast(); // EOF
        }
        return tokens.get(idx);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type() == type;
    }

    private boolean checkAny(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) {
                return true;
            }
        }
        return false;
    }

    /** Consumes the current token if it has one of the given types. */
    private boolean match(TokenType... types) {
        if (checkAny(types)) {
            advance();
            return true;
        }
        return false;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private void skipSeparators() {
        while (check(TokenType.NEWLINE) || check(TokenType.SEMICOLON)) {
            advance();
        }
    }

    /**
     * Panic-mode recovery: advances at least one token (guaranteeing progress),
     * then skips until a statement separator was just consumed.
     */
    private void synchronize() {
        if (!isAtEnd()) {
            advance();
        }
        while (!isAtEnd()) {
            if (previous().type() == TokenType.NEWLINE || previous().type() == TokenType.SEMICOLON) {
                return;
            }
            switch (peek().type()) {
                case IMPORT:
                case VAR:
                case IF:
                case WHILE:
                case FOR:
                case LOOP:
                case EXIT:
                case RETURN:
                case PRINT:
                case END:
                case ELSE:
                    return;
                default:
                    advance();
            }
        }
    }

    /** Consumes a token of the given type, or reports an error with the given message. */
    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        Token got = peek();
        throw panic(got, message, suggestFor(got, type));
    }

    private ParsePanic error(Token at, String message) {
        return panic(at, message, suggestFor(at, null));
    }

    /**
     * Reports an error and returns a {@link ParsePanic} to be thrown by the caller
     * ({@code throw panic(...)}). The error is recorded before the throw, so nothing is lost.
     *
     * @param suggestion optional hint such as "did you mean ...?", may be {@code null}
     */
    private ParsePanic panic(Token at, String message, String suggestion) {
        int len = switch (at.type()) {
            case EOF, NEWLINE -> 1;
            default -> Math.max(1, at.lexeme().length());
        };
        reporter.report("SyntaxError", message, suggestion, at.line(), at.column(), len);
        return new ParsePanic();
    }

    private SourceSpan span(SourceLocation start, Token end) {
        return new SourceSpan(
                start,
                new SourceLocation(end.line(), end.column() + end.lexeme().length())
        );
    }

    private SourceSpan span(Token token) {
        return span(new SourceLocation(token.line(), token.column()), token);
    }

    private SourceLocation bodyEnd(List<Statement> body, SourceLocation fallback) {
        return body.isEmpty() ? fallback : body.getLast().span().end();
    }

    // ============================================================
    // "Did you mean...?" heuristics
    // ============================================================

    private String suggestFor(Token got, TokenType expected) {
        if (expected == TokenType.ASSIGN && got.type() == TokenType.EQUAL) {
            return "did you mean ':=' instead of '='?";
        }
        if (expected == TokenType.END && got.type() == TokenType.EOF) {
            return "check for a missing 'end' earlier in the file";
        }
        if (got.type() == TokenType.IDENTIFIER) {
            String close = findClosestKeyword(got.lexeme());
            if (close != null) {
                return "did you mean '" + close + "'?";
            }
        }
        return null;
    }

    /**
     * Finds the candidate most similar to {@code text} (edit distance below 3).
     *
     * @return the best candidate, or {@code null} if nothing is close enough
     */
    private static String closestIn(String text, Iterable<String> candidates) {
        String lower = text.toLowerCase();
        String best = null;
        int bestDistance = Math.min(3, 1 + lower.length() / 3);
        for (String candidate : candidates) {
            if (candidate.equals(lower)) {
                continue;
            }
            int d = damerauLevenshtein(lower, candidate);
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        return best;
    }

    private static String findClosestKeyword(String text) {
        return closestIn(text, Keywords.getKeywords());
    }

    /**
     * Damerau–Levenshtein (optimal string alignment) edit distance: insertions,
     * deletions, substitutions, and transpositions of two adjacent characters.
     * <p>
     * The transposition rule is what makes {@code "pritn"} → {@code "print"} a
     * distance of 1 instead of 2, which matters for "did you mean" suggestions.
     */
    private static int damerauLevenshtein(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1,
                                             dp[i][j - 1] + 1),
                                             dp[i - 1][j - 1] + cost);
                if (i > 1 && j > 1
                        && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1)) {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 2][j - 2] + 1);
                }
            }
        }
        return dp[n][m];
    }

    private record ParsedTypeIndicator(TypeIndicator type, SourceSpan span) {}
}
