package pussycats.interpreter.ast;

/**
 * The set of type indicators usable on the right-hand side of {@code is}.
 * <p>
 * Syntax (from the language spec):
 * <pre>
 *   TypeIndicator : int | real | bool | string | none
 *                 | [ ]
 *                 | { }
 *                 | func
 * </pre>
 * Note that {@code []} and {@code {}} are two tokens each; the parser assembles
 * them into {@link #VECTOR} and {@link #TUPLE}.
 */
public enum TypeIndicator {
    INT,
    REAL,
    BOOL,
    STRING,
    NONE,
    VECTOR,   // []
    TUPLE,    // {}
    FUNC
}