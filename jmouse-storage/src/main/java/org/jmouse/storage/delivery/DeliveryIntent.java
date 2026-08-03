package org.jmouse.storage.delivery;

import org.jmouse.http.Headers;
import org.jmouse.http.HttpHeader;
import org.jmouse.http.HttpMethod;
import org.jmouse.http.Range;

import java.util.List;

/**
 * 🙋 What the client asked for.
 *
 * <p>Everything a delivery decision depends on that comes from the request rather than from the
 * file: whether to show or save, which bytes, what the client already holds, and whether the file
 * is being served publicly or to its owner — the last one deciding how long a cache may keep it.</p>
 *
 * <p>Deliberately not a servlet request. The planner is a pure function so that "what does this
 * request produce" is a table of inputs to expected outputs rather than something only observable
 * through a running server.</p>
 *
 * @param forceDownload  {@code true} to save rather than show, whatever the type allows
 * @param audience       who the file is being served to, which sets the cache lifetime
 * @param requestHeaders headers as received, for conditional evaluation and range parsing;
 *                       {@code null} when there is no request to speak of
 */
public record DeliveryIntent(boolean forceDownload, Audience audience, Headers requestHeaders) {

    /**
     * 👥 Who a file is being served to.
     */
    public enum Audience {

        /**
         * 🔒 Its owner, over an authenticated route. Never shared, so never cached publicly.
         */
        OWNER,

        /**
         * 🌍 Anyone holding a public link.
         */
        PUBLIC
    }

    /**
     * 🏗️ Fill in the audience nobody named.
     */
    public DeliveryIntent {
        audience = (audience == null) ? Audience.OWNER : audience;
    }

    /**
     * 🙋 An owner viewing their own file inline, with no conditional headers.
     *
     * @return the intent
     */
    public static DeliveryIntent view() {
        return new DeliveryIntent(false, Audience.OWNER, null);
    }

    /**
     * 🙋 An owner downloading their own file.
     *
     * @return the intent
     */
    public static DeliveryIntent download() {
        return new DeliveryIntent(true, Audience.OWNER, null);
    }

    /**
     * 📏 The range the client asked for, if it asked for exactly one.
     *
     * <p>A request naming several ranges gets {@code null}, and the planner serves the whole file.
     * Declining a {@code Range} by returning the complete representation is explicitly allowed, and
     * it is the honest answer here: a multipart range response is a body format the storage layer
     * has no business assembling, and half-answering by serving only the first range would give a
     * client bytes it did not ask for under a status saying it did.</p>
     *
     * @return the single requested range, or {@code null}
     */
    public Range range() {
        if (requestHeaders == null) {
            return null;
        }

        Object rawRange = requestHeaders.getHeader(HttpHeader.RANGE);

        if (rawRange == null) {
            return null;
        }

        try {
            List<Range> ranges = Range.parseRanges(rawRange.toString());
            return (ranges.size() == 1) ? ranges.getFirst() : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * ❓ Whether the client sent anything worth evaluating a precondition against.
     *
     * @return {@code true} when a conditional header is present
     */
    public boolean isConditional() {
        return requestHeaders != null
                && (requestHeaders.getHeader(HttpHeader.IF_NONE_MATCH) != null
                || requestHeaders.getHeader(HttpHeader.IF_MODIFIED_SINCE) != null);
    }

    /**
     * 📨 The request headers, or an empty {@code GET} when the caller supplied none.
     *
     * <p>Defaulting to {@code GET} rather than to nothing keeps precondition evaluation — which
     * treats safe and unsafe methods differently — from having to special-case an absent method.</p>
     *
     * @return headers safe to hand to a precondition evaluator
     */
    public Headers requestHeadersOrEmpty() {
        if (requestHeaders != null) {
            return requestHeaders;
        }

        Headers headers = new Headers();
        headers.setMethod(HttpMethod.GET);

        return headers;
    }
}
