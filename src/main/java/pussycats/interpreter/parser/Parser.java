package pussycats.interpreter.parser;

import pussycats.interpreter.ast.Expression;
import pussycats.interpreter.ast.Node;
import pussycats.interpreter.ast.Statement;
import pussycats.interpreter.ast.TypeIndicator;
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
    /** Nesting level of loops, used to check that {@code exit} is inside a loop. */
    private int loopDepth = 0;

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
        return new Statement.Block(statements);
    }

    /** Parses statements inside a construct, up to (not including) {@code end} or {@code else}. */
    private List<Statement> parseBody() {
        List<Statement> statements = new ArrayList<>();
        skipSeparators();
        while (!isAtEnd() && !check(TokenType.END) && !check(TokenType.ELSE)) {
            statements.add(parseStatement());
            if (!isAtEnd() && !check(TokenType.END) && !check(TokenType.ELSE)) {
                requireStatementSeparator(TokenType.END, TokenType.ELSE);
                skipSeparators();
            }
        }
        return statements;
    }

    private Statement parseStatement() {
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

    /** {@code var a := 1, b, c := 3}: one declaration, or a {@code VarDeclGroup} if there are several. */
    private Statement parseVarDecl() {
        List<Statement.VarDecl> decls = new ArrayList<>();
        do {
            Token name = consume(TokenType.IDENTIFIER, "Expected a variable name after 'var'");
            Expression init = null;
            if (match(TokenType.ASSIGN)) {
                init = parseExpression();
            } else if (check(TokenType.EQUAL)) {
                throw panic(peek(),
                        "Expected ':=' after variable name",
                        "did you mean ':=' instead of '='?");
            }
            decls.add(new Statement.VarDecl(name.lexeme(), init));
        } while (match(TokenType.COMMA));
        return decls.size() == 1 ? decls.getFirst() : new Statement.VarDeclGroup(decls);
    }

    private Statement parsePrint() {
        List<Expression> values = new ArrayList<>();
        values.add(parseExpression());
        while (match(TokenType.COMMA)) values.add(parseExpression());
        return new Statement.PrintStatement(values);
    }

    /** Parses an expression; if it is followed by {@code :=}, it becomes an assignment. */
    private Statement parseAssignment() {
        Expression expr = parseExpression();
        if (!match(TokenType.ASSIGN)) {
            throw panic(peek(),
                    "Expected ':=' after reference",
                    "every statement must be one of: var, print, if, while, " +
                              "loop, for, exit, return, or an assignment using ':='");
        }
        Expression value = parseExpression();
        return new Statement.Assign(expr, value);
    }

    /** Supports both forms: {@code if c => stmt} and {@code if c then ... [else ...] end}. */
    private Statement parseIf() {
        Token open = previous();
        Expression cond = parseExpression();

        if (match(TokenType.ARROW)) {
            Statement s = parseStatement();
            return new Statement.IfStatement(cond, new Statement.Block(List.of(s)), null);
        }

        consume(TokenType.THEN, "Expected 'then' or '=>' after condition of 'if'");
        Statement then = new Statement.Block(parseBody());
        Statement otherwise = null;
        if (match(TokenType.ELSE)) {
            otherwise = new Statement.Block(parseBody());
        }
        consumeEnd("if", open);
        return new Statement.IfStatement(cond, then, otherwise);
    }

    private Statement parseWhile() {
        Token open = previous();
        Expression cond = parseExpression();
        consume(TokenType.LOOP, "Expected 'loop' after condition of 'while'");

        loopDepth++;
        Statement.Block body;
        try {
            body = parseBodyBlock();
        } finally {
            loopDepth--;
        }
        consumeEnd("while", open);
        return new Statement.WhileStatement(cond, body);
    }

    private Statement parseLoop() {
        Token open = previous();
        loopDepth++;
        Statement.Block body;
        try {
            body = parseBodyBlock();
        } finally {
            loopDepth--;
        }
        consumeEnd("loop", open);
        return new Statement.LoopStatement(body);
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

        if (check(TokenType.LOOP)) {
            advance();
            loopDepth++;
            Statement.Block body;
            try {
                body = parseBodyBlock();
            } finally {
                loopDepth--;
            }
            consumeEnd("for", open);
            return new Statement.LoopStatement(body);
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
            loopDepth++;
            Statement.Block body;
            try {
                body = parseBodyBlock();
            } finally {
                loopDepth--;
            }
            consumeEnd("for", open);
            return new Statement.ForRangeStatement(var, first, to, body);
        }

        consume(TokenType.LOOP, "Expected 'loop' in 'for'");
        loopDepth++;
        Statement.Block body;
        try {
            body = parseBodyBlock();
        } finally {
            loopDepth--;
        }
        consumeEnd("for", open);
        return new Statement.ForInStatement(var, first, body);
    }

    /** {@code exit} is only allowed inside a loop. */
    private Statement parseExit() {
        if (loopDepth == 0) {
            throw error(previous(), "'exit' can only appear inside a loop");
        }
        return new Statement.ExitStatement();
    }

    /** {@code return} may have no value if the statement ends right after it. */
    private Statement parseReturn() {
        Expression value = isAtStatementBoundary() ? null : parseExpression();
        return new Statement.ReturnStatement(value);
    }

    // ============================================================
    // Expressions, from lowest to highest precedence:
    // or/and/xor -> relation -> + - -> * / -> unary -> postfix -> atom
    // ============================================================

    private Expression parseExpression() {
        Expression left = parseRelation();
        while (match(TokenType.OR, TokenType.AND, TokenType.XOR)) {
            TokenType op = previous().type();
            Expression right = parseRelation();
            left = new Expression.Binary(left, op, right);
        }
        return left;
    }

    /** Comparison operators are not chained: {@code a < b < c} is not allowed. */
    private Expression parseRelation() {
        Expression left = parseFactor();
        if (match(TokenType.LESS, TokenType.LESS_EQUAL,
                TokenType.GREATER, TokenType.GREATER_EQUAL,
                TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            TokenType op = previous().type();
            Expression right = parseFactor();
            return new Expression.Binary(left, op, right);
        }
        return left;
    }

    /** Addition and subtraction (left-associative). */
    private Expression parseFactor() {
        Expression left = parseTerm();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            TokenType op = previous().type();
            Expression right = parseTerm();
            left = new Expression.Binary(left, op, right);
        }
        return left;
    }

    /** Multiplication and division (left-associative). */
    private Expression parseTerm() {
        Expression left = parseUnary();
        while (match(TokenType.ASTERISK, TokenType.SLASH)) {
            TokenType op = previous().type();
            Expression right = parseUnary();
            left = new Expression.Binary(left, op, right);
        }
        return left;
    }

    /** Prefix {@code + - not}, or a postfix type check {@code expr is type}. */
    private Expression parseUnary() {
        if (match(TokenType.PLUS, TokenType.MINUS, TokenType.NOT)) {
            TokenType op = previous().type();
            Expression operand = parseUnary();
            return new Expression.Unary(op, operand);
        }
        Expression expr = parsePrimary();
        if (match(TokenType.IS)) {
            return new Expression.IsCheck(expr, parseTypeIndicator());
        }
        return expr;
    }

    /** An atom followed by any number of postfix operations: {@code [i]}, {@code (args)}, {@code .name}, {@code .1}. */
    private Expression parsePrimary() {
        Expression expr = parseAtom();
        while (true) {
            if (match(TokenType.LBRACKET)) {
                Token open = previous();
                Expression index = parseExpression();
                closeDelimiter(TokenType.RBRACKET, open, "]", "index expression");
                expr = new Expression.Index(expr, index);
            } else if (match(TokenType.LPAREN)) {
                Token open = previous();
                List<Expression> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    args.add(parseExpression());
                    while (match(TokenType.COMMA)) {
                        args.add(parseExpression());
                    }
                }
                closeDelimiter(TokenType.RPAREN, open, ")", "argument list");
                expr = new Expression.Call(expr, args);
            } else if (match(TokenType.DOT)) {
                if (match(TokenType.IDENTIFIER)) {
                    expr = new Expression.Field(expr, previous().lexeme());
                } else if (match(TokenType.INTEGER)) {
                    int n = new BigInteger(previous().lexeme()).intValueExact();
                    expr = new Expression.FieldIndex(expr, n);
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
            return new Expression.IntLit(new BigInteger(previous().lexeme()));
        }
        if (match(TokenType.REAL)) {
            return new Expression.RealLit(Double.parseDouble(previous().lexeme()));
        }
        if (match(TokenType.STRING)) {
            return new Expression.StringLit((String) previous().literal());
        }
        if (match(TokenType.TRUE)) {
            return new Expression.BoolLit(true);
        }
        if (match(TokenType.FALSE)) {
            return new Expression.BoolLit(false);
        }
        if (match(TokenType.NONE)) {
            return new Expression.NoneLit();
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expression.Ident(previous().lexeme());
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
            elems.add(parseExpression());
            while (match(TokenType.COMMA)) {
                elems.add(parseExpression());
            }
        }
        closeDelimiter(TokenType.RBRACKET, open, "]", "array literal");
        return new Expression.ArrayLit(elems);
    }

    /** Tuple elements may be named ({@code x := 1}) or unnamed ({@code 1}). */
    private Expression parseTupleLit(Token open) {
        List<Expression.TupleEntry> entries = new ArrayList<>();
        if (!check(TokenType.RBRACE)) {
            do {
                if (check(TokenType.IDENTIFIER) && peekAt(1).type() == TokenType.ASSIGN) {
                    String name = advance().lexeme();
                    advance();
                    entries.add(new Expression.TupleEntry(name, parseExpression()));
                } else if (check(TokenType.IDENTIFIER) && peekAt(1).type() == TokenType.EQUAL) {
                    throw panic(peekAt(1), "Expected ':=' after tuple element name",
                            "did you mean ':=' instead of '='?");
                } else {
                    entries.add(new Expression.TupleEntry(null, parseExpression()));
                }
            } while (match(TokenType.COMMA));
        }
        closeDelimiter(TokenType.RBRACE, open, "}", "tuple literal");
        return new Expression.TupleLit(entries);
    }

    /** {@code func(params) => expr} or {@code func(params) is ... end}; the parameter list is optional. */
    private Expression parseFuncLit() {
        List<String> params = new ArrayList<>();
        Token funcTok = previous();
        if (match(TokenType.LPAREN)) {
            Token open = previous();
            if (!check(TokenType.RPAREN)) {
                params.add(consume(TokenType.IDENTIFIER, "Expected parameter name").lexeme());
                while (match(TokenType.COMMA)) {
                    params.add(consume(TokenType.IDENTIFIER, "Expected parameter name").lexeme());
                }
            }
            closeDelimiter(TokenType.RPAREN, open, ")", "parameter list");
        }
        if (match(TokenType.ARROW)) {
            Expression body = parseExpression();
            return new Expression.FuncLit(params, body, false);
        }
        if (match(TokenType.IS)) {
            Statement.Block body = new Statement.Block(parseBody());
            consumeEnd("func", funcTok);
            return new Expression.FuncLit(params, body, true);
        }
        throw panic(peek(),
                "Expected '=>' or 'is' in function literal",
                "the 'func' at line " + funcTok.line()
                        + " needs a body: either '=> expr' or 'is ... end'");
    }

    /** Parses the type after {@code is}: {@code int, real, bool, string, none, func, [], {}}. */
    private TypeIndicator parseTypeIndicator() {
        if (match(TokenType.INT_TYPE)) {
            return TypeIndicator.INT;
        }
        if (match(TokenType.REAL_TYPE)) {
            return TypeIndicator.REAL;
        }
        if (match(TokenType.BOOL_TYPE)) {
            return TypeIndicator.BOOL;
        }
        if (match(TokenType.STRING_TYPE)) {
            return TypeIndicator.STRING;
        }
        if (match(TokenType.NONE)) {
            return TypeIndicator.NONE;
        }
        if (match(TokenType.FUNC)) {
            return TypeIndicator.FUNC;
        }
        if (match(TokenType.LBRACKET)) {
            consume(TokenType.RBRACKET, "Expected ']' after '[' in type indicator");
            return TypeIndicator.VECTOR;
        }
        if (match(TokenType.LBRACE)) {
            consume(TokenType.RBRACE, "Expected '}' after '{' in type indicator");
            return TypeIndicator.TUPLE;
        }
        throw panic(peek(),
                "Expected a type indicator (int, real, bool, string, none, [], {}, func)",
                suggestType(peek()));
    }

    /** Consumes the closing token, or reports an error that mentions where the delimiter was opened. */
    private void closeDelimiter(TokenType expected, Token openTok, String closer, String what) {
        if (match(expected)) return;
        Token got = peek();
        throw panic(got, "Expected '" + closer + "' to close " + what + " opened at line " + openTok.line(),
                closer.equals(")") ? "check for a missing ')' in the expression"
                        : "check for a missing '" + closer + "' before this point");
    }

    private static final List<String> TYPE_KEYWORDS = List.of("int", "real", "bool", "string", "none", "func");

    private String suggestType(Token got) {
        if (got.type() != TokenType.IDENTIFIER) return null;
        String close = closestIn(got.lexeme(), TYPE_KEYWORDS);
        return close == null ? null : "did you mean '" + close + "'?";
    }

    private Statement.Block parseBodyBlock() {
        return new Statement.Block(parseBody());
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
        throw panic(got, "Expected a newline or ';' between statements",
                "did you forget an operator, or a separator between two statements?");
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
    private void consumeEnd(String constructName, Token openTok) {
        if (match(TokenType.END)) {
            return;
        }
        Token got = peek();
        String message = "Expected 'end' to close the '" + constructName
                + "' opened at line " + openTok.line();
        throw panic(got, message, "check for a missing 'end' or mismatched nesting");
    }

    private void skipToCommaOrClose(TokenType closer) {
        int depth = 0;
        while (!isAtEnd()) {
            TokenType t = peek().type();
            if (depth == 0 && (t == TokenType.COMMA || t == closer)) {
                return;
            }
            if (t == TokenType.LPAREN || t == TokenType.LBRACKET || t == TokenType.LBRACE) {
                depth++;
            } else if (t == TokenType.RPAREN || t == TokenType.RBRACKET || t == TokenType.RBRACE) {
                if (depth == 0) {
                    return; // an unmatched closer
                }
                depth--;
            } else if (t == TokenType.NEWLINE || t == TokenType.SEMICOLON) {
                // while hunting for a list terminator before next statement
                return;
            }
            advance();
        }
    }

    private void skipOptionalNewlines() {
        while (check(TokenType.NEWLINE)) {
            advance();
        }
    }

    // ============================================================
    // Token helpers
    // ============================================================

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
        String full = message + " (found " + describe(at) + ")";
        reporter.report(full, suggestion, at.line(), at.column());
        return new ParsePanic();
    }

    private String describe(Token t) {
        return t.type() == TokenType.EOF ? "end of input" : "'" + t.lexeme() + "'";
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
}
