package org.jmouse.cad.store.plugin;

import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.PluginSetting;
import org.jmouse.plugin.PluginSettingRefused;
import org.jmouse.plugin.PluginSettings;
import org.jmouse.plugin.PluginState;
import org.jmouse.plugin.ServerFolders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 🎚️ The settings of the plugins the engine runs, as a reader is shown them and as a curator changes them: every
 * running plugin that declares a setting, each setting with the value it stands at. Reading them is open, but for a
 * secret, told to nobody, and what names the server's folders and programs, told only to whoever may change it; changing
 * them is {@link CadCapability#ENGINE_CONFIGURE}, installation-wide, and each change is logged with who made it: a program
 * named here is one the server will run.
 */
public class EnginePluginSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnginePluginSettings.class);

    /**
     * One setting as a reader is shown it.
     *
     * @param value  what it stands at now: its default, or what it was changed to; a secret's is never told
     * @param group  what it is found under among the plugin's others; null for none
     * @param secret where a secret's value comes from; null for any other setting
     * @param absent which of a {@code FOLDERS} setting's folders are not on the server now; empty for any other setting
     */
    public record SettingView(String key, String title, String description, PluginSetting.Kind kind, String defaultValue,
                              String value, List<PluginSetting.Choice> choices, String unit, BigDecimal least,
                              BigDecimal most, String group, PluginSettings.SecretState secret, List<String> absent) {
    }

    /** A plugin's settings, in the order it declares them. */
    public record PluginSettingsView(String plugin, String title, List<SettingView> settings) {
    }

    /**
     * Every running plugin's settings.
     *
     * @param changeable   whether the reader may change them: the installation keeps them, and the reader may set the engine up
     * @param keepsSecrets whether a key to a service can be kept here: the installation has what to seal it with
     */
    public record EngineSettingsView(boolean changeable, boolean keepsSecrets, List<PluginSettingsView> plugins) {
    }

    private final Extensions     extensions;
    private final PluginSettings settings;
    private final CadAuthority   authority;

    public EnginePluginSettings(Extensions extensions, PluginSettings settings, CadAuthority authority) {
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

    /**
     * Changes a running plugin's settings, all of them or none; a null value brings a setting back to its default.
     *
     * @return the plugin's settings as they now stand
     */
    public PluginSettingsView change(String plugin, Map<String, String> values) {
        authority.requireGlobally(CadCapability.ENGINE_CONFIGURE);
        PluginState state = extensions.plugins().stream()
                .filter(candidate -> candidate.plugin().id().equals(plugin) && candidate.status() == PluginState.Status.STARTED)
                .findFirst()
                .orElseThrow(() -> CadProblem.notFound("Plugin '" + plugin + "' does not run here."));
        try {
            settings.change(plugin, values);
            LOGGER.info("{} changed the settings {} of plugin {}.", authority.actor().orElse("Nobody signed in"), values.keySet(), plugin);
        } catch (PluginSettingRefused refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
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

    private boolean changeable() {
        return settings.changeable() && authority.permitsGlobally(CadCapability.ENGINE_CONFIGURE);
    }

    private PluginSettingsView viewOf(String plugin, String title) {
        Map<String, String> current = settings.currentOf(plugin);
        boolean curator = authority.permitsGlobally(CadCapability.ENGINE_CONFIGURE);
        List<SettingView> views = settings.declaredBy(plugin).stream().map(setting -> {
            boolean secret = setting.kind() == PluginSetting.Kind.SECRET;
            boolean told = !secret && (curator || !setting.kind().namesTheServer());
            String value = told ? current.get(setting.key()) : "";
            return new SettingView(setting.key(), setting.title(), setting.description(), setting.kind(),
                                   told ? setting.defaultValue() : "", value, setting.choices(), setting.unit(),
                                   setting.least(), setting.most(), setting.group(),
                                   secret ? settings.secretStateOf(plugin, setting) : null,
                                   setting.kind() == PluginSetting.Kind.FOLDERS ? absentFolders(value) : List.of());
        }).toList();
        return new PluginSettingsView(plugin, title, views);
    }

    /** The folders named that are not on the server now: a drive taken away keeps its place in the list. */
    private static List<String> absentFolders(String value) {
        return value.lines().map(String::strip).filter(line -> !line.isEmpty())
                .filter(line -> !Files.isDirectory(Path.of(line))).toList();
    }
}
