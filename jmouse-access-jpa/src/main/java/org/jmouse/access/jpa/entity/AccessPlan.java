package org.jmouse.access.jpa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * One tier: a named template of capabilities somebody may be put on.
 *
 * <h2>⚠️ A template, and nothing joins to it</h2>
 *
 * <p>Nothing anywhere points at this row as an answer to <em>what may this account do</em>. Who is on
 * a tier is a capability grant carrying the tier as its {@code source_reference}, and that has not
 * changed. This exists so that <em>which tiers exist and what each includes</em> has somewhere to live
 * once the policy document stops being read at runtime.
 *
 * <p>It is deliberately not the {@code plans} table ADR-0018 deleted. That one was half of a fact
 * written twice — the grants said who was on a tier and a column said it again. A catalogue of
 * templates duplicates nothing.
 *
 * <h2>⚠️ Editing one does not reissue it</h2>
 *
 * <p>A grant is materialised from a template at the moment of assignment. Changing what a tier
 * includes changes what the <em>next</em> account put on it receives and nothing about the accounts
 * already on it — which is right, because a text edit that silently re-provisioned every paying
 * customer would be an unreviewable mass mutation. Any screen editing this has to say so.
 */
@Entity
@Table(name = "access_plans")
public class AccessPlan {

    @Id
    @Column(name = "code", length = 64, nullable = false)
    private String code;

    @Column(name = "display_name", length = 128, nullable = false)
    private String displayName;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "note", length = 512)
    private String note;

    /**
     * ⚠️ The tier this one is written as a difference from, kept as a plain code rather than an
     * association.
     *
     * <p>Resolved on <strong>read</strong>, not flattened on write: an editor can then show an
     * inherited line as inherited, which is most of what makes a ladder legible, and a change to the
     * parent reaches the child without a rewrite. Flattening is simpler and loses both.
     */
    @Column(name = "extends_code", length = 64)
    private String extendsCode;

    /**
     * ⚠️ <strong>The join column is read-only, exactly as a role's bundle is.</strong> {@code plan_code}
     * is half of {@link AccessPlanGrant}'s own composite key, so leaving it writable maps one column
     * twice and Hibernate refuses to build the session factory at all — naming the entity rather than
     * this line. The inclusion carries its own plan code, set when it is constructed, so nothing is
     * lost by letting the key be the one thing that writes it.
     */
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "plan_code", nullable = false, insertable = false, updatable = false)
    private Set<AccessPlanGrant> grants = new LinkedHashSet<>();

    protected AccessPlan() {
    }

    public AccessPlan(String code, String displayName, int displayOrder, String note, String extendsCode) {
        this.code         = code;
        this.displayName  = displayName;
        this.displayOrder = displayOrder;
        this.note         = note;
        this.extendsCode  = extendsCode;
    }

    public String getCode()         { return code; }
    public String getDisplayName()  { return displayName; }
    public int    getDisplayOrder() { return displayOrder; }
    public String getNote()         { return note; }
    public String getExtendsCode()  { return extendsCode; }

    public Set<AccessPlanGrant> getGrants() {
        return grants;
    }

    /** Everything about the tier except what it contains — an edit, never a different tier. */
    public void describeAs(String displayName, int displayOrder, String note, String extendsCode) {
        this.displayName  = displayName;
        this.displayOrder = displayOrder;
        this.note         = note;
        this.extendsCode  = extendsCode;
    }

    /**
     * What the tier includes, replaced wholesale.
     *
     * <p>⚠️ Reconciled in place rather than cleared and refilled: {@code orphanRemoval} plus a
     * {@code clear()} makes Hibernate delete every row and insert it again, which turns a one-line
     * change into a rewrite of the table and gains nothing.
     *
     * <p>⚠️ <strong>A survivor takes the new allowance</strong>, and forgetting that is silent — the
     * entry stays, the row keeps yesterday's number, and a tier goes on selling what somebody just
     * changed. The same mistake was made once already on a role's bundle and the condition it carries.
     */
    public void replaceGrants(Set<AccessPlanGrant> wanted) {
        Map<AccessPlanGrant, AccessPlanGrant> asked = new LinkedHashMap<>();

        wanted.forEach(entry -> asked.put(entry, entry));

        grants.removeIf(held -> !asked.containsKey(held));

        grants.forEach(held -> {
            AccessPlanGrant wants = asked.get(held);

            held.allow(wants.getQuantity(), wants.getPeriod(), wants.isUnlimited());
        });

        wanted.stream().filter(entry -> !grants.contains(entry)).forEach(grants::add);
    }
}
