package org.jmouse.money.spring.provider;

import org.jmouse.money.CurrencyCode;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 📡 Somewhere rates come from.
 *
 * <h3>⚠️ A seam here, and the reason is not symmetry with the rest of the framework</h3>
 *
 * <p>An existing implementation in this workspace says in its own comment that one provider does not
 * justify a strategy interface, and it is right — <em>there</em>. That product converts to the hryvnia
 * and always will, so the feed and the pivot are one decision made once.</p>
 *
 * <p>Here the pivot is configuration. A configurable pivot the National Bank of Ukraine cannot serve —
 * it quotes only against the hryvnia — is an installation that must be able to name a different feed.
 * The seam is what makes the pivot honest rather than a setting that silently only has one legal value.</p>
 *
 * <h3>⚠️ A provider states its own pivot, and does not take one</h3>
 *
 * <p>Whatever a feed publishes is what it publishes. A provider asked to quote against something else
 * would have to convert, which is the caller's job and needs rates it does not have. So the provider
 * says what it quotes, and the service refuses the mismatch loudly rather than storing rows that mean
 * something other than the table claims.</p>
 */
public interface ExchangeRateProvider {

    /** 🏷️ A short name for logs and for the screen — {@code nbu}. */
    String name();

    /** 💱 The currency this feed quotes everything against. */
    CurrencyCode pivot();

    /**
     * 📡 Fetch the current rates.
     *
     * <p>⚠️ Codes the feed returns that {@link CurrencyCode#of(String)} cannot read are dropped by the
     * implementation rather than passed on — a row keyed on a string that is not a currency is a row
     * nothing will ever match, and it would sit in the table looking like data.</p>
     *
     * @return how many units of {@link #pivot()} one unit of each currency is worth
     */
    Map<CurrencyCode, BigDecimal> fetchRatesToPivot();
}
