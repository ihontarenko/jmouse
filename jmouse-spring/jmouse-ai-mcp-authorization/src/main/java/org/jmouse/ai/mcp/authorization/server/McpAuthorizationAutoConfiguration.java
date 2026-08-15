package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.mcp.authorization.AuthorizationCodeStore;
import org.jmouse.ai.mcp.authorization.LoopbackRedirectPolicy;
import org.jmouse.ai.mcp.authorization.ProofKeyPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Switches the shared flow on, and only for a product that has said what minting means.
 *
 * <p>⚠️ <strong>{@link ConditionalOnBean} on {@link CredentialIssuer} is the whole switch</strong>, and
 * it is the right one: these endpoints are reachable without a credential by design, so a module that
 * mapped them the moment it appeared on a classpath would open three public routes on somebody who only
 * wanted the dependency for something else. A product that has written a {@code CredentialIssuer} has
 * decided, out loud, to host this.
 *
 * <p>Everything else here is a default a product may replace: the loopback allow-list and the proof-key
 * policy come from configuration, the code store forgets on restart, and the client registry is a map.
 * The two that are worth replacing are the store — an installation running more than one instance must —
 * and the registry, for anybody who already has somewhere durable to put a display name.
 */
@AutoConfiguration
@ConditionalOnBean(CredentialIssuer.class)
@EnableConfigurationProperties(McpAuthorizationProperties.class)
public class McpAuthorizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProofKeyPolicy mcpProofKeyPolicy() {
        return new ProofKeyPolicy();
    }

    @Bean
    @ConditionalOnMissingBean
    public LoopbackRedirectPolicy mcpRedirectPolicy(McpAuthorizationProperties properties) {
        return new LoopbackRedirectPolicy(
                properties.getAllowedRedirectHosts(), properties.getAllowedRedirectPaths());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationCodeStore mcpAuthorizationCodeStore() {
        return new InMemoryAuthorizationCodeStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientNameRegistry mcpClientNameRegistry() {
        return new InMemoryClientNameRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsentPage mcpConsentPage(McpAuthorizationProperties properties) {
        return new ConsentPage(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientAuthorizationFlow mcpClientAuthorizationFlow(
            McpAuthorizationProperties properties,
            LoopbackRedirectPolicy     redirectPolicy,
            ProofKeyPolicy             proofKeyPolicy,
            AuthorizationCodeStore     codeStore,
            ClientNameRegistry         clientRegistry,
            ApprovingSubject           approvingSubject,
            CredentialIssuer           credentialIssuer
    ) {
        return new ClientAuthorizationFlow(properties, redirectPolicy, proofKeyPolicy,
                codeStore, clientRegistry, approvingSubject, credentialIssuer);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpProtocolEndpoints mcpProtocolEndpoints(
            ClientAuthorizationFlow flow,
            ClientNameRegistry clientRegistry,
            McpAuthorizationProperties properties
    ) {
        return new McpProtocolEndpoints(flow, clientRegistry, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpConsentEndpoints mcpConsentEndpoints(ClientAuthorizationFlow flow, ConsentPage consentPage) {
        return new McpConsentEndpoints(flow, consentPage);
    }
}
