package org.jmouse.storage;

import org.jmouse.storage.configuration.BackendSettings;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🗂️ The backends, built once at startup from configuration and held by name.
 *
 * <p>Constructed through {@link FileStoreFactory} rather than by knowing about implementations, so
 * this class has no idea what an object store is and a product using only local disk pulls no SDK.
 * A provider nothing claims fails here, at startup, naming what is missing — rather than on the
 * first upload months later.</p>
 */
public class StandardFileStores implements FileStores, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardFileStores.class);

    private final Map<String, FileStore> stores;
    private final String                 defaultBackendName;
    private final boolean                choiceExposed;

    /**
     * 🏗️ Build every configured backend.
     *
     * @param settings  the active storage settings
     * @param factories every factory the application could find
     */
    public StandardFileStores(StorageSettings settings, Collection<FileStoreFactory> factories) {
        Map<String, BackendSettings> configured = settings.resolveBackends();

        this.stores             = build(configured, factories);
        this.defaultBackendName = configured.keySet().iterator().next();
        this.choiceExposed      = settings.exposeBackendChoice();

        LOGGER.info("🗂️ Storage backends {} — default '{}', choice {}",
                    stores.keySet(), defaultBackendName, choiceExposed ? "exposed" : "hidden");
    }

    /**
     * 🏗️ Build a set holding one already-constructed store, for a test or a jMouse context wiring
     * its own.
     *
     * @param fileStore the only store
     */
    public StandardFileStores(FileStore fileStore) {
        this.stores             = Map.of(fileStore.backendName(), fileStore);
        this.defaultBackendName = fileStore.backendName();
        this.choiceExposed      = false;
    }

    @Override
    public FileStore defaultStore() {
        return stores.get(defaultBackendName);
    }

    @Override
    public String defaultBackendName() {
        return defaultBackendName;
    }

    @Override
    public Optional<FileStore> find(String backendName) {
        if (backendName == null || backendName.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(stores.get(backendName));
    }

    @Override
    public FileStore require(String backendName) {
        return find(backendName).orElseThrow(() -> new StorageException(
                "No storage backend named '%s' is configured — objects it wrote are unreachable "
                        + "until it is. Configured: %s".formatted(backendName, stores.keySet())));
    }

    @Override
    public List<String> backendNames() {
        return List.copyOf(stores.keySet());
    }

    @Override
    public boolean isChoiceExposed() {
        return choiceExposed;
    }

    @Override
    public FileStore forWriting(String requestedBackendName) {
        if (!choiceExposed || requestedBackendName == null || requestedBackendName.isBlank()) {
            return defaultStore();
        }

        return require(requestedBackendName);
    }

    /**
     * 🚪 Close every backend that holds something worth closing.
     *
     * <p>An object store owns a client and a presigner; local disk owns nothing. Closing what can
     * be closed and stepping over the rest keeps that difference out of every caller.</p>
     */
    @Override
    public void close() {
        for (FileStore store : stores.values()) {
            if (store instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception exception) {
                    LOGGER.warn("Could not close storage backend '{}'", store.backendName(), exception);
                }
            }
        }
    }

    /**
     * 🏭 Ask the factories to build each configured backend.
     *
     * @param configured backends to build, default first
     * @param factories  every factory the application could find
     * @return the stores, in the same order
     */
    private static Map<String, FileStore> build(Map<String, BackendSettings> configured,
                                                Collection<FileStoreFactory> factories) {
        Map<String, FileStore> built = new LinkedHashMap<>();

        for (Map.Entry<String, BackendSettings> entry : configured.entrySet()) {
            BackendSettings backend = entry.getValue();
            FileStoreFactory factory = factories.stream()
                    .filter(candidate -> candidate.supports(backend.provider()))
                    .findFirst()
                    .orElseThrow(() -> new StorageException(
                            "Backend '%s' asks for provider %s, which no factory on the classpath builds "
                                    + "— is the module for it missing?"
                                    .formatted(backend.name(), backend.provider())));

            built.put(entry.getKey(), factory.create(backend));
        }

        return built;
    }
}
