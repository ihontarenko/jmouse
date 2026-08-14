package org.jmouse.ai.guard;

import org.jmouse.ai.AffectedRecords;
import org.jmouse.ai.CallVerdict;

import java.util.List;

/**
 * What the guards did, and to what.
 *
 * <p>A guarded call does not always end in the work being done, and the three ways it can end are not
 * interchangeable to anyone reading a trail afterwards. Returning only the payload would leave every
 * reader inferring which of the three happened by looking for a {@code status} key inside it — a guess
 * dressed as a check, and one that silently stops working the day a status string is renamed.
 *
 * @param payload             what the caller receives
 * @param verdict             which of the three things happened
 * @param operationId         shared by a preview and the call that confirms it, so the two read as one
 *                            operation in a trail rather than as two unrelated entries
 * @param affected            the records the call reached, with their state as it stood beforehand
 * @param affectedCount       how many there really are; {@code affected} may be a capped prefix
 * @param throughConfirmation whether this call was previewed or was the confirmation of a preview —
 *                            which is exactly the set of calls whose previous state is worth keeping
 */
public record GuardedCall(
        Object                       payload,
        CallVerdict                  verdict,
        String                       operationId,
        List<AffectedRecords.Record> affected,
        long                         affectedCount,
        boolean                      throughConfirmation
) {

    public GuardedCall {
        affected = affected == null ? List.of() : List.copyOf(affected);
    }

    /** A call that reached nothing existing — a read, or a create. */
    public static GuardedCall carriedOut(Object payload, String operationId) {
        return new GuardedCall(payload, CallVerdict.CARRIED_OUT, operationId, List.of(), 0, false);
    }
}
