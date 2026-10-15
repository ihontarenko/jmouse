package org.jmouse.git.hosting;

import java.net.http.HttpHeaders;

/**
 * What a {@link HostingTransport} brings back from one GET.
 *
 * @param status  the HTTP status
 * @param headers the response headers; looked up case-insensitively
 * @param body    the parsed JSON body ({@code Map}, {@code List}, string, number, boolean) for a success status;
 *                always {@code null} for any other status, whose body is never read into memory
 */
public record HostingHttpResponse(int status, HttpHeaders headers, Object body) {

    public HostingHttpResponse {
        if (headers == null) {
            throw new IllegalArgumentException("A hosting response needs its headers, even when there are none.");
        }
    }

    /** Whether the status is a 2xx success. */
    public boolean successful() {
        return status >= 200 && status < 300;
    }
}
