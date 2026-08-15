package org.jmouse.ai.agent;

import org.jmouse.ai.CallerAttributes;
import org.jmouse.ai.CallerIdentity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An agent, read as a caller — the one line that turns a row into a ceiling.
 *
 * <p><strong>This is the whole of the privilege story, and it is deliberately this small.</strong> An
 * agent authorizes as <em>itself</em> and acts on behalf of its <em>owner</em>, which is precisely the
 * service-sub-account relationship an access engine already understands: its effective permissions are
 * its own, intersected with its owner's, in every scope. So there is no permission table here, no
 * intersection code here, and nothing to keep in step with the engine that decides everything else —
 * {@link CallerIdentity#actingFor(String, String)} says it, and the bridge to whatever engine is in use
 * reads it.
 *
 * <p>⚠️ <strong>Nothing here checks whether the agent may act.</strong> Reading a row as a caller and
 * deciding it is allowed to be one are different questions, and a factory that quietly refused would put
 * a security decision somewhere nobody looks for one. {@link AgentAdmission} is that decision, asked
 * explicitly, before this.
 *
 * <p>⚠️ <strong>The consequence, stated plainly: records belong to the owner.</strong> Authorization
 * rests on the caller and ownership on the acting subject, so something an agent creates is the owner's
 * — it appears in their inventory, and it survives the agent being deleted. That is the intended
 * behaviour and not a rounding error: an agent owning rows would mean discarding one orphans everything
 * it was created to capture. Which agent did it is provenance, recorded beside the record rather than
 * instead of its owner.
 *
 * <p>⚠️ <strong>The owner's display name is not knowable here.</strong> {@link Agent#ownerReference()}
 * is opaque by design, so {@link CallerAttributes#SUBJECT_NAME} is left for a product's resolver to add
 * through {@link CallerIdentity#with(Map)} — it is the only party that can turn that reference into
 * something a human reads.
 */
public final class AgentCallers {

    private AgentCallers() {
    }

    /** An agent acting for its owner. */
    public static CallerIdentity of(Agent agent) {
        return CallerIdentity
                .actingFor(agent.id(), agent.ownerReference())
                .with(Map.of(CallerAttributes.CALLER_NAME, agent.name()));
    }

    /**
     * The same, naming the client this call came in through.
     *
     * <p>Worth the two extra attributes wherever calls arrive over a connection: they are what lets a log
     * line about a misbehaving caller name the exact thing somebody can end.
     */
    public static CallerIdentity of(Agent agent, AgentConnection through) {
        Map<String, String> attributes = new LinkedHashMap<>();

        attributes.put(CallerAttributes.CALLER_NAME,   agent.name());
        attributes.put(CallerAttributes.CLIENT_NAME,   through.clientName());
        attributes.put(CallerAttributes.CONNECTION_ID, through.id());

        return CallerIdentity.actingFor(agent.id(), agent.ownerReference()).with(attributes);
    }
}
