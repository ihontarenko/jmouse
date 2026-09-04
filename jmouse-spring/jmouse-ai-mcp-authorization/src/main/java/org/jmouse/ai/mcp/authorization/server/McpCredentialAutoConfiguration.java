package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.agent.AgentConnections;
import org.jmouse.ai.agent.AgentDirectory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtEncoder;

/**
 * Wires the credential half of the protocol, for a product that supplies the two things only it knows.
 *
 * <h2>⚠️ Separate from {@link McpAuthorizationAutoConfiguration}, and it has to be</h2>
 *
 * <p>That one is {@code @ConditionalOnBean(CredentialIssuer.class)} — it configures the flow around an
 * issuer the product has already provided. This one <em>provides</em> the issuer, so putting the two
 * together would make the condition depend on its own output. Kept apart, the ordering is the ordinary
 * one: a product contributes {@link CredentialOwners} and a signing encoder, this fills in the rest, and
 * the flow configuration then sees an issuer and activates.
 *
 * <h2>What a product still owes</h2>
 *
 * <ul>
 *   <li>a {@link CredentialOwners} bean — who {@code Agent.ownerReference()} points at
 *   <li>a {@link JwtEncoder} bean named <strong>{@code mcpTokenEncoder}</strong>, over a secret only this
 *       product holds. ⚠️ By name rather than by type on purpose: a product may have several encoders,
 *       and the one that signs a protocol credential must never be picked by whichever happened to be
 *       the only candidate that day.
 * </ul>
 *
 * <p>Every bean here is {@link ConditionalOnMissingBean}, so a product that genuinely differs replaces
 * one without opting out of the rest — Innoventa contributes its own {@link ProtocolTokenMinter} because
 * it mints through its own authentication service, and still gets the enrolment, the refusals, the
 * rotation and the revocation from here.
 *
 * <h2>⚠️ Ordered on both sides, and the first attempt had only one</h2>
 *
 * <p>{@code afterName} is what makes the condition below answerable at all: {@link AgentDirectory} and
 * {@link AgentConnections} are contributed by another autoconfiguration, and {@link ConditionalOnBean}
 * sees only what has been registered by the time it runs. Without the ordering it evaluated first, found
 * no directory, and <strong>quietly contributed nothing</strong> — which surfaced as a product failing to
 * start on a missing {@link ProtocolCredentials} bean, pointing at the controller that wanted it rather than
 * at the condition that declined to make it.
 *
 * <p>By <strong>name</strong> rather than by class because this module must not depend on
 * {@code jmouse-ai-spring-boot}: that one already knows about management and providers, so a dependency
 * back would be a cycle between the protocol and the starter that wires it.
 */
@AutoConfiguration(
        afterName = "org.jmouse.ai.spring.AiJpaAutoConfiguration",
        before = McpAuthorizationAutoConfiguration.class)
@EnableConfigurationProperties(McpAuthorizationProperties.class)
@ConditionalOnBean({AgentDirectory.class, AgentConnections.class})
public class McpCredentialAutoConfiguration {

    /**
     * The default minter — a token this product signs itself.
     *
     * <p>⚠️ {@link ConditionalOnMissingBean}, which is how Innoventa opts out: it mints an ordinary
     * product JWT confined by a gate on the route, contributes its own {@link ProtocolTokenMinter}, and
     * still gets everything else here. That is also why the audience check lives inside
     * {@link SignedTokenMinter} rather than on the credential service — a product with its own minter has
     * no audience of ours to state, and failing its startup over an unused property would be our bug.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(name = "mcpTokenEncoder")
    public ProtocolTokenMinter protocolTokenMinter(
            CredentialOwners owners,
            @Qualifier("mcpTokenEncoder") JwtEncoder encoder,
            McpAuthorizationProperties properties) {

        return new SignedTokenMinter(owners, encoder, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProtocolCredentials protocolCredentials(
            AgentDirectory agents,
            AgentConnections connections,
            ProtocolTokenMinter minter,
            McpAuthorizationProperties properties) {

        return new ProtocolCredentials(agents, connections, minter, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CredentialIssuer credentialIssuer(
            ProtocolCredentials credentials, AgentDirectory agents, AgentConnections connections) {

        return new StandardCredentialIssuer(credentials, agents, connections);
    }
}
