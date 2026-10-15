package org.jmouse.telegram.administration;

import org.jmouse.telegram.ChatReference;

/**
 * What a chat is, as far as the identity asking can see.
 *
 * @param chat        the chat by id
 * @param type        Telegram's word — {@code private}, {@code group}, {@code supergroup}, {@code channel}
 * @param title       its title, or {@code null} for a private chat
 * @param username    its public {@code @username}, or {@code null}
 * @param description its description, or {@code null}
 * @param forum       whether it is a supergroup with topics
 * @param inviteLink  its primary invite link, or {@code null} — ⚠️ only reported to an administrator
 */
public record ChatDetails(
        ChatReference chat,
        String        type,
        String        title,
        String        username,
        String        description,
        boolean       forum,
        String        inviteLink
) {

    public ChatDetails {
        if (chat == null) {
            throw new IllegalArgumentException("Chat details need the chat");
        }
    }
}
