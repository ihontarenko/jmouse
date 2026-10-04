package org.jmouse.telegram.test;

import org.jmouse.telegram.CallbackAnswer;
import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.MessageDraft;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.SentMessage;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.spi.TelegramTransport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A transport that remembers what it was asked to do and answers plausibly.
 *
 * <h2>Why this ships in the library rather than in each product</h2>
 *
 * <p>Every consumer otherwise writes its own stub, and each one gets the interesting cases wrong in a
 * different way. It is also the only place those cases are reachable at all: a
 * {@link TelegramRefusal.FloodWait}, a {@link TelegramRefusal.BotBlocked}, a
 * {@link TelegramRefusal.MessageNotModified} on a second identical edit. Against real Telegram, the
 * first two are difficult to provoke on purpose and the third requires a live message.
 *
 * <p>⚠️ In {@code src/main/java} rather than a test jar, deliberately. This repository has no JUnit
 * tests — integration checks are {@code smoke} classes with {@code main} methods — and a consuming
 * product needs this on its own compile path to develop against, which a {@code test}-scoped artefact
 * would not give it.
 *
 * <h2>It behaves, rather than merely returning</h2>
 *
 * <p>Message ids increment, so a caller that stores a {@link SentMessage} and edits it later works end
 * to end. An edit against an unknown handle refuses as Telegram would. An edit whose text matches what
 * is already there raises {@link TelegramRefusal.MessageNotModified} — the case a scheduled updater
 * hits on every run, and the one most likely to be mishandled.
 */
public final class RecordingTransport implements TelegramTransport {

    private final List<Call>            calls     = new ArrayList<>();
    private final Map<Integer, String>  bodies    = new ConcurrentHashMap<>();
    private final AtomicInteger         nextId    = new AtomicInteger(1000);
    private final Set<Capability>       supported;
    private final IdentityKind          kind;

    /** Scripted refusals, consumed one per call, so a sequence can be rehearsed. */
    private final List<TelegramRefusal> scripted = new ArrayList<>();

    public RecordingTransport() {
        this(IdentityKind.BOT, EnumSet.of(
                Capability.SEND_MESSAGE,
                Capability.EDIT_MESSAGE,
                Capability.DELETE_MESSAGE,
                Capability.SEND_MEDIA,
                Capability.REPLY_MARKUP,
                Capability.ANSWER_CALLBACK,
                Capability.ADMINISTER_CHAT,
                Capability.MANAGE_TOPICS));
    }

    /**
     * A transport pretending to be something else — which is how a consumer checks that its own code
     * handles a {@link TelegramRefusal.CapabilityUnavailable} rather than only the happy path.
     */
    public RecordingTransport(IdentityKind kind, Set<Capability> supported) {
        this.kind      = kind;
        this.supported = EnumSet.copyOf(supported);
    }

    /** One thing this transport was asked to do. */
    public record Call(String method, ChatReference chat, MessageDraft draft, Integer messageId) {
    }

    /**
     * Makes the next call fail with {@code refusal}.
     *
     * <p>Queued rather than set, so a test can rehearse "refused, refused, then fine" — which is what
     * a retry policy has to survive and the one thing a single-shot failure flag cannot express.
     */
    public RecordingTransport willRefuse(TelegramRefusal refusal) {
        scripted.add(refusal);
        return this;
    }

    /** Everything asked of it, in order. */
    public List<Call> calls() {
        return List.copyOf(calls);
    }

    /** The most recent call, for the common assertion. */
    public Call lastCall() {
        if (calls.isEmpty()) {
            throw new IllegalStateException("nothing has been sent through this transport");
        }

        return calls.getLast();
    }

    public void reset() {
        calls.clear();
        bodies.clear();
        scripted.clear();
    }

    @Override
    public IdentityKind kind() {
        return kind;
    }

    @Override
    public boolean supports(Capability capability) {
        return supported.contains(capability);
    }

    @Override
    public SentMessage send(TelegramIdentity identity, ChatReference chat, MessageDraft draft) {
        refuseIfScripted();

        int messageId = nextId.incrementAndGet();

        calls.add(new Call("send", chat, draft, messageId));

        // ⚠️ ConcurrentHashMap refuses a null VALUE, and a media-only draft has no text. An empty
        // string stands in — it also keeps the not-modified comparison below honest for such a draft.
        bodies.put(messageId, bodyOf(draft));

        return new SentMessage(chat.inChat(), messageId, Instant.now(), fakeMediaIds(draft));
    }

    @Override
    public SentMessage edit(TelegramIdentity identity, MessageHandle message, MessageDraft draft) {
        refuseIfScripted();

        if (!bodies.containsKey(message.messageId())) {
            throw new TelegramException(new TelegramRefusal.Rejected(
                    400, "message to edit not found"));
        }

        // ⚠️ The case a periodic updater hits every run. Reproduced here because it is essentially
        // unreachable on purpose against real Telegram, and because treating it as an error is how such
        // a job logs an exception a minute forever.
        if (Objects.equals(bodies.get(message.messageId()), bodyOf(draft))) {
            throw new TelegramException(new TelegramRefusal.MessageNotModified());
        }

        bodies.put(message.messageId(), bodyOf(draft));
        calls.add(new Call("edit", message.chat(), draft, message.messageId()));

        return new SentMessage(message.chat(), message.messageId(), Instant.now(), List.of());
    }

    @Override
    public void delete(TelegramIdentity identity, MessageHandle message) {
        refuseIfScripted();

        bodies.remove(message.messageId());
        calls.add(new Call("delete", message.chat(), null, message.messageId()));
    }

    @Override
    public void answerCallback(TelegramIdentity identity, CallbackAnswer answer) {
        refuseIfScripted();

        calls.add(new Call("answerCallback", null, null, null));
    }

    private String bodyOf(MessageDraft draft) {
        return draft.text() == null ? "" : draft.text();
    }

    private void refuseIfScripted() {
        if (!scripted.isEmpty()) {
            throw new TelegramException(scripted.removeFirst());
        }
    }

    /** Plausible {@code file_id}s, so a caller that re-sends by id has something to re-send. */
    private List<String> fakeMediaIds(MessageDraft draft) {
        List<String> ids = new ArrayList<>(draft.attachments().size());

        for (int index = 0; index < draft.attachments().size(); index++) {
            ids.add("fake-file-id-" + nextId.incrementAndGet());
        }

        return ids;
    }
}
