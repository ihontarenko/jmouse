package org.jmouse.telegram.administration;

import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.media.MediaSource;
import org.jmouse.telegram.spi.AdministrationTransport;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * {@link ChatAdministration} the way {@code RoutingGateway} is {@code TelegramGateway}: resolve the
 * purpose's identity, pick the transport for its kind, check the capability, delegate.
 *
 * <p>Every call needs {@link Capability#ADMINISTER_CHAT}, topics {@link Capability#MANAGE_TOPICS}, and
 * {@link #createGroup} {@link Capability#CREATE_CHAT} — so a bot is refused that one with a sentence
 * naming the kind that would have it, before anything reaches Telegram.
 */
public final class RoutingChatAdministration implements ChatAdministration {

    private final IdentitySource                             identities;
    private final Map<IdentityKind, AdministrationTransport> transports;
    private final String                                     purpose;

    public RoutingChatAdministration(IdentitySource identities, List<AdministrationTransport> transports) {
        this(identities, index(transports), IdentitySource.GENERAL);
    }

    private RoutingChatAdministration(
            IdentitySource identities, Map<IdentityKind, AdministrationTransport> transports, String purpose) {
        this.identities = Objects.requireNonNull(identities, "identity source");
        this.transports = transports;
        this.purpose    = purpose;
    }

    private static Map<IdentityKind, AdministrationTransport> index(List<AdministrationTransport> transports) {
        Map<IdentityKind, AdministrationTransport> indexed = new EnumMap<>(IdentityKind.class);

        for (AdministrationTransport transport : Objects.requireNonNull(transports, "transports")) {
            AdministrationTransport previous = indexed.put(transport.kind(), transport);

            if (previous != null) {
                throw new IllegalArgumentException("two administration transports claim " + transport.kind());
            }
        }

        return Collections.unmodifiableMap(indexed);
    }

    @Override
    public ChatDetails details(ChatReference chat) {
        return administer((transport, identity) -> transport.details(identity, chat));
    }

    @Override
    public int memberCount(ChatReference chat) {
        return administer((transport, identity) -> transport.memberCount(identity, chat));
    }

    @Override
    public void rename(ChatReference chat, String title) {
        requireText(title, "A chat title");
        administer((transport, identity) -> {
            transport.rename(identity, chat, title);
            return null;
        });
    }

    @Override
    public void describe(ChatReference chat, String description) {
        administer((transport, identity) -> {
            transport.describe(identity, chat, description);
            return null;
        });
    }

    @Override
    public void changePhoto(ChatReference chat, MediaSource photo) {
        if (!(photo instanceof MediaSource.LocalFile) && !(photo instanceof MediaSource.Bytes)) {
            throw new IllegalArgumentException("A chat photo must be uploaded — a local file or bytes; Telegram takes no URL or file id here");
        }

        administer((transport, identity) -> {
            transport.changePhoto(identity, chat, photo);
            return null;
        });
    }

    @Override
    public void removePhoto(ChatReference chat) {
        administer((transport, identity) -> {
            transport.removePhoto(identity, chat);
            return null;
        });
    }

    @Override
    public void setPermissions(ChatReference chat, Set<ChatPermission> allowed) {
        administer((transport, identity) -> {
            transport.setPermissions(identity, chat, Objects.requireNonNull(allowed, "permissions"));
            return null;
        });
    }

    @Override
    public ChatMember member(ChatReference chat, long userId) {
        return administer((transport, identity) -> transport.member(identity, chat, userId));
    }

    @Override
    public List<ChatMember> administrators(ChatReference chat) {
        return administer((transport, identity) -> transport.administrators(identity, chat));
    }

    @Override
    public void promote(ChatReference chat, long userId, AdministratorRights rights) {
        administer((transport, identity) -> {
            transport.promote(identity, chat, userId, Objects.requireNonNull(rights, "rights"));
            return null;
        });
    }

    @Override
    public void restrict(ChatReference chat, long userId, Set<ChatPermission> allowed, Instant until) {
        administer((transport, identity) -> {
            transport.restrict(identity, chat, userId, Objects.requireNonNull(allowed, "permissions"), until);
            return null;
        });
    }

    @Override
    public void ban(ChatReference chat, long userId, Instant until, boolean revokeMessages) {
        administer((transport, identity) -> {
            transport.ban(identity, chat, userId, until, revokeMessages);
            return null;
        });
    }

    @Override
    public void unban(ChatReference chat, long userId) {
        administer((transport, identity) -> {
            transport.unban(identity, chat, userId);
            return null;
        });
    }

    @Override
    public InviteLink createInviteLink(ChatReference chat, InviteLinkTerms terms) {
        return administer((transport, identity) -> transport.createInviteLink(identity, chat, termsOrOpen(terms)));
    }

    @Override
    public InviteLink editInviteLink(ChatReference chat, String link, InviteLinkTerms terms) {
        requireText(link, "An invite link");
        return administer((transport, identity) -> transport.editInviteLink(identity, chat, link, termsOrOpen(terms)));
    }

    @Override
    public InviteLink revokeInviteLink(ChatReference chat, String link) {
        requireText(link, "An invite link");
        return administer((transport, identity) -> transport.revokeInviteLink(identity, chat, link));
    }

    @Override
    public void approveJoinRequest(ChatReference chat, long userId) {
        administer((transport, identity) -> {
            transport.approveJoinRequest(identity, chat, userId);
            return null;
        });
    }

    @Override
    public void declineJoinRequest(ChatReference chat, long userId) {
        administer((transport, identity) -> {
            transport.declineJoinRequest(identity, chat, userId);
            return null;
        });
    }

    @Override
    public void pin(MessageHandle message, boolean silently) {
        administer((transport, identity) -> {
            transport.pin(identity, message, silently);
            return null;
        });
    }

    @Override
    public void unpin(MessageHandle message) {
        administer((transport, identity) -> {
            transport.unpin(identity, message);
            return null;
        });
    }

    @Override
    public void unpinAll(ChatReference chat) {
        administer((transport, identity) -> {
            transport.unpinAll(identity, chat);
            return null;
        });
    }

    @Override
    public ForumTopic createTopic(ChatReference chat, String name) {
        requireText(name, "A topic name");
        return manageTopics((transport, identity) -> transport.createTopic(identity, chat.inChat(), name));
    }

    @Override
    public void renameTopic(ChatReference topic, String name) {
        requireTopic(topic);
        requireText(name, "A topic name");
        manageTopics((transport, identity) -> {
            transport.renameTopic(identity, topic, name);
            return null;
        });
    }

    @Override
    public void closeTopic(ChatReference topic) {
        requireTopic(topic);
        manageTopics((transport, identity) -> {
            transport.closeTopic(identity, topic);
            return null;
        });
    }

    @Override
    public void reopenTopic(ChatReference topic) {
        requireTopic(topic);
        manageTopics((transport, identity) -> {
            transport.reopenTopic(identity, topic);
            return null;
        });
    }

    @Override
    public void deleteTopic(ChatReference topic) {
        requireTopic(topic);
        manageTopics((transport, identity) -> {
            transport.deleteTopic(identity, topic);
            return null;
        });
    }

    @Override
    public ChatDetails createGroup(String title, List<Long> userIds) {
        requireText(title, "A group title");
        return perform(Capability.CREATE_CHAT,
                       (transport, identity) -> transport.createGroup(identity, title, List.copyOf(userIds)));
    }

    @Override
    public boolean supports(Capability capability) {
        TelegramIdentity        identity  = identities.require(purpose);
        AdministrationTransport transport = transports.get(identity.kind());

        return transport != null && transport.supports(capability);
    }

    @Override
    public ChatAdministration as(String purpose) {
        return new RoutingChatAdministration(identities, transports, Objects.requireNonNull(purpose, "purpose"));
    }

    private <T> T administer(BiFunction<AdministrationTransport, TelegramIdentity, T> call) {
        return perform(Capability.ADMINISTER_CHAT, call);
    }

    private <T> T manageTopics(BiFunction<AdministrationTransport, TelegramIdentity, T> call) {
        return perform(Capability.MANAGE_TOPICS, call);
    }

    private <T> T perform(Capability capability, BiFunction<AdministrationTransport, TelegramIdentity, T> call) {
        TelegramIdentity        identity  = identities.require(purpose);
        AdministrationTransport transport = transports.get(identity.kind());

        if (transport == null || !transport.supports(capability)) {
            throw new TelegramException(new TelegramRefusal.CapabilityUnavailable(
                    capability, identity.name(), identity.kind(), capableKind(capability)));
        }

        return call.apply(transport, identity);
    }

    private IdentityKind capableKind(Capability capability) {
        for (Map.Entry<IdentityKind, AdministrationTransport> entry : transports.entrySet()) {
            if (entry.getValue().supports(capability)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private static InviteLinkTerms termsOrOpen(InviteLinkTerms terms) {
        return terms == null ? InviteLinkTerms.open() : terms;
    }

    private static void requireTopic(ChatReference topic) {
        if (topic == null || !topic.hasThread()) {
            throw new IllegalArgumentException("A forum topic is addressed as a chat in its thread — use ForumTopic.topic()");
        }
    }

    private static void requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(what + " cannot be blank");
        }
    }
}
