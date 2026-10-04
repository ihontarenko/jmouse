package org.jmouse.script.spring.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.script.ScriptDocumentBinder;
import org.jmouse.script.ScriptTemplate;
import org.jmouse.script.jpa.ScriptDocumentStore;
import org.jmouse.script.jpa.StoredScriptDocuments;
import org.jmouse.script.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules an installation keeps, in the two tables this library owns.
 *
 * <h2>⚠️ SEPARATE FROM {@code ScriptAutoConfiguration}, BECAUSE A STORE IS OPTIONAL</h2>
 *
 * <p>A product may declare stages and facades and hold its rules somewhere this library has never heard
 * of. The mechanism works without a store — {@code ScriptDocuments.none()} means nothing runs, which is
 * exactly how a pipeline behaves before anybody has written a rule. So everything here is conditional
 * on {@code jmouse-script-jpa} being on the classpath and JPA being configured.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@AutoConfiguration
@ConditionalOnClass({ScriptDocumentStore.class, EntityManagerFactory.class})
@ConditionalOnBean(EntityManagerFactory.class)
public class ScriptStoreAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptStoreAutoConfiguration.class);

    /**
     * ⚠️ A SHARED entity manager, not the factory and not a plain one.
     *
     * <p>Boot registers an {@link EntityManagerFactory} and never an {@link EntityManager}, so a
     * library class asking for one would fail to wire in every application that has JPA. The shared
     * proxy is what makes {@code @Transactional} on the caller mean what it looks like it means: each
     * call reaches the manager bound to the current transaction.</p>
     */
    @Bean
    @ConditionalOnMissingBean(name = "scriptEntityManager")
    public EntityManager scriptEntityManager(EntityManagerFactory factory) {
        return SharedEntityManagerCreator.createSharedEntityManager(factory);
    }

    /**
     * Every stored rule, bound and indexed by the moment it runs at.
     *
     * <p>⚠️ Nothing rebuilds it here — see {@link #loadRules}. The rebuild writes, so it needs a
     * transaction, and it must run once the application is up rather than while it is being
     * assembled.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public StoredScriptDocuments storedScriptDocuments(EntityManager scriptEntityManager,
                                                       ScriptDocumentBinder binder, StageRegistry stages) {
        return new StoredScriptDocuments(scriptEntityManager, binder, stages);
    }

    /**
     * Writing, assigning and removing rules.
     *
     * <p>⚠️ Not transactional and cannot be — it is a library object. Whatever a product wraps around
     * it draws the transaction boundary, which is also where that decision belongs.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public ScriptDocumentStore scriptDocumentStore(EntityManager scriptEntityManager,
                                                   ScriptDocumentBinder binder, ScriptTemplate template) {
        return new ScriptDocumentStore(scriptEntityManager, binder, template);
    }

    /**
     * Binds every stored rule once the application is up.
     *
     * <h2>⚠️ {@code @Transactional} BECAUSE THE REBUILD WRITES</h2>
     *
     * <p>A rule whose text no longer binds has its state recorded, so a screen can say why. The library
     * method cannot carry the annotation — it is in a module that has never heard of Spring — so the
     * transaction is declared here, at the one call that happens before any request.</p>
     *
     * <p>⚠️ A failure must not stop the application. Rules are something somebody added to a product
     * that works without them, and refusing to start because one of them no longer binds would turn an
     * editing mistake into an outage.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public ScriptStartup scriptStartup(StoredScriptDocuments documents) {
        return new ScriptStartup(documents);
    }

    /** Loads the rules once, at startup, inside a transaction. */
    public static class ScriptStartup {

        private final StoredScriptDocuments documents;

        public ScriptStartup(StoredScriptDocuments documents) {
            this.documents = documents;
        }

        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void loadRules() {
            try {
                documents.reload();
            } catch (RuntimeException exception) {
                LOGGER.error("jMS rules could not be loaded — the pipeline runs with none", exception);
            }
        }

    }

}
