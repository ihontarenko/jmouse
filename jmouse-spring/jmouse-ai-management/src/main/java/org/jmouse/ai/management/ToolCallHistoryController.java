package org.jmouse.ai.management;

import org.jmouse.ai.PublishedTool;
import org.jmouse.ai.view.ToolCallHistory;
import org.jmouse.ai.view.ToolCatalogView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * What has actually been called, by whom, and how it went.
 *
 * <p>The screen somebody opens after something surprising happened. Everything here comes from
 * {@link ToolCallHistory}, which most products will not have implemented — and an empty answer from
 * this endpoint means <em>no trail is configured</em> at least as often as it means nothing has been
 * called. ⚠️ A page rendering it should say which, because the two look identical and mean opposite
 * things.
 *
 * <p>An action is addressed by its <strong>wire</strong> name here, the same spelling
 * {@link ToolCatalogController} publishes, so that one screen can link to the other without knowing that
 * the trail records the dotted form. The translation is a catalogue lookup, which is also what turns a
 * mistyped name into a refusal rather than an empty list that reads as "this has never been called".
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX)
public class ToolCallHistoryController {

    private final ToolCallHistory history;
    private final ToolCatalogView tools;

    public ToolCallHistoryController(ToolCallHistory history, ToolCatalogView tools) {
        this.history = history;
        this.tools   = tools;
    }

    /** The most recent calls, newest first, optionally narrowed to one caller. */
    @GetMapping("/calls")
    public List<ToolCallHistory.Entry> calls(
            @RequestParam(name = "caller", required = false) String caller,
            @RequestParam(name = "limit", defaultValue = "0") int limit) {

        int bounded = ManagementRoutes.boundedLimit(limit);

        return caller == null || caller.isBlank()
                ? history.recent(bounded)
                : history.forCaller(caller, bounded);
    }

    /** The most recent calls of one action, addressed as {@code parts_discard}. */
    @GetMapping("/tools/{publishedName}/calls")
    public List<ToolCallHistory.Entry> callsOf(
            @PathVariable("publishedName") String publishedName,
            @RequestParam(name = "limit", defaultValue = "0") int limit) {

        PublishedTool tool = tools.find(publishedName)
                .orElseThrow(() -> new UnknownPublishedToolException(publishedName));

        return history.forAction(tool.qualifiedName(), ManagementRoutes.boundedLimit(limit));
    }
}
