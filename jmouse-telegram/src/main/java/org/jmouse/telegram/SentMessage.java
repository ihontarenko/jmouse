package org.jmouse.telegram;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A message that exists in Telegram, and the handle needed to change it.
 *
 * <h2>⚠️ This is returned, and it is meant to be stored</h2>
 *
 * <p>The temptation is to make sending {@code void} — it looks like fire-and-forget, and nothing at the
 * call site wants an answer. That choice is what turns a notification channel into a spam source.
 *
 * <p>A channel that reports "two people opened this" and later "three people opened this" should
 * <em>edit</em> one message, not post a second and then a third. Editing needs the {@code messageId},
 * the {@code messageId} exists only in the reply to the send, and a signature that discards it has
 * made editing impossible for every consumer permanently. So the handle comes back, and
 * {@code jmouse-telegram-jpa}'s outbox keeps it against the row that produced it.
 *
 * @param chat      where it landed. ⚠️ Resolved: a draft addressed to {@code @channel} comes back with
 *                  the numeric id, which is what an edit and a delete need and what a username cannot
 *                  always stand in for
 * @param messageId Telegram's id for it, unique within the chat rather than globally
 * @param sentAt    when Telegram says it was sent
 * @param mediaIds  the {@code file_id} of each attachment, in order, so the same bytes can be sent
 *                  again for free instead of uploaded twice. ⚠️ Bot-specific — one bot cannot use
 *                  another's file ids
 */
public record SentMessage(
        ChatReference chat,
        int           messageId,
        Instant       sentAt,
        List<String>  mediaIds
) {

    public SentMessage {
        Objects.requireNonNull(chat, "chat");
        mediaIds = mediaIds == null ? List.of() : List.copyOf(mediaIds);
    }

    public static SentMessage of(ChatReference chat, int messageId, Instant sentAt) {
        return new SentMessage(chat, messageId, sentAt, List.of());
    }

    /**
     * Where this message is, for editing or deleting it.
     *
     * <p>⚠️ Carries the chat <strong>without</strong> the thread. A message is addressed by chat and
     * id; {@code message_thread_id} is a routing instruction for a <em>new</em> message and Telegram
     * refuses it on an edit. Dropping it here is what stops that mistake reaching the wire.
     */
    public MessageHandle handle() {
        return new MessageHandle(chat.inChat(), messageId);
    }
}
