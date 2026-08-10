package org.jmouse.access.axis;

import org.jmouse.access.AccessDecision;
import org.jmouse.access.AccessTarget;
import org.jmouse.access.AxisKind;
import org.jmouse.access.EffectivePermissions;
import org.jmouse.access.EffectivePermissionsResolver;
import org.jmouse.access.PermissionProvenance;
import org.jmouse.access.PermissionSource;
import org.jmouse.access.RefusalReason;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.Subject;
import org.jmouse.access.spi.ConditionContext;
import org.jmouse.access.spi.GrantCondition;

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
 * <h2>⚠️ What this axis does not reach</h2>
 *
 * <p>A listing. {@code visibilityFor} never comes through here, so a route that filters rows without
 * deciding about any one of them applies no conditions at all. That is a property of the design
 * rather than an oversight — the alternative is a filter that cannot be written as a query — but it
 * means a condition is a guard on <em>touching</em> a row, not a rule about which rows exist.
 */
public class ConditionAxis implements AccessAxisEvaluator {

    private final AxisKind                     axis;
    private final EffectivePermissionsResolver resolver;
    private final RefusalReason                refusal;
    private final BiFunction<AccessTarget, ScopeReference, Object> resources;

    /**
     * @param axis      which question this bean answers, and where it runs. Declare it <em>after</em>
     *                  the permission axis: it reads what that one resolved
     * @param resolver  the same resolution the permission axis used — asked again, and answered from
     *                  the request-scoped cache rather than from the database
     * @param refusal   what a refusal is called
     * @param resources how to reach the row a condition talks about, given the target and the scope a
     *                  grant applies at. ⚠️ May answer null, and a condition must tolerate that: most
     *                  routes are aimed at a place rather than at a row
     */
    public ConditionAxis(
            AxisKind                                         axis,
            EffectivePermissionsResolver                     resolver,
            RefusalReason                                    refusal,
            BiFunction<AccessTarget, ScopeReference, Object> resources) {

        this.axis      = axis;
        this.resolver  = resolver;
        this.refusal   = refusal;
        this.resources = resources;
    }

    /** The common wiring: a product whose conditions never look at a row passes no resolver for one. */
    public ConditionAxis(
            AxisKind axis, EffectivePermissionsResolver resolver, RefusalReason refusal) {

        this(axis, resolver, refusal, (target, scope) -> null);
    }

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
            if (holds(narrowing, subject, target)) {
                return AccessDecision.refused(refusal, because(narrowing, "denied where"));
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
            if (!route.isConditional() || holds(route, subject, target)) {
                return AccessDecision.allowed();
            }

            last = route;
        }

        return AccessDecision.refused(refusal, because(last, "allowed only where"));
    }

    private boolean holds(PermissionSource source, Subject subject, AccessTarget target) {
        GrantCondition condition = source.condition();

        return condition != null && condition.holds(ConditionContext.of(
                subject, source.scope(), resources.apply(target, source.scope())));
    }

    /**
     * Why, in the rule's own words.
     *
     * <p>The condition is quoted as it was written rather than described. Somebody reading a refusal
     * has to be able to find the line it came from, and a paraphrase is a line nobody can search for.
     */
    private static String because(PermissionSource source, String phrasing) {
        return "This is " + phrasing + " `" + source.condition().source() + "`"
               + (source.origin().isDeclared() ? ", declared in " + source.origin().describe() : "") + ".";
    }
}
