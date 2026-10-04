package org.jmouse.telegram;

import org.jmouse.telegram.markup.ReplyMarkup;
import org.jmouse.telegram.media.MediaAttachment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What to send, with no statement about where.
 *
 * <p>⚠️ <strong>The destination is deliberately absent.</strong> A draft is composed once and may go
 * to fifty chats — that is the ordinary shape of a notification — so carrying a {@link ChatReference}
 * inside it would mean rebuilding the body per recipient, and with it re-running whatever escaping and
 * translation produced the text.
 *
 * <p>Built through {@link #builder()} rather than a nine-argument constructor, because most fields are
 * absent most of the time and a call site reading {@code new MessageDraft(text, NONE, List.of(), null,
 * null, false, false, false)} says nothing about which flag is which.
 *
 * @param text          the body, or {@code null} for a media-only message
 * @param parseMode     how Telegram reads the body — see {@link MarkupEscaper} before using a
 *                      formatted one
 * @param attachments   what goes with it; more than one makes it a media group
 * @param replyMarkup   buttons, or {@code null}. ⚠️ Needs {@link Capability#REPLY_MARKUP}
 * @param replyToMessageId the message this answers, or {@code null}
 * @param silent        delivered without a notification sound
 * @param protectedContent forbids forwarding and saving
 * @param withoutPreview suppresses the link preview a URL in the body would otherwise generate
 */
public record MessageDraft(
        String                text,
        ParseMode             parseMode,
        List<MediaAttachment> attachments,
        ReplyMarkup           replyMarkup,
        Integer               replyToMessageId,
        boolean               silent,
        boolean               protectedContent,
        boolean               withoutPreview
) {

    /** Telegram's ceiling on a message body, against 1024 for a media caption. */
    public static final int TEXT_LIMIT = 4096;

    /** Telegram's ceiling on one media group. */
    public static final int MEDIA_GROUP_LIMIT = 10;

    public MessageDraft {
        Objects.requireNonNull(parseMode, "parse mode");
        attachments = attachments == null ? List.of() : List.copyOf(attachments);

        boolean hasText = text != null && !text.isBlank();

        if (!hasText && attachments.isEmpty()) {
            throw new IllegalArgumentException("a draft carries text, an attachment, or both");
        }

        if (attachments.size() > MEDIA_GROUP_LIMIT) {
            throw new IllegalArgumentException(
                    "a media group holds at most %d items; this one has %d"
                            .formatted(MEDIA_GROUP_LIMIT, attachments.size()));
        }

        // ⚠️ Checked here rather than left to Telegram: the refusal for an over-long body arrives as a
        // generic 400 that says nothing about length, which is a genuinely confusing hour to spend.
        if (hasText && text.length() > TEXT_LIMIT) {
            throw new IllegalArgumentException(
                    "a message body is at most %d characters; this one is %d"
                            .formatted(TEXT_LIMIT, text.length()));
        }
    }

    /** Plain unformatted text — the safe default, since nothing in it needs escaping. */
    public static MessageDraft text(String text) {
        return builder().text(text).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    /** More than one attachment is a media group, which Telegram sends by a different method. */
    public boolean isMediaGroup() {
        return attachments.size() > 1;
    }

    /**
     * Which capabilities a transport must have to send this.
     *
     * <p>⚠️ Asked by the gateway <em>before</em> the call, so a missing capability is a refusal naming
     * what was needed rather than a partial send with the buttons quietly dropped.
     */
    public List<Capability> requiredCapabilities() {
        List<Capability> required = new ArrayList<>(3);

        required.add(Capability.SEND_MESSAGE);

        if (hasAttachments()) {
            required.add(Capability.SEND_MEDIA);
        }

        if (replyMarkup != null) {
            required.add(Capability.REPLY_MARKUP);
        }

        return List.copyOf(required);
    }

    /** Mutable while composing, immutable once built. */
    public static final class Builder {

        private final List<MediaAttachment> attachments = new ArrayList<>();

        private String      text;
        private ParseMode   parseMode = ParseMode.NONE;
        private ReplyMarkup replyMarkup;
        private Integer     replyToMessageId;
        private boolean     silent;
        private boolean     protectedContent;
        private boolean     withoutPreview;

        private Builder() {
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * Body and mode together, so the two cannot drift apart.
         *
         * <p>⚠️ The text is <strong>not</strong> escaped for you: escaping a finished body would
         * destroy the formatting it was written to carry. Escape the values as they go in — see
         * {@link MarkupEscaper}.
         */
        public Builder text(String text, ParseMode parseMode) {
            this.text      = text;
            this.parseMode = Objects.requireNonNull(parseMode, "parse mode");
            return this;
        }

        public Builder attach(MediaAttachment attachment) {
            attachments.add(Objects.requireNonNull(attachment, "attachment"));
            return this;
        }

        public Builder attach(List<MediaAttachment> several) {
            several.forEach(this::attach);
            return this;
        }

        public Builder markup(ReplyMarkup replyMarkup) {
            this.replyMarkup = replyMarkup;
            return this;
        }

        public Builder replyTo(int messageId) {
            this.replyToMessageId = messageId;
            return this;
        }

        /** Delivered without a sound — right for anything routine and frequent. */
        public Builder silent() {
            this.silent = true;
            return this;
        }

        public Builder protectedContent() {
            this.protectedContent = true;
            return this;
        }

        public Builder withoutPreview() {
            this.withoutPreview = true;
            return this;
        }

        public MessageDraft build() {
            return new MessageDraft(text, parseMode, attachments, replyMarkup,
                    replyToMessageId, silent, protectedContent, withoutPreview);
        }
    }
}
