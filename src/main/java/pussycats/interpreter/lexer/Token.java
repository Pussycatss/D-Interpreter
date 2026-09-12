package pussycats.interpreter.lexer;

/**
 * Lexical token produced by {@link Lexer}.
 *
 * <p>{@code literal} holds the value of the token, one of:
 * <ul>
 *   <li>{@link TokenType#INTEGER} -> {@link java.math.BigInteger}</li>
 *   <li>{@link TokenType#REAL}    -> {@link Double}</li>
 *   <li>{@link TokenType#STRING}  -> {@link String} (unescaped, without quotes)</li>
 *   <li>everything else -> {@code null}</li>
 * </ul>
 */
public record Token(TokenType type, String lexeme, Object literal, int line, int column) {

    @Override
    public String toString() {
        return String.format("%s('%s') [%d:%d]", type, lexeme, line, column);
    }
}
