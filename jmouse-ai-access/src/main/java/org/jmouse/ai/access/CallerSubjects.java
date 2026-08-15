package org.jmouse.ai.access;

import org.jmouse.access.Subject;
import org.jmouse.ai.CallerIdentity;

/**
 * A caller, as the access engine understands one — the point where two identity models meet.
 *
 * <p>They meet cleanly, and in one line each. A caller acting on behalf of somebody is exactly the
 * engine's <em>service sub-account</em>, whose effective set is its own capped by its master's in every
 * scope; a caller acting for itself is an ordinary signed-in subject. So authorization rests on the
 * caller and ownership on the master, which is what both mechanisms already meant separately.
 *
 * <p><strong>Public, and it did not start that way.</strong> This was private inside
 * {@link AccessToolAuthorizer}, on the reasoning that exposing it would invite a product to build a
 * {@link Subject} here and use it somewhere else — putting the translation in two places. What actually
 * happened is the failure the other way round: a product with an authorizer of its own re-derived the
 * translation, got the agent case wrong by omitting it entirely, and its tool calls quietly authorized
 * as the wrong subject. One shared translation with two callers beats two translations that agree by
 * inspection.
 *
 * <p>⚠️ It remains a <em>translation</em> and decides nothing. Whether the subject it returns may do
 * anything is the engine's answer, and whether the caller was allowed to be a caller at all was settled
 * before this ran.
 */
public final class CallerSubjects {

    private CallerSubjects() {
    }

    /** The subject an engine should be asked about for this caller. */
    public static Subject of(CallerIdentity caller) {
        return caller.actsForItself()
                ? Subject.of(caller.callerId(), caller.describe())
                : Subject.agent(caller.callerId(), caller.actsOnBehalfOfId(), caller.describe());
    }
}
