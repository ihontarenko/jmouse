package org.jmouse.taxonomy.management.autoconfigure;

import jakarta.persistence.EntityManager;
import org.jmouse.taxonomy.EntityType;
import org.jmouse.taxonomy.EntityTypes;
import org.jmouse.taxonomy.jpa.Taxonomies;
import org.jmouse.taxonomy.jpa.Taxonomy;
import org.jmouse.taxonomy.management.TaxonomyManagement;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 🗂️ Taxonomies, as services: the entity types the host registered, the store over its persistence context, and the
 * transactional management the routes and the host call. The routes themselves are imported, not configured here
 * ({@code TaxonomyManagementConfiguration}).
 *
 * <p>⚠️ The host's entity scan must include {@code org.jmouse.taxonomy.jpa}; a product listing its packages by hand
 * does not see these entities otherwise.</p>
 */
@AutoConfiguration
@ConditionalOnClass({EntityManager.class, Taxonomy.class})
public class TaxonomyAutoConfiguration {

    /**
     * 📚 Every {@link EntityType} bean the host declares — the only names an assignment may use.
     */
    @Bean
    @ConditionalOnMissingBean
    public EntityTypes taxonomyEntityTypes(ObjectProvider<EntityType> types) {
        return EntityTypes.of(types.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public Taxonomies taxonomies(EntityManager entityManager, EntityTypes entityTypes) {
        Supplier<String> identifiers = () -> UUID.randomUUID().toString();
        return new Taxonomies(entityManager, identifiers, entityTypes);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaxonomyManagement taxonomyManagement(Taxonomies taxonomies) {
        return new TaxonomyManagement(taxonomies);
    }
}
