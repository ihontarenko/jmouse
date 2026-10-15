package org.jmouse.vault.credential;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 🧩 The shape of a credential: which fields it has, and which of them are secret.
 *
 * <p>Declared by whoever consumes the credential — the AI library knows an HTTP provider needs a base URL
 * and a key, the storage library knows S3 needs four things and two of them are secret — so an
 * administration screen can draw the form from the kind instead of every product writing one.
 *
 * @param name       stable identifier, kebab-case: {@code http-api}, {@code s3}
 * @param title      what a screen shows
 * @param fields     the declared fields, in display order
 * @param openEnded  whether fields beyond the declared ones are accepted — {@code generic} is nothing else
 */
public record CredentialKind(String name, String title, String description, List<Field> fields, boolean openEnded) {

    /**
     * @param secret whether the value is a secret — sealed in the vault, never listed — or plain
     *               configuration such as a URL, kept beside the credential and shown
     */
    public record Field(String name, String title, boolean secret, boolean required, String hint) {

        public static Field plain(String name, String title, boolean required, String hint) {
            return new Field(name, title, false, required, hint);
        }

        public static Field secret(String name, String title, boolean required, String hint) {
            return new Field(name, title, true, required, hint);
        }
    }

    public CredentialKind {
        Objects.requireNonNull(name, "name");
        fields = List.copyOf(fields);
    }

    public Optional<Field> field(String fieldName) {
        return fields.stream().filter(field -> field.name().equals(fieldName)).findFirst();
    }
}
