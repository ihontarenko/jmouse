package org.jmouse.notifier.telegram.smoke;

import org.jmouse.notifier.Address;
import org.jmouse.notifier.Addressee;
import org.jmouse.notifier.Delivery;
import org.jmouse.notifier.InMemoryNotificationStore;
import org.jmouse.notifier.Notification;
import org.jmouse.notifier.Notifier;
import org.jmouse.notifier.PublicationReport;
import org.jmouse.notifier.Recipient;
import org.jmouse.notifier.Sender;
import org.jmouse.notifier.telegram.TelegramAddressBook;
import org.jmouse.notifier.telegram.TelegramAddresses;
import org.jmouse.notifier.telegram.TelegramChannel;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.RoutingGateway;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.markup.ReplyMarkup;
import org.jmouse.telegram.test.RecordingTransport;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.jmouse.notifier.smoke.Checks.check;
import static org.jmouse.notifier.smoke.Checks.report;
import static org.jmouse.notifier.smoke.Checks.throwsIllegalArgument;

/**
 * Runs the Telegram channel against {@code jmouse-telegram}'s {@link RecordingTransport}. Run its
 * {@code main}; it reaches no network and needs no bot token.
 */
public final class TelegramChannelSmoke {

    private static final long CHAT = 4242L;

    public static void main(String[] arguments) {
        deliversTheEscapedMessageWithItsSender();
        sendsInfoSilentlyAndAnAlarmAloud();
        turnsLinksIntoButtons();
        keepsTheReceipt();
        retriesAFloodWaitAfterExactlyItsDelay();
        forgetsABlockedChat();
        keepsTheBindingOfSomebodyWhoNeverStartedTheBot();
        cutsAnOverlongBodyByItsEscapedLength();
        readsAddressesBack();
        report();
    }

    private record Fixture(RecordingTransport transport, Notifier notifier, AddressBook book) {
    }

    /** One person, one chat, and a record of what was declared unreachable. */
    private static final class AddressBook implements TelegramAddressBook {

        private final List<ChatReference> unreachable = new ArrayList<>();

        @Override
        public List<ChatReference> chatsOf(Addressee addressee) {
            return "ivan".equals(addressee.subject()) ? List.of(ChatReference.of(CHAT)) : List.of();
        }

        @Override
        public void unreachable(ChatReference chat) {
            unreachable.add(chat);
        }
    }

    private static Fixture fixture() {
        RecordingTransport transport = new RecordingTransport();
        TelegramGateway    gateway   = new RoutingGateway(
                IdentitySource.fixed(TelegramIdentity.bot("smoke", "token")), List.of(transport));
        AddressBook        book      = new AddressBook();
        TelegramChannel    channel   = new TelegramChannel(gateway, "smoke-notifications", book);
        Notifier           notifier  = new Notifier(
                new InMemoryNotificationStore(), recipient -> List.of(Addressee.of("ivan")), List.of(channel));

        return new Fixture(transport, notifier, book);
    }

    private static PublicationReport publish(Fixture fixture, Notification notification) {
        return fixture.notifier().publish(new Sender.Rule("downloads"), new Recipient.Subject("ivan"), notification);
    }

    private static void deliversTheEscapedMessageWithItsSender() {
        Fixture fixture = fixture();
        publish(fixture, Notification.info("Alien (1979) & <Aliens>").titled("Films"));

        String text = fixture.transport().lastCall().draft().text();
        check("sent to the bound chat", fixture.transport().lastCall().chat().chatId() == CHAT);
        check("the title is bold", text.startsWith("<b>Films</b>"));
        check("the body is escaped", text.contains("Alien (1979) &amp; &lt;Aliens&gt;"));
        check("the sender signs it", text.endsWith("<i>Rule: downloads</i>"));
    }

    private static void sendsInfoSilentlyAndAnAlarmAloud() {
        Fixture fixture = fixture();
        publish(fixture, Notification.info("quiet"));
        boolean infoSilent = fixture.transport().lastCall().draft().silent();
        publish(fixture, Notification.alarm("loud"));
        boolean alarmSilent = fixture.transport().lastCall().draft().silent();

        check("info is silent, an alarm is not", infoSilent && !alarmSilent);
    }

    private static void turnsLinksIntoButtons() {
        Fixture fixture = fixture();
        publish(fixture, Notification.info("ready").linking("Open", "https://example.org/film/1"));

        MessageDraft draft = fixture.transport().lastCall().draft();
        check("a link becomes a button", draft.replyMarkup() instanceof ReplyMarkup.InlineKeyboard keyboard
                && keyboard.rows().getFirst().getFirst().text().equals("Open"));
    }

    private static void keepsTheReceipt() {
        PublicationReport report = publish(fixture(), Notification.info("receipt"));

        check("the receipt names the chat and the message", report.deliveries().getFirst().receipt().startsWith(CHAT + ":"));
    }

    private static void retriesAFloodWaitAfterExactlyItsDelay() {
        Fixture fixture = fixture();
        fixture.transport().willRefuse(new TelegramRefusal.FloodWait(Duration.ofSeconds(17)));

        Delivery delivery = publish(fixture, Notification.info("later")).deliveries().getFirst();
        check("a flood wait leaves it pending", delivery.isPending());
        check("due after exactly the delay Telegram gave",
              Duration.between(delivery.updatedAt(), delivery.nextAttemptAt()).equals(Duration.ofSeconds(17)));
    }

    private static void forgetsABlockedChat() {
        Fixture fixture = fixture();
        fixture.transport().willRefuse(new TelegramRefusal.BotBlocked(String.valueOf(CHAT)));

        PublicationReport report = publish(fixture, Notification.info("blocked"));
        check("a blocked bot is a failure", report.failed() == 1);
        check("and the address book is told", fixture.book().unreachable.size() == 1);
    }

    private static void keepsTheBindingOfSomebodyWhoNeverStartedTheBot() {
        Fixture fixture = fixture();
        fixture.transport().willRefuse(new TelegramRefusal.ConversationNotStarted(String.valueOf(CHAT)));

        PublicationReport report = publish(fixture, Notification.info("shared bot"));
        check("a bot nobody started is a failure", report.failed() == 1
                && report.deliveries().getFirst().reason().contains("press Start"));
        check("but the binding is kept", fixture.book().unreachable.isEmpty());
    }

    private static void cutsAnOverlongBodyByItsEscapedLength() {
        Fixture fixture = fixture();
        PublicationReport report = publish(fixture, Notification.info("&".repeat(5000)));

        String text = fixture.transport().lastCall().draft().text();
        check("an overlong body is still delivered", report.delivered() == 1);
        check("within Telegram's limit once escaped", text.length() <= MessageDraft.TEXT_LIMIT);
        check("with no entity split in half", !text.contains("&amp…") && text.contains("&amp;…"));
    }

    private static void readsAddressesBack() {
        ChatReference topic = ChatReference.of(-1001234567890L).inThread(7);
        Address       address = TelegramAddresses.of(topic);

        check("a topic survives the round trip", TelegramAddresses.chatOf(address).equals(topic));
        check("a username survives it", TelegramAddresses.chatOf(new Address("telegram", "@operators")).username().equals("@operators"));
        check("nonsense is refused with a sentence",
              throwsIllegalArgument(() -> TelegramAddresses.chatOf(new Address("telegram", "my chat"))));
    }
}
