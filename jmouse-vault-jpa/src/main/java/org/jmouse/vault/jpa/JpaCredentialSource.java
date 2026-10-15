package org.jmouse.vault.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.vault.SecretName;
import org.jmouse.vault.credential.CredentialDefinition;
import org.jmouse.vault.credential.WritableCredentialSource;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** 🗄️ Credential definitions in {@code vault_credentials}. Transactions belong to the caller. */
public final class JpaCredentialSource implements WritableCredentialSource {

    public static final String NAME = "database";

    private final EntityManager entityManager;
    private final Clock         clock;

    public JpaCredentialSource(EntityManager entityManager, Clock clock) {
        this.entityManager = Objects.requireNonNull(entityManager, "entity manager");
        this.clock         = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<CredentialDefinition> find(SecretName name) {
        return Optional.ofNullable(entityManager.find(VaultCredentialRecord.class, name.value()))
                .map(JpaCredentialSource::definitionOf);
    }

    @Override
    public List<CredentialDefinition> list() {
        return entityManager
                .createQuery("SELECT credential FROM VaultCredentialRecord credential ORDER BY credential.name",
                        VaultCredentialRecord.class)
                .getResultList()
                .stream()
                .map(JpaCredentialSource::definitionOf)
                .toList();
    }

    @Override
    public CredentialDefinition save(SecretName name, String kind, String description,
                                     Map<String, String> plainFields, Set<String> secretFields, String actor) {
        VaultCredentialRecord record  = entityManager.find(VaultCredentialRecord.class, name.value());
        boolean               created = record == null;

        if (created) {
            record = new VaultCredentialRecord(name.value(), UtcTime.now(clock));
        }

        record.define(kind, description, plainFields, secretFields, UtcTime.now(clock), actor);

        if (created) {
            entityManager.persist(record);
        }

        return definitionOf(record);
    }

    @Override
    public boolean remove(SecretName name) {
        VaultCredentialRecord record = entityManager.find(VaultCredentialRecord.class, name.value());

        if (record == null) {
            return false;
        }

        entityManager.remove(record);

        return true;
    }

    private static CredentialDefinition definitionOf(VaultCredentialRecord record) {
        return new CredentialDefinition(SecretName.of(record.getName()), record.getKind(), record.getDescription(),
                record.getPlainFields(), record.getSecretFields(), UtcTime.toInstant(record.getUpdatedAt()),
                record.getUpdatedBy(), NAME);
    }
}
