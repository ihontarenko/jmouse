package org.jmouse.telegram.command;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.media.MediaAttachment;
import org.jmouse.telegram.media.MediaSource;
import org.jmouse.telegram.update.Attachment;
import org.jmouse.telegram.update.IncomingMessage;
import org.jmouse.telegram.update.TelegramUser;

import org.jmouse.telegram.markup.InlineButton;
import org.jmouse.telegram.markup.ReplyMarkup;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Everything a handler needs: who asked, as whom they act, what they wrote, and how to answer.
 *
 * <p>⚠️ {@link #subject()} is the product identity to act as — resolved from a binding, checked by the
 * guard. A handler that reached into the product as anybody else, or as the bot, would turn a Telegram
 * account into a key to somebody else's data.
 */
public final class CommandContext {

    private final IncomingMessage message;
    private final String          subject;
    private final String          command;
    private final String          arguments;
    private final TelegramGateway gateway;
    private final CommandRouter   router;

    CommandContext(IncomingMessage message, String subject, String command, String arguments,
                   TelegramGateway gateway, CommandRouter router) {
        this.message   = message;
        this.subject   = subject;
        this.command   = command;
        this.arguments = arguments;
        this.gateway   = gateway;
        this.router    = router;
    }

    /** The product subject the sender is bound as — the identity every product call must use. */
    public String subject() {
        return subject;
    }

    public TelegramUser sender() {
        return message.from();
    }

    /** The command's name, without the slash. */
    public String command() {
        return command;
    }

    /** Everything after the command, trimmed; empty when nothing followed. */
    public String arguments() {
        return arguments;
    }

    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    public ChatReference chat() {
        return message.chat();
    }

    public IncomingMessage message() {
        return message;
    }

    /** What the message carries — a document, a photo — by reference; empty for a typed or pressed command. */
    public List<Attachment> attachments() {
        return message.attachments();
    }

    /**
     * The files of this chat as storage content — see {@link TelegramFiles}. Fetched through the same gateway
     * that heard the message, which is the only identity their {@code file_id} is valid for.
     */
    public TelegramFiles files() {
        return new TelegramFiles(gateway);
    }

    /** The sender's language as Telegram reported it, or {@code null} — for a product that translates. */
    public String languageCode() {
        return message.from() == null ? null : message.from().languageCode();
    }

    /** Plain text, no markup — nothing to escape, nothing to break. */
    public SentMessage reply(String text) {
        return gateway.send(message.chat(), MessageDraft.text(text));
    }

    public SentMessage reply(MessageDraft draft) {
        return gateway.send(message.chat(), draft);
    }

    /**
     * Text of any length: one message when it fits, otherwise pages with ‹ › buttons that only this person
     * can turn.
     */
    public SentMessage replyLong(String text) {
        return router.replyInPages(this, text);
    }

    /**
     * Text with buttons under it, one per line — each runs a command as if it were typed.
     *
     * <p>⚠️ At most a page of text: buttons and paging do not combine, so a list with actions is kept short
     * (and offers a "more" action of its own if it has to).
     */
    public SentMessage replyWithActions(String text, List<CommandAction> actions) {
        MessageDraft.Builder draft = MessageDraft.builder().text(text).withoutPreview();

        if (!actions.isEmpty()) {
            draft.markup(ReplyMarkup.InlineKeyboard.stacked(
                    actions.stream().map(CommandAction::button).toArray(InlineButton[]::new)));
        }

        return gateway.send(message.chat(), draft.build());
    }

    /** A whole document as a file — for something meant to be read rather than scrolled. */
    public SentMessage replyDocument(String fileName, String content, String caption) {
        MediaAttachment document = MediaAttachment
                .document(new MediaSource.Bytes(fileName, content.getBytes(StandardCharsets.UTF_8), "text/plain"));

        MessageDraft.Builder draft = MessageDraft.builder()
                .attach(caption == null ? document : document.withCaption(caption));

        return gateway.send(message.chat(), draft.build());
    }

    TelegramGateway gateway() {
        return gateway;
    }
}
