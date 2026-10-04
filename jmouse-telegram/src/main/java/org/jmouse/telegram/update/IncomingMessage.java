package org.jmouse.telegram.update;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.MessageHandle;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A message that arrived.
 *
 * <p>Deliberately not the whole of Telegram's {@code Message} object, which has upwards of eighty
 * fields. This carries what a handler routes on and answers with; anything more specialised is read
 * from {@link #attachmentFileIds()} or is a reason to widen this record on purpose rather than to
 * expose a JSON tree and let every product parse it differently.
 *
 * @param chat              where it arrived — ⚠️ carrying the forum topic, so a reply lands in the
 *                          same thread rather than at the top of the group
 * @param messageId         its id within that chat
 * @param from              who sent it, or {@code null} for a channel post, which has no author
 * @param text              the body, or {@code null} when the message is media with no caption
 * @param caption           the caption of an attachment, or {@code null}
 * @param sentAt            when Telegram says it was sent
 * @param attachmentFileIds the {@code file_id} of anything attached, so it can be re-sent or fetched
 * @param replyToMessageId  the message this answers, or {@code null}
 */
public record IncomingMessage(
        ChatReference chat,
        int           messageId,
        TelegramUser  from,
        String        text,
        String        caption,
        Instant       sentAt,
        List<String>  attachmentFileIds,
        Integer       replyToMessageId
) {

    public IncomingMessage {
        Objects.requireNonNull(chat, "chat");
        attachmentFileIds = attachmentFileIds == null ? List.of() : List.copyOf(attachmentFileIds);
    }

    /** The text if there is any, otherwise the caption — what a command or a filter reads. */
    public String body() {
        if (text != null && !text.isBlank()) {
            return text;
        }

        return caption;
    }

    public boolean hasText() {
        return body() != null && !body().isBlank();
    }

    /** Where to reply, edit or delete. ⚠️ The thread is stripped, as {@link MessageHandle} requires. */
    public MessageHandle handle() {
        return new MessageHandle(chat.inChat(), messageId);
    }

    /**
     * The command in {@code /name} or {@code /name@thisbot}, without its slash, when the message is
     * one.
     *
     * <p>⚠️ The {@code @botname} suffix matters: in a group, Telegram appends it so several bots can
     * share a command, and a naive {@code equals("/start")} therefore never matches in exactly the
     * place where several bots coexist. Stripped here so a handler never has to know.
     */
    public Optional<String> command() {
        String body = body();

        if (body == null || !body.startsWith("/")) {
            return Optional.empty();
        }

        int    end  = body.indexOf(' ');
        String head = end < 0 ? body.substring(1) : body.substring(1, end);
        int    at   = head.indexOf('@');

        if (at >= 0) {
            head = head.substring(0, at);
        }

        return head.isEmpty() ? Optional.empty() : Optional.of(head);
    }

    /**
     * Whatever followed the command, trimmed — the payload of a {@code /start <token>} deep link.
     *
     * <p>This is the field the binding flow reads: the link {@code t.me/<bot>?start=<token>} arrives
     * as exactly this.
     */
    public Optional<String> commandArgument() {
        String body = body();

        if (body == null || !body.startsWith("/")) {
            return Optional.empty();
        }

        int end = body.indexOf(' ');

        if (end < 0 || end + 1 >= body.length()) {
            return Optional.empty();
        }

        String argument = body.substring(end + 1).trim();

        return argument.isEmpty() ? Optional.empty() : Optional.of(argument);
    }
}
