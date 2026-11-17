package org.jmouse.cad.store.plugin;

import org.jmouse.cad.settings.SettingsSection;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 🎚️ The installation's settings of the engine and every plugin it runs, as a reader is shown them and as a curator changes them.
 * Every section of fixed parameters (ADR 0007) stands in the one settings tree by its path — the core's at its own
 * ({@code part-search}), a plugin's under its id ({@code plugin/cad-suppliers/mouser}) —, each parameter with the value it stands at.
 * Reading them is open, but for a secret, told to nobody, and what names the server's folders and programs, told only to whoever
 * may change it; changing any section is {@link CadCapability#ENGINE_CONFIGURE}, installation-wide, and each change is logged with
 * who made it: a program named here is one the server will run.
 */
public class EnginePluginSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnginePluginSettings.class);

    /**
     * A section of fixed parameters: whose it is, where it stands among its plugin's and in the tree, and what it is called.
     *
     * @param path where it stands in the settings tree ({@link SettingsSection#pathOf})
     */
    public record FixedSection(String plugin, String pathInOwner, String path, String label, String description) {
    }

    /**
     * Where a secret's value comes from, and whether another can be kept here.
     *
     * @param keepable the installation has what to seal one with
     */
    public record SecretView(Settings.SecretState state, boolean keepable) {
    }

    /**
     * One parameter as a reader is shown it.
     *
     * @param value  what it stands at now: its default, or what it was changed to; a secret's is never told
     * @param secret where a secret's value comes from; null for any other parameter
     * @param absent which of a {@code FOLDERS} parameter's folders are not on the server now; empty for any other
     */
    public record ParameterView(String key, String label, String description, ParameterKind kind, String defaultValue, String value,
                                List<Parameter.Choice> choices, String unit, BigDecimal least, BigDecimal most,
                                SecretView secret, List<String> absent) {
    }

    private final Extensions   extensions;
    private final Settings     settings;
    private final CadAuthority authority;

    public EnginePluginSettings(Extensions extensions, Settings settings, CadAuthority authority) {
        this.extensions = extensions;
        this.settings = settings;
        this.authority = authority;
    }

    /**
     * Every running plugin's sections of fixed parameters, by path — to a signed-in reader only: they are how this installation is
     * set up, never what a page is drawn with, so a host that opens the settings' records to a visitor opens none of these.
     */
    public List<FixedSection> fixedSections() {
        if (authority.actor().isEmpty()) {
            return List.of();
        }
        return extensions.plugins().stream()
                .filter(state -> state.status() == PluginState.Status.STARTED)
                .flatMap(state -> settings.sectionsOf(state.plugin().id()).stream()
                        .filter(section -> section.shape() instanceof Section.Fixed)
                        .map(section -> new FixedSection(state.plugin().id(), section.path(),
                                                         SettingsSection.pathOf(state.plugin().id(), section.path()),
                                                         section.label(), section.description())))
                .sorted(Comparator.comparing(FixedSection::path))
                .toList();
    }

    /** The section of fixed parameters at a path of the tree; empty for none. */
    public Optional<FixedSection> fixedSection(String path) {
        return fixedSections().stream().filter(section -> section.path().equals(path)).findFirst();
    }

    /** A section's parameters as the reader is shown them, in the order it declares them. */
    public List<ParameterView> parametersOf(FixedSection fixed) {
        boolean curator = authority.permitsGlobally(CadCapability.ENGINE_CONFIGURE);
        Map<String, String> current = settings.valuesOf(fixed.plugin(), fixed.pathInOwner());
        return declared(fixed).stream().map(parameter -> {
            boolean secret = parameter.kind() == ParameterKind.SECRET;
            boolean told = !secret && (curator || !parameter.kind().namesTheServer());
            String value = told ? current.getOrDefault(parameter.key(), parameter.defaultValue()) : "";
            return new ParameterView(parameter.key(), parameter.label(), parameter.description(), parameter.kind(),
                                     told ? parameter.defaultValue() : "", value, parameter.choices(), parameter.unit(),
                                     parameter.least(), parameter.most(),
                                     secret ? new SecretView(settings.secretStateOf(fixed.plugin(), fixed.pathInOwner(), parameter), settings.keepsSecrets()) : null,
                                     parameter.kind() == ParameterKind.FOLDERS ? absentFolders(value) : List.of());
        }).toList();
    }

    /** Whether a section stands other than its plugin declares it: a value changed, or a key to a service kept here. */
    public boolean changed(FixedSection fixed) {
        Map<String, String> current = settings.valuesOf(fixed.plugin(), fixed.pathInOwner());
        return declared(fixed).stream().anyMatch(parameter -> parameter.kind() == ParameterKind.SECRET
                ? settings.secretStateOf(fixed.plugin(), fixed.pathInOwner(), parameter) == Settings.SecretState.KEPT
                : !Objects.equals(current.getOrDefault(parameter.key(), parameter.defaultValue()), parameter.defaultValue()));
    }

    /** Changes the installation's values of one section of a plugin's settings, all or none, whatever its shape. */
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

    private List<Parameter> declared(FixedSection fixed) {
        return settings.section(fixed.plugin(), fixed.pathInOwner())
                .map(section -> ((Section.Fixed) section.shape()).parameters())
                .orElseThrow(() -> CadProblem.notFound("settings " + fixed.path()));
    }

    /** The folders named that are not on the server now: a drive taken away keeps its place in the list. */
    private static List<String> absentFolders(String value) {
        return value.lines().map(String::strip).filter(line -> !line.isEmpty())
                .filter(line -> !Files.isDirectory(Path.of(line))).toList();
    }
}
