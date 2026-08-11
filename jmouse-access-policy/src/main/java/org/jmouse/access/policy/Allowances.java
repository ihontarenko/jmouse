package org.jmouse.access.policy;

import org.jmouse.access.Allowance;
import org.jmouse.access.AllowancePeriod;

import java.util.Locale;

/**
 * Turns what a file wrote into an {@link Allowance} — the one place a quantity stops being text.
 *
 * <p>The parser deliberately hands binding the amount <em>exactly as written</em>. Resolving it there
 * would put a units table inside a parser, and dropping a suffix would silently turn five gigabytes
 * into five bytes — a mistake no reader could see in the file and no test would notice until an
 * invoice did.
 *
 * <p>What a suffix <em>means</em> is not decided here either: see {@link QuantityScale}. A product
 * measuring storage registers one backed by {@code jmouse-core}'s {@code Bytes}; a product counting
 * automation runs registers nothing and gets whole numbers.
 *
 * <h2>⚠️ Why suffixes are worth a seam at all</h2>
 *
 * <p>The catalogue this replaces held {@code 107374182400}. A number nobody can proofread is a number
 * nobody checks, and being checkable by a human reading the file is the entire reason a catalogue
 * moves out of a migration and into a document. {@code 100GB} is the feature.
 */
public final class Allowances {

    /** What a file writes to mean "no ceiling". */
    public static final String UNLIMITED = "unlimited";

    private Allowances() {
    }

    /**
     * The allowance a plan line or an entitlement line confers, read with whole numbers only.
     */
    public static Allowance parse(String quantity, String period, boolean unlimited) {
        return parse(quantity, period, unlimited, QuantityScale.PLAIN);
    }

    /**
     * The allowance a plan line or an entitlement line confers.
     *
     * @param quantity  the amount as written, or null where the line carried none
     * @param period    the word after {@code per}, as written, or null
     * @param unlimited whether the line said {@code unlimited}
     * @param scale     what an amount means in this product's units
     * @return the allowance, or null where the line carried no number at all — which means the grant
     *         is about whether the capability is open, not about how much of it there is
     * @throws PolicyException naming what was written, where it will not parse
     */
    public static Allowance parse(String quantity, String period, boolean unlimited, QuantityScale scale) {
        if (unlimited) {
            return Allowance.unlimited();
        }
        if (quantity == null || quantity.isBlank()) {
            return null;
        }

        return new Allowance(scale.resolve(quantity.trim()), parsePeriod(period));
    }

    private static AllowancePeriod parsePeriod(String period) {
        if (period == null || period.isBlank()) {
            return null;
        }

        try {
            return AllowancePeriod.valueOf(period.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new PolicyException(
                    "'" + period + "' is not a period. A consumed quantity is counted per day, month, "
                    + "year, or 'ever' for one that is never reset.");
        }
    }
}
