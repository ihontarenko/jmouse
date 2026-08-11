package org.jmouse.access.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.access.Allowance;
import org.jmouse.access.AllowancePeriod;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.Validity;
import org.jmouse.access.jpa.entity.AccessCapabilityGrant;
import org.jmouse.access.jpa.entity.AccessCapabilitySwitch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Changing the entitlement axis, against the engine's own tables.
 *
 * <h2>⚠️ Nothing here audits anything, and that is the design</h2>
 *
 * <p>Every write answers with what it changed instead. The product's screen records that somebody made
 * it, in the product's own audit vocabulary — which is the split that was being defended when
 * {@code EntitlementStore} was made read-only, minus the conclusion that was over-drawn from it: that
 * the <em>row</em> must therefore be the product's too.
 *
 * <h2>Plain JPA, deliberately</h2>
 *
 * <p>{@link EntityManager} and JPQL rather than Spring Data, for the reason the stores beside it give:
 * this module depends on {@code jakarta.persistence-api} and nothing else, so adopting the engine's
 * storage does not also mean adopting somebody's repository framework.
 */
public class JpaEntitlementAdministration implements EntitlementAdministration {

    private final EntityManager    entityManager;
    private final ScopeCatalog     scopes;
    private final Supplier<String> identifiers;

    public JpaEntitlementAdministration(EntityManager entityManager, ScopeCatalog scopes) {
        this(entityManager, scopes, () -> UUID.randomUUID().toString());
    }

    /**
     * @param identifiers how new rows are named — the same seam {@link JpaAccessAdministration} takes,
     *                    and for the same reason: a library quietly imposing UUIDs on a schema that
     *                    uses something else is a schema decision made by accident
     */
    public JpaEntitlementAdministration(EntityManager entityManager, ScopeCatalog scopes,
                                        Supplier<String> identifiers) {

        this.entityManager = entityManager;
        this.scopes        = scopes;
        this.identifiers   = identifiers;
    }

    // ── Grants ────────────────────────────────────────────────────────────────

    @Override
    public Issued issue(GrantTerms terms) {
        String kind = terms.allowed() ? "ALLOW" : "DENY";

        Optional<AccessCapabilityGrant> standing =
                grant(terms.at(), terms.capability(), kind, terms.source());

        AccessCapabilityGrant grant = standing.orElseGet(() -> new AccessCapabilityGrant(
                identifiers.get(), terms.at().type().name(), terms.at().id(),
                terms.capability(), kind, terms.source()));

        boolean unchanged = standing.isPresent() && statesTheSameAs(grant, terms);

        grant.restate(
                terms.validity() == null ? null : terms.validity().from(),
                terms.validity() == null ? null : terms.validity().until(),
                terms.allowance() == null ? null : terms.allowance().quantity(),
                periodNameOf(terms.allowance()),
                terms.sourceReference(),
                terms.reason(),
                terms.grantedBy());

        if (standing.isEmpty()) {
            entityManager.persist(grant);
        }

        return new Issued(describe(grant), !unchanged);
    }

    @Override
    public Optional<GrantView> withdraw(String grantId) {
        AccessCapabilityGrant grant = entityManager.find(AccessCapabilityGrant.class, grantId);

        if (grant == null) {
            return Optional.empty();
        }

        // Described before it goes, for the same reason an audit of a deletion is written first.
        GrantView withdrawn = describe(grant);

        entityManager.remove(grant);

        return Optional.of(withdrawn);
    }

    @Override
    public List<GrantView> withdrawAllFrom(ScopeReference at, String source) {
        List<AccessCapabilityGrant> issued = entityManager.createQuery("""
                        SELECT grant FROM AccessCapabilityGrant grant
                         WHERE grant.scopeType = :kind
                           AND grant.scopeId   = :instance
                           AND grant.source    = :source
                        """, AccessCapabilityGrant.class)
                .setParameter("kind", at.type().name())
                .setParameter("instance", at.id())
                .setParameter("source", source)
                .getResultList();

        // Described before they go, because the caller records what it removed and a removed row
        // cannot be read afterwards — the same reason an audit of a deletion is written first.
        List<GrantView> withdrawn = issued.stream().map(this::describe).toList();

        issued.forEach(entityManager::remove);

        return withdrawn;
    }

    @Override
    public List<GrantView> grantsAt(ScopeReference at) {
        return entityManager.createQuery("""
                        SELECT grant FROM AccessCapabilityGrant grant
                         WHERE grant.scopeType = :kind
                           AND grant.scopeId   = :instance
                        """, AccessCapabilityGrant.class)
                .setParameter("kind", at.type().name())
                .setParameter("instance", at.id())
                .getResultList().stream()
                .map(this::describe)
                .toList();
    }

    @Override
    public List<SourceReference> sourceReferencesAt(String scopeType, List<String> scopeIds, String source) {
        if (scopeIds.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery("""
                        SELECT DISTINCT grant.scopeId, grant.sourceReference
                          FROM AccessCapabilityGrant grant
                         WHERE grant.scopeType       = :kind
                           AND grant.source          = :source
                           AND grant.sourceReference IS NOT NULL
                           AND grant.scopeId IN :instances
                        """, Object[].class)
                .setParameter("kind", scopeType)
                .setParameter("source", source)
                .setParameter("instances", scopeIds)
                .getResultList().stream()
                .map(row -> new SourceReference((String) row[0], (String) row[1]))
                .toList();
    }

    // ── Switches ──────────────────────────────────────────────────────────────

    @Override
    public List<SwitchView> switchesAt(ScopeReference at) {
        return switches(at).stream()
                .map(stored -> new SwitchView(stored.getCapability(), stored.isEnabled(), stored.isForced()))
                .toList();
    }

    @Override
    public boolean setSwitch(ScopeReference at, String capability, boolean enabled, boolean forced) {
        Optional<AccessCapabilitySwitch> standing = capabilitySwitch(at, capability);

        if (standing.isPresent()) {
            return standing.get().flip(enabled, forced);
        }

        entityManager.persist(new AccessCapabilitySwitch(
                identifiers.get(), at.type().name(), at.id(), capability, enabled, forced));

        return true;
    }

    @Override
    public boolean clearSwitch(ScopeReference at, String capability) {
        return capabilitySwitch(at, capability)
                .map(stored -> {
                    entityManager.remove(stored);
                    return true;
                })
                .orElse(false);
    }

    // ── Shared ────────────────────────────────────────────────────────────────

    private Optional<AccessCapabilityGrant> grant(
            ScopeReference at, String capability, String kind, String source) {

        return entityManager.createQuery("""
                        SELECT grant FROM AccessCapabilityGrant grant
                         WHERE grant.scopeType  = :kind
                           AND grant.scopeId    = :instance
                           AND grant.capability = :capability
                           AND grant.grantKind  = :grantKind
                           AND grant.source     = :source
                        """, AccessCapabilityGrant.class)
                .setParameter("kind", at.type().name())
                .setParameter("instance", at.id())
                .setParameter("capability", capability)
                .setParameter("grantKind", kind)
                .setParameter("source", source)
                .getResultStream()
                .findFirst();
    }

    private List<AccessCapabilitySwitch> switches(ScopeReference at) {
        return entityManager.createQuery("""
                        SELECT stored FROM AccessCapabilitySwitch stored
                         WHERE stored.scopeType = :kind
                           AND stored.scopeId   = :instance
                        """, AccessCapabilitySwitch.class)
                .setParameter("kind", at.type().name())
                .setParameter("instance", at.id())
                .getResultList();
    }

    private Optional<AccessCapabilitySwitch> capabilitySwitch(ScopeReference at, String capability) {
        return entityManager.createQuery("""
                        SELECT stored FROM AccessCapabilitySwitch stored
                         WHERE stored.scopeType  = :kind
                           AND stored.scopeId    = :instance
                           AND stored.capability = :capability
                        """, AccessCapabilitySwitch.class)
                .setParameter("kind", at.type().name())
                .setParameter("instance", at.id())
                .setParameter("capability", capability)
                .getResultStream()
                .findFirst();
    }

    /**
     * Whether re-issuing would say exactly what the standing grant already says.
     *
     * <p>⚠️ {@code validFrom} is deliberately not compared. A caller that leaves it null means "now",
     * and comparing "now" against a row written a minute ago would report a change on every save — so
     * every re-issue of an unchanged plan would produce an audit event nobody made.
     */
    private static boolean statesTheSameAs(AccessCapabilityGrant standing, GrantTerms terms) {
        Validity  validity  = terms.validity();
        Allowance allowance = terms.allowance();

        return equal(standing.getValidUntil(), validity == null ? null : validity.until())
               && equal(standing.getAllowance(), allowance == null ? null : allowance.quantity())
               && equal(standing.getAllowancePeriod(), periodNameOf(allowance))
               && equal(standing.getSourceReference(), terms.sourceReference())
               && equal(standing.getReason(), terms.reason());
    }

    private static String periodNameOf(Allowance allowance) {
        AllowancePeriod period = allowance == null ? null : allowance.period();

        return period == null ? null : period.name();
    }

    private static boolean equal(Object one, Object other) {
        return one == null ? other == null : one.equals(other);
    }

    /**
     * ⚠️ Answers with a {@link ScopeReference} rather than the two stored strings, so a caller never
     * has to know that a place is a pair — and so the one place a scope <em>name</em> becomes a scope
     * <em>kind</em> is here, exactly as it is in the stores beside this class.
     */
    private GrantView describe(AccessCapabilityGrant grant) {
        return new GrantView(
                grant.getId(),
                ScopeReference.of(kind(grant.getScopeType()), grant.getScopeId()),
                grant.getCapability(),
                grant.allows(),
                grant.getSource(),
                grant.getSourceReference(),
                new Validity(grant.getValidFrom(), grant.getValidUntil()),
                allowanceOf(grant),
                grant.getReason(),
                grant.getGrantedBy());
    }

    /**
     * ⚠️ A null allowance on a metered capability means <strong>no ceiling</strong>, not "no allowance",
     * and the grant's own period is what says which reading applies — a row with a period is about a
     * quantity, and one without is about whether the capability is open at all.
     */
    private static Allowance allowanceOf(AccessCapabilityGrant grant) {
        if (grant.getAllowance() != null) {
            return new Allowance(grant.getAllowance(), periodOf(grant));
        }

        return grant.getAllowancePeriod() == null ? null : Allowance.unlimited();
    }

    private static AllowancePeriod periodOf(AccessCapabilityGrant grant) {
        return grant.getAllowancePeriod() == null
                ? null
                : AllowancePeriod.valueOf(grant.getAllowancePeriod());
    }

    /**
     * ⚠️ An unregistered scope name is a programming error rather than a case to handle: the row was
     * written by this installation, against a vocabulary it registers.
     */
    private org.jmouse.access.ScopeKind kind(String name) {
        return scopes.byName(name).orElseThrow(() -> new IllegalStateException(
                "A stored capability grant names the scope '" + name + "', which this installation "
                + "does not register."));
    }
}
