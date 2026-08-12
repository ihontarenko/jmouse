package org.jmouse.access;

import org.jmouse.access.spi.GrantAttribution;
import org.jmouse.access.spi.GrantCondition;
import org.jmouse.access.spi.GrantOrigin;

/**
 * One route by which a subject came to hold — or to lose — a permission.
 *
 * <p>This is the record that makes {@code /admin/access}'s <em>Who</em> view possible, and the reason
 * resolution answers with provenance rather than with a set of strings. A screen that could only say
 * "held" or "not held" would leave the question the whole cluster exists to answer — <em>why can this
 * person do this</em> — exactly where it was.
 *
 * <p>The vocabulary is {@code GrantSource}'s one level over (ADR-0013): a permission held because of
 * a workspace role and a permission held because of {@code ROLE_MANAGER} resolve identically and are
 * told apart by where they came from, not by which check reads them.
 *
 * @param kind        how it arrived
 * @param roleName    the role that bundled it, where {@link Kind#ROLE_ASSIGNMENT}; null otherwise
 * @param scope       where the assignment or override applies
 * @param attribution who did this and when, where the rule is written down, and what narrows it.
 *                    ⚠️ <strong>Where the rule lives is a different question from how the permission
 *                    arrived</strong>, and only the first answers <em>"may this screen change it"</em>
 *                    — a role assignment can be either, and so can a denial
 */
public record PermissionSource(
        Kind             kind,
        String           roleName,
        ScopeReference   scope,
        GrantAttribution attribution
) {

    public PermissionSource {
        attribution = attribution == null ? GrantAttribution.none() : attribution;
    }

    /**
     * Whether something narrows this route beyond the scope it applies at.
     *
     * <p>Carried through resolution and read afterwards. A conditionally granted permission is
     * <em>held</em> as far as the effective set is concerned — the set has to stay row-independent
     * to be resolvable once and cacheable — and the condition is what an axis running after the
     * permission axis may subtract with.
     */
    public boolean isConditional() {
        return attribution.isConditional();
    }

    /** What narrows this route, or null. */
    public GrantCondition condition() {
        return attribution.condition();
    }

    /** Where the rule is written down. */
    public GrantOrigin origin() {
        return attribution.origin();
    }

    public enum Kind {
        /** A role the subject holds at a scope covering the target. */
        ROLE_ASSIGNMENT,

        /** A personal allow, layered on top of the roles. */
        DIRECT_ALLOW,

        /** A personal deny. It runs last and beats everything above, at every level. */
        DIRECT_DENY,

        /**
         * The subject is a service sub-account and its master does not hold this here.
         *
         * <p>Not a deny anybody wrote — it is the ceiling of ADR-0006 showing up as a reason, so that
         * an agent that has quietly stopped being able to do something reads as capped rather than as
         * misconfigured.
         */
        AGENT_CAP,

        /**
         * A share link, which is an anonymous subject with exactly one permission over exactly one row.
         *
         * <p>Recorded as a source of its own rather than folded into an allow, because the question
         * the control room is asked about a share is a different one: not "who gave this to whom"
         * but "what is reachable without an account, and over which row".
         */
        SHARE_LINK
    }

    /**
     * A role assignment, as one entry of its bundle lands.
     *
     * @param narrowing what narrows <em>this entry</em> beyond the assignment's own condition, or
     *                  null. Composed with the assignment's, because a role handed out conditionally
     *                  may also bundle a permission conditionally and both were written down
     */
    public static PermissionSource role(
            String           roleName,
            ScopeReference   scope,
            GrantAttribution attribution,
            GrantCondition   narrowing) {

        return new PermissionSource(
                Kind.ROLE_ASSIGNMENT, roleName, scope, attribution.narrowedBy(narrowing));
    }

    /** A personal allow or deny. */
    public static PermissionSource override(
            boolean allow, ScopeReference scope, GrantAttribution attribution) {

        return new PermissionSource(
                allow ? Kind.DIRECT_ALLOW : Kind.DIRECT_DENY, null, scope, attribution);
    }

    /**
     * What a link grants its holder: one permission, over the row the token names.
     *
     * @param where the narrowest place the shared row lives in, or the widest scope where it lives in
     *              none. Passed in rather than read off the target, because "narrowest" is a question
     *              about a vocabulary and this type does not have one
     */
    public static PermissionSource share(ScopeReference where) {
        return new PermissionSource(Kind.SHARE_LINK, null, where, GrantAttribution.derived(
                "Whoever holds the link, and only over the resource it names."));
    }

    public static PermissionSource agentCap(ScopeReference scope) {
        return new PermissionSource(Kind.AGENT_CAP, null, scope, GrantAttribution.derived(
                "The account that owns this agent does not hold it here."));
    }

    /** Whether this source takes the permission away rather than giving it. */
    public boolean removes() {
        return kind == Kind.DIRECT_DENY || kind == Kind.AGENT_CAP;
    }

    /**
     * One line, the way the control room prints it.
     *
     * <p>A declared rule says where it is written. "Why can Nick do this" should end in a file
     * somebody can open, and a route that names only the role sends them to a table that does not
     * hold it.
     */
    public String describe() {
        String route = switch (kind) {
            case ROLE_ASSIGNMENT -> roleName + " → " + scope.describe();
            case DIRECT_ALLOW    -> "personal ALLOW " + scope.describe();
            case DIRECT_DENY     -> "personal DENY " + scope.describe();
            case AGENT_CAP       -> "capped by master " + scope.describe();
            case SHARE_LINK      -> "share link " + scope.describe();
        };

        return origin().isDeclared() ? route + " (declared in " + origin().describe() + ")" : route;
    }
}
