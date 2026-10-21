package org.jmouse.formula.el;

import java.util.List;
import java.util.Optional;

/**
 * One statement of a formula document as its author wrote it, its parts cut from the source by jMF's grammar: what it
 * defines, the shape of its expression — a tuple, a call, or anything else —, and the ranges written after it. What a
 * caller builds from a line it reads here — a point's coordinates, a curve's components, a construction step's
 * arguments —, never by cutting the text again.
 *
 * <pre>
 * r = 2; (r cos s, r sin s)  s in 0 .. 2 pi
 *   → r = 2                     a definition named r, its expression 2
 *   → (r cos s, r sin s) …      a tuple of two components, ranged over s from 0 to 2 pi
 * </pre>
 *
 * @param text       the whole statement, its ranges included
 * @param name       the name a statement {@code name = …} or {@code name(…) = …} gives its right side
 * @param parameters the parameters of a definition {@code f(u, v) = …}; empty for {@code name = …}
 * @param expression the right side of {@code name = …}, else the statement
 * @param ranges     the ranges written after it, in order
 */
public record WrittenStatement(String text, Optional<String> name, List<String> parameters, Written expression,
                               List<Range> ranges) {

    public WrittenStatement {
        parameters = List.copyOf(parameters);
        ranges = List.copyOf(ranges);
    }

    /** Whether it is {@code name = …} or {@code name(…) = …}: what defines a name for the statements after it. */
    public boolean defines() {
        return name.isPresent();
    }

    /**
     * An expression as written, and its shape.
     *
     * @param text  as the author wrote it, trimmed
     * @param shape what it is at its top level
     */
    public record Written(String text, Shape shape) {
    }

    /** What an expression is at its top level. */
    public sealed interface Shape {
    }

    /** {@code (a, b, …)}: values written together, two or more. */
    public record Tuple(List<Written> components) implements Shape {
        public Tuple {
            components = List.copyOf(components);
        }
    }

    /** {@code name(a, b, …)}: a call, its arguments as written. */
    public record Call(String name, List<Written> arguments) implements Shape {
        public Call {
            arguments = List.copyOf(arguments);
        }
    }

    /** Any other expression: a name, a number, an operation. */
    public record Expression() implements Shape {
    }

    /**
     * {@code s in 0 .. 2 pi}, {@code k in 1 .. N integer}.
     *
     * @param variable the name that runs over the range
     * @param from     the lower bound as written
     * @param to       the upper bound as written
     * @param integer  whether the variable takes whole values only
     * @param start    where it begins in the source as written — at its variable —, so what stands before it can be
     *                 taken as written
     */
    public record Range(String variable, String from, String to, boolean integer, int start) {
    }
}
