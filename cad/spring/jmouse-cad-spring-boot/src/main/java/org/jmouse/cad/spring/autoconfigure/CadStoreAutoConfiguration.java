package org.jmouse.cad.spring.autoconfigure;

import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.about.EngineAbout;
import org.jmouse.cad.store.activity.CadActivity;
import org.jmouse.cad.store.activity.LiveActivity;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.catalogue.DocumentTexts;
import org.jmouse.cad.store.catalogue.PlaceSource;
import org.jmouse.cad.store.editing.LanguageEditing;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.jmouse.cad.store.exchange.RoutingBoards;
import org.jmouse.cad.store.settings.SettingsTree;
import org.jmouse.cad.store.licence.DocumentLicences;
import org.jmouse.licence.Licences;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.cad.store.library.PartLibrary;
import org.jmouse.cad.store.library.ShippedDocuments;
import org.jmouse.cad.store.library.CatalogueIndexing;
import org.jmouse.git.local.CommitAuthor;
import org.jmouse.git.local.CommitAuthors;
import org.jmouse.git.local.JGitLocalGit;
import org.jmouse.cad.store.parts.CataloguePartSearch;
import org.jmouse.cad.store.parts.PartSearch;
import org.jmouse.cad.store.plugin.CadEnginePlugin;
import org.jmouse.cad.store.plugin.EngineConfiguration;
import org.jmouse.cad.store.plugin.EnginePluginSettings;
import org.jmouse.cad.store.simulation.AnalysisEngines;
import org.jmouse.cad.store.simulation.CatalogueChecks;
import org.jmouse.cad.store.simulation.SimulationJobs;
import org.jmouse.cad.store.showcase.FeaturedDesigns;
import org.jmouse.cad.store.simulation.Simulations;
import org.jmouse.files.management.FileManagement;
import org.jmouse.files.management.FileManagementContext;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.PluginSecrets;
import org.jmouse.plugin.PluginSettings;
import org.jmouse.plugin.spring.PluginAutoConfiguration;
import org.jmouse.vault.seal.AesGcmSecretCipher;
import org.jmouse.vault.seal.SecretCipher;
import org.jmouse.vault.seal.VaultKeyring;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Map;

/**
 * 📚 jmCAD's application services over its stores: reading documents, deciding who may see them, the catalogue, and
 * the shipped library imported when the application is ready.
 *
 * <p>⚠️ The host provides a {@link CadAuthority} and a {@link CadPlaces} — who is acting, and what an owner is. Nothing
 * here backs off without them: a catalogue that silently was not there would be found at the first request, far from
 * the cause, so the context refuses to start instead.</p>
 */
// After the managed files, where an installation keeps them: a fetched model's files are filed through them.
@AutoConfiguration(after = {CadJpaAutoConfiguration.class, PluginAutoConfiguration.class},
                   afterName = "org.jmouse.files.management.autoconfigure.FilesManagementAutoConfiguration")
@EnableConfigurationProperties(CadProperties.class)
public class CadStoreAutoConfiguration {

    private static final String SECRET_KEY_NAME = "primary";

    /** The engine as the first plugin: the points it reads, and the language's own modules. */
    @Bean
    public CadEnginePlugin cadEnginePlugin(CadProperties properties) {
        return new CadEnginePlugin(properties);
    }

    /** What the engine itself is set to while it runs, kept with the plugins' settings. */
    @Bean
    @ConditionalOnMissingBean
    public EngineConfiguration cadEngineRunningSettings(PluginSettings settings) {
        return new EngineConfiguration(settings);
    }

    /** The jM language of every module the plugins brought, the engine's first. */
    @Bean
    @ConditionalOnMissingBean
    public CadLanguage cadLanguage(Extensions extensions) {
        return CadLanguage.of(extensions.contributions(DocumentModule.POINT));
    }

    /** The licences of borrowed works, as the licence library ships their terms, with the program's own library owing nothing. */
    @Bean
    @ConditionalOnMissingBean
    public DocumentLicences cadDocumentLicences() {
        return new DocumentLicences(Licences.standard(), CadLibrary.library().holder());
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentExchange cadDocumentExchange(Extensions extensions, CadCatalogue catalogue, DocumentStore documents,
                                                CadAuthority authority, CadPlaces places, CadLanguage language, DocumentFiles files,
                                                DocumentLicences licences) {
        return new DocumentExchange(extensions, catalogue, documents, authority, places, language, files, licences);
    }

    @Bean
    @ConditionalOnMissingBean
    public SettingsTree cadSettingsTree(Extensions extensions) {
        return new SettingsTree(extensions);
    }

    /** Every section of the settings tree read once the plugins stand, before the port opens. */
    @Bean
    public SmartInitializingSingleton cadSettingsRead(SettingsTree tree) {
        return tree::readEvery;
    }

    @Bean
    @ConditionalOnMissingBean
    public RoutingBoards cadRoutingBoards(DocumentExchange exchange, CadProperties properties) {
        return new RoutingBoards(exchange, properties.documents().routingBoards());
    }

    @Bean
    @ConditionalOnMissingBean
    public EngineAbout cadEngineAbout(Extensions extensions) {
        return new EngineAbout(extensions);
    }

    @Bean
    @ConditionalOnMissingBean
    public EnginePluginSettings cadEnginePluginSettings(Extensions extensions, PluginSettings settings, CadAuthority authority) {
        return new EnginePluginSettings(extensions, settings, authority);
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
                                    PlatformTransactionManager transactions, EngineConfiguration configuration, CadLanguage language,
                                    DocumentFiles files, DocumentAccess access) {
        return new PartSearch(catalogueSearch, extensions, catalogue, documents, authority, places,
                              new TransactionTemplate(transactions), configuration, language, files,
                              access);
    }

    /** The files documents hold besides their text: files of the library folder beside each document's own. */
    @Bean
    @ConditionalOnMissingBean
    public DocumentFiles cadDocumentFiles(DocumentStore documents, CadAuthority authority) {
        return new DocumentFiles(documents, authority);
    }

    @Bean
    @ConditionalOnMissingBean
    public AnalysisEngines cadAnalysisEngines(Extensions extensions, CadCatalogue catalogue, CadAuthority authority,
                                              CadPlaces places, CadLanguage language, EngineConfiguration configuration) {
        return new AnalysisEngines(extensions, catalogue, authority, places, language, configuration);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentReading cadDocumentReading(CadProperties properties, EngineConfiguration configuration, CadLanguage language) {
        return new DocumentReading(properties, configuration, language);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentAccess cadDocumentAccess(DocumentStore documents, CadAuthority authority, CadPlaces places) {
        return new DocumentAccess(documents, authority, places);
    }

    /**
     * What seals the plugins' keys where they are kept: the installation's own secret, {@code jmouse.cad.secret-key}, base64 of
     * 32 bytes. Without it nothing is sealed, so no key is kept: a plugin then has what its deployment gave it. Losing it
     * loses what was kept: each key is then given again.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("!'${jmouse.cad.secret-key:}'.isBlank()")
    public PluginSecrets cadPluginSecrets(@Value("${jmouse.cad.secret-key}") String key) {
        SecretCipher cipher = new AesGcmSecretCipher(VaultKeyring.fromBase64(Map.of(SECRET_KEY_NAME, key), SECRET_KEY_NAME));
        return new PluginSecrets() {
            @Override
            public String seal(String secret, String context) {
                return cipher.seal(secret, context);
            }

            @Override
            public String open(String sealed, String context) {
                return cipher.open(sealed, context);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public CadCatalogue cadCatalogue(DocumentStore documents, DocumentAccess access,
                                     DocumentReading reading, CadAuthority authority, CadPlaces places,
                                     EngineConfiguration configuration, DocumentFiles files, DocumentTexts texts,
                                     ObjectProvider<Extensions> extensions) {
        return new CadCatalogue(documents, access, reading, authority, places, configuration, Clock.systemUTC(),
                files, texts, () -> extensions.getObject().contributions(PlaceSource.POINT));
    }

    @Bean
    @ConditionalOnMissingBean
    public PartLibrary cadPartLibrary(DocumentStore documents, DocumentAccess access, CadCatalogue catalogue,
                                      CadAuthority authority, CadPlaces places, CadLanguage language,
                                      DocumentReading reading) {
        return new PartLibrary(documents, access, catalogue, authority, places, language, reading);
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
    public SimulationJobs cadSimulationJobs(CadProperties properties, EngineConfiguration configuration) {
        return new SimulationJobs(properties, configuration::jobRetention, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public Simulations cadSimulations(CadCatalogue catalogue, DocumentAccess access, DocumentStore documents,
                                      DocumentReading reading, CadAuthority authority, CadPlaces places,
                                      SimulationJobs jobs, EngineConfiguration configuration, FeaturedDesigns featured) {
        return new Simulations(catalogue, access, documents, reading, authority, places, jobs, configuration, featured);
    }

    /** What a featured design shows to anybody: the one rule every route that shows one asks. */
    @Bean
    @ConditionalOnMissingBean
    public FeaturedDesigns cadFeaturedDesigns(DocumentStore documents, CadCatalogue catalogue, PartLibrary library) {
        return new FeaturedDesigns(documents, catalogue, library);
    }

    /** The documents the installation ships, read in place from their files (ADR 0001). */
    @Bean
    @ConditionalOnMissingBean
    public ShippedDocuments cadShippedDocuments(ResourcePatternResolver resources, CadProperties properties,
                                               CadLanguage language) {
        return new ShippedDocuments(resources, properties, language);
    }

    /** The one way a catalogued document's text is read, wherever it is held. */
    @Bean
    @ConditionalOnMissingBean
    public DocumentTexts cadDocumentTexts(DocumentStore documents, ShippedDocuments shipped) {
        return new DocumentTexts(documents, shipped);
    }

    /**
     * 📚 The documents: the library folder's files and their commits, and the index read from them and from the shipped
     * files (ADR 0002). A commit a person makes is by the author the host names, when it names one.
     */
    @Bean
    @ConditionalOnMissingBean
    public DocumentStore cadDocumentStore(CadProperties properties, EngineConfiguration configuration,
                                          ObjectProvider<CommitAuthors> authors) {
        CadProperties.Identity identity = properties.library().identity();
        return new DocumentStore(properties.library().folder(), new JGitLocalGit(), authors.getIfAvailable(() -> CommitAuthors.NONE),
                new CommitAuthor(identity.name(), identity.email()), configuration::revisionZone);
    }

    @Bean
    @ConditionalOnMissingBean
    public CatalogueIndexing cadCatalogueIndexing(CadCatalogue catalogue, ShippedDocuments shipped, DocumentStore documents,
                                                  CadLanguage language) {
        return new CatalogueIndexing(catalogue, shipped, documents, language);
    }

    /**
     * 🚀 The catalogue's index, made on every start from the files behind it — once every bean exists, and before the web
     * server opens its port. A request is never answered from a catalogue half read: until it is whole, the server is not
     * there to ask. It was made once the application was ready until 2026-11-11, and for as long as indexing took every
     * read answered as if the documents did not exist.
     */
    @Bean
    public SmartInitializingSingleton cadCatalogueIndex(CatalogueIndexing indexing) {
        return indexing::index;
    }
}
