package org.jmouse.access.policy;

import org.jmouse.access.Allowance;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.policy.model.PolicyBundleEntry;
import org.jmouse.access.policy.model.PolicyEntitlement;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.policy.model.PolicyEffect;
import org.jmouse.access.policy.model.PolicyGrant;
import org.jmouse.access.policy.model.PolicyRole;
import org.jmouse.access.policy.model.PolicyRoleAssignment;
import org.jmouse.access.policy.model.PolicyScope;
import org.jmouse.access.policy.model.PolicySubject;
import org.jmouse.access.policy.model.SourceSpan;
import org.jmouse.access.spi.BundledPermission;
import org.jmouse.access.spi.CapabilityGrant;
import org.jmouse.access.spi.DirectGrant;
import org.jmouse.access.spi.EntitlementStore;
import org.jmouse.access.spi.GrantStore;
import org.jmouse.access.spi.RoleGrant;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a grant store says, written as a policy — the direction the parser does not go.
 *
 * <pre>
 *      .jmp text  ──parse──►  PolicyDocument  ──bind────►  AccessPolicy
 *                 ◄─write───                  ◄─project──  GrantStore
 * </pre>
 *
 * <p>This is what makes {@link PolicyDocument} the exchange format in <em>both</em> directions rather
 * than a parser output. Rows somebody inserted, a role assigned through a screen, a denial recorded
 * against one account — none of them were ever written down, and all of them can be shown written
 * down. A control room that can render every grant in one notation is a control room where
 * "why can Nick do this" has one answer rather than one per source.
 *
 * <h2>⚠️ A projection is a rendering, not a source file</h2>
 *
 * <p>It carries no {@code include}, no comments and no formatting anybody chose, because none of that
 * was ever in a table to read back. Label it as <em>derived</em> wherever it is shown, or the first
 * person to copy it into a file will wonder where their comments went.
 *
 * <p>It also carries no {@code scopes} or {@code permissions} block. A store holds grants; the
 * vocabulary they are written against belongs to the application, and a projection that invented one
 * would be describing the installation rather than reporting it.
 */
public final class PolicyProjector {

    private PolicyProjector() {
    }

    /**
     * Everything these subjects hold, anywhere, as a policy.
     *
     * <p>⚠️ <strong>The subjects have to be named.</strong> {@link GrantStore} answers questions about
     * a subject and cannot be asked to enumerate them — deliberately, because a store that could list
     * every account would be a store the engine could walk. Whoever opens the control room already
     * knows which accounts they are looking at, and passes them.
     *
     * @param documentName what the derived document is called, and what the control room shows above it
     * @param store        where the grants come from — a database, a policy, or a composite of both
     * @param subjectIds   the accounts to report on, in the order they should be written
     */
    public static PolicyDocument project(String documentName, GrantStore store, List<String> subjectIds) {
        return project(documentName, subjectIds,
                store::rolesHeldBy,
                store::directHeldBy);
    }

    /**
     * The same, narrowed to one chain of scopes — what a screen looking at a single workspace shows.
     *
     * @param chain the scopes that could matter, widest first, as {@code ScopeCatalog.covering} builds it
     */
    public static PolicyDocument project(
            String documentName, GrantStore store, List<String> subjectIds, List<ScopeReference> chain) {

        return project(documentName, subjectIds,
                subjectId -> store.rolesCovering(subjectId, chain),
                subjectId -> store.directCovering(subjectId, chain));
    }

    private static PolicyDocument project(
            String                      documentName,
            List<String>                subjectIds,
            SubjectQuestion<RoleGrant>   rolesOf,
            SubjectQuestion<DirectGrant> grantsOf) {

        Map<String, Set<PolicyBundleEntry>> bundles  = new LinkedHashMap<>();
        List<PolicySubject>                 subjects = new ArrayList<>();

        for (String subjectId : subjectIds) {
            List<RoleGrant> held = rolesOf.about(subjectId);

            for (RoleGrant role : held) {
                collectBundle(bundles, role);
            }

            subjects.add(new PolicySubject(
                    subjectId,
                    toAssignments(held),
                    grantsOf.about(subjectId).stream().map(PolicyProjector::toGrant).toList(),
                    SourceSpan.none()));
        }

        return PolicyDocument.of(documentName, toRoles(bundles), subjects);
    }

    /**
     * Adds a role's bundle to the roles this projection declares.
     *
     * <p>⚠️ <strong>Two stores can answer with the same role name and different bundles</strong> — a
     * role whose structure moved into a file while some of its rows are still in a table. The union is
     * what the engine actually resolves, because a composite concatenates every store's answer and
     * every bundled permission in all of them lands; writing one of the two would show a reader less
     * than the decision path uses.
     */
    private static void collectBundle(Map<String, Set<PolicyBundleEntry>> bundles, RoleGrant role) {
        Set<PolicyBundleEntry> bundle = bundles.computeIfAbsent(role.roleName(), name -> new LinkedHashSet<>());

        for (BundledPermission permission : role.bundle()) {
            bundle.add(new PolicyBundleEntry(
                    permission.permission(), permission.carriedAt().name(), SourceSpan.none()));
        }
    }

    private static List<PolicyRole> toRoles(Map<String, Set<PolicyBundleEntry>> bundles) {
        return bundles.entrySet().stream()
                .map(bundle -> new PolicyRole(
                        bundle.getKey(), List.copyOf(bundle.getValue()), SourceSpan.none()))
                .toList();
    }

    /**
     * The assignments a subject holds, each written once.
     *
     * <p>⚠️ Deduplicated, for the same reason the bundles are unioned: two stores can both report
     * {@code SPACE_ADMIN} in Kyiv, and the notation has no way to say that twice that means anything.
     * Written twice it reads as a defect in the screen rather than as a fact about the installation —
     * <em>which</em> store said it belongs to provenance, beside the line, not in the line.
     */
    private static List<PolicyRoleAssignment> toAssignments(List<RoleGrant> held) {
        Set<PolicyRoleAssignment> assignments = new LinkedHashSet<>();

        for (RoleGrant role : held) {
            assignments.add(new PolicyRoleAssignment(role.roleName(), toScope(role.at()), SourceSpan.none()));
        }

        return List.copyOf(assignments);
    }

    /**
     * One personal allow or deny, written the way somebody would have written it.
     *
     * <p>⚠️ {@link DirectGrant#allowed()} false becomes a visible {@code deny}, never a missing line.
     * A denial is not the absence of a grant — it is what takes one away, at every level, and a
     * reader who cannot see it cannot understand the result.
     */
    private static PolicyGrant toGrant(DirectGrant grant) {
        return new PolicyGrant(
                grant.permission(),
                toScope(grant.at()),
                grant.allowed() ? PolicyEffect.ALLOW : PolicyEffect.DENY,
                null,
                SourceSpan.none());
    }

    private static PolicyScope toScope(ScopeReference where) {
        return where.type().namesAnInstance()
                ? PolicyScope.of(where.type().name(), where.id())
                : PolicyScope.kind(where.type().name());
    }

    // ── Entitlements ──────────────────────────────────────────────────────────

    /**
     * What an {@link EntitlementStore} says about these subjects, as an {@code entitlements { }} block.
     *
     * <p>The capability half of the same idea, and it earns its place more here than for permissions.
     * <em>"Why can this account use custody"</em> has four possible answers — their bundle, a trial, a
     * gift, or nothing — and a control room that renders all four in one notation is one where the
     * question has a single answer rather than one answer per source.
     *
     * <p>⚠️ <strong>Expired grants are rendered, not dropped.</strong> The store returns them
     * deliberately, and a projection that filtered them would destroy the difference between
     * <em>"ran until 12 September"</em> and <em>"never had it"</em> — which is the difference between
     * a sentence somebody can act on and one they cannot.
     *
     * <p>⚠️ <strong>A bundle comes back as its capabilities, never as {@code plan team}.</strong> A
     * store holds what a bundle <em>issued</em>; which bundle that was is provenance carried on each
     * grant. Reconstructing the bundle line would mean matching a set of grants against a catalogue
     * and guessing, and would then read as a fact when it was an inference.
     *
     * @param documentName what the derived document is called
     * @param store        where the capability grants come from
     * @param places       the places to report on, in the order they should be written. ⚠️ Named
     *                     rather than discovered: a store that could enumerate every account would be
     *                     a store the engine could walk, and whoever opens an administration screen
     *                     already knows which accounts they are looking at
     */
    public static PolicyDocument projectEntitlements(
            String documentName, EntitlementStore store, List<ScopeReference> places) {

        List<PolicyEntitlement> entitlements = new ArrayList<>();

        for (ScopeReference place : places) {
            for (CapabilityGrant grant : store.heldAt(place)) {
                entitlements.add(toEntitlement(grant));
            }
        }

        return new PolicyDocument(documentName, List.of(), List.of(), List.of(), List.of(),
                                  List.of(), List.of(), List.of(), entitlements);
    }

    /**
     * One capability grant, written the way somebody would have written it.
     *
     * <p>⚠️ A refusal becomes a visible {@code deny} carrying its reason, never a missing line. A
     * capability withheld by hand and one nobody ever granted are the same absence to the engine and
     * completely different sentences to whoever reads the refusal — and only one of the two is
     * something they can go and argue with.
     */
    private static PolicyEntitlement toEntitlement(CapabilityGrant grant) {
        Allowance allowance = grant.allowance();

        return new PolicyEntitlement(
                toScope(grant.at()),
                grant.allowed() ? PolicyEntitlement.Kind.ALLOW : PolicyEntitlement.Kind.DENY,
                grant.capability(),
                quantityOf(allowance),
                allowance != null && allowance.isUnlimited(),
                asDay(grant.validity().from()),
                asDay(grant.validity().until()),
                grant.reason(),
                SourceSpan.none());
    }

    private static String quantityOf(Allowance allowance) {
        if (allowance == null || allowance.isUnlimited()) {
            return null;
        }

        return String.valueOf(allowance.quantity());
    }

    /**
     * ⚠️ Rendered as a day in UTC — the same reading the loader used when it turned
     * {@code until 2026-09-12} into an instant. A projection printing a local date would round-trip
     * into a different grant on a machine in another timezone, and the two would differ by one day in
     * whichever direction nobody tested.
     */
    private static String asDay(Instant moment) {
        return moment == null ? null : moment.atZone(ZoneOffset.UTC).toLocalDate().toString();
    }

    /** One of the two questions a store answers about one subject — held-by, or covering a chain. */
    @FunctionalInterface
    private interface SubjectQuestion<T> {

        List<T> about(String subjectId);
    }
}
