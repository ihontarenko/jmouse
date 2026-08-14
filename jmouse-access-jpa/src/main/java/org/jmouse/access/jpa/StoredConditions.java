package org.jmouse.access.jpa;

import org.jmouse.access.spi.GrantCondition;

/**
 * Condition source out of a column, as something the engine can ask.
 *
 * <h2>Why the seam is here rather than the compiler</h2>
 *
 * <p>This module depends on {@code jmouse-access} and {@code jakarta.persistence-api} and nothing
 * else, deliberately: adopting the engine's storage must not also mean adopting somebody's expression
 * language. A column holding source text needs <em>a</em> reader, not <em>the</em> reader — so the
 * store asks for this, and whoever wires it decides which dialect the text is in.
 *
 * <h2>⚠️ Reading is not the same as compiling, and this is the read side</h2>
 *
 * <p>A row is compiled when it is <strong>written</strong>: that is where the source can be refused
 * with a line number and a person to show it to. What happens here is a lookup — the same expression
 * appears on every row of a bundle and on every request that resolves it, so an implementation that
 * compiled per call would put a parser in the resolution path. Cache by source text; the text is the
 * whole identity of a condition.
 *
 * <h2>⚠️ Nothing may silently drop a condition</h2>
 *
 * <p>{@link #none()} throws rather than answering null. A store with no reader that meets a
 * conditional row and returns the grant unconditionally has widened it — <em>"a grant reading
 * {@code allow if X} that grants unconditionally is a hole with a comment explaining itself"</em> —
 * and it has done so at the one moment nobody is watching.
 */
@FunctionalInterface
public interface StoredConditions {

    /**
     * @param source the expression exactly as the column holds it, or null
     * @return the condition, or null where the source was null — meaning <em>always</em>
     */
    GrantCondition of(String source);

    /** For an installation whose rows never carry one — and which must fail loudly if one appears. */
    static StoredConditions none() {
        return source -> {
            if (source == null || source.isBlank()) {
                return null;
            }

            throw new IllegalStateException(
                    "A stored grant carries the condition '" + source + "', and no condition reader is "
                    + "wired. Ignoring it would widen the grant to every call, which is the opposite of "
                    + "what the row says. Register a StoredConditions over this installation's policy "
                    + "dialect.");
        };
    }
}
