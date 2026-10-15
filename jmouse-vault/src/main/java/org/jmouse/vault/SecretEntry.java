package org.jmouse.vault;

import java.time.Instant;

/**
 * 📋 What one source knows about one secret — everything except the value.
 *
 * @param name        the secret
 * @param source      the source's name ({@code database}, {@code environment}, {@code configuration})
 * @param fingerprint {@link SecretValue#fingerprint()}
 * @param description what it is for, or null; only a writable source keeps one
 * @param updatedAt   when it last changed, or null where the source cannot say
 * @param updatedBy   who changed it, or null where the source cannot say
 * @param location    where in that source it lives — an environment variable's name, a property key, or null
 */
public record SecretEntry(
        SecretName name, String source, String fingerprint, String description,
        Instant updatedAt, String updatedBy, String location) {
}
