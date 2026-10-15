package org.jmouse.telegram.administration;

import java.time.Instant;

/**
 * The terms an invite link is created or edited with.
 *
 * <p>⚠️ A member limit and a join request cannot go together — Telegram refuses the pair, because a link
 * that asks an administrator has no count to run out. Refused here with a sentence instead.
 *
 * @param name                a label an administrator sees, at most 32 characters, or {@code null}
 * @param expiresAt           when it stops working, or {@code null} for never
 * @param memberLimit         1–99999 joins, or {@code null} for no limit
 * @param joinRequestRequired whether joining asks an administrator first
 */
public record InviteLinkTerms(String name, Instant expiresAt, Integer memberLimit, boolean joinRequestRequired) {

    public static final int NAME_LIMIT = 32;

    public InviteLinkTerms {
        if (name != null && name.length() > NAME_LIMIT) {
            throw new IllegalArgumentException("An invite link's name is at most %d characters".formatted(NAME_LIMIT));
        }

        if (memberLimit != null && (memberLimit < 1 || memberLimit > 99_999)) {
            throw new IllegalArgumentException("An invite link's member limit is between 1 and 99999");
        }

        if (memberLimit != null && joinRequestRequired) {
            throw new IllegalArgumentException(
                    "An invite link either limits its members or asks an administrator, never both");
        }
    }

    /** A plain link: no name, no expiry, no limit, no request. */
    public static InviteLinkTerms open() {
        return new InviteLinkTerms(null, null, null, false);
    }
}
