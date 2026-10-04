package org.jmouse.telegram;

import java.util.Objects;

/**
 * Which existing message to act on.
 *
 * <p>A pair rather than two parameters, because the two are meaningless apart: a message id is unique
 * only within its chat, so a signature taking {@code (long chatId, int messageId)} invites exactly one
 * mistake — the right id against the wrong chat — and Telegram answers that with "message to edit not
 * found", which sounds like the message was deleted.
 *
 * <p>⚠️ The chat here never carries a thread. {@code message_thread_id} routes a <em>new</em> message
 * into a topic; an edit or a delete addresses a message that already has a place, and Telegram refuses
 * the field. {@link SentMessage#handle()} strips it for this reason.
 *
 * @param chat      which chat the message is in
 * @param messageId its id within that chat
 */
public record MessageHandle(ChatReference chat, int messageId) {

    public MessageHandle {
        Objects.requireNonNull(chat, "chat");

        if (chat.hasThread()) {
            throw new IllegalArgumentException(
                    "a message handle addresses a message, not a topic; Telegram refuses "
                    + "message_thread_id on an edit or a delete");
        }
    }

    public static MessageHandle of(long chatId, int messageId) {
        return new MessageHandle(ChatReference.of(chatId), messageId);
    }

    @Override
    public String toString() {
        return chat + "/" + messageId;
    }
}
