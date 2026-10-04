package org.jmouse.telegram.bot;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.markup.InlineButton;
import org.jmouse.telegram.markup.ReplyMarkup;
import org.jmouse.telegram.media.MediaAttachment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the core's value types into the fields Telegram expects.
 *
 * <p>All of the wire's vocabulary lives here — {@code disable_notification},
 * {@code message_thread_id}, {@code link_preview_options} — so that neither the core (which must not
 * know a wire format) nor {@link BotApiTransport} (which is about HTTP and failure) has to carry it.
 *
 * <p>⚠️ Fields are omitted when they are absent rather than sent as {@code null}. Telegram tolerates
 * a null in most places and rejects it in a few, and the ones it rejects are not documented together —
 * so "absent means absent" is the only rule that holds everywhere.
 */
final class BotApiPayload {

    private BotApiPayload() {
    }

    /** The fields common to every send, before the content is added. */
    static Map<String, Object> destination(ChatReference chat) {
        Map<String, Object> fields = new LinkedHashMap<>();

        fields.put("chat_id", chat.wireValue());

        // ⚠️ The field that makes forum topics work. Omitted rather than null: Telegram refuses a
        // null message_thread_id outright, which reads as "this chat is not a forum".
        if (chat.hasThread()) {
            fields.put("message_thread_id", chat.threadId());
        }

        return fields;
    }

    /** The flags and markup every message-bearing method accepts. */
    static void applyOptions(Map<String, Object> fields, MessageDraft draft) {
        if (draft.parseMode().isFormatted()) {
            fields.put("parse_mode", draft.parseMode().wireValue());
        }

        if (draft.silent()) {
            fields.put("disable_notification", true);
        }

        if (draft.protectedContent()) {
            fields.put("protect_content", true);
        }

        // ⚠️ disable_web_page_preview was replaced by link_preview_options and is deprecated. Both
        // are accepted today; only the new one will be.
        if (draft.withoutPreview()) {
            fields.put("link_preview_options", Map.of("is_disabled", true));
        }

        if (draft.replyToMessageId() != null) {
            fields.put("reply_parameters", Map.of("message_id", draft.replyToMessageId()));
        }

        if (draft.replyMarkup() != null) {
            fields.put("reply_markup", markup(draft.replyMarkup()));
        }
    }

    /**
     * A {@link ReplyMarkup} as the nested object Telegram reads.
     *
     * <p>Returned as a {@link Map} rather than as a JSON string, because the two transports of this
     * value differ: a JSON request body carries it as an object, and a multipart field carries it as
     * a serialised string. Producing the object here lets the caller do whichever it needs.
     */
    static Map<String, Object> markup(ReplyMarkup markup) {
        return switch (markup) {
            case ReplyMarkup.InlineKeyboard keyboard -> Map.of("inline_keyboard", inlineRows(keyboard));

            case ReplyMarkup.ReplyKeyboard keyboard -> {
                Map<String, Object> fields = new LinkedHashMap<>();

                fields.put("keyboard", keyboard.rows());
                fields.put("one_time_keyboard", keyboard.oneTime());
                fields.put("resize_keyboard", keyboard.resize());

                yield fields;
            }

            case ReplyMarkup.RemoveKeyboard ignored -> Map.of("remove_keyboard", true);

            case ReplyMarkup.ForceReply forceReply -> {
                Map<String, Object> fields = new LinkedHashMap<>();

                fields.put("force_reply", true);

                if (forceReply.placeholder() != null) {
                    fields.put("input_field_placeholder", forceReply.placeholder());
                }

                yield fields;
            }
        };
    }

    private static List<List<Map<String, Object>>> inlineRows(ReplyMarkup.InlineKeyboard keyboard) {
        List<List<Map<String, Object>>> rows = new ArrayList<>(keyboard.rows().size());

        for (List<InlineButton> row : keyboard.rows()) {
            List<Map<String, Object>> buttons = new ArrayList<>(row.size());

            for (InlineButton button : row) {
                Map<String, Object> fields = new LinkedHashMap<>();

                fields.put("text", button.text());

                switch (button.action()) {
                    case InlineButton.Action.Callback callback -> fields.put("callback_data", callback.data());
                    case InlineButton.Action.OpenUrl open      -> fields.put("url", open.url());
                }

                buttons.add(fields);
            }

            rows.add(buttons);
        }

        return rows;
    }

    /**
     * One entry of a media group.
     *
     * @param reference what goes in the {@code media} field: a {@code file_id}, a URL, or an
     *                  {@code attach://name} pointing at a part of the same multipart body
     */
    static Map<String, Object> groupEntry(MediaAttachment attachment, String reference, MessageDraft draft) {
        Map<String, Object> fields = new LinkedHashMap<>();

        fields.put("type", attachment.kind().field());
        fields.put("media", reference);

        if (attachment.hasCaption()) {
            fields.put("caption", attachment.caption());

            if (draft.parseMode().isFormatted()) {
                fields.put("parse_mode", draft.parseMode().wireValue());
            }
        }

        if (attachment.spoiler()) {
            fields.put("has_spoiler", true);
        }

        return fields;
    }
}
