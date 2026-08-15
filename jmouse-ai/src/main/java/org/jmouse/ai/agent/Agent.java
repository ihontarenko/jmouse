package org.jmouse.ai.agent;

import java.time.Instant;

/**
 * A named thing that acts for somebody — the persona a person grants privileges to and switches off.
 *
 * <p><strong>Not a credential, and the difference is the whole reason this type exists.</strong> A
 * credential is what one client holds; an agent is what a person <em>decided</em>. Products that had
 * only the credential ended up with an identity that appeared when a client connected and vanished when
 * it disconnected, so there was nothing to attach a permission to, nothing to disable, and nothing for a
 * record to point at afterwards. One agent has many {@link AgentConnection}s; revoking one of those ends
 * a client, and disabling the agent ends all of them.
 *
 * <p><strong>Its privileges are not here, deliberately.</strong> What an agent may do is a question for
 * whatever authorization engine the product already runs, against the same policy as everything else —
 * duplicating a permission model here would produce a second answer that drifts from the first. What
 * this library supplies is the <em>subject</em> those privileges hang off, and {@link AgentAuthority} is
 * which subject that turns out to be. {@link AgentCallers} is the one line that says so.
 *
 * @param id             this agent's own identifier — what a permission is granted to, and what a
 *                       record created through it points at
 * @param ownerReference whoever it acts for, in the product's own vocabulary. ⚠️ <strong>Opaque, and
 *                       carrying no foreign key.</strong> One product puts an account identifier here
 *                       and another a membership row's; a library table cannot reference either without
 *                       knowing what an account is. The cost is that nothing cascades — see
 *                       {@link AgentDirectory#discardAllOwnedBy(String)}, which exists because of it
 * @param name           what a person called it, and what a screen and a provenance badge print
 * @param authority      whose permissions it acts with — its owner's, or its own capped by its owner's.
 *                       ⚠️ Two different questions from {@link #enabled}, and worth not confusing:
 *                       this is <em>how much</em>, that is <em>at all</em>
 * @param enabled        whether it may act at all. A disabled agent keeps its connections and its
 *                       privileges, and does nothing with either — which is what makes switching one off
 *                       a reversible decision rather than a deletion
 * @param lastActiveAt   when it last actually did something, or null if it never has. ⚠️ Refusals are
 *                       not activity: an agent being hammered and refused would otherwise look exactly
 *                       like an agent somebody is using
 */
public record Agent(
        String         id,
        String         ownerReference,
        String         name,
        AgentAuthority authority,
        boolean        enabled,
        Instant        createdAt,
        Instant        lastActiveAt
) {

    /** Whether it has ever run anything — how an owner spots the one they created and forgot. */
    public boolean hasEverActed() {
        return lastActiveAt != null;
    }
}
