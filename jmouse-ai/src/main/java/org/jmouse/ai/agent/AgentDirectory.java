package org.jmouse.ai.agent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Which agents exist, who owns them, and whether they may act.
 *
 * <p><strong>Why this is a library port and not a product's service.</strong> Every product that let
 * something act on a person's behalf wrote the same five decisions: an agent has an owner, an agent has
 * a name a human chose, an agent can be switched off without being deleted, an agent's last activity is
 * worth stamping, and an agent that outlives its owner is a leak. None of those five is a matter of
 * taste, and each one written twice is one written differently.
 *
 * <p><strong>What is deliberately absent: privileges.</strong> There is no {@code permissions} field and
 * no {@code grant} method, because a product that already authorizes its own endpoints already has a
 * place for both — and a second permission model here would be a second answer to the same question,
 * agreeing with the first until the afternoon somebody changed one of them. An agent's ceiling is its
 * owner's, in every scope, and {@link AgentCallers} is where that is said in one line.
 *
 * <h2>⚠️ Nothing cascades from a product's accounts, and that is not an oversight</h2>
 *
 * <p>{@link Agent#ownerReference()} carries no foreign key: a library table cannot reference a product's
 * accounts without knowing what an account is. So deleting a person leaves their agents behind unless
 * somebody says otherwise, and {@link #discardAllOwnedBy(String)} is that somebody. It has to be called
 * by hand from wherever accounts are deleted. Forgetting it leaves agents whose owner no longer exists —
 * which an access engine intersecting against a missing master will refuse rather than permit, so the
 * failure is quiet and safe rather than quiet and dangerous. It is still a leak.
 */
public interface AgentDirectory {

    /**
     * What somebody typed when they created one.
     *
     * @param authority whose permissions it acts with. {@link AgentAuthority#INHERITED} is the sane
     *                  default for something a person just connected — it can do what that person could
     *                  have done by hand, and follows them
     * @param enabled   whether it may act immediately. ⚠️ Worth passing {@code false} alongside
     *                  {@link AgentAuthority#RESTRICTED}, because the grants live in a different store
     *                  and the two writes cannot be one transaction — an agent that exists before its
     *                  own set does is an agent that can do nothing, and its owner will read that as a
     *                  broken connection
     */
    record Draft(String ownerReference, String name, AgentAuthority authority, boolean enabled) {
    }

    /**
     * Something this port will not do, in a sentence somebody can act on.
     *
     * <p>Unchecked, and a nested type rather than a package-level one, for the same reason the other
     * ports here do it: an adopter should not have to import an exception to call an interface.
     */
    class RefusedException extends RuntimeException {

        public RefusedException(String message) {
            super(message);
        }
    }

    /** Creates one. Refuses a name its owner is already using, because that is how a person tells two apart. */
    Agent register(Draft draft);

    Optional<Agent> find(String agentId);

    /** Everything one person has created, newest first — what an owner's screen lists. */
    List<Agent> ownedBy(String ownerReference);

    /**
     * Every agent in the installation, newest first — what an administration screen lists.
     *
     * <p>⚠️ <strong>A disclosure surface, and a bounded one.</strong> It answers who has connected what
     * across the whole installation, which is its own power and belongs behind the same permission the
     * rest of this library's management screens do. The limit is not a convenience: a listing with no
     * ceiling is a table scan anybody who reaches the route can ask for.
     */
    List<Agent> all(int limit);

    Agent rename(String agentId, String name);

    /**
     * Changes whose permissions it acts with.
     *
     * <p>⚠️ <strong>Switching to {@link AgentAuthority#RESTRICTED} takes effect on the next call and the
     * agent can then do nothing until it has been granted something.</strong> That is the honest
     * behaviour — restricting is an act, and one that silently left everything permitted would be worse
     * — but it means a screen offering this has to say so before the click rather than after it.
     */
    Agent actWith(String agentId, AgentAuthority authority);

    /** Lets it act again. Its connections and its privileges were never taken away. */
    Agent putInService(String agentId);

    /**
     * Stops it acting, immediately and reversibly.
     *
     * <p>⚠️ This is the switch that has to work while something is going wrong, so it does not touch the
     * connections: whoever pulls it wants the agent stopped, not a decision about which clients get to
     * come back afterwards. Ending the clients too is {@link AgentConnections#revokeAllOf(String)}, and
     * it is a separate call because it is a separate intention.
     */
    Agent takeOutOfService(String agentId);

    /** Deletes it and every connection under it. */
    void discard(String agentId);

    /** ⚠️ The cleanup the missing foreign key forces — call it wherever accounts are deleted. */
    void discardAllOwnedBy(String ownerReference);

    /**
     * Records that it actually did something.
     *
     * <p>⚠️ <strong>Never for a refusal.</strong> An agent that was refused did not act, and stamping it
     * would make a credential somebody is hammering indistinguishable from one somebody is using — which
     * is precisely the pair this field exists to tell apart.
     */
    void stampActive(String agentId, Instant when);
}
