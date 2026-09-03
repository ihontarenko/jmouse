package org.jmouse.money.jpa;

/**
 * 🏷️ Who decided a rate, and therefore whether a sync may replace it.
 */
public enum ExchangeRateSource {

    /** 📡 Came from a feed, and the next sync will overwrite it. */
    PROVIDER,

    /**
     * ✍️ Somebody set it by hand, and ⚠️ <strong>no sync will ever touch it again</strong>.
     *
     * <p>Deliberate and permanent until reversed explicitly. A rate pinned to a contract or to what an
     * accountant uses must not be quietly replaced overnight — but the flip side is a row that has
     * silently stopped tracking the feed, so any screen showing these has to mark them at a glance
     * rather than in a tooltip.</p>
     */
    MANUAL
}
