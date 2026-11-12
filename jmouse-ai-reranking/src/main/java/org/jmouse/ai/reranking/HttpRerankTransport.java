package org.jmouse.ai.reranking;

import org.jmouse.ai.provider.ProviderJsonExchange;
import org.jmouse.http.client.HttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * A reranking provider called over an HTTP transport, as every provider's JSON is ({@link ProviderJsonExchange}); what
 * went wrong said as a {@link RerankException}. The transport it is given never follows a redirect.
 */
public final class HttpRerankTransport implements RerankTransport {

    private final ProviderJsonExchange exchange;

    public HttpRerankTransport(HttpTransport transport) {
        this.exchange = new ProviderJsonExchange(transport);
    }

    @Override
    public Object post(URI endpoint, Object body, Map<String, String> headers, RerankLimits limits) {
        return exchange.post(RerankTransport.requireEndpoint(endpoint.toString()), body, headers,
                Duration.ofMillis(limits.timeoutMilliseconds()), limits.maximumResponseBytes(), HttpRerankTransport::refusal);
    }

    private static RerankException refusal(ProviderJsonExchange.Failure failure, Integer status) {
        return new RerankException(switch (failure) {
            case CONFIGURATION -> RerankException.Reason.CONFIGURATION;
            case TIMEOUT -> RerankException.Reason.TIMEOUT;
            case TRANSPORT -> RerankException.Reason.TRANSPORT;
            case REFUSED -> RerankException.Reason.PROVIDER_REFUSED;
            case UNREADABLE -> RerankException.Reason.INVALID_RESPONSE;
            case RESPONSE_LIMIT -> RerankException.Reason.RESPONSE_LIMIT;
        }, status);
    }
}
