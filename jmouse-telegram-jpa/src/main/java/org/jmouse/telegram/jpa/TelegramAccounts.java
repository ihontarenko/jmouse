package org.jmouse.telegram.jpa;

import org.jmouse.telegram.IdentityKind;

import java.util.List;
import java.util.Optional;

/**
 * Reading and administering the account table.
 *
 * <p>A port rather than a repository interface a framework generates, for the reason
 * {@code jmouse-storage-jpa}'s registry gives: whoever owns the table owns the queries, so the one place
 * that knows how the table is used stays inside the library rather than being spread across whichever
 * products query it.
 *
 * <h2>⚠️ Every write takes the credential in the CLEAR and seals it here</h2>
 *
 * <p>The alternative — a caller sealing before it writes — means every caller holds a
 * {@link CredentialCipher}, and the first one that forgets writes a plaintext token into a column that
 * looks exactly like a sealed one. Sealing on the way in makes that unrepresentable.
 *
 * <h2>⚠️ No read ever answers with a credential, sealed or otherwise</h2>
 *
 * <p>{@link #list()} answers with {@link Description}, which has no credential field at all. That is what
 * lets an administration endpoint list accounts without a reviewer having to check whether it masked
 * something — there is nothing to mask. Opening a credential happens only in {@link JpaIdentitySource},
 * at the moment of use.
 *
 * <h2>Transactions</h2>
 *
 * <p>⚠️ Not demarcated here. A product's transaction boundary is its own, and a library that opened one
 * would either nest inside the caller's or fight it. Call these inside whatever transaction the product
 * already has.
 */
public interface TelegramAccounts {

    /**
     * Every account, without credentials, most recently updated first.
     *
     * <p>For an administration listing. ⚠️ Unpaged deliberately: this table holds one row per purpose —
     * a handful, never a page — and a limit would imply otherwise.
     */
    List<Description> list();

    /** One account by the purpose it answers for. */
    Optional<Description> find(String purpose);

    /**
     * Creates or replaces the account for a purpose.
     *
     * @param credential ⚠️ in the clear; sealed on the way in
     * @return what the row says afterwards
     */
    Description upsert(String purpose, IdentityKind kind, String credential, String apiBase);

    /**
     * Replaces only the credential, leaving everything else alone.
     *
     * <p>The operation a rotation actually is. Separate from {@link #upsert} so rotating cannot
     * accidentally reset a disabled account to enabled, or drop a configured {@code apiBase}.
     *
     * @param credential ⚠️ in the clear; sealed on the way in
     */
    Description rotate(String purpose, String credential);

    /** Switches an account on or off without losing it — see {@link TelegramAccount#disable()}. */
    Description setEnabled(String purpose, boolean enabled);

    /**
     * Removes an account.
     *
     * @return whether there was one
     */
    boolean remove(String purpose);

    /**
     * What a read answers with: everything about an account except the one thing that must not travel.
     *
     * @param purpose   what a caller asks for
     * @param kind      which protocol it speaks
     * @param apiBase   a self-hosted Bot API server, or null
     * @param enabled   whether it will be used
     * @param updatedAt when it last changed — ⚠️ what tells an administrator whether a rotation landed
     */
    record Description(
            String                  purpose,
            IdentityKind            kind,
            String                  apiBase,
            boolean                 enabled,
            java.time.LocalDateTime updatedAt
    ) {
    }
}
