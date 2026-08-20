package org.jmouse.access.projection;

import org.jmouse.access.Allowance;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.jpa.EntitlementAdministration;
import org.jmouse.access.jpa.EntitlementAdministration.GrantView;
import org.jmouse.access.policy.model.PolicyEntitlement;
import org.jmouse.access.policy.model.PolicyEntitlement.Kind;
import org.jmouse.access.policy.model.PolicyScope;
import org.jmouse.access.policy.model.SourceSpan;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What each of these places is entitled to — <strong>as the lines somebody wrote</strong>, not as the
 * rows those lines became.
 *
 * <h2>⚠️ Why this and not {@code PolicyProjector.projectEntitlements}</h2>
 *
 * <p>The engine's projector renders one line per capability grant, because a grant is all it can see.
 * That is honest and unreadable: {@code plan business} is one decision and six rows, so a bare
 * installation rendered as six near-identical {@code allow} lines carrying the same date and the same
 * {@code reason 'Seeded from the policy files'} — six facts where there was one, and no way to tell
 * from the text that they stand or fall together.
 *
 * <p>Every one of those rows records <em>which tier issued it</em> in its source reference. That column
 * exists precisely so the question <em>what is this account on</em> needs no second copy — and reading
 * it here turns six lines back into the one line they came from.
 *
 * <h2>⚠️ Only a bundle collapses. Everything else stays one line per row</h2>
 *
 * <p>A gift, a purchase and a withholding are each a decision somebody made about one capability, with
 * their own reason and their own window. There is nothing to fold them into, and folding them by
 * capability would merge two different people's decisions into one sentence neither wrote.
 *
 * <h2>⚠️ Bounded by the places asked about, never by the store</h2>
 *
 * <p>{@code EntitlementStore} deliberately cannot enumerate — a store that could list every account
 * would be a store the engine could walk — so the caller names the places and gets those.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class EntitlementProjection {

    private EntitlementProjection() {
    }

    /**
     * The entitlement lines in force at these places.
     *
     * <p>⚠️ Identifiers are written as they stand, with no naming hook. Unlike the permission axis, a
     * place here is an account rather than a nesting somebody has to judge the reach of, and the screen
     * that shows this is one click from the account it names.
     *
     * @param entitlements the administration port to read grants from
     * @param places       the places to ask about, in the order the caller wants them considered
     * @return the lines, ordered by place, then kind, then subject
     */
    public static List<PolicyEntitlement> at(
            EntitlementAdministration entitlements, Collection<ScopeReference> places) {

        List<PolicyEntitlement> lines   = new ArrayList<>();
        Set<String>             bundles = new HashSet<>();

        for (ScopeReference place : places) {
            for (GrantView grant : entitlements.grantsAt(place)) {
                Kind bundle = bundleKindOf(grant);

                if (bundle == null) {
                    lines.add(lineOf(place, grant));
                    continue;
                }

                // ⚠️ Once per (place, tier), however many capabilities the tier carries. The six rows
                // `plan business` writes are one line, and a seventh capability added to the tier
                // tomorrow is still that one line.
                if (bundles.add(place + "/" + grant.source() + "/" + grant.sourceReference())) {
                    lines.add(new PolicyEntitlement(
                            scopeOf(place), bundle, grant.sourceReference(),
                            null, false, null, momentOf(validUntil(grant)), null, SourceSpan.none()));
                }
            }
        }

        return lines.stream()
                .sorted(Comparator.comparing((PolicyEntitlement line) -> line.at().toString())
                        .thenComparing(PolicyEntitlement::kind)
                        .thenComparing(PolicyEntitlement::subject))
                .toList();
    }

    /**
     * The tier line this grant is part of, or null where it is a decision of its own.
     *
     * <p>⚠️ A tier-sourced grant with <strong>no</strong> source reference is not collapsible: something
     * wrote it as a plan grant without saying which plan, and inventing a tier name for it would put a
     * word in the document that names nothing.
     *
     * <p>The source is matched against the two bundle kinds by name rather than against a product's
     * enum — a store records provenance as text, and {@code PLAN} and {@code TRIAL} are the only two
     * provenances the grammar has a bundle line for.
     */
    private static Kind bundleKindOf(GrantView grant) {
        if (grant.sourceReference() == null || grant.sourceReference().isBlank()) {
            return null;
        }

        if (Kind.PLAN.name().equals(grant.source())) {
            return Kind.PLAN;
        }

        return Kind.TRIAL.name().equals(grant.source()) ? Kind.TRIAL : null;
    }

    /** One grant nobody's tier issued, exactly as it stands. */
    private static PolicyEntitlement lineOf(ScopeReference place, GrantView grant) {
        Allowance allowance = grant.allowance();

        return new PolicyEntitlement(
                scopeOf(place),
                grant.allowed() ? Kind.ALLOW : Kind.DENY,
                grant.capability(),
                allowance == null || allowance.quantity() == null
                        ? null
                        : String.valueOf(allowance.quantity()),
                allowance != null && allowance.quantity() == null,
                momentOf(validFrom(grant)),
                momentOf(validUntil(grant)),
                grant.reason(),
                SourceSpan.none());
    }

    private static PolicyScope scopeOf(ScopeReference where) {
        return where.type().namesAnInstance()
                ? PolicyScope.of(where.type().name(), where.id())
                : PolicyScope.kind(where.type().name());
    }

    private static Instant validFrom(GrantView grant) {
        return grant.validity() == null ? null : grant.validity().from();
    }

    private static Instant validUntil(GrantView grant) {
        return grant.validity() == null ? null : grant.validity().until();
    }

    /**
     * A moment as the document writes a date, or null.
     *
     * <p>⚠️ <strong>Null renders as nothing, and that is the point.</strong> Every row carries the
     * instant it was written, so printing it as {@code from …} put today's date on every line of a
     * fresh installation — a window nobody set, on a grant that has no window, read as a fact about
     * when access began.
     */
    private static String momentOf(Instant moment) {
        return moment == null ? null : LocalDate.ofInstant(moment, ZoneId.systemDefault()).toString();
    }
}
