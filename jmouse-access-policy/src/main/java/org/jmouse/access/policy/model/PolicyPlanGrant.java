package org.jmouse.access.policy.model;

/**
 * One line in a plan's body — a capability the bundle includes, and how much of it.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * plan team "Team" order 20 {
 *     seat          5
 *     storage-byte  5GB per month
 *     parametric-search
 * }
 * </pre>
 *
 * <p>produces, in order:
 *
 * <pre>
 *   new PolicyPlanGrant("seat",              "5",          null,    false, …)
 *   new PolicyPlanGrant("storage-byte",      "5GB",        "month", false, …)
 *   new PolicyPlanGrant("parametric-search", null,         null,    false, …)
 * </pre>
 *
 * <p>and {@code seat unlimited} produces {@code new PolicyPlanGrant("seat", null, null, true, …)}.
 *
 * <p>⚠️ <strong>{@link #quantity} is the text as written, not a number.</strong> {@code 5GB} has to
 * survive to binding intact: a parser that resolved it would own a units table, and a parser that
 * dropped the suffix would silently turn five gigabytes into five bytes. Binding turns it into an
 * {@link org.jmouse.access.Allowance}, where a bad suffix can fail by name.
 *
 * <p>⚠️ <strong>{@code unlimited} is a flag, not a sentinel quantity.</strong> A very large number
 * would be indistinguishable from a real ceiling to every screen above, and a tier sold as "unlimited"
 * that renders as a number has lied to whoever is paying for it.
 *
 * @param capability what the line is about, as written
 * @param quantity   the amount exactly as written — {@code "5"}, {@code "5GB"} — or null where the
 *                   line carried none, which means the bundle simply includes the capability
 * @param period     the word after {@code per}, as written, or null
 * @param unlimited  whether the line said {@code unlimited}
 */
public record PolicyPlanGrant(
        String     capability,
        String     quantity,
        String     period,
        boolean    unlimited,
        SourceSpan at
) {

    /** A capability the bundle includes with no number — a gate. */
    public static PolicyPlanGrant included(String capability, SourceSpan at) {
        return new PolicyPlanGrant(capability, null, null, false, at);
    }

    /** Whether this line carries a ceiling at all. */
    public boolean isMetered() {
        return unlimited || quantity != null;
    }
}
