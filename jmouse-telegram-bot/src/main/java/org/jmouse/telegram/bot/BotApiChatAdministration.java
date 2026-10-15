package org.jmouse.telegram.bot;

import com.fasterxml.jackson.databind.JsonNode;
import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentityKind;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.TelegramException;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.administration.AdministratorRight;
import org.jmouse.telegram.administration.AdministratorRights;
import org.jmouse.telegram.administration.ChatDetails;
import org.jmouse.telegram.administration.ChatMember;
import org.jmouse.telegram.administration.ChatPermission;
import org.jmouse.telegram.administration.ForumTopic;
import org.jmouse.telegram.administration.InviteLink;
import org.jmouse.telegram.administration.InviteLinkTerms;
import org.jmouse.telegram.administration.MemberStatus;
import org.jmouse.telegram.media.MediaSource;
import org.jmouse.telegram.spi.AdministrationTransport;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chat administration over the Bot API.
 *
 * <p>⚠️ <strong>A bot administers only chats it was added to as an administrator</strong>, with the
 * rights it was given there; Telegram's refusal arrives as {@code InsufficientRights} through
 * {@link BotApiErrors}. {@link Capability#CREATE_CHAT} is not supported — the Bot API has no such method.
 *
 * <p>⚠️ Chat-level calls send the chat <em>without</em> its thread: {@code message_thread_id} belongs to
 * topic calls, and Telegram refuses it where it does not apply.
 */
public class BotApiChatAdministration extends BotApiClient implements AdministrationTransport {

    private static final Set<Capability> SUPPORTED = EnumSet.of(Capability.ADMINISTER_CHAT, Capability.MANAGE_TOPICS);

    public BotApiChatAdministration() {
        super();
    }

    public BotApiChatAdministration(Duration connectTimeout, Duration readTimeout) {
        super(connectTimeout, readTimeout);
    }

    @Override
    public IdentityKind kind() {
        return IdentityKind.BOT;
    }

    @Override
    public boolean supports(Capability capability) {
        return SUPPORTED.contains(capability);
    }

    // —— the chat ————————————————————————————————————————————————————————————————————————————————

    @Override
    public ChatDetails details(TelegramIdentity identity, ChatReference chat) {
        return readChat(call(identity, "getChat", chatOnly(chat)));
    }

    @Override
    public int memberCount(TelegramIdentity identity, ChatReference chat) {
        return call(identity, "getChatMemberCount", chatOnly(chat)).asInt();
    }

    @Override
    public void rename(TelegramIdentity identity, ChatReference chat, String title) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("title", title);
        call(identity, "setChatTitle", fields);
    }

    @Override
    public void describe(TelegramIdentity identity, ChatReference chat, String description) {
        Map<String, Object> fields = chatOnly(chat);
        // ⚠️ An empty description clears it; a missing field would too, but empty says so on purpose.
        fields.put("description", description == null ? "" : description);
        call(identity, "setChatDescription", fields);
    }

    @Override
    public void changePhoto(TelegramIdentity identity, ChatReference chat, MediaSource photo) {
        MultipartBody body = new MultipartBody().field("chat_id", chat.wireValue());

        switch (photo) {
            case MediaSource.Bytes bytes -> body.file("photo", bytes.fileName(), bytes.contentType(), bytes.content());
            case MediaSource.LocalFile file -> body.file("photo", file.path(), file.contentType());
            case MediaSource.FileId ignored -> throw notAnUpload();
            case MediaSource.RemoteUrl ignored -> throw notAnUpload();
        }

        upload(identity, "setChatPhoto", body);
    }

    @Override
    public void removePhoto(TelegramIdentity identity, ChatReference chat) {
        call(identity, "deleteChatPhoto", chatOnly(chat));
    }

    @Override
    public void setPermissions(TelegramIdentity identity, ChatReference chat, Set<ChatPermission> allowed) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("permissions", permissionsOf(allowed));
        // ⚠️ Without this, Telegram derives the media permissions from can_send_messages and the set is
        // not what was asked for.
        fields.put("use_independent_chat_permissions", true);
        call(identity, "setChatPermissions", fields);
    }

    // —— members ——————————————————————————————————————————————————————————————————————————————————

    @Override
    public ChatMember member(TelegramIdentity identity, ChatReference chat, long userId) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("user_id", userId);
        return readMember(call(identity, "getChatMember", fields));
    }

    @Override
    public List<ChatMember> administrators(TelegramIdentity identity, ChatReference chat) {
        List<ChatMember> administrators = new ArrayList<>();

        for (JsonNode member : call(identity, "getChatAdministrators", chatOnly(chat))) {
            administrators.add(readMember(member));
        }

        return administrators;
    }

    @Override
    public void promote(TelegramIdentity identity, ChatReference chat, long userId, AdministratorRights rights) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("user_id", userId);
        fields.put("is_anonymous", rights.anonymous());

        // ⚠️ Every right, true or false: a right left out is false to Telegram, and demotion is exactly
        // "every right false".
        for (AdministratorRight right : AdministratorRight.values()) {
            fields.put(right.wireName(), rights.has(right));
        }

        call(identity, "promoteChatMember", fields);
    }

    @Override
    public void restrict(
            TelegramIdentity identity, ChatReference chat, long userId, Set<ChatPermission> allowed, Instant until) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("user_id", userId);
        fields.put("permissions", permissionsOf(allowed));
        fields.put("use_independent_chat_permissions", true);
        putUntil(fields, until);
        call(identity, "restrictChatMember", fields);
    }

    @Override
    public void ban(TelegramIdentity identity, ChatReference chat, long userId, Instant until, boolean revokeMessages) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("user_id", userId);
        fields.put("revoke_messages", revokeMessages);
        putUntil(fields, until);
        call(identity, "banChatMember", fields);
    }

    @Override
    public void unban(TelegramIdentity identity, ChatReference chat, long userId) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("user_id", userId);
        // ⚠️ Without it, unbanning somebody who is IN the chat removes them — Telegram's own default.
        fields.put("only_if_banned", true);
        call(identity, "unbanChatMember", fields);
    }

    // —— invitations ——————————————————————————————————————————————————————————————————————————————

    @Override
    public InviteLink createInviteLink(TelegramIdentity identity, ChatReference chat, InviteLinkTerms terms) {
        Map<String, Object> fields = chatOnly(chat);
        putTerms(fields, terms);
        return readInviteLink(call(identity, "createChatInviteLink", fields));
    }

    @Override
    public InviteLink editInviteLink(
            TelegramIdentity identity, ChatReference chat, String link, InviteLinkTerms terms) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("invite_link", link);
        putTerms(fields, terms);
        return readInviteLink(call(identity, "editChatInviteLink", fields));
    }

    @Override
    public InviteLink revokeInviteLink(TelegramIdentity identity, ChatReference chat, String link) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("invite_link", link);
        return readInviteLink(call(identity, "revokeChatInviteLink", fields));
    }

    @Override
    public void approveJoinRequest(TelegramIdentity identity, ChatReference chat, long userId) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("user_id", userId);
        call(identity, "approveChatJoinRequest", fields);
    }

    @Override
    public void declineJoinRequest(TelegramIdentity identity, ChatReference chat, long userId) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("user_id", userId);
        call(identity, "declineChatJoinRequest", fields);
    }

    // —— pins ———————————————————————————————————————————————————————————————————————————————————

    @Override
    public void pin(TelegramIdentity identity, MessageHandle message, boolean silently) {
        Map<String, Object> fields = chatOnly(message.chat());
        fields.put("message_id", message.messageId());
        fields.put("disable_notification", silently);
        call(identity, "pinChatMessage", fields);
    }

    @Override
    public void unpin(TelegramIdentity identity, MessageHandle message) {
        Map<String, Object> fields = chatOnly(message.chat());
        fields.put("message_id", message.messageId());
        call(identity, "unpinChatMessage", fields);
    }

    @Override
    public void unpinAll(TelegramIdentity identity, ChatReference chat) {
        call(identity, "unpinAllChatMessages", chatOnly(chat));
    }

    // —— forum topics —————————————————————————————————————————————————————————————————————————————

    @Override
    public ForumTopic createTopic(TelegramIdentity identity, ChatReference chat, String name) {
        Map<String, Object> fields = chatOnly(chat);
        fields.put("name", name);

        JsonNode topic = call(identity, "createForumTopic", fields);

        return new ForumTopic(chat.inChat().inThread(topic.path("message_thread_id").asInt()),
                              topic.path("name").asText(name));
    }

    @Override
    public void renameTopic(TelegramIdentity identity, ChatReference topic, String name) {
        Map<String, Object> fields = topicFields(topic);
        fields.put("name", name);
        call(identity, "editForumTopic", fields);
    }

    @Override
    public void closeTopic(TelegramIdentity identity, ChatReference topic) {
        call(identity, "closeForumTopic", topicFields(topic));
    }

    @Override
    public void reopenTopic(TelegramIdentity identity, ChatReference topic) {
        call(identity, "reopenForumTopic", topicFields(topic));
    }

    @Override
    public void deleteTopic(TelegramIdentity identity, ChatReference topic) {
        call(identity, "deleteForumTopic", topicFields(topic));
    }

    // —— creation ———————————————————————————————————————————————————————————————————————————————

    @Override
    public ChatDetails createGroup(TelegramIdentity identity, String title, List<Long> userIds) {
        // Unreachable through RoutingChatAdministration, which checks CREATE_CHAT first. Kept as a refusal
        // rather than an UnsupportedOperationException for a caller that holds this class directly.
        throw new TelegramException(new TelegramRefusal.CapabilityUnavailable(
                Capability.CREATE_CHAT, identity.name(), IdentityKind.BOT, null));
    }

    // —— wire ————————————————————————————————————————————————————————————————————————————————————

    private static Map<String, Object> chatOnly(ChatReference chat) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("chat_id", chat.wireValue());
        return fields;
    }

    private static Map<String, Object> topicFields(ChatReference topic) {
        Map<String, Object> fields = chatOnly(topic);
        fields.put("message_thread_id", topic.threadId());
        return fields;
    }

    private static Map<String, Object> permissionsOf(Set<ChatPermission> allowed) {
        Map<String, Object> permissions = new LinkedHashMap<>();

        for (ChatPermission permission : ChatPermission.values()) {
            permissions.put(permission.wireName(), allowed.contains(permission));
        }

        return permissions;
    }

    private static void putUntil(Map<String, Object> fields, Instant until) {
        if (until != null) {
            fields.put("until_date", until.getEpochSecond());
        }
    }

    private static void putTerms(Map<String, Object> fields, InviteLinkTerms terms) {
        if (terms.name() != null) {
            fields.put("name", terms.name());
        }

        if (terms.expiresAt() != null) {
            fields.put("expire_date", terms.expiresAt().getEpochSecond());
        }

        if (terms.memberLimit() != null) {
            fields.put("member_limit", terms.memberLimit());
        }

        fields.put("creates_join_request", terms.joinRequestRequired());
    }

    private static ChatDetails readChat(JsonNode chat) {
        return new ChatDetails(
                ChatReference.of(chat.path("id").asLong()),
                chat.path("type").asText(null),
                textOrNull(chat, "title"),
                textOrNull(chat, "username"),
                textOrNull(chat, "description"),
                chat.path("is_forum").asBoolean(false),
                textOrNull(chat, "invite_link"));
    }

    private static ChatMember readMember(JsonNode member) {
        MemberStatus status = MemberStatus.fromWire(member.path("status").asText());

        Set<AdministratorRight> rights = EnumSet.noneOf(AdministratorRight.class);
        for (AdministratorRight right : AdministratorRight.values()) {
            if (member.path(right.wireName()).asBoolean(false)) {
                rights.add(right);
            }
        }

        Set<ChatPermission> permissions = EnumSet.noneOf(ChatPermission.class);
        if (status == MemberStatus.RESTRICTED) {
            for (ChatPermission permission : ChatPermission.values()) {
                if (member.path(permission.wireName()).asBoolean(false)) {
                    permissions.add(permission);
                }
            }
        }

        // ⚠️ until_date 0 means forever, not 1970.
        long    untilSeconds = member.path("until_date").asLong(0);
        Instant until        = untilSeconds == 0 ? null : Instant.ofEpochSecond(untilSeconds);

        return new ChatMember(
                BotApiUpdates.readUser(member.path("user")),
                status,
                new AdministratorRights(member.path("is_anonymous").asBoolean(false), rights),
                permissions,
                until,
                textOrNull(member, "custom_title"));
    }

    private static InviteLink readInviteLink(JsonNode link) {
        long    expireSeconds = link.path("expire_date").asLong(0);
        Integer memberLimit   = link.hasNonNull("member_limit") ? link.get("member_limit").asInt() : null;

        return new InviteLink(
                link.path("invite_link").asText(),
                textOrNull(link, "name"),
                expireSeconds == 0 ? null : Instant.ofEpochSecond(expireSeconds),
                memberLimit,
                link.path("creates_join_request").asBoolean(false),
                link.path("is_primary").asBoolean(false),
                link.path("is_revoked").asBoolean(false));
    }

    private static String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static IllegalArgumentException notAnUpload() {
        return new IllegalArgumentException("A chat photo must be uploaded; Telegram takes no URL or file id here");
    }
}
