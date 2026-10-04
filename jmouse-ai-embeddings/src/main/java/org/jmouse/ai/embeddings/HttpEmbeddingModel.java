package org.jmouse.ai.embeddings;

import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderSettings;

import java.util.Objects;

/**
 * One bounded embedding call. A protocol owns wire translation; a transport owns HTTP.
 * Retries, scheduling, persistence and provider routing remain the caller's responsibility.
 */
public final class HttpEmbeddingModel implements EmbeddingModel {

    private final EmbeddingProtocol protocol;
    private final EmbeddingTransport transport;

    public HttpEmbeddingModel(EmbeddingProtocol protocol, EmbeddingTransport transport) {
        this.protocol = Objects.requireNonNull(protocol, "Embedding protocol is required.");
        this.transport = Objects.requireNonNull(transport, "Embedding transport is required.");
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
    public boolean supportsInputType(InputType inputType) {
        return protocol.supportsInputType(inputType);
    }

    @Override
    public Response embed(ProviderSettings settings, Request request, CallLimits limits) {
        Objects.requireNonNull(settings, "Provider settings are required.");
        Objects.requireNonNull(request, "Embedding request is required.");
        Objects.requireNonNull(limits, "Call limits are required.");
        settings.requireCapability(AiCapability.EMBEDDINGS);
        requireConfiguration(settings, request);
        limits.validate(request.inputs());

        var endpoint = EmbeddingTransport.requireEndpoint(settings.apiUrl());
        Object response = transport.post(endpoint, settings, protocol.encode(settings, request), protocol.headers(settings), limits);
        Response decoded = protocol.decode(response, settings, request);
        EmbeddingResponseReader.validate(decoded, request);

        return decoded;
    }

    private void requireConfiguration(ProviderSettings settings, Request request) {
        if (!supports(settings.providerName()) || !supportsInputType(request.inputType())
                || settings.model() == null || settings.model().isBlank()
                || (protocol.requiresApiKey(settings.providerName()) && !settings.hasApiKey())) {
            throw new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null);
        }
    }
}
