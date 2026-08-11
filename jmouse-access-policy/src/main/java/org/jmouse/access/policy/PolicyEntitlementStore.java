package org.jmouse.access.policy;

import org.jmouse.access.Allowance;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.Validity;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.policy.model.PolicyEntitlement;
import org.jmouse.access.policy.model.PolicyScope;
import org.jmouse.access.spi.CapabilityGrant;
import org.jmouse.access.spi.CapabilityProvenance;
import org.jmouse.access.spi.EntitlementStore;
import org.jmouse.access.spi.GrantOrigin;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A document, read as an {@link EntitlementStore} — so the engine cannot tell a declared entitlement
 * from a stored one.
 *
 * <p>The sibling of {@code PolicyGrantStore}, and the reason the composite works: a file and a table
 * both answer the same two questions, their answers are concatenated, and no precedence sits between
 * them. Deny already wins over every allow regardless of where either was written, and a composite
 * that introduced precedence would put a rule in front of that one.
 *
 * <h2>⚠️ A file may deny; it may never delete</h2>
 *
 * <p>This store contributes grants and removes none. Editing the document cannot take a row out of
 * anybody's table — it can only add a {@code deny}, which is visible, attributable and reversible.
 * The alternative would let a document silently undo a decision somebody else made on a screen.
 *
 * <h2>⚠️ Handles are resolved once, at construction</h2>
 *
 * <p>{@code @ORGANIZATION:acme} names a slug, not an identifier, because a document full of UUIDs is
 * a document nobody can be asked to approve. The resolution happens here, at load: a policy whose
 * meaning depends on runtime state is a policy nobody can read, and an unresolvable handle is a
 * failure to start rather than a grant belonging to an account that cannot exist.
 */
public final class PolicyEntitlementStore implements EntitlementStore {

    /** What a grant issued by a bundle reports as its source. */
    public static final String FROM_PLAN = "PLAN";

    /** What a bundle granted for a limited time reports. */
    public static final String FROM_TRIAL = "TRIAL";

    /** What a line outside any bundle reports. */
    public static final String DECLARED = "DECLARED";

    private final String                                     documentName;
    private final Map<ScopeReference, List<CapabilityGrant>> byPlace;

    private PolicyEntitlementStore(
            String documentName, Map<ScopeReference, List<CapabilityGrant>> byPlace) {

        this.documentName = documentName;
        this.byPlace      = byPlace;
    }

    /**
     * Reads a document into a store.
     *
     * @param document the merged policy
     * @param scopes   the vocabulary the {@code @SCOPE} prefixes are resolved against
     * @param handles  turns a written handle into the identifier the product's rows carry
     */
    public static PolicyEntitlementStore of(
            PolicyDocument document, ScopeCatalog scopes, SubjectHandleResolver handles) {
        return of(document, scopes, handles, QuantityScale.PLAIN);
    }

    /**
     * Reads a document into a store, with amounts read in the product's own units.
     *
     * @param scale what {@code 100GB} means here — see {@link QuantityScale}
     */
    public static PolicyEntitlementStore of(
            PolicyDocument document, ScopeCatalog scopes, SubjectHandleResolver handles,
            QuantityScale scale) {

        Map<ScopeReference, List<CapabilityGrant>> byPlace = new java.util.LinkedHashMap<>();

        for (PolicyEntitlement entitlement : document.entitlements()) {
            ScopeReference at = placeOf(entitlement.at(), scopes, handles, document.name());

            byPlace.computeIfAbsent(at, key -> new ArrayList<>())
                   .addAll(grantsOf(document, entitlement, at, scale));
        }

        return new PolicyEntitlementStore(document.name(), Map.copyOf(byPlace));
    }

    @Override
    public List<CapabilityGrant> covering(List<ScopeReference> chain) {
        List<CapabilityGrant> covering = new ArrayList<>();

        for (ScopeReference place : chain) {
            covering.addAll(heldAt(place));
        }

        return covering;
    }

    @Override
    public List<CapabilityGrant> heldAt(ScopeReference place) {
        return byPlace.getOrDefault(place, List.of());
    }

    /** What the control room prints as the provenance of everything in here. */
    public String documentName() {
        return documentName;
    }

    private static List<CapabilityGrant> grantsOf(
            PolicyDocument document, PolicyEntitlement entitlement, ScopeReference at,
            QuantityScale scale) {

        Validity validity = validityOf(entitlement);
        GrantOrigin origin = GrantOrigin.declaredIn(document.name(), entitlement.span().line());

        if (entitlement.isBundle()) {
            return bundleGrants(document, entitlement, at, validity, origin, scale);
        }

        CapabilityProvenance provenance = CapabilityProvenance.of(DECLARED);
        Allowance            allowance  = Allowances.parse(
                entitlement.quantity(), null, entitlement.unlimited(), scale);

        return List.of(new CapabilityGrant(
                entitlement.subject(),
                entitlement.kind() == PolicyEntitlement.Kind.ALLOW,
                at, allowance, validity, provenance,
                null, entitlement.reason(), null, origin));
    }

    /**
     * A bundle line becomes one grant per capability it contains.
     *
     * <p>⚠️ <strong>Expanded here rather than carried as "they are on Team".</strong> A grant that
     * named a bundle would have to be re-read against the catalogue on every decision, and the answer
     * would change under existing customers the moment somebody edited the tier. Expanding at load
     * makes the document's meaning fixed at the moment it was written, which is what a reviewer
     * approving it is actually approving.
     */
    private static List<CapabilityGrant> bundleGrants(
            PolicyDocument document, PolicyEntitlement entitlement,
            ScopeReference at, Validity validity, GrantOrigin origin, QuantityScale scale) {

        CapabilityProvenance provenance = CapabilityProvenance.of(
                entitlement.kind() == PolicyEntitlement.Kind.TRIAL ? FROM_TRIAL : FROM_PLAN,
                entitlement.subject());

        List<CapabilityGrant> grants = new ArrayList<>();

        for (Map.Entry<String, Allowance> included
                : PolicyPlans.contentsOf(document, entitlement.subject(), scale).entrySet()) {

            grants.add(new CapabilityGrant(
                    included.getKey(), true, at, included.getValue(), validity, provenance,
                    null, entitlement.reason(), null, origin));
        }

        return grants;
    }

    private static Validity validityOf(PolicyEntitlement entitlement) {
        return new Validity(startOfDay(entitlement.from()), startOfDay(entitlement.until()));
    }

    /**
     * ⚠️ A date in a policy is a <em>day</em>, and {@code until 2026-09-12} means the grant is gone
     * when that day begins.
     *
     * <p>The alternative reading — inclusive of the day named — is defensible and is not what
     * {@code Validity} implements, so the two must not disagree: the resolver asks
     * {@code moment.isBefore(until)}, and a reader who expects the 12th to be their last full day
     * writes {@code until 2026-09-13}.
     */
    private static Instant startOfDay(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(date.trim()).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException malformed) {
            throw new PolicyException(
                    "'" + date + "' is not a date. Write it as 2026-09-12 — a policy that has to be "
                    + "read in somebody's local format is a policy two readers disagree about.");
        }
    }

    /**
     * A place written in a document, as the engine addresses it.
     *
     * <p>⚠️ <strong>The handle is resolved here, and that is the whole point of the method.</strong> A
     * document says {@code @ORGANIZATION:acme} because a slug is what a person can write down and
     * recognise; the covering chain the engine walks carries the identifier the product's rows carry.
     * Building the reference out of the written handle produces a grant that looks right in every
     * projection and matches nothing at decision time — a rule that is written down and does nothing,
     * which is worse than no rule.
     */
    private static ScopeReference placeOf(
            PolicyScope scope, ScopeCatalog scopes, SubjectHandleResolver handles, String document) {

        ScopeKind kind = scopes.byName(scope.kind()).orElseThrow(() -> new PolicyException(
                "'" + document + "' entitles something at '" + scope.kind() + "', which this "
                + "installation has no scope called. Known scopes: " + describe(scopes) + "."));

        return ScopeReference.of(kind, handles.resolve(scope.kind(), scope.instance()));
    }

    private static String describe(ScopeCatalog scopes) {
        return String.join(", ", scopes.all().stream().map(ScopeKind::name).toList());
    }

    /**
     * Turns a handle written in a document into the identifier a product's rows carry.
     *
     * <p>⚠️ Registered by the product, because only it knows that {@code acme} is an organisation's
     * slug. The engine names no place and cannot look one up.
     */
    @FunctionalInterface
    public interface SubjectHandleResolver {

        /**
         * @param scopeName the scope the handle was written against
         * @param handle    what the file wrote
         * @return the identifier, never null — an unresolvable handle must throw, so a document
         *         naming an account that does not exist fails at load rather than granting nothing
         *         silently
         */
        String resolve(String scopeName, String handle);
    }
}
