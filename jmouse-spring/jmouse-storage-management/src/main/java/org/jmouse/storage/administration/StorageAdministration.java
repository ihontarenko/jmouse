package org.jmouse.storage.administration;

import org.jmouse.storage.FileStores;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.configuration.UploadSettings;
import org.jmouse.storage.jpa.StoredFileReferences;
import org.jmouse.storage.jpa.StoredFileRegistry;
import org.jmouse.storage.jpa.sweeper.OrphanSweeper;
import org.jmouse.storage.jpa.sweeper.SweepReport;
import org.jmouse.storage.jpa.sweeper.SweepRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * 🗄️ What an operator can ask about storage, and the two things they can do to it.
 *
 * <h3>Why this exists at all</h3>
 *
 * <p>Every product on this library ships the sweeper <strong>disabled</strong>, and nothing anywhere
 * reports what it would do. So the honest state of every installation was: an unknown amount of storage
 * is leaking, the mechanism to reclaim it exists, and the only way to find out how much is to turn that
 * mechanism on. That is the wrong order — it bets live bytes on every reference source having been
 * declared — and it is why {@link #previewSweep()} is the point of this class and
 * {@link #sweep()} is the afterthought.</p>
 *
 * <p>{@code StorageDiagnostics} already knew most of the read side and wrote it to a log at startup,
 * once, where nobody looks at it again.</p>
 */
public class StorageAdministration {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageAdministration.class);

    private final StorageSettings                  settings;
    private final FileStores                       fileStores;
    private final StoredFileRegistry               registry;
    private final Collection<StoredFileReferences> referenceSources;
    private final OrphanSweeper                    sweeper;

    /**
     * 🏗️ Build the administration surface.
     *
     * @param settings         what this installation is configured to do
     * @param fileStores       every backend it built
     * @param registry         what is stored
     * @param referenceSources who still points at it
     * @param sweeper          what reclaims the rest
     */
    public StorageAdministration(StorageSettings settings, FileStores fileStores,
                                 StoredFileRegistry registry,
                                 Collection<StoredFileReferences> referenceSources,
                                 OrphanSweeper sweeper) {
        this.settings         = settings;
        this.fileStores       = fileStores;
        this.registry         = registry;
        this.referenceSources = referenceSources;
        this.sweeper          = sweeper;
    }

    /**
     * 🗄️ What this installation is configured to do.
     *
     * <p>⚠️ The upload policy is {@link UploadSettings#resolve() resolved} — a product names a profile
     * and the effective lists live in code, so "what does this installation actually accept" was not
     * answerable without reading Java. It is the question most often got wrong.</p>
     *
     * @return the overview
     */
    public StorageOverview overview() {
        UploadSettings upload = settings.upload().resolve();

        return new StorageOverview(
            fileStores.backendNames().stream().sorted().toList(),
            fileStores.defaultBackendName(),
            settings.contentAddressedKeys(),
            settings.maxSizeBytes(),
            upload.mode().name(),
            upload.contentTypes().stream().sorted().toList(),
            upload.extensions().stream().sorted().toList(),
            settings.sweeper().enabled(),
            settings.sweeper().gracePeriod().toString(),
            registry.count());
    }

    /**
     * 📦 A page of what is stored.
     *
     * @param offset where to start
     * @param limit  how many
     * @return the entries
     */
    public List<RegistryEntry> registry(int offset, int limit) {
        return registry.list(offset, limit).stream().map(RegistryEntry::of).toList();
    }

    /**
     * 🔗 Every reference source, and what each currently reports.
     *
     * <p>⚠️ <strong>A source that throws is reported as failed rather than allowed to fail the
     * request.</strong> One broken query must not hide the other nine — and a screen whose whole purpose
     * is "is anything wrong with my reference sources" would be useless if the first broken one blanked
     * it.</p>
     *
     * @return one status per source
     */
    public List<ReferenceSourceStatus> references() {
        return referenceSources.stream().map(this::statusOf).toList();
    }

    /**
     * 👁️ What a sweep WOULD reclaim, removing nothing.
     *
     * <p>⚠️ The call to reach for first, and the one this whole screen was worth building for.</p>
     *
     * @return the outcome, with {@code reclaiming} false
     */
    public SweepOutcome previewSweep() {
        return SweepOutcome.of(drain(sweeper.preview()), false);
    }

    /**
     * 🧹 Sweep, for real.
     *
     * <p>⚠️ Runs even when the scheduled sweep is disabled: the switch governs the <em>schedule</em>, and
     * an operator who has just read a preview and asked for it deliberately is not the case that switch
     * exists to prevent.</p>
     *
     * @return the outcome, with {@code reclaiming} true
     */
    public SweepOutcome sweep() {
        LOGGER.info("🧹 Sweep requested through the administration surface");

        return SweepOutcome.of(drain(sweeper.begin()), true);
    }

    private SweepReport drain(SweepRun run) {
        while (run.hasMore()) {
            run.sweepNextBatch();
        }

        return run.report();
    }

    private ReferenceSourceStatus statusOf(StoredFileReferences source) {
        try {
            return new ReferenceSourceStatus(source.sourceName(),
                                             source.referencedIdentifiers().count(), false, null);
        } catch (RuntimeException failure) {
            LOGGER.warn("🔗 Reference source '{}' could not be asked", source.sourceName(), failure);

            return new ReferenceSourceStatus(source.sourceName(), 0, true, failure.getMessage());
        }
    }
}
