package org.jmouse.telegram.user;

import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.administration.AdministratorRights;
import org.jmouse.telegram.administration.ChatDetails;
import org.jmouse.telegram.administration.ChatMember;
import org.jmouse.telegram.administration.ChatPermission;
import org.jmouse.telegram.administration.ForumTopic;
import org.jmouse.telegram.administration.InviteLink;
import org.jmouse.telegram.administration.InviteLinkTerms;
import org.jmouse.telegram.media.MediaSource;
import org.jmouse.telegram.spi.AdministrationTransport;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Chat administration as a person — today only what a bot cannot: <strong>creating a group</strong>.
 *
 * <p>Registered beside {@code BotApiChatAdministration}, it makes {@code ChatAdministration.createGroup}
 * work for a purpose whose identity is {@code USER}. Everything else a bot already administers; a person
 * administering through MTProto is not offered yet, and {@code RoutingChatAdministration} refuses it with
 * {@code CapabilityUnavailable} naming {@code BOT} before any method below is reached.
 */
public final class UserAdministration implements AdministrationTransport {

    private final UserAccounts accounts;

    public UserAdministration(UserAccounts accounts) {
        this.accounts = accounts;
    }

    @Override
    public IdentityKind kind() {
        return IdentityKind.USER;
    }

    @Override
    public boolean supports(Capability capability) {
        return capability == Capability.CREATE_CHAT;
    }

    @Override
    public ChatDetails createGroup(TelegramIdentity identity, String title, List<Long> userIds) {
        return accounts.ready(identity.credential()).createGroup(title, userIds);
    }

    /** A channel, which the shared {@code ChatAdministration} has no method for — reached through this class. */
    public ChatDetails createChannel(TelegramIdentity identity, String title, String description) {
        return accounts.ready(identity.credential()).createChannel(title, description);
    }

    // —— not offered as a person: unreachable through RoutingChatAdministration ——————————————————————

    @Override
    public ChatDetails details(TelegramIdentity identity, ChatReference chat) {
        throw unsupported(identity);
    }

    @Override
    public int memberCount(TelegramIdentity identity, ChatReference chat) {
        throw unsupported(identity);
    }

    @Override
    public void rename(TelegramIdentity identity, ChatReference chat, String title) {
        throw unsupported(identity);
    }

    @Override
    public void describe(TelegramIdentity identity, ChatReference chat, String description) {
        throw unsupported(identity);
    }

    @Override
    public void changePhoto(TelegramIdentity identity, ChatReference chat, MediaSource photo) {
        throw unsupported(identity);
    }

    @Override
    public void removePhoto(TelegramIdentity identity, ChatReference chat) {
        throw unsupported(identity);
    }

    @Override
    public void setPermissions(TelegramIdentity identity, ChatReference chat, Set<ChatPermission> allowed) {
        throw unsupported(identity);
    }

    @Override
    public ChatMember member(TelegramIdentity identity, ChatReference chat, long userId) {
        throw unsupported(identity);
    }

    @Override
    public List<ChatMember> administrators(TelegramIdentity identity, ChatReference chat) {
        throw unsupported(identity);
    }

    @Override
    public void promote(TelegramIdentity identity, ChatReference chat, long userId, AdministratorRights rights) {
        throw unsupported(identity);
    }

    @Override
    public void restrict(TelegramIdentity identity, ChatReference chat, long userId, Set<ChatPermission> allowed, Instant until) {
        throw unsupported(identity);
    }

    @Override
    public void ban(TelegramIdentity identity, ChatReference chat, long userId, Instant until, boolean revokeMessages) {
        throw unsupported(identity);
    }

    @Override
    public void unban(TelegramIdentity identity, ChatReference chat, long userId) {
        throw unsupported(identity);
    }

    @Override
    public InviteLink createInviteLink(TelegramIdentity identity, ChatReference chat, InviteLinkTerms terms) {
        throw unsupported(identity);
    }

    @Override
    public InviteLink editInviteLink(TelegramIdentity identity, ChatReference chat, String link, InviteLinkTerms terms) {
        throw unsupported(identity);
    }

    @Override
    public InviteLink revokeInviteLink(TelegramIdentity identity, ChatReference chat, String link) {
        throw unsupported(identity);
    }

    @Override
    public void approveJoinRequest(TelegramIdentity identity, ChatReference chat, long userId) {
        throw unsupported(identity);
    }

    @Override
    public void declineJoinRequest(TelegramIdentity identity, ChatReference chat, long userId) {
        throw unsupported(identity);
    }

    @Override
    public void pin(TelegramIdentity identity, MessageHandle message, boolean silently) {
        throw unsupported(identity);
    }

    @Override
    public void unpin(TelegramIdentity identity, MessageHandle message) {
        throw unsupported(identity);
    }

    @Override
    public void unpinAll(TelegramIdentity identity, ChatReference chat) {
        throw unsupported(identity);
    }

    @Override
    public ForumTopic createTopic(TelegramIdentity identity, ChatReference chat, String name) {
        throw unsupported(identity);
    }

    @Override
    public void renameTopic(TelegramIdentity identity, ChatReference topic, String name) {
        throw unsupported(identity);
    }

    @Override
    public void closeTopic(TelegramIdentity identity, ChatReference topic) {
        throw unsupported(identity);
    }

    @Override
    public void reopenTopic(TelegramIdentity identity, ChatReference topic) {
        throw unsupported(identity);
    }

    @Override
    public void deleteTopic(TelegramIdentity identity, ChatReference topic) {
        throw unsupported(identity);
    }

    private static TelegramException unsupported(TelegramIdentity identity) {
        return new TelegramException(new TelegramRefusal.CapabilityUnavailable(
                Capability.ADMINISTER_CHAT, identity.name(), IdentityKind.USER, IdentityKind.BOT));
    }
}
