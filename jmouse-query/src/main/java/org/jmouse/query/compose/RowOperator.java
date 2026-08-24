package org.jmouse.query.compose;

import org.jmouse.el.node.Expression;

import java.util.Optional;

/**
 * One comparison a builder row can express, in <strong>both directions</strong>.
 *
 * <h2>⚠️ Writing and reading are one object, and that is the whole point</h2>
 *
 * <p>An operator that knows how to compose itself but not how to recognise itself needs a second,
 * separately maintained reader — and the two drift. They drifted in exactly this product: the reader
 * accepted anything on the right of a comparison, the writer quoted whatever it was handed, and a
 * supplied value came back out as a string literal. Nothing refused; the answer was simply wrong.</p>
 *
 * <p>So {@link #write} and {@link #read} sit on the same constant, and a new operator cannot be added
 * with only half of itself.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface RowOperator {

    /**
     * What a row calls it, and what an interface sends.
     *
     * <p>⚠️ Not {@code name()}: the implementation is an enum and {@link Enum#name()} is final. It would
     * also be the wrong word — the constant is {@code NOT_CONTAINS} and the spelling is
     * {@code notContains}.</p>
     */
    String spelling();

    /** Whether it compares against something. {@code is null} does not. */
    boolean needsValue();

    /**
     * Whether it is an <em>ordered</em> comparison.
     *
     * <p>⚠️ The only place a missing converter answers <em>wrongly</em> rather than refusing:
     * {@code "900" > "1000"} is true of words and false of numbers.</p>
     */
    boolean ordered();

    /**
     * Whether asking about <em>absence</em> makes sense beside it.
     *
     * <p>Only a negative comparison leaves rows out for having nothing to compare, so only a negative one
     * is offered the question.</p>
     */
    boolean negative();

    /**
     * Composes the comparison.
     *
     * @param left  the attribute, already carrying its converter if it needs one
     * @param value the value as a literal, or {@code null} where {@link #needsValue()} is false
     */
    Expression write(Expression left, Expression value);

    /**
     * Recognises the comparison, or answers empty because this is not it.
     *
     * <p>⚠️ Empty is the <strong>right</strong> answer for anything a builder could not have written —
     * an expression on the right, a supplied value, a call. The caller then says so and hands over the
     * text, which is honest. Guessing produces a row that rewrites the person's query the moment they
     * touch it.</p>
     */
    Optional<Reading> read(Expression node);

    /** What a recognised comparison was made of. */
    record Reading(Expression left, Expression value) {
    }
}
