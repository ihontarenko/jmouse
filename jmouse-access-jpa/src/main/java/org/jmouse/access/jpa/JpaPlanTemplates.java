package org.jmouse.access.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.access.jpa.entity.AccessPlan;
import org.jmouse.access.jpa.entity.AccessPlanGrant;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** The tier catalogue, out of the engine's own tables. */
public class JpaPlanTemplates implements PlanTemplates {

    private final EntityManager entityManager;

    public JpaPlanTemplates(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<TierView> all() {
        return entityManager.createQuery("""
                        SELECT plan FROM AccessPlan plan
                         ORDER BY plan.displayOrder ASC, plan.code ASC
                        """, AccessPlan.class)
                .getResultList().stream()
                .map(JpaPlanTemplates::describe)
                .toList();
    }

    @Override
    public Optional<TierView> byCode(String code) {
        return Optional.ofNullable(entityManager.find(AccessPlan.class, code))
                .map(JpaPlanTemplates::describe);
    }

    @Override
    public boolean define(TierView tier) {
        AccessPlan plan     = entityManager.find(AccessPlan.class, tier.code());
        boolean    existing = plan != null;

        // ⚠️ Both sides ordered before they are compared. A caller listing the same capabilities in a
        // different order is describing the same tier, and a comparison that disagreed would report an
        // edit on every save — which is how an audit trail becomes noise nobody reads.
        if (existing && describe(plan).equals(ordered(tier))) {
            return false;
        }

        if (!existing) {
            plan = new AccessPlan(
                    tier.code(), tier.displayName(), tier.order(), tier.note(), tier.extendsCode());

            entityManager.persist(plan);
        } else {
            plan.describeAs(tier.displayName(), tier.order(), tier.note(), tier.extendsCode());
        }

        plan.replaceGrants(grantsOf(tier));

        return true;
    }

    @Override
    public boolean retire(String code) {
        AccessPlan plan = entityManager.find(AccessPlan.class, code);

        if (plan == null) {
            return false;
        }

        entityManager.remove(plan);

        return true;
    }

    /**
     * The same tier in the shape {@link #describe} produces: inclusions ordered, periods upper-cased.
     *
     * <p>⚠️ The case matters as much as the order. A document writes {@code per month} and the column
     * holds {@code MONTH}, so a comparison against the raw text would report an edit on every single
     * save — and an audit trail that records a change nobody made is one nobody reads.
     */
    private static TierView ordered(TierView tier) {
        return new TierView(
                tier.code(), tier.displayName(), tier.order(), tier.note(), tier.extendsCode(),
                tier.includes().stream()
                        .map(inclusion -> new Inclusion(
                                inclusion.capability(), inclusion.quantity(),
                                upperCased(inclusion.period()), inclusion.unlimited()))
                        .sorted(Comparator.comparing(Inclusion::capability))
                        .toList());
    }

    private static String upperCased(String period) {
        return period == null || period.isBlank() ? null : period.toUpperCase(Locale.ROOT);
    }

    private static Set<AccessPlanGrant> grantsOf(TierView tier) {
        Set<AccessPlanGrant> grants = new LinkedHashSet<>();

        for (Inclusion inclusion : tier.includes()) {
            grants.add(new AccessPlanGrant(
                    tier.code(), inclusion.capability(), inclusion.quantity(),
                    inclusion.period(), inclusion.unlimited()));
        }

        return grants;
    }

    /**
     * ⚠️ Ordered by capability, and the order is what makes {@link #define} able to say <em>nothing
     * changed</em>. Two tiers listing the same capabilities in two orders are the same tier, and a
     * comparison that thought otherwise would report an edit on every save.
     */
    private static TierView describe(AccessPlan plan) {
        List<Inclusion> includes = plan.getGrants().stream()
                .map(grant -> new Inclusion(
                        grant.getCapability(), grant.getQuantity(), grant.getPeriod(), grant.isUnlimited()))
                .sorted(Comparator.comparing(Inclusion::capability))
                .toList();

        return new TierView(
                plan.getCode(),
                plan.getDisplayName(),
                plan.getDisplayOrder(),
                plan.getNote(),
                plan.getExtendsCode(),
                includes);
    }
}
