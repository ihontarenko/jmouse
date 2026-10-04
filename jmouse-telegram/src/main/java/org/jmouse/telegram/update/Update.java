package org.jmouse.telegram.update;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.MessageHandle;

/**
 * Something that happened, delivered to us.
 *
 * <h2>⚠️ Sealed, but with an {@link Unknown} member — and that is not a compromise</h2>
 *
 * <p>The obvious reading is that Telegram's set of update types is closed, so a sealed hierarchy can
 * be exhaustive and a product can switch over it with the compiler checking the cases. The first half
 * is wrong: Telegram adds update types regularly — business messages, reactions, boosts and paid
 * media all arrived after bots did — and a hierarchy with no room for one it has never seen would
 * throw on the first delivery after a Telegram release, in an ingestion loop, for an update nobody
 * cared about.
 *
 * <p>So the set is sealed for the value it genuinely gives — a product that adds a case gets a
 * compiler error at every switch — and {@link Unknown} is the member that makes the hierarchy survive
 * Telegram growing. Anything unrecognised becomes one, is logged, and is skipped.
 *
 * <p>⚠️ An implication worth stating: <strong>{@link Unknown} is not an error.</strong> A dispatcher
 * that treats it as one turns every Telegram feature release into an incident.
 */
public sealed interface Update {

    /**
     * Telegram's sequence number for this update.
     *
     * <p>⚠️ What long polling acknowledges with, and the only thing that makes ingestion resumable:
     * the offset is this plus one, and advancing it before an update has been handed over is how an
     * update is lost rather than redelivered.
     */
    long updateId();

    /** Where it happened, or {@code null} for an update that belongs to no chat. */
    ChatReference chat();

    /** A message arrived in a private chat, a group or a supergroup. */
    record MessageReceived(long updateId, IncomingMessage message) implements Update {

        @Override
        public ChatReference chat() {
            return message.chat();
        }
    }

    /** A message was edited. */
    record MessageEdited(long updateId, IncomingMessage message) implements Update {

        @Override
        public ChatReference chat() {
            return message.chat();
        }
    }

    /**
     * A post appeared in a channel.
     *
     * <p>⚠️ Only for a channel this identity is in — a bot must have been added as an administrator,
     * and then sees posts only from that moment on. Reading a channel nobody added it to, or anything
     * posted earlier, needs a user account.
     */
    record ChannelPost(long updateId, IncomingMessage message) implements Update {

        @Override
        public ChatReference chat() {
            return message.chat();
        }
    }

    /** A channel post was edited. */
    record ChannelPostEdited(long updateId, IncomingMessage message) implements Update {

        @Override
        public ChatReference chat() {
            return message.chat();
        }
    }

    /**
     * Somebody pressed an inline button.
     *
     * <p>⚠️ This <strong>owes an answer</strong>. The person's client shows a progress indicator until
     * one arrives — see {@link org.jmouse.telegram.CallbackAnswer}.
     *
     * @param queryId what to answer with
     * @param data    the button's callback data. ⚠️ At most 64 bytes, so it is a key rather than a
     *                payload
     * @param message the message the button is attached to, for editing it in place
     */
    record CallbackPressed(
            long          updateId,
            String        queryId,
            TelegramUser  from,
            MessageHandle message,
            String        data
    ) implements Update {

        @Override
        public ChatReference chat() {
            return message == null ? null : message.chat();
        }

        /** Whether the data begins with a routing prefix, which is how callbacks are dispatched. */
        public boolean dataStartsWith(String prefix) {
            return data != null && data.startsWith(prefix);
        }
    }

    /**
     * Somebody's membership of a chat changed — joined, left, was promoted, was banned.
     *
     * @param about  whose membership changed
     * @param from   who changed it, which for a join is the same person
     * @param status what they are now: Telegram's own word — {@code member}, {@code administrator},
     *               {@code creator}, {@code left}, {@code kicked}, {@code restricted}
     * @param mine   ⚠️ {@code true} when the membership that changed is <em>this identity's own</em>.
     *               That is the update saying the bot was added to or removed from a chat, and it is
     *               delivered as a separate kind by Telegram precisely because it means something
     *               different
     */
    record MembershipChanged(
            long          updateId,
            ChatReference chat,
            TelegramUser  about,
            TelegramUser  from,
            String        status,
            boolean       mine
    ) implements Update {

        public boolean isGone() {
            return "left".equals(status) || "kicked".equals(status);
        }
    }

    /** Somebody asked to join a chat that requires approval. */
    record JoinRequested(
            long          updateId,
            ChatReference chat,
            TelegramUser  from,
            String        inviteLink
    ) implements Update {
    }

    /**
     * Something arrived that this library does not model.
     *
     * <p>⚠️ Expected rather than exceptional — see the note on {@link Update}. Telegram adds update
     * types on its own schedule, and this is what keeps an ingestion loop running through one.
     *
     * @param kind the field name Telegram used, so a log line says what was skipped and a decision to
     *             model it can be taken with evidence
     */
    record Unknown(long updateId, String kind) implements Update {

        @Override
        public ChatReference chat() {
            return null;
        }
    }
}
