package org.jmouse.telegram.administration;

import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.media.MediaSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Administering chats — members, rights, bans, invite links, settings, pins, forum topics.
 *
 * <h2>⚠️ Deliberately not on {@code TelegramGateway}</h2>
 *
 * <p>The code that sends notifications and the code that bans members are never the same code, and an
 * interface that offered both would hand the power to ban to everything that can send. A product injects
 * this only where it administers.
 *
 * <h2>⚠️ What a bot can administer</h2>
 *
 * <p>Only the chats it was added to <em>as an administrator</em>, with the rights it was given there.
 * Anything else is refused by Telegram and arrives as {@code InsufficientRights}. Creating a chat is not
 * in the Bot API at all: {@link #createGroup} is refused with {@code CapabilityUnavailable} naming the
 * identity kind that would have it.
 *
 * <p>{@link RoutingChatAdministration} is the implementation; every call is checked against the
 * transport's {@link Capability} roster before it is made.
 */
public interface ChatAdministration {

    // —— the chat ————————————————————————————————————————————————————————————————————————————————

    ChatDetails details(ChatReference chat);

    int memberCount(ChatReference chat);

    void rename(ChatReference chat, String title);

    /** {@code null} or blank clears it. */
    void describe(ChatReference chat, String description);

    /** ⚠️ An upload only — {@link MediaSource.LocalFile} or {@link MediaSource.Bytes}. Telegram takes no URL here. */
    void changePhoto(ChatReference chat, MediaSource photo);

    void removePhoto(ChatReference chat);

    /** What every ordinary member may do — the chat's defaults. */
    void setPermissions(ChatReference chat, Set<ChatPermission> allowed);

    // —— members ——————————————————————————————————————————————————————————————————————————————————

    ChatMember member(ChatReference chat, long userId);

    List<ChatMember> administrators(ChatReference chat);

    /** ⚠️ {@link AdministratorRights#none()} demotes. */
    void promote(ChatReference chat, long userId, AdministratorRights rights);

    /**
     * Narrows what one member may do.
     *
     * @param until when it ends, or {@code null} for never. ⚠️ Telegram treats under 30 seconds or over
     *              366 days as forever
     */
    void restrict(ChatReference chat, long userId, Set<ChatPermission> allowed, Instant until);

    /**
     * @param until          when it ends, or {@code null} for never
     * @param revokeMessages also delete everything they wrote — ⚠️ cannot be undone
     */
    void ban(ChatReference chat, long userId, Instant until, boolean revokeMessages);

    /** ⚠️ Only lifts a ban; it never removes somebody who is in the chat. */
    void unban(ChatReference chat, long userId);

    // —— invitations ——————————————————————————————————————————————————————————————————————————————

    InviteLink createInviteLink(ChatReference chat, InviteLinkTerms terms);

    InviteLink editInviteLink(ChatReference chat, String link, InviteLinkTerms terms);

    InviteLink revokeInviteLink(ChatReference chat, String link);

    void approveJoinRequest(ChatReference chat, long userId);

    void declineJoinRequest(ChatReference chat, long userId);

    // —— pins ———————————————————————————————————————————————————————————————————————————————————

    void pin(MessageHandle message, boolean silently);

    void unpin(MessageHandle message);

    void unpinAll(ChatReference chat);

    // —— forum topics —————————————————————————————————————————————————————————————————————————————

    ForumTopic createTopic(ChatReference chat, String name);

    /** @param topic the chat in the topic's thread, as {@link ForumTopic#topic()} gives it */
    void renameTopic(ChatReference topic, String name);

    void closeTopic(ChatReference topic);

    void reopenTopic(ChatReference topic);

    /** ⚠️ Deletes every message in it. */
    void deleteTopic(ChatReference topic);

    // —— creation ———————————————————————————————————————————————————————————————————————————————

    /**
     * Creates a group.
     *
     * <p>⚠️ <strong>Not possible for a bot.</strong> Refused with {@code CapabilityUnavailable} until a
     * user-account transport exists ({@code JMF-339}).
     */
    ChatDetails createGroup(String title, List<Long> userIds);

    // —— routing ————————————————————————————————————————————————————————————————————————————————

    /** Whether the identity behind this view's transport can do this at all — never a permission. */
    boolean supports(Capability capability);

    /** A view bound to a purpose; bare calls mean {@code general}. */
    ChatAdministration as(String purpose);
}
