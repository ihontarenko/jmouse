package org.jmouse.storage.s3;

import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileStoreFactory;
import org.jmouse.storage.configuration.BackendSettings;
import org.jmouse.storage.configuration.StorageProvider;

/**
 * 🏭 Builds every S3-compatible backend: AWS S3, MinIO and Supabase Storage.
 *
 * <p>One factory for all three, because they are one implementation differing only by
 * configuration. It lives in this module so that a product using only local disk never pulls the
 * AWS SDK onto its classpath — the factory is simply not there to be found.</p>
 */
public class S3FileStoreFactory implements FileStoreFactory {

    @Override
    public boolean supports(StorageProvider provider) {
        return provider.isObjectStore();
    }

    @Override
    public FileStore create(BackendSettings backend) {
        return new S3FileStore(backend);
    }
}
