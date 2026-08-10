package org.jmouse.access.enforcement;

import org.jmouse.access.AccessDecision;
import org.jmouse.access.AccessTarget;

/**
 * A call that may not proceed, with everything needed to say why.
 *
 * <p>The decision alone is enough to throw; it is not enough to <em>explain</em>. A refusal worth
 * logging names what the endpoint asked for and where the call was aimed, and reconstructing either
 * from the decision is impossible — so the guard hands both back rather than making the adapter guess.
 *
 * @param required what the endpoint declared it needed
 * @param target   where the call was aimed, or null where the declaration named a row that does not
 *                 resolve — the one refusal that happens before there is a target at all
 * @param decision the engine's verdict, naming the axis that produced it
 */
public record AccessRefusal(AccessRequirement required, AccessTarget target, AccessDecision decision) {
}
