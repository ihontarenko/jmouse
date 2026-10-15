package org.jmouse.telegram.administration;

import java.util.EnumSet;
import java.util.Set;

/**
 * What an administrator may do, and whether they act anonymously.
 *
 * <p>⚠️ Promoting with {@link #none()} <strong>demotes</strong> — that is how Telegram removes an
 * administrator, and there is no separate method for it.
 *
 * @param anonymous whether their messages appear as the chat's rather than theirs
 * @param granted   the powers they hold
 */
public record AdministratorRights(boolean anonymous, Set<AdministratorRight> granted) {

    public AdministratorRights {
        granted = granted == null || granted.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(granted));
    }

    public static AdministratorRights of(AdministratorRight... rights) {
        return new AdministratorRights(false, rights.length == 0 ? Set.of() : EnumSet.of(rights[0], rights));
    }

    /** No powers at all — promoting with this demotes. */
    public static AdministratorRights none() {
        return new AdministratorRights(false, Set.of());
    }

    public boolean has(AdministratorRight right) {
        return granted.contains(right);
    }
}
