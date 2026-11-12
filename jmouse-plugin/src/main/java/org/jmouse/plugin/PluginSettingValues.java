package org.jmouse.plugin;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * 🎚️ A plugin's settings as the plugin reads them: each value as it stands at the moment it is read — the default, or
 * what it was changed to —, so a plugin that reads its setting each time it is used applies a change at once.
 *
 * <p>Asking for a key the plugin does not declare, or for a value in a form its kind does not take, is the plugin's own
 * mistake, and fails loudly.</p>
 */
public final class PluginSettingValues {

    private final PluginSettings settings;
    private final String         plugin;

    PluginSettingValues(PluginSettings settings, String plugin) {
        this.settings = settings;
        this.plugin = plugin;
    }

    public String text(String key) {
        return settings.valueOf(plugin, key, null);
    }

    /** A secret setting in the clear, for the call it is used in: keep it no longer. */
    public String secret(String key) {
        return settings.valueOf(plugin, key, PluginSetting.Kind.SECRET);
    }

    public boolean enabled(String key) {
        return Boolean.parseBoolean(settings.valueOf(plugin, key, PluginSetting.Kind.SWITCH));
    }

    public long wholeNumber(String key) {
        return new BigDecimal(settings.valueOf(plugin, key, PluginSetting.Kind.WHOLE_NUMBER)).longValueExact();
    }

    public BigDecimal number(String key) {
        return new BigDecimal(settings.valueOf(plugin, key, PluginSetting.Kind.NUMBER));
    }

    public Duration duration(String key) {
        return Duration.parse(settings.valueOf(plugin, key, PluginSetting.Kind.DURATION));
    }

    /** The folders of the server the setting names, in its order; none is an empty list. */
    public List<Path> folders(String key) {
        return PluginSetting.linesOf(settings.valueOf(plugin, key, PluginSetting.Kind.FOLDERS)).stream().map(Path::of).toList();
    }

    /** How the program the setting names is started, word by word; empty for no program. */
    public List<String> command(String key) {
        return PluginSetting.linesOf(settings.valueOf(plugin, key, PluginSetting.Kind.COMMAND));
    }
}
