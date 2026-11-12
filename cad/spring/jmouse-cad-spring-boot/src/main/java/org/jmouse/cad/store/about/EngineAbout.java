package org.jmouse.cad.store.about;

import org.jmouse.plugin.ArtifactFacts;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.PluginIcon;
import org.jmouse.plugin.PluginState;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * What the engine says of itself — {@code JMOUSE_CAD_API.md} §2.8: its name, version, maker and licence, the plugins it
 * runs and the plans. A product that embeds the engine shows the same window, whatever the product is. The version,
 * maker and licence are the engine jar's manifest; the name, the summary and the plans are {@code about.json} beside this
 * class — neither is written in code.
 */
public class EngineAbout {

    private static final String ABOUT = "about.json";

    /** What the window shows. */
    /** @param pluginGroups the order the plugins' kinds are listed in; a kind not named here comes after them */
    public record About(Engine engine, List<Plugin> plugins, List<String> pluginGroups, Runtime runtime) {
    }

    public record Engine(String name, String summary, String version, String author, String license, String site) {
    }

    /**
     * @param summary   what it brings, in one line; null where its jar does not say
     * @param category  the kind it is, the list's group; null for none
     * @param icon      its icon, and whether the page paints it in its colour or it keeps its own; null for none
     * @param reason    why it does not run; null for one that does
     * @param requires  the plugins it cannot run without — Gerber for a pcb house's set —, by id
     * @param extending the running plugins it adds to when they run — KiCad's model libraries to the 3D models —, by id
     */
    public record Plugin(String id, String title, String version, String summary, String category, String author, PluginIcon icon,
                         boolean running, String reason, List<String> requires, List<String> extending) {
    }

    public record Runtime(String java) {
    }

    /** {@code about.json}: what the manifest cannot say. */
    private record Written(String name, String summary, String site, List<String> pluginGroups) {
    }

    private final Extensions extensions;
    private final Engine     engine;
    private final List<String> pluginGroups;

    public EngineAbout(Extensions extensions) {
        this.extensions = extensions;
        Written written = written();
        ArtifactFacts facts = ArtifactFacts.of(EngineAbout.class);
        this.engine = new Engine(written.name(), written.summary(), facts.version(), facts.author(), facts.license(),
                                 written.site());
        this.pluginGroups = written.pluginGroups() == null ? List.of() : List.copyOf(written.pluginGroups());
    }

    public About about() {
        List<Plugin> plugins = extensions.plugins().stream().map(EngineAbout::pluginOf).toList();
        return new About(engine, plugins, pluginGroups, new Runtime(System.getProperty("java.version")));
    }

    private static Plugin pluginOf(PluginState state) {
        var plugin = state.plugin();
        return new Plugin(plugin.id(), plugin.title(), plugin.version(), plugin.summary(), plugin.category(), plugin.author(),
                          plugin.icon(), state.status() == PluginState.Status.STARTED, state.reason(),
                          plugin.requires().stream().sorted().toList(), state.extending().stream().sorted().toList());
    }

    private static Written written() {
        try (InputStream input = EngineAbout.class.getResourceAsStream(ABOUT)) {
            if (input == null) {
                throw new IllegalStateException("The engine holds no " + ABOUT + " beside " + EngineAbout.class.getName());
            }
            return JsonMapper.builder().build().readValue(input, Written.class);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The engine's " + ABOUT + " does not read", unreadable);
        }
    }
}
