package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.agent.Agent;
import org.jmouse.ai.agent.AgentConnections;
import org.jmouse.ai.agent.AgentDirectory;
import org.jmouse.ai.agent.AgentEnrolment;
import org.jmouse.ai.mcp.authorization.McpAuthorizationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns an approval into a credential, for a product that has nothing unusual to say about it.
 *
 * <h2>⚠️ Three products wrote this class and none of them put anything of their own in it</h2>
 *
 * <p>Identity's, Tessera's and WiQ's were 66, 60 and 77 lines, and every line was library types calling
 * library types: {@link ApprovingSubject} to tell an enrolment apart from a returning agent, {@link
 * AgentEnrolment} to mint the first one, {@link AgentCredentials} to do the actual work, and a record
 * copy to cross the interface. Nothing in any of them knew which product it was in.
 *
 * <p>So it is here, and a product that genuinely differs still implements {@link CredentialIssuer}
 * directly — Innoventa does, because it mints through its own authentication service rather than through
 * an agent's connection, and that is a real difference rather than an accidental one.
 *
 * <h2>The one decision it makes</h2>
 *
 * <p>An approval's {@code subjectReference} is either <em>a person enrolling a new client</em> or <em>an
 * agent that already exists</em>, and {@link ApprovingSubject} is what tells those apart. The first mints
 * an agent for that person; the second must find one, and a reference naming neither is a refusal rather
 * than a silently created agent belonging to nobody.
 */
public class StandardCredentialIssuer implements CredentialIssuer {

    private final AgentCredentials   credentials;
    private final AgentDirectory     agents;
    private final AgentConnections   connections;

    public StandardCredentialIssuer(
            AgentCredentials credentials, AgentDirectory agents, AgentConnections connections) {

        this.credentials = credentials;
        this.agents      = agents;
        this.connections = connections;
    }

    @Override
    @Transactional
    public IssuedCredential issue(ApprovedAuthorization approval) {
        Agent agent = approvedAgent(approval);

        return asIssued(credentials.issueFor(agent, approval.clientName(), approval.clientId()));
    }

    @Override
    public IssuedCredential renew(String refreshToken) {
        return asIssued(credentials.renew(refreshToken));
    }

    private Agent approvedAgent(ApprovedAuthorization approval) {
        return ApprovingSubject.ownerOfNewSubject(approval.subjectReference())
                .map(ownerReference -> AgentEnrolment.agentFor(
                        agents, connections, ownerReference,
                        approval.clientName(), approval.clientId()))
                .orElseGet(() -> agents.find(approval.subjectReference()).orElseThrow(() ->
                        new McpAuthorizationException(
                                "The agent this code was approved for no longer exists.")));
    }

    private static IssuedCredential asIssued(AgentCredentials.IssuedCredential credential) {
        return new IssuedCredential(
                credential.accessToken(), credential.refreshToken(), credential.expiresIn());
    }
}
