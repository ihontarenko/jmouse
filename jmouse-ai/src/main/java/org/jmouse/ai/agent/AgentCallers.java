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
 *       <td>{@code actingFor(agent, owner)}</td>
 *       <td>the agent, capped by the owner in every scope</td></tr>
 * </table>
 *
 * <p>⚠️ <strong>The agent is named in the attributes either way, and that matters.</strong> Under
 * {@code INHERITED} the caller identifier is the <em>owner's</em>, so
 * {@link CallerAttributes#AGENT_ID} is the only place the agent survives — and provenance, a trail, and
 * the badge on a record all read it. Attributes are display and record-keeping, never an authorization
 * input, which is exactly the right register for "which of my agents did this".
 *
 * <p><strong>Records belong to the owner under both.</strong> Authorization rests on the caller and
 * ownership on the acting subject, so something an agent creates is the owner's — it appears in their
 * inventory and survives the agent being discarded. An agent owning rows would mean deleting one orphans
 * everything it was created to capture.
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
            case RESTRICTED -> CallerIdentity.actingFor(agent.id(), agent.ownerReference());
        };
    }
}
