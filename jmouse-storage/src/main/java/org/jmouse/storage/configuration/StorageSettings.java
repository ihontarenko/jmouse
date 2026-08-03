package org.jmouse.storage.configuration;

import org.jmouse.core.access.TypedValue;
import org.jmouse.core.binding.Bind;
import org.jmouse.core.binding.BindDefault;
import org.jmouse.storage.exception.StorageException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

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
 * <h3>One default backend, and any number of others</h3>
 *
 * <p>{@link #provider}, {@link #storageDirectory} and {@link #s3} describe the <em>default</em>
 * backend — the one everything is written to unless a caller says otherwise — and an application
 * that only ever uses one need configure nothing else. {@link #backends} adds further named ones
 * beside it:</p>
 *
 * <pre>{@code
 * innoventa.file.provider: local            # the default backend
 * innoventa.file.storage-directory: ./uploads
 * innoventa.file.expose-backend-choice: true
 * innoventa.file.backends.archive.provider: minio
 * innoventa.file.backends.archive.s3.bucket: archive
 * }</pre>
 *
 * <p>Every backend stays readable for as long as it is configured, because an object records which
 * one wrote it. Adding an object store to a product that started on local disk therefore changes
 * where <em>new</em> objects go and nothing about the old ones — they keep being served from disk,
 * and one listing spans both.</p>
 *
 * <p>{@link #exposeBackendChoice} decides whether callers may pick at all. Left at its default,
 * the whole arrangement is invisible: uploads go to the default backend and no interface has any
 * reason to mention that there are others.</p>
 *
 * @param provider            kind of the default backend
 * @param storageDirectory    root directory when the default backend is {@link StorageProvider#LOCAL}
 * @param maxSizeBytes        largest upload accepted
 * @param upload              what may enter storage
 * @param s3                  connection details of the default backend, when it is an object store
 * @param backends            further backends beside the default, by name
 * @param exposeBackendChoice whether callers may choose where an upload goes
 * @param contentAddressedKeys whether new objects are placed by their digest rather than by owner,
 *                             which is what deduplication needs
 * @param cache               how long clients may hold what storage serves
 * @param sweeper             when unreferenced objects are reclaimed
 */
public record StorageSettings(@BindDefault("LOCAL") StorageProvider provider,
                              @BindDefault(StorageSettings.DEFAULT_STORAGE_DIRECTORY) String storageDirectory,
                              @BindDefault(StorageSettings.DEFAULT_MAX_SIZE_BYTES) long maxSizeBytes,
                              UploadSettings upload,
                              S3Settings s3,
                              Map<String, BackendSettings> backends,
                              @BindDefault("false") boolean exposeBackendChoice,
                              @BindDefault("false") boolean contentAddressedKeys,
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
        backends         = (backends == null) ? Map.of() : Map.copyOf(backends);
        cache            = (cache == null) ? CacheSettings.defaults() : cache;
        sweeper          = (sweeper == null) ? SweeperSettings.defaults() : sweeper;
    }

    /**
     * 🏗️ The shipped defaults — local disk under {@code ./uploads}, refusing nothing.
     *
     * @return default settings
     */
    public static StorageSettings defaults() {
        return new StorageSettings(null, null, 0, null, null, null, false, false, null, null);
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
     * 🎯 The backend everything is written to unless a caller names another.
     *
     * <p>Assembled from the flat settings rather than configured separately, so an application
     * running one backend never learns that backends are a set.</p>
     *
     * @return the default backend
     */
    public BackendSettings defaultBackend() {
        return new BackendSettings(null, provider, storageDirectory, s3);
    }

    /**
     * 🔌 Every configured backend, by name, with the default first.
     *
     * <p>Order is meaningful: the first entry is the default, which is what an interface offering
     * a choice preselects. A named backend colliding with the default is rejected rather than
     * merged — two backends answering to one name would make an object's recorded backend
     * ambiguous, and a read could silently land at the wrong bucket.</p>
     *
     * @return the backends, default first
     * @throws StorageException when a named backend collides with the default
     */
    public Map<String, BackendSettings> resolveBackends() {
        BackendSettings              defaultBackend = defaultBackend();
        Map<String, BackendSettings> resolved       = new LinkedHashMap<>();

        resolved.put(defaultBackend.name(), defaultBackend);

        for (Map.Entry<String, BackendSettings> entry : new TreeMap<>(backends).entrySet()) {
            String name = entry.getKey();

            if (resolved.containsKey(name)) {
                throw new StorageException(
                        "Backend '%s' is already the default backend; give it a different name"
                                .formatted(name));
            }

            resolved.put(name, entry.getValue().named(name));
        }

        return resolved;
    }

    /**
     * ✅ Reject a configuration that could not possibly work, at startup rather than on the first
     * upload — where the misconfiguration would surface as a confusing SDK error hours later.
     *
     * <p>Every backend is checked, not only the default: a backend that cannot connect is just as
     * broken whether or not it is the one uploads happen to go to today.</p>
     */
    public void validate() {
        resolveBackends().values().forEach(BackendSettings::validate);
    }
}
