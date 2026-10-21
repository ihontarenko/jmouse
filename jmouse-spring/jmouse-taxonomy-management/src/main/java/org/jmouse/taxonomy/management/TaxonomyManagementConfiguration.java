package org.jmouse.taxonomy.management;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 🔌 The taxonomy REST API. Imported by a product, never auto-configured or component-scanned — the routes appear
 * because somebody typed {@code @Import}, and the same product gates them with {@link TaxonomyAccessRules}, which
 * refuses to build while any handler here is left without a rule. The product's security chain lets
 * {@code /jmouse/taxonomy/**} through; these routes decide for themselves.
 */
@Configuration(proxyBeanMethods = false)
public class TaxonomyManagementConfiguration {

    @Bean
    public TaxonomyController taxonomyController(TaxonomyManagement management) {
        return new TaxonomyController(management);
    }

    @Bean
    public TaxonomyProblemDetails taxonomyProblemDetails() {
        return new TaxonomyProblemDetails();
    }
}
