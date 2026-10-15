package org.jmouse.ai.jpa.credential;

import org.jmouse.vault.SecretName;
import org.jmouse.vault.SecretValue;
import org.jmouse.vault.credential.CredentialKind;
import org.jmouse.vault.credential.CredentialKind.Field;
import org.jmouse.vault.credential.Credentials;
import org.jmouse.vault.credential.ResolvedCredential;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 🗝️ Where a provider configuration's URL and key live when the installation has a vault: a credential of kind
 * {@value #KIND}, named {@code ai/providers/<configuration id>} when they were entered with the configuration,
 * or any other name when several configurations share one account ({@code ai/openai/primary}).
 *
 * <p>The row keeps only the name. Its key is therefore one more secret on the Vault screen — rotated, revealed
 * and audited there like every other — instead of a column only this library knows how to open.
 *
 * <p>⚠️ Vault writes run in a transaction of their own ({@link Transactions}), before the row's: a failure after
 * them leaves a credential nobody points at, which the Vault screen shows, rather than a row pointing at nothing.
 */
public final class ProviderCredentials {

    public static final String KIND     = "ai-provider";
    public static final String BASE_URL = "base-url";
    public static final String API_KEY  = "api-key";

    private static final String OWN_PREFIX = "ai/providers/";

    /** Runs vault work inside a transaction the vault's entity manager can join. */
    public interface Transactions {

        <T> T call(Supplier<T> work);
    }

    /** What a credential answers for a call: the address, or null for the provider's own, and the key, or null. */
    public record Resolved(String apiUrl, String apiKey) {

        @Override
        public String toString() {
            return "Resolved[apiUrl=" + apiUrl + ", apiKey=" + (apiKey == null ? "<absent>" : "<redacted>") + "]";
        }
    }

    private final Credentials  credentials;
    private final Transactions transactions;

    public ProviderCredentials(Credentials credentials, Transactions transactions) {
        this.credentials  = Objects.requireNonNull(credentials, "credentials");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    /** The shape a screen draws for an AI provider account; neither field is required — a local model has neither. */
    public static CredentialKind kind() {
        return new CredentialKind(KIND, "AI provider", "The address and key of an AI model provider account.",
                List.of(Field.plain(BASE_URL, "Base URL", false, "Empty for the provider's own address."),
                        Field.secret(API_KEY, "API key", false, "Empty for a provider that takes none, such as a local model.")),
                false);
    }

    public static String ownName(String configurationId) {
        return OWN_PREFIX + configurationId.toLowerCase(Locale.ROOT);
    }

    public static boolean isOwn(String credentialName, String configurationId) {
        return ownName(configurationId).equals(credentialName);
    }

    public Resolved resolve(String credentialName) {
        ResolvedCredential credential = credentials.require(SecretName.of(credentialName));

        return new Resolved(credential.plain(BASE_URL).orElse(null),
                credential.secret(API_KEY).map(SecretValue::reveal).orElse(null));
    }

    /** Never throws: a screen listing configurations shows a broken reference as "no key", not as an error page. */
    public Optional<Resolved> tryResolve(String credentialName) {
        try {
            return Optional.of(resolve(credentialName));
        } catch (RuntimeException unresolved) {
            return Optional.empty();
        }
    }

    public void requireExists(String credentialName) {
        if (credentials.definition(SecretName.of(credentialName)).isEmpty()) {
            throw new IllegalArgumentException("No vault credential is named '%s'.".formatted(credentialName));
        }
    }

    /**
     * Creates or replaces the configuration's own credential.
     *
     * @param apiKey null or blank keeps the stored key
     */
    public String saveOwn(String configurationId, String apiUrl, String apiKey) {
        String name = ownName(configurationId);

        transactions.call(() -> credentials.save(SecretName.of(name), KIND, "AI provider configuration " + configurationId,
                apiUrl == null || apiUrl.isBlank() ? Map.of() : Map.of(BASE_URL, apiUrl.strip()),
                apiKey == null || apiKey.isBlank() ? Map.of() : Map.of(API_KEY, apiKey.strip()),
                Set.of(), null));

        return name;
    }

    public void removeOwn(String configurationId) {
        transactions.call(() -> credentials.remove(SecretName.of(ownName(configurationId))));
    }
}
