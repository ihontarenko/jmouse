package org.jmouse.storage.configuration;

import org.jmouse.storage.exception.StorageException;

/**
 * 🔌 One backend, fully described: which kind it is and everything that kind needs.
 *
 * <p>Split out of {@link StorageSettings} because an application may run more than one at a time.
 * A product that started on local disk and later added an object store keeps reading its old
 * objects from disk while writing new ones to the store — so "the backend" stopped being a single
 * value and became a set with one default.</p>
 *
 * <p>This is also where connection details are <em>interpreted</em>: {@link S3Settings} is inert
 * data, and what any of it means — which endpoint applies, which region to fall back to, whether
 * addressing is path-style — depends on the active {@link #provider}. Both halves live here, so
 * the resolution happens where both are already in hand.</p>
 *
 * @param name             how this backend is referred to, and what is recorded against every
 *                         object it writes
 * @param provider         which kind of backend it is
 * @param storageDirectory root directory for {@link StorageProvider#LOCAL}; ignored by object stores
 * @param s3               object-store connection details; ignored by local disk
 */
public record BackendSettings(String name, StorageProvider provider, String storageDirectory,
                              S3Settings s3) {

    private static final String SUPABASE_STORAGE_PATH = "/storage/v1/s3";
    private static final String TRAILING_SLASH        = "/";

    /**
     * 🏗️ Fill in whatever configuration omitted, including a name derived from the provider — so
     * a single-backend application never has to invent one.
     */
    public BackendSettings {
        provider         = (provider == null) ? StorageProvider.LOCAL : provider;
        name             = (name == null || name.isBlank()) ? provider.getDefaultBackendName() : name;
        storageDirectory = (storageDirectory == null || storageDirectory.isBlank())
                ? StorageSettings.DEFAULT_STORAGE_DIRECTORY : storageDirectory;
        s3               = (s3 == null) ? S3Settings.none() : s3;
    }

    /**
     * 🏗️ A local-disk backend under an explicit root.
     *
     * @param name      how the backend is referred to
     * @param directory root directory every key resolves against
     * @return the backend definition
     */
    public static BackendSettings local(String name, String directory) {
        return new BackendSettings(name, StorageProvider.LOCAL, directory, null);
    }

    /**
     * 🏷️ This backend under a different name, for configuration that names it explicitly.
     *
     * @param name the name to use
     * @return a copy carrying that name
     */
    public BackendSettings named(String name) {
        return new BackendSettings(name, provider, storageDirectory, s3);
    }

    /**
     * 🌐 The endpoint to hand the object-store client, or {@code null} to let the SDK derive the
     * standard AWS endpoint from the region.
     *
     * @return the resolved endpoint, or {@code null}
     */
    public String resolveEndpoint() {
        if (provider == StorageProvider.SUPABASE) {
            if (s3.supabaseProjectUrl() == null || s3.supabaseProjectUrl().isBlank()) {
                throw new StorageException("Setting 's3.supabase-project-url' is required when provider=supabase");
            }

            return trimTrailingSlash(s3.supabaseProjectUrl()) + SUPABASE_STORAGE_PATH;
        }

        String endpoint = s3.endpoint();

        return (endpoint == null || endpoint.isBlank()) ? null : trimTrailingSlash(endpoint);
    }

    /**
     * 🌐 The endpoint direct links are signed against.
     *
     * <p>A direct link is followed by the <em>browser</em>, not by the application. When the
     * application reaches the object store over an internal address — a Docker hostname,
     * {@code localhost} — a link signed against that address is unreachable for every real client.
     * The signature covers the host, so such a URL cannot be rewritten afterwards; it has to be
     * signed this way in the first place.</p>
     *
     * @return the resolved public endpoint, or {@code null}
     */
    public String resolvePublicEndpoint() {
        String publicEndpoint = s3.publicEndpoint();

        if (publicEndpoint != null && !publicEndpoint.isBlank()) {
            return trimTrailingSlash(publicEndpoint);
        }

        return resolveEndpoint();
    }

    /**
     * 🌍 The region to sign with.
     *
     * @return the configured region, or the provider's default
     */
    public String resolveRegion() {
        String region = s3.region();

        if (region != null && !region.isBlank()) {
            return region;
        }

        return provider.getDefaultRegion();
    }

    /**
     * 🛣️ Whether to address objects as {@code endpoint/bucket/key}.
     *
     * @return the configured override when set, otherwise the provider's default
     */
    public boolean resolvePathStyleAccess() {
        return (s3.pathStyleAccess() != null) ? s3.pathStyleAccess() : provider.isPathStyleAccess();
    }

    /**
     * ✅ Reject a configuration that could not possibly work, at startup rather than on the first
     * upload — where the misconfiguration would surface as a confusing SDK error hours later.
     */
    public void validate() {
        if (!provider.isObjectStore()) {
            return;
        }

        requireConfigured(s3.bucket(), "s3.bucket");
        requireConfigured(s3.accessKey(), "s3.access-key");
        requireConfigured(s3.secretKey(), "s3.secret-key");
        requireConfigured(resolveRegion(), "s3.region");

        if (provider == StorageProvider.MINIO) {
            requireConfigured(s3.endpoint(), "s3.endpoint");
        }

        if (s3.linkTimeToLive().isNegative() || s3.linkTimeToLive().isZero()) {
            throw new StorageException("Setting 's3.link-time-to-live' must be positive");
        }
    }

    private void requireConfigured(String value, String setting) {
        if (value == null || value.isBlank()) {
            throw new StorageException("Backend '%s': setting '%s' is required when provider=%s"
                                               .formatted(name, setting, provider.name().toLowerCase()));
        }
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith(TRAILING_SLASH) ? value.substring(0, value.length() - 1) : value;
    }
}
