package org.jmouse.telegram;

import org.jmouse.telegram.spi.TelegramTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The standard {@link TelegramGateway}: resolve an identity, pick the transport that speaks its
 * protocol, check the capabilities, delegate.
 *
 * <p>Four responsibilities, all of them the ones a transport should not have. `RoutingChatModel` in
 * {@code jmouse-ai-provider} is the same arrangement and the precedent it follows.
 *
 * <h2>⚠️ The capability check happens here, before the call</h2>
 *
 * <p>This is the whole reason {@link Capability} exists. A draft carrying buttons handed to a user
 * account has to fail with a sentence naming the identity, the capability and the kind that would have
 * worked — not with an {@code UnsupportedOperationException} from three frames down, and emphatically
 * not by sending the message with the buttons quietly dropped, which looks to the recipient like a
 * product defect and to the sender like a success.
 *
 * <h2>⚠️ A kind with no transport installed is a refusal, not a crash</h2>
 *
 * <p>An installation may perfectly reasonably have a {@link IdentityKind#USER} identity configured and
 * no MTProto transport on the classpath — that is the state this library ships in today. The answer is
 * a refusal saying exactly that, because it tells an administrator what to do. A
 * {@code NullPointerException} tells them nothing.
 */
public final class RoutingGateway implements TelegramGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingGateway.class);

    private final IdentitySource                     identities;
    private final Map<IdentityKind, TelegramTransport> transports;
    private final String                             purpose;

    public RoutingGateway(IdentitySource identities, List<TelegramTransport> transports) {
        this(identities, index(transports), IdentitySource.GENERAL);
    }

    private RoutingGateway(
            IdentitySource identities,
            Map<IdentityKind, TelegramTransport> transports,
            String purpose) {

        this.identities = Objects.requireNonNull(identities, "identity source");
        this.transports = transports;
        this.purpose    = purpose;
    }

    private static Map<IdentityKind, TelegramTransport> index(List<TelegramTransport> transports) {
        Objects.requireNonNull(transports, "transports");

        Map<IdentityKind, TelegramTransport> indexed = new EnumMap<>(IdentityKind.class);

        for (TelegramTransport transport : transports) {
            TelegramTransport previous = indexed.put(transport.kind(), transport);

            // ⚠️ Two transports for one protocol is a wiring mistake that would otherwise resolve
            // silently to whichever was last in the list — a genuinely unpleasant thing to debug,
            // because both of them work.
            if (previous != null) {
                throw new IllegalArgumentException(
                        "two transports claim %s: %s and %s".formatted(
                                transport.kind(),
                                previous.getClass().getName(),
                                transport.getClass().getName()));
            }
        }

        return Collections.unmodifiableMap(indexed);
    }

    @Override
    public SentMessage send(ChatReference chat, MessageDraft draft) {
        TelegramIdentity  identity  = resolve();
        TelegramTransport transport = transportFor(identity);

        requireCapabilities(identity, transport, draft.requiredCapabilities());

        LOGGER.debug("send: identity={}, chat={}, attachments={}",
                identity.name(), chat, draft.attachments().size());

        return transport.send(identity, chat, draft);
    }

    @Override
    public SentMessage edit(MessageHandle message, MessageDraft draft) {
        TelegramIdentity  identity  = resolve();
        TelegramTransport transport = transportFor(identity);

        requireCapability(identity, transport, Capability.EDIT_MESSAGE);
        requireCapabilities(identity, transport, draft.requiredCapabilities());

        return transport.edit(identity, message, draft);
    }

    @Override
    public void delete(MessageHandle message) {
        TelegramIdentity  identity  = resolve();
        TelegramTransport transport = transportFor(identity);

        requireCapability(identity, transport, Capability.DELETE_MESSAGE);

        transport.delete(identity, message);
    }

    @Override
    public void answerCallback(CallbackAnswer answer) {
        TelegramIdentity  identity  = resolve();
        TelegramTransport transport = transportFor(identity);

        requireCapability(identity, transport, Capability.ANSWER_CALLBACK);

        transport.answerCallback(identity, answer);
    }

    @Override
    public boolean supports(Capability capability) {
        TelegramIdentity  identity  = resolve();
        TelegramTransport transport = transports.get(identity.kind());

        return transport != null && transport.supports(capability);
    }

    @Override
    public TelegramGateway as(String purpose) {
        Objects.requireNonNull(purpose, "purpose");

        return new RoutingGateway(identities, transports, purpose);
    }

    private TelegramIdentity resolve() {
        TelegramIdentity identity = identities.identity(purpose);

        if (identity == null) {
            throw new TelegramException(new TelegramRefusal.Unauthorized(
                    purpose, "no identity is configured for this purpose"));
        }

        if (!identity.hasCredential()) {
            throw new TelegramException(new TelegramRefusal.Unauthorized(
                    identity.name(), "the identity has no credential"));
        }

        return identity;
    }

    private TelegramTransport transportFor(TelegramIdentity identity) {
        TelegramTransport transport = transports.get(identity.kind());

        if (transport == null) {
            throw new TelegramException(new TelegramRefusal.CapabilityUnavailable(
                    Capability.SEND_MESSAGE, identity.name(), identity.kind(), null));
        }

        return transport;
    }

    private void requireCapabilities(
            TelegramIdentity identity, TelegramTransport transport, List<Capability> required) {

        for (Capability capability : required) {
            requireCapability(identity, transport, capability);
        }
    }

    private void requireCapability(
            TelegramIdentity identity, TelegramTransport transport, Capability capability) {

        if (transport.supports(capability)) {
            return;
        }

        throw new TelegramException(new TelegramRefusal.CapabilityUnavailable(
                capability, identity.name(), identity.kind(), capableKind(capability)));
    }

    /**
     * Which kind, if any, would have had this capability — so the refusal can say what to use instead
     * of only what failed.
     *
     * <p>⚠️ Asked of the transports that are actually installed rather than from a hard-coded table.
     * A table would keep claiming MTProto can create a channel in an installation that has no MTProto
     * transport, which is advice nobody can follow.
     */
    private IdentityKind capableKind(Capability capability) {
        for (Map.Entry<IdentityKind, TelegramTransport> entry : transports.entrySet()) {
            if (entry.getValue().supports(capability)) {
                return entry.getKey();
            }
        }

        return null;
    }
}
