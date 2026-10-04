package org.jmouse.ai.embeddings;

import org.jmouse.ai.provider.ProviderSettings;

import java.util.List;
import java.util.Objects;

/** Vectorization independent of source documents, persistence and conversation tools. */
public interface EmbeddingModel {

    String code();

    boolean supports(String providerName);

    /** Non-default retrieval roles must be implemented explicitly; silently ignoring them is forbidden. */
    default boolean supportsInputType(InputType inputType) {
        return inputType == InputType.UNSPECIFIED;
    }

    enum InputType {
        UNSPECIFIED,
        DOCUMENT,
        QUERY
    }

    Response embed(ProviderSettings settings, Request request, CallLimits limits);

    /** Model revision is caller-managed; dimensions describe the expected vector space, not a wire override. */
    record Request(List<String> inputs, int dimensions, InputType inputType) {

        public Request(List<String> inputs, int dimensions) {
            this(inputs, dimensions, InputType.UNSPECIFIED);
        }

        public Request {
            inputType = Objects.requireNonNull(inputType, "Embedding input type is required.");
            inputs = List.copyOf(inputs);
            if (inputs.isEmpty() || inputs.stream().anyMatch(text -> text == null || text.isEmpty())
                    || dimensions < 1 || dimensions > 16384) {
                throw new IllegalArgumentException("Nonempty inputs and expected dimensions are required.");
            }
        }
    }

    /** Usage null means the protocol did not report it; zero never substitutes for unknown. */
    record Response(List<List<Double>> vectors, Long inputTokens) {
        public Response {
            vectors = vectors.stream().map(List::copyOf).toList();
            if (inputTokens != null && inputTokens < 0) {
                throw new IllegalArgumentException("Reported input usage cannot be negative.");
            }
        }
    }
}
