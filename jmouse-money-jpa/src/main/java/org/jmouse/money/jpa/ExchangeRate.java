package org.jmouse.money.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 📈 One currency's rate against the pivot, as a row.
 *
 * <h3>⚠️ The pivot is a COLUMN, not a constant</h3>
 *
 * <p>A rate is meaningless without saying what it is a rate <em>to</em>. Rows quoted against the
 * hryvnia and rows quoted against the euro are different numbers that look identical, so a table which
 * did not record the pivot would be silently wrong the first time an installation pointed at a
 * different feed — and would look entirely correct while being so.</p>
 *
 * <p>The pivot itself is never a row here. Its rate against itself is one by definition, and storing it
 * invites somebody to edit it, which rescales every conversion at once.</p>
 *
 * <h3>⚠️ {@code source} is what makes a manual rate survive a sync</h3>
 *
 * <p>A {@link ExchangeRateSource#MANUAL} row is never overwritten by the provider. That is the whole
 * point of the column: somebody who deliberately pinned a rate — a contract rate, a rate their
 * accountant uses — must not have it silently replaced overnight.</p>
 *
 * <h3>⚠️ Keyed on the currency alone, which is a deliberate limit</h3>
 *
 * <p>One pivot at a time. A table holding two pivots at once is not a richer table, it is a broken one:
 * half the rows would answer a question the other half was not asked. The primary key says so, and
 * {@code ExchangeRateRegistry#pivots()} is how a caller notices that a feed has changed underneath it.</p>
 */
@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {

    @Id
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /** 📈 How many units of {@link #pivot} one unit of {@link #currency} is worth. */
    @Column(name = "rate_to_pivot", precision = 19, scale = 6, nullable = false)
    private BigDecimal rateToPivot;

    @Column(name = "pivot", length = 3, nullable = false)
    private String pivot;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 16, nullable = false)
    private ExchangeRateSource source;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExchangeRate() {
        // Jakarta Persistence.
    }

    public ExchangeRate(String currency, String pivot, BigDecimal rateToPivot, ExchangeRateSource source) {
        this.currency    = currency;
        this.pivot       = pivot;
        this.rateToPivot = rateToPivot;
        this.source      = source;
    }

    /**
     * 🕒 Stamped on every write, by the persistence provider rather than by a caller.
     *
     * <p>⚠️ A caller-set timestamp is a timestamp somebody forgets to set on the one path that matters.
     * The age of a rate is the only thing separating a trustworthy converted total from a misleading
     * one, so it is not left to discipline.</p>
     */
    @PrePersist
    @PreUpdate
    void stampUpdatedAt() {
        updatedAt = Instant.now();
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getRateToPivot() {
        return rateToPivot;
    }

    public void setRateToPivot(BigDecimal rateToPivot) {
        this.rateToPivot = rateToPivot;
    }

    public String getPivot() {
        return pivot;
    }

    public void setPivot(String pivot) {
        this.pivot = pivot;
    }

    public ExchangeRateSource getSource() {
        return source;
    }

    public void setSource(ExchangeRateSource source) {
        this.source = source;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ExchangeRate rate && Objects.equals(currency, rate.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(currency);
    }

    @Override
    public String toString() {
        return "1 " + currency + " = " + rateToPivot + " " + pivot + " (" + source + ")";
    }
}
