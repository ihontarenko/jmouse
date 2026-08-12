package org.jmouse.access.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Every one of these, or null where there is nothing to narrow with.
     *
     * <p>One route can be narrowed twice: a role assigned {@code when caller.isServiceAccount}
     * carrying a bundle entry written {@code when resource.status == 'DRAFT'}. Both were written down
     * and both apply, so they compose — <strong>a narrowing composed of narrowings is still a
     * narrowing</strong>, which is what keeps this inside the one rule the axis is allowed to follow.
     *
     * <p>⚠️ <strong>The composed {@link #source()} joins the halves verbatim.</strong> Each half is
     * still a line somebody can search for in the file, which is the only property {@code source()}
     * has to keep. Re-rendering a combined syntax tree would give <em>a</em> spelling of the rule and
     * send whoever read a refusal looking for a line that is not there.
     *
     * @param conditions the conditions to compose; nulls are ignored, which is what makes this safe
     *                   to call with an unconditional rule on either side
     * @return null where every argument was null, the single condition where exactly one was not, and
     *         a composition otherwise
     */
    static GrantCondition all(GrantCondition... conditions) {
        List<GrantCondition> present = new ArrayList<>();

        for (GrantCondition condition : conditions) {
            if (condition != null) {
                present.add(condition);
            }
        }

        if (present.size() < 2) {
            return present.isEmpty() ? null : present.getFirst();
        }

        return new All(List.copyOf(present));
    }

    /**
     * Several conditions that must all hold.
     *
     * <p>⚠️ Short-circuits, and the order is the order they were composed in — assignment before
     * bundle entry. A condition that reads a lazily attached value therefore does not pay for it when
     * an earlier half has already answered false.
     */
    record All(List<GrantCondition> conditions) implements GrantCondition {

        @Override
        public boolean holds(ConditionContext context) {
            for (GrantCondition condition : conditions) {
                if (!condition.holds(context)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String source() {
            return conditions.stream().map(GrantCondition::source).collect(Collectors.joining(" and "));
        }
    }
}
