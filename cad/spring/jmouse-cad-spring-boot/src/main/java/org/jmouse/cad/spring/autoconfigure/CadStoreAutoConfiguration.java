package org.jmouse.cad.spring.autoconfigure;

import java.util.List;

import org.jmouse.cad.table.ShippedTable;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.table.TableSource;
import org.jmouse.cad.store.library.CatalogueTables;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.pcb.geometry.PourMemory;
import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.about.EngineAbout;
import org.jmouse.cad.store.activity.CadActivity;
import org.jmouse.cad.store.activity.LiveActivity;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadFolderLayout;
import org.jmouse.cad.store.authority.CadHostPlaces;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.catalogue.DocumentTexts;
import org.jmouse.cad.store.catalogue.PlaceSource;
import org.jmouse.cad.store.editing.LanguageEditing;
import org.jmouse.cad.store.exchange.DocumentExchange;
import org.jmouse.cad.store.exchange.RoutingBoards;
import org.jmouse.cad.store.project.CadProjects;
import org.jmouse.cad.store.settings.SettingsTree;
import org.jmouse.cad.store.licence.DocumentLicences;
import org.jmouse.licence.Licences;
import org.jmouse.cad.store.files.DocumentFiles;
import org.jmouse.cad.store.files.DocumentUnderlays;
import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.documentation.Documentation;
import org.jmouse.documentation.DocumentationSource;
import org.jmouse.documentation.markdown.MarkdownDocumentation;
import org.jmouse.formula.FormulaDocumentation;
import org.jmouse.cad.store.library.PartLibrary;
import org.jmouse.cad.store.library.ShippedDocuments;
import org.jmouse.cad.store.library.CatalogueIndexing;
import org.jmouse.git.local.CommitAuthor;
import org.jmouse.git.local.CommitAuthors;
import org.jmouse.git.local.JGitLocalGit;
import org.jmouse.cad.store.parts.PartSearch;
import org.jmouse.cad.store.plugin.CadEnginePlugin;
import org.jmouse.cad.store.plugin.EngineConfiguration;
import org.jmouse.cad.store.plugin.EnginePluginSettings;
import org.jmouse.cad.store.simulation.AnalysisEngines;
import org.jmouse.cad.store.simulation.CatalogueChecks;
import org.jmouse.cad.store.simulation.SimulationJobs;
import org.jmouse.cad.store.showcase.FeaturedDesigns;
import org.jmouse.cad.store.simulation.Simulations;
import org.jmouse.plugin.Extensions;
import org.jmouse.settings.SecretSealing;
import org.jmouse.settings.SectionReader;
import org.jmouse.settings.Settings;
import org.jmouse.settings.spring.SettingsAutoConfiguration;
import org.jmouse.cad.settings.CadSectionReader;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.store.settings.ReaderSettings;
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
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
 * <p>⚠️ The host provides a {@link CadAuthority} and a {@link CadHostPlaces} — who is acting, and what its own owners are;
 * the engine answers its projects itself ({@link CadPlaces}). Nothing
 * here backs off without them: a catalogue that silently was not there would be found at the first request, far from
 * the cause, so the context refuses to start instead.</p>
 */
@AutoConfiguration(after = {CadJpaAutoConfiguration.class, PluginAutoConfiguration.class, SettingsAutoConfiguration.class})
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
    public EngineConfiguration cadEngineRunningSettings(Settings settings) {
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
                                                DocumentLicences licences, CadSettings settings, CadStandards standards,
                                                CadProperties properties) {
        return new DocumentExchange(extensions, catalogue, documents, authority, places, language, files, licences, settings, standards,
                PourMemory.keeping(properties.documents().keptPours()));
    }

    /** How a section is written where it is kept: JSON, read strictly into its record. */
    @Bean
    @ConditionalOnMissingBean
    public SectionReader cadSectionReader() {
        return CadSectionReader.JSON;
    }

    /** The settings tree as every module reads it: the installation's values, each read when it is used. */
    @Bean
    @ConditionalOnMissingBean
    public CadSettings cadSettings(Settings settings) {
        return CadSettings.kept(settings);
    }

    /** What the interface keeps of how its reader works, the engine's for every host. */
    @Bean
    @ConditionalOnMissingBean
    public ReaderSettings cadReaderSettings(Settings settings, EngineConfiguration configuration, CadAuthority authority) {
        return new ReaderSettings(settings, configuration, authority);
    }

    @Bean
    @ConditionalOnMissingBean
    public SettingsTree cadSettingsTree(Extensions extensions, Settings settings, CadSettings values) {
        return new SettingsTree(extensions, settings, values);
    }

    /** Every section of the settings tree read once the plugins stand and their values are kept, before the port opens. */
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
    public EnginePluginSettings cadEnginePluginSettings(Extensions extensions, Settings settings, CadAuthority authority) {
        return new EnginePluginSettings(extensions, settings, authority);
    }

    /** Parts looked for beyond the library, in every place a plugin brought. */
    @Bean
    @ConditionalOnMissingBean
    public PartSearch cadPartSearch(Extensions extensions, CadCatalogue catalogue,
                                    DocumentStore documents, CadAuthority authority, CadPlaces places,
                                    PlatformTransactionManager transactions, EngineConfiguration configuration, CadLanguage language,
                                    DocumentFiles files, DocumentAccess access, DocumentReading reading) {
        return new PartSearch(extensions, catalogue, documents, authority, places,
                              new TransactionTemplate(transactions), configuration, language, files,
                              access, reading);
    }

    /** Once the engine is up — its plugins registered, their settings readable —, every place parts are looked for is made ready. */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> cadPartProvidersPrepared(PartSearch search) {
        return ready -> search.prepareProviders();
    }

    /** The files documents hold besides their text: files of the library folder beside each document's own. */
    @Bean
    @ConditionalOnMissingBean
    public DocumentFiles cadDocumentFiles(DocumentStore documents, CadAuthority authority) {
        return new DocumentFiles(documents, authority);
    }

    /** Where the engine's own pages of documentation are kept: one Markdown file for each window of the interface. */
    public static final String DOCUMENTATION_PAGES = "classpath:org/jmouse/cad/documentation/*.md";

    /** The engine's own pages of documentation, read from its resources as it starts. */
    @Bean
    public DocumentationSource cadDocumentationPages() {
        return MarkdownDocumentation.of(new CompositeResourceLoader().findResources(DOCUMENTATION_PAGES));
    }

    /** The formula language's reference, as jMF says it of itself: its notation, then every function and constant it knows. */
    @Bean
    public DocumentationSource cadFormulaLanguage() {
        return new FormulaDocumentation();
    }

    /** Every page of documentation the engine and its plugins bring, found by subject. */
    @Bean
    @ConditionalOnMissingBean
    public Documentation cadDocumentation(List<DocumentationSource> sources) {
        return new Documentation(sources);
    }

    /** The picture each drawing may be drawn over, kept with its document as two of its files. */
    @Bean
    @ConditionalOnMissingBean
    public DocumentUnderlays cadDocumentUnderlays(DocumentFiles files, DocumentAccess access) {
        return new DocumentUnderlays(files, access);
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

    /** Every owner as a place: the engine's projects from their documents, the host's owners through the host. */
    @Bean
    @ConditionalOnMissingBean
    public CadPlaces cadPlaces(CadHostPlaces host, DocumentStore documents) {
        return new CadPlaces(host, documents);
    }

    @Bean
    @ConditionalOnMissingBean
    public DocumentAccess cadDocumentAccess(DocumentStore documents, CadAuthority authority, CadPlaces places) {
        return new DocumentAccess(documents, authority, places);
    }

    /**
     * What seals the keys to services where they are kept: the installation's own secret, {@code jmouse.cad.secret-key}, base64 of
     * 32 bytes. Without it nothing is sealed, so no key is kept: a plugin then has what its deployment gave it. Losing it
     * loses what was kept: each key is then given again.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("!'${jmouse.cad.secret-key:}'.isBlank()")
    public SecretSealing cadSecretSealing(@Value("${jmouse.cad.secret-key}") String key) {
        SecretCipher cipher = new AesGcmSecretCipher(VaultKeyring.fromBase64(Map.of(SECRET_KEY_NAME, key), SECRET_KEY_NAME));
        return new SecretSealing() {
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

    /** The engine's tables: the catalogue's table documents by slug, a curator's revision where one is kept. */
    @Bean
    @ConditionalOnMissingBean
    public TableSource cadTables(ObjectProvider<CadCatalogue> catalogue) {
        return new CatalogueTables(catalogue::getObject);
    }

    /** The standards the library ships as tables — land patterns, the ways a pcb is made —, read through the engine's tables. */
    @Bean
    @ConditionalOnMissingBean
    public CadStandards cadStandards(TableSource tables) {
        return new CadStandards(tables);
    }

    /** The projects: readers' own, kept in the library folder, and those the libraries ship. */
    @Bean
    @ConditionalOnMissingBean
    public CadProjects cadProjects(DocumentStore documents, CadCatalogue catalogue, DocumentAccess access, DocumentReading reading,
                                   CadAuthority authority, CadPlaces places, CadProperties properties, PartLibrary library) {
        return new CadProjects(documents, catalogue, access, reading, authority, places, properties, library);
    }

    @Bean
    @ConditionalOnMissingBean
    public PartLibrary cadPartLibrary(DocumentStore documents, DocumentAccess access, CadCatalogue catalogue,
                                      CadAuthority authority, CadPlaces places, CadLanguage language,
                                      DocumentReading reading, CadStandards standards) {
        return new PartLibrary(documents, access, catalogue, authority, places, language, reading, standards);
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
                                               CadLanguage language, ObjectProvider<Extensions> extensions) {
        return new ShippedDocuments(resources, properties, language, () -> extensions.getObject().contributions(ShippedTable.POINT));
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
                                          ObjectProvider<CommitAuthors> authors, CadHostPlaces host, CadSettings settings) {
        CadProperties.Identity identity = properties.library().identity();
        return new DocumentStore(configuration::workingFolder, new CadFolderLayout(host, settings), new JGitLocalGit(), authors.getIfAvailable(() -> CommitAuthors.NONE),
                new CommitAuthor(identity.name(), identity.email()));
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
    public SmartInitializingSingleton cadCatalogueIndex(CatalogueIndexing indexing, Settings settings) {
        return () -> {
            indexing.index();
            // Another working folder is another library: the store opens on it and the catalogue is made from it again.
            settings.whenChanged(CadEnginePlugin.ID, EngineConfiguration.LIBRARY, indexing::index);
        };
    }
}
