package org.jmouse.access;

/**
 * What is being counted, for whom, over which window.
 *
 * <h2>⚠️ {@code subjectKind} is a string, and that is the whole point of moving this</h2>
 *
 * <p>The implementation this replaces keyed its counters on a product enum of scopes, so a counter
 * could only ever be about a <em>place</em> — an organisation, a workspace. Which meant the obvious
 * question, <em>"how much has this person used in the last three hours"</em>, had nowhere to be
 * recorded, and the answer offered was to add a scope to an enum in a product the library cannot see.
 *
 * <p>A string costs the compiler's help and buys a counter about anything: a person, an agent, a group,
 * a tenant, a board, whatever a product invents next year. The library never interprets it — it
 * compares it and stores it — so there is nothing here for a new kind to break.
 *
 * <h2>⚠️ This does not make a quota grantable per person</h2>
 *
 * <p>Counting and entitling are different questions and this answers only the first.
 * {@code EntitlementStore} says a capability is granted to a <em>place</em>, and if per-person metering
 * is ever entitled it arrives as a scope rather than as a second addressing scheme. A counter needs no
 * grant resolution to record what somebody used, which is why it can be about a person years before an
 * allowance can.
 *
 * @param subjectKind whose consumption this is, in the product's own vocabulary — {@code user},
 *                    {@code agent}, {@code organization}. Never interpreted here
 * @param subjectId   the identifier within that kind
 * @param meter       what is being counted — {@code ai-token}, {@code storage-byte}. A product's word
 * @param windowKey   which period this counts, as {@link AllowancePeriod} spelled it. A window comes
 *                    into being by being written to, so nothing has to reset anything — and a reset job
 *                    that failed to run would hand somebody an unlimited month, discovered on an invoice
 */
public record ConsumptionKey(
        String subjectKind,
        String subjectId,
        String meter,
        String windowKey
) {

    public ConsumptionKey {
        requireText(subjectKind, "subjectKind");
        requireText(subjectId,   "subjectId");
        requireText(meter,       "meter");
        requireText(windowKey,   "windowKey");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "a consumption counter needs a " + name + ": a key missing one would silently share "
                    + "a row with every other key missing the same one");
        }
    }
}
