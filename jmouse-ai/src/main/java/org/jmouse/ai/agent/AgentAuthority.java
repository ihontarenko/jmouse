package org.jmouse.ai.agent;

/**
 * Whose permissions an agent acts with.
 *
 * <p><strong>Two answers, and the fork is real rather than a setting nobody touches.</strong> An access
 * engine caps a service sub-account against its owner by intersecting the two sets — which is exactly
 * right for an agent deliberately given less than its owner, and exactly wrong for one that has been
 * given nothing yet, because an empty set intersected with anything is empty. An agent created a moment
 * ago would therefore be refused every single call, and the refusal would read as a broken connection
 * rather than as a permission nobody has granted.
 *
 * <p>So the two cases are named instead of one of them being a bug. Which one an agent is stays a
 * decision somebody made and can see, rather than something inferred from whether a grant table happens
 * to have rows in it — an inference is the same behaviour with nowhere to read it and no way to say
 * <em>yes, on purpose</em>.
 *
 * <p>⚠️ <strong>Neither is a permission model.</strong> This picks which <em>subject</em> the engine is
 * asked about; the engine still answers, against the same policy as everything else.
 */
public enum AgentAuthority {

    /**
     * It acts with everything its owner holds, and follows them.
     *
     * <p>The owner joins a project on Monday and the agent reaches it on Monday, because there is no
     * copy to go stale — the engine is asked about the owner. What an agent may do is therefore exactly
     * what the person could have done by hand, which is the only defensible default for something a
     * person connected to act for them.
     *
     * <p>⚠️ <strong>It is still a distinct identity.</strong> Authorization asks about the owner;
     * everything else — what a record says made it, what a trail counted, what a screen revokes — is the
     * agent. Collapsing those would be losing the identity, not inheriting the authority.
     */
    INHERITED,

    /**
     * It holds its own grants, capped by its owner's in every scope.
     *
     * <p>The narrower half of the pair, and the point of having agents at all where the answer to
     * <em>what could this thing do if it went wrong</em> has to be smaller than <em>everything I can
     * do</em>. Its set is subtracted from live, so an owner losing access loses it for the agent in the
     * same request — a stale copy can only ever narrow, never widen.
     *
     * <p>⚠️ <strong>An agent switched to this before it has been granted anything can do nothing.</strong>
     * That is the honest behaviour and not a defect: restricting is an act, and an act that silently left
     * everything permitted would be worse than one that briefly permits nothing.
     */
    RESTRICTED
}
