package org.jmouse.telegram.bot;

import com.fasterxml.jackson.databind.JsonNode;
import org.jmouse.telegram.CallbackAnswer;
import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.media.MediaAttachment;
import org.jmouse.telegram.media.MediaKind;
import org.jmouse.telegram.media.MediaSource;
import org.jmouse.telegram.spi.TelegramTransport;
import org.jmouse.telegram.update.Update;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Telegram's Bot API, over HTTP.
 *
 * <p>The ordinary transport: a bot token, JSON over HTTPS, and everything a bot is allowed to do. Its
 * limits are the protocol's rather than this class's — see {@link #supports(Capability)} for the four
 * capabilities it does not have and cannot acquire.
 *
 * <p>⚠️ It holds no configuration. The identity arrives as a parameter on every call, which is what
 * keeps purposes, fallbacks and credential rotation entirely in the gateway above — see
 * {@link TelegramTransport}.
 */
public class BotApiTransport extends BotApiClient implements TelegramTransport, UpdateFetcher {

    /**
     * Telegram's own upload ceiling.
     *
     * <p>⚠️ Checked only when the identity points at Telegram itself: a self-hosted
     * {@code telegram-bot-api} server accepts 2 GB, and refusing there would break the one arrangement
     * that exists to get past this limit.
     */
    public static final long TELEGRAM_UPLOAD_LIMIT = 50L * 1024 * 1024;

    private static final Set<Capability> SUPPORTED = EnumSet.of(
            Capability.SEND_MESSAGE,
            Capability.EDIT_MESSAGE,
            Capability.DELETE_MESSAGE,
            Capability.SEND_MEDIA,
            Capability.REPLY_MARKUP,
            Capability.ANSWER_CALLBACK,
            Capability.ADMINISTER_CHAT,
            Capability.MANAGE_TOPICS);

    public BotApiTransport() {
        super();
    }

    public BotApiTransport(Duration connectTimeout, Duration readTimeout) {
        super(connectTimeout, readTimeout);
    }

    @Override
    public IdentityKind kind() {
        return IdentityKind.BOT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>⚠️ Four are absent and no configuration adds them:
     * {@link Capability#CREATE_CHAT} (MTProto only — a bot cannot create a group or a channel),
     * {@link Capability#INITIATE_CONVERSATION} (a person must start the bot first, which is why
     * binding is a flow), and {@link Capability#READ_HISTORY} (a bot sees only what arrives while it
     * is listening).
     */
    @Override
    public boolean supports(Capability capability) {
        return SUPPORTED.contains(capability);
    }

    @Override
    public SentMessage send(TelegramIdentity identity, ChatReference chat, MessageDraft draft) {
        if (!draft.hasAttachments()) {
            return sendText(identity, chat, draft);
        }

        if (draft.isMediaGroup()) {
            return sendMediaGroup(identity, chat, draft);
        }

        return sendSingleMedia(identity, chat, draft);
    }

    @Override
    public SentMessage edit(TelegramIdentity identity, MessageHandle message, MessageDraft draft) {
        // ⚠️ Deliberately not supported rather than half-supported. editMessageMedia replaces the
        // attachment and has its own rules about which kinds may replace which; guessing here would
        // produce a call that works for a photo and silently fails for an audio file.
        if (draft.hasAttachments()) {
            throw new IllegalArgumentException(
                    "editing a message's media is not supported; edit the text and markup, or delete "
                    + "and send again");
        }

        // MessageHandle already refuses a threaded chat, so destination() cannot have added
        // message_thread_id here - which is the field Telegram rejects on an edit.
        Map<String, Object> fields = BotApiPayload.destination(message.chat());

        fields.put("message_id", message.messageId());
        fields.put("text", draft.text());

        BotApiPayload.applyOptions(fields, draft);

        return readMessage(call(identity, "editMessageText", fields));
    }

    @Override
    public void delete(TelegramIdentity identity, MessageHandle message) {
        call(identity, "deleteMessage", Map.of(
                "chat_id",    message.chat().wireValue(),
                "message_id", message.messageId()));
    }

    @Override
    public void answerCallback(TelegramIdentity identity, CallbackAnswer answer) {
        Map<String, Object> fields = new LinkedHashMap<>();

        fields.put("callback_query_id", answer.queryId());

        if (answer.notice() != null) {
            fields.put("text", answer.notice());
            fields.put("show_alert", answer.alert());
        }

        call(identity, "answerCallbackQuery", fields);
    }

    /**
     * Asks Telegram for updates, waiting up to {@code pollTimeout} for one to arrive.
     *
     * <p>⚠️ Not on {@link TelegramTransport}: long polling is a Bot API arrangement, and MTProto
     * delivers updates over a socket it already holds. Putting it on the shared SPI would give every
     * transport a method only one of them can mean.
     *
     * @param offset         the next update id to receive — ⚠️ sending it is also what
     *                       <strong>acknowledges</strong> everything before it, so it must not be
     *                       advanced until the previous batch has been handed over
     * @param pollTimeout    how long Telegram should hold the request open with nothing to say
     * @param allowedUpdates which kinds to receive, or empty for Telegram's default. ⚠️ That default
     *                       excludes {@code chat_member}, so an application that wants to know who
     *                       joined has to ask for it by name
     */
    @Override
    public List<Update> fetchUpdates(
            TelegramIdentity identity,
            long             offset,
            Duration         pollTimeout,
            Set<String>      allowedUpdates) {

        Map<String, Object> fields = new LinkedHashMap<>();

        fields.put("timeout", pollTimeout.toSeconds());

        if (offset > 0) {
            fields.put("offset", offset);
        }

        if (allowedUpdates != null && !allowedUpdates.isEmpty()) {
            fields.put("allowed_updates", List.copyOf(allowedUpdates));
        }

        // ⚠️ The read timeout must outlast the long poll, or the client aborts the very request it
        // asked Telegram to hold open - which looks like Telegram timing out and is us.
        Duration readTimeout = pollTimeout.plusSeconds(10);

        return BotApiUpdates.readAll(call(identity, "getUpdates", fields, readTimeout));
    }

    private SentMessage sendText(TelegramIdentity identity, ChatReference chat, MessageDraft draft) {
        Map<String, Object> fields = BotApiPayload.destination(chat);

        fields.put("text", draft.text());
        BotApiPayload.applyOptions(fields, draft);

        return readMessage(call(identity, "sendMessage", fields));
    }

    private SentMessage sendSingleMedia(TelegramIdentity identity, ChatReference chat, MessageDraft draft) {
        MediaAttachment attachment = draft.attachments().getFirst();
        MediaKind       kind       = attachment.kind();

        Map<String, Object> fields = BotApiPayload.destination(chat);

        // The draft's text becomes the caption; an explicit caption on the attachment wins, since it
        // was set closer to the thing it describes.
        String caption = attachment.hasCaption() ? attachment.caption() : draft.text();

        if (caption != null && !caption.isBlank()) {
            fields.put("caption", caption);
        }

        if (attachment.spoiler()) {
            fields.put("has_spoiler", true);
        }

        BotApiPayload.applyOptions(fields, draft);

        if (!attachment.source().isUpload()) {
            fields.put(kind.field(), reference(attachment.source()));

            return readMessage(call(identity, kind.method(), fields));
        }

        MultipartBody body = new MultipartBody();

        writeFields(body, fields);
        attachUpload(identity, body, kind.field(), attachment.source());

        return readMessage(upload(identity, kind.method(), body));
    }

    private SentMessage sendMediaGroup(TelegramIdentity identity, ChatReference chat, MessageDraft draft) {
        List<MediaAttachment> attachments = draft.attachments();

        requireGroupable(attachments);

        // ⚠️ Telegram refuses reply_markup on sendMediaGroup outright. Refusing here names the field;
        // Telegram's own answer is a generic 400.
        if (draft.replyMarkup() != null) {
            throw new IllegalArgumentException(
                    "a media group cannot carry buttons; send the album, then a message with the markup");
        }

        Map<String, Object>       fields  = BotApiPayload.destination(chat);
        List<Map<String, Object>> entries = new ArrayList<>(attachments.size());
        MultipartBody             body    = new MultipartBody();

        boolean uploading = attachments.stream().anyMatch(item -> item.source().isUpload());

        for (int index = 0; index < attachments.size(); index++) {
            MediaAttachment attachment = attachments.get(index);
            String          reference;

            if (attachment.source().isUpload()) {
                String part = "file" + index;

                attachUpload(identity, body, part, attachment.source());
                reference = "attach://" + part;
            } else {
                reference = reference(attachment.source());
            }

            Map<String, Object> entry = BotApiPayload.groupEntry(attachment, reference, draft);

            // Telegram shows the album's caption from its FIRST entry only, so a draft's text goes
            // there when that entry carries none of its own.
            if (index == 0 && !attachment.hasCaption() && draft.hasText()) {
                entry.put("caption", draft.text());

                if (draft.parseMode().isFormatted()) {
                    entry.put("parse_mode", draft.parseMode().wireValue());
                }
            }

            entries.add(entry);
        }

        if (draft.silent()) {
            fields.put("disable_notification", true);
        }

        if (draft.protectedContent()) {
            fields.put("protect_content", true);
        }

        if (!uploading) {
            fields.put("media", entries);

            return readFirstOfGroup(call(identity, "sendMediaGroup", fields));
        }

        writeFields(body, fields);
        body.field("media", writeValueAsString(entries));

        return readFirstOfGroup(upload(identity, "sendMediaGroup", body));
    }

    /**
     * ⚠️ Telegram allows only photos, videos, documents and audio in an album, and will not mix
     * incompatible kinds. Both refusals arrive as an undifferentiated 400, so they are caught here
     * where the reason can be named.
     */
    private void requireGroupable(List<MediaAttachment> attachments) {
        MediaKind first = attachments.getFirst().kind();

        for (MediaAttachment attachment : attachments) {
            MediaKind kind = attachment.kind();

            if (!kind.groupable()) {
                throw new IllegalArgumentException(
                        "%s cannot appear in a media group".formatted(kind));
            }

            if (!mixable(first, kind)) {
                throw new IllegalArgumentException(
                        "a media group mixes %s with %s; only photos and videos may be mixed"
                                .formatted(first, kind));
            }
        }
    }

    /** Photos and videos mix with each other; documents and audio group only with their own kind. */
    private boolean mixable(MediaKind first, MediaKind other) {
        if (first == other) {
            return true;
        }

        return isVisual(first) && isVisual(other);
    }

    private boolean isVisual(MediaKind kind) {
        return kind == MediaKind.PHOTO || kind == MediaKind.VIDEO;
    }

    private void writeFields(MultipartBody body, Map<String, Object> fields) {
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            Object value = field.getValue();

            if (value == null) {
                continue;
            }

            // A nested structure - reply_markup, link_preview_options - travels through multipart as
            // a JSON-serialised string, which is how Telegram documents every "JSON-serialized" field.
            if (value instanceof Map<?, ?> || value instanceof List<?>) {
                body.field(field.getKey(), writeValueAsString(value));
            } else {
                body.field(field.getKey(), String.valueOf(value));
            }
        }
    }

    private void attachUpload(
            TelegramIdentity identity, MultipartBody body, String part, MediaSource source) {

        switch (source) {
            case MediaSource.Bytes bytes -> {
                requireWithinUploadLimit(identity, bytes.content().length, bytes.fileName());
                body.file(part, bytes.fileName(), bytes.contentType(), bytes.content());
            }

            case MediaSource.LocalFile file -> {
                requireWithinUploadLimit(identity, sizeOf(file), file.path().toString());
                body.file(part, file.path(), file.contentType());
            }

            // Unreachable: isUpload() is what routed us here. Kept so the switch stays exhaustive and
            // a new MediaSource cannot be added without this being considered.
            case MediaSource.FileId ignored    -> throw new IllegalStateException("a file id is not an upload");
            case MediaSource.RemoteUrl ignored -> throw new IllegalStateException("a url is not an upload");
        }
    }

    private long sizeOf(MediaSource.LocalFile file) {
        try {
            return Files.size(file.path());
        } catch (IOException exception) {
            throw new TelegramException(new TelegramRefusal.TransportFailure(
                    "the file to upload could not be read: " + file.path()), exception);
        }
    }

    /**
     * ⚠️ Enforced only against Telegram's own endpoint. Refusing at 50 MB when a self-hosted Bot API
     * server is configured would disable the single arrangement that exists to lift the limit.
     */
    private void requireWithinUploadLimit(TelegramIdentity identity, long bytes, String what) {
        boolean telegramItself = TelegramIdentity.TELEGRAM_API_BASE.equals(identity.apiBaseOrDefault());

        if (telegramItself && bytes > TELEGRAM_UPLOAD_LIMIT) {
            throw new TelegramException(new TelegramRefusal.Rejected(413,
                    ("'%s' is %d bytes; the Bot API accepts %d. Configure a self-hosted "
                     + "telegram-bot-api server on the identity's apiBase to send larger files.")
                            .formatted(what, bytes, TELEGRAM_UPLOAD_LIMIT)));
        }
    }

    private String reference(MediaSource source) {
        return switch (source) {
            case MediaSource.FileId fileId    -> fileId.value();
            case MediaSource.RemoteUrl url    -> url.value();
            case MediaSource.Bytes ignored    -> throw new IllegalStateException("bytes are uploaded, not referenced");
            case MediaSource.LocalFile ignored -> throw new IllegalStateException("a file is uploaded, not referenced");
        };
    }

    /** {@code sendMediaGroup} answers with an array; the first message is the album's handle. */
    private SentMessage readFirstOfGroup(JsonNode result) {
        if (result.isArray() && !result.isEmpty()) {
            List<String> mediaIds = new ArrayList<>();

            result.forEach(message -> mediaIds.addAll(mediaIdsOf(message)));

            SentMessage first = readMessage(result.get(0));

            return new SentMessage(first.chat(), first.messageId(), first.sentAt(), mediaIds);
        }

        return readMessage(result);
    }

    private SentMessage readMessage(JsonNode message) {
        long chatId = message.path("chat").path("id").asLong();

        return new SentMessage(
                ChatReference.of(chatId),
                message.path("message_id").asInt(),
                Instant.ofEpochSecond(message.path("date").asLong()),
                mediaIdsOf(message));
    }

    /**
     * The {@code file_id} of whatever the message carries, so the same bytes can be sent again for
     * free rather than uploaded twice.
     *
     * <p>⚠️ A photo arrives as an array of sizes. The last is the largest, and it is the one worth
     * keeping: a {@code file_id} for a thumbnail re-sends a thumbnail.
     */
    private List<String> mediaIdsOf(JsonNode message) {
        List<String> ids = new ArrayList<>(1);

        JsonNode photo = message.path("photo");

        if (photo.isArray() && !photo.isEmpty()) {
            ids.add(photo.get(photo.size() - 1).path("file_id").asText());
        }

        for (MediaKind kind : MediaKind.values()) {
            JsonNode node = message.path(kind.field());

            if (node.isObject() && node.hasNonNull("file_id")) {
                ids.add(node.get("file_id").asText());
            }
        }

        return ids;
    }
}
