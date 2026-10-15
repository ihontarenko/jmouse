package org.jmouse.vault.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.vault.SecretEntry;
import org.jmouse.vault.SecretName;
import org.jmouse.vault.SecretValue;
import org.jmouse.vault.seal.SecretCipher;
import org.jmouse.vault.source.WritableVaultSource;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 🗄️ The database source: secrets sealed in {@code vault_secrets}, the only source a running application
 * can write. Jakarta Persistence alone; transactions belong to the caller.
 */
public final class JpaVaultSource implements WritableVaultSource {

    public static final String NAME = "database";

    private static final String CONTEXT_PREFIX = "vault-secret:";
    private static final String EXTERNAL_KEY   = "external";

    private final EntityManager entityManager;
    private final SecretCipher  cipher;
    private final Clock         clock;

    public JpaVaultSource(EntityManager entityManager, SecretCipher cipher, Clock clock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entity manager");
        this.cipher        = Objects.requireNonNull(cipher, "cipher");
        this.clock         = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<SecretValue> find(SecretName name) {
        VaultSecretRecord record = entityManager.find(VaultSecretRecord.class, name.value());

        if (record == null) {
            return Optional.empty();
        }

        return Optional.of(SecretValue.of(cipher.open(record.getSealedValue(), contextOf(name))));
    }

    @Override
    public List<SecretEntry> list() {
        return entityManager
                .createQuery("SELECT secret FROM VaultSecretRecord secret ORDER BY secret.name", VaultSecretRecord.class)
                .getResultList()
                .stream()
                .map(JpaVaultSource::entryOf)
                .toList();
    }

    @Override
    public String locate(SecretName name) {
        return "vault_secrets";
    }

    @Override
    public SecretEntry put(SecretName name, SecretValue value, String description, String actor) {
        VaultSecretRecord record  = entityManager.find(VaultSecretRecord.class, name.value());
        boolean           created = record == null;
        String            sealed  = cipher.seal(value.reveal(), contextOf(name));

        if (created) {
            record = new VaultSecretRecord(name.value(), UtcTime.now(clock));
        }

        record.seal(sealed, keyIdentifierOf(sealed), value.fingerprint(), UtcTime.now(clock), actor);

        if (description != null) {
            record.describe(description, UtcTime.now(clock), actor);
        }

        if (created) {
            entityManager.persist(record);
        }

        return entryOf(record);
    }

    @Override
    public boolean describe(SecretName name, String description, String actor) {
        VaultSecretRecord record = entityManager.find(VaultSecretRecord.class, name.value());

        if (record == null) {
            return false;
        }

        record.describe(description, UtcTime.now(clock), actor);

        return true;
    }

    @Override
    public boolean remove(SecretName name) {
        VaultSecretRecord record = entityManager.find(VaultSecretRecord.class, name.value());

        if (record == null) {
            return false;
        }

        entityManager.remove(record);

        return true;
    }

    /** 🔁 Reseals every row not under the active key; returns how many. Run inside a transaction. */
    public int resealOutdated() {
        int resealed = 0;

        for (VaultSecretRecord record : entityManager
                .createQuery("SELECT secret FROM VaultSecretRecord secret", VaultSecretRecord.class)
                .getResultList()) {
            if (cipher.isCurrent(record.getSealedValue())) {
                continue;
            }

            String context = CONTEXT_PREFIX + record.getName();
            String sealed  = cipher.seal(cipher.open(record.getSealedValue(), context), context);

            record.reseal(sealed, keyIdentifierOf(sealed));
            resealed++;
        }

        return resealed;
    }

    /** 🔑 The key identifiers rows are sealed under, for the start-up check. */
    public List<String> keyIdentifiersInUse() {
        return entityManager
                .createQuery("SELECT DISTINCT secret.keyIdentifier FROM VaultSecretRecord secret", String.class)
                .getResultList();
    }

    /** A cipher that records no key (a key-management service) still needs a value in the NOT NULL column. */
    private String keyIdentifierOf(String sealed) {
        return Objects.requireNonNullElse(cipher.keyIdentifierOf(sealed), EXTERNAL_KEY);
    }

    private static String contextOf(SecretName name) {
        return CONTEXT_PREFIX + name.value();
    }

    private static SecretEntry entryOf(VaultSecretRecord record) {
        return new SecretEntry(SecretName.of(record.getName()), NAME, record.getFingerprint(), record.getDescription(),
                UtcTime.toInstant(record.getUpdatedAt()), record.getUpdatedBy(), "vault_secrets");
    }
}
