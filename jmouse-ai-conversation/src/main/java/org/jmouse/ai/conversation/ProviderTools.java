package org.jmouse.ai.conversation;

import org.jmouse.ai.PublishedTool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The catalogue, as a model is shown it.
 *
 * <p>Three fields out of ten, and the seven that are left out are the point. A model chooses what to
 * call by reading a name, a sentence and a schema; a permission name, a scope-confinement flag and an
 * origin are facts about how the call will be <em>handled</em>, and putting them in front of a model
 * invites it to reason about authorization it cannot see the answer to — which is how a model ends up
 * telling somebody they lack a permission they hold.
 *
 * <p>⚠️ The read-only and destructive hints are not dropped because they do not matter. They matter to a
 * <strong>client</strong>, which can warn a person before a call rather than after, and a client is
 * what {@code jmouse-ai-mcp} publishes to. They are not shown to a model here because this loop has no
 * client and no user to warn — the guards are what stand between the model and the data, and they run
 * whatever the model believed.
 */
public final class ProviderTools {

    private ProviderTools() {
    }

    /**
     * The actions to put in front of a model, in the catalogue's stable order.
     *
     * <p>⚠️ <strong>A list rather than the catalogue, and the difference is a permission.</strong> This
     * took a {@link ToolCatalog} and rendered all of it, which is every action the installation has
     * rather than every action the person asking may run. Ask {@code ToolDispatcher.reachable()} for
     * the list — it is the dispatcher's own gate, so what is rendered here is exactly what would be
     * allowed through.
     */
    public static List<Map<String, Object>> from(List<PublishedTool> tools) {
        return tools.stream().map(ProviderTools::from).toList();
    }

    /**
     * One tool, in the shape the canonical request carries.
     *
     * <p>{@code input_schema} rather than {@code parameters}: the canonical shape is Anthropic's, and
     * translating it into anybody else's is the provider implementation's job. A renderer that guessed
     * which provider was behind the port would be the second place that decision lives.
     */
    public static Map<String, Object> from(PublishedTool tool) {
        Map<String, Object> rendered = new LinkedHashMap<>();

        rendered.put("name",         tool.publishedName());
        rendered.put("description",  tool.description());
        rendered.put("input_schema", tool.inputSchema());

        return rendered;
    }
}
