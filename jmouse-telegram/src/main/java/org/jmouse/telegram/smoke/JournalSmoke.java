package org.jmouse.telegram.smoke;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.RoutingGateway;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.journal.JournalEntry;
import org.jmouse.telegram.journal.JournalPage;
import org.jmouse.telegram.journal.JournalQuery;
import org.jmouse.telegram.journal.JournalingTransport;
import org.jmouse.telegram.journal.MemoryTelegramJournal;
import org.jmouse.telegram.journal.TelegramJournal;
import org.jmouse.telegram.pace.Pace;
import org.jmouse.telegram.pace.PacedTransport;
import org.jmouse.telegram.test.RecordingTransport;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.jmouse.telegram.smoke.Checks.check;
import static org.jmouse.telegram.smoke.Checks.refusalOf;

/**
 * Runs {@link JournalingTransport} and {@link MemoryTelegramJournal} against the fake transport. Run its
 * {@code main}; no network.
 */
public final class JournalSmoke {

    private static final ChatReference CHAT = ChatReference.of(-1001234567890L).inThread(7);

    public static void main(String[] arguments) {
        journalsADeliveryWithItsMessageAndIdentity();
        journalsARefusalWithItsCaseAndSentence();
        journalsTheFinalOutcomeNotEachRetry();
        neverStoresTheText();
        filtersAndPagesNewestFirst();
        prunesByAge();
        aFailingJournalDoesNotFailTheSend();
        Checks.report();
    }

    private record Fixture(RecordingTransport transport, TelegramGateway gateway, MemoryTelegramJournal journal) {
    }

    private static Fixture fixture() {
        RecordingTransport    transport = new RecordingTransport();
        MemoryTelegramJournal journal   = new MemoryTelegramJournal();
        TelegramGateway       gateway   = new RoutingGateway(
                IdentitySource.fixed(TelegramIdentity.bot("shared-notifications", "secret-token")),
                List.of(new JournalingTransport(new PacedTransport(transport, Pace.unlimited(), 3, Duration.ZERO), journal)));

        return new Fixture(transport, gateway, journal);
    }

    private static JournalEntry latest(Fixture fixture) {
        return fixture.journal().list(JournalQuery.latest()).entries().getFirst();
    }

    private static void journalsADeliveryWithItsMessageAndIdentity() {
        Fixture     fixture = fixture();
        SentMessage sent    = fixture.gateway().send(CHAT, MessageDraft.text("hello"));
        JournalEntry entry  = latest(fixture);

        check("a delivery is journaled", entry.outcome() == JournalEntry.Outcome.DELIVERED);
        check("with the identity's name", "shared-notifications".equals(entry.identityName()));
        check("with the chat and its topic", "-1001234567890".equals(entry.chat()) && entry.threadId() == 7);
        check("with the message id Telegram gave", entry.messageId() == sent.messageId());
    }

    private static void journalsARefusalWithItsCaseAndSentence() {
        Fixture fixture = fixture();
        fixture.transport().willRefuse(new TelegramRefusal.BotBlocked("-1001234567890"));
        refusalOf(() -> fixture.gateway().send(CHAT, MessageDraft.text("blocked")));
        JournalEntry entry = latest(fixture);

        check("a refusal is journaled", entry.refused());
        check("by its case name", "BotBlocked".equals(entry.refusal()));
        check("with its sentence", entry.detail() != null && entry.detail().contains("blocked"));
    }

    private static void journalsTheFinalOutcomeNotEachRetry() {
        Fixture fixture = fixture();
        fixture.transport().willRefuse(new TelegramRefusal.FloodWait(Duration.ZERO));
        fixture.gateway().send(CHAT, MessageDraft.text("retried"));
        JournalPage page = fixture.journal().list(JournalQuery.latest());

        check("a retried send is one entry, not two", page.total() == 1);
        check("and it says delivered", page.entries().getFirst().outcome() == JournalEntry.Outcome.DELIVERED);
    }

    private static void neverStoresTheText() {
        Fixture fixture = fixture();
        fixture.gateway().send(CHAT, MessageDraft.text("a private sentence"));

        check("no field holds the text or the token", !latest(fixture).toString().contains("private sentence")
                && !latest(fixture).toString().contains("secret-token"));
    }

    private static void filtersAndPagesNewestFirst() {
        Fixture fixture = fixture();

        for (int number = 0; number < 5; number++) {
            fixture.gateway().send(ChatReference.of(100L + number), MessageDraft.text("n" + number));
        }

        fixture.transport().willRefuse(new TelegramRefusal.ChatNotFound("999"));
        refusalOf(() -> fixture.gateway().send(ChatReference.of(999L), MessageDraft.text("lost")));

        JournalPage refusals = fixture.journal().list(JournalQuery.refusals());
        JournalPage second   = fixture.journal().list(new JournalQuery(null, null, null, null, 2, 2));
        JournalPage oneChat  = fixture.journal().list(new JournalQuery(null, "102", null, null, 0, 10));

        check("only the refusal is a refusal", refusals.total() == 1 && "999".equals(refusals.entries().getFirst().chat()));
        check("a page carries the total", second.total() == 6 && second.entries().size() == 2);
        check("newest first", "103".equals(second.entries().getFirst().chat()));
        check("filtered by chat", oneChat.total() == 1);
    }

    private static void prunesByAge() {
        MemoryTelegramJournal journal = new MemoryTelegramJournal();
        Instant               now     = Instant.now();

        journal.record(entryAt(now.minus(Duration.ofDays(40))));
        journal.record(entryAt(now));

        check("pruning forgets only the old", journal.prune(now.minus(Duration.ofDays(30))) == 1
                && journal.list(JournalQuery.latest()).total() == 1);
    }

    private static void aFailingJournalDoesNotFailTheSend() {
        TelegramJournal broken = new TelegramJournal() {
            @Override
            public void record(JournalEntry entry) {
                throw new IllegalStateException("database down");
            }

            @Override
            public JournalPage list(JournalQuery query) {
                return new JournalPage(List.of(), 0);
            }

            @Override
            public long prune(Instant before) {
                return 0;
            }
        };
        TelegramGateway gateway = new RoutingGateway(
                IdentitySource.fixed(TelegramIdentity.bot("smoke", "token")),
                List.of(new JournalingTransport(new RecordingTransport(), broken)));

        check("a send survives a journal that cannot write", gateway.send(CHAT, MessageDraft.text("still")) != null);
    }

    private static JournalEntry entryAt(Instant at) {
        return new JournalEntry(at, "smoke", JournalEntry.Operation.SEND, "1", null, 1,
                                JournalEntry.Outcome.DELIVERED, null, null, Duration.ZERO);
    }
}
