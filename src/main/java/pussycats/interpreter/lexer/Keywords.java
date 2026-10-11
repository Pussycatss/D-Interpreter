package pussycats.interpreter.lexer;

import java.util.HashMap;
import java.util.Map;

/**
 * Static map of reserved words.
 * <p>
 * Contains all keywords and their mapping to {@link TokenType}.
 */
final class Keywords {
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        // control-flow / structural keywords
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("then", TokenType.THEN);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("end", TokenType.END);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("loop", TokenType.LOOP);
        KEYWORDS.put("exit", TokenType.EXIT);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("func", TokenType.FUNC);

        // literals
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("none", TokenType.NONE);

        // type indicators
        KEYWORDS.put("int", TokenType.INT);
        KEYWORDS.put("real", TokenType.REAL_TYPE);
        KEYWORDS.put("bool", TokenType.BOOL);
        KEYWORDS.put("string", TokenType.STRING_TYPE);

        // logical operators
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("xor", TokenType.XOR);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("is", TokenType.IS);
    }

    private Keywords() {
    }

    /** Returns the keyword TokenType for {@code text}, or {@code null} if it's not a keyword. */
    static TokenType lookup(String text) {
        return KEYWORDS.get(text);
    }
}
