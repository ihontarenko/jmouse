package org.jmouse.ai.embeddings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderSettings;

/** Exact model-server tokenization, explicitly approximate policy, or an honest unknown. */
public final class ManagedTokenCounter implements TokenCounter {
    private final EmbeddingTransport transport;
    public ManagedTokenCounter(EmbeddingTransport transport) {
        this.transport = java.util.Objects.requireNonNull(transport);
    }
    @Override
    public Count count(ProviderSettings settings, List<String> inputs, Policy policy, CallLimits limits) {
        settings.requireCapability(AiCapability.EMBEDDINGS);
        limits.validate(inputs);
        if (policy.strategy() == Strategy.UNKNOWN) {
            return new Count(java.util.Collections.nCopies(inputs.size(), null), Quality.UNKNOWN);
        }
        var counts = new ArrayList<Long>();
        if (policy.strategy() == Strategy.CODE_POINT_ESTIMATE) {
            for (String input : inputs) {
                counts.add(Math.addExact((long) Math.ceil(input.codePointCount(0, input.length()) / policy.codePointsPerToken()),
                        policy.additionalTokensPerInput()));
            }
            return new Count(counts, Quality.APPROXIMATE);
        }
        var endpoint = EmbeddingTransport.requireEndpoint(policy.endpoint().toString());
        if (!"tei".equals(settings.providerName()) || !policy.tokenizer().equals(settings.model())
                || !EmbeddingTransport.sameOrigin(EmbeddingTransport.requireEndpoint(settings.apiUrl()), endpoint)) {
            throw new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null);
        }
        Object answer = transport.post(endpoint, settings, Map.of("inputs", inputs, "add_special_tokens", true), limits);
        try {
            var batches = (List<?>) answer;
            if (batches.size() != inputs.size()) {
                throw new IllegalArgumentException();
            }
            for (Object batch : batches) {
                var tokens = (List<?>) batch;
                for (Object token : tokens) {
                    if (!(token instanceof Map<?, ?> record) || record.get("id") == null) {
                        throw new IllegalArgumentException();
                    }
                    ProtocolEmbeddingModel.whole(record.get("id"));
                }
                counts.add((long) tokens.size());
            }
        } catch (RuntimeException failure) {
            throw new EmbeddingException(EmbeddingException.Reason.INVALID_RESPONSE, null);
        }
        return new Count(counts, Quality.EXACT);
    }
}
