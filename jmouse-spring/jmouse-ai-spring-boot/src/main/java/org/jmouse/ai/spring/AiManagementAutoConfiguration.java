package org.jmouse.ai.spring;

import org.jmouse.ai.management.ProviderController;
import org.jmouse.ai.management.ToolCallHistoryController;
import org.jmouse.ai.management.ToolCatalogController;
import org.jmouse.ai.management.UsageController;
import org.jmouse.ai.view.ProviderRegistry;
import org.jmouse.ai.view.ToolCallHistory;
import org.jmouse.ai.view.ToolCatalogView;
import org.jmouse.ai.view.UsageTotals;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
}
