package org.jmouse.telegram.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.jmouse.telegram.IdentityKind;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link TelegramAccounts} over Jakarta Persistence.
 *
 * <p>Jakarta Persistence alone — no Spring Data, no Spring. An {@link EntityManager} is what every
 * adopting product already has, whatever it wires it with.
 */
public final class JpaTelegramAccounts implements TelegramAccounts {

    private final EntityManager     entityManager;
    private final CredentialCipher  cipher;

    public JpaTelegramAccounts(EntityManager entityManager, CredentialCipher cipher) {
        this.entityManager = Objects.requireNonNull(entityManager, "entity manager");
        this.cipher        = Objects.requireNonNull(cipher, "credential cipher");
    }

    @Override
    public List<Description> list() {
        return entityManager
                .createQuery("""
                        SELECT account FROM TelegramAccount account
                        ORDER BY account.updatedAt DESC
                        """, TelegramAccount.class)
                .getResultList()
                .stream()
                .map(JpaTelegramAccounts::describe)
                .toList();
    }

    @Override
    public Optional<Description> find(String purpose) {
        return locate(purpose).map(JpaTelegramAccounts::describe);
    }

    @Override
    public Description upsert(String purpose, IdentityKind kind, String credential, String apiBase) {
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(kind, "kind");
        requireCredential(credential);

        TelegramAccount account = locate(purpose).orElse(null);

        if (account == null) {
            account = new TelegramAccount(
                    UUID.randomUUID().toString(), purpose, kind, cipher.seal(credential), apiBase);

            entityManager.persist(account);
        } else {
            account.reseal(cipher.seal(credential));
            account.pointAt(apiBase);
        }

        return describe(account);
    }

    @Override
    public Description rotate(String purpose, String credential) {
        requireCredential(credential);

        TelegramAccount account = require(purpose);

        account.reseal(cipher.seal(credential));

        return describe(account);
    }

    @Override
    public Description setEnabled(String purpose, boolean enabled) {
        TelegramAccount account = require(purpose);

        if (enabled) {
            account.enable();
        } else {
            account.disable();
        }

        return describe(account);
    }

    @Override
    public boolean remove(String purpose) {
        TelegramAccount account = locate(purpose).orElse(null);

        if (account == null) {
            return false;
        }

        entityManager.remove(account);

        return true;
    }

    /**
     * The row for a purpose, for {@link JpaIdentitySource}.
     *
     * <p>⚠️ Package-private and returning the entity, which is the one place the sealed credential is
     * reachable. Kept off {@link TelegramAccounts} on purpose: a public method answering with the entity
     * would put the sealed value within reach of every caller, and the next step after that is somebody
     * logging it.
     */
    Optional<TelegramAccount> locate(String purpose) {
        try {
            return Optional.of(entityManager
                    .createQuery("""
                            SELECT account FROM TelegramAccount account
                            WHERE account.purpose = :purpose
                            """, TelegramAccount.class)
                    .setParameter("purpose", purpose)
                    .getSingleResult());
        } catch (NoResultException exception) {
            return Optional.empty();
        }
    }

    /** Opens the credential — the only path that does. Called by {@link JpaIdentitySource}. */
    String openCredential(TelegramAccount account) {
        return cipher.open(account.getCredentialSealed());
    }

    private TelegramAccount require(String purpose) {
        return locate(purpose).orElseThrow(() -> new IllegalArgumentException(
                "no Telegram account is configured for purpose '%s'".formatted(purpose)));
    }

    /**
     * ⚠️ Rejects a blank credential rather than sealing one. A sealed empty string is a perfectly valid
     * row whose failure arrives later as an `Unauthorized` from Telegram — which reads as a revoked
     * token and sends somebody to rotate a credential that was never set.
     */
    private void requireCredential(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("a Telegram account needs a credential");
        }
    }

    private static Description describe(TelegramAccount account) {
        return new Description(
                account.getPurpose(),
                account.getKind(),
                account.getApiBase(),
                account.isEnabled(),
                account.getUpdatedAt());
    }
}
