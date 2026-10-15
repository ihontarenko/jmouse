package org.jmouse.notifier.telegram;

import org.jmouse.notifier.Notification;
import org.jmouse.notifier.Publication;
import org.jmouse.telegram.MarkupEscaper;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.ParseMode;
import org.jmouse.telegram.markup.InlineButton;
import org.jmouse.telegram.markup.ReplyMarkup;

/**
 * How a publication looks as a Telegram message.
 *
 * <pre>
 *   <b>Downloads</b>             ← the title, when there is one
 *   Blade Runner finished downloading.
 *   <i>Rule: downloads</i>       ← the sender, always — a reader must be able to tell who is speaking
 *   [ Open ]                     ← one URL button per link
 * </pre>
 *
 * <p>⚠️ HTML rather than MarkdownV2: three characters to escape instead of eighteen, and every value —
 * the title, the body, the sender — is escaped here, because a film called {@code Alien (1979)} must
 * not fail a send.
 *
 * <p>⚠️ An {@code INFO} notification is sent silently; {@code WARN} and {@code ALARM} make a sound. That
 * is the level's decision ({@link org.jmouse.notifier.Level#audible()}), not this format's.
 *
 * <p>Not final: a product that wants a different look overrides {@link #draftOf(Publication)}.
 */
public class TelegramMessageFormat {

    /** What is appended when a body had to be cut to fit. */
    private static final String ELLIPSIS = "…";

    /** A heading longer than this is cut — it is a heading. */
    private static final int TITLE_LIMIT = 256;

    public MessageDraft draftOf(Publication publication) {
        Notification notification = publication.notification();
        String       heading      = notification.hasTitle()
                ? "<b>" + MarkupEscaper.html(cut(notification.title(), TITLE_LIMIT)) + "</b>\n"
                : "";
        String       signature    = "\n\n<i>" + MarkupEscaper.html(publication.sender().label()) + "</i>";
        int          room         = MessageDraft.TEXT_LIMIT - heading.length() - signature.length();
        String       text         = heading + escapedWithin(notification.body(), room) + signature;

        MessageDraft.Builder draft = MessageDraft.builder().text(text, ParseMode.HTML).withoutPreview();

        if (!notification.links().isEmpty()) {
            InlineButton[] buttons = notification.links().stream()
                                                 .map(link -> InlineButton.url(link.label(), link.url()))
                                                 .toArray(InlineButton[]::new);
            draft.markup(ReplyMarkup.InlineKeyboard.stacked(buttons));
        }

        if (!notification.level().audible()) {
            draft.silent();
        }

        return draft.build();
    }

    /**
     * The body escaped, cut so that the <em>escaped</em> text fits {@code room}.
     *
     * <p>⚠️ Measured after escaping, because the draft's limit counts what is sent — a body of
     * ampersands is five times its own length once escaped. And cut on the raw text, so an entity such
     * as {@code &amp;} is never split in half.
     */
    private static String escapedWithin(String body, int room) {
        String escaped = MarkupEscaper.html(body);

        if (escaped.length() <= room) {
            return escaped;
        }

        int budget = room - ELLIPSIS.length();
        int used   = 0;
        int end    = 0;

        while (end < body.length()) {
            int cost = escapedLength(body.charAt(end));

            if (used + cost > budget) {
                break;
            }

            used += cost;
            end++;
        }

        return MarkupEscaper.html(cut(body, end)) + ELLIPSIS;
    }

    private static int escapedLength(char character) {
        return switch (character) {
            case '&' -> 5;
            case '<', '>' -> 4;
            default -> 1;
        };
    }

    /** At most {@code length} characters, never splitting a surrogate pair. */
    private static String cut(String value, int length) {
        if (value.length() <= length) {
            return value;
        }

        int end = length;

        if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }

        return value.substring(0, end);
    }
}
