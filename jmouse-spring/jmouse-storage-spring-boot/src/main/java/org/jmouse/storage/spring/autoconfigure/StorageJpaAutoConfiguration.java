package org.jmouse.storage.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileStores;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.jpa.JpaStoredFileRegistry;
import org.jmouse.storage.jpa.StoredFileIngestion;
import org.jmouse.storage.jpa.StoredFileReferences;
import org.jmouse.storage.jpa.StoredFileRegistry;
import org.jmouse.storage.jpa.sweeper.OrphanSweeper;
import org.jmouse.storage.key.StorageKeyStrategy;
import org.jmouse.storage.policy.UploadPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 🗃️ The registry, the write path and the sweeper — for an application that has a database.
 *
 * <p>Conditional on Jakarta Persistence being present and an {@link EntityManager} being available,
 * so the byte layer stays usable on its own. A product with no database gets backends, a planner
 * and a renderer, and simply never sees these beans.</p>
 *
 * <h3>One thing a product has to do</h3>
 *
 * <p>The registry entity lives outside the product's own package, so the default entity scan will
 * not find it. Widen the scan once:</p>
 *
 * <pre>{@code
 * @EntityScan({"net.innoventa", "org.jmouse.storage.jpa"})
 * }</pre>
 *
 * <p>Done reflectively here it would depend on a Boot internal that moved package between major
 * versions; one visible annotation in the product is both more honest and more stable.</p>
 */
@AutoConfiguration(after = StorageAutoConfiguration.class)
@ConditionalOnClass({EntityManager.class, StoredFileRegistry.class})
public class StorageJpaAutoConfiguration {

    /**
     * 📇 The registry, over the application's own persistence context.
     *
     * <p>The shared {@link EntityManager} proxy Spring exposes, so registry work joins whatever
     * transaction the caller is already in — which is the whole point of the library not
     * demarcating any of its own.</p>
     *
     * @param entityManager the application's persistence context
     * @param fileStore     the default backend, whose name is recorded against what it writes
     * @return the registry
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(EntityManager.class)
    public StoredFileRegistry storedFileRegistry(EntityManager entityManager, FileStore fileStore) {
        return new JpaStoredFileRegistry(entityManager, fileStore);
    }

    /**
     * 📥 The write path: judge, place, store, record.
     *
     * @param fileStores   every backend the application has
     * @param registry     where written objects are recorded
     * @param keyStrategy  where content is laid out
     * @param uploadPolicy what may enter storage
     * @return the ingestion path
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StoredFileRegistry.class)
    public StoredFileIngestion storedFileIngestion(FileStores fileStores, StoredFileRegistry registry,
                                                   StorageKeyStrategy keyStrategy,
                                                   UploadPolicy uploadPolicy) {
        return new StoredFileIngestion(fileStores, registry, keyStrategy, uploadPolicy);
    }

    /**
     * 🧹 The sweeper, over whatever reference sources the product declared.
     *
     * <p>An application that declares none gets a sweeper whose reference union is empty — which
     * would make every object an orphan. That is why the sweeper ships disabled and an operator
     * has to turn it on: enabling it is also the moment to check that the sources exist.</p>
     *
     * @param registry         registry to sweep
     * @param fileStore        default backend, whose bytes are reclaimed
     * @param referenceSources one per table pointing at the registry
     * @param settings         whether the sweeper runs, and how long an object is left alone
     * @return the sweeper
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StoredFileRegistry.class)
    public OrphanSweeper orphanSweeper(StoredFileRegistry registry, FileStore fileStore,
                                       List<StoredFileReferences> referenceSources,
                                       StorageSettings settings) {
        return new OrphanSweeper(registry, fileStore, referenceSources, settings.sweeper());
    }
}
