package org.jmouse.suppliers;

import java.util.List;

/**
 * 🔑 What a distributor knows its caller by. Each protocol takes its own kind, so a driver given the wrong kind can say
 * so rather than send nothing.
 *
 * <p>⚠️ A credential's {@code toString} says only which kind it is: a record's own would print the secret into whatever
 * log the record reaches.</p>
 */
public sealed interface SupplierCredentials {

    /** Whether every part of it was given; one not given leaves a distributor not configured. */
    boolean complete();

    /** The values that must never be repeated in a message. */
    List<String> secrets();

    /** The text with every value of this credential blanked out: a distributor may echo what it was sent. */
    default String withoutSecrets(String text) {
        String clean = text;
        for (String secret : secrets()) {
            if (!secret.isEmpty()) {
                clean = clean.replace(secret, "[redacted]");
            }
        }
        return clean;
    }

    /** One key — Mouser's. */
    record ApiKey(String key) implements SupplierCredentials {

        public ApiKey {
            key = key == null ? "" : key.strip();
        }

        @Override
        public boolean complete() {
            return !key.isEmpty();
        }

        @Override
        public List<String> secrets() {
            return List.of(key);
        }

        @Override
        public String toString() {
            return "ApiKey[redacted]";
        }
    }

    /** An application's client, exchanged for a token — DigiKey's. */
    record ClientCredentials(String clientIdentifier, String clientSecret) implements SupplierCredentials {

        public ClientCredentials {
            clientIdentifier = clientIdentifier == null ? "" : clientIdentifier.strip();
            clientSecret = clientSecret == null ? "" : clientSecret.strip();
        }

        @Override
        public boolean complete() {
            return !clientIdentifier.isEmpty() && !clientSecret.isEmpty();
        }

        @Override
        public List<String> secrets() {
            return List.of(clientIdentifier, clientSecret);
        }

        @Override
        public String toString() {
            return "ClientCredentials[redacted]";
        }
    }
}
