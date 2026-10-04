package org.jmouse.ai.embeddings;

import java.net.URI;
import java.util.List;
import org.jmouse.ai.provider.ProviderSettings;

/** Counts planned embedding inputs without generating vectors. Exactness is an explicit result. */
public interface TokenCounter {
    enum Quality { EXACT, APPROXIMATE, UNKNOWN }
    enum Strategy { TEI, CODE_POINT_ESTIMATE, UNKNOWN }
    record Policy(Strategy strategy, String tokenizer, String tokenizerRevision, URI endpoint,
            double codePointsPerToken, int additionalTokensPerInput) {
        public Policy {
            if (strategy == null || tokenizer == null || tokenizer.isBlank() || tokenizer.length() > 128 || tokenizerRevision == null
                    || tokenizerRevision.isBlank() || tokenizerRevision.length() > 128 || !Double.isFinite(codePointsPerToken)
                    || codePointsPerToken < 0.01 || codePointsPerToken > 1000
                    || additionalTokensPerInput < 0 || additionalTokensPerInput > 10000) {
                throw new IllegalArgumentException("Explicit tokenizer identity/revision and bounded estimation policy are required.");
            }
            if (strategy == Strategy.TEI && endpoint == null) {
                throw new IllegalArgumentException("TEI tokenization requires an explicit endpoint.");
            }
            if (endpoint != null) {
                if (endpoint.toString().length() > 255) {
                    throw new IllegalArgumentException("Tokenizer endpoint exceeds its configured bounds.");
                }
                EmbeddingTransport.requireEndpoint(endpoint.toString());
            }
        }
    }
    /** Counts preserve input order. Unknown entries have no invented numeric count. */
    record Count(List<Long> perInput, Quality quality) {
        public Count {
            perInput = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(perInput));
            if (quality == null || (quality == Quality.UNKNOWN ? perInput.stream().anyMatch(count -> count != null)
                    : perInput.stream().anyMatch(count -> count == null || count < 0))) {
                throw new IllegalArgumentException("Token count numbers must agree with their declared quality.");
            }
        }
    }
    Count count(ProviderSettings settings, List<String> inputs, Policy policy, CallLimits limits);
}
