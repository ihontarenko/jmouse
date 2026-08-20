package org.jmouse.ai.agent;

import org.jmouse.ai.CallerAttributes;
import org.jmouse.ai.CallerIdentity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An agent, read as a caller — the whole of the privilege story, in one branch.
 *
 * <p><strong>The branch is here and nowhere else, on purpose.</strong> Which subject an engine is asked
 * about is a security decision with two correct answers, and a product re-deriving it from
 * {@link AgentAuthority} would be a product that can get it wrong — differently in each product, which
 * is how two installations end up disagreeing about what an agent may do. So the enum is read exactly
 * once, in this class, and everything downstream sees an ordinary {@link CallerIdentity} it does not
 * have to interpret.
 *
 * <table border="1">
 *   <caption>What each authority produces</caption>
 *   <tr><th>Authority</th><th>Identity</th><th>What the engine is asked</th></tr>
 *   <tr><td>{@link AgentAuthority#INHERITED}</td>
 *       <td>{@code of(owner)}</td>
 *       <td>the owner — so the agent follows them, with nothing to go stale</td></tr>
 *   <tr><td>{@link AgentAuthority#RESTRICTED}</td>
 *       <td>{@code of(agent)}</td>
 *       <td>the agent, and ONLY the agent — its own roles and permissions, uncapped</td></tr>
 * </table>
 *
 * <p>⚠️ <strong>The agent is named in the attributes either way, and that matters.</strong> Under
 * {@code INHERITED} the caller identifier is the <em>owner's</em>, so
 * {@link CallerAttributes#AGENT_ID} is the only place the agent survives — and provenance, a trail, and
 * the badge on a record all read it. Attributes are display and record-keeping, never an authorization
 * input, which is exactly the right register for "which of my agents did this".
 *
 * <p>⚠️ <strong>{@code RESTRICTED} is not "the owner, narrowed" — it is a different account.</strong>
 * The agent's own roles and permissions decide, on their own, and the owner's set is not consulted at
 * all. It used to be {@code actingFor(agent, owner)}, which made the engine intersect the two; that cap
 * is gone deliberately. An agent restricted to a set its owner does not hold is now a thing an
 * installation can say — a client trusted with one destructive action nobody else has, or a service
 * account whose owner is merely who set it up.
 *
 * <p>⚠️ <strong>Which means the two authorities are answers to different questions, not two points on
 * one scale.</strong> {@code INHERITED} is <em>be this person</em>: the owner's roles, the owner's
 * permissions, the owner's tool switches, followed live and going stale nowhere. {@code RESTRICTED} is
 * <em>be yourself</em>: whatever was granted to the agent, and nothing implicit. Reading the second as
 * "the first with fewer things" is the mistake this note exists to prevent — an agent granted nothing is
 * not a slightly limited owner, it is an account that holds nothing.
 *
 * <p>⚠️ <strong>Records follow the same split.</strong> Under {@code INHERITED} something the agent
 * creates is the owner's, because the owner is who is acting. Under {@code RESTRICTED} it is the
 * agent's — which is right where an agent is a row in the people table with a name and a face, and is
 * the reason such a row is retired rather than deleted: what it authored has to keep resolving.
 *
 * <p>⚠️ <strong>Nothing here checks whether the agent may act.</strong> Reading a row as a caller and
 * deciding it is allowed to be one are different questions, and a factory that quietly refused would put
 * a security decision somewhere nobody looks for one. {@link AgentAdmission} is that decision, asked
 * explicitly, before this.
 *
 * <p>⚠️ <strong>The human names are not knowable here.</strong> {@link Agent#ownerReference()} is opaque
 * by design, so {@link CallerAttributes#CALLER_NAME} and {@link CallerAttributes#SUBJECT_NAME} are left
 * for a product's resolver to add through {@link CallerIdentity#with(Map)} — it is the only party that
 * can turn that reference into something a human reads.
 */
public final class AgentCallers {

    private AgentCallers() {
    }

    /** An agent acting under whichever authority it was given. */
    public static CallerIdentity of(Agent agent) {
        return of(agent, null);
    }

    /**
     * The same, naming the client this call came in through.
     *
     * <p>Worth the two extra attributes wherever calls arrive over a connection: they are what lets a log
     * line about a misbehaving caller name the exact thing somebody can end, rather than an agent with
     * four clients and no clue which one to revoke.
     *
     * @param through the connection, or null where there is none — an in-application assistant has no
     *                connection, and saying so beats inventing one
     */
    public static CallerIdentity of(Agent agent, AgentConnection through) {
        Map<String, String> attributes = new LinkedHashMap<>();

        attributes.put(CallerAttributes.AGENT_ID,   agent.id());
        attributes.put(CallerAttributes.AGENT_NAME, agent.name());

        if (through != null) {
            attributes.put(CallerAttributes.CLIENT_NAME,   through.clientName());
            attributes.put(CallerAttributes.CONNECTION_ID, through.id());
        }

        return identityOf(agent).with(attributes);
    }

    private static CallerIdentity identityOf(Agent agent) {
        return switch (agent.authority()) {
            case INHERITED  -> CallerIdentity.of(agent.ownerReference());
            case RESTRICTED -> CallerIdentity.of(agent.id());
        };
    }
}
