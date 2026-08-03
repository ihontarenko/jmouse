package org.jmouse.storage.delivery;

import org.jmouse.http.Headers;
import org.jmouse.http.HttpStatus;

import java.net.URI;

/**
 * 📦 What to do with a request for a file — a closed set, so a renderer that handles every case is
 * a renderer that cannot be surprised.
 *
 * <p>Each variant carries its own resolved {@link #headers()}, already including status, content
 * type, disposition, cache lifetime and entity tag. That is the point of planning separately from
 * rendering: the arithmetic that is easy to get wrong — a redirect's lifetime against its
 * signature's, a content range against an object's length — happens once, in a value that can be
 * asserted on without an HTTP server anywhere in sight.</p>
 *
 * <p>Nothing here holds bytes or a stream. A plan says <em>what</em> to serve; fetching it is the
 * renderer's job, which keeps the planner free of input and output entirely.</p>
 */
public sealed interface DeliveryPlan {

    /**
     * 📨 Response headers this plan resolves to, status included.
     *
     * @return the headers
     */
    Headers headers();

    /**
     * 📄 The file the plan is about.
     *
     * @return the file
     */
    DeliverableFile file();

    /**
     * 🔗 Send the client to the backend, which serves the bytes itself.
     *
     * <p>Chosen when the backend can produce a direct link. The application never sees the bytes,
     * which is the whole gain — and why the headers a client would have received are signed into
     * the link rather than attached to this response.</p>
     *
     * @param file     the file being delivered
     * @param location the signed, publicly reachable URL
     * @param headers  resolved response headers, status {@code 302}
     */
    record Redirected(DeliverableFile file, URI location, Headers headers) implements DeliveryPlan {
    }

    /**
     * 📤 Stream the whole object through the application.
     *
     * @param file    the file being delivered
     * @param headers resolved response headers, status {@code 200}
     */
    record Streamed(DeliverableFile file, Headers headers) implements DeliveryPlan {
    }

    /**
     * ✂️ Stream part of the object, so a player can seek without waiting for the rest.
     *
     * @param file    the file being delivered
     * @param start   first byte to send, inclusive
     * @param end     last byte to send, inclusive
     * @param headers resolved response headers, status {@code 206} and a truthful content range
     */
    record PartiallyStreamed(DeliverableFile file, long start, long end, Headers headers)
            implements DeliveryPlan {

        /**
         * 📏 How many bytes the response body carries.
         *
         * @return the segment length
         */
        public long length() {
            return end - start + 1;
        }
    }

    /**
     * ♻️ The client already holds this exact object.
     *
     * @param file    the file that was asked for
     * @param headers resolved response headers, status {@code 304} and no body
     */
    record NotModified(DeliverableFile file, Headers headers) implements DeliveryPlan {
    }

    /**
     * 🚫 The requested range cannot exist in an object this size.
     *
     * <p>Refused rather than quietly widened to the whole file: a client asking for bytes past the
     * end has miscalculated, and answering {@code 200} with everything hides that while sending it
     * far more than it wanted.</p>
     *
     * @param file    the file that was asked for
     * @param headers resolved response headers, status {@code 416} and the object's real length
     */
    record RangeNotSatisfiable(DeliverableFile file, Headers headers) implements DeliveryPlan {
    }

    /**
     * 🔢 The status this plan resolves to.
     *
     * @return the response status
     */
    default HttpStatus status() {
        return headers().getStatus();
    }
}
