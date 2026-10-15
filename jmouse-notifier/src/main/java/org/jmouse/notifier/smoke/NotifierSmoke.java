package org.jmouse.notifier.smoke;

import org.jmouse.notifier.Address;
import org.jmouse.notifier.Addressee;
import org.jmouse.notifier.Channel;
import org.jmouse.notifier.Delivery;
import org.jmouse.notifier.DeliveryOutcome;
import org.jmouse.notifier.InMemoryNotificationStore;
import org.jmouse.notifier.Notification;
import org.jmouse.notifier.Notifier;
import org.jmouse.notifier.Publication;
import org.jmouse.notifier.PublicationReport;
import org.jmouse.notifier.Recipient;
import org.jmouse.notifier.RecipientResolver;
import org.jmouse.notifier.Sender;
import org.jmouse.notifier.test.RecordingChannel;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.jmouse.notifier.smoke.Checks.check;
import static org.jmouse.notifier.smoke.Checks.throwsIllegalArgument;

/**
 * Runs the notifier against {@link RecordingChannel} and checks what it actually does. Run its
 * {@code main}; it reaches no network.
 */
public final class NotifierSmoke {

    private static final Sender SENDER = new Sender.System("Smoke");

    public static void main(String[] arguments) {
        deliversToEveryAddressOnEveryChannel();
        deliversOnceToAPersonResolvedThroughTwoDevices();
        leavesTheExcludedDeviceOut();
        sendsDirectWithoutAskingTheResolver();
        failsAnAddressOnAChannelNobodyInstalled();
        aRepeatedIdempotencyKeyDeliversNothing();
        retriesAfterExactlyTheDelayItWasGiven();
        givesUpAfterTheMaximumAttempts();
        isolatesAChannelThatThrows();
        refusesTwoChannelsWithOneName();
        refusesABlankNotification();
        forgetsTheOldestBeyondCapacity();
        Checks.report();
    }

    private static RecipientResolver everyoneIs(String... subjects) {
        return recipient -> List.of(subjects).stream().map(Addressee::of).toList();
    }

    private static void deliversToEveryAddressOnEveryChannel() {
        RecordingChannel telegram = new RecordingChannel("telegram").reach("ivan", "100", "200");
        RecordingChannel screen   = new RecordingChannel("screen").reach("ivan", "tv");
        Notifier         notifier = new Notifier(new InMemoryNotificationStore(), everyoneIs("ivan"), List.of(telegram, screen));

        PublicationReport report = notifier.publish(SENDER, new Recipient.Subject("ivan"), Notification.info("hello"));

        check("two chats and one screen are three deliveries", report.deliveries().size() == 3);
        check("all three delivered", report.delivered() == 3);
        check("telegram was asked twice", telegram.calls().size() == 2);
        check("the receipt is kept", report.deliveries().getFirst().receipt() != null);
    }

    private static void deliversOnceToAPersonResolvedThroughTwoDevices() {
        RecordingChannel  telegram = new RecordingChannel("telegram").reach("ivan", "100");
        RecipientResolver resolver = recipient -> List.of(Addressee.onDevice("ivan", "tv"), Addressee.onDevice("ivan", "browser"));
        Notifier          notifier = new Notifier(new InMemoryNotificationStore(), resolver, List.of(telegram));

        notifier.publish(SENDER, new Recipient.Everyone(null), Notification.info("once"));

        check("one person on two devices gets one Telegram message", telegram.calls().size() == 1);
    }

    private static void leavesTheExcludedDeviceOut() {
        RecordingChannel  screen   = new RecordingChannel("screen").reach("ivan", "tv").reach("olena", "browser");
        RecipientResolver resolver = recipient -> List.of(Addressee.onDevice("ivan", "tv"), Addressee.onDevice("olena", "browser"));
        Notifier          notifier = new Notifier(new InMemoryNotificationStore(), resolver, List.of(screen));

        notifier.publish(SENDER, new Recipient.Except(new Recipient.Everyone(null), "tv"), Notification.info("started"));

        check("the device where it happened is left out", screen.calls().size() == 1
                && screen.calls().getFirst().address().value().equals("browser"));
    }

    private static void sendsDirectWithoutAskingTheResolver() {
        RecordingChannel  telegram = new RecordingChannel("telegram");
        RecipientResolver resolver = recipient -> {
            throw new IllegalStateException("a direct recipient must not be resolved");
        };
        Notifier          notifier = new Notifier(new InMemoryNotificationStore(), resolver, List.of(telegram));

        PublicationReport report = notifier.publish(
                SENDER, new Recipient.Direct(new Address("telegram", "-1001")), Notification.warn("operators"));

        check("a direct address is delivered as given", report.delivered() == 1
                && telegram.calls().getFirst().address().value().equals("-1001"));
    }

    private static void failsAnAddressOnAChannelNobodyInstalled() {
        Notifier notifier = new Notifier(new InMemoryNotificationStore(), everyoneIs(), List.of(new RecordingChannel("telegram")));

        PublicationReport report = notifier.publish(
                SENDER, new Recipient.Direct(new Address("sms", "+380")), Notification.info("text"));

        check("an unknown channel is a failure with a sentence", report.failed() == 1
                && report.deliveries().getFirst().reason().contains("sms"));
    }

    private static void aRepeatedIdempotencyKeyDeliversNothing() {
        RecordingChannel telegram = new RecordingChannel("telegram").reach("ivan", "100");
        Notifier         notifier = new Notifier(new InMemoryNotificationStore(), everyoneIs("ivan"), List.of(telegram));

        PublicationReport first  = notifier.publish(SENDER, new Recipient.Subject("ivan"), Notification.info("once"), "download:42");
        PublicationReport second = notifier.publish(SENDER, new Recipient.Subject("ivan"), Notification.info("once"), "download:42");

        check("the second publish is marked repeated", second.repeated());
        check("it reports the first publication", second.publication().id().equals(first.publication().id()));
        check("and nothing was delivered twice", telegram.calls().size() == 1);
    }

    private static void retriesAfterExactlyTheDelayItWasGiven() {
        MovableClock     clock    = new MovableClock();
        RecordingChannel telegram = new RecordingChannel("telegram").reach("ivan", "100")
                .willAnswer(new DeliveryOutcome.RetryAfter(Duration.ofSeconds(30), "flood wait"));
        Notifier         notifier = new Notifier(new InMemoryNotificationStore(), everyoneIs("ivan"), List.of(telegram), clock, 5);

        PublicationReport report = notifier.publish(SENDER, new Recipient.Subject("ivan"), Notification.info("later"));
        check("told to wait, it stays pending", report.pending() == 1);

        clock.advance(Duration.ofSeconds(29));
        check("not attempted a second early", notifier.retryDue() == 0);

        clock.advance(Duration.ofSeconds(1));
        check("attempted when the delay is over", notifier.retryDue() == 1);
        check("and delivered on the second attempt", telegram.calls().size() == 2);
    }

    private static void givesUpAfterTheMaximumAttempts() {
        MovableClock     clock    = new MovableClock();
        RecordingChannel telegram = new RecordingChannel("telegram").reach("ivan", "100")
                .willAnswer(new DeliveryOutcome.RetryAfter(Duration.ZERO, "down"))
                .willAnswer(new DeliveryOutcome.RetryAfter(Duration.ZERO, "down"));
        InMemoryNotificationStore store    = new InMemoryNotificationStore();
        Notifier                  notifier = new Notifier(store, everyoneIs("ivan"), List.of(telegram), clock, 2);

        PublicationReport report = notifier.publish(SENDER, new Recipient.Subject("ivan"), Notification.info("never"));
        notifier.retryDue();

        Delivery latest = store.deliveriesOf(report.publication().id()).getFirst();
        check("at the ceiling a retry becomes a failure", latest.state() == Delivery.State.FAILED
                && latest.reason().startsWith("gave up after 2 attempts"));
        check("and nothing more is attempted", notifier.retryDue() == 0 && telegram.calls().size() == 2);
    }

    private static void isolatesAChannelThatThrows() {
        Channel exploding = new Channel() {
            @Override
            public String name() {
                return "broken";
            }

            @Override
            public List<Address> addressesOf(Addressee addressee) {
                return List.of(new Address("broken", "x"));
            }

            @Override
            public DeliveryOutcome deliver(Publication publication, Address address) {
                throw new IllegalStateException("boom");
            }
        };
        RecordingChannel telegram = new RecordingChannel("telegram").reach("ivan", "100");
        Notifier         notifier = new Notifier(new InMemoryNotificationStore(), everyoneIs("ivan"), List.of(exploding, telegram));

        PublicationReport report = notifier.publish(SENDER, new Recipient.Subject("ivan"), Notification.alarm("still"));

        check("a throwing channel is a failure, not an exception", report.failed() == 1);
        check("and the other channel still delivered", report.delivered() == 1);
    }

    private static void refusesTwoChannelsWithOneName() {
        check("two channels with one name are refused", throwsIllegalArgument(() -> new Notifier(
                new InMemoryNotificationStore(), everyoneIs(), List.of(new RecordingChannel("a"), new RecordingChannel("a")))));
    }

    private static void refusesABlankNotification() {
        check("a blank notification is refused", throwsIllegalArgument(() -> Notification.info("  ")));
    }

    private static void forgetsTheOldestBeyondCapacity() {
        InMemoryNotificationStore store    = new InMemoryNotificationStore(2);
        Notifier                  notifier = new Notifier(store, everyoneIs(), List.of());

        String first = notifier.publish(SENDER, new Recipient.Everyone(null), Notification.info("1"), "one").publication().id();
        notifier.publish(SENDER, new Recipient.Everyone(null), Notification.info("2"));
        notifier.publish(SENDER, new Recipient.Everyone(null), Notification.info("3"));

        check("the oldest is forgotten", store.find(first).isEmpty());
        check("and so is its idempotency key", !notifier.publish(
                SENDER, new Recipient.Everyone(null), Notification.info("1"), "one").repeated());
    }

    /** A clock a check can move. */
    private static final class MovableClock extends Clock {

        private Instant now = Instant.parse("2026-10-06T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
