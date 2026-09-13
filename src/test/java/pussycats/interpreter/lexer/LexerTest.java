package pussycats.interpreter.lexer;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LexerTest {

    /** Token types of {@code src}, without the trailing EOF. */
    private static List<TokenType> types(String src) {
        List<Token> tokens = Lexer.tokenize(src);
        return tokens.subList(0, tokens.size() - 1)
                .stream()
                .map(Token::type)
                .toList();
    }

    @Test void emptySourceProducesOnlyEof() {
        List<Token> tokens = Lexer.tokenize("");
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.getFirst().type());
    }

    @Test void skipsWhitespace() {
        assertEquals(List.of(TokenType.VAR, TokenType.IDENTIFIER),
                types("   var    x   "));
    }

    @Test void keywords() {
        assertEquals(
                List.of(TokenType.VAR, TokenType.IF, TokenType.THEN, TokenType.ELSE,
                        TokenType.END, TokenType.WHILE, TokenType.FOR, TokenType.IN,
                        TokenType.LOOP, TokenType.EXIT, TokenType.RETURN,
                        TokenType.PRINT, TokenType.FUNC),
                types("var if then else end while for in loop exit return print func"));
    }

    @Test void booleanAndNoneLiterals() {
        List<Token> ts = Lexer.tokenize("true false none");
        assertEquals(TokenType.TRUE,  ts.get(0).type());
        assertEquals(Boolean.TRUE,    ts.get(0).literal());
        assertEquals(TokenType.FALSE, ts.get(1).type());
        assertEquals(Boolean.FALSE,   ts.get(1).literal());
        assertEquals(TokenType.NONE,  ts.get(2).type());
        assertNull(ts.get(2).literal());
    }

    @Test void typeIndicators() {
        assertEquals(List.of(TokenType.INT, TokenType.REAL_TYPE,
                        TokenType.BOOL, TokenType.STRING_TYPE),
                types("int real bool string"));
    }

    @Test void logicalOperators() {
        assertEquals(List.of(TokenType.AND, TokenType.OR, TokenType.XOR,
                        TokenType.NOT, TokenType.IS),
                types("and or xor not is"));
    }

    @Test void integerLiteral() {
        List<Token> ts = Lexer.tokenize("42");
        assertEquals(TokenType.INTEGER, ts.getFirst().type());
        assertEquals(new BigInteger("42"), ts.getFirst().literal());
    }

    @Test void realLiteral() {
        List<Token> ts = Lexer.tokenize("3.14");
        assertEquals(TokenType.REAL, ts.getFirst().type());
        assertEquals(3.14, (Double) ts.getFirst().literal(), 1e-9);
    }

    @Test void rangeIsTwoIntegersNotAReal() {
        assertEquals(List.of(TokenType.INTEGER, TokenType.RANGE, TokenType.INTEGER),
                types("1..3"));
    }

    @Test void dotAfterDigitsFollowedByDigitIsReal() {
        assertEquals(List.of(TokenType.REAL), types("1.5"));
    }

    @Test void dotAfterDigitsNotFollowedByDigitIsDot() {
        assertEquals(List.of(TokenType.IDENTIFIER, TokenType.DOT, TokenType.INTEGER),
                types("t.2"));
    }

    @Test void twoCharacterOperators() {
        assertEquals(List.of(TokenType.ASSIGN),     types(":="));
        assertEquals(List.of(TokenType.ARROW),      types("=>"));
        assertEquals(List.of(TokenType.LESS_EQUAL), types("<="));
        assertEquals(List.of(TokenType.GREATER_EQUAL), types(">="));
        assertEquals(List.of(TokenType.NOT_EQUAL),  types("/="));
        assertEquals(List.of(TokenType.RANGE),      types(".."));
    }

    @Test void equalityIsNotAssignment() {
        assertEquals(List.of(TokenType.EQUAL),  types("="));
        assertEquals(List.of(TokenType.ASSIGN), types(":="));
    }

    @Test void slashDisambiguation() {
        assertEquals(List.of(TokenType.SLASH),     types("/"));
        assertEquals(List.of(TokenType.NOT_EQUAL), types("/="));
        assertEquals(List.of(),                    types("// this is a comment"));
    }

    @Test void newlineIsAToken() {
        assertEquals(List.of(TokenType.INTEGER, TokenType.NEWLINE, TokenType.INTEGER),
                types("1\n2"));
    }

    @Test void crlfIsASingleNewline() {
        assertEquals(List.of(TokenType.INTEGER, TokenType.NEWLINE, TokenType.INTEGER),
                types("1\r\n2"));
    }

    @Test void loneCrIsANewline() {
        assertEquals(List.of(TokenType.INTEGER, TokenType.NEWLINE, TokenType.INTEGER),
                types("1\r2"));
    }

    @Test void blankLinesProduceEmptyNewlines() {
        assertEquals(List.of(TokenType.NEWLINE, TokenType.NEWLINE),
                types("\n\n"));
    }

    @Test void lineCommentIsSkipped() {
        assertEquals(List.of(TokenType.INTEGER),
                types("42 // comment"));
    }

    @Test void commentConsumesToEndOfLine() {
        assertEquals(List.of(TokenType.INTEGER, TokenType.NEWLINE, TokenType.INTEGER),
                types("1 // comment\n2"));
    }

    @Test void slashSlashInsideStringIsNotAComment() {
        assertEquals("a // b",
                Lexer.tokenize("\"a // b\"").get(0).literal());
    }

    @Test void positionsOnSingleLine() {
        List<Token> ts = Lexer.tokenize("var x");
        assertEquals(1, ts.get(0).line());
        assertEquals(1, ts.get(0).column());
        assertEquals(1, ts.get(1).line());
        assertEquals(5, ts.get(1).column());
    }

    @Test void positionsAcrossLines() {
        List<Token> ts = Lexer.tokenize("a\nb");
        assertEquals(1, ts.get(0).line());
        assertEquals(1, ts.get(0).column());

        assertEquals(TokenType.NEWLINE, ts.get(1).type());
        assertEquals(1, ts.get(1).line());

        assertEquals(2, ts.get(2).line());
        assertEquals(1, ts.get(2).column());
    }

    @Test void eofPositionIsAfterLastToken() {
        Token eof = Lexer.tokenize("ab").get(1);
        assertEquals(TokenType.EOF, eof.type());
        assertEquals(1, eof.line());
        assertEquals(3, eof.column());
    }
}