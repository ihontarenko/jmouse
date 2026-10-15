package org.jmouse.telegram.spi;

import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.administration.AdministratorRights;
import org.jmouse.telegram.administration.ChatDetails;
import org.jmouse.telegram.administration.ChatMember;
import org.jmouse.telegram.administration.ChatPermission;
import org.jmouse.telegram.administration.ForumTopic;
import org.jmouse.telegram.administration.InviteLink;
import org.jmouse.telegram.administration.InviteLinkTerms;
import org.jmouse.telegram.media.MediaSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * One way of administering chats over one protocol — the counterpart of {@link TelegramTransport}.
 *
 * <p>Every method takes the identity to act as; {@code RoutingChatAdministration} resolves it and checks
 * {@link #supports} first. Failures are thrown as {@code TelegramException} carrying a refusal.
 */
public interface AdministrationTransport {

    IdentityKind kind();

    boolean supports(Capability capability);

    ChatDetails details(TelegramIdentity identity, ChatReference chat);

    int memberCount(TelegramIdentity identity, ChatReference chat);

    void rename(TelegramIdentity identity, ChatReference chat, String title);

    void describe(TelegramIdentity identity, ChatReference chat, String description);

    void changePhoto(TelegramIdentity identity, ChatReference chat, MediaSource photo);

    void removePhoto(TelegramIdentity identity, ChatReference chat);

    void setPermissions(TelegramIdentity identity, ChatReference chat, Set<ChatPermission> allowed);

    ChatMember member(TelegramIdentity identity, ChatReference chat, long userId);

    List<ChatMember> administrators(TelegramIdentity identity, ChatReference chat);

    void promote(TelegramIdentity identity, ChatReference chat, long userId, AdministratorRights rights);

    void restrict(TelegramIdentity identity, ChatReference chat, long userId, Set<ChatPermission> allowed, Instant until);

    void ban(TelegramIdentity identity, ChatReference chat, long userId, Instant until, boolean revokeMessages);

    void unban(TelegramIdentity identity, ChatReference chat, long userId);

    InviteLink createInviteLink(TelegramIdentity identity, ChatReference chat, InviteLinkTerms terms);

    InviteLink editInviteLink(TelegramIdentity identity, ChatReference chat, String link, InviteLinkTerms terms);

    InviteLink revokeInviteLink(TelegramIdentity identity, ChatReference chat, String link);

    void approveJoinRequest(TelegramIdentity identity, ChatReference chat, long userId);

    void declineJoinRequest(TelegramIdentity identity, ChatReference chat, long userId);

    void pin(TelegramIdentity identity, MessageHandle message, boolean silently);

    void unpin(TelegramIdentity identity, MessageHandle message);

    void unpinAll(TelegramIdentity identity, ChatReference chat);

    ForumTopic createTopic(TelegramIdentity identity, ChatReference chat, String name);

    void renameTopic(TelegramIdentity identity, ChatReference topic, String name);

    void closeTopic(TelegramIdentity identity, ChatReference topic);

    void reopenTopic(TelegramIdentity identity, ChatReference topic);

    void deleteTopic(TelegramIdentity identity, ChatReference topic);

    ChatDetails createGroup(TelegramIdentity identity, String title, List<Long> userIds);
}
