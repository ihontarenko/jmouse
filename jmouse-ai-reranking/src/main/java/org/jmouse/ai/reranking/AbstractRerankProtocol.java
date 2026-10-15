package org.jmouse.ai.reranking;

import java.util.Objects;
import java.util.Set;

/** Shared immutable protocol identity, supported providers and credential requirements. */
public abstract class AbstractRerankProtocol implements RerankProtocol {

    private final String      code;
    private final Set<String> providers;
    private final Set<String> authenticatedProviders;

    protected AbstractRerankProtocol(String code, Set<String> providers, Set<String> authenticatedProviders) {
        this.code = Objects.requireNonNull(code, "Protocol code is required.");
        this.providers = Set.copyOf(providers);
        this.authenticatedProviders = Set.copyOf(authenticatedProviders);
        if (code.isBlank() || providers.isEmpty() || providers.stream().anyMatch(String::isBlank)
                || !providers.containsAll(authenticatedProviders)) {
            throw new IllegalArgumentException("A protocol requires a code, supported providers and a valid credential subset.");
        }
    }

    @Override
    public final String code() {
        return code;
    }

    @Override
    public final boolean supports(String providerName) {
        return providerName != null && providers.contains(providerName);
    }

    @Override
    public final boolean requiresApiKey(String providerName) {
        return providerName != null && authenticatedProviders.contains(providerName);
    }
}
