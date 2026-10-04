package org.jmouse.telegram.jpa;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.update.TelegramUser;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Which chat reaches which of the product's subjects.
 *
 * <h2>⚠️ This is the thing whose absence blocks notifications entirely</h2>
 *
 * <p>You cannot send to a person. You can only send to a chat id. So something has to learn, once, that
 * <em>this</em> subject of the product corresponds to <em>that</em> Telegram chat — and a bot cannot go
 * and find out, because it may not write to somebody who has not written to it first.
 *
 * <p>The flow, which is why this is a port rather than a table:
 *
 * <ol>
 *   <li>the product asks for an invitation for one of its subjects</li>
 *   <li>it shows {@code t.me/<bot>?start=<token>} — a link, or a QR code</li>
 *   <li>the person opens it; Telegram sends the bot {@code /start <token>} from their own chat</li>
 *   <li>{@link #complete} records the chat and burns the token</li>
 * </ol>
 *
 * <h2>⚠️ A "subject" is the PRODUCT's identifier and this library never interprets one</h2>
 *
 * <p>A person, a workspace, a team, a shared channel — all of them are a string here. The moment this
 * module knows what a subject <em>is</em>, it has learned a product's domain and every other product
 * has to phrase itself in those terms.
 */
public interface TelegramBindings {

    /** How long an invitation is good for when nothing says otherwise. */
    Duration DEFAULT_VALIDITY = Duration.ofHours(24);

    /**
     * Starts a binding for a subject.
     *
     * <p>⚠️ A previous unused invitation for the same subject is <strong>replaced</strong>, not added
     * to. Several live tokens for one subject means several links in the wild, any of which binds
     * whoever finds it — and a person who asks twice because the first link scrolled away should not
     * leave a working one behind.
     */
    Invitation invite(String subject, Duration validFor);

    /** As {@link #invite(String, Duration)} with {@link #DEFAULT_VALIDITY}. */
    default Invitation invite(String subject) {
        return invite(subject, DEFAULT_VALIDITY);
    }

    /**
     * Finishes a binding from a {@code /start <token>} message.
     *
     * @param token what arrived after the command
     * @param chat  where it arrived from — ⚠️ the chat, not the person: that is what a send addresses
     * @param from  who sent it, so the language can be captured
     * @return the binding, or empty when the token is unknown, expired or already used. ⚠️ Empty rather
     *         than an exception, and indistinguishable between those three cases: telling somebody
     *         <em>which</em> it was lets them probe for live tokens
     */
    Optional<Binding> complete(String token, ChatReference chat, TelegramUser from);

    /** Every deliverable binding for a subject — what a notification iterates. */
    List<Binding> deliverableFor(String subject);

    /** Every binding for a subject, deliverable or not, for an administration listing. */
    List<Binding> allFor(String subject);

    /**
     * Stops sending to a chat.
     *
     * <p>⚠️ <strong>This is what a {@code BotBlocked} refusal must call.</strong> The person turned the
     * bot off; the binding claiming they are reachable has stopped being true, and leaving it active
     * means an outbox accumulating work that can never be delivered and never cleared.
     *
     * @return whether anything was deactivated
     */
    boolean deactivate(ChatReference chat);

    /** Removes a binding outright — for a person who asks to be forgotten. */
    boolean remove(String subject, ChatReference chat);

    /**
     * An invitation waiting to be accepted.
     *
     * @param subject   whose binding this will be
     * @param token     ⚠️ opaque and unguessable. It is not the subject in any encoding — a token
     *                  derived from an identifier is a token anybody can compute for anybody
     * @param expiresAt after which it does nothing
     */
    record Invitation(String subject, String token, LocalDateTime expiresAt) {

        /**
         * The link to show.
         *
         * <p>⚠️ Takes the bot's username because this library does not know it — an identity carries a
         * token, not a name, and the name comes from Telegram's {@code getMe} or from the product's own
         * configuration. Passing it in keeps a network call out of building a link.
         */
        public String deepLink(String botUsername) {
            String name = botUsername.startsWith("@") ? botUsername.substring(1) : botUsername;

            return "https://t.me/%s?start=%s".formatted(name, token);
        }
    }

    /**
     * A chat that can be reached on a subject's behalf.
     *
     * @param subject      the product's own identifier
     * @param chat         where to send
     * @param telegramUserId who accepted it, for an administration screen — ⚠️ the id, never the
     *                     username, which is optional and changeable
     * @param languageCode their language as Telegram reported it, or null
     * @param deliverable  false once the bot was blocked or the binding was switched off
     */
    record Binding(
            String        subject,
            ChatReference chat,
            long          telegramUserId,
            String        languageCode,
            boolean       deliverable
    ) {
    }
}
