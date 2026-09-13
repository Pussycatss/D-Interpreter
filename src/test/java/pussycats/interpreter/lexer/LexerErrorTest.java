package pussycats.interpreter.lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LexerErrorTest {

    @Test void unterminatedString() {
        LexerException e = assertThrows(LexerException.class,
                () -> Lexer.tokenize("\"hello"));
        assertTrue(e.getMessage().contains("Unterminated string"));
    }

    @Test void newlineInsideStringIsAnError() {
        assertThrows(LexerException.class, () -> Lexer.tokenize("\"a\nb\""));
    }

    @Test void unknownCharacter() {
        LexerException e = assertThrows(LexerException.class,
                () -> Lexer.tokenize("a @ b"));
        assertTrue(e.getMessage().contains("@"));
    }

    @Test void colonWithoutEquals() {
        LexerException e = assertThrows(LexerException.class,
                () -> Lexer.tokenize("x : y"));
        assertTrue(e.getMessage().contains(":="));
    }

    @Test void errorReportsLineAndColumn() {
        LexerException e = assertThrows(LexerException.class,
                () -> Lexer.tokenize("abc\n  @"));
        assertEquals(2, e.getLine());
        assertEquals(3, e.getColumn());
    }
}