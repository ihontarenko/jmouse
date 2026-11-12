package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderJsonExchange;
import org.jmouse.ai.provider.ProviderSettings;
import org.jmouse.http.client.HttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * An embedding provider called over an HTTP transport, as every provider's JSON is ({@link ProviderJsonExchange}); what
 * went wrong said as an {@link EmbeddingException}. The transport it is given never follows a redirect.
 */
public final class HttpEmbeddingTransport implements EmbeddingTransport {

    private final ProviderJsonExchange exchange;

    public HttpEmbeddingTransport(HttpTransport transport) {
        this.exchange = new ProviderJsonExchange(transport);
    }

    @Override
    public Object post(URI endpoint, ProviderSettings settings, Object body, CallLimits limits) {
        return post(endpoint, settings, body, EmbeddingTransport.bearerHeaders(settings), limits);
    }

    @Override
    public Object post(URI endpoint, ProviderSettings settings, Object body, Map<String, String> headers, CallLimits limits) {
        return exchange.post(EmbeddingTransport.requireEndpoint(endpoint.toString()), body, headers,
                Duration.ofMillis(limits.timeoutMilliseconds()), limits.maximumResponseBytes(), HttpEmbeddingTransport::refusal);
    }

    private static EmbeddingException refusal(ProviderJsonExchange.Failure failure, Integer status) {
        return new EmbeddingException(switch (failure) {
            case CONFIGURATION -> EmbeddingException.Reason.CONFIGURATION;
            case TIMEOUT -> EmbeddingException.Reason.TIMEOUT;
            case TRANSPORT -> EmbeddingException.Reason.TRANSPORT;
            case REFUSED -> EmbeddingException.Reason.PROVIDER_REFUSED;
            case UNREADABLE -> EmbeddingException.Reason.INVALID_RESPONSE;
            case RESPONSE_LIMIT -> EmbeddingException.Reason.RESPONSE_LIMIT;
        }, status);
    }
}
