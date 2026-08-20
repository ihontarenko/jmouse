package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.mcp.authorization.AuthorizationCodeStore;
import org.jmouse.ai.mcp.authorization.LoopbackRedirectPolicy;
import org.jmouse.ai.mcp.authorization.ProofKeyPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
                properties.getAllowedRedirectHosts(),
                properties.getAllowedRedirectPaths(),
                properties.isAllowNestedRedirectPaths());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationCodeStore mcpAuthorizationCodeStore() {
        return new InMemoryAuthorizationCodeStore();
    }

    /**
     * What a client called itself — durable wherever this library's tables exist.
     *
     * <h2>⚠️ The default changed, and the old one was losing names on every restart</h2>
     *
     * <p>This used to be {@link InMemoryClientNameRegistry} unconditionally, on the reasoning that a
     * display name is a claim nobody verified and losing one costs a client nothing. True about the
     * client; wrong about everything downstream. The name is <strong>copied onto the connection row</strong>
     * at approval, and in at least one product it <strong>becomes the agent's name</strong> — the thing a
     * person then grants permissions to and switches on and off. So a restart between registration and
     * approval baked <em>An unnamed client</em> into a durable record, permanently, and two clients that
     * both landed there became one agent in a product that matches by name.
     *
     * <p>⚠️ The in-memory form is still the fallback and is still correct for an application without
     * these tables — it degrades a label rather than refusing a connection. What changed is which one
     * you get by default when there is somewhere better to put it.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.jmouse.ai.jpa.entity.AiClientRegistration")
    @ConditionalOnBean(EntityManagerFactory.class)
    public ClientNameRegistry mcpClientNameRegistry(
            EntityManagerFactory entityManagerFactory, McpAuthorizationProperties properties) {

        return new JpaClientNameRegistry(
                entityManagerFactory, properties.getClientRegistrationLifetime());
    }

    /** For an application with no tables of this library's: a label that a restart forgets. */
    @Bean
    @ConditionalOnMissingBean
    public ClientNameRegistry mcpInMemoryClientNameRegistry() {
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
