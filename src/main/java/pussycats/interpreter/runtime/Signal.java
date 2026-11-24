package pussycats.interpreter.runtime;

import pussycats.interpreter.SourceLocation;

/**
 * Control-flow signals. These are NOT errors — they represent normal control
 * transfers (return from a function, exit from a loop) that must propagate
 * up through intermediate constructs (blocks, if-statements) until they
 * reach a construct that handles them.
 *
 * <p>Convention for {@code interpret(...)} return values:
 * <ul>
 *   <li>{@code Cell} — a normal value</li>
 *   <li>{@code null} — an error was already reported via {@code ErrorReporter}</li>
 *   <li>{@code Signal} — a control transfer is in progress</li>
 * </ul>
 *
 * <p>Intermediate constructs (blocks, if-statements) propagate {@code Signal}s
 * unchanged; only the matching construct consumes them:
 * <ul>
 *   <li>{@link ReturnSignal} — consumed by {@code interpretCall}</li>
 *   <li>{@link ExitSignal}   — consumed by the loop interpreters</li>
 * </ul>
 */
public sealed interface Signal {
    /**
     * Return from a function. {@code valueCell} carries the returned value;
     * for a bare {@code return}, it is an {@code ImmutableCell(null, loc)}
     * (i.e., None).
     */
    record ReturnSignal(Cell valueCell, SourceLocation loc) implements Signal {}

    /**
     * Exit from the innermost enclosing loop. Per the language spec, {@code exit}
     * can only appear inside a loop body; the loop interpreter catches this signal
     * and stops iterating.
     */
    record ExitSignal() implements Signal {}
}