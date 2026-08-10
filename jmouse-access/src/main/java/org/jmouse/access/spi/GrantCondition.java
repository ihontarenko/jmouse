package org.jmouse.access.spi;

/**
 * A predicate attached to one grant — opaque on purpose.
 *
 * <p>Nothing looks inside. The engine asks one question, and an opaque callable answers it as well as
 * a syntax tree would, without the engine owning operator precedence or a second grammar.
 *
 * <h2>⚠️ Where a condition is allowed to matter, and where it is not</h2>
 *
 * <p>A predicate <em>inside the grant set</em> would break three things at once: the effective set
 * stops being resolvable once, the {@code (subject, chain)} memoisation dies, and a listing filter
 * becomes inexpressible — a filter exists precisely because the answer does not depend on the row.
 *
 * <p>So a condition is <strong>carried</strong> through resolution and <strong>never evaluated</strong>
 * there. The set stays row-independent and cacheable; the condition is read afterwards, by an axis
 * that runs after the permission axis, sees a resolved target, and may only <em>narrow</em>.
 *
 * <p>Two consequences worth knowing before writing one:
 *
 * <ul>
 *   <li><strong>A listing is filtered by the unconditional answer.</strong> {@code VisibilityScope}
 *       sees a conditionally granted permission as granted, so a listing is a conservative
 *       over-approximation and the per-row check is what narrows it. A route that lists rows without
 *       ever deciding about one will not apply conditions at all.
 *   <li><strong>{@link #source()} is what a person reads.</strong> Rendering a compiled form back to
 *       text gives <em>a</em> spelling rather than <em>the</em> line, and somebody who cannot find it
 *       in the file cannot act on it.
 * </ul>
 */
public interface GrantCondition {

    /**
     * Whether this condition holds.
     *
     * <p>⚠️ Must not throw for ordinary data, including a null resource. A rule that is wrong is a
     * load-time fault; a rule that throws at decision time is an outage wearing an authorization
     * failure's clothes.
     */
    boolean holds(ConditionContext context);

    /** The source exactly as written. */
    String source();
}
