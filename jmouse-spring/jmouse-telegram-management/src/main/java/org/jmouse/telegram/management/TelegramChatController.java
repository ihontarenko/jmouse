package org.jmouse.telegram.management;

import org.jmouse.telegram.ChatReference;
import org.jmouse.telegram.IdentitySource;
import org.jmouse.telegram.TelegramIdentity;
import org.jmouse.telegram.administration.AdministratorRights;
import org.jmouse.telegram.administration.ChatAdministration;
import org.jmouse.telegram.administration.ChatDetails;
import org.jmouse.telegram.administration.ChatMember;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * One chat as a purpose's bot sees it — what it is, how many are in it, who administers it, and whether
 * the bot is one of them.
 *
 * <h2>⚠️ There is no "every chat the bot is in"</h2>
 *
 * <p>The Bot API has no method that lists a bot's chats; a bot learns of one only from the update that
 * adds it. A listing would need those updates recorded as they arrive, which no store here does yet — so
 * this route answers for a chat that is named, and says nothing it would have to guess.
 *
 * <p>Read-only by design. Banning, promoting and the rest are {@link ChatAdministration} for the
 * product's own code, behind the product's own decisions — not a generic button on a library's API.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX + "/chats")
public class TelegramChatController {

    private final ChatAdministration administration;
    private final IdentitySource     identities;

    public TelegramChatController(ChatAdministration administration, IdentitySource identities) {
        this.administration = administration;
        this.identities     = identities;
    }

    /**
     * @param details        what the chat is
     * @param memberCount    how many are in it
     * @param administrators who administers it
     * @param botIsAdministrator whether this purpose's bot is one of them — ⚠️ without it, every
     *                       administration call is refused
     * @param botRights      the bot's powers there, {@link AdministratorRights#none()} when it is not one
     */
    public record ChatOverview(
            ChatDetails         details,
            int                 memberCount,
            List<ChatMember>    administrators,
            boolean             botIsAdministrator,
            AdministratorRights botRights
    ) {
    }

    /**
     * @param chat    a chat id or {@code @username}
     * @param purpose whose bot looks, default {@code general}
     */
    @GetMapping("/{chat}")
    public ChatOverview chat(
            @PathVariable("chat") String chat,
            @RequestParam(name = "purpose", defaultValue = IdentitySource.GENERAL) String purpose) {
        ChatReference      reference      = ChatReference.parse(chat);
        ChatAdministration asPurpose      = administration.as(purpose);
        List<ChatMember>   administrators = asPurpose.administrators(reference);
        Long               botUserId      = botUserIdOf(identities.require(purpose));

        ChatMember bot = administrators.stream()
                                       .filter(member -> botUserId != null && member.user().id() == botUserId)
                                       .findFirst()
                                       .orElse(null);

        return new ChatOverview(
                asPurpose.details(reference),
                asPurpose.memberCount(reference),
                administrators,
                bot != null,
                bot == null ? AdministratorRights.none() : bot.rights());
    }

    /**
     * A bot's own user id is the part of its token before the colon — {@code 123456:ABC…}.
     *
     * <p>⚠️ Read here and nowhere else, and only the digits leave this method: the token itself must never
     * reach a response.
     */
    private static Long botUserIdOf(TelegramIdentity identity) {
        String credential = identity.credential();
        int    colon      = credential == null ? -1 : credential.indexOf(':');

        if (colon <= 0) {
            return null;
        }

        try {
            return Long.parseLong(credential.substring(0, colon));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
