package org.jmouse.cad.store.plugin;

import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.PluginState;
import org.jmouse.plugin.ServerFolders;
import org.jmouse.settings.Parameter;
import org.jmouse.settings.ParameterKind;
import org.jmouse.settings.Section;
import org.jmouse.settings.SettingHolder;
import org.jmouse.settings.SettingRefused;
import org.jmouse.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 🎚️ The parameters of the plugins the engine runs, as a reader is shown them and as a curator changes them: every running plugin's
 * sections of fixed parameters (ADR 0007), each parameter with the value it stands at. Reading them is open, but for a secret, told
 * to nobody, and what names the server's folders and programs, told only to whoever may change it; changing them is
 * {@link CadCapability#ENGINE_CONFIGURE}, installation-wide, and each change is logged with who made it: a program named here is one
 * the server will run. A structured section is the settings tree's ({@code /settings}).
 */
public class EnginePluginSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnginePluginSettings.class);

    /**
     * One parameter as a reader is shown it.
     *
     * @param value  what it stands at now: its default, or what it was changed to; a secret's is never told
     * @param secret where a secret's value comes from; null for any other parameter
     * @param absent which of a {@code FOLDERS} parameter's folders are not on the server now; empty for any other
     */
    public record ParameterView(String key, String label, String description, ParameterKind kind, String defaultValue, String value,
                                List<Parameter.Choice> choices, String unit, BigDecimal least, BigDecimal most,
                                Settings.SecretState secret, List<String> absent) {
    }

    /** A section of a plugin's parameters, in the order it declares them. */
    public record SectionView(String path, String label, String description, List<ParameterView> parameters) {
    }

    /** A plugin's sections of parameters, in the order it declares them. */
    public record PluginSettingsView(String plugin, String title, List<SectionView> sections) {
    }

    /**
     * Every running plugin's parameters.
     *
     * @param changeable   whether the reader may change them: the installation keeps them, and the reader may set the engine up
     * @param keepsSecrets whether a key to a service can be kept here: the installation has what to seal it with
     */
    public record EngineSettingsView(boolean changeable, boolean keepsSecrets, List<PluginSettingsView> plugins) {
    }

    private final Extensions   extensions;
    private final Settings     settings;
    private final CadAuthority authority;

    public EnginePluginSettings(Extensions extensions, Settings settings, CadAuthority authority) {
        this.extensions = extensions;
        this.settings = settings;
        this.authority = authority;
    }

    public EngineSettingsView settings() {
        List<PluginSettingsView> plugins = extensions.plugins().stream()
                .filter(state -> state.status() == PluginState.Status.STARTED)
                .map(state -> viewOf(state.plugin().id(), state.plugin().title()))
                .toList();
        return new EngineSettingsView(changeable(), settings.keepsSecrets(), plugins);
    }

    /** Changes the installation's values of one section of a plugin's settings, all or none; one that is structured included. */
    public void changeSection(String plugin, String section, Map<String, String> values) {
        authority.requireGlobally(CadCapability.ENGINE_CONFIGURE);
        try {
            settings.change(plugin, section, SettingHolder.INSTALLATION, values);
            LOGGER.info("{} changed {} of {}/{}.", authority.actor().orElse("Nobody signed in"), values.keySet(), plugin, section);
        } catch (SettingRefused refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
    }

    /** Replaces the installation's values of one section with these, its lists grown or shrunk: an editor's whole record. */
    public void replaceSection(String plugin, String section, Map<String, String> values) {
        authority.requireGlobally(CadCapability.ENGINE_CONFIGURE);
        try {
            settings.replace(plugin, section, SettingHolder.INSTALLATION, values);
            LOGGER.info("{} replaced {}/{}.", authority.actor().orElse("Nobody signed in"), plugin, section);
        } catch (SettingRefused refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
    }

    /** Brings one section of a plugin's settings back to what the plugin declares, its keys to services kept. */
    public void restoreSection(String plugin, String section) {
        authority.requireGlobally(CadCapability.ENGINE_CONFIGURE);
        try {
            settings.restore(plugin, section, SettingHolder.INSTALLATION);
            LOGGER.info("{} brought {}/{} back to its defaults.", authority.actor().orElse("Nobody signed in"), plugin, section);
        } catch (SettingRefused refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
    }

    /**
     * Changes a running plugin's parameters of one section, all of them or none; a null value brings one back to its default.
     *
     * @return the plugin's parameters as they now stand
     */
    public PluginSettingsView change(String plugin, String section, Map<String, String> values) {
        PluginState state = extensions.plugins().stream()
                .filter(candidate -> candidate.plugin().id().equals(plugin) && candidate.status() == PluginState.Status.STARTED)
                .findFirst()
                .orElseThrow(() -> CadProblem.notFound("Plugin '" + plugin + "' does not run here."));
        if (settings.section(plugin, section).filter(found -> found.shape() instanceof Section.Fixed).isEmpty()) {
            throw CadProblem.notFound("Plugin '" + plugin + "' has no parameters '" + section + "'.");
        }
        changeSection(plugin, section, values);
        return viewOf(plugin, state.plugin().title());
    }

    /** The drives of the server, to choose a folder from: only for whoever may set the engine up. */
    public List<ServerFolders.Folder> serverRoots() {
        authority.requireGlobally(CadCapability.ENGINE_CONFIGURE);
        return ServerFolders.roots();
    }

    /** What a folder of the server holds, to choose one from: only for whoever may set the engine up. */
    public ServerFolders.Listing serverFolder(String path) {
        authority.requireGlobally(CadCapability.ENGINE_CONFIGURE);
        try {
            return ServerFolders.listing(path);
        } catch (IllegalArgumentException | UncheckedIOException refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
    }

    /** Whether the reader may change the engine's settings: the installation keeps them, and the reader may set the engine up. */
    public boolean changeable() {
        return settings.changeable() && authority.permitsGlobally(CadCapability.ENGINE_CONFIGURE);
    }

    private PluginSettingsView viewOf(String plugin, String title) {
        boolean curator = authority.permitsGlobally(CadCapability.ENGINE_CONFIGURE);
        List<SectionView> sections = settings.sectionsOf(plugin).stream()
                .filter(section -> section.shape() instanceof Section.Fixed)
                .map(section -> {
                    Map<String, String> current = settings.valuesOf(plugin, section.path());
                    List<ParameterView> parameters = ((Section.Fixed) section.shape()).parameters().stream().map(parameter -> {
                        boolean secret = parameter.kind() == ParameterKind.SECRET;
                        boolean told = !secret && (curator || !parameter.kind().namesTheServer());
                        String value = told ? current.getOrDefault(parameter.key(), parameter.defaultValue()) : "";
                        return new ParameterView(parameter.key(), parameter.label(), parameter.description(), parameter.kind(),
                                                 told ? parameter.defaultValue() : "", value, parameter.choices(), parameter.unit(),
                                                 parameter.least(), parameter.most(),
                                                 secret ? settings.secretStateOf(plugin, section.path(), parameter) : null,
                                                 parameter.kind() == ParameterKind.FOLDERS ? absentFolders(value) : List.of());
                    }).toList();
                    return new SectionView(section.path(), section.label(), section.description(), parameters);
                }).toList();
        return new PluginSettingsView(plugin, title, sections);
    }

    /** The folders named that are not on the server now: a drive taken away keeps its place in the list. */
    private static List<String> absentFolders(String value) {
        return value.lines().map(String::strip).filter(line -> !line.isEmpty())
                .filter(line -> !Files.isDirectory(Path.of(line))).toList();
    }
}
