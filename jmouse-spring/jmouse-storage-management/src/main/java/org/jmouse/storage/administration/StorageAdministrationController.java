package org.jmouse.storage.administration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🗄️ The storage administration surface.
 *
 * <p>⚠️ <strong>No {@code @RequiresAccess} here either</strong> — gated from outside through
 * {@code ExternalAccessRules}, like every other controller this library ships. An annotation would win
 * over the product's own rule and make it silently unreachable.</p>
 *
 * <p>⚠️ <strong>And this one is switched off separately from the file routes.</strong> Reading the
 * registry lists every stored object's key and name across the whole installation, which is a
 * disclosure surface of its own — a product may well want file endpoints and not want this. It needs
 * {@code jmouse.storage.administration.enabled}, and it should be declared at {@code GLOBAL} scope with
 * an administrative permission rather than the one that gates ordinary file reads.</p>
 */
@RestController
public class StorageAdministrationController {

    private final StorageAdministration administration;

    /**
     * 🏗️ Serve the administration surface.
     *
     * @param administration what the routes actually do
     */
    public StorageAdministrationController(StorageAdministration administration) {
        this.administration = administration;
    }

    /**
     * 🗄️ What this installation is configured to do — including the RESOLVED upload policy.
     *
     * @return the overview
     */
    @GetMapping(AdministrationRoutes.OVERVIEW)
    public StorageOverview overview() {
        return administration.overview();
    }

    /**
     * 📦 A page of what is stored.
     *
     * @param offset where to start
     * @param limit  how many, capped so a request cannot ask for the whole registry at once
     * @return the entries
     */
    @GetMapping(AdministrationRoutes.REGISTRY)
    public List<RegistryEntry> registry(@RequestParam(defaultValue = "0") int offset,
                                        @RequestParam(defaultValue = "50") int limit) {
        return administration.registry(Math.max(0, offset), Math.clamp(limit, 1, 500));
    }

    /**
     * 🔗 Every reference source, and what each reports.
     *
     * <p>⚠️ The screen worth opening before ever enabling the sweeper: a source reporting zero is either
     * honest or a query that quietly stopped matching, and those two look identical from anywhere else.</p>
     *
     * @return one status per source
     */
    @GetMapping(AdministrationRoutes.REFERENCES)
    public List<ReferenceSourceStatus> references() {
        return administration.references();
    }

    /**
     * 👁️ What a sweep would reclaim, removing nothing.
     *
     * <p>⚠️ {@code POST} despite changing nothing, because it can be expensive: it walks the whole
     * registry. A {@code GET} invites a browser, a proxy or a prefetcher to run it unasked.</p>
     *
     * @return the outcome, with {@code reclaiming} false
     */
    @PostMapping(AdministrationRoutes.SWEEP_PREVIEW)
    public SweepOutcome previewSweep() {
        return administration.previewSweep();
    }

    /**
     * 🧹 Sweep, for real.
     *
     * <p>⚠️ Irreversible. Nothing here asks for confirmation — that belongs to the interface, which is
     * where somebody can be shown the preview they are agreeing to.</p>
     *
     * @return the outcome, with {@code reclaiming} true
     */
    @PostMapping(AdministrationRoutes.SWEEP)
    public SweepOutcome sweep() {
        return administration.sweep();
    }
}
