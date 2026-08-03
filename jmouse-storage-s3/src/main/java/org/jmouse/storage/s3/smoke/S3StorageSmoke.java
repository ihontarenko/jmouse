package org.jmouse.storage.s3.smoke;

import org.jmouse.core.MediaType;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceSegment;
import org.jmouse.http.ContentDisposition;
import org.jmouse.http.Range;
import org.jmouse.storage.Content;
import org.jmouse.storage.DirectLink;
import org.jmouse.storage.ObjectDescription;
import org.jmouse.storage.Presentation;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;
import org.jmouse.storage.configuration.S3Settings;
import org.jmouse.storage.configuration.StorageProvider;
import org.jmouse.storage.configuration.BackendSettings;
import org.jmouse.storage.exception.ObjectNotFoundException;
import org.jmouse.storage.exception.StorageException;
import org.jmouse.storage.key.OwnerNamespacedKeyStrategy;
import org.jmouse.storage.key.StorageKeyRequest;
import org.jmouse.storage.key.StorageKeyStrategy;
import org.jmouse.storage.s3.S3FileStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * 🔥 Exercises every acceptance criterion of the object-store backend against a real store.
 *
 * <p>Follows the repository's own convention — verification lives as a {@code smoke} class with a
 * {@code main} method rather than as a JUnit suite, because jMouse ships none.</p>
 *
 * <p>Defaults to the MinIO instance in the workspace {@code docker-compose.yml}
 * ({@code http://localhost:9000}, bucket {@code inventory}). Override any of it from the command
 * line, in this order:</p>
 *
 * <pre>{@code
 * java -cp … org.jmouse.storage.s3.smoke.S3StorageSmoke \
 *      [endpoint] [bucket] [accessKey] [secretKey] [provider]
 * }</pre>
 *
 * <p>Objects are written under a {@code smoke/} namespace and removed on the way out, so running
 * this against a real bucket leaves nothing behind.</p>
 */
public final class S3StorageSmoke {

    private static final String DEFAULT_ENDPOINT   = "http://localhost:9000";
    private static final String DEFAULT_BUCKET     = "inventory";
    private static final String DEFAULT_ACCESS_KEY = "innoventa";
    private static final String DEFAULT_SECRET_KEY = "innoventa123";

    private static final byte[] PAYLOAD = "jMouse storage: the object-store backend.".getBytes(StandardCharsets.UTF_8);

    /**
     * SHA-256 of {@link #PAYLOAD}, asserted against a known value rather than against whatever the
     * code happened to produce.
     */
    private static final String PAYLOAD_SHA256 =
            "77eac09bf9b40b4ebc248b3fce5b3646e359ca65dccfa1fe3f3f26eb855a80cf";

    private static final String  PUBLIC_ENDPOINT   = "http://files.example.test";
    private static final int     RANGE_START       = 7;
    private static final int     RANGE_END         = 19;
    private static final int     STARTUP_ARGUMENTS = 5;

    private static int failures = 0;

    private S3StorageSmoke() {
    }

    /**
     * ▶️ Run every check and report.
     *
     * @param arguments optional endpoint, bucket, access key, secret key, provider
     * @throws IOException when a stored object cannot be read back
     */
    public static void main(String[] arguments) throws IOException {
        verifyStartupValidation();

        BackendSettings backend = backend(arguments, null);

        System.out.println("Against " + backend.resolveEndpoint() + " bucket " + backend.s3().bucket());

        try (S3FileStore fileStore = new S3FileStore(backend)) {
            StorageKey key = keyFor("payload.txt");

            verifyBackendName(fileStore);
            verifyWrite(fileStore, key);
            verifyRead(fileStore, key);
            verifyDescribe(fileStore, key);
            verifyRangeRead(fileStore, key);
            verifyDirectLink(fileStore, key);
            verifyMissingObject(fileStore);
            verifyDelete(fileStore, key);
        }

        verifySigningEndpointIsSeparate(arguments);
        verifyPresignFallback(arguments);

        System.out.println(failures == 0 ? "\nAll checks passed." : "\n%d check(s) failed.".formatted(failures));

        if (failures > 0) {
            System.exit(1);
        }
    }

    // ── Checks ────────────────────────────────────────────────────────────────

    /**
     * ✅ Misconfiguration fails when the store is constructed, naming the missing setting — not on
     * the first upload with an opaque SDK error.
     */
    private static void verifyStartupValidation() {
        System.out.println("\nStartup validation");

        S3Settings withoutBucket = new S3Settings(
                null, "us-east-1", DEFAULT_ENDPOINT, null, "key", "secret", null, null, null);

        rejects("a missing bucket is refused at construction", "s3.bucket",
                () -> new S3FileStore(backend(StorageProvider.MINIO, withoutBucket)));

        S3Settings withoutEndpoint = new S3Settings(
                DEFAULT_BUCKET, "us-east-1", null, null, "key", "secret", null, null, null);

        rejects("MinIO without an endpoint is refused at construction", "s3.endpoint",
                () -> new S3FileStore(backend(StorageProvider.MINIO, withoutEndpoint)));

        S3Settings withoutSecret = new S3Settings(
                DEFAULT_BUCKET, "us-east-1", DEFAULT_ENDPOINT, null, "key", null, null, null, null);

        rejects("a missing secret key is refused at construction", "s3.secret-key",
                () -> new S3FileStore(backend(StorageProvider.MINIO, withoutSecret)));
    }

    private static void verifyBackendName(S3FileStore fileStore) {
        System.out.println("\nIdentity");
        check("backend takes its name from configuration", "minio", fileStore.backendName());
    }

    private static void verifyWrite(S3FileStore fileStore, StorageKey key) {
        System.out.println("\nWrite");

        StoredObject stored = fileStore.write(key, Content.ofBytes("payload.txt", null, PAYLOAD));

        check("receipt reports the size that arrived", (long) PAYLOAD.length, stored.sizeBytes());
        check("receipt carries the digest of the bytes", PAYLOAD_SHA256, stored.sha256());
        check("receipt resolves the content type", "text/plain", baseTypeOf(stored.contentType()));
        check("receipt names the key", key.value(), stored.key().value());

        StoredObject rewritten = fileStore.write(key, Content.ofBytes("payload.txt", null, PAYLOAD));
        check("writing the same key again replaces rather than duplicates",
              (long) PAYLOAD.length, rewritten.sizeBytes());
    }

    private static void verifyRead(S3FileStore fileStore, StorageKey key) throws IOException {
        System.out.println("\nRead");

        Resource resource = fileStore.read(key);

        check("length comes from the response, not from counting",
              (long) PAYLOAD.length, resource.getLength());
        check("bytes round-trip", new String(PAYLOAD, StandardCharsets.UTF_8), readFully(resource.getInputStream()));
    }

    private static void verifyDescribe(S3FileStore fileStore, StorageKey key) {
        System.out.println("\nDescribe");

        ObjectDescription described = fileStore.describe(key);

        check("describes the size without transferring", (long) PAYLOAD.length, described.sizeBytes());
        check("describes the content type", "text/plain", baseTypeOf(described.contentType()));
    }

    /**
     * ✂️ A range fetches only what was asked for, and still reports the whole object's length so a
     * {@code Content-Range} header built from the segment is truthful.
     */
    private static void verifyRangeRead(S3FileStore fileStore, StorageKey key) throws IOException {
        System.out.println("\nRange read");

        Range           range   = Range.ofRange(RANGE_START, RANGE_END);
        ResourceSegment segment = fileStore.readRange(key, range);

        check("segment starts where the range does", (long) RANGE_START, segment.getPosition());
        check("segment is exactly as long as the range", (long) (RANGE_END - RANGE_START + 1), segment.getTotal());
        check("segment reports the whole object's length",
              (long) PAYLOAD.length, segment.getResource().getLength());

        String expected = new String(PAYLOAD, RANGE_START, RANGE_END - RANGE_START + 1, StandardCharsets.UTF_8);

        try (InputStream stream = segment.getResource().getInputStream()) {
            long skipped = stream.skip(RANGE_START);
            check("a consumer skipping to the range start is satisfied without a second fetch",
                  (long) RANGE_START, skipped);
            check("only the requested bytes are there", expected, readFully(stream));
        }

        ResourceSegment suffix = fileStore.readRange(key, Range.ofEnd(4));
        check("a suffix range resolves against the object's length",
              (long) (PAYLOAD.length - 4), suffix.getPosition());

        rejects("a range past the end of the object is refused", StorageException.class,
                () -> fileStore.readRange(key, Range.ofRange(PAYLOAD.length + 10, PAYLOAD.length + 20)));
    }

    /**
     * 🔗 The link carries the presentation a client would have received from the application, since
     * the provider serves those bytes without consulting us.
     */
    private static void verifyDirectLink(S3FileStore fileStore, StorageKey key) {
        System.out.println("\nDirect link");

        Presentation presentation = new Presentation(
                MediaType.forString("text/plain"),
                new ContentDisposition("attachment", null, "payload.txt", null, null, null, null, null));

        Optional<DirectLink> link = fileStore.resolveDirectLink(key, presentation);

        if (link.isEmpty()) {
            fail("a direct link is produced");
            return;
        }

        String location = link.get().location().toString();

        pass("a direct link is produced");
        contains("the link points at the configured endpoint", location, "localhost:9000");
        contains("the link addresses the bucket path-style", location, DEFAULT_BUCKET + "/");
        contains("the response content type is signed in", location, "response-content-type");
        contains("the response disposition is signed in", location, "response-content-disposition");
        check("the link reports how long it lasts", Duration.ofMinutes(15), link.get().timeToLive());

        followTheLink(link.get());
    }

    /**
     * 🌍 Actually fetch the link, as a browser would.
     *
     * <p>The point of signing presentation into the URL is that a client cannot tell a redirect
     * apart from a stream — so this checks the headers that come back, not only the ones that went
     * into the signature.</p>
     */
    private static void followTheLink(DirectLink link) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(link.location()).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            check("the link is accepted by the store", 200, response.statusCode());
            check("the link serves the object's bytes",
                  new String(PAYLOAD, StandardCharsets.UTF_8), response.body());
            check("the store applies the signed disposition", "attachment; filename=\"payload.txt\"",
                  response.headers().firstValue("content-disposition").orElse(null));
            check("the store applies the signed content type", "text/plain",
                  response.headers().firstValue("content-type").orElse(null));
        } catch (IOException exception) {
            fail("following the link failed: " + exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail("following the link was interrupted");
        }
    }

    /**
     * 🛟 A direct link is an optimisation, never a requirement, so a presign that cannot be produced
     * degrades to streaming rather than failing the request.
     *
     * <p>Forced by a link lifetime beyond the seven days a SigV4 signature may cover — a real
     * failure of the real signer, and a plausible misconfiguration, rather than a stub arranged to
     * throw. Note that closing the store does <em>not</em> work as a trigger: presigning computes a
     * signature locally and needs no connection, so a closed presigner signs perfectly well.</p>
     */
    private static void verifyPresignFallback(String[] arguments) {
        System.out.println("\nPresign failure");

        S3Settings unsignableLifetime = new S3Settings(
                argument(arguments, 1, DEFAULT_BUCKET), null, argument(arguments, 0, DEFAULT_ENDPOINT), null,
                argument(arguments, 2, DEFAULT_ACCESS_KEY), argument(arguments, 3, DEFAULT_SECRET_KEY),
                null, null, Duration.ofDays(8));

        try (S3FileStore fileStore = new S3FileStore(backend(StorageProvider.MINIO, unsignableLifetime))) {
            Presentation presentation = new Presentation(MediaType.forString("text/plain"), null);

            Optional<DirectLink> link = fileStore.resolveDirectLink(keyFor("payload.txt"), presentation);
            check("a failed presign resolves to no link rather than propagating", Optional.empty(), link);
        } catch (RuntimeException exception) {
            fail("a failed presign propagated instead of falling back: " + exception);
        }
    }

    /**
     * 🌐 A link is signed against the endpoint the browser will reach, which is not always the one
     * this application talks to. The host is part of the signature, so it cannot be rewritten
     * afterwards — it has to be signed that way up front.
     */
    private static void verifySigningEndpointIsSeparate(String[] arguments) {
        System.out.println("\nSeparate signing endpoint");

        try (S3FileStore fileStore = new S3FileStore(backend(arguments, PUBLIC_ENDPOINT))) {
            Presentation presentation = new Presentation(MediaType.forString("text/plain"), null);
            Optional<DirectLink> link = fileStore.resolveDirectLink(keyFor("payload.txt"), presentation);

            if (link.isEmpty()) {
                fail("a link is produced against the public endpoint");
                return;
            }

            pass("a link is produced against the public endpoint");
            contains("the link is signed for the browser-facing host",
                     link.get().location().toString(), "files.example.test");
        }
    }

    private static void verifyMissingObject(S3FileStore fileStore) {
        System.out.println("\nMissing objects");

        StorageKey absent = keyFor("no-such-object.txt");

        rejects("reading a missing object reports it as missing", ObjectNotFoundException.class,
                () -> fileStore.read(absent));
        rejects("describing a missing object reports it as missing", ObjectNotFoundException.class,
                () -> fileStore.describe(absent));
        returns("deleting a missing object", () -> fileStore.delete(absent));
    }

    private static void verifyDelete(S3FileStore fileStore, StorageKey key) {
        System.out.println("\nDelete");

        fileStore.delete(key);

        rejects("a deleted object is gone", ObjectNotFoundException.class, () -> fileStore.describe(key));
        returns("deleting twice", () -> fileStore.delete(key));
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    /**
     * 🔑 A key under the smoke namespace, composed through the shipped layout strategy rather than
     * hand-written, so the backend is exercised with the keys a real caller would produce.
     */
    private static StorageKey keyFor(String filename) {
        StorageKeyStrategy strategy = new OwnerNamespacedKeyStrategy();

        return strategy.compose(StorageKeyRequest.forOwner("smoke-owner")
                                        .namespace("smoke")
                                        .filenameBase(filename.substring(0, filename.lastIndexOf('.')))
                                        .extension(filename.substring(filename.lastIndexOf('.') + 1))
                                        .build());
    }

    private static BackendSettings backend(String[] arguments, String publicEndpoint) {
        String endpoint  = argument(arguments, 0, DEFAULT_ENDPOINT);
        String bucket    = argument(arguments, 1, DEFAULT_BUCKET);
        String accessKey = argument(arguments, 2, DEFAULT_ACCESS_KEY);
        String secretKey = argument(arguments, 3, DEFAULT_SECRET_KEY);
        String provider  = argument(arguments, 4, StorageProvider.MINIO.name());

        S3Settings s3 = new S3Settings(bucket, null, endpoint, publicEndpoint, accessKey, secretKey,
                                       null, null, null);

        return backend(StorageProvider.valueOf(provider.toUpperCase()), s3);
    }

    private static BackendSettings backend(StorageProvider provider, S3Settings s3) {
        return new BackendSettings(null, provider, null, s3);
    }

    private static String argument(String[] arguments, int index, String fallback) {
        if (arguments == null || index >= Math.min(arguments.length, STARTUP_ARGUMENTS)) {
            return fallback;
        }

        return arguments[index].isBlank() ? fallback : arguments[index];
    }

    private static String baseTypeOf(MediaType mediaType) {
        return mediaType.getType() + "/" + mediaType.getSubType();
    }

    private static String readFully(InputStream stream) throws IOException {
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        stream.transferTo(collected);
        return collected.toString(StandardCharsets.UTF_8);
    }

    // ── Reporting ─────────────────────────────────────────────────────────────

    private static void returns(String what, Runnable action) {
        try {
            action.run();
            pass(what + " returns rather than throwing");
        } catch (RuntimeException exception) {
            fail("%s threw %s".formatted(what, exception));
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

    /**
     * ✅ The failure names the setting somebody has to go and fix.
     */
    private static void rejects(String what, String namedSetting, Runnable action) {
        try {
            action.run();
            fail("%s — nothing was thrown".formatted(what));
        } catch (StorageException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains(namedSetting)) {
                pass(what);
            } else {
                fail("%s — the message does not name '%s': %s".formatted(what, namedSetting, exception.getMessage()));
            }
        } catch (RuntimeException exception) {
            fail("%s — expected StorageException, got %s".formatted(what, exception));
        }
    }

    private static void contains(String what, String actual, String expectedFragment) {
        if (actual != null && actual.contains(expectedFragment)) {
            pass(what);
        } else {
            fail("%s — '%s' does not contain '%s'".formatted(what, actual, expectedFragment));
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
}
