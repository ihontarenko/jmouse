package org.jmouse.cad.spring.autoconfigure;

import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.jpa.catalogue.CategoryStore;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.store.about.EngineAbout;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.jmouse.cad.store.parts.CataloguePartSearch;
import org.jmouse.cad.store.parts.PartSearch;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.jmouse.cad.store.plugin.CadEnginePlugin;
import org.jmouse.cad.store.simulation.AnalysisEngines;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.spring.PluginAutoConfiguration;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.editing.LanguageEditing;
import org.jmouse.cad.store.library.PartLibrary;
import org.jmouse.cad.store.library.ShippedLibrary;
import org.jmouse.cad.store.activity.CadActivity;
import org.jmouse.cad.store.activity.LiveActivity;
import org.jmouse.cad.store.simulation.CatalogueChecks;
import org.jmouse.cad.store.simulation.SimulationJobs;
import org.jmouse.cad.store.simulation.Simulations;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.time.Clock;

/**
 * 📚 jmCAD's application services over its stores: reading documents, deciding who may see them, the catalogue, and
 * the shipped library imported when the application is ready.
 *
 * <p>⚠️ The host provides a {@link CadAuthority} and a {@link CadPlaces} — who is acting, and what an owner is. Nothing
 * here backs off without them: a catalogue that silently was not there would be found at the first request, far from
 * the cause, so the context refuses to start instead.</p>
 */
@AutoConfiguration(after = {CadJpaAutoConfiguration.class, PluginAutoConfiguration.class})
@EnableConfigurationProperties(CadProperties.class)
public class CadStoreAutoConfiguration {

    /** The engine as the first plugin: the points it reads, and the language's own modules. */
    @Bean
    public CadEnginePlugin cadEnginePlugin() {
        return new CadEnginePlugin();
    }

    /** The jM language of every module the plugins brought, the engine's first. */
    @Bean
    @ConditionalOnMissingBean
    public CadLanguage cadLanguage(Extensions extensions) {
        return CadLanguage.of(extensions.contributions(DocumentModule.POINT));
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentExchange cadDocumentExchange(Extensions extensions, CadCatalogue catalogue, DocumentStore documents,
                                                CadAuthority authority, CadPlaces places, CadLanguage language) {
        return new DocumentExchange(extensions, catalogue, documents, authority, places, language);
    }

    @Bean
    @ConditionalOnMissingBean
    public EngineAbout cadEngineAbout(Extensions extensions) {
        return new EngineAbout(extensions);
    }

    @Bean
    @ConditionalOnMissingBean
    public CataloguePartSearch cadCataloguePartSearch(CadCatalogue catalogue) {
        return new CataloguePartSearch(catalogue);
    }

    /** Parts looked for in the catalogue, then in every place a plugin brought. */
    @Bean
    @ConditionalOnMissingBean
    public PartSearch cadPartSearch(CataloguePartSearch catalogueSearch, Extensions extensions, CadCatalogue catalogue,
                                    DocumentStore documents, CadAuthority authority, CadPlaces places,
                                    PlatformTransactionManager transactions, CadProperties properties, CadLanguage language) {
        return new PartSearch(catalogueSearch, extensions, catalogue, documents, authority, places,
                              new TransactionTemplate(transactions), properties.parts().limit(), language);
    }

    @Bean
    @ConditionalOnMissingBean
    public AnalysisEngines cadAnalysisEngines(Extensions extensions, CadCatalogue catalogue, CadAuthority authority,
                                              CadPlaces places, CadLanguage language, CadProperties properties) {
        return new AnalysisEngines(extensions, catalogue, authority, places, language, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentReading cadDocumentReading(CadProperties properties, CadLanguage language) {
        return new DocumentReading(properties, language);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentAccess cadDocumentAccess(DocumentStore documents, CadAuthority authority, CadPlaces places) {
        return new DocumentAccess(documents, authority, places);
    }

    @Bean
    @ConditionalOnMissingBean
    public CadCatalogue cadCatalogue(DocumentStore documents, CategoryStore categories, DocumentAccess access,
                                     DocumentReading reading, CadAuthority authority, CadPlaces places,
                                     CadProperties properties) {
        return new CadCatalogue(documents, categories, access, reading, authority, places, properties, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public PartLibrary cadPartLibrary(DocumentStore documents, DocumentAccess access, CadCatalogue catalogue,
                                      CadAuthority authority, CadPlaces places) {
        return new PartLibrary(documents, access, catalogue, authority, places);
    }

    @Bean
    @ConditionalOnMissingBean
    public LanguageEditing cadLanguageEditing(DocumentReading reading) {
        return new LanguageEditing(reading);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public CatalogueChecks cadCatalogueChecks(Simulations simulations, CadAuthority authority) {
        return new CatalogueChecks(simulations, authority, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public CadActivity cadActivity(SimulationJobs jobs, CadAuthority authority, ObjectProvider<LiveActivity> live) {
        return new CadActivity(jobs, authority, live);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public SimulationJobs cadSimulationJobs(CadProperties properties) {
        return new SimulationJobs(properties, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public Simulations cadSimulations(CadCatalogue catalogue, DocumentAccess access, DocumentStore documents,
                                      DocumentReading reading, CadAuthority authority, CadPlaces places,
                                      SimulationJobs jobs, CadProperties properties) {
        return new Simulations(catalogue, access, documents, reading, authority, places, jobs, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ShippedLibrary cadShippedLibrary(CadCatalogue catalogue, ResourcePatternResolver resources,
                                            CadProperties properties, CadLanguage language) {
        return new ShippedLibrary(catalogue, resources, properties, language);
    }

    /**
     * 🚀 The shipped library, written into the catalogue once the application is ready — so a product's own start-up
     * work, its categories among it, is done first.
     */
    @Bean
    @ConditionalOnProperty(name = "jmouse.cad.library.import-on-start", havingValue = "true", matchIfMissing = true)
    public ApplicationListener<ApplicationReadyEvent> cadShippedLibraryImport(ShippedLibrary library) {
        return ready -> library.apply();
    }
}
