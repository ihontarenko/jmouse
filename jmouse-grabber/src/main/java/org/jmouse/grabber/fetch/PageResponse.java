package org.jmouse.grabber.fetch;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import org.jmouse.core.MediaType;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpStatus;

/**
 * 📬 What came back.
 *
 * <p>⚠️ <b>{@link #address()} is the FINAL address, after redirects</b>, and it is the one a relative
 * link resolves against. A grabber that resolves against the requested address produces wrong links on
 * every redirected page, and the pages it then fetches 404 for reasons that look like the site's
 * fault.</p>
 *
 * <p>⚠️ <b>A non-2xx status is a response, not a failure.</b> What a 404 or a 503 means belongs to the
 * run: one grabber treats a 404 as the end of a branch and another treats it as a reason to stop.
 * Only a transport failure — no connection, no answer, a body that could not be read — is an
 * exception.</p>
 *
 * @param address     where the answer actually came from
 * @param requested   what was asked for, which differs when a redirect was followed
 * @param status      the status
 * @param headers     the response headers
 * @param body        the bytes, exactly as they arrived
 * @param elapsed     how long the exchange took
 */
public record PageResponse(
        URI address,
        URI requested,
        HttpStatus status,
        Headers headers,
        byte[] body,
        Duration elapsed
) {

    public PageResponse {
        Objects.requireNonNull(address, "A response needs an address");
        Objects.requireNonNull(status, "A response needs a status");
        headers = headers == null ? new Headers() : headers;
        body = body == null ? new byte[0] : body;
        requested = requested == null ? address : requested;
        elapsed = elapsed == null ? Duration.ZERO : elapsed;
    }

    /**
     * 🎯 What the server said this is, or {@code null} when it said nothing.
     */
    public MediaType contentType() {
        return headers.getContentType();
    }

    /**
     * 🔤 The body as text, decoded with the charset the response declared.
     *
     * <p>Falls back to UTF-8, which is right far more often than the specification's Latin-1 default
     * and wrong in the same cases a browser would also be wrong.</p>
     */
    public String text() {
        return new String(body, charset());
    }

    /**
     * 🔤 The charset the response declared, or UTF-8.
     */
    public Charset charset() {
        MediaType contentType = contentType();

        if (contentType != null) {
            Charset declared = contentType.getCharset();

            if (declared != null) {
                return declared;
            }
        }

        return StandardCharsets.UTF_8;
    }

    /**
     * ✅ 2xx.
     */
    public boolean isSuccessful() {
        int code = status.getCode();
        return code >= 200 && code < 300;
    }

    /**
     * 🙅 4xx — the client asked for something wrong, and asking again changes nothing.
     */
    public boolean isClientError() {
        int code = status.getCode();
        return code >= 400 && code < 500;
    }

    /**
     * 💥 5xx — the server failed, and asking again might well work.
     */
    public boolean isServerError() {
        return status.getCode() >= 500;
    }

    /**
     * 🔁 Whether this was redirected somewhere else along the way.
     */
    public boolean wasRedirected() {
        return !address.equals(requested);
    }

    public int size() {
        return body.length;
    }

    @Override
    public String toString() {
        return status.getCode() + " " + address + " (" + body.length + " bytes)";
    }

}
