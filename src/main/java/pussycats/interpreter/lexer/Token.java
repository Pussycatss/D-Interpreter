package pussycats.interpreter.lexer;

/**
 * Lexical token produced by {@link Lexer}.
 * <p>
 * {@code literal} holds the value of the token, one of:
 * <ul>
 *   <li>{@link TokenType#INTEGER} -> {@link java.math.BigInteger}
 *   <li>{@link TokenType#REAL}    -> {@link Double}
 *   <li>{@link TokenType#STRING}  -> {@link String} (unescaped, without quotes)
 *   <li>everything else -> {@code null}
 * </ul>
 */
public record Token(TokenType type, String lexeme, Object literal, int line, int column) {

    @Override
    public String toString() {
        return String.format("%s('%s') [%d:%d]", type, lexeme, line, column);
    }
}
