package org.jmouse.access;

import org.jmouse.access.spi.CapabilityGrant;
import org.jmouse.access.spi.EntitlementStore;
import org.jmouse.access.spi.ScopeHierarchy;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where a capability stands, resolved the same way a permission is.
 *
 * <h2>One sentence, for both axes</h2>
 *
 * <p><strong>Deny wins and the subtraction runs last, at every level.</strong> Not "most specific
 * wins" — deliberately, and identically to {@code EffectivePermissionsResolver}, so an installation
 * has one rule to learn rather than two that differ in a corner nobody reads about until it bites. A
 * denial written at an account takes a capability away from every workspace inside it, and no grant
 * at a narrower place puts it back.
 *
 * <h2>⚠️ Expiry is reading a date</h2>
 *
 * <p>Grants arrive from the store <em>unfiltered</em>, including the ones whose window has closed —
 * see {@link EntitlementStore}. That is what lets this report {@link CapabilityStanding.Outcome#EXPIRED}
 * with the lapsed grant in hand, instead of the absence an expired row would otherwise become. A
 * sweeper marking rows instead would, the day it failed to run, hand somebody an unlimited month, and
 * would destroy the only record of what they used to have.
 *
 * <h2>⚠️ Generosity, only among survivors</h2>
 *
 * <p>Where several allows survive the denial pass, the <em>most generous</em> allowance wins and
 * {@code unlimited} beats every number. Two allows are two promises, and honouring the smaller one
 * silently breaks the larger. This never competes with a refusal: taking a capability away is a deny,
 * which removes it outright and never has to argue with a number.
 *
 * <h2>What this class does not do</h2>
 *
 * <p>It does not count. An allowance is a <em>ceiling</em>; how much of it has been used is a counter
 * written on every metered action, and it belongs to whoever is doing the metering. An authorization
 * library owning that counter would own a write on the hot path of every product using it.
 *
 * <p>And it knows nothing about what is being counted. {@code capability} is a string a product's
 * catalogue defines — no {@code seat}, no {@code workspace}, no {@code board}.
 */
public final class CapabilityResolver {

    private final EntitlementStore store;
    private final ScopeHierarchy   hierarchy;
    private final Clock            clock;

    public CapabilityResolver(EntitlementStore store, ScopeHierarchy hierarchy, Clock clock) {
        this.store     = store;
        this.hierarchy = hierarchy;
        this.clock     = clock;
    }

    /**
     * How one capability stands at a place.
     *
     * @param capability what is being asked about
     * @param at         where — the covering chain is built from this
     */
    public CapabilityStanding standingOf(String capability, ScopeReference at) {
        return standingOfAll(List.of(capability), at).get(capability);
    }

    /**
     * How several capabilities stand, in one round trip.
     *
     * <p>What a menu render asks. Asking {@link #standingOf} in a loop is the N+1 this exists to
     * avoid: a workspace's navigation asks about every module it might show, on every load.
     */
    public Map<String, CapabilityStanding> standingOfAll(List<String> capabilities, ScopeReference at) {
        List<ScopeReference>            chain    = coveringChain(at);
        List<CapabilityGrant>           relevant = store.covering(chain);
        Instant                         now      = clock.instant();
        Map<String, CapabilityStanding> standing = new LinkedHashMap<>();

        for (String capability : capabilities) {
            standing.put(capability, resolveOne(capability, relevant, now));
        }

        return standing;
    }

    private CapabilityStanding resolveOne(String capability, List<CapabilityGrant> grants, Instant now) {
        List<CapabilityGrant> about = new ArrayList<>();

        for (CapabilityGrant grant : grants) {
            if (grant.capability().equals(capability)) {
                about.add(grant);
            }
        }

        if (about.isEmpty()) {
            return CapabilityStanding.ungranted(capability);
        }

        /*
         * ⚠️ The subtraction runs FIRST here and still runs "last" in the sentence — a denial that
         * applies now beats every allow regardless of where either sits, so finding one ends the
         * question. Scanning for it before considering any allowance is the same rule expressed as
         * the cheaper loop, not a different one.
         */
        for (CapabilityGrant grant : about) {
            if (!grant.allowed() && grant.appliesAt(now)) {
                return CapabilityStanding.withheld(capability, grant);
            }
        }

        CapabilityGrant governing = null;

        for (CapabilityGrant grant : about) {
            if (!grant.allowed() || !grant.appliesAt(now)) {
                continue;
            }

            if (governing == null || decidesOver(grant, governing)) {
                governing = grant;
            }
        }

        if (governing != null) {
            return CapabilityStanding.granted(capability, governing.allowance(), governing);
        }

        return lapsed(capability, about, now);
    }

    /**
     * Which of two surviving allows speaks — <strong>one grant, supplying both the number and the
     * place.</strong>
     *
     * <p>⚠️ The two cannot be chosen separately, and doing so is a quiet way to let more through than
     * any grant allowed. Take a plan granting 25 at an account and a top-up granting 50 at one place
     * inside it: the most generous ceiling is 50 and the widest place is the account, and pairing those
     * counts <em>every</em> place in the account against a limit bought for one. So the more generous
     * grant is taken whole.
     *
     * <p>Width breaks the tie, which is what makes the common case read well: where nothing is metered
     * and every allowance is absent, the wider grant is the one a reader recognises — "included in your
     * plan" explains a capability better than the top-up sitting beside it.
     */
    private static boolean decidesOver(CapabilityGrant candidate, CapabilityGrant held) {
        Allowance offered = candidate.allowance();
        Allowance standing = held.allowance();

        if (offered != null && offered.isMoreGenerousThan(standing)) {
            return true;
        }
        if (standing != null && standing.isMoreGenerousThan(offered)) {
            return false;
        }

        return candidate.at().type().rank() < held.at().type().rank();
    }

    /**
     * Everything was granted and nothing applies now — so say <em>which</em> way.
     *
     * <p>An ended trial and one that starts on Monday are opposite situations wearing the same
     * absence, and the reader can act on only one of them.
     */
    private CapabilityStanding lapsed(String capability, List<CapabilityGrant> about, Instant now) {
        CapabilityGrant pending = null;

        for (CapabilityGrant grant : about) {
            if (!grant.allowed()) {
                continue;
            }
            if (grant.validity().hasExpiredBy(now)) {
                return CapabilityStanding.expired(capability, grant);
            }
            if (grant.validity().startsAfter(now)) {
                pending = grant;
            }
        }

        return pending == null
                ? CapabilityStanding.ungranted(capability)
                : CapabilityStanding.notYet(capability, pending);
    }


    /**
     * The place, and every wider place containing it.
     *
     * <p>⚠️ The containment comes from {@link ScopeHierarchy} rather than from anything here: the
     * engine names no place, and a product with departments inside divisions registers its own chain
     * without a line of this changing.
     */
    private List<ScopeReference> coveringChain(ScopeReference at) {
        List<ScopeReference> chain = new ArrayList<>(hierarchy.containing(at));

        if (!chain.contains(at)) {
            chain.add(at);
        }

        return chain;
    }

}
