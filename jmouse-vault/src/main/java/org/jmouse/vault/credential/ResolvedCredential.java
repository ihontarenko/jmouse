package org.jmouse.vault.credential;

import org.jmouse.vault.MissingSecretException;
import org.jmouse.vault.SecretName;
import org.jmouse.vault.SecretValue;
import org.jmouse.vault.Vault;

import java.util.List;
import java.util.Optional;

/**
 * 🔓 A credential ready to use: plain fields read off the definition, secrets resolved through the vault
 * at the moment they are asked for.
 */
public final class ResolvedCredential {

    private final CredentialDefinition definition;
    private final Vault                vault;

    ResolvedCredential(CredentialDefinition definition, Vault vault) {
        this.definition = definition;
        this.vault      = vault;
    }

    public SecretName name() {
        return definition.name();
    }

    public String kind() {
        return definition.kind();
    }

    public Optional<String> plain(String field) {
        return Optional.ofNullable(definition.plainFields().get(field)).filter(value -> !value.isBlank());
    }

    public Optional<SecretValue> secret(String field) {
        return vault.find(definition.secretName(field));
    }

    /** @throws MissingSecretException naming where it was looked for */
    public SecretValue requireSecret(String field) {
        SecretName name = definition.secretName(field);

        return vault.find(name).orElseThrow(() -> new MissingSecretException(name, vault.lookedAt(name)));
    }

    public CredentialDefinition definition() {
        return definition;
    }

    @Override
    public String toString() {
        return "ResolvedCredential[%s, kind=%s, secrets=%s]".formatted(name(), kind(), List.copyOf(definition.secretFields()));
    }
}
