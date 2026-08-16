package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.ai.administration.ProviderAdministration;
import org.jmouse.ai.jpa.entity.AiProviderSettings;
import org.jmouse.ai.provider.ProviderCatalog;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The provider configuration, administered as the rows {@link JpaProviderSettingsSource} reads.
 *
 * <p><strong>Here rather than in each product, because the rules are this module's own.</strong>
 * "Exactly one row is in force" is not a preference somebody adopted — it is the condition the settings
 * source refuses to resolve without, loudly, at the moment somebody asks a question. Leaving the rule to
 * whoever wrote the screen meant every adopter re-derived it, and the one that got it wrong found out
 * through an application that had stopped answering.
 *
 * <p>⚠️ <strong>Every query is filtered by application.</strong> One installation's table can serve
 * several, so a screen must never be able to activate, change or delete somebody else's row. The
 * application name is fixed at construction and has no business travelling in a request — putting it
 * there is precisely what stopped the gateway this design was learned from being usable as a library.
 *
 * <p>⚠️ <strong>The key is write-only.</strong> {@link Configuration} has no field for one, and a draft
 * carrying a blank key keeps whatever is stored — see {@link Draft}.
 */
public final class JpaProviderAdministration implements ProviderAdministration {

    private final EntityManagerFactory entityManagerFactory;
    private final String               application;

    public JpaProviderAdministration(EntityManagerFactory entityManagerFactory, String application) {
        this.entityManagerFactory = entityManagerFactory;
        this.application          = application;
    }

    @Override
    public List<String> supportedProviders() {
        return ProviderCatalog.shipped();
    }

    /** The same list with what a screen needs, in the catalogue's own order rather than alphabetically. */
    @Override
    public List<SupportedProvider> describeSupportedProviders() {
        return ProviderCatalog.describeShipped().stream()
                .map(shipped -> new SupportedProvider(
                        shipped.name(), shipped.defaultApiUrl(), shipped.requiresKey(), shipped.note()))
                .toList();
    }

    @Override
    public List<Configuration> configurations() {
        return OwnTransaction.call(entityManagerFactory, entityManager -> entityManager.createQuery("""
                                select configured
                                  from AiProviderSettings configured
                                 where configured.application = :application
                                 order by configured.createdAt asc
                                """, AiProviderSettings.class)
                        .setParameter("application", application)
                        .getResultList())
                .stream()
                .map(JpaProviderAdministration::describe)
                .toList();
    }

    @Override
    public Optional<Configuration> find(String id) {
        return OwnTransaction.call(entityManagerFactory,
                entityManager -> row(entityManager, id).map(JpaProviderAdministration::describe));
    }

    @Override
    public Configuration add(Draft draft) {
        String        provider = requireSupported(draft.provider());
        LocalDateTime now      = LocalDateTime.now();

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiProviderSettings created = new AiProviderSettings(
                    UUID.randomUUID().toString(),
                    application,
                    provider,
                    blankToNull(draft.apiKey()),
                    blankToNull(draft.apiUrl()),
                    draft.model(),
                    draft.maximumTokens(),
                    // Idle. Typing a key is not saying "start spending money through this one".
                    false,
                    now,
                    now);

            entityManager.persist(created);

            return describe(created);
        });
    }

    @Override
    public Configuration change(String id, Draft draft) {
        String provider = requireSupported(draft.provider());

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiProviderSettings configured = require(entityManager, id);

            configured.setProvider(provider);
            configured.setModel(draft.model());
            configured.setApiUrl(blankToNull(draft.apiUrl()));
            configured.setMaximumTokens(draft.maximumTokens());
            configured.setUpdatedAt(LocalDateTime.now());

            // Blank means "leave it", never "clear it" — see Draft.
            if (draft.carriesKey()) {
                configured.setApiKey(draft.apiKey());
            }

            return describe(configured);
        });
    }

    @Override
    public Configuration putInForce(String id) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiProviderSettings target = require(entityManager, id);

            // ⚠️ Asked of the provider rather than demanded of every configuration. A model running on
            // this machine has no credential to give, and refusing it for not having one would make the
            // only free-in-every-sense option the one option that cannot be switched on.
            if (ProviderCatalog.requiresKey(target.getProvider()) && !hasKey(target)) {
                throw new RefusedException(
                        "This configuration has no key, so every call through it would be refused before "
                        + "it was sent. Add the key first, then put it in force.");
            }

            // ⚠️ Not a tidy-up afterwards — it is the other half of this operation. Two active rows is
            // the one state the settings source cannot resolve.
            entityManager.createQuery("""
                            select configured
                              from AiProviderSettings configured
                             where configured.application = :application
                               and configured.active      = true
                            """, AiProviderSettings.class)
                    .setParameter("application", application)
                    .getResultList()
                    .stream()
                    .filter(inForce -> !inForce.getId().equals(id))
                    .forEach(JpaProviderAdministration::deactivate);

            target.setActive(true);
            target.setUpdatedAt(LocalDateTime.now());

            return describe(target);
        });
    }

    @Override
    public Configuration takeOutOfForce(String id) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiProviderSettings configured = require(entityManager, id);

            deactivate(configured);

            return describe(configured);
        });
    }

    @Override
    public void discard(String id) {
        OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiProviderSettings configured = require(entityManager, id);

            if (configured.isActive()) {
                throw new RefusedException(
                        "This is the configuration currently in force. Take it out of force, or put "
                        + "another one in force, before deleting it.");
            }

            entityManager.remove(configured);

            return null;
        });
    }

    private String requireSupported(String provider) {
        String named = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);

        if (!ProviderCatalog.ships(named)) {
            throw new RefusedException(
                    "'" + provider + "' is not a provider this application can speak to. Choose one of: "
                    + String.join(", ", supportedProviders()) + ".");
        }

        return named;
    }

    /**
     * ⚠️ Always by identifier <em>and</em> application. A bare {@code find} over a table one installation
     * shares between applications is an endpoint that edits somebody else's row.
     */
    private AiProviderSettings require(EntityManager entityManager, String id) {
        return row(entityManager, id).orElseThrow(() -> new RefusedException(
                "No AI provider configuration of this application has the identifier '" + id + "'."));
    }

    private Optional<AiProviderSettings> row(EntityManager entityManager, String id) {
        return entityManager.createQuery("""
                        select configured
                          from AiProviderSettings configured
                         where configured.id          = :id
                           and configured.application = :application
                        """, AiProviderSettings.class)
                .setParameter("id", id)
                .setParameter("application", application)
                .getResultList()
                .stream()
                .findFirst();
    }

    private static void deactivate(AiProviderSettings configured) {
        configured.setActive(false);
        configured.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Whether a credential is stored — the only question about it anything outside this class may ask.
     */
    private static boolean hasKey(AiProviderSettings configured) {
        return configured.getApiKey() != null && !configured.getApiKey().isBlank();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * ⚠️ The stored times are zoneless columns written from {@link LocalDateTime#now()}, so the zone they
     * were written in is the one running this — the same reading {@link JpaCallCounter} takes.
     */
    private static Configuration describe(AiProviderSettings configured) {
        return new Configuration(
                configured.getId(),
                configured.getProvider(),
                configured.getModel(),
                configured.getApiUrl(),
                configured.getMaximumTokens(),
                configured.isActive(),
                hasKey(configured),
                configured.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                configured.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant());
    }
}
