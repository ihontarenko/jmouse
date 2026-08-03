package org.jmouse.storage.smoke;

import org.jmouse.core.MediaType;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceSegment;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpHeader;
import org.jmouse.http.HttpMethod;
import org.jmouse.http.HttpStatus;
import org.jmouse.http.Range;
import org.jmouse.storage.Content;
import org.jmouse.storage.DirectLink;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileStores;
import org.jmouse.storage.ObjectDescription;
import org.jmouse.storage.Presentation;
import org.jmouse.storage.StandardFileStores;
import org.jmouse.storage.StorageKey;
import org.jmouse.storage.StoredObject;
import org.jmouse.storage.configuration.BackendSettings;
import org.jmouse.storage.configuration.CacheSettings;
import org.jmouse.storage.configuration.StorageProvider;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.delivery.ContentDispositions;
import org.jmouse.storage.delivery.DeliverableFile;
import org.jmouse.storage.delivery.DeliveryIntent;
import org.jmouse.storage.delivery.DeliveryPlan;
import org.jmouse.storage.delivery.DeliveryPlanner;
import org.jmouse.storage.exception.StorageException;
import org.jmouse.storage.key.ContentAddressedKeyStrategy;
import org.jmouse.storage.key.StorageKeyRequest;
import org.jmouse.storage.key.StorageKeyStrategy;
import org.jmouse.storage.local.LocalFileStoreFactory;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🔥 Exercises the delivery planner, the content-addressed layout and the multi-backend registry.
 *
 * <p>Every check here runs without an HTTP server, without a database and without a single byte
 * moving, which is the whole argument for planning separately from rendering: the subtle half is a
 * table of inputs to expected outputs.</p>
 *
 * <pre>{@code
 * java -cp jmouse-storage/target/classes:… org.jmouse.storage.smoke.DeliverySmoke
 * }</pre>
 */
public final class DeliverySmoke {

    private static final String DIGEST =
            "a3f92b1c4e5d6f708192a3b4c5d6e7f8091a2b3c4d5e6f708192a3b4c5d6e7f8";

    private static final String OTHER_DIGEST =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private static final long     OBJECT_SIZE      = 1_000L;
    private static final Duration LINK_TIME_TO_LIVE = Duration.ofMinutes(15);
    private static final Duration SAFETY_MARGIN     = Duration.ofMinutes(1);

    private static int failures = 0;

    private DeliverySmoke() {
    }

    /**
     * ▶️ Run every check and report.
     *
     * @param arguments ignored
     */
    public static void main(String[] arguments) {
        verifyBackendRegistry();
        verifyContentAddressedKeys();
        verifyRedirectVersusStream();
        verifyRedirectCacheLifetime();
        verifyConditionalRequests();
        verifyRanges();
        verifyDispositions();

        System.out.println(failures == 0 ? "\ndelivery smoke passed" : "\n%d check(s) failed.".formatted(failures));

        if (failures > 0) {
            System.exit(1);
        }
    }

    // ── Backends ──────────────────────────────────────────────────────────────

    /**
     * 🗂️ Several backends coexist: reads route by the backend an object records, and whether a
     * caller may pick where a write goes is configuration.
     */
    private static void verifyBackendRegistry() {
        System.out.println("\nBackend registry");

        FileStores hidden   = fileStores(false);
        FileStores exposed  = fileStores(true);

        check("default backend comes first", "local", hidden.defaultBackendName());
        check("every configured backend is present", List.of("local", "archive"), hidden.backendNames());
        check("a read routes to the backend that wrote the object",
              "archive", hidden.require("archive").backendName());

        check("with the choice hidden, a named write still goes to the default",
              "local", hidden.forWriting("archive").backendName());
        check("with the choice exposed, a named write is honoured",
              "archive", exposed.forWriting("archive").backendName());
        check("a blank request always means the default",
              "local", exposed.forWriting(null).backendName());

        check("the choice is hidden by default", false, hidden.isChoiceExposed());
        rejects("an unconfigured backend is named rather than silently swapped",
                () -> hidden.require("glacier"));
    }

    // ── Key layout ────────────────────────────────────────────────────────────

    /**
     * 🧬 The hybrid layout: a logical namespace, then the digest, fanned out two levels.
     */
    private static void verifyContentAddressedKeys() {
        System.out.println("\nContent-addressed keys");

        StorageKeyStrategy strategy = new ContentAddressedKeyStrategy();

        check("the layout announces that it needs the digest first", true, strategy.requiresContentDigest());

        StorageKey key = strategy.compose(StorageKeyRequest.forOwner("user-42")
                                                  .namespace("documents")
                                                  .extension("pdf")
                                                  .contentDigest(DIGEST)
                                                  .build());

        check("namespace, algorithm, fan-out and digest",
              "documents/sha256/a3/f9/" + DIGEST, key.value());
        check("the owner leaves the key entirely", false, key.value().contains("user-42"));
        check("no extension, so content type comes from the registry", "", key.extension());

        StorageKey sameBytes = strategy.compose(StorageKeyRequest.forOwner("someone-else")
                                                        .namespace("documents")
                                                        .contentDigest(DIGEST)
                                                        .build());

        check("identical bytes compose an identical key, whoever uploads them",
              key.value(), sameBytes.value());

        rejects("composing without a digest fails loudly rather than inventing one",
                () -> strategy.compose(StorageKeyRequest.forOwner("user-42").namespace("documents").build()));
    }

    // ── Redirect versus stream ────────────────────────────────────────────────

    /**
     * 🔗 A backend that can serve its own bytes produces a redirect; one that cannot produces a
     * stream.
     */
    private static void verifyRedirectVersusStream() {
        System.out.println("\nRedirect versus stream");

        DeliveryPlan streamed = plannerOver(false).plan(file(), DeliveryIntent.view());
        DeliveryPlan redirected = plannerOver(true).plan(file(), DeliveryIntent.view());

        check("a backend that cannot serve its own bytes streams",
              true, streamed instanceof DeliveryPlan.Streamed);
        check("a streamed response is 200", HttpStatus.OK, streamed.status());
        check("a streamed response advertises range support",
              "bytes", header(streamed, HttpHeader.ACCEPT_RANGES));

        check("a backend that can serve its own bytes redirects",
              true, redirected instanceof DeliveryPlan.Redirected);
        check("a redirect is 302", HttpStatus.FOUND, redirected.status());
    }

    /**
     * ⏳ A redirect's cache lifetime is the link's own, less the margin — never negative, never
     * immutable.
     */
    private static void verifyRedirectCacheLifetime() {
        System.out.println("\nRedirect cache lifetime");

        DeliveryPlan plan       = plannerOver(true).plan(file(), DeliveryIntent.view());
        String       cacheValue = header(plan, HttpHeader.CACHE_CONTROL);
        long         expected   = LINK_TIME_TO_LIVE.minus(SAFETY_MARGIN).toSeconds();

        check("max-age is link lifetime minus the safety margin",
              true, cacheValue.contains("max-age=" + expected));
        check("a redirect is never immutable — the target genuinely changes",
              false, cacheValue.contains("immutable"));

        DeliveryPlanner tightPlanner = new DeliveryPlanner(
                presigningStores(Duration.ofSeconds(10)),
                new CacheSettings(null, null, Duration.ofMinutes(5)));

        String tight = header(tightPlanner.plan(file(), DeliveryIntent.view()), HttpHeader.CACHE_CONTROL);

        check("a margin larger than the lifetime floors at zero rather than going negative",
              true, tight.contains("max-age=0"));

        String publicStreamed = header(
                plannerOver(false).plan(file(), new DeliveryIntent(false, DeliveryIntent.Audience.PUBLIC, null)),
                HttpHeader.CACHE_CONTROL);

        check("streamed public bytes, unlike a redirect, may be immutable",
              true, publicStreamed.contains("immutable"));
    }

    // ── Conditional requests ──────────────────────────────────────────────────

    /**
     * ♻️ A client holding the object gets told so; one holding a different object does not.
     */
    private static void verifyConditionalRequests() {
        System.out.println("\nConditional requests");

        DeliveryPlan matched = plannerOver(false).plan(file(), conditionalOn("\"" + DIGEST + "\""));

        check("a matching entity tag produces not-modified",
              true, matched instanceof DeliveryPlan.NotModified);
        check("not-modified is 304", HttpStatus.NOT_MODIFIED, matched.status());
        check("the tag is the object's digest", "\"" + DIGEST + "\"", header(matched, HttpHeader.ETAG));

        DeliveryPlan stale = plannerOver(false).plan(file(), conditionalOn("\"" + OTHER_DIGEST + "\""));

        check("a stale entity tag is served in full", true, stale instanceof DeliveryPlan.Streamed);

        DeliverableFile withoutDigest = new DeliverableFile(
                StorageKey.of("files/u/42/report.pdf"), "local", "report.pdf",
                MediaType.APPLICATION_PDF, OBJECT_SIZE, null);

        DeliveryPlan undigested = plannerOver(false).plan(withoutDigest, conditionalOn("\"anything\""));

        check("a file stored before the registry has no tag to answer with, and is served",
              true, undigested instanceof DeliveryPlan.Streamed);
    }

    // ── Ranges ────────────────────────────────────────────────────────────────

    /**
     * ✂️ A satisfiable range becomes partial content; an impossible one is refused rather than
     * quietly widened to the whole file.
     */
    private static void verifyRanges() {
        System.out.println("\nRanges");

        DeliveryPlan partial = plannerOver(false).plan(file(), rangeOf("bytes=100-199"));

        check("a range request produces partial content",
              true, partial instanceof DeliveryPlan.PartiallyStreamed);
        check("partial content is 206", HttpStatus.PARTIAL_CONTENT, partial.status());
        check("the content range names both ends and the whole length",
              "bytes 100-199/1000", header(partial, HttpHeader.CONTENT_RANGE));
        check("the body length is the range, not the object",
              100L, ((DeliveryPlan.PartiallyStreamed) partial).length());

        DeliveryPlan suffix = plannerOver(false).plan(file(), rangeOf("bytes=-200"));

        check("a suffix range resolves against the object's length",
              "bytes 800-999/1000", header(suffix, HttpHeader.CONTENT_RANGE));

        DeliveryPlan openEnded = plannerOver(false).plan(file(), rangeOf("bytes=900-"));

        check("an open-ended range runs to the last byte",
              "bytes 900-999/1000", header(openEnded, HttpHeader.CONTENT_RANGE));

        DeliveryPlan impossible = plannerOver(false).plan(file(), rangeOf("bytes=5000-6000"));

        check("a range past the end is refused",
              true, impossible instanceof DeliveryPlan.RangeNotSatisfiable);
        check("refusing a range is 416", HttpStatus.RANGE_NOT_SATISFIABLE, impossible.status());
        check("the refusal reports the object's real length",
              "bytes */1000", header(impossible, HttpHeader.CONTENT_RANGE));

        DeliveryPlan multiple = plannerOver(false).plan(file(), rangeOf("bytes=0-99,200-299"));

        check("several ranges are declined by serving the whole representation",
              true, multiple instanceof DeliveryPlan.Streamed);
    }

    // ── Presentation ──────────────────────────────────────────────────────────

    /**
     * 🎁 SVG saves rather than shows; other images show; a non-ASCII name gains the RFC 5987 form
     * and an ASCII one does not.
     */
    private static void verifyDispositions() {
        System.out.println("\nContent disposition");

        check("SVG is an attachment — it is a script host by specification",
              ContentDispositions.ATTACHMENT,
              ContentDispositions.compose(MediaType.forString("image/svg+xml"), "logo.svg", false).type());
        check("markup is an attachment too",
              ContentDispositions.ATTACHMENT,
              ContentDispositions.compose(MediaType.forString("text/html"), "page.html", false).type());
        check("other images render inline — previews are the point",
              ContentDispositions.INLINE,
              ContentDispositions.compose(MediaType.forString("image/png"), "photo.png", false).type());
        check("PDF renders inline",
              ContentDispositions.INLINE,
              ContentDispositions.compose(MediaType.APPLICATION_PDF, "invoice.pdf", false).type());
        check("forcing a download overrides an otherwise viewable type",
              ContentDispositions.ATTACHMENT,
              ContentDispositions.compose(MediaType.forString("image/png"), "photo.png", true).type());

        String ascii = ContentDispositions
                .compose(MediaType.APPLICATION_PDF, "invoice.pdf", true).toString();

        check("an ASCII name carries one filename parameter", false, ascii.contains("filename*="));

        String unicode = ContentDispositions
                .compose(MediaType.APPLICATION_PDF, "рахунок.pdf", true).toString();

        check("a non-ASCII name gains the RFC 5987 form", true, unicode.contains("filename*="));

        DeliveryPlan svg = plannerOver(false).plan(
                new DeliverableFile(StorageKey.of("files/u/42/logo.svg"), "local", "logo.svg",
                                    MediaType.forString("image/svg+xml"), OBJECT_SIZE, DIGEST),
                DeliveryIntent.view());

        check("the planner applies the same rule",
              true, header(svg, HttpHeader.CONTENT_DISPOSITION).startsWith(ContentDispositions.ATTACHMENT));
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static DeliverableFile file() {
        return new DeliverableFile(StorageKey.of("files/u/42/report.pdf"), "local", "report.pdf",
                                   MediaType.APPLICATION_PDF, OBJECT_SIZE, DIGEST);
    }

    private static DeliveryPlanner plannerOver(boolean presigning) {
        FileStores stores = presigning ? presigningStores(LINK_TIME_TO_LIVE) : streamingStores();
        return new DeliveryPlanner(stores, new CacheSettings(null, null, SAFETY_MARGIN));
    }

    private static FileStores streamingStores() {
        return new StandardFileStores(new StubFileStore("local", null));
    }

    private static FileStores presigningStores(Duration timeToLive) {
        return new StandardFileStores(new StubFileStore("local", timeToLive));
    }

    private static FileStores fileStores(boolean exposeChoice) {
        StorageSettings settings = new StorageSettings(
                StorageProvider.LOCAL, "./target/smoke-uploads", 0, null, null,
                Map.of("archive", BackendSettings.local("archive", "./target/smoke-archive")),
                exposeChoice, false, null, null);

        return new StandardFileStores(settings, List.of(new LocalFileStoreFactory()));
    }

    private static DeliveryIntent conditionalOn(String entityTag) {
        Headers headers = new Headers();

        headers.setMethod(HttpMethod.GET);
        headers.setHeader(HttpHeader.IF_NONE_MATCH, entityTag);

        return new DeliveryIntent(false, DeliveryIntent.Audience.OWNER, headers);
    }

    private static DeliveryIntent rangeOf(String rangeHeader) {
        Headers headers = new Headers();

        headers.setMethod(HttpMethod.GET);
        headers.setHeader(HttpHeader.RANGE, rangeHeader);

        return new DeliveryIntent(false, DeliveryIntent.Audience.OWNER, headers);
    }

    private static String header(DeliveryPlan plan, HttpHeader header) {
        Object value = plan.headers().getHeader(header);
        return (value == null) ? "" : value.toString();
    }

    /**
     * 🎭 A backend that answers the one question the planner asks it — can you serve your own
     * bytes — and refuses everything else, since the planner must never read.
     */
    private record StubFileStore(String name, Duration linkTimeToLive) implements FileStore {

        @Override
        public String backendName() {
            return name;
        }

        @Override
        public Optional<DirectLink> resolveDirectLink(StorageKey key, Presentation presentation) {
            if (linkTimeToLive == null) {
                return Optional.empty();
            }

            return Optional.of(new DirectLink(URI.create("https://cdn.example.test/" + key.value()),
                                              linkTimeToLive));
        }

        @Override
        public StoredObject write(StorageKey key, Content content) {
            throw new StorageException("The planner must not write.");
        }

        @Override
        public Resource read(StorageKey key) {
            throw new StorageException("The planner must not read.");
        }

        @Override
        public ResourceSegment readRange(StorageKey key, Range range) {
            throw new StorageException("The planner must not read.");
        }

        @Override
        public ObjectDescription describe(StorageKey key) {
            throw new StorageException("The planner must not describe.");
        }

        @Override
        public void delete(StorageKey key) {
            throw new StorageException("The planner must not delete.");
        }
    }

    // ── Reporting ─────────────────────────────────────────────────────────────

    private static void rejects(String what, Runnable action) {
        try {
            action.run();
            fail("%s — nothing was thrown".formatted(what));
        } catch (RuntimeException exception) {
            pass(what);
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
