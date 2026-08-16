package org.jmouse.ai.spring;

import org.jmouse.ai.administration.ProviderAdministration;
import org.jmouse.ai.agent.AgentConnections;
import org.jmouse.ai.agent.AgentDirectory;
import org.jmouse.ai.agent.AgentGrants;
import org.jmouse.ai.agent.AgentOwners;
import org.jmouse.ai.management.AgentAdministrationController;
import org.jmouse.ai.management.AgentSelfController;
import org.jmouse.ai.management.OverviewController;
import org.jmouse.ai.management.ProviderAdministrationController;
import org.jmouse.ai.management.ProviderController;
import org.jmouse.ai.management.ToolCallHistoryController;
import org.jmouse.ai.management.ToolCatalogController;
import org.jmouse.ai.management.UsageController;
import org.jmouse.ai.view.ProviderRegistry;
import org.jmouse.ai.view.ToolCallHistory;
import org.jmouse.ai.view.ToolCatalogView;
import org.jmouse.ai.view.UsageTotals;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * The optional screens, mounted only when both conditions hold.
 *
 * <p><strong>Present <em>and</em> switched on.</strong> The module being on the classpath is not enough,
 * and the extra property is not ceremony: these endpoints publish a call history and a provider
 * configuration, and ⚠️ <strong>nothing in that module guards them</strong> — the product mounts them
 * behind its own authorization, because a library cannot know what that is. Switching them on by the
 * mere presence of a jar would publish both to anyone who could reach the application.
 *
 * <p>The controllers are registered as beans here rather than component-scanned, which is what lets the
 * module be on the classpath and contribute nothing. A {@code @ComponentScan} over somebody else's
 * package is also how a starter quietly acquires an opinion about a product's bean naming.
 *
 * <p>Registered last of everything here, so that the ports these read are whatever the satellites
 * actually contributed rather than the empty defaults.
 */
@AutoConfiguration(after = AiAutoConfiguration.class)
@ConditionalOnClass(ToolCatalogController.class)
@ConditionalOnProperty(name = "jmouse.ai.management.enabled", havingValue = "true")
@EnableConfigurationProperties(AiProperties.class)
public class AiManagementAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ToolCatalogController aiToolCatalogController(ToolCatalogView tools) {
        return new ToolCatalogController(tools);
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolCallHistoryController aiToolCallHistoryController(
            ToolCallHistory history, ToolCatalogView tools) {

        return new ToolCallHistoryController(history, tools);
    }

    @Bean
    @ConditionalOnMissingBean
    public UsageController aiUsageController(UsageTotals usage) {
        return new UsageController(usage);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProviderController aiProviderController(ProviderRegistry providers) {
        return new ProviderController(providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public OverviewController aiOverviewController(
            ProviderRegistry providers, ToolCatalogView tools, ToolCallHistory history) {

        return new OverviewController(providers, tools, history);
    }

    /**
     * The one that writes, and only where there is something to write.
     *
     * <p>⚠️ {@code @ConditionalOnBean} rather than a default of
     * {@link ProviderAdministration#unavailable()}: an application whose settings come from a property
     * should have no write routes at all, rather than routes that answer a refusal. The unavailable form
     * is for an application that mounts the controller deliberately and wants the screen to say why it
     * can change nothing.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ProviderAdministration.class)
    public ProviderAdministrationController aiProviderAdministrationController(
            ProviderAdministration configurations) {

        return new ProviderAdministrationController(configurations);
    }

    /**
     * Every agent in the installation, and the clients holding a credential for one.
     *
     * <p>⚠️ {@code @ConditionalOnBean} on both ports, and on both rather than either: the screen shows
     * an agent <em>with</em> its connections, and half of that is a screen offering a revoke button with
     * no reason to press one.
     *
     * <p>An application that stores agents its own way supplies its own {@link AgentDirectory} and gets
     * the same screen over it — which is the entire reason that is a port.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({AgentDirectory.class, AgentConnections.class})
    public AgentAdministrationController aiAgentAdministrationController(
            AgentDirectory agents, AgentConnections connections, ObjectProvider<AgentGrants> grants) {

        // ⚠️ Optional, and the unavailable form is what a product without an access engine gets: every
        // read answers empty and every write refuses with a sentence saying so. A screen that explains
        // itself beats one that appears to save something going nowhere.
        return new AgentAdministrationController(
                agents, connections, grants.getIfAvailable(AgentGrants::unavailable));
    }

    /**
     * The same screen, narrowed to whoever is looking at it.
     *
     * <p>⚠️ <strong>Registered whether or not a product supplies {@link AgentOwners}, and the fallback
     * refuses everything.</strong> The alternative — leaving the routes off — is a personal screen that
     * 404s in one product and works in the other, which is the divergence this module exists to remove.
     * A refusal carrying a sentence is a screen that explains itself; a missing route is not.
     *
     * <p>⚠️ It is <strong>mounted behind a different permission</strong> from the administration
     * controller, and a product that forgets to say so has put an ordinary person's screen behind an
     * administrator's gate — where it works for nobody. See this module's {@code package-info}.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({AgentDirectory.class, AgentConnections.class})
    public AgentSelfController aiAgentSelfController(
            AgentDirectory agents,
            AgentConnections connections,
            ObjectProvider<AgentGrants> grants,
            ObjectProvider<AgentOwners> owners) {

        return new AgentSelfController(
                agents,
                connections,
                grants.getIfAvailable(AgentGrants::unavailable),
                owners.getIfAvailable(AgentOwners::nobody));
    }
}
