package pussycats.interpreter.lexer;

/**
 * Token types.
 *
 * <p>Represents enumeration of all possible token types</p>
 */
public enum TokenType {
    // Literals
    INTEGER,
    REAL,
    STRING,

    // Identifiers
    IDENTIFIER,

    // Keywords
    VAR,
    IF,
    THEN,
    ELSE,
    END,
    WHILE,
    FOR,
    IN,
    LOOP,
    EXIT,
    RETURN,
    PRINT,
    FUNC,

    TRUE,
    FALSE,
    NONE,

    INT,
    REAL_TYPE,
    BOOL,
    STRING_TYPE,

    AND,
    OR,
    XOR,
    NOT,
    IS,

    // Operators
    PLUS,
    MINUS,
    STAR,
    SLASH,

    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL,
    EQUAL,
    NOT_EQUAL,

    ASSIGN,      // :=
    ARROW,       // =>
    DOT,         // .
    RANGE,       // ..

    // Delimiters
    LPAREN,      // (
    RPAREN,      // )
    LBRACKET,    // [
    RBRACKET,    // ]
    LBRACE,      // {
    RBRACE,      // }

    COMMA,       // ,
    SEMICOLON,   // ;

    NEWLINE,
    EOF
}
