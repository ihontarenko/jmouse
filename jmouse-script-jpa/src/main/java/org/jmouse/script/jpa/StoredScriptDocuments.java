package org.jmouse.script.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.script.ScriptBinding;
import org.jmouse.script.ScriptDocumentBinder;
import org.jmouse.script.el.host.BoundScript;
import org.jmouse.script.spi.ScriptDocuments;
import org.jmouse.script.spi.ScriptStage;
import org.jmouse.script.stage.StageRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every rule an installation holds, bound and indexed by the moment it runs at.
 *
 * <h2>⚠️ THE INDEX IS REBUILT, NEVER QUERIED</h2>
 *
 * <p>{@link #forStage} is on the dispatch path — a per-item stage asks it once per item in a walk of
 * thousands. A query there would put a round trip in front of every item. So the whole set is bound once
 * and held, and {@link #reload()} replaces it after a save.</p>
 *
 * <h2>⚠️ THE PRODUCT CALLS {@link #reload()}; NOTHING HERE LISTENS FOR STARTUP</h2>
 *
 * <p>Where this came from it was annotated to run when the container announced it was ready, and to run
 * inside a transaction. Both are the product's to arrange: this library knows what rebuilding means, not
 * how an application says it has started.</p>
 *
 * <p>⚠️ It must run in a transaction, because it WRITES — a document whose text no longer binds has its
 * state recorded so the screen can say so.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class StoredScriptDocuments implements ScriptDocuments {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoredScriptDocuments.class);

    /**
     * ⚠️ Fetch-joined rather than lazily walked. This rebuilds the whole index, so a lazy collection
     * would be one query per document — the classic N+1, on a path that runs at startup and after every
     * save.
     */
    private static final String EVERY_DOCUMENT = """
            SELECT DISTINCT document FROM ScriptDocument document
            LEFT JOIN FETCH document.assignments
            ORDER BY document.sortOrder ASC, document.name ASC
            """;

    private final EntityManager        entityManager;
    private final ScriptDocumentBinder binder;
    private final StageRegistry        stages;

    /**
     * ⚠️ {@code volatile}, and it matters in the ordinary lazy-field way plus one more: a walk runs on
     * an executor thread while a save happens on a request thread, and the walk must see the new index
     * rather than a half-built one.
     */
    /**
     * The index: stage → tenant → the rules that run there, in the store's own order.
     *
     * <h2>⚠️ TWO LEVELS RATHER THAN A COMPOSITE KEY, BECAUSE EVERY LOOKUP NEEDS BOTH GROUPS</h2>
     *
     * <p>A dispatch answers with the installation's rules <em>and</em> the tenant's, so a map keyed on
     * a {@code (scope, stage)} pair would be two hash lookups and a merge on the per-item path anyway.
     * Nested, the stage is resolved once and the two groups are read out of the same small map.
     *
     * <p>⚠️ <strong>The installation's rules are held under {@link #INSTALLATION}</strong> — a key that
     * cannot collide with a tenant's, because a map does not take a null key and because a product
     * choosing the literal string "" as a tenant id has bigger problems.
     */
    private volatile Map<String, Map<String, List<BoundScript>>> byStage = Map.of();

    /**
     * Where a rule with no scope is filed.
     *
     * <p>⚠️ Not null: {@code Map.of()} refuses a null key outright, and an index that threw while being
     * rebuilt would leave the previous one in place with nothing saying so.
     */
    private static final String INSTALLATION = "";

    public StoredScriptDocuments(EntityManager entityManager, ScriptDocumentBinder binder,
                                 StageRegistry stages) {
        this.entityManager = entityManager;
        this.binder = binder;
        this.stages = stages;
    }

    /**
     * ⚠️ <strong>The installation's rules first, then the tenant's</strong> — see
     * {@code ScriptDocuments#forStage}, where both halves of that are argued.
     *
     * <p>⚠️ The common answer is one of the two groups being empty, so the list is only ever copied
     * when both are populated. This is the per-item path.
     */
    @Override
    public List<BoundScript> forStage(String scope, String stage) {
        Map<String, List<BoundScript>> atStage = byStage.get(stage);

        if (atStage == null) {
            return List.of();
        }

        List<BoundScript> everywhere = atStage.getOrDefault(INSTALLATION, List.of());
        List<BoundScript> here = scope == null
                ? List.of()
                : atStage.getOrDefault(scope, List.of());

        if (here.isEmpty()) {
            return everywhere;
        }

        if (everywhere.isEmpty()) {
            return here;
        }

        List<BoundScript> both = new ArrayList<>(everywhere.size() + here.size());

        both.addAll(everywhere);
        both.addAll(here);

        return both;
    }

    /**
     * Binds every rule and replaces the index.
     *
     * <p>⚠️ Call this inside a transaction — it records each document's bind state.</p>
     *
     * @return how many rules are runnable
     */
    public int reload() {
        Map<String, Map<String, List<BoundScript>>> rebuilt = new LinkedHashMap<>();

        int runnable = 0;

        for (ScriptDocument document : entityManager.createQuery(EVERY_DOCUMENT, ScriptDocument.class)
                .getResultList()) {
            /*
              ⚠️ A DISABLED DOCUMENT IS STILL BOUND-CHECKED, and only its INDEXING is skipped.

              Skipping it altogether was the first shape and it produced a screen that lies: a rule that
              is off keeps whatever bind state it last had, so an example seeded with a typo — or a rule
              switched off months ago, under a build that has since withdrawn a facade — shows as healthy
              until somebody happens to press Save on it. "Off" says nothing about whether the text is
              valid, and the screen must not imply that it does.
             */
            boolean bound = bindInto(rebuilt, document, document.isEnabled());

            if (bound && document.isEnabled()) {
                runnable++;
            }
        }

        byStage = deepCopyOf(rebuilt);

        LOGGER.info("jMS rules loaded: {} runnable across {} stage(s) in {} tenant(s)", runnable, byStage.size(),
                    byStage.values().stream().flatMap(scopes -> scopes.keySet().stream()).distinct().count());

        return runnable;
    }

    /**
     * Binds one document once per stage it is assigned to, and records what the host said.
     *
     * @param rebuilt  the index being built
     * @param document the rule
     * @param index    false for a disabled document: its text is still checked, so the screen tells the
     *                 truth about it, and nothing runs
     * @return whether it bound and has somewhere to run
     */
    private boolean bindInto(Map<String, Map<String, List<BoundScript>>> rebuilt, ScriptDocument document,
                             boolean index) {
        List<BoundScript> bound      = new ArrayList<>();
        List<String>      placements = new ArrayList<>();
        ScriptBinding     refusal    = null;

        for (String assigned : document.stages()) {
            ScriptStage<?> stage = stages.stageNamed(assigned);

            if (stage == null) {
                // ⚠️ An assignment naming a stage this build no longer declares. NOT an error and NOT a
                // reason to refuse the document: a stage that was renamed or withdrawn is the build's
                // change, not the author's, and the screen shows it as unknown so somebody can decide
                // what they meant. The rest of the document still runs.
                LOGGER.warn("rule '{}' is assigned to '{}', which this build does not declare",
                            document.getName(), assigned);

                continue;
            }

            ScriptBinding binding = binder.bind(document.getName(), document.getSource(), stage);

            if (!binding.wasAccepted()) {
                refusal = binding;

                break;
            }

            bound.add(binding.script());
            placements.add(assigned);
        }

        if (refusal != null) {
            document.refused("%s: %s".formatted(refusal.stage(), refusal.problem()));
            entityManager.merge(document);

            LOGGER.warn("rule '{}' did not load — {}", document.getName(), refusal.problem());

            return false;
        }

        document.bound();
        entityManager.merge(document);

        if (index) {
            /* ⚠️ A rule with no scope is filed under INSTALLATION rather than under a null key: a map
               does not take one, and the index is rebuilt inside a transaction where throwing would
               leave the previous index in place with nothing saying so. */
            String tenant = document.getScope() == null ? INSTALLATION : document.getScope();

            for (int at = 0; at < placements.size(); at++) {
                rebuilt.computeIfAbsent(placements.get(at), stage -> new LinkedHashMap<>())
                        .computeIfAbsent(tenant, scope -> new ArrayList<>())
                        .add(bound.get(at));
            }
        }

        return !placements.isEmpty();
    }


    /**
     * Freezes the rebuilt index, both levels of it.
     *
     * <p>⚠️ <strong>{@code Map.copyOf} is shallow, and a shallow copy here is not a copy.</strong> The
     * inner maps and their lists would still be the mutable ones the rebuild was filling, so the next
     * reload would be mutating the index a dispatch is reading — on the per-item path, from another
     * thread, with no lock. It would work for a long time and then not.
     */
    private static Map<String, Map<String, List<BoundScript>>> deepCopyOf(
            Map<String, Map<String, List<BoundScript>>> rebuilt) {

        Map<String, Map<String, List<BoundScript>>> frozen = new LinkedHashMap<>();

        rebuilt.forEach((stage, byScope) -> {
            Map<String, List<BoundScript>> scopes = new LinkedHashMap<>();

            byScope.forEach((scope, scripts) -> scopes.put(scope, List.copyOf(scripts)));
            frozen.put(stage, Map.copyOf(scopes));
        });

        return Map.copyOf(frozen);
    }
}
