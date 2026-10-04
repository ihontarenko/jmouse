package org.jmouse.telegram;

import java.util.Objects;

/**
 * Where a message goes: a chat, and optionally one thread inside it.
 *
 * <p>Telegram addresses a chat two ways and they are not interchangeable. A numeric id works for
 * everything and is what you get back from an update; an {@code @username} works only for a public
 * channel or a public group, and never for a private conversation. Both are kept rather than
 * normalised to one, because normalising means resolving, and resolving means a network call at a
 * point where the caller only wanted to name a destination.
 *
 * <h2>⚠️ A chat id is a signed 64-bit number and is very often negative</h2>
 *
 * <p>Groups are negative; supergroups and channels are large negatives, conventionally prefixed
 * {@code -100}. So this is a {@code long} and any column holding one is {@code BIGINT} — never
 * {@code INT}, never unsigned. The failure from getting this wrong is silent truncation, which
 * delivers a message to a different chat rather than failing.
 *
 * <h2>⚠️ {@code threadId} is here from the first commit deliberately</h2>
 *
 * <p>A forum supergroup has topics, and a topic is how one group serves several products without
 * becoming unreadable — {@code Kitsu} in one, deployments in another. Telegram carries that as
 * {@code message_thread_id} on <em>every</em> send method, so a design that adds it later has to
 * touch every call site that will ever exist. It costs one nullable field now and an afternoon
 * later.
 *
 * @param chatId   the numeric chat, or {@code null} when addressed by {@link #username}
 * @param username the {@code @name} of a public chat, or {@code null} when addressed by id
 * @param threadId the forum topic within the chat, or {@code null} for the chat itself
 */
public record ChatReference(Long chatId, String username, Integer threadId) {

    public ChatReference {
        boolean hasId       = chatId != null;
        boolean hasUsername = username != null && !username.isBlank();

        if (hasId == hasUsername) {
            throw new IllegalArgumentException(
                    "a chat is addressed either by id or by username, never both and never neither");
        }
    }

    /** A chat by its numeric id — what an update reports and what a binding stores. */
    public static ChatReference of(long chatId) {
        return new ChatReference(chatId, null, null);
    }

    /**
     * A public chat by name. The leading {@code @} is added when it is missing, because half the
     * places a name is copied from carry it and half do not.
     */
    public static ChatReference of(String username) {
        Objects.requireNonNull(username, "username");

        String normalised = username.startsWith("@") ? username : "@" + username;

        return new ChatReference(null, normalised, null);
    }

    /** The same chat, addressed inside one of its forum topics. */
    public ChatReference inThread(int threadId) {
        return new ChatReference(chatId, username, threadId);
    }

    /** The same chat, addressed at its root rather than in a topic. */
    public ChatReference inChat() {
        return new ChatReference(chatId, username, null);
    }

    public boolean hasThread() {
        return threadId != null;
    }

    /**
     * What goes on the wire as {@code chat_id}: the number, or the {@code @name}.
     *
     * <p>Telegram accepts either in the same field, which is why this returns a {@link String} rather
     * than forcing every transport to branch.
     */
    public String wireValue() {
        if (chatId != null) {
            return String.valueOf(chatId);
        }

        return username;
    }

    @Override
    public String toString() {
        if (hasThread()) {
            return wireValue() + "#" + threadId;
        }

        return wireValue();
    }
}
