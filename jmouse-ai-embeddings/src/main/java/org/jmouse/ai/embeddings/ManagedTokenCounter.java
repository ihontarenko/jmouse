package org.jmouse.ai.embeddings;

import org.jmouse.ai.model.AiCapability;
import org.jmouse.ai.provider.ProviderSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Counts inputs without vectorization: explicit estimates, unknown counts, or native TEI tokenization.
 * Additional tokenizer integrations implement {@link TokenCounter}, independently of embedding protocols.
 */
public final class ManagedTokenCounter implements TokenCounter {

    private static final String TEI = "tei";
    private static final String INPUTS = "inputs";
    private static final String ADD_SPECIAL_TOKENS = "add_special_tokens";
    private static final String TOKEN_ID = "id";

    private final EmbeddingTransport transport;

    public ManagedTokenCounter(EmbeddingTransport transport) {
        this.transport = Objects.requireNonNull(transport, "Tokenization transport is required.");
    }

    @Override
    public Count count(ProviderSettings settings, List<String> inputs, Policy policy, CallLimits limits) {
        Objects.requireNonNull(settings, "Provider settings are required.");
        Objects.requireNonNull(policy, "Tokenization policy is required.");
        Objects.requireNonNull(limits, "Call limits are required.");
        settings.requireCapability(AiCapability.EMBEDDINGS);
        limits.validate(inputs);

        return switch (policy.strategy()) {
            case UNKNOWN -> new Count(Collections.nCopies(inputs.size(), null), Quality.UNKNOWN);
            case CODE_POINT_ESTIMATE -> estimate(inputs, policy);
            case TEI -> tokenize(settings, inputs, policy, limits);
        };
    }

    private Count estimate(List<String> inputs, Policy policy) {
        var counts = new ArrayList<Long>(inputs.size());
        for (String input : inputs) {
            long tokens = (long) Math.ceil(input.codePointCount(0, input.length()) / policy.codePointsPerToken());
            counts.add(Math.addExact(tokens, policy.additionalTokensPerInput()));
        }
        return new Count(counts, Quality.APPROXIMATE);
    }

    private Count tokenize(ProviderSettings settings, List<String> inputs, Policy policy, CallLimits limits) {
        var endpoint = EmbeddingTransport.requireEndpoint(policy.endpoint().toString());
        if (!TEI.equals(settings.providerName()) || !policy.tokenizer().equals(settings.model())
                || !EmbeddingTransport.sameOrigin(EmbeddingTransport.requireEndpoint(settings.apiUrl()), endpoint)) {
            throw new EmbeddingException(EmbeddingException.Reason.CONFIGURATION, null);
        }

        Object response = transport.post(endpoint, settings, Map.of(INPUTS, inputs, ADD_SPECIAL_TOKENS, true), limits);
        List<?> batches = EmbeddingResponseReader.array(response);
        if (batches.size() != inputs.size()) {
            throw EmbeddingResponseReader.invalidResponse();
        }

        var counts = new ArrayList<Long>(batches.size());
        for (Object batch : batches) {
            List<?> tokens = EmbeddingResponseReader.array(batch);
            for (Object token : tokens) {
                EmbeddingResponseReader.nonnegativeInteger(EmbeddingResponseReader.object(token).get(TOKEN_ID));
            }
            counts.add((long) tokens.size());
        }
        return new Count(counts, Quality.EXACT);
    }
}
