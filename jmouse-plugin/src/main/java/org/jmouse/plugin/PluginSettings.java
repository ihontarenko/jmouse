package org.jmouse.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 🎚️ The settings of an application's plugins: what each plugin declares ({@link Plugin#settings()}), the values they
 * stand at, and the one way they are changed — checked against the declaration, kept all at once or not at all.
 *
 * <p>The plugins are read when a setting is first asked for, not when this is made: a plugin's own beans read their
 * settings through this, so it cannot wait for the plugins to be made. Without a {@link PluginSettingStore} every setting
 * stands at its default and a change is refused — a change that would be lost at the next start is not offered as
 * kept.</p>
 */
public final class PluginSettings {

    private final Supplier<? extends Collection<? extends Plugin>> plugins;
    private final Optional<PluginSettingStore>                    store;
    private final Optional<PluginSecrets>                         secrets;
    /** The values kept for each plugin, read once and dropped on every change made here. */
    private final Map<String, Map<String, String>>                changed = new ConcurrentHashMap<>();
    private volatile Map<String, List<PluginSetting>>             declared;

    public PluginSettings(Supplier<? extends Collection<? extends Plugin>> plugins, Optional<PluginSettingStore> store) {
        this(plugins, store, Optional.empty());
    }

    /** @param secrets what seals a secret setting where it is kept; empty to keep none */
    public PluginSettings(Supplier<? extends Collection<? extends Plugin>> plugins, Optional<PluginSettingStore> store,
                          Optional<PluginSecrets> secrets) {
        this.plugins = plugins;
        this.store = store;
        this.secrets = secrets;
    }

    /** Where a secret setting's value comes from now. It is all a reader is ever told of one. */
    public enum SecretState {
        /** It has none. */
        NONE,
        /** The deployment's: nobody kept another. */
        DEPLOYED,
        /** One kept here, sealed. */
        KEPT
    }

    /** Whether a secret can be kept here. */
    public boolean keepsSecrets() {
        return secrets.isPresent();
    }

    /** Where one of a plugin's secret settings stands. */
    public SecretState secretStateOf(String plugin, PluginSetting setting) {
        if (changedOf(plugin).containsKey(setting.key())) {
            return SecretState.KEPT;
        }
        return setting.defaultValue().isBlank() ? SecretState.NONE : SecretState.DEPLOYED;
    }

    /** Whether a change can be kept: the application keeps its plugins' settings somewhere. */
    public boolean changeable() {
        return store.isPresent();
    }

    /** What a plugin may be set to, in the order it declares it; empty for a plugin that declares nothing or is unknown. */
    public List<PluginSetting> declaredBy(String plugin) {
        return declarations().getOrDefault(plugin, List.of());
    }

    /** The values a plugin's settings stand at, by key, in the order it declares them; a secret's is never among them. */
    public Map<String, String> currentOf(String plugin) {
        Map<String, String> kept = changedOf(plugin);
        Map<String, String> current = new LinkedHashMap<>();
        for (PluginSetting setting : declaredBy(plugin)) {
            if (setting.kind() != PluginSetting.Kind.SECRET) {
                current.put(setting.key(), kept.getOrDefault(setting.key(), setting.defaultValue()));
            }
        }
        return current;
    }

    /** A plugin's settings as the plugin reads them, each value read as it stands at that moment. */
    public PluginSettingValues valuesOf(String plugin) {
        return new PluginSettingValues(this, plugin);
    }

    /**
     * Changes a plugin's settings: every value checked against its declaration, then all kept at once. A null value
     * forgets the change, the setting back at its default.
     *
     * @throws PluginSettingRefused naming what was refused and why; then nothing is kept
     */
    public void change(String plugin, Map<String, String> values) {
        PluginSettingStore keeping = store.orElseThrow(() -> new PluginSettingRefused(
                "This application keeps no plugin settings: they stand at their defaults."));
        List<PluginSetting> settings = declaredBy(plugin);
        if (settings.isEmpty()) {
            throw new PluginSettingRefused("Plugin '" + plugin + "' has no settings.");
        }
        for (Map.Entry<String, String> value : values.entrySet()) {
            PluginSetting setting = settingOf(settings, value.getKey()).orElseThrow(() -> new PluginSettingRefused(
                    "Plugin '" + plugin + "' has no setting '" + value.getKey() + "'."));
            if (value.getValue() != null) {
                Optional<String> refusal = setting.refusalOf(value.getValue());
                if (refusal.isPresent()) {
                    throw new PluginSettingRefused(setting.title() + " was not changed: " + refusal.get() + ".");
                }
            }
        }
        keeping.keep(plugin, sealed(plugin, settings, values));
        changed.remove(plugin);
    }

    /** The value a plugin's setting stands at, asked for as {@code kind} — null to take it as text, whatever it is. */
    String valueOf(String plugin, String key, PluginSetting.Kind kind) {
        PluginSetting setting = settingOf(declaredBy(plugin), key).orElseThrow(() -> new IllegalArgumentException(
                "Plugin '" + plugin + "' declares no setting '" + key + "'."));
        if (kind != null && setting.kind() != kind) {
            throw new IllegalArgumentException("Setting '" + key + "' of plugin '" + plugin + "' is " + setting.kind()
                                               + ", not " + kind + ".");
        }
        String kept = changedOf(plugin).get(key);
        if (kept == null) {
            return setting.defaultValue();
        }
        return setting.kind() == PluginSetting.Kind.SECRET ? requireSecrets(setting).open(kept, contextOf(plugin, key)) : kept;
    }

    /** The values as they are kept: a secret sealed, a blank one forgotten, as nothing else tells a key from no key. */
    private Map<String, String> sealed(String plugin, List<PluginSetting> settings, Map<String, String> values) {
        Map<String, String> kept = new HashMap<>(values);
        for (Map.Entry<String, String> value : values.entrySet()) {
            PluginSetting setting = settingOf(settings, value.getKey()).orElseThrow();
            if (setting.kind() == PluginSetting.Kind.SECRET && value.getValue() != null) {
                kept.put(value.getKey(), value.getValue().isBlank() ? null
                        : requireSecrets(setting).seal(value.getValue().strip(), contextOf(plugin, value.getKey())));
            }
        }
        return kept;
    }

    private PluginSecrets requireSecrets(PluginSetting setting) {
        return secrets.orElseThrow(() -> new PluginSettingRefused(
                setting.title() + " cannot be kept: this installation has no key to seal it with."));
    }

    private static String contextOf(String plugin, String key) {
        return "plugin-setting:" + plugin + ":" + key;
    }

    private static Optional<PluginSetting> settingOf(List<PluginSetting> settings, String key) {
        return settings.stream().filter(candidate -> candidate.key().equals(key)).findFirst();
    }

    private Map<String, String> changedOf(String plugin) {
        return changed.computeIfAbsent(plugin, unread -> store.map(keeping -> Map.copyOf(keeping.changed(plugin)))
                .orElse(Map.of()));
    }

    private Map<String, List<PluginSetting>> declarations() {
        Map<String, List<PluginSetting>> known = declared;
        if (known == null) {
            Map<String, List<PluginSetting>> read = new LinkedHashMap<>();
            for (Plugin plugin : plugins.get()) {
                read.put(plugin.descriptor().id(), List.copyOf(plugin.settings()));
            }
            known = Map.copyOf(read);
            declared = known;
        }
        return known;
    }
}
