package org.jmouse.storage.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileStores;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.delivery.DeliveryPlanner;
import org.jmouse.storage.jpa.JpaStoredFileRegistry;
import org.jmouse.storage.jpa.StoredFileDelivery;
import org.jmouse.storage.jpa.StoredFileIngestion;
import org.jmouse.storage.jpa.StoredFileReferenceDiscovery;
import org.jmouse.storage.jpa.StoredFileReferences;
import org.jmouse.storage.jpa.StoredFileRegistry;
import org.jmouse.storage.jpa.sweeper.OrphanSweeper;
import org.jmouse.storage.key.StorageKeyStrategy;
import org.jmouse.storage.policy.UploadPolicyResolver;
import org.jmouse.storage.spring.ScheduledOrphanSweep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageJpaAutoConfiguration.class);

    /**
     * 📇 The registry, over the application's own persistence context.
     *
     * <p>The shared {@link EntityManager} proxy Spring exposes, so registry work joins whatever
     * transaction the caller is already in — which is the whole point of the library not
     * demarcating any of its own.</p>
     *
     * @param entityManager the application's persistence context
     * @return the registry
     */
    @Bean
    @ConditionalOnMissingBean
    public StoredFileRegistry storedFileRegistry(EntityManager entityManager) {
        return new JpaStoredFileRegistry(entityManager);
    }

    /**
     * 📥 The write path: judge, place, store, record.
     *
     * @param fileStores     every backend the application has
     * @param registry       where written objects are recorded
     * @param keyStrategy    where content is laid out
     * @param uploadPolicies what may enter storage, per destination
     * @return the ingestion path
     */
    @Bean
    @ConditionalOnMissingBean
    public StoredFileIngestion storedFileIngestion(FileStores fileStores, StoredFileRegistry registry,
                                                   StorageKeyStrategy keyStrategy,
                                                   UploadPolicyResolver uploadPolicies) {
        return new StoredFileIngestion(fileStores, registry, keyStrategy, uploadPolicies);
    }

    /**
     * 📤 The read path: a registry row, planned for delivery.
     *
     * <p>Sits here rather than beside the planner because it takes a registry row, and the registry
     * is what this configuration is conditional on. A product with no database plans deliveries
     * straight through {@link DeliveryPlanner} as before.</p>
     *
     * @param deliveryPlanner decides between streaming and a presigned redirect
     * @return the read path
     */
    @Bean
    @ConditionalOnMissingBean
    public StoredFileDelivery storedFileDelivery(DeliveryPlanner deliveryPlanner) {
        return new StoredFileDelivery(deliveryPlanner);
    }

    /**
     * 🧹 The sweeper, over every reference source — the ones the mappings imply and the ones the
     * product declared.
     *
     * <p>⚠️ <strong>Discovered first, declared second, both kept.</strong>
     * {@link StoredFileReferenceDiscovery} finds every mapped association pointing at the registry,
     * which is what stops a new kind of file being invisible to the sweeper because somebody forgot a
     * bean. A product's own {@link StoredFileReferences} beans are added on top rather than replaced:
     * a collection, a bare identifier column or a query spanning several tables is not a singular
     * association and cannot be discovered.</p>
     *
     * <p>Overlap between the two is harmless — the sweeper unions identifiers, so an association that
     * is both discovered and declared by hand simply appears twice in the report.</p>
     *
     * @param registry         registry to sweep
     * @param fileStores       every backend, so each orphan is reclaimed through the one holding it
     * @param referenceSources what the product declared, on top of what the mappings imply
     * @param entityManager    persistence context whose metamodel is asked
     * @param settings         whether the sweeper runs, and how long an object is left alone
     * @return the sweeper
     */
    @Bean
    @ConditionalOnMissingBean
    public OrphanSweeper orphanSweeper(StoredFileRegistry registry, FileStores fileStores,
                                       List<StoredFileReferences> referenceSources,
                                       EntityManager entityManager, StorageSettings settings) {
        List<StoredFileReferences> sources =
            new ArrayList<>(StoredFileReferenceDiscovery.discover(entityManager));

        sources.addAll(referenceSources);

        LOGGER.info("🔎 Stored-file reference sources: {} discovered, {} declared — {}",
                    sources.size() - referenceSources.size(), referenceSources.size(),
                    sources.stream().map(StoredFileReferences::sourceName).toList());

        return new OrphanSweeper(registry, fileStores, sources, settings.sweeper());
    }

    /**
     * ⏰ The scheduled sweep, one transaction per batch.
     *
     * <p>Shipped rather than left to each product, because a product that binds through the registry
     * has already stopped deleting bytes when it deletes a row — so a product without a sweep leaks
     * every file it has ever deleted, and finding that out later is expensive. Scheduling still
     * needs {@code @EnableScheduling} in the application, and the sweeper itself still ships
     * disabled.</p>
     *
     * @param sweeper             the sweeper to drive
     * @param transactionTemplate what each batch runs inside
     * @return the scheduled sweep
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(TransactionTemplate.class)
    public ScheduledOrphanSweep scheduledOrphanSweep(OrphanSweeper sweeper,
                                                     TransactionTemplate transactionTemplate) {
        return new ScheduledOrphanSweep(sweeper, transactionTemplate);
    }
}
