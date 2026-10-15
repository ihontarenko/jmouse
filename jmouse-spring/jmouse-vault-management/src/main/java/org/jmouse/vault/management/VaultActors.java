package org.jmouse.vault.management;

import java.security.Principal;

/**
 * 👤 Who to write into the audit trail and the "changed by" column for a caller.
 *
 * <p>A principal's name is whatever the product's authentication put there — a person's name for one sign-in,
 * a subject UUID for another — so a trail recorded from it reads as two different people. A product that knows
 * its people declares a bean of this type; without one the principal's name is used as it is.
 */
@FunctionalInterface
public interface VaultActors {

    /** @return the name to record, or null for an anonymous caller */
    String nameOf(Principal principal);

    static VaultActors principalNames() {
        return principal -> principal == null ? null : principal.getName();
    }
}
