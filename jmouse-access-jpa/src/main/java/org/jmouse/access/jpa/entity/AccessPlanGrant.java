package org.jmouse.access.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * One capability a tier includes, with its ceiling where it has one.
 *
 * <h2>⚠️ {@code quantity} is text, and that is not laziness</h2>
 *
 * <p>A tier says {@code storage-byte 100GB} three lines from {@code workspace 25}, and only one of
 * those is a size. What {@code GB} means belongs to the product — it is registered where the storage
 * layer's units are known — so a column parsing it here would make this schema hold an opinion about
 * units it cannot have. The amount is kept exactly as somebody wrote it and read through the
 * product's own scale.
 *
 * <p>No quantity and no {@code unlimited} means a <strong>gate</strong>: the capability is included
 * and nothing counts it.
 */
@Entity
@Table(name = "access_plan_grants")
public class AccessPlanGrant {

    @EmbeddedId
    private Key key;

    @Column(name = "quantity", length = 32)
    private String quantity;

    @Column(name = "period", length = 8)
    private String period;

    @Column(name = "unlimited", nullable = false)
    private boolean unlimited;

    protected AccessPlanGrant() {
    }

    public AccessPlanGrant(String planCode, String capability, String quantity, String period,
                           boolean unlimited) {

        this.key       = new Key(planCode, capability);
        this.quantity  = quantity;
        this.period    = normalised(period);
        this.unlimited = unlimited;
    }

    /**
     * ⚠️ <strong>Upper-cased, and this is a portability bug rather than a preference.</strong>
     *
     * <p>A document writes {@code per month} in lower case and the {@code CHECK} lists {@code MONTH}.
     * MySQL's case-insensitive collation accepts the row; PostgreSQL compares case-sensitively and
     * refuses it — so the same seed would build one database and fail on the other, at the first
     * install rather than in a test. Every other period column in this schema is upper case; this one
     * now agrees with them.
     */
    private static String normalised(String period) {
        return period == null || period.isBlank() ? null : period.toUpperCase(Locale.ROOT);
    }

    public String  getPlanCode()   { return key.planCode(); }
    public String  getCapability() { return key.capability(); }
    public String  getQuantity()   { return quantity; }
    public String  getPeriod()     { return period; }
    public boolean isUnlimited()   { return unlimited; }

    /** The allowance this line states — an edit to this line, never a different one. */
    public void allow(String quantity, String period, boolean unlimited) {
        this.quantity  = quantity;
        this.period    = normalised(period);
        this.unlimited = unlimited;
    }

    /**
     * ⚠️ <strong>Identity is the key alone</strong>, exactly as {@link AccessRolePermission} learned it.
     *
     * <p>The tempting alternative — comparing the whole line, so a changed allowance reads as a
     * different entry — produces a delete and an insert sharing one composite primary key in a single
     * flush, which Hibernate is under no obligation to order in the only way that works. A tier
     * includes a capability once; changing how much is an edit, and {@link #allow} is how it is made.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AccessPlanGrant entry)) {
            return false;
        }

        return Objects.equals(key, entry.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    /** A tier includes a capability once. */
    @Embeddable
    public record Key(
            @Column(name = "plan_code", length = 64, nullable = false) String planCode,
            @Column(name = "capability", length = 64, nullable = false) String capability
    ) implements Serializable {

        public Key {
            Objects.requireNonNull(planCode, "a plan grant belongs to a tier");
            Objects.requireNonNull(capability, "a plan grant is about a capability");
        }
    }
}
