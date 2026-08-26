package org.jmouse.access.axis;

import org.jmouse.access.AccessDecision;
import org.jmouse.access.AccessTarget;
import org.jmouse.access.AxisKind;
import org.jmouse.access.EffectivePermissions;
import org.jmouse.access.EffectivePermissionsResolver;
import org.jmouse.access.PermissionProvenance;
import org.jmouse.access.PermissionSource;
import org.jmouse.access.RefusalReason;
import org.jmouse.access.RefusalWords;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.Subject;
import org.jmouse.access.spi.AccessContextScope;
import org.jmouse.access.spi.ConditionContext;
import org.jmouse.access.spi.ConditionFunctionFailure;
import org.jmouse.access.spi.GrantCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

/**
 * The axis that reads the conditions the others carried — <strong>the last one, and it may only take
 * away</strong>.
 *
 * <h2>Why a condition has to be an axis and cannot be a grant</h2>
 *
 * <p>Written inside the grant set, {@code entry:write if resource.status == 'DRAFT'} breaks three
 * things at once. {@link EffectivePermissions} stops being a set resolved once and becomes a function
 * of a row. The {@code (subject, scope chain)} memoisation dies, and one answer no longer serves a
 * page of twenty-five. And {@code VisibilityScope} becomes inexpressible, because a listing filter
 * exists precisely <em>because</em> the answer does not depend on the row.
 *
 * <p>Running last, over an already-resolved target, with permission only to narrow, all three
 * survive. The set is still resolved once and still cacheable; the filter is a conservative
 * over-approximation that the per-row decision tightens.
 *
 * <h2>The two rules, and why they are two</h2>
 *
 * <ol>
 *   <li><strong>A conditional deny refuses when it holds.</strong> Pure subtraction, and the easy one.
 *   <li><strong>A conditionally granted permission refuses when <em>every</em> route that granted it
 *       is conditional and none of them holds.</strong> ⚠️ The qualifier is the whole correctness of
 *       this axis: a permission somebody also holds unconditionally must not be taken away by
 *       somebody else's conditional allow. An axis that refused on "some condition failed" would turn
 *       a rule that <em>gives</em> into a rule that <em>takes</em>, which is the one thing narrowing
 *       is supposed to make impossible.
 * </ol>
 *
 * <h2>What a condition may also know: what is being done</h2>
 *
 * <p>Beside the caller, the place and the row, a condition reads the <strong>action</strong> and the
 * <strong>values</strong> whoever made the call published — {@code when action == 'entry.listByPurpose'
 * and purpose != 'HOLDER'}. Both are constant for a call, so a rule about them is evaluated once and
 * decides whether the call happens: a 403, never a shorter list. That is why it is cheap, and it is
 * also exactly its boundary — see {@link org.jmouse.access.spi.AccessContextScope}.
 *
 * <h2>⚠️ What this axis does not reach</h2>
 *
 * <p>A listing. {@code visibilityFor} never comes through here, so a route that filters rows without
 * deciding about any one of them applies no conditions at all. That is a property of the design
 * rather than an oversight — the alternative is a filter that cannot be written as a query — but it
 * means a condition is a guard on <em>touching</em> a row, not a rule about which rows exist.
 */
public class ConditionAxis implements AccessAxisEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionAxis.class);

    private final AxisKind                     axis;
    private final EffectivePermissionsResolver resolver;
    private final RefusalReason                refusal;
    private final BiFunction<AccessTarget, ScopeReference, Object> resources;
    private final AccessContextScope           published;

    /**
     * @param axis      which question this bean answers, and where it runs. Declare it <em>after</em>
     *                  the permission axis: it reads what that one resolved
     * @param resolver  the same resolution the permission axis used — asked again, and answered from
     *                  the request-scoped cache rather than from the database
     * @param refusal   what a refusal is called
     * @param resources how to reach the row a condition talks about, given the target and the scope a
     *                  grant applies at. ⚠️ May answer null, and a condition must tolerate that: most
     *                  routes are aimed at a place rather than at a row
     * @param published what is being done, where the caller said. ⚠️ Read at <em>evaluation</em> time
     *                  rather than held, which is what makes an action visible to a check made from
     *                  code rather than from an annotation — the two paths differ in who opens the
     *                  window and in nothing else
     */
    public ConditionAxis(
            AxisKind                                         axis,
            EffectivePermissionsResolver                     resolver,
            RefusalReason                                    refusal,
            BiFunction<AccessTarget, ScopeReference, Object> resources,
            AccessContextScope                               published) {

        this.axis      = axis;
        this.resolver  = resolver;
        this.refusal   = refusal;
        this.resources = resources;
        this.published = published == null ? AccessContextScope.none() : published;
    }

    /** The wiring for a product whose conditions look at a row but publish no actions. */
    public ConditionAxis(
            AxisKind                                         axis,
            EffectivePermissionsResolver                     resolver,
            RefusalReason                                    refusal,
            BiFunction<AccessTarget, ScopeReference, Object> resources) {

        this(axis, resolver, refusal, resources, AccessContextScope.none());
    }

    /** The common wiring: a product whose conditions never look at a row passes no resolver for one. */
    public ConditionAxis(
            AxisKind axis, EffectivePermissionsResolver resolver, RefusalReason refusal) {

        this(axis, resolver, refusal, (target, scope) -> null, AccessContextScope.none());
    }

    /*
     * ⚠️ There is deliberately NO (axis, resolver, refusal, AccessContextScope) overload.
     *
     * It would be a fourth constructor whose only difference from the one above is the type of its
     * last parameter — so `new ConditionAxis(axis, resolver, refusal, null)` stops compiling for every
     * existing caller, and the fix somebody reaches for is a cast. A product that publishes actions
     * and reads no row writes the five-argument form with `(target, scope) -> null`, which says the
     * same thing and can only be read one way.
     */

    @Override
    public AxisKind axis() {
        return axis;
    }

    @Override
    public AccessDecision evaluate(Subject subject, String permission, AccessTarget target) {
        PermissionProvenance provenance = resolver.resolve(subject, target).provenanceOf(permission);

        if (provenance == null || !provenance.isConditional()) {
            // The overwhelmingly common case, and it costs one map lookup: nothing here is about
            // conditions, and an axis with nothing to say says allowed.
            return AccessDecision.allowed();
        }

        AccessDecision refusedByADenial = refusedByADenial(subject, target, provenance);

        if (refusedByADenial != null) {
            return refusedByADenial;
        }

        return refusedByEveryRouteFailing(subject, target, provenance);
    }

    /** Rule one: any conditional deny that holds refuses, exactly as an unconditional one would. */
    private AccessDecision refusedByADenial(
            Subject subject, AccessTarget target, PermissionProvenance provenance) {

        for (PermissionSource narrowing : provenance.narrowedBy()) {
            // ⚠️ A deny whose function could not answer is APPLIED. That is the fail-closed reading
            // here: a quota nobody can read refuses rather than waves through.
            if (holds(narrowing, subject, target, true)) {
                return refusedBy(narrowing, "denied where");
            }
        }

        return null;
    }

    /**
     * Rule two: every route conditional, and none of them holding.
     *
     * <p>⚠️ One unconditional route is enough to allow. That is what keeps a conditional allow from
     * behaving like a deny for somebody who was never subject to it.
     */
    private AccessDecision refusedByEveryRouteFailing(
            Subject subject, AccessTarget target, PermissionProvenance provenance) {

        if (provenance.grantedBy().isEmpty()) {
            return AccessDecision.allowed();
        }

        PermissionSource last = null;

        for (PermissionSource route : provenance.grantedBy()) {
            // ⚠️ And an allow whose function could not answer is DROPPED — which is the same
            // fail-closed reading from the other side, and why the two callers differ in this one flag.
            if (!route.isConditional() || holds(route, subject, target, false)) {
                return AccessDecision.allowed();
            }

            last = route;
        }

        return refusedBy(last, "allowed only where");
    }

    /**
     * Whether one conditional source's rule holds — and what to answer when its function cannot say.
     *
     * <p>⚠️ <strong>{@code whenUnanswerable} is not a preference, it is the fail-closed answer read from
     * two different sides.</strong> A condition that throws used to be flattened to {@code false}
     * inside the evaluator, and that was safe only while conditions were pure. Once a function may read
     * a counter, {@code false} means <em>refuse</em> on an allow and <em>permit</em> on a deny — so the
     * single boolean the evaluator could return was safe in one position and dangerous in the other.
     *
     * <p>The axis is the first place that knows which position it is in, which is why the failure
     * travels this far instead of being handled where it happened. A caller in the denial loop passes
     * {@code true} (apply the deny); a caller in the granting loop passes {@code false} (drop the
     * allow). Both refuse.
     *
     * <p>A function may opt out with {@link ConditionFunctionFailure#failsOpen()}, and then the old
     * behaviour applies — for a signal that is genuinely advisory.
     */
    private boolean holds(
            PermissionSource source, Subject subject, AccessTarget target, boolean whenUnanswerable) {

        GrantCondition condition = source.condition();

        if (condition == null) {
            return false;
        }

        try {
            return condition.holds(ConditionContext.of(
                    subject,
                    source.scope(),
                    resources.apply(target, source.scope()),
                    published.action(),
                    published.values()));
        } catch (ConditionFunctionFailure unanswerable) {
            LOGGER.warn("The rule `{}` could not be evaluated: {} — reading it as {}.",
                        condition.source(), unanswerable.getMessage(),
                        unanswerable.failsOpen() ? "not holding" : "holding, which refuses");

            return !unanswerable.failsOpen() && whenUnanswerable;
        }
    }

    /**
     * Why, in the rule's own words.
     *
     * <p>The condition is quoted as it was written rather than described. Somebody reading a refusal
     * has to be able to find the line it came from, and a paraphrase is a line nobody can search for.
     *
     * <p>⚠️ <strong>The quote stays, and the sentence is added beside it.</strong> Quoting the condition
     * verbatim is what lets whoever administers the installation <em>find</em> the rule — respelling it
     * would leave them searching for a line that is not in any file. But to somebody who merely pressed
     * a button, {@code now is not workingHours} is an expression, not an explanation. So a policy file
     * may write {@code reason "…"} and the refusal carries both: the quote for the administrator, the
     * sentence for the person.
     *
     * <p>⚠️ The tail is {@link RefusalWords}' rather than this method's, and that is not tidying: this
     * axis is no longer the only one with an explanation to add, and two axes spelling the same tail
     * two ways is how a reader learns to distrust both.
     */
    private AccessDecision refusedBy(PermissionSource source, String phrasing) {
        String written = "This is " + phrasing + " `" + source.condition().source() + "`"
                         + (source.origin().isDeclared()
                                    ? ", declared in " + source.origin().describe()
                                    : "");

        String explanation = source.attribution().explanation();

        return AccessDecision.refused(
                refusal, RefusalWords.explained(written, explanation), explanation);
    }
}
