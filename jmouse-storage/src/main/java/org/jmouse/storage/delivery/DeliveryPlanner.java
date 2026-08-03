package org.jmouse.storage.delivery;

import org.jmouse.http.CacheControl;
import org.jmouse.http.ConditionalRequest;
import org.jmouse.http.ContentDisposition;
import org.jmouse.http.ETag;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpHeader;
import org.jmouse.http.HttpStatus;
import org.jmouse.http.PreconditionResult;
import org.jmouse.http.Range;
import org.jmouse.storage.DirectLink;
import org.jmouse.storage.FileStore;
import org.jmouse.storage.FileStores;
import org.jmouse.storage.Presentation;
import org.jmouse.storage.configuration.CacheSettings;
import org.jmouse.storage.support.ByteRanges;

import java.time.Duration;
import java.util.Optional;

/**
 * 🧭 Turns "who asked for what" into "what to send", once, for every product.
 *
 * <p>This is the genuinely subtle part of serving a file, and it was previously restated per
 * product — where two restatements are two chances to disagree. Written here as a pure function
 * over values, the whole of it becomes a table of input to expected output: no server to start, no
 * bytes to move, no framework to stand up.</p>
 *
 * <h3>The three rules that live here and nowhere else</h3>
 *
 * <p><strong>A redirect never outlives the signature it points at.</strong> Its cache lifetime is
 * the direct link's own time-to-live minus a safety margin, floored at zero — so a response cached
 * moments before the signature expires is not handed out already broken. It is also why a redirect
 * is never marked {@code immutable}: the target genuinely does change.</p>
 *
 * <p><strong>Presentation is composed identically on both paths.</strong> A direct link bakes its
 * headers into the signature long before any response is built, so the type and disposition are
 * resolved before the redirect-or-stream question is even asked. Composing them per path is how
 * one file ends up rendering inline from disk and downloading from an object store.</p>
 *
 * <p><strong>The entity tag is the object's digest.</strong> Already recorded during the write, so
 * conditional requests cost no extra column and no extra read — and two objects with identical
 * bytes get identical tags, which is exactly right.</p>
 *
 * <h3>Order of decisions</h3>
 *
 * <ol>
 *   <li>Preconditions, because a client that already holds the object should be told so before
 *       anything is signed or read.</li>
 *   <li>A direct link, because a backend serving its own bytes is always the cheaper answer — and
 *       a client following the redirect repeats its {@code Range} at the backend, which handles it
 *       natively.</li>
 *   <li>The range, which only applies once we know this application is doing the streaming.</li>
 * </ol>
 */
public class DeliveryPlanner {

    private static final String BYTES_UNIT           = "bytes";
    private static final String CONTENT_RANGE_FORMAT = "bytes %d-%d/%d";
    private static final String UNSATISFIED_FORMAT   = "bytes */%d";

    private final FileStores    fileStores;
    private final CacheSettings cacheSettings;

    /**
     * 🏗️ Plan deliveries out of a set of backends, under a cache policy.
     *
     * @param fileStores    every backend the application has
     * @param cacheSettings how long clients may hold what is served
     */
    public DeliveryPlanner(FileStores fileStores, CacheSettings cacheSettings) {
        this.fileStores    = fileStores;
        this.cacheSettings = cacheSettings;
    }

    /**
     * 🧭 Decide what to send.
     *
     * @param file   the file being asked for
     * @param intent what the client asked for
     * @return the plan, with every response header already resolved
     */
    public DeliveryPlan plan(DeliverableFile file, DeliveryIntent intent) {
        Presentation presentation = presentationFor(file, intent);
        ETag         entityTag    = entityTagFor(file);

        if (isNotModified(entityTag, intent)) {
            return notModified(file, entityTag, intent);
        }

        Optional<DirectLink> directLink = resolveDirectLink(file, presentation);

        if (directLink.isPresent()) {
            return redirected(file, directLink.get(), entityTag, intent);
        }

        Range range = intent.range();

        if (range == null) {
            return streamed(file, presentation, entityTag, intent);
        }

        return partiallyStreamed(file, range, presentation, entityTag, intent);
    }

    /**
     * 🎁 How this file should be presented, resolved before the delivery route is chosen.
     *
     * @param file   the file being delivered
     * @param intent what the client asked for
     * @return the presentation both paths use
     */
    public Presentation presentationFor(DeliverableFile file, DeliveryIntent intent) {
        ContentDisposition disposition = ContentDispositions.compose(
                file.contentType(), file.presentedFilename(), intent.forceDownload());

        return new Presentation(file.contentType(), disposition);
    }

    /**
     * 🔐 The entity tag for a file, or {@code null} when its digest is not yet known.
     *
     * <p>Strong rather than weak: the digest identifies the bytes exactly, so two representations
     * sharing a tag really are byte-identical — which is what a strong validator promises and what
     * makes a range request safe to answer conditionally.</p>
     *
     * @param file the file
     * @return the tag, or {@code null}
     */
    private ETag entityTagFor(DeliverableFile file) {
        return file.hasDigest() ? ETag.strong(file.sha256()) : null;
    }

    /**
     * ♻️ Whether the client already holds this exact object.
     *
     * <p>Delegated to the shared conditional-request evaluator rather than reimplemented, so the
     * RFC precedence between entity tags and modification dates is the one everything else in the
     * framework uses.</p>
     *
     * @param entityTag current tag, or {@code null} when unknown
     * @param intent    what the client asked for
     * @return {@code true} when nothing needs to be sent
     */
    private boolean isNotModified(ETag entityTag, DeliveryIntent intent) {
        if (entityTag == null || !intent.isConditional()) {
            return false;
        }

        PreconditionResult result = ConditionalRequest.evaluate(
                intent.requestHeadersOrEmpty(), new Headers(), 0, entityTag);

        return result == PreconditionResult.NOT_MODIFIED_304;
    }

    /**
     * 🔗 Ask the backend holding this object whether it can serve its own bytes.
     *
     * <p>Routed by the backend recorded on the file rather than by whichever store is currently the
     * default, which is what keeps objects written before a migration readable after one.</p>
     *
     * @param file         the file being delivered
     * @param presentation headers to sign into the link
     * @return the link, or empty when this backend streams instead
     */
    private Optional<DirectLink> resolveDirectLink(DeliverableFile file, Presentation presentation) {
        FileStore fileStore = fileStores.require(file.backendName());
        return fileStore.resolveDirectLink(file.storageKey(), presentation);
    }

    private DeliveryPlan notModified(DeliverableFile file, ETag entityTag, DeliveryIntent intent) {
        Headers headers = new Headers();

        headers.setStatus(HttpStatus.NOT_MODIFIED);
        headers.setHeader(HttpHeader.ETAG, entityTag.toHeaderValue());
        headers.setHeader(HttpHeader.CACHE_CONTROL, streamedCacheControl(intent).toHeaderValue());

        return new DeliveryPlan.NotModified(file, headers);
    }

    private DeliveryPlan redirected(DeliverableFile file, DirectLink link, ETag entityTag,
                                    DeliveryIntent intent) {
        Headers headers = new Headers();

        headers.setStatus(HttpStatus.FOUND);
        headers.setHeader(HttpHeader.LOCATION, link.location().toString());
        headers.setHeader(HttpHeader.CACHE_CONTROL, redirectCacheControl(link, intent).toHeaderValue());

        if (entityTag != null) {
            headers.setHeader(HttpHeader.ETAG, entityTag.toHeaderValue());
        }

        return new DeliveryPlan.Redirected(file, link.location(), headers);
    }

    private DeliveryPlan streamed(DeliverableFile file, Presentation presentation, ETag entityTag,
                                  DeliveryIntent intent) {
        Headers headers = streamingHeaders(file, presentation, entityTag, intent);

        headers.setStatus(HttpStatus.OK);
        headers.setContentLength(file.sizeBytes());

        return new DeliveryPlan.Streamed(file, headers);
    }

    private DeliveryPlan partiallyStreamed(DeliverableFile file, Range range, Presentation presentation,
                                           ETag entityTag, DeliveryIntent intent) {
        ByteRanges.ByteRange resolved;

        try {
            resolved = ByteRanges.resolve(range, file.storageKey(), file.sizeBytes());
        } catch (RuntimeException exception) {
            return rangeNotSatisfiable(file, intent);
        }

        Headers headers = streamingHeaders(file, presentation, entityTag, intent);

        headers.setStatus(HttpStatus.PARTIAL_CONTENT);
        headers.setContentLength(resolved.length());
        headers.setHeader(HttpHeader.CONTENT_RANGE,
                          CONTENT_RANGE_FORMAT.formatted(resolved.start(), resolved.end(), file.sizeBytes()));

        return new DeliveryPlan.PartiallyStreamed(file, resolved.start(), resolved.end(), headers);
    }

    private DeliveryPlan rangeNotSatisfiable(DeliverableFile file, DeliveryIntent intent) {
        Headers headers = new Headers();

        headers.setStatus(HttpStatus.RANGE_NOT_SATISFIABLE);
        headers.setHeader(HttpHeader.ACCEPT_RANGES, BYTES_UNIT);
        headers.setHeader(HttpHeader.CONTENT_RANGE, UNSATISFIED_FORMAT.formatted(file.sizeBytes()));
        headers.setHeader(HttpHeader.CACHE_CONTROL, streamedCacheControl(intent).toHeaderValue());

        return new DeliveryPlan.RangeNotSatisfiable(file, headers);
    }

    /**
     * 📨 Headers common to both streaming plans.
     *
     * <p>{@code Accept-Ranges} is advertised on the full response too, because that is where a
     * client looks before deciding it may seek at all.</p>
     */
    private Headers streamingHeaders(DeliverableFile file, Presentation presentation, ETag entityTag,
                                     DeliveryIntent intent) {
        Headers headers = new Headers();

        headers.setContentType(presentation.contentType());
        headers.setContentDisposition(presentation.contentDisposition());
        headers.setHeader(HttpHeader.ACCEPT_RANGES, BYTES_UNIT);
        headers.setHeader(HttpHeader.CACHE_CONTROL, streamedCacheControl(intent).toHeaderValue());

        if (entityTag != null) {
            headers.setHeader(HttpHeader.ETAG, entityTag.toHeaderValue());
        }

        return headers;
    }

    /**
     * ⏳ How long a redirect may be cached: the link's own lifetime, less the safety margin, never
     * below zero and never immutable.
     *
     * @param link   the direct link being handed out
     * @param intent what the client asked for
     * @return the cache policy for the redirect
     */
    private CacheControl redirectCacheControl(DirectLink link, DeliveryIntent intent) {
        Duration maxAge = link.timeToLive().minus(cacheSettings.redirectSafetyMargin());

        if (maxAge.isNegative()) {
            maxAge = Duration.ZERO;
        }

        return visibility(intent).maxAge(maxAge);
    }

    /**
     * ⏳ How long streamed bytes may be cached.
     *
     * <p>Unlike a redirect, these have a lifetime of their own rather than one bounded by a
     * signature — so a publicly shared file is safe to mark {@code immutable}.</p>
     *
     * @param intent what the client asked for
     * @return the cache policy for the body
     */
    private CacheControl streamedCacheControl(DeliveryIntent intent) {
        if (intent.audience() == DeliveryIntent.Audience.PUBLIC) {
            return CacheControl.empty().cachePublic().maxAge(cacheSettings.streamedPublicMaxAge()).immutable();
        }

        return CacheControl.empty().cachePrivate().maxAge(cacheSettings.streamedPrivateMaxAge());
    }

    private CacheControl visibility(DeliveryIntent intent) {
        return (intent.audience() == DeliveryIntent.Audience.PUBLIC)
                ? CacheControl.empty().cachePublic()
                : CacheControl.empty().cachePrivate();
    }
}
