package org.jmouse.notifier;

import java.util.List;

/**
 * One medium a notification can travel by — Telegram, a screen, email.
 *
 * <p>⚠️ <strong>A channel keeps no delivery state.</strong> Attempts, retries, "did this already go"
 * and the receipt all belong to the {@link Notifier} and its {@link NotificationStore}, shared by every
 * channel. A channel does two things: finds its addresses for a person, and tries one delivery and says
 * what happened.
 */
public interface Channel {

    /** The name addresses are filed under — {@code "telegram"}. Stable, because it is stored. */
    String name();

    /**
     * Where this channel reaches a person.
     *
     * @return the addresses, possibly several (two bound chats) or none (never bound); never {@code null}
     */
    List<Address> addressesOf(Addressee addressee);

    /**
     * One attempt at one address.
     *
     * <p>⚠️ Report a failure as an outcome rather than throwing. {@link Notifier} isolates a channel that
     * throws anyway — it records a permanent failure and carries on with the others — but an exception
     * loses the difference between "try again in a minute" and "never again".
     */
    DeliveryOutcome deliver(Publication publication, Address address);
}
