package org.jmouse.ai.reranking;

import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderSettings;

import java.util.Objects;

/**
 * One bounded rerank call. A protocol owns wire translation; a transport owns HTTP.
 * Retries, scheduling, persistence and provider routing remain the caller's responsibility.
 */
public final class HttpRerankModel implements RerankModel {

    private final RerankProtocol  protocol;
    private final RerankTransport transport;

    public HttpRerankModel(RerankProtocol protocol, RerankTransport transport) {
        this.protocol = Objects.requireNonNull(protocol, "Rerank protocol is required.");
        this.transport = Objects.requireNonNull(transport, "Rerank transport is required.");
    }

    @Override
    public String code() {
        return protocol.code();
    }

    @Override
    public boolean supports(String providerName) {
        return protocol.supports(providerName);
    }

    @Override
    public Response rerank(ProviderSettings settings, Request request, RerankLimits limits) {
        Objects.requireNonNull(settings, "Provider settings are required.");
        Objects.requireNonNull(request, "Rerank request is required.");
        Objects.requireNonNull(limits, "Rerank limits are required.");
        settings.requireCapability(AiCapability.RERANK);
        requireConfiguration(settings);
        limits.validate(request);

        var endpoint = RerankTransport.requireEndpoint(settings.apiUrl());
        Object response = transport.post(endpoint, protocol.encode(settings, request), protocol.headers(settings), limits);
        return RerankResponseReader.validated(protocol.decode(response, settings, request), request);
    }

    private void requireConfiguration(ProviderSettings settings) {
        if (!supports(settings.providerName()) || settings.model() == null || settings.model().isBlank()
                || (protocol.requiresApiKey(settings.providerName()) && !settings.hasApiKey())) {
            throw new RerankException(RerankException.Reason.CONFIGURATION, null);
        }
    }
}
