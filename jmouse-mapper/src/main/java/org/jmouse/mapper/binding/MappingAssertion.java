package org.jmouse.mapper.binding;

import org.jmouse.mapper.MappingContext;

import java.util.function.BiPredicate;

/**
 * A condition that stops a mapping, and what to say when it holds. 🚫
 *
 * <h2>⚠️ This is not a property rule, and that is why it is its own type</h2>
 *
 * <p>Everything else a {@link TypeMappingRule} carries is keyed by a target property: a name, and how
 * to fill it. An assertion names no property. It is about the pair — whether this mapping may happen at
 * all — and folding it into the property map would mean inventing a key nothing ever looks up, which is
 * the shape of a declaration that loads and silently does nothing.</p>
 *
 * <h2>⚠️ It is not bean validation either</h2>
 *
 * <p>Validation asks whether a <em>type</em> satisfies its constraints, wherever that type appears.
 * This asks whether <em>this mapping</em> may proceed: a target can be a perfectly valid object and
 * still be wrong for the source it was built from, and a source can be a valid object that this
 * particular mapping cannot use. Constraints belonging to a type belong to the validation subsystem, in
 * one place; anything reusable across mappings is that subsystem's job and not this one's.</p>
 *
 * @param subject   what the condition is about
 * @param phase     when it runs
 * @param condition evaluated against the subject; when it holds, the mapping is refused
 * @param message   what to report — it says what is wrong with the data, never what the code should
 *                  have done
 */
public record MappingAssertion(
        Subject subject,
        Phase phase,
        BiPredicate<Object, MappingContext> condition,
        String message
) {

    /** What an assertion is about. */
    public enum Subject {

        /** The object being mapped from. */
        SOURCE,

        /** The object being mapped into. */
        TARGET
    }

    /** When an assertion runs. */
    public enum Phase {

        /**
         * Before anything is built or written.
         *
         * <p>⚠️ For a target this only happens when the caller supplied the instance. When the mapper
         * constructs the target there is nothing to inspect but type defaults, and every assertion
         * about it would be an assertion about {@code null} and zero — so the phase does not run at
         * all rather than running against a blank object and passing for the wrong reason.</p>
         */
        BEFORE,

        /** Once the target is fully written. */
        AFTER
    }

    /**
     * Whether this assertion applies at a given point.
     *
     * @param subject what is being checked
     * @param phase   when
     * @return {@code true} when it belongs here
     */
    public boolean applies(Subject subject, Phase phase) {
        return this.subject == subject && this.phase == phase;
    }
}
