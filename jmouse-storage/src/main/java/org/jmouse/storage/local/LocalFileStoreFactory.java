package org.jmouse.storage.local;

import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileStoreFactory;
import org.jmouse.storage.configuration.BackendSettings;
import org.jmouse.storage.configuration.StorageProvider;

/**
 * 🏭 Builds local-disk backends.
 *
 * <p>Ships in the core module because local disk is the one backend every application has, needs
 * no credentials and drags nothing onto a classpath.</p>
 */
public class LocalFileStoreFactory implements FileStoreFactory {

    @Override
    public boolean supports(StorageProvider provider) {
        return provider == StorageProvider.LOCAL;
    }

    @Override
    public FileStore create(BackendSettings backend) {
        backend.validate();
        return new LocalFileStore(backend);
    }
}
