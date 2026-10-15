package org.jmouse.telegram.bot.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.jmouse.telegram.Capability;
import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.MessageHandle;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.TelegramRefusal;
import org.jmouse.telegram.administration.AdministratorRight;
import org.jmouse.telegram.administration.AdministratorRights;
import org.jmouse.telegram.administration.ChatAdministration;
import org.jmouse.telegram.administration.ChatMember;
import org.jmouse.telegram.administration.ChatPermission;
import org.jmouse.telegram.administration.ForumTopic;
import org.jmouse.telegram.administration.InviteLink;
import org.jmouse.telegram.administration.InviteLinkTerms;
import org.jmouse.telegram.administration.MemberStatus;
import org.jmouse.telegram.administration.RoutingChatAdministration;
import org.jmouse.telegram.bot.BotApiChatAdministration;
import org.jmouse.telegram.media.MediaSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.jmouse.telegram.smoke.Checks.check;
import static org.jmouse.telegram.smoke.Checks.refusalOf;
import static org.jmouse.telegram.smoke.Checks.report;
import static org.jmouse.telegram.smoke.Checks.throwsIllegalArgument;

/**
 * Drives {@link BotApiChatAdministration} through {@link RoutingChatAdministration} against a local HTTP
 * server that answers the way the Bot API does, and checks both what was SENT and what was READ.
 *
 * <p>⚠️ It reaches no network and needs no bot: the identity's API base points at the local server. What
 * it cannot prove is that Telegram still names its fields this way — that needs a real bot in a real
 * group, which no smoke here has.
 */
public final class AdministrationSmoke {

    private static final ObjectMapper          JSON     = new ObjectMapper();
    private static final Map<String, JsonNode> received = new ConcurrentHashMap<>();
    private static final Map<String, String>   answers  = new HashMap<>();

    private static final ChatReference GROUP = ChatReference.of(-1001234567890L);

    public static void main(String[] arguments) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path   = exchange.getRequestURI().getPath();
            String method = path.substring(path.lastIndexOf('/') + 1);
            byte[] body   = exchange.getRequestBody().readAllBytes();

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            received.put(method, contentType != null && contentType.startsWith("application/json")
                    ? JSON.readTree(body)
                    : JSON.getNodeFactory().textNode(new String(body, StandardCharsets.ISO_8859_1)));

            byte[] answer = answers.getOrDefault(method, "{\"ok\":true,\"result\":true}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        server.start();

        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            ChatAdministration administration = new RoutingChatAdministration(
                    IdentitySource.fixed(TelegramIdentity.bot("smoke", "123:token", base)),
                    List.of(new BotApiChatAdministration()));

            readsAMember(administration);
            promotesWithEveryRightExplicit(administration);
            demotesWithNoRights(administration);
            restrictsWithIndependentPermissionsAndADeadline(administration);
            unbansOnlyIfBanned(administration);
            createsAnInviteLinkAndReadsItBack(administration);
            refusesALimitedLinkThatAlsoAsks();
            createsATopicAndAddressesItsThread(administration);
            pinsWithoutTheThread(administration);
            uploadsAPhotoAsMultipart(administration);
            refusesAPhotoByAddress(administration);
            refusesToCreateAGroupAsABot(administration);
            translatesMissingRights(administration);
        } finally {
            server.stop(0);
        }

        report();
    }

    private static void readsAMember(ChatAdministration administration) {
        answers.put("getChatMember", """
                {"ok":true,"result":{"status":"restricted","user":{"id":7,"first_name":"Olena","is_bot":false},
                 "until_date":1893456000,"can_send_messages":true,"can_send_photos":false}}""");

        ChatMember member = administration.member(GROUP, 7);

        check("asked about the right user", received.get("getChatMember").path("user_id").asLong() == 7);
        check("a restricted member reads as RESTRICTED", member.status() == MemberStatus.RESTRICTED);
        check("with the permissions Telegram reported", member.permissions().equals(EnumSet.of(ChatPermission.SEND_MESSAGES)));
        check("until the date Telegram gave", Instant.ofEpochSecond(1893456000).equals(member.until()));
        check("the user is read", member.user().id() == 7 && "Olena".equals(member.user().firstName()));
    }

    private static void promotesWithEveryRightExplicit(ChatAdministration administration) {
        administration.promote(GROUP, 7, AdministratorRights.of(AdministratorRight.PIN_MESSAGES, AdministratorRight.INVITE_USERS));
        JsonNode sent = received.get("promoteChatMember");

        check("a granted right is sent true", sent.path("can_pin_messages").asBoolean() && sent.path("can_invite_users").asBoolean());
        check("an ungranted right is sent false, not left out", sent.has("can_promote_members") && !sent.path("can_promote_members").asBoolean());
    }

    private static void demotesWithNoRights(ChatAdministration administration) {
        administration.promote(GROUP, 7, AdministratorRights.none());
        JsonNode sent = received.get("promoteChatMember");

        boolean anyTrue = false;
        for (AdministratorRight right : AdministratorRight.values()) {
            anyTrue |= sent.path(right.wireName()).asBoolean();
        }

        check("promoting with none demotes — every right false", !anyTrue);
    }

    private static void restrictsWithIndependentPermissionsAndADeadline(ChatAdministration administration) {
        Instant until = Instant.parse("2027-01-01T00:00:00Z");
        administration.restrict(GROUP, 7, EnumSet.of(ChatPermission.SEND_MESSAGES), until);
        JsonNode sent = received.get("restrictChatMember");

        check("permissions are a complete object", sent.path("permissions").size() == ChatPermission.values().length);
        check("independent permissions are asked for", sent.path("use_independent_chat_permissions").asBoolean());
        check("the deadline is epoch seconds", sent.path("until_date").asLong() == until.getEpochSecond());
    }

    private static void unbansOnlyIfBanned(ChatAdministration administration) {
        administration.unban(GROUP, 7);

        check("unban never removes a present member", received.get("unbanChatMember").path("only_if_banned").asBoolean());
    }

    private static void createsAnInviteLinkAndReadsItBack(ChatAdministration administration) {
        answers.put("createChatInviteLink", """
                {"ok":true,"result":{"invite_link":"https://t.me/+abc","name":"ops","member_limit":5,
                 "creates_join_request":false,"is_primary":false,"is_revoked":false}}""");

        InviteLink link = administration.createInviteLink(GROUP, new InviteLinkTerms("ops", null, 5, false));

        check("the terms are sent", received.get("createChatInviteLink").path("member_limit").asInt() == 5);
        check("the link is read back", "https://t.me/+abc".equals(link.link()) && link.memberLimit() == 5);
        check("and never printed", !link.toString().contains("t.me"));
    }

    private static void refusesALimitedLinkThatAlsoAsks() {
        check("a member limit and a join request together are refused",
              throwsIllegalArgument(() -> new InviteLinkTerms(null, null, 5, true)));
    }

    private static void createsATopicAndAddressesItsThread(ChatAdministration administration) {
        answers.put("createForumTopic", "{\"ok\":true,\"result\":{\"message_thread_id\":42,\"name\":\"Kitsu\"}}");

        ForumTopic topic = administration.createTopic(GROUP, "Kitsu");
        administration.closeTopic(topic.topic());

        check("a new topic carries its thread", topic.threadId() == 42);
        check("a topic call sends the thread", received.get("closeForumTopic").path("message_thread_id").asInt() == 42);
        check("a topic call on a plain chat is refused",
              throwsIllegalArgument(() -> {
                  administration.closeTopic(GROUP);
                  return null;
              }));
    }

    private static void pinsWithoutTheThread(ChatAdministration administration) {
        administration.pin(new MessageHandle(GROUP, 9), true);
        JsonNode sent = received.get("pinChatMessage");

        check("a pin names the message, silently", sent.path("message_id").asInt() == 9 && sent.path("disable_notification").asBoolean());
    }

    private static void uploadsAPhotoAsMultipart(ChatAdministration administration) {
        administration.changePhoto(GROUP, new MediaSource.Bytes("logo.png", new byte[] {1, 2, 3}, "image/png"));

        check("a chat photo is uploaded as a file part", received.get("setChatPhoto").asText().contains("filename=\"logo.png\""));
    }

    private static void refusesAPhotoByAddress(ChatAdministration administration) {
        check("a photo by URL is refused before any call", throwsIllegalArgument(() -> {
            administration.changePhoto(GROUP, new MediaSource.RemoteUrl("https://example.org/logo.png"));
            return null;
        }));
    }

    private static void refusesToCreateAGroupAsABot(ChatAdministration administration) {
        TelegramRefusal refusal = refusalOf(() -> administration.createGroup("New", List.of()));

        check("a bot cannot create a group, and says so", refusal instanceof TelegramRefusal.CapabilityUnavailable unavailable
                && unavailable.capability() == Capability.CREATE_CHAT);
        check("supports() agrees", !administration.supports(Capability.CREATE_CHAT) && administration.supports(Capability.ADMINISTER_CHAT));
    }

    private static void translatesMissingRights(ChatAdministration administration) {
        answers.put("banChatMember", "{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: not enough rights to restrict/unrestrict chat member\"}");

        TelegramRefusal refusal = refusalOf(() -> administration.ban(GROUP, 7, null, false));

        check("missing rights arrive as InsufficientRights", refusal instanceof TelegramRefusal.InsufficientRights);
    }
}
