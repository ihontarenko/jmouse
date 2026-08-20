package org.jmouse.storage.administration;

/**
 * 🛣️ Where the storage administration surface lives.
 *
 * <h3>⚠️ One constant, because this exact address has already gone wrong once</h3>
 *
 * <p>The AI management screen's prefix lived in three or four files that nothing checked against each
 * other — a controller, a YAML property, a router in the interface, a build step — and when they drifted
 * apart the screen did not fail. It <strong>rendered as an empty installation</strong>: no error, no
 * refusal, just a product that appeared to have nothing configured. That failure mode is why the address
 * is stated once here and read from here by everything, and why
 * {@code StorageAdministrationDiagnostics} logs it at startup rather than leaving it implicit.</p>
 */
public final class AdministrationRoutes {

    /** Everything the administration surface serves sits under here. */
    public static final String BASE = "/api/administration/storage";

    /** What this installation is configured to do. */
    public static final String OVERVIEW = BASE;

    /** What is actually stored. */
    public static final String REGISTRY = BASE + "/registry";

    /** Who still points at it. */
    public static final String REFERENCES = BASE + "/references";

    /** What a sweep WOULD reclaim. */
    public static final String SWEEP_PREVIEW = BASE + "/sweep/preview";

    /** A sweep, for real. */
    public static final String SWEEP = BASE + "/sweep";

    private AdministrationRoutes() {
    }
}
