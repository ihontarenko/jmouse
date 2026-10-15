package org.jmouse.telegram.administration;

import org.jmouse.telegram.update.TelegramUser;

import java.time.Instant;
import java.util.Set;

/**
 * Somebody's standing in one chat.
 *
 * @param user        who
 * @param status      where they stand
 * @param rights      their powers when {@link MemberStatus#ADMINISTRATOR}, {@link AdministratorRights#none()}
 *                    otherwise. ⚠️ A {@link MemberStatus#CREATOR} holds every power whatever this says
 * @param permissions what they may do when {@link MemberStatus#RESTRICTED}; empty otherwise, which means
 *                    "the chat's defaults", not "nothing"
 * @param until       when a restriction or a ban ends, or {@code null} for never
 * @param customTitle an administrator's title in the member list, or {@code null}
 */
public record ChatMember(
        TelegramUser             user,
        MemberStatus             status,
        AdministratorRights      rights,
        Set<ChatPermission>      permissions,
        Instant                  until,
        String                   customTitle
) {

    public ChatMember {
        if (user == null || status == null) {
            throw new IllegalArgumentException("A chat member needs a user and a status");
        }

        rights      = rights == null ? AdministratorRights.none() : rights;
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean isAdministrator() {
        return status == MemberStatus.CREATOR || status == MemberStatus.ADMINISTRATOR;
    }
}
