package org.jmouse.settings;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * 🎚️ A section of fixed parameters as its owner reads it: each value as it stands at the moment it is read, so a section read each
 * time it is used applies a change at once. Asking for a key the section does not declare, or for a value in a form its kind
 * does not take, is the owner's own mistake, and fails loudly.
 */
public final class ParameterValues {

    private final Settings      settings;
    private final String        owner;
    private final String        section;
    private final SettingHolder holder;

    ParameterValues(Settings settings, String owner, String section, SettingHolder holder) {
        this.settings = settings;
        this.owner = owner;
        this.section = section;
        this.holder = holder;
    }

    public String text(String key) {
        return settings.parameterOf(owner, section, key, null, holder);
    }

    /** A secret in the clear, for the call it is used in: keep it no longer. */
    public String secret(String key) {
        return settings.parameterOf(owner, section, key, ParameterKind.SECRET, holder);
    }

    public boolean enabled(String key) {
        return Boolean.parseBoolean(settings.parameterOf(owner, section, key, ParameterKind.SWITCH, holder));
    }

    public long wholeNumber(String key) {
        return new BigDecimal(settings.parameterOf(owner, section, key, ParameterKind.WHOLE_NUMBER, holder)).longValueExact();
    }

    public BigDecimal number(String key) {
        return new BigDecimal(settings.parameterOf(owner, section, key, ParameterKind.NUMBER, holder));
    }

    public Duration duration(String key) {
        return Duration.parse(settings.parameterOf(owner, section, key, ParameterKind.DURATION, holder));
    }

    /** The folders of the server the parameter names, in its order; none is an empty list. */
    public List<Path> folders(String key) {
        return Parameter.linesOf(settings.parameterOf(owner, section, key, ParameterKind.FOLDERS, holder)).stream().map(Path::of).toList();
    }

    /** The one folder of the server the parameter names. */
    public Path folder(String key) {
        return Path.of(settings.parameterOf(owner, section, key, ParameterKind.FOLDER, holder).strip());
    }

    /** How the program the parameter names is started, word by word; empty for no program. */
    public List<String> command(String key) {
        return Parameter.linesOf(settings.parameterOf(owner, section, key, ParameterKind.COMMAND, holder));
    }
}
