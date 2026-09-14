package pussycats.interpreter.lexer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Lexical analyzer.
 *
 * <p>Performs lexical analysis (tokenization) on the source code. Detects simple syntax errors
 * (see {@link LexerException}).
 *
 * <p>Usage:
 * <pre>{@code
 *   Lexer lexer = new Lexer(sourceCode);
 *   List<Token> tokens = lexer.scan();
 * }</pre>
 */
public final class Lexer {
    private final String source;
    private final String sourceName;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    private int startTokenColumn = 1;

    public Lexer(String source, String sourceName) {
        this.source = source;
        this.sourceName = sourceName;
    }

    public Lexer(String source) {
        this(source, "<unknown>");
    }

    public List<Token> scan() {
        while (!isAtEOF()) {
            start = current;
            startTokenColumn = column;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, column));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ' ':
            case '\t':
            case '\r':
                break; // whitespace

            case '\n':
                addLineBreak();
                break;

            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '[': addToken(TokenType.LBRACKET); break;
            case ']': addToken(TokenType.RBRACKET); break;
            case '{': addToken(TokenType.LBRACE); break;
            case '}': addToken(TokenType.RBRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case ';': addToken(TokenType.SEMICOLON); break;

            case '+': addToken(TokenType.PLUS); break;
            case '-': addToken(TokenType.MINUS); break;
            case '*': addToken(TokenType.STAR); break;

            case '/':
                if (match('/')) {
                    skipLineComment();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            case '.':
                if (match('.')) {
                    addToken(TokenType.RANGE);
                } else {
                    addToken(TokenType.DOT);
                }
                break;

            case ':':
                if (match('=')) {
                    addToken(TokenType.ASSIGN);
                } else {
                    throw error("Expected '=' after ':'");
                }
                break;

            case '=':
                if (match('>')) {
                    addToken(TokenType.ARROW);
                } else {
                    addToken(TokenType.EQUAL);
                }
                break;

            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;

            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;

            default:
                if (c == '\'' || c == '"') {
                    scanString(c);
                } else if (isDigit(c)) {
                    scanNumber();
                } else if (isAlpha(c)) {
                    scanIdentifierOrKeyword();
                } else {
                    throw error("Unexpected character '" + c + "'");
                }
        }
    }

    private void scanNumber() {
        while (isDigit(peek())) {
            advance();
        }

        boolean isReal = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isReal = true; // is real 🥀🥀😭✌️
            do advance();
            while (isDigit(peek()));
        }

        String text = source.substring(start, current);
        if (isReal) {
            addToken(TokenType.REAL, Double.parseDouble(text));
        } else {
            addToken(TokenType.INTEGER, new BigInteger(text));
        }
    }

    private void scanString(char quote) {
        StringBuilder value = new StringBuilder();
        while (peek() != quote) {
            if (isAtEOF() || peek() == '\n') {
                throw error("Unterminated string literal");
            }
            char c = advance();
            if (c == '\\') {
                value.append(scanEscape());
            } else {
                value.append(c);
            }
        }
        advance(); // consume closing quote!
        addToken(TokenType.STRING, value.toString());
    }

    private char scanEscape() {
        if (isAtEOF()) {
            throw error("Unterminated escape sequence");
        }
        char c = advance();
        return switch (c) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case '\\' -> '\\';
            case '\'' -> '\'';
            case '"' -> '"';
            default -> throw error("Unknown escape sequence '\\" + c + "'");
        };
    }

    private void scanIdentifierOrKeyword() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(start, current);
        TokenType keyword = Keywords.lookup(text);
        addToken(keyword != null ? keyword : TokenType.IDENTIFIER);
    }

    private void skipLineComment() {
        while (peek() != '\n' && !isAtEOF()) {
            advance();
        }
    }

    private void addLineBreak() {
        boolean lastWasNewline = !tokens.isEmpty() && tokens.getLast().type() == TokenType.NEWLINE;
        line++;
        column = 1;
        if (!lastWasNewline) {
            tokens.add(new Token(TokenType.NEWLINE, "\\n", null, line - 1, startTokenColumn));
        }
    }

    private boolean isAtEOF() {
        return current >= source.length();
    }

    private char advance() {
        char c = source.charAt(current);
        current++;
        column++;
        return c;
    }

    private boolean match(char expect) {
        if (isAtEOF() || source.charAt(current) != expect) {
            return false;
        }
        current++;
        column++;
        return true;
    }

    private char peek() {
        if (isAtEOF()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlpha(char c) {
        return c == '_' || Character.isLetter(c);
    }

    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String lexeme = source.substring(start, current);
        tokens.add(new Token(type, lexeme, literal, line, startTokenColumn));
    }

    private LexerException error(String message) {
        return new LexerException(message, source, sourceName, line, startTokenColumn);
    }
}