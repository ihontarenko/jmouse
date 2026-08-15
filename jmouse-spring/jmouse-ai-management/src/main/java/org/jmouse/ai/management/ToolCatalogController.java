package org.jmouse.ai.management;

import org.jmouse.ai.PublishedTool;
import org.jmouse.ai.view.ToolCatalogView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * What this application can be asked to do, and what each of those costs.
 *
 * <p>The screen somebody opens to answer <em>"can the assistant delete things, and who would have to
 * hold what for it to"</em>. Every field it shows is declared in the source of a tool and is not a
 * secret; what it cannot show, because it cannot reach one, is a handler.
 *
 * <p>⚠️ This controller holds a {@link ToolCatalogView} and nothing else — not the catalogue, not the
 * dispatcher. That is deliberate and is the property that keeps this module from becoming a second way
 * into an action.
 */
@RestController
@RequestMapping(ManagementRoutes.PREFIX)
public class ToolCatalogController {

    private final ToolCatalogView tools;

    public ToolCatalogController(ToolCatalogView tools) {
        this.tools = tools;
    }

    /**
     * Every published action, in the catalogue's own order.
     *
     * <p>Namespaces alphabetically, actions in the order their feature declares them — a tool reads best
     * with its listing actions above its writing ones, and re-sorting here would throw that away.
     */
    @GetMapping("/tools")
    public List<PublishedTool> tools() {
        return tools.tools();
    }

    /**
     * The shape of the catalogue rather than its contents.
     *
     * <p>Its own endpoint because the two counts are not derivable from each other — eight actions may
     * be three tools — and because a header line should not have to fetch every schema to render.
     *
     * @param actions     how many things can be done
     * @param tools       the namespaces they are grouped under
     * @param destructive how many of the actions remove or overwrite something
     * @param remote      how many are forwarded to a server this application connected to
     */
    public record CatalogueSummary(
            int          actions,
            List<String> tools,
            long         destructive,
            int          remote
    ) {
    }

    @GetMapping("/tools/summary")
    public CatalogueSummary summary() {
        List<PublishedTool> published = tools.tools();

        return new CatalogueSummary(
                published.size(),
                tools.toolNames(),
                published.stream().filter(PublishedTool::destructive).count(),
                tools.remoteTools().size());
    }

    /** One action by its wire name, e.g. {@code parts_discard}. */
    @GetMapping("/tools/{publishedName}")
    public PublishedTool tool(@PathVariable("publishedName") String publishedName) {
        return tools.find(publishedName)
                .orElseThrow(() -> new UnknownPublishedToolException(publishedName));
    }
}
