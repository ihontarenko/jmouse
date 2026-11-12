package org.jmouse.ai.provider;

import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.HttpCallException;
import org.jmouse.http.client.HttpReply;
import org.jmouse.http.client.HttpTransport;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * A provider's JSON call over an HTTP transport — what an embedding and a reranking provider are both called by: the body
 * written as JSON, the whole call timed and its answer read only up to its limit ({@link HttpCall}), the answer read as
 * JSON. What went wrong is said as one {@link Failure}, which the caller turns into its own exception — one that never
 * carries a credential or the caller's input.
 */
public final class ProviderJsonExchange {

    private static final String JSON = "application/json";

    /** Why a provider call gave no JSON answer. */
    public enum Failure {
        /** The call could not be made as configured: a header, the body. */
        CONFIGURATION,
        TIMEOUT,
        /** The connection failed, or the wait was interrupted. */
        TRANSPORT,
        /** The provider answered with a status that is no success. */
        REFUSED,
        /** Its answer is not JSON. */
        UNREADABLE,
        /** Its answer is longer than the call reads. */
        RESPONSE_LIMIT
    }

    private final ObjectMapper json = new ObjectMapper();
    private final HttpTransport transport;

    public ProviderJsonExchange(HttpTransport transport) {
        this.transport = transport;
    }

    /**
     * The provider's JSON answer to a body posted to it.
     *
     * @param refusal the caller's exception for a failure, with the status of a refused call (null for any other)
     */
    public Object post(URI endpoint, Object body, Map<String, String> headers, Duration timeout, long responseLimit,
                       BiFunction<Failure, Integer, RuntimeException> refusal) {
        HttpCall call;
        try {
            call = HttpCall.post(endpoint).header("Accept", JSON).headers(headers).body(json.writeValueAsBytes(body), JSON)
                    .timeout(timeout).responseLimit(responseLimit).build();
        } catch (JacksonException | IllegalArgumentException failure) {
            throw refusal.apply(Failure.CONFIGURATION, null);
        }
        HttpReply reply;
        try {
            reply = transport.exchange(call);
        } catch (HttpCallException failed) {
            throw refusal.apply(switch (failed.failure()) {
                case TIMEOUT -> Failure.TIMEOUT;
                case RESPONSE_LIMIT -> Failure.RESPONSE_LIMIT;
                case TRANSPORT, INTERRUPTED -> Failure.TRANSPORT;
            }, null);
        }
        if (!reply.successful()) {
            throw refusal.apply(Failure.REFUSED, reply.status());
        }
        try {
            return json.readValue(reply.body(), Object.class);
        } catch (JacksonException unreadable) {
            throw refusal.apply(Failure.UNREADABLE, null);
        }
    }
}
