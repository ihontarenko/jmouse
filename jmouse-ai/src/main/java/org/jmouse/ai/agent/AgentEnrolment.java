package org.jmouse.ai.agent;

import java.util.Optional;

/**
 * Which agent a connecting client becomes — found where it has been here before, made where it has not.
 *
 * <h2>⚠️ The one place two products may not disagree</h2>
 *
 * <p>Connecting a client is a <strong>one-step act</strong>: a person approves a screen and the client
 * works. It stops being one the moment a product answers <em>"you have no agent yet"</em> with a refusal,
 * because the person is then sent off to a settings screen to create something whose purpose they have to
 * infer, and the state in between reads as a broken connection rather than as a deliberate one. That
 * happened — one product created the persona on first sight and the other refused with <em>"there is
 * nothing here for a client to act as"</em>, from the same shared screen, on the same day.
 *
 * <p>So the decision is here, once, and both products call it. What a product still owns is <em>who</em>
 * the owner is and what the credential minted for the agent looks like; what it no longer owns is whether
 * connecting works at all for somebody doing it the first time.
 *
 * <h2>⚠️ Born {@link AgentAuthority#INHERITED} and enabled</h2>
 *
 * <p>The alternative — created restricted, and therefore able to do nothing until somebody grants it
 * something — is the same refusal wearing a different hat: the client connects, every call is denied, and
 * nothing on the screen says why. A person approving a client has just been told, in the screen's own
 * words, that it will act with exactly the permissions of whatever they approve it as. Narrowing it is
 * then an act somebody takes, on a switch that is one click away, rather than the default that silently
 * refuses everything.
 */
public final class AgentEnrolment {

    private AgentEnrolment() {
    }

    /**
     * The persona this client connects under, created on first sight.
     *
     * @param ownerReference whose it is — opaque, and never checked against anything here
     * @param clientName     what the client called itself, or {@link ClientNames#UNNAMED}
     * @param clientId       the registration it is using, which is the identity the name was being asked
     *                       to be and never was
     */
    public static Agent agentFor(
            AgentDirectory   agents,
            AgentConnections connections,
            String           ownerReference,
            String           clientName,
            String           clientId
    ) {
        return byRegistration(agents, connections, ownerReference, clientId)
                .or(() -> byName(agents, ownerReference, clientName))
                .orElseGet(() -> agents.register(new AgentDirectory.Draft(
                        ownerReference,
                        ClientNames.distinguish(clientName, clientId),
                        AgentAuthority.INHERITED,
                        true)));
    }

    /**
     * The registration this client used last time, if it has been here before.
     *
     * <h2>⚠️ The collapse this replaced</h2>
     *
     * <p>This used to be a name match and nothing else, on the reasonable-sounding basis that a client
     * calls itself the same thing every time. It does — <em>when the registry still remembers what it
     * called itself.</em> A client whose registration has been forgotten comes through as
     * {@link ClientNames#UNNAMED}, and every such client matches every other one: a laptop and a desktop
     * become <strong>one agent</strong>, with one permission set, one switch, and disconnecting either
     * taking the other's persona with it.
     *
     * <p>The identifier is issued by the server rather than claimed by the client, and a client reuses it
     * across reconnects — which is the identity the name was standing in for.
     *
     * <p>⚠️ <strong>Still checked against this owner.</strong> A registration is not a person: two people
     * can approve the same client installation, and each gets their own agent. Matching on the identifier
     * alone would hand the second one the first one's persona.
     */
    private static Optional<Agent> byRegistration(
            AgentDirectory agents, AgentConnections connections, String ownerReference, String clientId) {

        if (clientId == null || clientId.isBlank()) {
            return Optional.empty();
        }

        return connections.byClientId(clientId).stream()
                .map(connection -> agents.find(connection.agentId()))
                .flatMap(Optional::stream)
                .filter(candidate -> ownerReference.equals(candidate.ownerReference()))
                .findFirst();
    }

    /**
     * ⚠️ The fallback, for a connection older than the identifier column — and never for an unnamed one.
     * Two clients that both failed to name themselves are two clients, and the whole point of the match
     * above is that they stop being one.
     */
    private static Optional<Agent> byName(
            AgentDirectory agents, String ownerReference, String clientName) {

        if (!ClientNames.identifies(clientName)) {
            return Optional.empty();
        }

        return agents.ownedBy(ownerReference).stream()
                .filter(candidate -> candidate.name().equals(clientName))
                .findFirst();
    }
}
