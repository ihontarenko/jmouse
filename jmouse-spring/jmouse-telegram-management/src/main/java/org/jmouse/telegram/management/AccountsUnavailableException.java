package org.jmouse.telegram.management;

/**
 * Accounts or bindings were asked for in an installation that keeps none.
 *
 * <p>Identities configured in properties are not rows, and properties are not writable from an API —
 * so the answer is a sentence saying which arrangement this is, not an empty list that looks like a
 * database with nothing in it.
 */
public class AccountsUnavailableException extends RuntimeException {

    public AccountsUnavailableException(String what) {
        super(("%s are not kept in the database here — identities come from jmouse.telegram.identities. "
               + "Add jmouse-telegram-jpa, configure the vault (jmouse.vault.active-key) and set jmouse.telegram.accounts.store=database to manage them.").formatted(what));
    }
}
