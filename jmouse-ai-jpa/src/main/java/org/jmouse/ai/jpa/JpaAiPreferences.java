package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.ai.jpa.entity.AiPreference;
import org.jmouse.ai.preferences.AiPreferences;
import org.jmouse.ai.preferences.PreferenceCatalog;
import org.jmouse.ai.preferences.PreferenceDefinition;
import org.jmouse.ai.preferences.PreferenceVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The stored wordings of every declared setting, seeded from what the product ships.
 *
 * <p><strong>The catalogue decides what exists and the table decides what it says.</strong> A row
 * naming a setting nothing declared is refused on the way in and ignored on the way out, so a row left
 * behind by a setting the product has since dropped is inert rather than a value nobody can explain.
 *
 * <h2>Seeding, and why it is here rather than in a migration</h2>
 *
 * <p>⚠️ A library migration cannot know what an application's assistant should say. The wordings are the
 * product's, in code, so the seed is too: the first read of a setting with <strong>no rows at all</strong>
 * writes one row per shipped variant and puts the declared default in force.
 *
 * <p>Three consequences worth stating. A fresh database fills itself, so nobody runs anything. An
 * <em>edited</em> row is never overwritten, because seeding only ever happens into emptiness. And
 * deleting every row for a setting is how somebody asks for the shipped set back — the next read
 * rebuilds it, which is a predictable rule and cheaper than a ledger table nobody would remember.
 *
 * <p>⚠️ <strong>Every query is filtered by application</strong>, for the reason
 * {@link JpaProviderAdministration} states: one installation's table may serve several applications, and
 * the name is fixed at construction rather than travelling in a request.
 */
public final class JpaAiPreferences implements AiPreferences {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaAiPreferences.class);

    private final EntityManagerFactory entityManagerFactory;
    private final String               application;
    private final PreferenceCatalog    catalog;

    public JpaAiPreferences(
            EntityManagerFactory entityManagerFactory,
            String               application,
            PreferenceCatalog    catalog) {

        this.entityManagerFactory = entityManagerFactory;
        this.application          = application;
        this.catalog              = catalog;
    }

    /**
     * ⚠️ <strong>Falls back twice, and both fallbacks are the same promise.</strong> A setting with no
     * rows is seeded and answered from the declaration; a setting with rows but none in force — somebody
     * took the last one out of force — answers from the declaration too. An assistant is never left with
     * no instructions because of a state somebody left the table in.
     */
    @Override
    public String value(String name) {
        PreferenceDefinition declared = catalog.require(name);

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            seedIfEmpty(entityManager, declared);

            return inForce(entityManager, name).map(AiPreference::getValue);
        }).orElseGet(declared::defaultValue);
    }

    @Override
    public List<Setting> all() {
        return catalog.declared().stream().map(this::settingOf).toList();
    }

    @Override
    public Setting find(String name) {
        return settingOf(catalog.require(name));
    }

    @Override
    public StoredValue add(String name, Draft draft) {
        PreferenceDefinition declared = catalog.require(name);

        requireText(draft);

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            seedIfEmpty(entityManager, declared);

            LocalDateTime now     = LocalDateTime.now();
            AiPreference  created = new AiPreference(
                    UUID.randomUUID().toString(),
                    application,
                    name,
                    requireFreeLabel(entityManager, name, null, draft.label()),
                    draft.value(),
                    // ⚠️ Idle, like a new provider configuration. Writing a prompt is not the same as
                    // deciding the assistant should start using it.
                    false,
                    null,
                    now,
                    now);

            entityManager.persist(created);

            return describe(declared, created);
        });
    }

    @Override
    public StoredValue change(String id, Draft draft) {
        requireText(draft);

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiPreference stored = require(entityManager, id);

            stored.setLabel(requireFreeLabel(entityManager, stored.getName(), id, draft.label()));
            stored.setValue(draft.value());
            stored.setUpdatedAt(LocalDateTime.now());

            return describe(catalog.require(stored.getName()), stored);
        });
    }

    @Override
    public StoredValue putInForce(String id) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiPreference target = require(entityManager, id);

            // Every other wording of THIS setting, and no other setting's — one prompt being in force
            // has nothing to say about what any other setting is set to.
            rowsOf(entityManager, target.getName()).stream()
                    .filter(AiPreference::isInForce)
                    .filter(other -> !other.getId().equals(id))
                    .forEach(other -> {
                        other.setInForce(false);
                        other.setUpdatedAt(LocalDateTime.now());
                    });

            target.setInForce(true);
            target.setUpdatedAt(LocalDateTime.now());

            return describe(catalog.require(target.getName()), target);
        });
    }

    @Override
    public StoredValue restore(String id) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiPreference         stored   = require(entityManager, id);
            PreferenceDefinition declared = catalog.require(stored.getName());

            PreferenceVariant shipped = Optional.ofNullable(stored.getSeedKey())
                    .flatMap(declared::variant)
                    .orElseThrow(() -> new RefusedException(
                            "'" + stored.getLabel() + "' is not one of the wordings this build ships, so "
                            + "there is nothing to put it back to. Only a seeded one can be restored — "
                            + "for anything else, the text here is the only copy."));

            stored.setValue(shipped.value());
            stored.setUpdatedAt(LocalDateTime.now());

            return describe(declared, stored);
        });
    }

    @Override
    public void discard(String id) {
        OwnTransaction.run(entityManagerFactory, entityManager -> {
            AiPreference stored = require(entityManager, id);

            if (stored.isInForce()) {
                throw new RefusedException(
                        "'" + stored.getLabel() + "' is the wording currently in force. Put another one "
                        + "in force before deleting it, so the assistant is never told nothing.");
            }

            entityManager.remove(stored);
        });
    }

    // ── Seeding ──────────────────────────────────────────────────────────────────

    /**
     * ⚠️ Into emptiness only. Anything else would be a restart rewriting somebody's edits, which is the
     * one thing that would make this table not worth having.
     */
    private void seedIfEmpty(EntityManager entityManager, PreferenceDefinition declared) {
        if (!rowsOf(entityManager, declared.name()).isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        declared.variants().forEach(variant -> entityManager.persist(new AiPreference(
                UUID.randomUUID().toString(),
                application,
                declared.name(),
                variant.label(),
                variant.value(),
                variant.key().equals(declared.defaultVariant()),
                variant.key(),
                now,
                now)));

        LOGGER.info("Seeded {} wording(s) for '{}' in {}, '{}' in force",
                declared.variants().size(), declared.name(), application, declared.defaultVariant());
    }

    // ── Reading ──────────────────────────────────────────────────────────────────

    private Setting settingOf(PreferenceDefinition declared) {
        List<AiPreference> rows = OwnTransaction.call(entityManagerFactory, entityManager -> {
            seedIfEmpty(entityManager, declared);

            return rowsOf(entityManager, declared.name());
        });

        return new Setting(
                declared.name(),
                declared.title(),
                declared.description(),
                declared.multiline(),
                rows.stream().map(row -> describe(declared, row)).toList());
    }

    private List<AiPreference> rowsOf(EntityManager entityManager, String name) {
        return entityManager.createQuery("""
                        select preference
                          from AiPreference preference
                         where preference.application = :application
                           and preference.name        = :name
                         order by preference.createdAt asc
                        """, AiPreference.class)
                .setParameter("application", application)
                .setParameter("name", name)
                .getResultList();
    }

    private Optional<AiPreference> inForce(EntityManager entityManager, String name) {
        return rowsOf(entityManager, name).stream().filter(AiPreference::isInForce).findFirst();
    }

    /**
     * ⚠️ Always by identifier <em>and</em> application. A bare {@code find} over a table one installation
     * shares between applications is an endpoint that edits somebody else's row.
     */
    private AiPreference require(EntityManager entityManager, String id) {
        return entityManager.createQuery("""
                        select preference
                          from AiPreference preference
                         where preference.id          = :id
                           and preference.application = :application
                        """, AiPreference.class)
                .setParameter("id", id)
                .setParameter("application", application)
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RefusedException(
                        "No stored AI preference of this application has the identifier '" + id + "'."));
    }

    /**
     * ⚠️ Checked here rather than left to the unique constraint. Two wordings a screen cannot tell apart
     * is the problem; a constraint violation surfacing as an unexplained failure at commit is a second
     * one, and this way the refusal says which name is taken.
     */
    private String requireFreeLabel(
            EntityManager entityManager, String name, String exceptId, String label) {

        String named = label == null ? "" : label.trim();

        if (named.isEmpty()) {
            throw new RefusedException(
                    "Give this wording a name — it is how it is told apart from the others on a screen.");
        }

        boolean taken = rowsOf(entityManager, name).stream()
                .filter(other -> !other.getId().equals(exceptId))
                .anyMatch(other -> other.getLabel().equalsIgnoreCase(named));

        if (taken) {
            throw new RefusedException(
                    "Another wording of this setting is already called '" + named + "'. Two rows under "
                    + "one name are two rows nobody can tell apart.");
        }

        return named;
    }

    private static void requireText(Draft draft) {
        if (draft == null || draft.value() == null) {
            throw new RefusedException(
                    "A wording with no text would be an assistant told nothing the moment it was put in "
                    + "force.");
        }
    }

    /**
     * ⚠️ The stored times are zoneless columns written from {@link LocalDateTime#now()}, so the zone they
     * were written in is the one running this — the same reading every other row in this module takes.
     */
    private static StoredValue describe(PreferenceDefinition declared, AiPreference stored) {
        boolean asShipped = declared.variant(stored.getSeedKey())
                .map(shipped -> shipped.value().equals(stored.getValue()))
                .orElse(false);

        return new StoredValue(
                stored.getId(),
                stored.getLabel(),
                stored.getValue(),
                stored.isInForce(),
                stored.getSeedKey(),
                asShipped,
                stored.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                stored.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant());
    }
}
