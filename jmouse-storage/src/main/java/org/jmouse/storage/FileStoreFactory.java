package org.jmouse.storage;

import org.jmouse.storage.configuration.BackendSettings;
import org.jmouse.storage.configuration.StorageProvider;

/**
 * 🏭 Builds one kind of {@link FileStore} from its configuration.
 *
 * <p>What makes "add a storage backend by implementing one interface and registering one bean"
 * true. An application collects every factory it can find and asks each configured backend's
 * provider which one claims it — so a new provider is a factory plus an enum constant, and no base
 * class, no {@code switch} and no existing file changes.</p>
 *
 * <p>It also keeps the object-store SDK off a classpath that does not want it: the factory for a
 * provider lives in the module implementing it, so a product using only local disk never sees the
 * S3 one.</p>
 */
public interface FileStoreFactory {

    /**
     * ❓ Whether this factory builds stores for a provider.
     *
     * @param provider provider to claim
     * @return {@code true} when this factory can build it
     */
    boolean supports(StorageProvider provider);

    /**
     * 🏗️ Build the store a backend describes.
     *
     * <p>Configuration is validated here rather than on first upload, so a backend that could not
     * possibly work fails at startup with a message naming the setting it is missing.</p>
     *
     * @param backend the backend to build
     * @return the store, named as the backend is
     */
    FileStore create(BackendSettings backend);
}
