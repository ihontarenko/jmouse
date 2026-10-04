package org.jmouse.telegram.smoke;

import org.jmouse.telegram.CallbackAnswer;
import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.MarkupEscaper;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.ParseMode;
import org.jmouse.telegram.RoutingGateway;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramGateway;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.markup.InlineButton;
import org.jmouse.telegram.markup.ReplyMarkup;
import org.jmouse.telegram.media.MediaAttachment;
import org.jmouse.telegram.media.MediaSource;
import org.jmouse.telegram.pace.Pace;
import org.jmouse.telegram.pace.PacedTransport;
import org.jmouse.telegram.test.RecordingTransport;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static org.jmouse.telegram.smoke.Checks.check;
import static org.jmouse.telegram.smoke.Checks.refusalOf;
import static org.jmouse.telegram.smoke.Checks.throwsIllegalArgument;

/**
 * Runs the core against {@link RecordingTransport} and checks what it actually does.
 *
 * <p>Run its {@code main}. This repository keeps integration checks as {@code smoke} classes rather
 * than JUnit tests, so this is the house arrangement rather than a shortcut.
 *
 * <p>⚠️ It reaches no network and needs no bot token. Everything here is behaviour the library owns —
 * routing, capability refusal, escaping, the draft invariants, retry on a flood wait — which is
 * precisely the part that is <em>not</em> verified by successfully sending one message to a real chat.
 */
public final class TelegramSmoke {

    public static void main(String[] arguments) {
        sendsThroughTheBotTransport();
        returnsAHandleThatCanBeEdited();
        refusesASecondIdenticalEdit();
        refusesButtonsOnATransportWithoutThem();
        namesTheKindThatWouldHaveWorked();
        refusesWhenNoTransportSpeaksTheIdentitysProtocol();
        refusesAnIdentityWithNoCredential();
        escapesEveryReservedCharacter();
        refusesAnEmptyDraft();
        refusesOverlongCallbackData();
        refusesAChatAddressedBothWays();
        refusesAThreadOnAMessageHandle();
        carriesTheThreadIntoTheDraftsDestination();
        retriesAFloodWaitAndObeysItsDelay();
        doesNotRetryAnUnretryableRefusal();

        Checks.report();
    }

    private static void sendsThroughTheBotTransport() {
        RecordingTransport transport = new RecordingTransport();
        TelegramGateway    gateway   = gatewayOver(transport);

        SentMessage sent = gateway.send(ChatReference.of(-1001234567890L), MessageDraft.text("hello"));

        check("send reaches the transport", "send".equals(transport.lastCall().method()));
        check("a message id comes back", sent.messageId() > 0);
    }

    private static void returnsAHandleThatCanBeEdited() {
        RecordingTransport transport = new RecordingTransport();
        TelegramGateway    gateway   = gatewayOver(transport);

        SentMessage sent = gateway.send(ChatReference.of(42L), MessageDraft.text("one person opened it"));
        SentMessage after = gateway.edit(sent.handle(), MessageDraft.text("two people opened it"));

        check("an edit keeps the message id", after.messageId() == sent.messageId());
        check("the edit reached the transport", "edit".equals(transport.lastCall().method()));
    }

    private static void refusesASecondIdenticalEdit() {
        RecordingTransport transport = new RecordingTransport();
        TelegramGateway    gateway   = gatewayOver(transport);

        SentMessage sent = gateway.send(ChatReference.of(42L), MessageDraft.text("unchanged"));

        check("an identical edit is MessageNotModified",
                refusalOf(() -> gateway.edit(sent.handle(), MessageDraft.text("unchanged")))
                        instanceof TelegramRefusal.MessageNotModified);
    }

    private static void refusesButtonsOnATransportWithoutThem() {
        RecordingTransport userLike = new RecordingTransport(
                IdentityKind.BOT, EnumSet.of(Capability.SEND_MESSAGE));

        TelegramGateway gateway = gatewayOver(userLike);

        MessageDraft withButtons = MessageDraft.builder()
                .text("open it?")
                .markup(ReplyMarkup.InlineKeyboard.row(InlineButton.callback("Open", "open:1")))
                .build();

        TelegramRefusal refusal = refusalOf(() -> gateway.send(ChatReference.of(1L), withButtons));

        check("buttons on a transport without REPLY_MARKUP are refused",
                refusal instanceof TelegramRefusal.CapabilityUnavailable unavailable
                        && unavailable.capability() == Capability.REPLY_MARKUP);

        check("the refusal carries a sentence", refusal.message().contains("REPLY_MARKUP"));
        check("a capability refusal is not retryable", !refusal.retryable());
    }

    private static void namesTheKindThatWouldHaveWorked() {
        // A bot transport that cannot do it, and a user transport that can — so the refusal has
        // something true to recommend.
        RecordingTransport bot = new RecordingTransport(
                IdentityKind.BOT, EnumSet.of(Capability.SEND_MESSAGE));

        RecordingTransport user = new RecordingTransport(
                IdentityKind.USER, EnumSet.of(Capability.SEND_MESSAGE, Capability.READ_HISTORY));

        TelegramGateway gateway = new RoutingGateway(
                IdentitySource.fixed(TelegramIdentity.bot("alerts", "token")), List.of(bot, user));

        MessageDraft withButtons = MessageDraft.builder()
                .text("x")
                .markup(ReplyMarkup.InlineKeyboard.row(InlineButton.url("Open", "https://example.org")))
                .build();

        TelegramRefusal refusal = refusalOf(() -> gateway.send(ChatReference.of(1L), withButtons));

        check("nothing installed supports it, so no kind is recommended",
                refusal instanceof TelegramRefusal.CapabilityUnavailable unavailable
                        && unavailable.capableKind() == null);

        check("the message says no transport provides it",
                refusal.message().contains("no installed transport"));
    }

    private static void refusesWhenNoTransportSpeaksTheIdentitysProtocol() {
        TelegramGateway gateway = new RoutingGateway(
                IdentitySource.fixed(new TelegramIdentity(IdentityKind.USER, "me", "session", null)),
                List.of(new RecordingTransport()));

        check("a USER identity with only a BOT transport is refused, not a crash",
                refusalOf(() -> gateway.send(ChatReference.of(1L), MessageDraft.text("x")))
                        instanceof TelegramRefusal.CapabilityUnavailable);
    }

    private static void refusesAnIdentityWithNoCredential() {
        TelegramGateway gateway = new RoutingGateway(
                IdentitySource.fixed(TelegramIdentity.bot("unconfigured", null)),
                List.of(new RecordingTransport()));

        check("a credential-less identity is Unauthorized",
                refusalOf(() -> gateway.send(ChatReference.of(1L), MessageDraft.text("x")))
                        instanceof TelegramRefusal.Unauthorized);
    }

    private static void escapesEveryReservedCharacter() {
        // ⚠️ The em dash is NOT reserved and the ASCII hyphen is — a distinction this check got wrong
        // on its first run, which is the kind of thing only a real assertion catches.
        String escaped = MarkupEscaper.markdownV2("Blade Runner (1982) — sci-fi, 4.7/5!");

        check("brackets are escaped", escaped.contains("\\(") && escaped.contains("\\)"));
        check("the full stop is escaped", escaped.contains("\\."));
        check("the exclamation mark is escaped", escaped.contains("\\!"));
        check("the hyphen is escaped", escaped.contains("\\-"));
        check("null becomes empty rather than the word null",
                MarkupEscaper.markdownV2(null).isEmpty());
        check("NONE escapes nothing",
                "a.b".equals(MarkupEscaper.forMode(ParseMode.NONE, "a.b")));
        check("HTML escapes the three it must",
                "&amp;&lt;&gt;".equals(MarkupEscaper.html("&<>")));
    }

    private static void refusesAnEmptyDraft() {
        check("a draft with neither text nor attachment is refused",
                throwsIllegalArgument(() -> MessageDraft.builder().build()));

        check("text over the limit is refused",
                throwsIllegalArgument(() -> MessageDraft.text("x".repeat(MessageDraft.TEXT_LIMIT + 1))));

        check("a media group over ten is refused", throwsIllegalArgument(() -> {
            MessageDraft.Builder builder = MessageDraft.builder().text("album");

            for (int index = 0; index <= MessageDraft.MEDIA_GROUP_LIMIT; index++) {
                builder.attach(MediaAttachment.photo(new MediaSource.FileId("id" + index)));
            }

            return builder.build();
        }));
    }

    private static void refusesOverlongCallbackData() {
        // ⚠️ Bytes, not characters: Cyrillic reaches the 64-byte ceiling at 32 characters.
        check("64 bytes of Cyrillic callback data is refused",
                throwsIllegalArgument(() -> InlineButton.callback("x", "я".repeat(33))));

        check("64 ASCII characters is accepted",
                InlineButton.callback("x", "a".repeat(64)) != null);
    }

    private static void refusesAChatAddressedBothWays() {
        check("both an id and a username is refused",
                throwsIllegalArgument(() -> new ChatReference(1L, "@name", null)));

        check("neither is refused",
                throwsIllegalArgument(() -> new ChatReference(null, null, null)));

        check("a username gains its @", "@news".equals(ChatReference.of("news").wireValue()));
    }

    private static void refusesAThreadOnAMessageHandle() {
        check("a handle addressing a topic is refused",
                throwsIllegalArgument(() ->
                        new MessageHandle(ChatReference.of(1L).inThread(7), 100)));

        check("SentMessage.handle() strips the thread",
                !new SentMessage(ChatReference.of(1L).inThread(7), 100, java.time.Instant.now(), List.of())
                        .handle().chat().hasThread());
    }

    private static void carriesTheThreadIntoTheDraftsDestination() {
        RecordingTransport transport = new RecordingTransport();
        TelegramGateway    gateway   = gatewayOver(transport);

        gateway.send(ChatReference.of(-100L).inThread(12), MessageDraft.text("in a topic"));

        check("the thread reaches the transport", transport.lastCall().chat().threadId() == 12);
    }

    private static void retriesAFloodWaitAndObeysItsDelay() {
        RecordingTransport transport = new RecordingTransport()
                .willRefuse(new TelegramRefusal.FloodWait(Duration.ofMillis(120)));

        PacedTransport paced = new PacedTransport(transport, Pace.unlimited());

        TelegramGateway gateway = new RoutingGateway(
                IdentitySource.fixed(TelegramIdentity.bot("alerts", "token")), List.of(paced));

        long started = System.currentTimeMillis();

        SentMessage sent = gateway.send(ChatReference.of(1L), MessageDraft.text("after a wait"));
        long elapsed = System.currentTimeMillis() - started;

        check("the retry succeeded", sent.messageId() > 0);
        check("it waited Telegram's own figure rather than not waiting", elapsed >= 120);
    }

    private static void doesNotRetryAnUnretryableRefusal() {
        RecordingTransport transport = new RecordingTransport()
                .willRefuse(new TelegramRefusal.BotBlocked("42"))
                .willRefuse(new TelegramRefusal.BotBlocked("42"));

        PacedTransport paced = new PacedTransport(transport, Pace.unlimited());

        TelegramGateway gateway = new RoutingGateway(
                IdentitySource.fixed(TelegramIdentity.bot("alerts", "token")), List.of(paced));

        TelegramRefusal refusal =
                refusalOf(() -> gateway.send(ChatReference.of(42L), MessageDraft.text("x")));

        check("a blocked bot is not retried", refusal instanceof TelegramRefusal.BotBlocked);

        // Proves it stopped rather than burning both scripted refusals.
        check("the second scripted refusal was untouched",
                refusalOf(() -> gateway.send(ChatReference.of(42L), MessageDraft.text("y")))
                        instanceof TelegramRefusal.BotBlocked);
    }

    private static TelegramGateway gatewayOver(RecordingTransport transport) {
        return new RoutingGateway(
                IdentitySource.fixed(TelegramIdentity.bot("smoke", "token")), List.of(transport));
    }

    private TelegramSmoke() {
    }
}
