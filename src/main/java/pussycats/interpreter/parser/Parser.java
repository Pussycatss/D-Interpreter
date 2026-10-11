package pussycats.interpreter.parser;

import pussycats.interpreter.lexer.Token;
import pussycats.interpreter.lexer.TokenType;
import pussycats.interpreter.lexer.Keywords;
import java.util.List;

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
    private final int loopDepth = 0;

    public Parser(List<Token> tokens, String source, String filename) {
        this.tokens = tokens;
        this.reporter = new ErrorReporter(source, filename);
    }

    public void parse() {
        parseProgram();
    }

    private void parseProgram() {

    }

    private void parseStatement() {

    }

    private void parseExpression() {

    }

    private void parseRelation() {

    }

    private void parseFactor() {

    }

    private void parseTerm() {

    }

    private void parseUnary() {

    }

    private void parsePrimary() {

    }

    private void requireStatementSeparator(TokenType... terminators) {
        if (isAtEnd() || check(TokenType.NEWLINE) || check(TokenType.SEMICOLON) || checkAny(terminators)) {
            return;
        }
        Token got = peek();
        throw panic(got, "Expected a newline or ';' between statements",
                "did you forget an operator, or a separator between two statements?");
    }

    private boolean isAtStatementBoundary() {
        return isAtEnd()
                || check(TokenType.NEWLINE)
                || check(TokenType.SEMICOLON)
                || check(TokenType.END)
                || check(TokenType.ELSE);
    }

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

    private Token peek() {
        return tokens.get(current);
    }

    private Token peekAt(int offset) {
        int idx = current + offset;
        if (idx >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // EOF
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

    private static String findClosestKeyword(String text) {
        String best = null;
        int bestDistance = 3; // distances >= 3 are treated as "not close enough"
        String lower = text.toLowerCase();
        for (String kw : Keywords.getKeywords()) {
            if (kw.equals(lower)) {
                continue; // exact match isn't a typo
            }
            int d = levenshtein(lower, kw);
            if (d < bestDistance) {
                bestDistance = d;
                best = kw;
            }
        }
        return best;
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
