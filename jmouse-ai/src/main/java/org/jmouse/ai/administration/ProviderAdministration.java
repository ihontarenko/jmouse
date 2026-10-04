package org.jmouse.ai.administration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jmouse.ai.model.AiCapability;

/** Shared management: provider says who, capabilities what, application purpose why. Credentials are write-only. */
public interface ProviderAdministration {
    record Configuration(String id, String provider, String model, String apiUrl, int maximumTokens,
            boolean active, boolean keyConfigured, Instant createdAt, Instant updatedAt, String purpose,
            Set<AiCapability> capabilities, long revision) {
        public Configuration {
            capabilities = Set.copyOf(capabilities);
        }
        public Configuration(String id, String provider, String model, String apiUrl, int maximumTokens,
                boolean active, boolean keyConfigured, Instant createdAt, Instant updatedAt, String purpose) {
            this(id, provider, model, apiUrl, maximumTokens, active, keyConfigured, createdAt, updatedAt,
                    purpose, Set.of(AiCapability.CHAT), 0);
        }
        public Configuration(String id, String provider, String model, String apiUrl, int maximumTokens,
                boolean active, boolean keyConfigured, Instant createdAt, Instant updatedAt) {
            this(id, provider, model, apiUrl, maximumTokens, active, keyConfigured, createdAt, updatedAt, null);
        }
    }

    /** Missing capabilities retains legacy CHAT semantics; a blank key retains the stored key on change. */
    record Draft(String provider, String model, String apiKey, String apiUrl, int maximumTokens, String purpose,
            Set<AiCapability> capabilities) {
        public Draft {
            capabilities = capabilities == null ? Set.of(AiCapability.CHAT) : Set.copyOf(capabilities);
            if (capabilities.isEmpty() || model == null || model.isBlank() || model.length() > 128
                    || maximumTokens < 1 || (purpose != null && purpose.trim().length() > 64)
                    || (apiUrl != null && apiUrl.length() > 255) || (apiKey != null && apiKey.length() > 255)) {
                throw new IllegalArgumentException("A bounded model, positive token limit and nonempty capabilities are required.");
            }
        }
        public Draft(String provider, String model, String apiKey, String apiUrl, int maximumTokens, String purpose) {
            this(provider, model, apiKey, apiUrl, maximumTokens, purpose, Set.of(AiCapability.CHAT));
        }
        public Draft(String provider, String model, String apiKey, String apiUrl, int maximumTokens) {
            this(provider, model, apiKey, apiUrl, maximumTokens, null);
        }
        public boolean carriesKey() {
            return apiKey != null && !apiKey.isBlank();
        }
        @Override
        public String toString() {
            return "Draft[provider=" + provider + ", model=" + model + ", capabilities=" + capabilities + "]";
        }
    }

    class RefusedException extends RuntimeException {
        public RefusedException(String message) {
            super(message);
        }
    }

    /** Vendor-wide ceiling; model declarations and installed operation adapters still need validation. */
    record SupportedProvider(String name, String defaultApiUrl, boolean requiresKey, String note,
            Set<AiCapability> capabilities) {
        public SupportedProvider {
            capabilities = Set.copyOf(capabilities);
        }
        public SupportedProvider(String name, String defaultApiUrl, boolean requiresKey, String note) {
            this(name, defaultApiUrl, requiresKey, note, Set.of(AiCapability.CHAT));
        }
    }

    List<String> supportedProviders();
    default List<SupportedProvider> describeSupportedProviders() {
        return supportedProviders().stream().map(name -> new SupportedProvider(name, null, true, null)).toList();
    }
    List<Configuration> configurations();
    Optional<Configuration> find(String id);
    Configuration add(Draft draft);
    Configuration change(String id, Draft draft);
    Configuration putInForce(String id);
    Configuration takeOutOfForce(String id);
    void discard(String id);

    static ProviderAdministration unavailable() {
        return new ProviderAdministration() {
            private static final String WHY = "Provider settings are not administered through database rows.";
            @Override
            public List<String> supportedProviders() {
                return List.of();
            }
            @Override
            public List<Configuration> configurations() {
                return List.of();
            }
            @Override
            public Optional<Configuration> find(String id) {
                return Optional.empty();
            }
            @Override
            public Configuration add(Draft draft) {
                throw new RefusedException(WHY);
            }
            @Override
            public Configuration change(String id, Draft draft) {
                throw new RefusedException(WHY);
            }
            @Override
            public Configuration putInForce(String id) {
                throw new RefusedException(WHY);
            }
            @Override
            public Configuration takeOutOfForce(String id) {
                throw new RefusedException(WHY);
            }
            @Override
            public void discard(String id) {
                throw new RefusedException(WHY);
            }
        };
    }
}
