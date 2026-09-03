package org.jmouse.money;

import org.jmouse.money.exception.CurrencyMismatchException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 💰 An amount of one currency.
 *
 * <h3>⚠️ Adding two currencies THROWS, and that is not a runtime condition</h3>
 *
 * <p>Hryvnia plus dollars is not a number that is slightly wrong; it is not a number. Code that reaches
 * {@link #plus(Money)} with mismatched currencies has a bug in it, and returning an empty result would
 * let that bug travel as far as a report. So it throws {@link CurrencyMismatchException}, in the
 * tradition of {@link BigDecimal} throwing on a division with no exact representation: refuse the
 * meaningless answer at the point it is asked for.</p>
 *
 * <p>Converting between currencies is a different question with a different answer — it needs rates,
 * the rates may be missing, and missing is an ordinary outcome. That lives in {@link MoneyConverter}
 * and returns an {@code Optional}.</p>
 *
 * <h3>Why not JSR-354</h3>
 *
 * <p>{@code javax.money} brings a provider SPI, a units model and a rounding-operator abstraction, to
 * hold what is here a record with two components. This library needs the record.</p>
 *
 * @param amount   how much. ⚠️ Its {@link BigDecimal#scale()} is carried as given — {@code 5} and
 *                 {@code 5.00} are the same amount and are deliberately not normalised, because a price
 *                 quoted to four places is saying something about its precision
 * @param currency what it is an amount of
 */
public record Money(BigDecimal amount, CurrencyCode currency) {

    public Money {
        Objects.requireNonNull(amount, "An amount of money needs a number");
        Objects.requireNonNull(currency, "An amount of money needs a currency");
    }

    /** 💰 An amount, from a plain number. */
    public static Money of(BigDecimal amount, CurrencyCode currency) {
        return new Money(amount, currency);
    }

    /** 💰 Nothing, in a stated currency — the identity for {@link #plus(Money)}. */
    public static Money zero(CurrencyCode currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * ➕ Two amounts of the same currency.
     *
     * @throws CurrencyMismatchException where they are not the same currency
     */
    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }

        return new Money(amount.add(other.amount), currency);
    }

    /** ✖️ The same currency, a different quantity of it — a unit price times how many there are. */
    public Money times(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier), currency);
    }

    /** 🪙 Whether this is nothing at all. ⚠️ {@code signum}, so {@code 0.00} is zero as much as {@code 0} is. */
    public boolean isZero() {
        return amount.signum() == 0;
    }

    /**
     * 📏 The same amount at a stated number of decimal places.
     *
     * <p>⚠️ Rounding is <strong>stated</strong>, never defaulted, because the right answer differs by
     * caller: a total on a screen rounds half-up, and a figure somebody is invoiced from does not round
     * at all.</p>
     */
    public Money withScale(int scale, RoundingMode rounding) {
        return new Money(amount.setScale(scale, rounding), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
