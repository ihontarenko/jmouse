package org.jmouse.storage.jpa.sweeper;

import org.jmouse.storage.FileStores;
import org.jmouse.storage.configuration.SweeperSettings;
import org.jmouse.storage.jpa.StoredFileRegistry;
import org.jmouse.storage.jpa.StoredFileReferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 🧹 Reclaims objects nothing points at any more.
 *
 * <p>Storage leaked because the only record an object existed was the row referencing it. Once the
 * registry knows independently, the missing half is something that compares what exists against
 * what is still referenced — which is this.</p>
 *
 * <h3>The two rules that matter</h3>
 *
 * <p><strong>References are asked for, never counted.</strong> Every
 * {@link StoredFileReferences} source is queried on every sweep and the union is the answer. A
 * maintained counter was the obvious alternative and is worse than nothing: it drifts the moment a
 * cascade, a hand-written statement or a rolled-back transaction removes a row behind its back, and
 * a drifted counter either leaks forever or destroys a file somebody still holds.</p>
 *
 * <p><strong>The grace period is load-bearing.</strong> An object written seconds ago by a
 * transaction that has not committed yet is referenced by nothing that a query can see. Only rows
 * registered before {@code now - gracePeriod} are ever candidates, which is why the default is
 * generous rather than tight.</p>
 */
public class OrphanSweeper {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanSweeper.class);

    /**
     * 📦 Rows one batch examines, when the caller expresses no preference.
     */
    public static final int DEFAULT_BATCH_SIZE = 500;

    private final StoredFileRegistry               registry;
    private final FileStores                       fileStores;
    private final Collection<StoredFileReferences> referenceSources;
    private final SweeperSettings                  settings;
    private final int                              batchSize;

    /**
     * 🏗️ Build a sweeper over every reference source the application registered.
     *
     * @param registry         registry to sweep
     * @param fileStores       every backend, so each orphan is reclaimed through the one holding it
     * @param referenceSources one per table that points at the registry
     * @param settings         whether the sweeper runs, and how long an object is left alone
     */
    public OrphanSweeper(StoredFileRegistry registry, FileStores fileStores,
                         Collection<StoredFileReferences> referenceSources, SweeperSettings settings) {
        this(registry, fileStores, referenceSources, settings, DEFAULT_BATCH_SIZE);
    }

    /**
     * 🏗️ Build a sweeper with an explicit batch size.
     *
     * @param registry         registry to sweep
     * @param fileStores       every backend, so each orphan is reclaimed through the one holding it
     * @param referenceSources one per table that points at the registry
     * @param settings         whether the sweeper runs, and how long an object is left alone
     * @param batchSize        how many rows one batch examines
     */
    public OrphanSweeper(StoredFileRegistry registry, FileStores fileStores,
                         Collection<StoredFileReferences> referenceSources, SweeperSettings settings,
                         int batchSize) {
        this.registry         = registry;
        this.fileStores       = fileStores;
        this.referenceSources = List.copyOf(referenceSources);
        this.settings         = settings;
        this.batchSize        = batchSize;
    }

    /**
     * ▶️ Take the reference union and the cut-off, and hand back a run to pump.
     *
     * <p>Collecting the union first — before a single row is examined — is what makes the sweep
     * safe: a reference added while the sweep is walking belongs to an object that is either
     * already in the union or younger than the grace period, and so is not a candidate either way.</p>
     *
     * <h3>⚠️ Refused when there are no sources at all</h3>
     *
     * <p>An empty union means every object in the registry is an orphan, and a sweep that reclaims
     * everything is never what somebody meant — it is what an application looks like when its
     * reference sources failed to appear. Answering "nothing is referenced" and deleting the lot is
     * the one failure this whole mechanism exists to prevent, so it is refused instead of performed.</p>
     *
     * <p>Note that this is about having no <em>sources</em>, not about a source returning nothing: an
     * installation whose files are genuinely all unreferenced has sources that all answer empty, and
     * that sweep runs.</p>
     *
     * @return a run positioned at the first candidate
     * @throws IllegalStateException when no reference source is registered
     */
    public SweepRun begin() {
        if (referenceSources.isEmpty()) {
            throw new IllegalStateException(
                "Refusing to sweep: no stored-file reference sources are registered, so every stored "
                + "object would read as an orphan. Check that the persistence metamodel is available "
                + "and that at least one mapping points at StoredFile.");
        }

        Set<String> referenced = collectReferences();
        LocalDateTime cutOff = LocalDateTime.now().minus(settings.gracePeriod());

        LOGGER.info("🧹 Sweeping objects registered before {} — {} reference(s) across {} source(s)",
                    cutOff, referenced.size(), referenceSources.size());

        return new SweepRun(registry, fileStores, referenced, referenceSources.size(), cutOff,
                            batchSize, true);
    }

    /**
     * 👁️ The same walk, removing nothing — what a sweep WOULD reclaim.
     *
     * <p>⚠️ <strong>This is the call an operator should reach for first, and the sweeper existed for
     * three products without it.</strong> The sweep ships disabled everywhere, so the only way to learn
     * how much is leaking was to enable it — which is to say, to bet live bytes on every reference
     * source having been declared. A preview answers the same question and cannot delete anything.</p>
     *
     * <p>It is also cheap: nothing is asked of any backend, because the sizes come from the registry.</p>
     *
     * @return a run positioned at the first candidate, which will reclaim nothing
     */
    public SweepRun preview() {
        if (referenceSources.isEmpty()) {
            throw new IllegalStateException(
                "Refusing to preview a sweep: no stored-file reference sources are registered, so every "
                + "stored object would read as an orphan.");
        }

        Set<String> referenced = collectReferences();

        return new SweepRun(registry, fileStores, referenced, referenceSources.size(),
                            LocalDateTime.now().minus(settings.gracePeriod()), batchSize, false);
    }

    /**
     * 🧹 Sweep everything in one go, with no transaction demarcation of any kind.
     *
     * <p>The convenient form, for a scheduler that already runs the whole sweep inside one
     * transaction or inside none. An application that wants a transaction per batch drives
     * {@link #begin()} itself.</p>
     *
     * @return what the sweep did
     */
    public SweepReport sweep() {
        SweepRun run = begin();

        while (run.hasMore()) {
            run.sweepNextBatch();
        }

        SweepReport report = run.report();
        LOGGER.info("🧹 {}", report);

        return report;
    }

    /**
     * ⚙️ Whether the sweeper is configured to run at all.
     *
     * <p>Reclaiming bytes is a decision an operator makes, so the shipped default is off. A manual
     * trigger deliberately does not consult this — asking for a sweep is the decision.</p>
     *
     * @return {@code true} when scheduled sweeping is enabled
     */
    public boolean isEnabled() {
        return settings.enabled();
    }

    /**
     * 📤 The union of everything every source still points at.
     *
     * <p>A source that fails aborts the sweep rather than shrinking the union. Sweeping on a
     * partial answer would treat every object belonging to the failed source as an orphan and
     * delete all of them, which is the one mistake this component must never make.</p>
     *
     * @return the referenced identifiers
     */
    private Set<String> collectReferences() {
        Set<String> referenced = new HashSet<>();

        for (StoredFileReferences source : referenceSources) {
            try (Stream<String> identifiers = source.referencedIdentifiers()) {
                identifiers.forEach(referenced::add);
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                        "Reference source '%s' failed; refusing to sweep on a partial reference set"
                                .formatted(source.sourceName()), exception);
            }
        }

        return referenced;
    }
}
