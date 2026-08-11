package org.jmouse.access.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.access.Allowance;
import org.jmouse.access.AllowancePeriod;
import org.jmouse.access.ScopeCatalog;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.Validity;
import org.jmouse.access.jpa.entity.AccessCapabilityGrant;
import org.jmouse.access.spi.CapabilityGrant;
import org.jmouse.access.spi.CapabilityProvenance;
import org.jmouse.access.spi.EntitlementStore;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The engine's capability grants, out of the engine's own table.
 *
 * <h2>⚠️ Expired rows are returned, not filtered</h2>
 *
 * <p>There is no {@code AND valid_until > now} in either query, deliberately. Resolution drops an
 * expired grant and the control room prints it — that is what separates <em>"your trial ended on the
 * 12th"</em> from <em>"you never had this"</em>, and a predicate here would collapse the two into the
 * same silence.
 *
 * <h2>What this class no longer has to do</h2>
 *
 * <p>Its predecessor in the product carried a translation table between a persistence enum and a scope,
 * because a capability grant was addressed one way and a permission grant another. There is one
 * addressing scheme now, so a row is a {@link ScopeReference} and nothing has to be mapped.
 */
public class JpaEntitlementStore implements EntitlementStore {

    private final EntityManager entityManager;
    private final ScopeCatalog  scopes;

    public JpaEntitlementStore(EntityManager entityManager, ScopeCatalog scopes) {
        this.entityManager = entityManager;
        this.scopes        = scopes;
    }

    @Override
    public List<CapabilityGrant> covering(List<ScopeReference> chain) {
        if (chain.isEmpty()) {
            return List.of();
        }

        /*
         * ⚠️ Kinds and instances as two independent lists — one round trip rather than one per place —
         * so the result is WIDER than the chain and is narrowed back here. An engine that had to know
         * a store's queries were approximate would be an engine that knew the store.
         */
        return entityManager.createQuery("""
                        SELECT grant FROM AccessCapabilityGrant grant
                         WHERE grant.scopeType IN :kinds
                           AND grant.scopeId   IN :instances
                        """, AccessCapabilityGrant.class)
                .setParameter("kinds", chain.stream().map(place -> place.type().name()).distinct().toList())
                .setParameter("instances", chain.stream().map(ScopeReference::id).distinct().toList())
                .getResultList().stream()
                .map(this::toCapabilityGrant)
                .filter(grant -> chain.contains(grant.at()))
                .toList();
    }

    @Override
    public List<CapabilityGrant> heldAt(ScopeReference place) {
        return entityManager.createQuery("""
                        SELECT grant FROM AccessCapabilityGrant grant
                         WHERE grant.scopeType = :kind
                           AND grant.scopeId   = :instance
                        """, AccessCapabilityGrant.class)
                .setParameter("kind", place.type().name())
                .setParameter("instance", place.id())
                .getResultList().stream()
                .map(this::toCapabilityGrant)
                .toList();
    }

    private CapabilityGrant toCapabilityGrant(AccessCapabilityGrant row) {
        return new CapabilityGrant(
                row.getCapability(),
                row.allows(),
                ScopeReference.of(kind(row.getScopeType()), row.getScopeId()),
                allowanceOf(row),
                new Validity(row.getValidFrom(), row.getValidUntil()),
                CapabilityProvenance.of(row.getSource(), row.getSourceReference()),
                row.getGrantedBy(),
                row.getReason(),
                row.getCreatedAt() == null
                        ? null
                        : java.time.LocalDateTime.ofInstant(row.getCreatedAt(), java.time.ZoneId.systemDefault()),
                null);
    }

    /**
     * ⚠️ A null quantity on a <em>metered</em> capability means <strong>no ceiling</strong>, and on an
     * unmetered one means there is no ceiling to speak of. The two readings are opposite and the
     * <em>period</em> is what separates them: a row with a period is about a quantity, one without is
     * about whether the capability is open at all.
     */
    private Allowance allowanceOf(AccessCapabilityGrant row) {
        if (row.getAllowance() != null) {
            return new Allowance(row.getAllowance(), periodOf(row));
        }

        return row.getAllowancePeriod() == null ? null : Allowance.unlimited();
    }

    private AllowancePeriod periodOf(AccessCapabilityGrant row) {
        return row.getAllowancePeriod() == null ? null : AllowancePeriod.valueOf(row.getAllowancePeriod());
    }

    private org.jmouse.access.ScopeKind kind(String name) {
        return scopes.byName(name).orElseThrow(() -> new IllegalStateException(
                "A stored capability grant names the scope '" + name + "', which this installation "
                + "does not register. Known scopes: "
                + scopes.all().stream().map(org.jmouse.access.ScopeKind::name).collect(Collectors.joining(", "))
                + "."));
    }
}
