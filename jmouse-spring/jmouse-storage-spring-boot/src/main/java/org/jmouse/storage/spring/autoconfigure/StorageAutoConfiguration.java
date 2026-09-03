package org.jmouse.storage.spring.autoconfigure;

import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileStoreFactory;
import org.jmouse.storage.FileStores;
import org.jmouse.storage.StandardFileStores;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.delivery.DeliveryPlanner;
import org.jmouse.storage.key.ContentAddressedKeyStrategy;
import org.jmouse.storage.key.OwnerNamespacedKeyStrategy;
import org.jmouse.storage.key.StorageKeyStrategy;
import org.jmouse.storage.local.LocalFileStoreFactory;
import org.jmouse.storage.policy.FixedUploadPolicy;
import org.jmouse.storage.policy.UploadPolicy;
import org.jmouse.storage.policy.UploadPolicyResolver;
import org.jmouse.storage.resource.FileStoreResourceLoader;
import org.jmouse.storage.spring.DeliveryRenderer;
import org.jmouse.storage.spring.StorageSettingsBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.List;

/**
 * ⚙️ Everything a Spring Boot application needs to use jMouse storage: add the dependency, set a
 * few properties, get a working set of backends.
 *
 * <h3>Two rules every bean here follows</h3>
 *
 * <p><strong>Only the configured backends are built.</strong> Backends are constructed from
 * configuration through {@link FileStoreFactory}, so an object store nobody configured never
 * demands credentials it will never use — and its factory is only on the classpath if the product
 * asked for that module.</p>
 *
 * <p><strong>Every bean steps aside.</strong> All of them are conditional on the application not
 * already declaring one of the same type, so a product that wants its own key layout, its own
 * acceptance policy or its own planner declares a bean and this configuration goes quiet.</p>
 *
 * <h3>On Boot 4</h3>
 *
 * <p>Boot 4 split autoconfiguration into per-technology modules and a jar on the classpath no
 * longer brings its own Spring integration along for free. The registration mechanism itself is
 * unchanged — this class is listed in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} — and
 * the failure mode when it is wrong is silence rather than an error, so
 * {@link StorageDiagnostics} logs what was actually built at startup. If that line is missing, the
 * autoconfiguration did not run.</p>
 */
@AutoConfiguration
@ConditionalOnClass(FileStore.class)
public class StorageAutoConfiguration {

    /**
     * ⚙️ Settings bound out of the environment, under the prefix the product already publishes.
     *
     * <p>Validated here, at startup, so a backend that could not possibly work fails while the
     * context is coming up rather than on somebody's first upload.</p>
     *
     * @param environment the application environment
     * @return the active settings
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageSettings storageSettings(ConfigurableEnvironment environment) {
        StorageSettings settings = StorageSettingsBinder.bind(environment);

        settings.validate();

        return settings;
    }

    /**
     * 🏭 The local-disk factory, which every application has.
     *
     * @return the factory
     */
    @Bean
    @ConditionalOnMissingBean(LocalFileStoreFactory.class)
    public LocalFileStoreFactory localFileStoreFactory() {
        return new LocalFileStoreFactory();
    }

    /**
     * 🗂️ Every configured backend, held by name with one of them the default.
     *
     * <p>A set rather than a single store, because a product that adds an object store to a
     * local-disk deployment still has to read everything written before the move. Which backend
     * wrote an object is recorded against it, so reads route themselves.</p>
     *
     * @param settings  the active settings
     * @param factories every factory on the classpath
     * @return the backends
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public FileStores fileStores(StorageSettings settings, List<FileStoreFactory> factories) {
        return new StandardFileStores(settings, factories);
    }

    /**
     * 🎯 The default backend, for the many callers that never need to name one.
     *
     * <p>Not the owner of anything — {@link FileStores} closes the backends — so this is exposed
     * purely as a convenience for code that writes to wherever uploads normally go.</p>
     *
     * @param fileStores the backends
     * @return the default store
     */
    @Bean
    @ConditionalOnMissingBean
    public FileStore fileStore(FileStores fileStores) {
        return fileStores.defaultStore();
    }

    /**
     * 🗺️ Where content is laid out.
     *
     * <p>Owner-namespaced by default, which reproduces the layout products already have on disk, so
     * adopting the library is a code change rather than a data movement. Setting
     * {@code content-addressed-keys} switches to placing content by its digest, which is what
     * deduplication needs — and applies to new writes only, since a key is stored verbatim against
     * whatever row already has it.</p>
     *
     * @param settings the active settings
     * @return the key layout
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageKeyStrategy storageKeyStrategy(StorageSettings settings) {
        return settings.contentAddressedKeys()
                ? new ContentAddressedKeyStrategy()
                : new OwnerNamespacedKeyStrategy();
    }

    /**
     * 🛃 What may enter storage, entirely from configuration.
     *
     * <p>⚠️ Published even though the write paths now ask {@link #uploadPolicyResolver} instead: this is
     * a bean of a released library, something outside them may well inject it, and withdrawing one is a
     * breaking change nobody asked for. It is also what the default resolver hands back.</p>
     *
     * @param settings the active settings
     * @return the acceptance policy
     */
    @Bean
    @ConditionalOnMissingBean
    public UploadPolicy uploadPolicy(StorageSettings settings) {
        return UploadPolicy.of(settings);
    }

    /**
     * 🛃 Which policy applies to content headed for a given destination.
     *
     * <p>The installation's one policy, everywhere — which is what a product got before destinations
     * could carry their own, and what it keeps until a module contributes a resolver that knows better.
     * {@code jmouse-storage-management} publishes one that reads a directory's own rule.</p>
     *
     * @param uploadPolicy the installation's policy
     * @return the resolver
     */
    @Bean
    @ConditionalOnMissingBean
    public UploadPolicyResolver uploadPolicyResolver(UploadPolicy uploadPolicy) {
        return new FixedUploadPolicy(uploadPolicy);
    }

    /**
     * 🧭 The delivery decision, for every product.
     *
     * @param fileStores the backends
     * @param settings   the active settings
     * @return the planner
     */
    @Bean
    @ConditionalOnMissingBean
    public DeliveryPlanner deliveryPlanner(FileStores fileStores, StorageSettings settings) {
        return new DeliveryPlanner(fileStores, settings.cache());
    }

    /**
     * 🖨️ The renderer turning a plan into a response entity.
     *
     * @param fileStores the backends
     * @return the renderer
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.http.ResponseEntity")
    public DeliveryRenderer deliveryRenderer(FileStores fileStores) {
        return new DeliveryRenderer(fileStores);
    }

    /**
     * 📚 A read-only resource loader over the default backend, registering a {@code storage}
     * protocol.
     *
     * <p>So that jMouse code already reading a resource by location string can read from storage
     * without learning a new API. Only reading: writing, describing, deleting and presigning stay
     * on {@link FileStore}, because a resource loader has nowhere to express them.</p>
     *
     * @param fileStores the backends
     * @return the loader
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "jmouse.storage.resource-loader.enabled", havingValue = "true",
                           matchIfMissing = true)
    public FileStoreResourceLoader fileStoreResourceLoader(FileStores fileStores) {
        return new FileStoreResourceLoader(fileStores.defaultStore());
    }

    /**
     * 🔎 Says out loud what was built, because the failure mode of autoconfiguration not running is
     * silence.
     *
     * @param fileStores the backends
     * @param settings   the active settings
     * @return the diagnostic
     */
    @Bean
    public StorageDiagnostics storageDiagnostics(FileStores fileStores, StorageSettings settings) {
        return new StorageDiagnostics(fileStores, settings);
    }
}
