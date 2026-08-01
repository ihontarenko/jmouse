package org.jmouse.storage.configuration;

/**
 * 🔌 The backends storage can be pointed at.
 *
 * <p>AWS S3, MinIO and Supabase Storage all speak the same API and differ only in endpoint,
 * addressing style and region — so they share one implementation and are distinguished here, in
 * configuration, rather than in code. Adding a fourth S3-compatible provider is an entry in this
 * enumeration, not a class.</p>
 */
public enum StorageProvider {

    /**
     * 💾 Local disk. Cannot produce direct links — the application streams every byte.
     */
    LOCAL(false, null),

    /**
     * ☁️ Amazon S3. Region is required; the SDK derives the endpoint.
     */
    AWS(false, null),

    /**
     * 🪣 Self-hosted MinIO. Requires an explicit endpoint; region is a formality.
     */
    MINIO(true, "us-east-1"),

    /**
     * ⚡ Supabase Storage. Endpoint is derived from the project URL.
     */
    SUPABASE(true, null);

    private final boolean pathStyleAccess;
    private final String  defaultRegion;

    StorageProvider(boolean pathStyleAccess, String defaultRegion) {
        this.pathStyleAccess = pathStyleAccess;
        this.defaultRegion   = defaultRegion;
    }

    /**
     * 🛣️ Whether this provider addresses objects as {@code endpoint/bucket/key}.
     *
     * <p>Mandatory for MinIO and Supabase; AWS S3 uses virtual-host addressing
     * ({@code bucket.s3.region.amazonaws.com/key}).</p>
     *
     * @return {@code true} when path-style addressing is required
     */
    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    /**
     * 🌍 Region to sign with when none is configured.
     *
     * @return the default region, or {@code null} when the provider has none
     */
    public String getDefaultRegion() {
        return defaultRegion;
    }

    /**
     * 🪣 Whether this provider is an object store rather than local disk.
     *
     * @return {@code true} for everything but {@link #LOCAL}
     */
    public boolean isObjectStore() {
        return this != LOCAL;
    }
}
