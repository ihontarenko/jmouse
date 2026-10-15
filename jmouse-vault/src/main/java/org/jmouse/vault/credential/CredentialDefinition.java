package org.jmouse.vault.credential;

import org.jmouse.vault.SecretName;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * 📇 A credential as stored: its kind, its plain fields, and the names of its secret fields.
 *
 * <p>The secrets themselves are not here. Each lives in the vault under the credential's name plus the
 * field — {@code ai/openai/api-key} for field {@code api-key} of credential {@code ai/openai} — so it
 * resolves through the same chain as any other secret and can come from the environment just as well.
 *
 * @param source where the definition came from ({@code database}, {@code configuration})
 */
public record CredentialDefinition(
        SecretName name, String kind, String description,
        Map<String, String> plainFields, Set<String> secretFields,
        Instant updatedAt, String updatedBy, String source) {

    public CredentialDefinition {
        plainFields  = Map.copyOf(plainFields);
        secretFields = Set.copyOf(secretFields);
    }

    public SecretName secretName(String field) {
        return name.child(field);
    }
}
