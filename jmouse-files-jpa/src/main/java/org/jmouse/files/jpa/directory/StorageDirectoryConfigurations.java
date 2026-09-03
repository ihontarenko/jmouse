package org.jmouse.files.jpa.directory;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.jmouse.files.directory.DirectoryConfigurationKind;
import org.jmouse.files.directory.DirectoryConfigurationKinds;
import org.jmouse.files.exception.DirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 🔧 Reading and writing what a folder says about itself.
 *
 * <p>Two reads matter, and the second is why this class exists rather than a repository method: one
 * kind for one directory, and <strong>one kind across a list of directories in a single query</strong>.
 * Inheritance walks a whole ancestor chain on every upload, and doing that a row at a time is the N+1
 * that turns a cache from a nicety into the only thing holding the feature up.</p>
 *
 * <p>Transaction demarcation is the caller's, as everywhere else in this module.</p>
 */
public class StorageDirectoryConfigurations {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageDirectoryConfigurations.class);

    private final EntityManager                entityManager;
    private final Supplier<String>             identifiers;
    private final DirectoryConfigurationKinds  kinds;
    private final ObjectMapper                 documents;

    /**
     * 🏗️ Build the store over the application's persistence context.
     *
     * @param entityManager the persistence context
     * @param identifiers   where a new row's identifier comes from
     * @param kinds         what kinds this installation knows about
     */
    public StorageDirectoryConfigurations(EntityManager entityManager, Supplier<String> identifiers,
                                          DirectoryConfigurationKinds kinds) {
        this(entityManager, identifiers, kinds, new ObjectMapper());
    }

    /**
     * 🏗️ Build the store over a caller-supplied document reader.
     *
     * @param entityManager the persistence context
     * @param identifiers   where a new row's identifier comes from
     * @param kinds         what kinds this installation knows about
     * @param documents     how a payload becomes its record and back
     */
    public StorageDirectoryConfigurations(EntityManager entityManager, Supplier<String> identifiers,
                                          DirectoryConfigurationKinds kinds, ObjectMapper documents) {
        this.entityManager = entityManager;
        this.identifiers   = identifiers;
        this.kinds         = kinds;
        this.documents     = documents;
    }

    /**
     * 🔎 What this folder itself says, of this kind — never what it inherits.
     *
     * @param directoryId the folder
     * @param kind        which question
     * @param <T>         the kind's payload type
     * @return the configuration, or empty when the folder carries none of its own
     */
    public <T> Optional<T> find(String directoryId, DirectoryConfigurationKind<T> kind) {
        return row(directoryId, kind.name()).map(stored -> read(stored, kind));
    }

    /**
     * 🔎 Which of these folders carry a configuration of this kind, in ONE query.
     *
     * <p>⚠️ What inheritance is built on. A caller resolving a rule holds a whole ancestor chain and
     * needs to know which link in it answers — asking per link is a query per level of the tree, on a
     * path that runs on every upload.</p>
     *
     * @param directoryIds the folders to ask about
     * @param kind         which question
     * @param <T>          the kind's payload type
     * @return the configurations found, keyed by directory; folders carrying none are absent
     */
    public <T> Map<String, T> findAll(Collection<String> directoryIds,
                                      DirectoryConfigurationKind<T> kind) {
        if (directoryIds == null || directoryIds.isEmpty()) {
            return Map.of();
        }

        List<StorageDirectoryConfiguration> found = entityManager.createQuery(
                "SELECT configuration FROM StorageDirectoryConfiguration configuration "
                + "WHERE configuration.kind = :kind AND configuration.directoryId IN :directoryIds",
                StorageDirectoryConfiguration.class)
            .setParameter("kind", kind.name())
            .setParameter("directoryIds", directoryIds)
            .getResultList();

        Map<String, T> configurations = new LinkedHashMap<>();

        for (StorageDirectoryConfiguration stored : found) {
            configurations.put(stored.getDirectoryId(), read(stored, kind));
        }

        return configurations;
    }

    /**
     * 📋 Everything this folder itself says, whatever the kind.
     *
     * @param directoryId the folder
     * @return the kinds it carries a row for
     */
    public List<String> kindsOf(String directoryId) {
        return entityManager.createQuery(
                "SELECT configuration.kind FROM StorageDirectoryConfiguration configuration "
                + "WHERE configuration.directoryId = :directoryId ORDER BY configuration.kind",
                String.class)
            .setParameter("directoryId", directoryId)
            .getResultList();
    }

    /**
     * ✏️ Say what this folder does, of one kind — replacing whatever it said before.
     *
     * <p>⚠️ The kind has to be registered. An unknown one is refused here rather than stored, because a
     * row nothing can bind is a row nothing will ever read, and a table of those is a junk drawer.</p>
     *
     * @param directoryId the folder
     * @param kind        which question
     * @param payload     the answer
     * @param <T>         the kind's payload type
     */
    public <T> void write(String directoryId, DirectoryConfigurationKind<T> kind, T payload) {
        kinds.require(kind.name());

        if (payload == null) {
            throw new DirectoryException(
                "A '%s' configuration needs a payload — clear the row to go back to inheriting."
                    .formatted(kind.name()));
        }

        String document = write(payload, kind);

        row(directoryId, kind.name()).ifPresentOrElse(
            stored -> stored.setPayload(document),
            () -> entityManager.persist(new StorageDirectoryConfiguration(
                    identifiers.get(), directoryId, kind.name(), document)));

        LOGGER.debug("🔧 Directory '{}' now carries a '{}' configuration", directoryId, kind.name());
    }

    /**
     * 🧹 Stop saying anything of this kind, and go back to inheriting.
     *
     * <p>⚠️ Reachable on purpose. A configuration that can be set and not removed is a one-way door on
     * every folder — and "cleared" here is genuinely <em>no row</em>, which is the state a resolver
     * reads as "ask my parent".</p>
     *
     * @param directoryId the folder
     * @param kind        which question
     * @return whether there was anything to clear
     */
    public boolean clear(String directoryId, DirectoryConfigurationKind<?> kind) {
        Optional<StorageDirectoryConfiguration> stored = row(directoryId, kind.name());

        stored.ifPresent(entityManager::remove);

        return stored.isPresent();
    }

    /**
     * 🗑️ Drop everything these folders say.
     *
     * <p>⚠️ Called when a subtree is deleted, and it takes the <strong>whole</strong> subtree's rows
     * rather than only the named folder's. A row keyed to a folder that no longer exists is invisible
     * until an identifier is reused, which is the worst moment to discover it.</p>
     *
     * @param directoryIds the folders going away
     * @return how many rows were removed
     */
    public int deleteAll(Collection<String> directoryIds) {
        if (directoryIds == null || directoryIds.isEmpty()) {
            return 0;
        }

        return entityManager.createQuery(
                "DELETE FROM StorageDirectoryConfiguration configuration "
                + "WHERE configuration.directoryId IN :directoryIds")
            .setParameter("directoryIds", directoryIds)
            .executeUpdate();
    }

    /**
     * 🧩 A document as it arrived, bound into the record its kind declares.
     *
     * <p>⚠️ Here rather than at the route, so every way in gets the same typing — a product writing a
     * configuration from its own code is validated exactly as an HTTP caller is. A document that will
     * not bind is a refusal, never a row that explodes at somebody's next upload.</p>
     *
     * @param kind     which question the document answers
     * @param document the document, typically a map straight off a request body
     * @param <T>      the kind's payload type
     * @return the bound record
     */
    public <T> T bind(DirectoryConfigurationKind<T> kind, Object document) {
        if (document == null) {
            throw new DirectoryException(
                "A '%s' configuration needs a document — clear the row to go back to inheriting."
                    .formatted(kind.name()));
        }

        try {
            return documents.convertValue(document, kind.payloadType());
        } catch (RuntimeException unbindable) {
            throw new DirectoryException(
                "That is not a valid '%s' configuration: %s"
                    .formatted(kind.name(), rootCauseOf(unbindable)), unbindable);
        }
    }

    /**
     * 🔎 The sentence that actually says which field was wrong.
     */
    private static String rootCauseOf(Throwable failure) {
        Throwable deepest = failure;

        while (deepest.getCause() != null) {
            deepest = deepest.getCause();
        }

        return deepest.getMessage();
    }

    private Optional<StorageDirectoryConfiguration> row(String directoryId, String kind) {
        return entityManager.createQuery(
                "SELECT configuration FROM StorageDirectoryConfiguration configuration "
                + "WHERE configuration.directoryId = :directoryId AND configuration.kind = :kind",
                StorageDirectoryConfiguration.class)
            .setParameter("directoryId", directoryId)
            .setParameter("kind", kind)
            .getResultStream()
            .findFirst();
    }

    /**
     * 📖 A stored document as the record its kind declares.
     *
     * <p>A payload that will not bind is a refusal naming the folder and the kind — it is a row somebody
     * wrote by hand or a kind whose record has changed shape, and either way silently returning the
     * installation's rule instead would hide it forever.</p>
     */
    private <T> T read(StorageDirectoryConfiguration stored, DirectoryConfigurationKind<T> kind) {
        try {
            return documents.readValue(stored.getPayload(), kind.payloadType());
        } catch (Exception unreadable) {
            throw new DirectoryException(
                "The '%s' configuration on directory '%s' cannot be read as %s: %s"
                    .formatted(kind.name(), stored.getDirectoryId(),
                               kind.payloadType().getSimpleName(), unreadable.getMessage()),
                unreadable);
        }
    }

    private <T> String write(T payload, DirectoryConfigurationKind<T> kind) {
        try {
            return documents.writeValueAsString(payload);
        } catch (Exception unwritable) {
            throw new DirectoryException(
                "A '%s' configuration could not be written as a document: %s"
                    .formatted(kind.name(), unwritable.getMessage()), unwritable);
        }
    }
}
