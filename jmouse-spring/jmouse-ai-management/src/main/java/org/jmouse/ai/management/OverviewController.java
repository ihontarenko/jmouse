package org.jmouse.ai.management;

import org.jmouse.ai.view.ProviderRegistry;
import org.jmouse.ai.view.ToolCallHistory;
import org.jmouse.ai.view.ToolCatalogView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Everything a management screen opens with, in one request.
 *
 * <p>One request rather than four, because the header of that screen is a single sentence — <em>what is
 * in force, can it answer, is anything recorded, how much is there</em> — and rendering it from four
 * round trips means four loading states for one line of text.
 *
 * <p>⚠️ <strong>Every field here is derived from the ports beside it and none of it is new
 * information</strong>, which is why this controller holds no state and reaches nothing the others
 * cannot. It is a convenience over reads, not a fifth port.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX)
public class OverviewController {

    private final ProviderRegistry providers;
    private final ToolCatalogView  tools;
    private final ToolCallHistory  history;

    public OverviewController(
            ProviderRegistry providers, ToolCatalogView tools, ToolCallHistory history) {

        this.providers = providers;
        this.tools     = tools;
        this.history   = history;
    }

    /**
     * @param activeProvider     what resolved, or null where nothing did
     * @param assistantAvailable ⚠️ a model <strong>and</strong> a key. Configured and available are
     *                           different questions — a provider with no key resolves perfectly and
     *                           would be refused before anything was sent — so they are reported apart
     *                           rather than collapsed into one boolean somebody has to guess at
     * @param trailRecorded      ⚠️ whether anything records a per-call trail at all. An application with
     *                           no trail and one where nothing has been called produce the same empty
     *                           list and mean opposite things
     * @param publishedActions   how many things can be done
     * @param publishedTools     the namespaces they are grouped under — not derivable from the count
     *                           above, since eight actions may be three tools
     */
    public record Overview(
            ProviderRegistry.ActiveProvider activeProvider,
            boolean                         assistantAvailable,
            boolean                         trailRecorded,
            int                             publishedActions,
            int                             publishedTools
    ) {
    }

    @GetMapping("/overview")
    public Overview overview() {
        ProviderRegistry.ActiveProvider active = providers.active().orElse(null);

        return new Overview(
                active,
                active != null && active.keyConfigured(),
                // ⚠️ One entry is enough to tell "nothing is recording" from "nothing has happened", and
                // it costs a single row rather than a listing nobody reads.
                !history.recent(1).isEmpty(),
                tools.tools().size(),
                tools.toolNames().size());
    }
}
