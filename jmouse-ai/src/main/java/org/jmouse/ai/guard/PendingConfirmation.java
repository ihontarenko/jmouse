package org.jmouse.ai.guard;

import org.jmouse.ai.AffectedRecords;

import java.util.List;

/**
 * What a preview promised.
 *
 * <p><strong>What is stored is the resolved set of records, never the filter.</strong> Re-running a
 * filter on the confirming call means the set can change between the preview and the commit, and more
 * is destroyed than was ever shown. A preview is a contract about specific records, not an estimate of
 * how many there might be by then.
 *
 * <p>{@code records} carries each record's state as it stood at preview time rather than merely its
 * identifier, and that is what lets a trail say what a deleted record <em>was</em>. By the time the
 * confirming call runs, the only honest moment to have read the record has already passed: re-reading
 * it would either find it unchanged, which is pointless, or changed, which is misleading.
 *
 * <p>Every field except the records is something the redemption compares, and each comparison has its
 * own refusal — see {@code ConfirmationGuard}, where the five of them are argued for individually.
 *
 * @param operationId   ties the preview and the commit together as one operation in a trail
 * @param callerId      who previewed; another caller's token is not a token
 * @param publishedName the action previewed, so a token for one operation cannot confirm another
 * @param fingerprint   the previewed arguments, so changed arguments are a different operation
 * @param scopeId       the scope the call <em>resolved to</em>, which the fingerprint does not cover;
 *                      null for an action that is not confined to one
 * @param records       the exact records, resolved and snapshotted at preview time, and frozen
 */
public record PendingConfirmation(
        String                       operationId,
        String                       callerId,
        String                       publishedName,
        String                       fingerprint,
        String                       scopeId,
        List<AffectedRecords.Record> records
) {

    public PendingConfirmation {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
