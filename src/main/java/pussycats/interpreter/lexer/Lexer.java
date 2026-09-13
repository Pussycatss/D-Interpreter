package pussycats.interpreter.lexer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;

    private int tokenLine;
    private int tokenColumn;

    public Lexer(String source) {
        this.source = source;
    }

    public static List<Token> tokenize(String source) {
        return new Lexer(source).tokenize();
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            tokenLine = line;
            tokenColumn = column;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, column));
        return tokens;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            case '(' -> addToken(TokenType.LPAREN);
            case ')' -> addToken(TokenType.RPAREN);
            case '[' -> addToken(TokenType.LBRACKET);
            case ']' -> addToken(TokenType.RBRACKET);
            case '{' -> addToken(TokenType.LBRACE);
            case '}' -> addToken(TokenType.RBRACE);
            case ',' -> addToken(TokenType.COMMA);
            case ';' -> addToken(TokenType.SEMICOLON);

            case '+' -> addToken(TokenType.PLUS);
            case '-' -> addToken(TokenType.MINUS);
            case '*' -> addToken(TokenType.STAR);

            case ':' -> {
                if (match('=')) addToken(TokenType.ASSIGN);
                else throw error("Unexpected  ':' (expected ':=')" );
            }

            case '<' -> addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '>' -> addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '=' -> {
                if (match('>')) addToken(TokenType.ARROW);
                else            addToken(TokenType.EQUAL);
            }

            case '.' -> addToken(match('.') ? TokenType.RANGE : TokenType.DOT);

            case '/' -> {
                if (match('/')) {
                    while (!isAtEnd() && peek() != '\n' && peek() != 'r') advance();
                } else if (match('=')) {
                    addToken(TokenType.NOT_EQUAL);
                } else {
                    addToken(TokenType.SLASH);
                }
            }

            case '"', '\'' -> string(c);

            case '\n' -> addToken(TokenType.NEWLINE);

            case '\r' -> {
                if (peek() == '\n') {
                    advance();
                } else {
                    line++;
                    column = 1;
                }
                addToken(TokenType.NEWLINE);
            }

            case ' ', '\t', '\f' -> { }

            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw error("Unexpected character '" + c + "'");
                }
            }

        }
    }

    private void string(char quote) {
        StringBuilder sb = new StringBuilder();

        while (!isAtEnd()) {
            char c = peek();

            if (c == quote) {
                advance();
                addToken(TokenType.STRING, sb.toString());
                return;
            }
            if (c == '\n' || c == '\r') {
                throw error("Unterminated string literal");
            }
            if (c == '\\') {
                advance();
                if (isAtEnd()) throw error("Unterminated escape sequence");
                char esc = advance();
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '0' -> sb.append('\0');
                    case '\\' -> sb.append('\\');
                    case '"' -> sb.append('"');
                    case '\'' -> sb.append('\'');
                    default -> throw error("Unknown escape sequence: \\" + esc);
                }
            } else {
                sb.append(advance());
            }
        }
        throw error("Unterminated string literal");
    }

    private void number() {
        while (isDigit(peek())) advance();

        boolean isReal = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isReal = true;
            advance();
            while (isDigit(peek())) advance();
        }

        String text = source.substring(start, current);
        if (isReal) {
            addToken(TokenType.REAL, Double.parseDouble(text));
        } else {
            addToken(TokenType.INTEGER, new BigInteger(text));
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = Keywords.lookup(text);

        if (type == null) {
            addToken(TokenType.IDENTIFIER);
        } else if (type == TokenType.TRUE) {
            addToken(TokenType.TRUE, Boolean.TRUE);
        } else if (type == TokenType.FALSE) {
            addToken(TokenType.FALSE, Boolean.FALSE);
        } else {
            addToken(type);
        }
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        char c = source.charAt(current++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, tokenLine, tokenColumn));
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++;
        if (expected == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return true;
    }

    private LexerException error(String message) {
        return new LexerException(message, tokenLine, tokenColumn);
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private char peekNext() {
        return current + 1 >= source.length() ? '\0' : source.charAt(current + 1);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
