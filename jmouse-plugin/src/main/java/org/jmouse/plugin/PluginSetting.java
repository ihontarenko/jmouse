package org.jmouse.plugin;

import java.math.BigDecimal;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * 🎚️ One thing a plugin may be set to by the people who run it, while it runs — how many parts one search asks for, how
 * long a simulation may take —: declared by the plugin ({@link Plugin#settings()}), kept by the application
 * ({@link PluginSettingStore}), and read by the plugin each time it is used ({@link PluginSettingValues}), so a change
 * applies at once, without a restart.
 *
 * <p>A deployment's value is usually the setting's {@code defaultValue}: the setting changes it, and forgetting the
 * change brings it back. A key to a service is a {@link Kind#SECRET}: sealed where it is kept, and never again to
 * anybody, its default included. What names what the server has — {@link Kind#FOLDERS}, {@link Kind#COMMAND} — is told
 * only to whoever may change it ({@link Kind#namesTheServer()}).</p>
 *
 * @param key          one word of the plugin's, as its values are kept: {@code limit}, {@code timeout}
 * @param title        how a reader is shown it
 * @param description  what it changes, in a sentence; null when the title says it
 * @param kind         the form its value takes
 * @param defaultValue its value while nobody changed it, written as a value of its kind is
 * @param choices      a {@link Kind#CHOICE}'s values, in their order; empty for every other kind
 * @param unit         what a number counts — {@code parts} —; null for none
 * @param least        the least a number may be; null for no bound
 * @param most         the most a number may be; null for no bound
 * @param group        what a reader finds it under among the plugin's others — {@code Connection} —; null for none
 */
public record PluginSetting(String key, String title, String description, Kind kind, String defaultValue,
                            List<Choice> choices, String unit, BigDecimal least, BigDecimal most, String group) {

    /** The form a setting's value takes, and how it is written as text. */
    public enum Kind {
        /** On or off: {@code true}, {@code false}. */
        SWITCH,
        /** A whole number: {@code 24}. */
        WHOLE_NUMBER,
        /** A number: {@code 0.25}. */
        NUMBER,
        /** A stretch of time, as ISO-8601 writes it: {@code PT60S}. */
        DURATION,
        /** A line of text. */
        TEXT,
        /** One of the setting's {@link Choice}s, by its value. */
        CHOICE,
        /** A key to a service: written, used by the plugin, and never read back. */
        SECRET,
        /** Folders of the server, each whole, one to a line. */
        FOLDERS,
        /** How a program of the server is started, one word to a line: a word may hold spaces, as a path does. */
        COMMAND;

        /** Whether its value says what the server has: told only to whoever may change it, as an address to attack. */
        public boolean namesTheServer() {
            return this == FOLDERS || this == COMMAND;
        }
    }

    /** A value a {@link Kind#CHOICE} may take, and how a reader is shown it. */
    public record Choice(String value, String title) {
    }

    public PluginSetting {
        // One word, so it is one key beside the plugin's id: cad-nexar.limit.
        if (key == null || !key.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("A setting's key is one word of lowercase letters, digits and dashes, as "
                                               + "limit, not '" + key + "'.");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Setting '" + key + "' says no kind.");
        }
        title = title == null || title.isBlank() ? key : title;
        choices = List.copyOf(choices == null ? List.of() : choices);
        if ((kind == Kind.CHOICE) == choices.isEmpty()) {
            throw new IllegalArgumentException("Setting '" + key + "' is " + kind + ": a choice, and only a choice, lists "
                                               + "its values.");
        }
        Optional<String> refusal = refusalOf(kind, choices, least, most, defaultValue);
        if (refusal.isPresent()) {
            throw new IllegalArgumentException("Setting '" + key + "' defaults to a value it refuses: " + refusal.get());
        }
    }

    /** The same setting, found under a group among the plugin's others. */
    public PluginSetting inGroup(String name) {
        return new PluginSetting(key, title, description, kind, defaultValue, choices, unit, least, most, name);
    }

    /**
     * A key to a service.
     *
     * @param deployed what the deployment gave, used while nobody kept another; blank for nothing
     */
    public static PluginSetting secret(String key, String title, String description, String deployed) {
        return new PluginSetting(key, title, description, Kind.SECRET, deployed == null ? "" : deployed, List.of(), null, null, null, null);
    }

    /** A setting that is on or off. */
    public static PluginSetting toggle(String key, String title, String description, boolean defaultValue) {
        return new PluginSetting(key, title, description, Kind.SWITCH, String.valueOf(defaultValue), List.of(), null,
                                 null, null, null);
    }

    /** A number between {@code least} and {@code most}, counting {@code unit}. */
    public static PluginSetting number(String key, String title, String description, BigDecimal defaultValue, BigDecimal least,
                                       BigDecimal most, String unit) {
        return new PluginSetting(key, title, description, Kind.NUMBER, defaultValue.toPlainString(), List.of(), unit, least, most, null);
    }

    /** A whole number between {@code least} and {@code most}, counting {@code unit}. */
    public static PluginSetting wholeNumber(String key, String title, String description, long defaultValue, long least,
                                            long most, String unit) {
        return new PluginSetting(key, title, description, Kind.WHOLE_NUMBER, String.valueOf(defaultValue), List.of(),
                                 unit, BigDecimal.valueOf(least), BigDecimal.valueOf(most), null);
    }

    /** A stretch of time between {@code least} and {@code most}. */
    public static PluginSetting duration(String key, String title, String description, Duration defaultValue,
                                         Duration least, Duration most) {
        return new PluginSetting(key, title, description, Kind.DURATION, defaultValue.toString(), List.of(), null,
                                 BigDecimal.valueOf(least.toMillis()), BigDecimal.valueOf(most.toMillis()), null);
    }

    /** One of {@code choices}. */
    public static PluginSetting choice(String key, String title, String description, String defaultValue,
                                       List<Choice> choices) {
        return new PluginSetting(key, title, description, Kind.CHOICE, defaultValue, choices, null, null, null, null);
    }

    /** A line of text. */
    public static PluginSetting text(String key, String title, String description, String defaultValue) {
        return new PluginSetting(key, title, description, Kind.TEXT, defaultValue, List.of(), null, null, null, null);
    }

    /** Folders of the server; {@code deployed} is what the deployment gave, and none is a plugin without any. */
    public static PluginSetting folders(String key, String title, String description, List<Path> deployed) {
        return new PluginSetting(key, title, description, Kind.FOLDERS, joined(deployed.stream().map(Path::toString).toList()),
                                 List.of(), null, null, null, null);
    }

    /** How a program of the server is started, word by word; {@code deployed} is what the deployment gave, none is off. */
    public static PluginSetting command(String key, String title, String description, List<String> deployed) {
        return new PluginSetting(key, title, description, Kind.COMMAND, joined(deployed), List.of(), null, null, null, null);
    }

    private static String joined(List<String> lines) {
        return String.join("\n", lines);
    }

    /** The lines of a {@link Kind#FOLDERS} or {@link Kind#COMMAND} value, each stripped; a blank one is no line. */
    static List<String> linesOf(String value) {
        return value.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
    }

    /**
     * Why this setting may not take the value, in words a reader is shown; empty when it may. A duration's bounds are in
     * milliseconds.
     */
    public Optional<String> refusalOf(String value) {
        return refusalOf(kind, choices, least, most, value);
    }

    private static Optional<String> refusalOf(Kind kind, List<Choice> choices, BigDecimal least, BigDecimal most,
                                              String value) {
        if (value == null) {
            return Optional.of("it has no value");
        }
        return switch (kind) {
            case SWITCH -> value.equals("true") || value.equals("false") ? Optional.empty()
                    : Optional.of("'" + value + "' is neither true nor false");
            case WHOLE_NUMBER -> numberOf(value).filter(number -> number.stripTrailingZeros().scale() <= 0)
                    .map(number -> outOfBounds(kind, least, most, number))
                    .orElse(Optional.of("'" + value + "' is not a whole number"));
            case NUMBER -> numberOf(value).map(number -> outOfBounds(kind, least, most, number))
                    .orElse(Optional.of("'" + value + "' is not a number"));
            case DURATION -> durationOf(value)
                    .map(duration -> outOfBounds(kind, least, most, BigDecimal.valueOf(duration.toMillis())))
                    .orElse(Optional.of("'" + value + "' is not a stretch of time, as PT60S is"));
            case TEXT, SECRET, COMMAND -> Optional.empty();
            case FOLDERS -> linesOf(value).stream().filter(line -> !isWholePath(line)).findFirst()
                    .map(line -> "'" + line + "' is not a whole path, from the top of a drive");
            case CHOICE -> choices.stream().anyMatch(choice -> choice.value().equals(value)) ? Optional.empty()
                    : Optional.of("'" + value + "' is none of its choices");
        };
    }

    private static boolean isWholePath(String line) {
        try {
            return Path.of(line).isAbsolute();
        } catch (InvalidPathException notAPath) {
            return false;
        }
    }

    private static Optional<String> outOfBounds(Kind kind, BigDecimal least, BigDecimal most, BigDecimal number) {
        if (least != null && number.compareTo(least) < 0) {
            return Optional.of("it is at least " + boundOf(kind, least));
        }
        if (most != null && number.compareTo(most) > 0) {
            return Optional.of("it is at most " + boundOf(kind, most));
        }
        return Optional.empty();
    }

    private static String boundOf(Kind kind, BigDecimal bound) {
        return kind == Kind.DURATION ? Duration.ofMillis(bound.longValue()).toString() : bound.toPlainString();
    }

    static Optional<BigDecimal> numberOf(String value) {
        try {
            return Optional.of(new BigDecimal(value.trim()));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    static Optional<Duration> durationOf(String value) {
        try {
            return Optional.of(Duration.parse(value.trim()));
        } catch (DateTimeParseException notADuration) {
            return Optional.empty();
        }
    }
}
