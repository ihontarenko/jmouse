package org.jmouse.ai.spring;

import jakarta.persistence.EntityManagerFactory;
import org.jmouse.ai.agent.AgentConnections;
import org.jmouse.ai.agent.AgentDirectory;
import org.jmouse.ai.guard.GuardSettings;
import org.jmouse.ai.administration.ProviderAdministration;
import org.jmouse.ai.jpa.JpaAgentConnections;
import org.jmouse.ai.jpa.JpaAgentDirectory;
import org.jmouse.ai.jpa.JpaCallCounter;
import org.jmouse.ai.jpa.JpaConfirmationStore;
import org.jmouse.ai.jpa.JpaProviderAdministration;
import org.jmouse.ai.jpa.JpaProviderSettingsSource;
import org.jmouse.ai.provider.ProviderSettingsSource;
import org.jmouse.ai.spi.ConfirmationStore;
import org.jmouse.ai.spi.InvocationTrace;
import org.jmouse.ai.view.UsageTotals;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * What is worth surviving a restart.
 *
 * <p>Counters, so a management screen has something to show and a runaway caller is visible. Pending
 * confirmations, so a preview issued by one instance can be redeemed against another — ⚠️ the in-memory
 * store is not merely non-durable, it is <em>per-instance</em>, and behind two application servers a
 * caller redeeming its own token has a coin's chance of being told the token does not exist. Provider
 * settings, so a key can be rotated without a deploy. And agents with the clients connected to them,
 * because an identity that exists only while a client is connected is one nothing can be granted to,
 * switched off, or pointed at afterwards.
 *
 * <p>Registered before everything else here, so that these win over the in-memory defaults rather than
 * losing to them: {@code @ConditionalOnMissingBean} is decided in registration order, and a store
 * contributed later is a store that is never used.
 *
 * <h2>⚠️ One thing a product must do, and one failure mode if it does not</h2>
 *
 * <p>The {@code ai_*} tables are mapped by {@code jmouse-ai-jpa}, following the rule the other jMouse
 * persistence modules already set: <strong>whoever owns the table owns the mapping</strong>. So a
 * consuming application must name {@code org.jmouse.ai.jpa.entity} in its {@code @EntityScan}. Hibernate
 * validates only what it was given, which means a missing package <em>starts cleanly</em> and dies on
 * the first query — a failure that looks like a bug in this library and is a line of configuration.
 */
@AutoConfiguration(before = {AiProviderAutoConfiguration.class, AiAutoConfiguration.class})
@ConditionalOnClass({JpaCallCounter.class, EntityManagerFactory.class})
@EnableConfigurationProperties(AiProperties.class)
public class AiJpaAutoConfiguration {

    /**
     * One bean answering as both the writing half of the trail and the reading half of the totals.
     *
     * <p>Declared as its own type rather than as {@link InvocationTrace}, so that Spring can inject it
     * for {@link UsageTotals} as well. Two beans over one table would be two connections counting and
     * reading the same rows for no reason.
     *
     * <p>⚠️ A product that also records tool calls into its own audit trail contributes an
     * {@link InvocationTrace} of its own, which replaces this one — the interface composes, so the way
     * to have both is a third implementation that calls both.
     */
    @Bean
    @ConditionalOnMissingBean({InvocationTrace.class, JpaCallCounter.class})
    public JpaCallCounter aiJpaCallCounter(EntityManagerFactory entityManagerFactory) {
        return new JpaCallCounter(entityManagerFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfirmationStore aiJpaConfirmationStore(
            EntityManagerFactory entityManagerFactory, GuardSettings settings) {

        return new JpaConfirmationStore(entityManagerFactory, settings.confirmationLifetime());
    }

    /**
     * Provider settings from {@code ai_provider_settings}, so a key can be rotated without a deploy.
     *
     * <p>Opt-in rather than automatic: having the module on the classpath means a product wants the
     * counters and the confirmations, which is not the same as wanting its provider configuration to
     * live in a table it now has to seed before anything works.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "jmouse.ai.provider.source", havingValue = "database")
    public ProviderSettingsSource aiJpaProviderSettingsSource(
            EntityManagerFactory entityManagerFactory, AiProperties properties) {

        return new JpaProviderSettingsSource(entityManagerFactory, properties.getApplication());
    }

    /**
     * The write half of the same rows, so the settings above can be administered rather than seeded by
     * hand.
     *
     * <p>Under the same condition as the read half, and for the same reason: rows are only worth
     * administering where they are what is read. An application whose settings come from configuration
     * gets no bean here, and {@code AiManagementAutoConfiguration} therefore mounts no write routes —
     * which is better than routes that answer a refusal.
     *
     * <p>⚠️ Every rule it keeps — exactly one row in force, a blank key meaning <em>keep the stored
     * one</em>, a refusal to delete what is in force — is a condition
     * {@link JpaProviderSettingsSource} above refuses to resolve without. They live beside it because
     * two products re-derived them before it existed, and one of them getting it wrong shows up as an
     * application that has stopped answering.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "jmouse.ai.provider.source", havingValue = "database")
    public ProviderAdministration aiJpaProviderAdministration(
            EntityManagerFactory entityManagerFactory, AiProperties properties) {

        return new JpaProviderAdministration(entityManagerFactory, properties.getApplication());
    }

    /**
     * Which agents exist, who owns them, and whether they may act.
     *
     * <p>Unconditional, unlike the provider pair above, and the asymmetry is intentional. Provider
     * settings in a table are an <em>alternative</em> to settings in a property file, so a product has to
     * ask for them; an agent has no alternative — the only other place to keep one is a table the product
     * writes itself, which is the duplication this port exists to end. A product that never creates one
     * pays for an unused bean and two empty tables.
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentDirectory aiJpaAgentDirectory(EntityManagerFactory entityManagerFactory) {
        return new JpaAgentDirectory(entityManagerFactory);
    }

    /**
     * The clients connected to those agents.
     *
     * <p>⚠️ Storage only. What a credential <em>is</em> — how it is signed, how long it lasts, what stops
     * it working anywhere else — stays with whoever mints it, because products disagree about all three
     * on purpose. This bean holds the half they agree about.
     */
    @Bean
    @ConditionalOnMissingBean
    public AgentConnections aiJpaAgentConnections(EntityManagerFactory entityManagerFactory) {
        return new JpaAgentConnections(entityManagerFactory);
    }
}
