package org.jmouse.git.hosting;

import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.HttpCallException;
import org.jmouse.http.client.HttpReply;
import org.jmouse.http.client.HttpTransport;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.time.Duration;
import java.util.Map;

/**
 * A hosting's GET over an HTTP transport: the whole call timed and its answer read only up to the limits
 * ({@link HttpCall}); every status an answer — a refusal's rate-limit headers are read off it —, the body parsed as JSON
 * for a success alone. Only when there is no answer is it a {@link HostingException}, which never repeats a header's
 * value. The transport it is given never follows a redirect.
 */
public final class HttpHostingTransport implements HostingTransport {

    private final ObjectMapper json = new ObjectMapper();
    private final HttpTransport transport;

    public HttpHostingTransport(HttpTransport transport) {
        this.transport = transport;
    }

    @Override
    public HostingHttpResponse get(URI endpoint, Map<String, String> headers, HostingLimits limits) {
        HttpCall call;
        try {
            call = HttpCall.get(endpoint).headers(headers).timeout(Duration.ofMillis(limits.timeoutMilliseconds()))
                    .responseLimit(limits.maximumResponseBytes()).build();
        } catch (IllegalArgumentException misconfigured) {
            // A rejected header value may be a credential; the diagnostic must not repeat it.
            throw new HostingException(HostingException.Reason.CONFIGURATION);
        }
        HttpReply reply;
        try {
            reply = transport.exchange(call);
        } catch (HttpCallException failed) {
            throw new HostingException(switch (failed.failure()) {
                case TIMEOUT -> HostingException.Reason.TIMEOUT;
                case RESPONSE_LIMIT -> HostingException.Reason.RESPONSE_LIMIT;
                case TRANSPORT, INTERRUPTED -> HostingException.Reason.TRANSPORT;
            });
        }
        return new HostingHttpResponse(reply.status(), HttpHeaders.of(reply.headers(), (name, value) -> true), bodyOf(reply));
    }

    /** A success's body as JSON; nothing for an empty one or for any other status, whose body is not the hosting's to read. */
    private Object bodyOf(HttpReply reply) {
        byte[] body = reply.body();
        if (!reply.successful() || body.length == 0) {
            return null;
        }
        try {
            return json.readValue(body, Object.class);
        } catch (JacksonException unreadable) {
            throw new HostingException(HostingException.Reason.INVALID_RESPONSE);
        }
    }
}
