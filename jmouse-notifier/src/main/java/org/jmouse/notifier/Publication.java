package org.jmouse.notifier;

import java.time.Instant;

/**
 * A notification once it has been published: who sent it, to whom, and when.
 *
 * @param id             assigned by the {@link Notifier}
 * @param idempotencyKey the publisher's own key, or {@code null}. ⚠️ With one, publishing the same key
 *                       twice delivers once — the answer to "did this already go?" after a crash
 *                       between the delivery and the commit that would have recorded it
 * @param sender         who it comes from
 * @param recipient      who it was addressed to, before resolution
 * @param notification   what it says
 * @param publishedAt    when
 */
public record Publication(
        String id,
        String idempotencyKey,
        Sender sender,
        Recipient recipient,
        Notification notification,
        Instant publishedAt
) {

    public Publication {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A publication needs an identifier");
        }

        if (sender == null || recipient == null || notification == null || publishedAt == null) {
            throw new IllegalArgumentException("A publication needs a sender, a recipient, a notification and a time");
        }

        idempotencyKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey;
    }

    public boolean hasIdempotencyKey() {
        return idempotencyKey != null;
    }
}
