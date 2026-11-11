package org.jmouse.cad.store.about;

import org.jmouse.plugin.ArtifactFacts;
import org.jmouse.plugin.Extensions;
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
    public record About(Engine engine, List<Plugin> plugins, Runtime runtime, List<Stage> roadmap) {
    }

    public record Engine(String name, String summary, String version, String author, String license, String site) {
    }

    /**
     * @param icon   its icon as an SVG document, drawn in the current colour; null for none
     * @param reason why it does not run; null for one that does
     */
    public record Plugin(String id, String title, String version, String description, String author, String icon,
                         boolean running, String reason) {
    }

    public record Runtime(String java) {
    }

    /** @param state {@code done}, {@code now}, {@code next} or {@code later} */
    public record Stage(String title, String state, List<String> items) {
    }

    /** {@code about.json}: what the manifest cannot say. */
    private record Written(String name, String summary, String site, List<Stage> roadmap) {
    }

    private final Extensions extensions;
    private final Engine     engine;
    private final List<Stage> roadmap;

    public EngineAbout(Extensions extensions) {
        this.extensions = extensions;
        Written written = written();
        ArtifactFacts facts = ArtifactFacts.of(EngineAbout.class);
        this.engine = new Engine(written.name(), written.summary(), facts.version(), facts.author(), facts.license(),
                                 written.site());
        this.roadmap = written.roadmap() == null ? List.of() : List.copyOf(written.roadmap());
    }

    /** What the engine is, in a sentence: its own summary, as a reader is told it wherever the engine is listed. */
    public static String engineSummary() {
        return written().summary();
    }

    public About about() {
        List<Plugin> plugins = extensions.plugins().stream().map(EngineAbout::pluginOf).toList();
        return new About(engine, plugins, new Runtime(System.getProperty("java.version")), roadmap);
    }

    private static Plugin pluginOf(PluginState state) {
        var plugin = state.plugin();
        return new Plugin(plugin.id(), plugin.title(), plugin.version(), plugin.description(), plugin.author(),
                          plugin.icon(), state.status() == PluginState.Status.STARTED, state.reason());
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
