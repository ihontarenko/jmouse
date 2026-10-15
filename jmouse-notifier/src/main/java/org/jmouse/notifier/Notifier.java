package org.jmouse.notifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Publishes a notification: keeps it, resolves who it is for, and delivers it on every channel.
 *
 * <pre>{@code
 * Notifier notifier = new Notifier(new InMemoryNotificationStore(), resolver, List.of(telegramChannel));
 *
 * notifier.publish(new Sender.Rule("downloads"),
 *                  new Recipient.Subject(accountId),
 *                  Notification.info("Blade Runner finished downloading"));
 * }</pre>
 *
 * <h2>⚠️ A failing channel never takes the publish down</h2>
 *
 * <p>Publishing usually happens inside somebody's save. A channel that refuses, or throws, is recorded
 * against its own delivery and the others carry on; nothing propagates to the caller except invalid
 * arguments and the product's own resolver failing, which is a defect worth seeing.
 *
 * <h2>⚠️ Retries need somebody to call {@link #retryDue()}</h2>
 *
 * <p>A delivery told to wait is kept {@link Delivery.State#PENDING} until its time comes, and nothing in
 * this class owns a thread. The product schedules {@code retryDue()} — every few seconds is plenty.
 */
public final class Notifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(Notifier.class);

    /** How many attempts a delivery gets when nothing says otherwise. */
    public static final int DEFAULT_MAXIMUM_ATTEMPTS = 5;

    /** How many due deliveries one {@link #retryDue()} call takes on. */
    private static final int RETRY_BATCH = 100;

    private final NotificationStore    store;
    private final RecipientResolver    resolver;
    private final Map<String, Channel> channels;
    private final Clock                clock;
    private final int                  maximumAttempts;

    public Notifier(NotificationStore store, RecipientResolver resolver, List<Channel> channels) {
        this(store, resolver, channels, Clock.systemUTC(), DEFAULT_MAXIMUM_ATTEMPTS);
    }

    public Notifier(
            NotificationStore store, RecipientResolver resolver, List<Channel> channels,
            Clock clock, int maximumAttempts) {
        if (store == null || resolver == null || channels == null || clock == null) {
            throw new IllegalArgumentException("A notifier needs a store, a resolver, its channels and a clock");
        }

        if (maximumAttempts < 1) {
            throw new IllegalArgumentException("A delivery needs at least one attempt");
        }

        this.store           = store;
        this.resolver        = resolver;
        this.channels        = indexByName(channels);
        this.clock           = clock;
        this.maximumAttempts = maximumAttempts;
    }

    /** As {@link #publish(Sender, Recipient, Notification, String)} with no idempotency key. */
    public PublicationReport publish(Sender sender, Recipient recipient, Notification notification) {
        return publish(sender, recipient, notification, null);
    }

    /**
     * Keeps the notification and makes the first delivery attempt on every address it resolves to.
     *
     * @param idempotencyKey the publisher's own key, or {@code null}. ⚠️ A key seen before delivers
     *                       nothing and reports the earlier publication, marked {@code repeated}
     */
    public PublicationReport publish(
            Sender sender, Recipient recipient, Notification notification, String idempotencyKey) {
        Instant     now         = clock.instant();
        Publication publication = new Publication(
                UUID.randomUUID().toString(), idempotencyKey, sender, recipient, notification, now);
        Publication kept        = store.register(publication);

        if (!kept.id().equals(publication.id())) {
            return new PublicationReport(kept, store.deliveriesOf(kept.id()), true);
        }

        for (Address address : addressesFor(recipient)) {
            Channel channel = channels.get(address.channel());

            if (channel == null) {
                store.save(Delivery.failedUnattempted(
                        publication.id(), address, "no channel named '%s' is installed".formatted(address.channel()), now));
                continue;
            }

            // ⚠️ Recorded as pending BEFORE the attempt: with a durable store, a process that dies here
            // leaves a row that retryDue() finds, instead of an address nobody remembers was owed.
            Delivery pending = Delivery.due(publication.id(), address, now);
            store.save(pending);
            attempt(channel, publication, pending);
        }

        PublicationReport report = new PublicationReport(publication, store.deliveriesOf(publication.id()), false);

        if (report.reachedNobody()) {
            LOGGER.info("Notification {} from '{}' to {} reached nobody: {} deliveries, none delivered or pending",
                        publication.id(), sender.label(), recipient, report.deliveries().size());
        }

        return report;
    }

    /**
     * Attempts every delivery whose time has come.
     *
     * <p>⚠️ Synchronized so two schedulers in one process cannot attempt the same delivery twice; two
     * processes over one shared store need that store to claim rows, which memory cannot.
     *
     * @return how many deliveries were attempted
     */
    public synchronized int retryDue() {
        List<Delivery> due       = store.due(clock.instant(), RETRY_BATCH);
        int            attempted = 0;

        for (Delivery delivery : due) {
            Channel channel = channels.get(delivery.address().channel());

            if (channel == null) {
                store.save(delivery.after(new DeliveryOutcome.Failed(
                        "the channel '%s' is no longer installed".formatted(delivery.address().channel())),
                        clock.instant(), maximumAttempts));
                continue;
            }

            Publication publication = store.find(delivery.publicationId()).orElse(null);

            if (publication == null) {
                continue;
            }

            attempt(channel, publication, delivery);
            attempted++;
        }

        return attempted;
    }

    /** The channels installed, by name. */
    public Set<String> channelNames() {
        return channels.keySet();
    }

    private Set<Address> addressesFor(Recipient recipient) {
        Set<Address> addresses = new LinkedHashSet<>();

        if (recipient instanceof Recipient.Direct direct) {
            addresses.add(direct.address());
            return addresses;
        }

        String excludedDevice = recipient instanceof Recipient.Except except ? except.deviceId() : null;

        for (Addressee addressee : resolver.resolve(recipient)) {
            if (excludedDevice != null && excludedDevice.equals(addressee.deviceId())) {
                continue;
            }

            for (Channel channel : channels.values()) {
                // ⚠️ A set, so a person resolved through two devices still gets one Telegram message.
                addresses.addAll(channel.addressesOf(addressee));
            }
        }

        return addresses;
    }

    private void attempt(Channel channel, Publication publication, Delivery delivery) {
        DeliveryOutcome outcome;

        try {
            outcome = channel.deliver(publication, delivery.address());
        } catch (RuntimeException exception) {
            LOGGER.warn("Channel '{}' threw delivering notification {} to {}",
                        channel.name(), publication.id(), delivery.address(), exception);
            outcome = new DeliveryOutcome.Failed("the channel failed unexpectedly: " + exception.getMessage());
        }

        if (outcome == null) {
            outcome = new DeliveryOutcome.Failed("the channel reported nothing");
        }

        store.save(delivery.after(outcome, clock.instant(), maximumAttempts));
    }

    private static Map<String, Channel> indexByName(List<Channel> channels) {
        Map<String, Channel> byName = new LinkedHashMap<>();

        for (Channel channel : new ArrayList<>(channels)) {
            Channel previous = byName.putIfAbsent(channel.name(), channel);

            if (previous != null) {
                throw new IllegalArgumentException(
                        "Two channels are named '%s'; addresses are filed by name, so one would never be used"
                                .formatted(channel.name()));
            }
        }

        return Collections.unmodifiableMap(byName);
    }
}
