package org.jmouse.grabber.fetch;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

import org.jmouse.http.Headers;
import org.jmouse.http.HttpMethod;

/**
 * 📨 What to ask a server for.
 *
 * <p>⚠️ It speaks {@code jmouse-http} rather than {@link String}: the method is an
 * {@link HttpMethod}, the headers are {@link Headers}. A grabber that models a method as text has
 * re-implemented a type this reactor already ships, and the drift starts there.</p>
 *
 * @param address the address to fetch
 * @param method  the method, {@link HttpMethod#GET} unless something says otherwise
 * @param headers what to send — the run's standing headers, already merged with any the visit added
 * @param body    a body for the methods that take one, or {@code null}
 * @param timeout how long to wait for the whole exchange
 */
public record PageRequest(URI address, HttpMethod method, Headers headers, byte[] body, Duration timeout) {

    public PageRequest {
        Objects.requireNonNull(address, "A request needs an address");
        method = method == null ? HttpMethod.GET : method;
        headers = headers == null ? new Headers() : headers;
    }

    /**
     * 📨 A plain GET with no headers of its own.
     */
    public static PageRequest get(URI address) {
        return new PageRequest(address, HttpMethod.GET, new Headers(), null, null);
    }

    /**
     * 📨 A GET carrying these headers and this timeout.
     */
    public static PageRequest get(URI address, Headers headers, Duration timeout) {
        return new PageRequest(address, HttpMethod.GET, headers, null, timeout);
    }

    public boolean hasBody() {
        return body != null && body.length > 0;
    }

    @Override
    public String toString() {
        return method + " " + address;
    }

}
