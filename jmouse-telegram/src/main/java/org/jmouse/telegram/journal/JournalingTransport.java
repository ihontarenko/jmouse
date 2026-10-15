package org.jmouse.telegram.journal;

import org.jmouse.telegram.CallbackAnswer;
import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.spi.TelegramTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A decorator that writes one {@link JournalEntry} per call, delivered or refused.
 *
 * <pre>{@code
 * TelegramTransport transport = new JournalingTransport(new PacedTransport(bot, pace), journal);
 * }</pre>
 *
 * <p>⚠️ <strong>Wrap it OUTSIDE {@code PacedTransport}.</strong> Outside, an entry is the call's final
 * outcome with the retries and waits inside its {@code elapsed}; inside, a flood wait that was retried
 * and then went through would leave a refusal in the journal for a message that arrived.
 *
 * <p>⚠️ A journal that fails to record is logged and ignored — the call has already happened, and
 * failing it now would report a failure that did not.
 */
public final class JournalingTransport implements TelegramTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalingTransport.class);

    private final TelegramTransport delegate;
    private final TelegramJournal   journal;
    private final Clock             clock;

    public JournalingTransport(TelegramTransport delegate, TelegramJournal journal) {
        this(delegate, journal, Clock.systemUTC());
    }

    public JournalingTransport(TelegramTransport delegate, TelegramJournal journal, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate transport");
        this.journal  = Objects.requireNonNull(journal, "journal");
        this.clock    = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public IdentityKind kind() {
        return delegate.kind();
    }

    @Override
    public boolean supports(Capability capability) {
        return delegate.supports(capability);
    }

    @Override
    public SentMessage send(TelegramIdentity identity, ChatReference chat, MessageDraft draft) {
        return journaled(identity, JournalEntry.Operation.SEND, chat, null,
                         () -> delegate.send(identity, chat, draft));
    }

    @Override
    public SentMessage edit(TelegramIdentity identity, MessageHandle message, MessageDraft draft) {
        return journaled(identity, JournalEntry.Operation.EDIT, message.chat(), message.messageId(),
                         () -> delegate.edit(identity, message, draft));
    }

    @Override
    public void delete(TelegramIdentity identity, MessageHandle message) {
        journaled(identity, JournalEntry.Operation.DELETE, message.chat(), message.messageId(), () -> {
            delegate.delete(identity, message);
            return null;
        });
    }

    @Override
    public void answerCallback(TelegramIdentity identity, CallbackAnswer answer) {
        journaled(identity, JournalEntry.Operation.ANSWER_CALLBACK, null, null, () -> {
            delegate.answerCallback(identity, answer);
            return null;
        });
    }

    private <T> T journaled(
            TelegramIdentity identity, JournalEntry.Operation operation,
            ChatReference chat, Integer knownMessageId, Supplier<T> call) {
        Instant started = clock.instant();

        try {
            T       result    = call.get();
            Integer messageId = result instanceof SentMessage sent ? Integer.valueOf(sent.messageId()) : knownMessageId;

            write(new JournalEntry(clock.instant(), nameOf(identity), operation, wireOf(chat), threadOf(chat),
                                   messageId, JournalEntry.Outcome.DELIVERED, null, null, since(started)));

            return result;
        } catch (TelegramException exception) {
            write(new JournalEntry(clock.instant(), nameOf(identity), operation, wireOf(chat), threadOf(chat),
                                   knownMessageId, JournalEntry.Outcome.REFUSED,
                                   exception.refusal().getClass().getSimpleName(),
                                   exception.refusal().message(), since(started)));

            throw exception;
        }
    }

    private void write(JournalEntry entry) {
        try {
            journal.record(entry);
        } catch (RuntimeException exception) {
            LOGGER.warn("the Telegram journal could not record a {} to {}; the call itself was not affected",
                        entry.operation(), entry.chat(), exception);
        }
    }

    private Duration since(Instant started) {
        return Duration.between(started, clock.instant());
    }

    private static String nameOf(TelegramIdentity identity) {
        return identity == null ? null : identity.name();
    }

    private static String wireOf(ChatReference chat) {
        return chat == null ? null : chat.wireValue();
    }

    private static Integer threadOf(ChatReference chat) {
        return chat == null ? null : chat.threadId();
    }
}
