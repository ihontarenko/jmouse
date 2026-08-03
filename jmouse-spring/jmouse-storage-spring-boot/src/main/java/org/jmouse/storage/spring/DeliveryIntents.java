package org.jmouse.storage.spring;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpHeader;
import org.jmouse.http.HttpMethod;
import org.jmouse.storage.delivery.DeliveryIntent;

/**
 * 🙋 Reads what a client asked for out of a servlet request.
 *
 * <p>Small enough that every product wrote it by hand, and identical enough that they wrote the
 * same thing — the same three headers copied across, in the same order, into the same value object.
 * A third product would have copied it again, and the first one to forget {@code Range} would have
 * shipped a video player that cannot seek, with nothing to point at as the bug.</p>
 *
 * <p>Only the headers a delivery decision actually reads are carried over. The planner is a pure
 * function precisely so that it never sees a request, so handing it the whole set would put a
 * servlet where the design says there is none.</p>
 */
public final class DeliveryIntents {

    private static final HttpHeader[] DELIVERY_HEADERS = {
            HttpHeader.RANGE,
            HttpHeader.IF_NONE_MATCH,
            HttpHeader.IF_MODIFIED_SINCE
    };

    private DeliveryIntents() {
    }

    /**
     * 🙋 What this request asked for.
     *
     * @param request       the incoming request
     * @param forceDownload {@code true} to save rather than show, whatever the type allows
     * @param audience      who the file is being served to, which sets the cache lifetime
     * @return the intent
     */
    public static DeliveryIntent of(HttpServletRequest request, boolean forceDownload,
                                    DeliveryIntent.Audience audience) {
        return new DeliveryIntent(forceDownload, audience, headersOf(request));
    }

    /**
     * 📨 The request's headers, as the protocol values the planner reads.
     *
     * @param request the incoming request
     * @return the headers a delivery decision depends on
     */
    private static Headers headersOf(HttpServletRequest request) {
        Headers headers = new Headers();

        headers.setMethod(HttpMethod.ofName(request.getMethod()));

        for (HttpHeader header : DELIVERY_HEADERS) {
            String value = request.getHeader(header.value());

            if (value != null) {
                headers.setHeader(header, value);
            }
        }

        return headers;
    }
}
