package org.jmouse.notifier.web;

import org.jmouse.notifier.Address;
import org.jmouse.notifier.Addressee;
import org.jmouse.notifier.Channel;
import org.jmouse.notifier.DeliveryOutcome;
import org.jmouse.notifier.Notification;
import org.jmouse.notifier.Publication;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Notifications shown on the product's own screens — a browser, a television.
 *
 * <pre>{@code
 * Inbox inbox = new MemoryInbox();
 * Notifier notifier = new Notifier(store, resolver, List.of(new ScreenChannel(inbox), telegram));
 *
 * // the product's own route, behind its own authentication:
 * List<Notice> fresh = inbox.collect(caller.subject(), caller.deviceId());
 * }</pre>
 *
 * <p>Delivering is depositing into the {@link Inbox}; collecting is the client asking. ⚠️ The library never
 * decides who the caller is — the product's route passes its own authenticated subject.
 *
 * <h2>Addresses</h2>
 *
 * <p>{@code <subject>} reaches every device of the subject; {@code <subject>|<device>} only that one —
 * what an addressee resolved through a device produces, and how {@code Except} leaves the screen where
 * something happened out. Both parts are URL-encoded.
 */
public final class ScreenChannel implements Channel {

    public static final String NAME = "screen";

    private static final char DEVICE_SEPARATOR = '|';

    private final Inbox               inbox;
    private final Predicate<Addressee> reaches;

    public ScreenChannel(Inbox inbox) {
        this(inbox, addressee -> true);
    }

    /**
     * @param reaches which addressees have a screen at all — a product whose resolver answers with people
     *                for Telegram <em>and</em> devices for screens gives each channel only its own, rather
     *                than depositing notices for people no screen will ever collect
     */
    public ScreenChannel(Inbox inbox, Predicate<Addressee> reaches) {
        if (inbox == null || reaches == null) {
            throw new IllegalArgumentException("A screen channel needs an inbox and a rule for whom it reaches");
        }

        this.inbox   = inbox;
        this.reaches = reaches;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<Address> addressesOf(Addressee addressee) {
        if (!reaches.test(addressee)) {
            return List.of();
        }

        String value = addressee.deviceId() == null
                ? encode(addressee.subject())
                : encode(addressee.subject()) + DEVICE_SEPARATOR + encode(addressee.deviceId());

        return List.of(new Address(NAME, value));
    }

    @Override
    public DeliveryOutcome deliver(Publication publication, Address address) {
        String value     = address.value();
        int    separator = value.indexOf(DEVICE_SEPARATOR);
        String subject   = decode(separator < 0 ? value : value.substring(0, separator));
        String device    = separator < 0 ? null : decode(value.substring(separator + 1));

        Notification notification = publication.notification();
        String       noticeId     = UUID.randomUUID().toString();

        inbox.deposit(new Notice(noticeId, subject, device, publication.publishedAt(), notification.level(),
                                 notification.title(), notification.body(), publication.sender().label(),
                                 notification.links()));

        return new DeliveryOutcome.Delivered(noticeId);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
