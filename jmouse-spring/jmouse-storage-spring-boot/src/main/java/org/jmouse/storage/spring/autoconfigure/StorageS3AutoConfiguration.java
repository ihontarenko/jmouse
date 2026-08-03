package org.jmouse.storage.spring.autoconfigure;

import org.jmouse.storage.s3.S3FileStore;
import org.jmouse.storage.s3.S3FileStoreFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * ☁️ Registers the object-store factory, and only when the module is actually present.
 *
 * <p>Separate from the main configuration so that a product using local disk alone never causes a
 * class-loading attempt against the AWS SDK. Adding {@code jmouse-storage-s3} to a build is the
 * whole of what it takes to make {@code provider: minio} work.</p>
 */
@AutoConfiguration(before = StorageAutoConfiguration.class)
@ConditionalOnClass(S3FileStore.class)
public class StorageS3AutoConfiguration {

    /**
     * 🏭 The factory covering AWS S3, MinIO and Supabase Storage.
     *
     * @return the factory
     */
    @Bean
    @ConditionalOnMissingBean(S3FileStoreFactory.class)
    public S3FileStoreFactory s3FileStoreFactory() {
        return new S3FileStoreFactory();
    }
}
