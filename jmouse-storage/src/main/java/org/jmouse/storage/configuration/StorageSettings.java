package org.jmouse.storage.configuration;

import org.jmouse.core.access.TypedValue;
import org.jmouse.core.binding.Bind;
import org.jmouse.core.binding.BindDefault;
import org.jmouse.storage.exception.StorageException;

import java.util.Map;

/**
 * ⚙️ Every setting the storage layer reads, as one record.
 *
 * <p>Bound through {@link Bind} rather than a framework annotation, so the same type binds inside
 * a jMouse context and inside a Spring one. {@code @BeanProperties} would have been the jMouse-
 * native choice and is deliberately not used: it is applied by a jMouse {@code BeanPostProcessor}
 * and would simply never fire in a Spring application.</p>
 *
 * <h3>Binding</h3>
 * <pre>{@code
 * StorageSettings settings = StorageSettings.bind(properties, "innoventa.file");
 * }</pre>
 *
 * <p>The prefix is the caller's, so a product keeps the property namespace it already publishes.</p>
 *
 * <p>This is also where connection details are <em>interpreted</em>: {@link S3Settings} is inert
 * data, and what any of it means depends on the active {@link #provider}. Both live here, so the
 * resolution happens where both are already in hand.</p>
 *
 * @param provider         which backend is active
 * @param storageDirectory root directory for {@link StorageProvider#LOCAL}; ignored by object stores
 * @param maxSizeBytes     largest upload accepted
 * @param upload           what may enter storage
 * @param s3               object-store connection details
 * @param cache            how long clients may hold what storage serves
 * @param sweeper          when unreferenced objects are reclaimed
 */
public record StorageSettings(@BindDefault("LOCAL") StorageProvider provider,
                              @BindDefault(StorageSettings.DEFAULT_STORAGE_DIRECTORY) String storageDirectory,
                              @BindDefault(StorageSettings.DEFAULT_MAX_SIZE_BYTES) long maxSizeBytes,
                              UploadSettings upload,
                              S3Settings s3,
                              CacheSettings cache,
                              SweeperSettings sweeper) {

    /**
     * 📏 The shipped upload ceiling: 50 MB.
     */
    public static final String DEFAULT_MAX_SIZE_BYTES = "52428800";

    /**
     * 📂 The shipped local root, relative to the working directory.
     */
    public static final String DEFAULT_STORAGE_DIRECTORY = "./uploads";

    private static final String SUPABASE_STORAGE_PATH = "/storage/v1/s3";
    private static final String TRAILING_SLASH        = "/";

    /**
     * 🏗️ Fill in whatever configuration omitted, so a partially specified block is still a usable
     * settings object.
     */
    public StorageSettings {
        provider         = (provider == null) ? StorageProvider.LOCAL : provider;
        storageDirectory = (storageDirectory == null || storageDirectory.isBlank())
                ? DEFAULT_STORAGE_DIRECTORY : storageDirectory;
        maxSizeBytes     = (maxSizeBytes > 0) ? maxSizeBytes : Long.parseLong(DEFAULT_MAX_SIZE_BYTES);
        upload           = (upload == null) ? UploadSettings.permissive() : upload;
        s3               = (s3 == null) ? S3Settings.none() : s3;
        cache            = (cache == null) ? CacheSettings.defaults() : cache;
        sweeper          = (sweeper == null) ? SweeperSettings.defaults() : sweeper;
    }

    /**
     * 🏗️ The shipped defaults — local disk under {@code ./uploads}, refusing nothing.
     *
     * @return default settings
     */
    public static StorageSettings defaults() {
        return new StorageSettings(null, null, 0, null, null, null, null);
    }

    /**
     * 🔗 Bind settings out of a property source, under a caller-chosen prefix.
     *
     * @param source source of values — a map, a bean, anything {@link Bind} can wrap
     * @param prefix property prefix, e.g. {@code innoventa.file}
     * @return the bound settings, defaulted where the source said nothing
     */
    public static StorageSettings bind(Object source, String prefix) {
        return Bind.with(source)
                .to(prefix, TypedValue.of(StorageSettings.class))
                .orElse(StorageSettings::defaults);
    }

    /**
     * 🔗 Bind settings out of a flat property map.
     *
     * @param properties flattened properties, e.g. {@code innoventa.file.provider -> minio}
     * @param prefix     property prefix
     * @return the bound settings
     */
    public static StorageSettings bind(Map<String, Object> properties, String prefix) {
        return bind((Object) properties, prefix);
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
            throw new StorageException("Setting '%s' is required when provider=%s"
                                               .formatted(setting, provider.name().toLowerCase()));
        }
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith(TRAILING_SLASH) ? value.substring(0, value.length() - 1) : value;
    }
}
