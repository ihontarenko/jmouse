package org.jmouse.access.policy.model;

/**
 * One capability given to — or taken away from — one subject, at one place, possibly for a while.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * entitlements {
 *     {@literal @}ORGANIZATION:acme  plan team
 *     {@literal @}ORGANIZATION:acme  allow custody
 *         from 2026-08-01 until 2026-09-01
 *         reason "Custody pilot — INN-482"
 *     {@literal @}ORGANIZATION:acme  allow entry 50000
 *     {@literal @}SPACE:acme-warehouse  deny custody  reason "Withheld pending the audit"
 *     {@literal @}ORGANIZATION:northwind  trial business until 2026-09-12
 * }
 * </pre>
 *
 * <p>⚠️ <strong>{@link #at} carries a handle, not an identifier.</strong> {@code acme} is a slug the
 * product resolves; UUIDs in a policy document are unreadable, and a document nobody can proofread is
 * a document nobody can be asked to approve. Resolution happens once, at load — a policy whose meaning
 * depends on runtime state is a policy nobody can read, and an unresolvable handle is a failure to
 * start rather than a grant belonging to an account that cannot exist.
 *
 * <h2>⚠️ A file may deny a row; it may never delete one</h2>
 *
 * <p>The same guarantee the permission half already makes, and the same reason: the document and the
 * tables are two stores whose grants are concatenated, so editing a document that could delete would
 * silently undo a decision made elsewhere by somebody else.
 *
 * <h2>⚠️ {@link #from} and {@link #until} are qualifiers, never a predicate</h2>
 *
 * <p>A window is expressible as a condition and must not be one. A {@code GrantCondition} is opaque —
 * it answers and reports its own source, and nothing looks inside — so a window in one leaves the
 * resolver unable to tell an <em>expired</em> grant from a <em>refused</em> one. Then no editor can
 * grey it out, no lint can warn that a rule died in September, and nobody can be told their trial
 * ended on the 12th.
 *
 * @param at         where it applies, with a handle rather than an identifier
 * @param kind       what the line said
 * @param subject    the capability key, or the plan code for {@link Kind#PLAN} and {@link Kind#TRIAL}
 * @param quantity   the amount exactly as written, or null
 * @param unlimited  whether the line said {@code unlimited}
 * @param from       the {@code from} date, as written, or null
 * @param until      the {@code until} date, as written, or null
 * @param reason     the words from {@code reason "…"}, or null. ⚠️ Not decoration on a deny: a
 *                   capability withheld by hand and one that was never in the bundle are the same
 *                   absence to the engine and completely different sentences to whoever reads the
 *                   refusal
 */
public record PolicyEntitlement(
        PolicyScope at,
        Kind        kind,
        String      subject,
        String      quantity,
        boolean     unlimited,
        String      from,
        String      until,
        String      reason,
        SourceSpan  span
) {

    /** What a line in {@code entitlements { }} says. */
    public enum Kind {

        /** {@code plan <code>} — issue everything that bundle contains. */
        PLAN,

        /** {@code trial <code> until <date>} — the same, with an end and a different provenance. */
        TRIAL,

        /** {@code allow <capability> [<amount>]} — one capability, outside any bundle. */
        ALLOW,

        /** {@code deny <capability>} — take it away, and say why. */
        DENY
    }

    public boolean isBundle() {
        return kind == Kind.PLAN || kind == Kind.TRIAL;
    }

    public boolean isBounded() {
        return from != null || until != null;
    }
}
