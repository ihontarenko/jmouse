package org.jmouse.cad.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.cad.jpa.catalogue.CategoryStore;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/**
 * 🗃️ The jmCAD stores, over the application's own persistence unit.
 *
 * <p>Each works over a shared {@link EntityManager} proxy, so store work joins whatever transaction the caller is
 * already in — the library demarcates none of its own. Every bean backs off when the application declares its own.</p>
 *
 * <h3>⚠️ The proxy is made here, from the factory, and not injected</h3>
 *
 * <p>An injectable {@code EntityManager} is a bean Spring Data registers, not the persistence provider, and it is not
 * resolvable from a servlet-context bean built while the server starts — the live socket. The factory is always
 * there, and {@link SharedEntityManagerCreator} is what that bean is made of anyway.</p>
 *
 * <h3>One thing a product has to do</h3>
 *
 * <p>The entities live outside the product's package, so the default entity scan will not find them. Widen it
 * once:</p>
 *
 * <pre>{@code
 * @EntityScan({"org.example.product", "org.jmouse.cad.jpa"})
 * }</pre>
 */
@AutoConfiguration
@ConditionalOnClass({EntityManagerFactory.class, SharedEntityManagerCreator.class, DocumentStore.class})
public class CadJpaAutoConfiguration {

    /**
     * 📚 The documents and every revision of their texts.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the store
     */
    @Bean
    @ConditionalOnMissingBean
    public DocumentStore cadDocumentStore(EntityManagerFactory entityManagerFactory) {
        return new DocumentStore(sharedEntityManager(entityManagerFactory));
    }

    /**
     * 🗂️ The catalogue's sections.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the store
     */
    @Bean
    @ConditionalOnMissingBean
    public CategoryStore cadCategoryStore(EntityManagerFactory entityManagerFactory) {
        return new CategoryStore(sharedEntityManager(entityManagerFactory));
    }

    private static EntityManager sharedEntityManager(EntityManagerFactory entityManagerFactory) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }
}
