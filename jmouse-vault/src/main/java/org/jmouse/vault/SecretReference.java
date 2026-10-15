package org.jmouse.vault;

import java.util.Objects;
import java.util.Optional;

/**
 * 🔗 What a configuration property holds where a library needs a secret: either a reference
 * {@code vault:telegram/notifications/token}, resolved through the {@link Vault} at each use, or a
 * literal value.
 *
 * <p>The literal exists for libraries used without a vault at all; inside the workspace a property
 * names a secret, and the secret lives in the vault.
 */
public sealed interface SecretReference {

    String SCHEME = "vault:";

    /** Parses a property value; null or blank gives null. */
    static SecretReference parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String trimmed = text.strip();

        if (trimmed.startsWith(SCHEME)) {
            return new Named(SecretName.of(trimmed.substring(SCHEME.length())));
        }

        return new Literal(SecretValue.of(trimmed));
    }

    static SecretReference named(String name) {
        return new Named(SecretName.of(name));
    }

    /** @param vault may be null when the application has none; a {@link Named} then resolves to nothing */
    Optional<SecretValue> resolve(Vault vault);

    record Named(SecretName name) implements SecretReference {

        @Override
        public Optional<SecretValue> resolve(Vault vault) {
            if (vault == null) {
                return Optional.empty();
            }

            return vault.find(name);
        }

        @Override
        public String toString() {
            return SCHEME + name;
        }
    }

    record Literal(SecretValue value) implements SecretReference {

        public Literal {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public Optional<SecretValue> resolve(Vault vault) {
            return Optional.of(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }
}
