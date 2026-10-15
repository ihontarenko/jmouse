package org.jmouse.notifier.telegram;

import org.jmouse.notifier.Address;
import org.jmouse.notifier.Addressee;
import org.jmouse.notifier.Channel;
import org.jmouse.notifier.DeliveryOutcome;
import org.jmouse.notifier.Publication;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.TelegramRefusal;

import java.time.Duration;
import java.util.List;

/**
 * Notifications delivered as Telegram messages.
 *
 * <pre>{@code
 * Channel telegram = new TelegramChannel(gateway, "kitsu-notifications", new BindingsAddressBook(bindings));
 * Notifier notifier = new Notifier(new InMemoryNotificationStore(), resolver, List.of(telegram));
 * }</pre>
 *
 * <h2>⚠️ The purpose decides the bot, and that is the whole migration story</h2>
 *
 * <p>Every send goes through {@code gateway.as(purpose)}. Today every product may name a purpose its
 * identity source answers with one shared bot; giving a product its own bot later is configuration on
 * the identity source, and nothing here changes.
 *
 * <h2>⚠️ What each refusal becomes</h2>
 *
 * <table>
 *   <tr><th>Telegram said</th><th>Outcome</th></tr>
 *   <tr><td>{@code FloodWait}</td><td>retry after <em>exactly</em> its delay — never a guessed backoff</td></tr>
 *   <tr><td>{@code TransportFailure}</td><td>retry after {@link #TRANSPORT_RETRY}</td></tr>
 *   <tr><td>{@code BotBlocked}, {@code ChatNotFound}</td><td>failed, <strong>and the address book is told</strong>
 *       — a binding that keeps answering with a blocked chat fails on every notification forever</td></tr>
 *   <tr><td>anything else</td><td>failed, with the refusal's own sentence</td></tr>
 * </table>
 */
public final class TelegramChannel implements Channel {

    /** The name addresses are filed under when nothing says otherwise. */
    public static final String NAME = "telegram";

    /** How long to wait when nothing answered — Telegram gave no delay of its own to obey. */
    public static final Duration TRANSPORT_RETRY = Duration.ofSeconds(30);

    private final String                name;
    private final TelegramGateway       gateway;
    private final TelegramAddressBook   addressBook;
    private final TelegramMessageFormat format;

    public TelegramChannel(TelegramGateway gateway, String purpose, TelegramAddressBook addressBook) {
        this(NAME, gateway, purpose, addressBook, new TelegramMessageFormat());
    }

    /**
     * @param name    the channel's name — {@link #NAME} unless two Telegram channels are installed, one
     *                per bot
     * @param purpose the product's word for this use, handed to {@link TelegramGateway#as(String)}
     */
    public TelegramChannel(
            String name, TelegramGateway gateway, String purpose,
            TelegramAddressBook addressBook, TelegramMessageFormat format) {
        if (name == null || name.isBlank() || gateway == null || addressBook == null || format == null) {
            throw new IllegalArgumentException("A Telegram channel needs a name, a gateway, an address book and a format");
        }

        this.name        = name;
        this.gateway     = purpose == null || purpose.isBlank() ? gateway : gateway.as(purpose);
        this.addressBook = addressBook;
        this.format      = format;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Address> addressesOf(Addressee addressee) {
        return addressBook.chatsOf(addressee).stream()
                          .map(chat -> TelegramAddresses.of(name, chat))
                          .toList();
    }

    @Override
    public DeliveryOutcome deliver(Publication publication, Address address) {
        ChatReference chat;

        try {
            chat = TelegramAddresses.chatOf(address);
        } catch (IllegalArgumentException exception) {
            return new DeliveryOutcome.Failed(exception.getMessage());
        }

        try {
            SentMessage sent = gateway.send(chat, format.draftOf(publication));
            return new DeliveryOutcome.Delivered(receiptOf(sent));
        } catch (TelegramException exception) {
            return outcomeOf(exception.refusal(), chat);
        }
    }

    private DeliveryOutcome outcomeOf(TelegramRefusal refusal, ChatReference chat) {
        return switch (refusal) {
            case TelegramRefusal.FloodWait wait -> new DeliveryOutcome.RetryAfter(wait.retryAfter(), refusal.message());
            case TelegramRefusal.TransportFailure failure -> new DeliveryOutcome.RetryAfter(TRANSPORT_RETRY, refusal.message());
            case TelegramRefusal.BotBlocked blocked -> unreachable(chat, refusal);
            case TelegramRefusal.ChatNotFound missing -> unreachable(chat, refusal);
            // ⚠️ Not unreachable: bound through a product's bot, never started this one. Keep the binding.
            case TelegramRefusal.ConversationNotStarted notStarted -> new DeliveryOutcome.Failed(refusal.message());
            default -> new DeliveryOutcome.Failed(refusal.message());
        };
    }

    private DeliveryOutcome unreachable(ChatReference chat, TelegramRefusal refusal) {
        addressBook.unreachable(chat);
        return new DeliveryOutcome.Failed(refusal.message());
    }

    /**
     * {@code <chat>:<message id>} — what an edit or a delete needs to find the message again.
     */
    private static String receiptOf(SentMessage sent) {
        return sent.chat().wireValue() + ":" + sent.messageId();
    }
}
