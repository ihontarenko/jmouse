package org.jmouse.telegram.bot;

import com.fasterxml.jackson.databind.JsonNode;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.update.IncomingMessage;
import org.jmouse.telegram.update.TelegramUser;
import org.jmouse.telegram.update.Update;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Telegram's update JSON, as the core's {@link Update} model.
 *
 * <p>Here rather than in the core because the core knows nothing about JSON, and public because the
 * webhook endpoint in the Spring module parses exactly the same shape as the polling loop — Telegram
 * posts an identical {@code Update} object either way.
 *
 * <p>⚠️ <strong>Anything unrecognised becomes {@link Update.Unknown} rather than throwing.</strong>
 * Telegram adds update types on its own schedule; a parser that refused one would stop an ingestion
 * loop on the day of a Telegram release, over an update nobody wanted.
 */
public final class BotApiUpdates {

    /** The update members this library models, in the order they are tested. */
    private static final String[] MESSAGE_KINDS = {
            "message", "edited_message", "channel_post", "edited_channel_post"
    };

    private BotApiUpdates() {
    }

    /** One update. */
    public static Update read(JsonNode node) {
        long updateId = node.path("update_id").asLong();

        for (String kind : MESSAGE_KINDS) {
            if (node.hasNonNull(kind)) {
                IncomingMessage message = readMessage(node.get(kind));

                return switch (kind) {
                    case "message"              -> new Update.MessageReceived(updateId, message);
                    case "edited_message"       -> new Update.MessageEdited(updateId, message);
                    case "channel_post"         -> new Update.ChannelPost(updateId, message);
                    default                     -> new Update.ChannelPostEdited(updateId, message);
                };
            }
        }

        if (node.hasNonNull("callback_query")) {
            return readCallback(updateId, node.get("callback_query"));
        }

        // ⚠️ my_chat_member is THIS identity's own membership changing - the update that says the bot
        // was added to or removed from a chat. Telegram delivers it as a separate kind because it
        // means something different, and the flag carries that distinction into the model.
        if (node.hasNonNull("my_chat_member")) {
            return readMembership(updateId, node.get("my_chat_member"), true);
        }

        if (node.hasNonNull("chat_member")) {
            return readMembership(updateId, node.get("chat_member"), false);
        }

        if (node.hasNonNull("chat_join_request")) {
            return readJoinRequest(updateId, node.get("chat_join_request"));
        }

        return new Update.Unknown(updateId, firstUnknownField(node));
    }

    /** A batch, as {@code getUpdates} answers and as a webhook never does. */
    public static List<Update> readAll(JsonNode array) {
        List<Update> updates = new ArrayList<>(array.size());

        array.forEach(node -> updates.add(read(node)));

        return updates;
    }

    private static IncomingMessage readMessage(JsonNode message) {
        JsonNode chat   = message.path("chat");
        JsonNode thread = message.path("message_thread_id");

        ChatReference reference = ChatReference.of(chat.path("id").asLong());

        // ⚠️ Carried through, so a reply lands in the topic it answers rather than at the top of the
        // group - which is the single most visible way a forum integration looks broken.
        if (!thread.isMissingNode() && thread.isNumber()) {
            reference = reference.inThread(thread.asInt());
        }

        return new IncomingMessage(
                reference,
                message.path("message_id").asInt(),
                readUser(message.get("from")),
                message.path("text").asText(null),
                message.path("caption").asText(null),
                Instant.ofEpochSecond(message.path("date").asLong()),
                attachmentIds(message),
                message.path("reply_to_message").path("message_id").isNumber()
                        ? message.path("reply_to_message").path("message_id").asInt()
                        : null);
    }

    private static Update readCallback(long updateId, JsonNode callback) {
        JsonNode      message = callback.path("message");
        MessageHandle handle  = null;

        // ⚠️ Absent for a button on an inline-mode result, and for a message too old for Telegram to
        // still hold. A handler that assumes it is present fails on exactly those two cases.
        if (message.isObject() && message.hasNonNull("message_id")) {
            handle = new MessageHandle(
                    ChatReference.of(message.path("chat").path("id").asLong()),
                    message.path("message_id").asInt());
        }

        return new Update.CallbackPressed(
                updateId,
                callback.path("id").asText(),
                readUser(callback.get("from")),
                handle,
                callback.path("data").asText(null));
    }

    private static Update readMembership(long updateId, JsonNode change, boolean mine) {
        return new Update.MembershipChanged(
                updateId,
                ChatReference.of(change.path("chat").path("id").asLong()),
                readUser(change.path("new_chat_member").get("user")),
                readUser(change.get("from")),
                change.path("new_chat_member").path("status").asText(null),
                mine);
    }

    private static Update readJoinRequest(long updateId, JsonNode request) {
        return new Update.JoinRequested(
                updateId,
                ChatReference.of(request.path("chat").path("id").asLong()),
                readUser(request.get("from")),
                request.path("invite_link").path("invite_link").asText(null));
    }

    /** ⚠️ Null for a channel post, which genuinely has no author. */
    private static TelegramUser readUser(JsonNode user) {
        if (user == null || !user.isObject()) {
            return null;
        }

        return new TelegramUser(
                user.path("id").asLong(),
                user.path("first_name").asText(""),
                user.path("last_name").asText(null),
                user.path("username").asText(null),
                user.path("language_code").asText(null),
                user.path("is_bot").asBoolean(false));
    }

    /**
     * Every {@code file_id} the message carries.
     *
     * <p>⚠️ A photo arrives as an array of sizes and the last is the largest — the same trap as on the
     * sending side, and with the same consequence: a {@code file_id} taken from the front fetches a
     * thumbnail.
     */
    private static List<String> attachmentIds(JsonNode message) {
        List<String> ids   = new ArrayList<>(1);
        JsonNode     photo = message.path("photo");

        if (photo.isArray() && !photo.isEmpty()) {
            ids.add(photo.get(photo.size() - 1).path("file_id").asText());
        }

        for (String field : new String[]{"document", "video", "audio", "animation", "voice", "video_note"}) {
            JsonNode node = message.path(field);

            if (node.isObject() && node.hasNonNull("file_id")) {
                ids.add(node.get("file_id").asText());
            }
        }

        return ids;
    }

    /**
     * Which member of the update object we did not recognise, so a log line names it and a decision to
     * model it can be taken with evidence rather than from a guess.
     */
    private static String firstUnknownField(JsonNode node) {
        var fields = node.fieldNames();

        while (fields.hasNext()) {
            String field = fields.next();

            if (!"update_id".equals(field)) {
                return field;
            }
        }

        return "empty";
    }
}
