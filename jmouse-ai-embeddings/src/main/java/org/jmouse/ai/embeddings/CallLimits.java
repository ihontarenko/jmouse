package org.jmouse.ai.embeddings;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Explicit per-call resource ceilings supplied by the adopter's managed policy. */
public record CallLimits(int maximumInputs, int maximumInputCodePoints, int maximumTotalCodePoints,
        int timeoutMilliseconds, int maximumResponseBytes) {

    public CallLimits {
        if (maximumInputs < 1 || maximumInputs > 512 || maximumInputCodePoints < 1 || maximumInputCodePoints > 1048576
                || maximumTotalCodePoints < 1 || maximumTotalCodePoints > 4194304
                || timeoutMilliseconds < 10 || timeoutMilliseconds > 120000
                || maximumResponseBytes < 1024 || maximumResponseBytes > 67108864) {
            throw new IllegalArgumentException("Explicit bounded embedding call limits are required.");
        }
    }

    public void validate(List<String> inputs) {
        long total = 0;
        if (inputs.isEmpty() || inputs.size() > maximumInputs) {
            throw new IllegalArgumentException("Embedding input count exceeds the configured ceiling.");
        }
        for (String input : inputs) {
            if (input == null || input.isEmpty() || !StandardCharsets.UTF_8.newEncoder().canEncode(input)) {
                throw new IllegalArgumentException("Embedding inputs must be nonempty valid Unicode.");
            }
            int count = input.codePointCount(0, input.length());
            if (count > maximumInputCodePoints) {
                throw new IllegalArgumentException("Embedding input exceeds the configured Unicode ceiling.");
            }
            total += count;
        }
        if (total > maximumTotalCodePoints) {
            throw new IllegalArgumentException("Embedding batch exceeds the configured Unicode ceiling.");
        }
    }
}
