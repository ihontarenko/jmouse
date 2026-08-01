package org.jmouse.storage.smoke;

import org.jmouse.core.MediaType;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceLoaderRegistry;
import org.jmouse.core.io.ResourceSegment;
import org.jmouse.core.io.CompositeResourceLoader;
import org.jmouse.http.Range;
import org.jmouse.storage.Content;
import org.jmouse.storage.ContentTypes;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.ObjectDescription;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;
import org.jmouse.storage.configuration.StorageProvider;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.configuration.UploadSettings;
import org.jmouse.storage.exception.ObjectNotFoundException;
import org.jmouse.storage.exception.StorageException;
import org.jmouse.storage.exception.StorageKeyException;
import org.jmouse.storage.exception.UploadRejectedException;
import org.jmouse.storage.key.OwnerNamespacedKeyStrategy;
import org.jmouse.storage.key.StorageKeyRequest;
import org.jmouse.storage.key.StorageKeyStrategy;
import org.jmouse.storage.local.LocalFileStore;
import org.jmouse.storage.policy.AcceptanceMode;
import org.jmouse.storage.policy.UploadPolicy;
import org.jmouse.storage.resource.FileStoreResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 🔥 Exercises every acceptance criterion of the byte layer against a throwaway directory.
 *
 * <p>Follows the repository's own convention — verification lives as a {@code smoke} class with a
 * {@code main} method rather than as a JUnit suite, because jMouse ships none.</p>
 *
 * <pre>{@code
 * java -cp jmouse-storage/target/classes:… org.jmouse.storage.smoke.StorageSmoke
 * }</pre>
 */
public final class StorageSmoke {

    private static final byte[] PAYLOAD = "jMouse storage: the byte layer.".getBytes(StandardCharsets.UTF_8);

    /**
     * SHA-256 of {@link #PAYLOAD}, so the digest is asserted against a known value rather than
     * against whatever the code happened to produce.
     */
    private static final String PAYLOAD_SHA256 =
            "a5f1588c5960905c312d5089363853da99cbc4e3f3407a8b1cd31c331a583333";

    private static int failures = 0;

    private StorageSmoke() {
    }

    /**
     * ▶️ Run every check and report.
     *
     * @param arguments ignored
     * @throws IOException when the throwaway directory cannot be created
     */
    public static void main(String[] arguments) throws IOException {
        Path root = Files.createTempDirectory("jmouse-storage-smoke-");

        try {
            FileStore fileStore = new LocalFileStore(root);

            verifyKeyRejection();
            verifyKeyStrategy();
            verifyRoundTrip(fileStore);
            verifyRangeRead(fileStore);
            verifyDeleteOfMissingKey(fileStore);
            verifyDirectLinkAbsence(fileStore);
            verifyResourceLoader(fileStore);
            verifyPolicyModes();
            verifySettingsBinding();
        } finally {
            removeRecursively(root);
        }

        System.out.println(failures == 0
                                   ? "storage smoke passed"
                                   : "storage smoke: %d failure(s)".formatted(failures));
        System.exit(failures == 0 ? 0 : 1);
    }

    /**
     * 🚫 Every escape shape is refused at construction, on every backend.
     */
    private static void verifyKeyRejection() {
        List<String> escapes = List.of(
                "../etc/passwd", "..\\etc\\passwd", "a/../../b", "a\\..\\..\\b",
                "/etc/passwd", "\\etc\\passwd", "C:/Windows/System32", "C:\\Windows\\System32",
                "file:///etc/passwd", "https://example.com/x", "a/b\0c", "", "   "
        );

        for (String escape : escapes) {
            rejects("key '%s'".formatted(escape.replace("\0", "\\0")),
                    StorageKeyException.class, () -> StorageKey.of(escape));
        }

        check("safe key survives", "documents/md/u/42/notes.md",
              StorageKey.of("documents/md/u/42/notes.md").value());
        check("backslashes fold", "documents/md/notes.md",
              StorageKey.of("documents\\md\\notes.md").value());
        check("extension parsed", "md", StorageKey.of("documents/md/notes.md").extension());
    }

    /**
     * 🗺️ The owner-namespaced layout is byte-identical to the one already in production.
     */
    private static void verifyKeyStrategy() {
        StorageKeyStrategy strategy = new OwnerNamespacedKeyStrategy(() -> "GENERATED");

        check("namespaced key", "documents/md/u/42/finance/report-a1.md",
              strategy.compose(StorageKeyRequest.forOwner("42")
                                       .namespace("documents/md")
                                       .segments("finance")
                                       .filenameBase("report-a1")
                                       .extension("md")
                                       .build()).value());

        check("blank segments dropped", "documents/md/u/42/report-a1.md",
              strategy.compose(StorageKeyRequest.forOwner("42")
                                       .namespace("documents/md")
                                       .segments("", null)
                                       .filenameBase("report-a1")
                                       .extension("md")
                                       .build()).value());

        check("blank namespace omitted", "u/42/GENERATED.png",
              strategy.compose(StorageKeyRequest.forOwner("42").extension("png").build()).value());

        check("blank extension omitted", "u/42/GENERATED",
              strategy.compose(StorageKeyRequest.forOwner("42").build()).value());

        check("extension taken from content", "u/42/GENERATED.pdf",
              strategy.compose(StorageKeyRequest.forContent("42", pdf()).build()).value());
    }

    /**
     * 🔄 Write, read and describe agree with each other and with the bytes.
     */
    private static void verifyRoundTrip(FileStore fileStore) throws IOException {
        StorageKey   key    = StorageKey.of("u/42/payload.txt");
        StoredObject stored = fileStore.write(key, Content.ofBytes("payload.txt", MediaType.TEXT_PLAIN, PAYLOAD));

        check("written size", (long) PAYLOAD.length, stored.sizeBytes());
        check("written digest", PAYLOAD_SHA256, stored.sha256());
        check("written type", "text/plain", ContentTypes.baseType(stored.contentType()));
        check("backend recorded", "local", fileStore.backendName());

        Resource resource = fileStore.read(key);
        try (InputStream stream = resource.getInputStream()) {
            check("read back", new String(PAYLOAD, StandardCharsets.UTF_8),
                  new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }

        ObjectDescription description = fileStore.describe(key);
        check("described size", (long) PAYLOAD.length, description.sizeBytes());
        check("described type", "text/plain", ContentTypes.baseType(description.contentType()));

        StoredObject rewritten = fileStore.write(key, Content.ofBytes("payload.txt", MediaType.TEXT_PLAIN, PAYLOAD));
        check("rewrite is idempotent", stored.sha256(), rewritten.sha256());

        verifyDescribeTypeFidelity(fileStore);

        fileStore.delete(key);
        rejects("read after delete", ObjectNotFoundException.class, () -> fileStore.read(key));
        rejects("describe after delete", ObjectNotFoundException.class, () -> fileStore.describe(key));
    }

    /**
     * 🎨 Where {@code describe} can and cannot recover the type {@code write} established.
     *
     * <p>With an extension in the key the two agree. Without one, the key carries no type and the
     * file system may or may not guess — so {@code describe} degrades to the default rather than
     * inventing an answer. That is the documented limitation, not a defect: an extensionless key
     * moves type resolution to the registry, which is where the write receipt gets recorded.</p>
     */
    private static void verifyDescribeTypeFidelity(FileStore fileStore) {
        StorageKey   named  = StorageKey.of("u/42/described.txt");
        StoredObject stored = fileStore.write(named, Content.ofBytes("notes.txt", MediaType.TEXT_PLAIN, PAYLOAD));

        check("describe agrees with write when the key carries an extension",
              ContentTypes.baseType(stored.contentType()),
              ContentTypes.baseType(fileStore.describe(named).contentType()));

        StorageKey   bare     = StorageKey.of("u/42/extensionless");
        StoredObject fromName = fileStore.write(bare, Content.ofBytes("notes.txt", MediaType.TEXT_PLAIN, PAYLOAD));

        check("extensionless write still resolves by original filename", "text/plain",
              ContentTypes.baseType(fromName.contentType()));
        check("extensionless describe never returns null", true,
              fileStore.describe(bare).contentType() != null);

        fileStore.delete(named);
        fileStore.delete(bare);
    }

    /**
     * ✂️ A byte range comes back with the right offset and length.
     */
    private static void verifyRangeRead(FileStore fileStore) throws IOException {
        StorageKey key = StorageKey.of("u/42/ranged.txt");
        fileStore.write(key, Content.ofBytes("ranged.txt", MediaType.TEXT_PLAIN, PAYLOAD));

        ResourceSegment head = fileStore.readRange(key, Range.ofRange(0, 5));
        check("head offset", 0L, head.getPosition());
        check("head length", 6L, head.getTotal());

        ResourceSegment tail = fileStore.readRange(key, Range.ofEnd(4));
        check("tail offset", (long) PAYLOAD.length - 4, tail.getPosition());
        check("tail length", 4L, tail.getTotal());

        ResourceSegment open = fileStore.readRange(key, Range.ofStart(10));
        check("open-ended length", (long) PAYLOAD.length - 10, open.getTotal());

        ResourceSegment clamped = fileStore.readRange(key, Range.ofRange(0, PAYLOAD.length + 100));
        check("over-long range clamps", (long) PAYLOAD.length, clamped.getTotal());

        rejects("unsatisfiable range", StorageException.class,
                () -> fileStore.readRange(key, Range.ofStart(PAYLOAD.length + 1)));

        fileStore.delete(key);
    }

    /**
     * 🗑️ Deleting nothing is a success, not a failure.
     */
    private static void verifyDeleteOfMissingKey(FileStore fileStore) {
        returns("delete of missing key", () -> fileStore.delete(StorageKey.of("u/42/never-existed.bin")));
        returns("delete under a directory that was never created",
                () -> fileStore.delete(StorageKey.of("never/created/at/all.bin")));
    }

    /**
     * 🔗 Local disk cannot serve its own bytes, and says so.
     */
    private static void verifyDirectLinkAbsence(FileStore fileStore) {
        Optional<?> link = fileStore.resolveDirectLink(StorageKey.of("u/42/payload.txt"), null);
        check("local backend offers no direct link", true, link.isEmpty());
    }

    /**
     * 📚 Stored objects are reachable through the resource-loader registry.
     */
    private static void verifyResourceLoader(FileStore fileStore) throws IOException {
        StorageKey key = StorageKey.of("u/42/loaded.txt");
        fileStore.write(key, Content.ofBytes("loaded.txt", MediaType.TEXT_PLAIN, PAYLOAD));

        ResourceLoaderRegistry registry = new CompositeResourceLoader();
        registry.addResourceLoader(new FileStoreResourceLoader(fileStore));

        String   location = FileStoreResourceLoader.STORAGE_PROTOCOL + ":" + key.value();
        Resource resource = registry.getRequiredResourceLoader(location).getResource(location);

        try (InputStream stream = resource.getInputStream()) {
            check("loaded through registry", (long) PAYLOAD.length, (long) stream.readAllBytes().length);
        }

        fileStore.delete(key);
    }

    /**
     * 🛃 One component, both modes, configuration alone.
     */
    private static void verifyPolicyModes() {
        UploadPolicy denying = UploadPolicy.of(settingsWith(
                new UploadSettings(AcceptanceMode.DENYLIST, Set.of("text/html", "image/svg+xml"),
                                   Set.of("exe", "svg"))));

        accepts("denylist admits a PDF", denying, pdf());
        rejects("denylist refuses declared markup", UploadRejectedException.class,
                () -> denying.accept(named("page.txt", "text/html")));
        rejects("denylist refuses a listed extension", UploadRejectedException.class,
                () -> denying.accept(named("payload.exe", null)));
        accepts("denylist admits an unknown extension", denying, named("archive.tar.zst", null));
        accepts("denylist admits no extension at all", denying, named("README", null));

        UploadPolicy allowing = UploadPolicy.of(settingsWith(
                new UploadSettings(AcceptanceMode.ALLOWLIST, Set.of("application/pdf", "image/png"),
                                   Set.of("pdf", "png"))));

        accepts("allowlist admits a PDF", allowing, pdf());
        rejects("allowlist refuses an unlisted extension", UploadRejectedException.class,
                () -> allowing.accept(named("notes.txt", null)));
        rejects("allowlist refuses an unlisted type", UploadRejectedException.class,
                () -> allowing.accept(named("notes.pdf", "text/plain")));
        rejects("allowlist refuses no extension at all", UploadRejectedException.class,
                () -> allowing.accept(named("README", null)));

        UploadPolicy tiny = new UploadPolicy(AcceptanceMode.DENYLIST, Set.of(), Set.of(), 8);
        rejects("size limit enforced", UploadRejectedException.class,
                () -> tiny.accept(Content.ofBytes("payload.txt", MediaType.TEXT_PLAIN, PAYLOAD)));
        accepts("unknown size passes the declared check", tiny,
                new Content("payload.txt", null, Content.UNKNOWN_SIZE, () -> null));
        rejects("empty content refused", UploadRejectedException.class,
                () -> tiny.accept(Content.ofBytes("payload.txt", MediaType.TEXT_PLAIN, new byte[0])));

        verifyShippedPolicies();
    }

    /**
     * 📦 The two configurations already in production ship with the library, SVG included.
     */
    private static void verifyShippedPolicies() {
        UploadPolicy dangerous = UploadPolicy.of(settingsWith(UploadSettings.blockingDangerousContent()));

        accepts("shipped denylist admits a PDF", dangerous, pdf());
        rejects("shipped denylist refuses an SVG extension", UploadRejectedException.class,
                () -> dangerous.accept(named("logo.svg", null)));
        rejects("shipped denylist refuses a declared SVG", UploadRejectedException.class,
                () -> dangerous.accept(named("logo.png", "image/svg+xml")));
        rejects("shipped denylist refuses an executable", UploadRejectedException.class,
                () -> dangerous.accept(named("setup.exe", null)));

        UploadPolicy documents = UploadPolicy.of(settingsWith(UploadSettings.allowingDocumentsAndImages()));

        accepts("shipped allowlist admits a PDF", documents, pdf());
        accepts("shipped allowlist admits a PNG", documents, named("chart.png", "image/png"));
        rejects("shipped allowlist refuses an SVG", UploadRejectedException.class,
                () -> documents.accept(named("logo.svg", null)));
    }

    /**
     * ⚙️ Settings bind through jMouse's own binder, under the caller's prefix.
     */
    private static void verifySettingsBinding() {
        Map<String, Object> storage = new HashMap<>();
        storage.put("provider", "MINIO");
        storage.put("storageDirectory", "/var/lib/innoventa");
        storage.put("maxSizeBytes", 1024L);

        StorageSettings settings = StorageSettings.bind(Map.of("innoventa", Map.of("file", storage)),
                                                        "innoventa.file");

        check("bound provider", StorageProvider.MINIO, settings.provider());
        check("bound directory", "/var/lib/innoventa", settings.storageDirectory());
        check("bound size", 1024L, settings.maxSizeBytes());
        check("absent groups default", true, settings.cache() != null && settings.sweeper() != null);
        check("default link lifetime", "PT15M", settings.s3().linkTimeToLive().toString());
        check("provider drives path-style addressing", true, settings.resolvePathStyleAccess());
        check("provider supplies the fallback region", "us-east-1", settings.resolveRegion());
        rejects("object store validates at startup", StorageException.class, settings::validate);

        StorageSettings defaults = StorageSettings.defaults();
        check("default provider", StorageProvider.LOCAL, defaults.provider());
        check("default directory", StorageSettings.DEFAULT_STORAGE_DIRECTORY, defaults.storageDirectory());
        check("default size", Long.parseLong(StorageSettings.DEFAULT_MAX_SIZE_BYTES), defaults.maxSizeBytes());
    }

    // ---------------------------------------------------------------- helpers

    private static StorageSettings settingsWith(UploadSettings upload) {
        return new StorageSettings(StorageProvider.LOCAL, null, 0, upload, null, null, null);
    }

    private static Content pdf() {
        return named("invoice.pdf", "application/pdf");
    }

    private static Content named(String filename, String contentType) {
        return Content.of(filename, contentType, Content.UNKNOWN_SIZE, () -> null);
    }

    private static void returns(String what, Runnable action) {
        try {
            action.run();
            pass(what + " returns rather than throwing");
        } catch (RuntimeException exception) {
            fail("%s threw %s".formatted(what, exception));
        }
    }

    private static void accepts(String what, UploadPolicy policy, Content content) {
        try {
            policy.accept(content);
            pass(what);
        } catch (RuntimeException exception) {
            fail("%s — refused with %s".formatted(what, exception.getMessage()));
        }
    }

    private static void rejects(String what, Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
            fail("%s — expected %s, nothing was thrown".formatted(what, expected.getSimpleName()));
        } catch (RuntimeException exception) {
            if (expected.isInstance(exception)) {
                pass(what);
            } else {
                fail("%s — expected %s, got %s".formatted(what, expected.getSimpleName(), exception));
            }
        }
    }

    private static void check(String what, Object expected, Object actual) {
        if (expected.equals(actual)) {
            pass(what);
        } else {
            fail("%s — expected '%s', got '%s'".formatted(what, expected, actual));
        }
    }

    private static void pass(String what) {
        System.out.println("  [ ok ] " + what);
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  [FAIL] " + what);
    }

    private static void removeRecursively(Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }
}
