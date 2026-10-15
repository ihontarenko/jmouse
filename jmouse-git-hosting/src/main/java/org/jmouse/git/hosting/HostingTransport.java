package org.jmouse.git.hosting;

import java.net.URI;
import java.util.Map;

/**
 * Injectable bounded JSON GET. An implementation returns every status as a response rather than throwing, so the
 * hosting can read rate-limit headers off a refusal; it throws {@link HostingException} only when there is no
 * response to return — {@code TIMEOUT}, {@code TRANSPORT}, {@code RESPONSE_LIMIT}, {@code INVALID_RESPONSE} or
 * {@code CONFIGURATION}.
 *
 * <p>Implementations must not follow redirects, so credentials are never carried to another address, and must not
 * put a header value into any message.
 */
@FunctionalInterface
public interface HostingTransport {

    /**
     * Sends one GET.
     *
     * @param endpoint the absolute address, query included
     * @param headers  request headers, credentials among them
     * @param limits   the request's ceilings
     * @return the status, headers and — for a success — the parsed body
     */
    HostingHttpResponse get(URI endpoint, Map<String, String> headers, HostingLimits limits);
}
