package org.jmouse.files.management.autoconfigure;

import jakarta.persistence.EntityManager;
import org.jmouse.files.jpa.FileBindings;
import org.jmouse.files.jpa.ManagedFile;
import org.jmouse.files.jpa.ManagedFiles;
import org.jmouse.files.jpa.directory.StorageDirectories;
import org.jmouse.files.management.DirectoryController;
import org.jmouse.files.management.DirectoryManagement;
import org.jmouse.files.management.FileController;
import org.jmouse.files.management.FileManagement;
import org.jmouse.files.management.FileManagementContext;
import org.jmouse.files.management.RemoteFileFetcher;
import org.jmouse.files.management.UploadAllowance;
import org.jmouse.storage.FileStores;
import org.jmouse.storage.administration.StorageAdministration;
import org.jmouse.storage.administration.StorageAdministrationController;
import org.jmouse.storage.administration.StorageAdministrationDiagnostics;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.policy.UploadPolicy;
import org.jmouse.storage.jpa.StoredFileDelivery;
import org.jmouse.storage.jpa.StoredFileIngestion;
import org.jmouse.storage.jpa.StoredFileReferences;
import org.jmouse.storage.jpa.StoredFileRegistry;
import org.jmouse.storage.jpa.sweeper.OrphanSweeper;
import org.jmouse.storage.spring.DeliveryRenderer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 📁 Managed files, their filing, their tree — and, if a product asks for them, their endpoints.
 *
 * <h3>⚠️ The endpoints are OFF by default, and the reason is not caution</h3>
 *
 * <p>These controllers carry no {@code @RequiresAccess}: they are gated from outside through
 * {@code ExternalAccessRules}, because a library cannot know a product's permissions or its scope
 * names. That works — but it means the routes are only guarded once the product has declared rules for
 * them, and a dependency that published a file API the moment it landed on the classpath would put the
 * unguarded window somewhere nobody was looking.</p>
 *
 * <p>So the controller needs {@code jmouse.files.management.endpoints.enabled: true}, and turning it on
 * is meant to be the same change that declares the access rules. The services below are unconditional —
 * a product using this library from its own controllers is the ordinary case.</p>
 */
@AutoConfiguration
@ConditionalOnClass({EntityManager.class, ManagedFile.class})
public class FilesManagementAutoConfiguration {

    /** Property a product sets to publish the library's own file routes. */
    public static final String ENDPOINTS_ENABLED = "jmouse.files.management.endpoints.enabled";

    /** Property naming the one namespace uploads land under, where a product declares no context. */
    public static final String DEFAULT_NAMESPACE = "jmouse.files.management.namespace";

    /**
     * Property a product turns OFF to keep the tree routes out while keeping the file ones.
     *
     * <p>⚠️ Defaults to on, because a product that switched the endpoints on asked for this module and
     * the tree is most of it. It exists for the other case: Tessera files an attachment against an issue
     * and has no folders at all, so publishing a tree API there would advertise a surface with no product
     * behind it — and, worse, one whose {@code StorageDirectory} targets nothing in that product can
     * resolve, so every call refuses in a way that reads as a broken route rather than an absent feature.</p>
     */
    public static final String DIRECTORY_ENDPOINTS_ENABLED = "jmouse.files.management.directories.enabled";

    /** Property a product sets to allow fetching files from web addresses. */
    public static final String IMPORT_ENABLED = "jmouse.files.management.import.enabled";

    /** Property naming what this installation announces itself as when fetching. */
    public static final String IMPORT_USER_AGENT = "jmouse.files.management.import.user-agent";

    /** Property a product sets to publish the storage administration surface. */
    public static final String ADMINISTRATION_ENABLED = "jmouse.storage.administration.enabled";

    /**
     * 🆔 Where a new file's or directory's identifier comes from.
     *
     * <p>A random UUID unless the product says otherwise — several here mint identifiers with a prefix
     * so that a row's kind is readable in a log line, and declaring a bean of this type takes it over.</p>
     *
     * @return the identifier supplier
     */
    @Bean("filesIdentifierSupplier")
    @ConditionalOnMissingBean(name = "filesIdentifierSupplier")
    public Supplier<String> filesIdentifierSupplier() {
        return () -> UUID.randomUUID().toString();
    }

    /**
     * 🔗 Filing, unfiling and re-filing.
     *
     * @param entityManager the application's persistence context
     * @return the bindings
     */
    @Bean
    @ConditionalOnMissingBean
    public FileBindings fileBindings(EntityManager entityManager) {
        return new FileBindings(entityManager);
    }

    /**
     * 📄 The file rows.
     *
     * @param entityManager the application's persistence context
     * @param bindings      filing, so removing a file takes its places with it
     * @return the files
     */
    @Bean
    @ConditionalOnMissingBean
    public ManagedFiles managedFiles(EntityManager entityManager, FileBindings bindings) {
        return new ManagedFiles(entityManager, bindings);
    }

    /**
     * 🌳 The directory tree.
     *
     * @param entityManager the application's persistence context
     * @param identifiers   where a new directory's identifier comes from
     * @return the tree
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageDirectories storageDirectories(
            EntityManager entityManager,
            @Qualifier("filesIdentifierSupplier") Supplier<String> identifiers) {
        return new StorageDirectories(entityManager, identifiers);
    }

    /**
     * 🧭 Where an upload goes and who is making it — answered by the server.
     *
     * <p>⚠️ A product with more than one kind of file, or any notion of who owns one, declares its own
     * and this steps aside. See {@link FileManagementContext} for why neither answer may come off the
     * request.</p>
     *
     * @param namespace the single namespace this installation files everything under
     * @return the context
     */
    @Bean
    @ConditionalOnMissingBean
    public FileManagementContext fileManagementContext(
            @Value("${" + DEFAULT_NAMESPACE + ":files}") String namespace) {
        return new ConfiguredFileManagementContext(namespace);
    }

    /**
     * 🌐 How a file is fetched from a web address.
     *
     * <p>⚠️ Only where the product asked for it. Importing makes this server fetch things on a caller's
     * behalf, which is a capability rather than a convenience — a product that never wanted it should not
     * acquire it by adding a dependency.</p>
     *
     * @param uploadPolicy what this installation accepts
     * @param userAgent    what to announce this installation as
     * @return the fetcher
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = IMPORT_ENABLED, havingValue = "true")
    public RemoteFileFetcher remoteFileFetcher(
            UploadPolicy uploadPolicy,
            @Value("${" + IMPORT_USER_AGENT + ":jMouse-Storage/1.0}") String userAgent) {
        return new RemoteFileFetcher(uploadPolicy, userAgent);
    }

    /**
     * 📁 What a file endpoint actually does.
     *
     * @param ingestion   the write path into storage
     * @param delivery    the read path out of it
     * @param files       the file rows
     * @param bindings    where files are filed
     * @param identifiers where a new file's identifier comes from
     * @return the file surface
     */
    @Bean
    @ConditionalOnMissingBean
    public FileManagement fileManagement(StoredFileIngestion ingestion, StoredFileDelivery delivery,
                                         ManagedFiles files, FileBindings bindings,
                                         @Qualifier("filesIdentifierSupplier") Supplier<String> identifiers,
                                         ApplicationEventPublisher events, FileStores fileStores,
                                         ObjectProvider<UploadAllowance> allowance,
                                         ObjectProvider<RemoteFileFetcher> fetcher) {
        return new FileManagement(ingestion, delivery, files, bindings, identifiers, events, fileStores,
                                  allowance.getIfAvailable(), fetcher.getIfAvailable());
    }

    /**
     * 🌐 The routes — only where a product asked for them.
     *
     * @param management what the routes do
     * @param renderer   turns a delivery plan into a response
     * @param context    where an upload goes and who is making it
     * @return the controller
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ENDPOINTS_ENABLED, havingValue = "true")
    public FileController fileController(FileManagement management, DeliveryRenderer renderer,
                                         FileManagementContext context) {
        return new FileController(management, renderer, context);
    }

    /**
     * 🌳 The tree's routes — where a product asked for them AND has a tree.
     *
     * <p>⚠️ Behind the file routes' switch, for the same reason they are: they carry no authorization of
     * their own, so publishing them before a product has declared rules would open an unguarded tree API
     * the moment the dependency landed. And behind {@link #DIRECTORY_ENDPOINTS_ENABLED} as well, so a
     * product taking the file surface without the tree can say so.</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = ENDPOINTS_ENABLED, havingValue = "true")
    public static class DirectoryEndpoints {

        /**
         * 🌳 What the tree's routes do, and where their transaction begins.
         *
         * <p>⚠️ **The boundary lives here rather than on the controller or on the tree**, for the reason
         * {@link FileManagement} already gives for itself. {@code StorageDirectories} renumbers in bulk
         * statements and a bulk statement without an active transaction throws rather than fails
         * quietly — so before this bean existed, every write route answered 500 while every read worked.
         * See {@link DirectoryManagement}.</p>
         *
         * @param directories the tree
         * @return the tree's transactional surface
         */
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = DIRECTORY_ENDPOINTS_ENABLED, havingValue = "true",
                               matchIfMissing = true)
        public DirectoryManagement directoryManagement(StorageDirectories directories) {
            return new DirectoryManagement(directories);
        }

        /**
         * 🌳 The tree's routes.
         *
         * @param directories the tree's transactional surface
         * @return the controller
         */
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(name = DIRECTORY_ENDPOINTS_ENABLED, havingValue = "true",
                               matchIfMissing = true)
        public DirectoryController directoryController(DirectoryManagement directories) {
            return new DirectoryController(directories);
        }
    }

    /**
     * 🗄️ What an operator can ask about storage.
     *
     * @param settings         what this installation is configured to do
     * @param fileStores       every backend it built
     * @param registry         what is stored
     * @param referenceSources who still points at it
     * @param sweeper          what reclaims the rest
     * @return the administration surface
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageAdministration storageAdministration(StorageSettings settings, FileStores fileStores,
                                                       StoredFileRegistry registry,
                                                       List<StoredFileReferences> referenceSources,
                                                       OrphanSweeper sweeper) {
        return new StorageAdministration(settings, fileStores, registry, referenceSources, sweeper);
    }

    /**
     * 🗄️ The administration routes — separately switched from the file ones.
     *
     * <p>⚠️ Reading the registry lists every stored object's key and name across the whole installation.
     * That is a disclosure surface of its own, and a product may well want file endpoints without it.</p>
     *
     * @param administration what the routes do
     * @return the controller
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ADMINISTRATION_ENABLED, havingValue = "true")
    public StorageAdministrationController storageAdministrationController(
            StorageAdministration administration) {
        return new StorageAdministrationController(administration);
    }

    /**
     * 🔎 Says where the administration surface is mounted, so a drifted address is visible.
     *
     * @return the diagnostics
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ADMINISTRATION_ENABLED, havingValue = "true")
    public StorageAdministrationDiagnostics storageAdministrationDiagnostics() {
        return new StorageAdministrationDiagnostics();
    }

    /**
     * 🚑 Filing refusals as RFC 7807 problem details.
     *
     * @return the advice
     */
    @Bean
    @ConditionalOnMissingBean
    public FilesProblemDetails filesProblemDetails() {
        return new FilesProblemDetails();
    }
}
