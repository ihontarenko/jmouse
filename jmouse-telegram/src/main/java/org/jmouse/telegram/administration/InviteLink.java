package org.jmouse.telegram.administration;

import java.time.Instant;

/**
 * An invite link as Telegram reports it.
 *
 * @param link                the {@code https://t.me/+…} address — ⚠️ a credential to the chat in all
 *                            but name: whoever holds it can join, so log it no more than a password
 * @param name                its label, or {@code null}
 * @param expiresAt           when it stops working, or {@code null}
 * @param memberLimit         how many may join through it, or {@code null}
 * @param joinRequestRequired whether joining through it asks an administrator first
 * @param primary             whether it is the chat's primary link
 * @param revoked             whether it was revoked
 */
public record InviteLink(
        String  link,
        String  name,
        Instant expiresAt,
        Integer memberLimit,
        boolean joinRequestRequired,
        boolean primary,
        boolean revoked
) {

    @Override
    public String toString() {
        // ⚠️ Not the link itself — see the class note.
        return "InviteLink[name=%s, expiresAt=%s, memberLimit=%s, joinRequestRequired=%s, primary=%s, revoked=%s]"
                .formatted(name, expiresAt, memberLimit, joinRequestRequired, primary, revoked);
    }
}
