package org.jmouse.parallax.spring.autoconfigure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.parallax.jpa.catalogue.CategoryStore;
import org.jmouse.parallax.jpa.catalogue.InstrumentStore;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.jpa.collection.CollectionStore;
import org.jmouse.parallax.jpa.computation.DatasetCache;
import org.jmouse.parallax.jpa.computation.JobQueue;
import org.jmouse.parallax.jpa.evaluation.EvaluationLedger;
import org.jmouse.parallax.jpa.scene.ComparisonStore;
import org.jmouse.parallax.jpa.scene.SceneStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/**
 * 🗃️ The Parallax stores, over the application's own persistence unit.
 *
 * <p>Each works over a shared {@link EntityManager} proxy, so store work joins whatever transaction the caller is
 * already in — the library demarcates none of its own. Every bean backs off when the application declares its own.</p>
 *
 * <h3>⚠️ The proxy is made here, from the factory, and not injected</h3>
 *
 * <p>An injectable {@code EntityManager} is a bean Spring Data registers, not the persistence provider, and it was
 * not resolvable when Parallax's live socket — a servlet-context bean, built while Tomcat starts — reached the
 * catalogue and through it these stores. The factory is always there, and {@link SharedEntityManagerCreator} is what
 * that bean is made of anyway: the behaviour is the same, and nothing depends on when that bean appears.</p>
 *
 * <h3>One thing a product has to do</h3>
 *
 * <p>The entities live outside the product's package, so the default entity scan will not find them. Widen it
 * once:</p>
 *
 * <pre>{@code
 * @EntityScan({"org.example.product", "org.jmouse.parallax.jpa"})
 * }</pre>
 *
 * <p>Done here it would depend on a Boot internal that moved package between major versions; one visible
 * annotation in the product is both more honest and more stable — the files library asks the same.</p>
 */
@AutoConfiguration
@ConditionalOnClass({EntityManagerFactory.class, SharedEntityManagerCreator.class, ObjectStore.class})
public class ParallaxJpaAutoConfiguration {

    /**
     * 🧮 The objects and their texts.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the store
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectStore parallaxObjectStore(EntityManagerFactory entityManagerFactory) {
        return new ObjectStore(sharedEntityManager(entityManagerFactory));
    }

    /**
     * 📚 The catalogue's sections.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the store
     */
    @Bean
    @ConditionalOnMissingBean
    public CategoryStore parallaxCategoryStore(EntityManagerFactory entityManagerFactory) {
        return new CategoryStore(sharedEntityManager(entityManagerFactory));
    }

    /**
     * 🔭 The instruments.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the store
     */
    @Bean
    @ConditionalOnMissingBean
    public InstrumentStore parallaxInstrumentStore(EntityManagerFactory entityManagerFactory) {
        return new InstrumentStore(sharedEntityManager(entityManagerFactory));
    }

    /**
     * 🎬 Scenes, revisions, snapshots.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the store
     */
    @Bean
    @ConditionalOnMissingBean
    public SceneStore parallaxSceneStore(EntityManagerFactory entityManagerFactory) {
        return new SceneStore(sharedEntityManager(entityManagerFactory));
    }

    /**
     * 🪟 Comparisons and their slots.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the store
     */
    @Bean
    @ConditionalOnMissingBean
    public ComparisonStore parallaxComparisonStore(EntityManagerFactory entityManagerFactory) {
        return new ComparisonStore(sharedEntityManager(entityManagerFactory));
    }

    /**
     * 🗂️ Collections and their items.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the store
     */
    @Bean
    @ConditionalOnMissingBean
    public CollectionStore parallaxCollectionStore(EntityManagerFactory entityManagerFactory) {
        return new CollectionStore(sharedEntityManager(entityManagerFactory));
    }

    /**
     * 💾 The computed datasets.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the cache
     */
    @Bean
    @ConditionalOnMissingBean
    public DatasetCache parallaxDatasetCache(EntityManagerFactory entityManagerFactory) {
        return new DatasetCache(sharedEntityManager(entityManagerFactory));
    }

    /**
     * ⏳ The job queue.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the queue
     */
    @Bean
    @ConditionalOnMissingBean
    public JobQueue parallaxJobQueue(EntityManagerFactory entityManagerFactory) {
        return new JobQueue(sharedEntityManager(entityManagerFactory));
    }

    /**
     * 📒 The evaluation runs and their results.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the ledger
     */
    @Bean
    @ConditionalOnMissingBean
    public EvaluationLedger parallaxEvaluationLedger(EntityManagerFactory entityManagerFactory) {
        return new EvaluationLedger(sharedEntityManager(entityManagerFactory));
    }

    /**
     * 🔗 A proxy that joins the caller's transaction, or works on its own outside one.
     *
     * @param entityManagerFactory the application's persistence unit
     * @return the proxy
     */
    private static EntityManager sharedEntityManager(EntityManagerFactory entityManagerFactory) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }
}
