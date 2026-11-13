package org.jmouse.settings;

import java.math.BigDecimal;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 🎚️ One parameter of a section of fixed parameters — how many parts one search asks for, how long a simulation may take, a key
 * to a service —: its key in the section, what a reader is shown of it, the form of its value and its bounds, and the value it
 * starts from. Its value is one row ({@link Settings}).
 *
 * @param key          dotted words of lower-case letters, digits and dashes: {@code limit}, {@code digi-key.site}
 * @param label        what a reader is shown it as
 * @param description  what it changes, in a sentence; null when the label says it
 * @param kind         the form its value takes
 * @param defaultValue its value as the installation is born with it, written as a value of its kind is; a secret's is what the
 *                     deployment gives, never kept
 * @param choices      a {@link ParameterKind#CHOICE}'s values, in their order; empty for every other kind
 * @param unit         what a number counts — {@code parts} —; null for none
 * @param least        the least a number may be; null for no bound
 * @param most         the most a number may be; null for no bound
 */
public record Parameter(String key, String label, String description, ParameterKind kind, String defaultValue,
                        List<Choice> choices, String unit, BigDecimal least, BigDecimal most) {

    /** Dotted words of lower-case letters, digits and dashes. */
    private static final Pattern KEY = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*(\\.[a-z0-9]+(-[a-z0-9]+)*)*");

    /** A value a {@link ParameterKind#CHOICE} may take, and how a reader is shown it. */
    public record Choice(String value, String label) {
    }

    public Parameter {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("A parameter's key is dotted words of lowercase letters, digits and dashes, as limit "
                                               + "or digi-key.site, not '" + key + "'.");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Parameter '" + key + "' says no kind.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Parameter '" + key + "' has no label: a reader is never shown a key.");
        }
        choices = List.copyOf(choices == null ? List.of() : choices);
        if ((kind == ParameterKind.CHOICE) == choices.isEmpty()) {
            throw new IllegalArgumentException("Parameter '" + key + "' is " + kind + ": a choice, and only a choice, lists its values.");
        }
        defaultValue = defaultValue == null ? "" : defaultValue;
        Optional<String> refusal = kind == ParameterKind.SECRET ? Optional.empty() : refusalOf(kind, choices, least, most, defaultValue);
        if (refusal.isPresent()) {
            throw new IllegalArgumentException("Parameter '" + key + "' defaults to a value it refuses: " + refusal.get());
        }
    }

    /** A key to a service; {@code deployed} is what the deployment gave, used while nobody kept another — blank for nothing. */
    public static Parameter secret(String key, String label, String description, String deployed) {
        return new Parameter(key, label, description, ParameterKind.SECRET, deployed, List.of(), null, null, null);
    }

    /** On or off. */
    public static Parameter toggle(String key, String label, String description, boolean defaultValue) {
        return new Parameter(key, label, description, ParameterKind.SWITCH, String.valueOf(defaultValue), List.of(), null, null, null);
    }

    /** A number between {@code least} and {@code most}, counting {@code unit}. */
    public static Parameter number(String key, String label, String description, BigDecimal defaultValue, BigDecimal least,
                                   BigDecimal most, String unit) {
        return new Parameter(key, label, description, ParameterKind.NUMBER, defaultValue.toPlainString(), List.of(), unit, least, most);
    }

    /** A whole number between {@code least} and {@code most}, counting {@code unit}. */
    public static Parameter wholeNumber(String key, String label, String description, long defaultValue, long least, long most,
                                        String unit) {
        return new Parameter(key, label, description, ParameterKind.WHOLE_NUMBER, String.valueOf(defaultValue), List.of(), unit,
                             BigDecimal.valueOf(least), BigDecimal.valueOf(most));
    }

    /** A stretch of time between {@code least} and {@code most}. */
    public static Parameter duration(String key, String label, String description, Duration defaultValue, Duration least,
                                     Duration most) {
        return new Parameter(key, label, description, ParameterKind.DURATION, defaultValue.toString(), List.of(), null,
                             BigDecimal.valueOf(least.toMillis()), BigDecimal.valueOf(most.toMillis()));
    }

    /** One of {@code choices}. */
    public static Parameter choice(String key, String label, String description, String defaultValue, List<Choice> choices) {
        return new Parameter(key, label, description, ParameterKind.CHOICE, defaultValue, choices, null, null, null);
    }

    /** A line of text. */
    public static Parameter text(String key, String label, String description, String defaultValue) {
        return new Parameter(key, label, description, ParameterKind.TEXT, defaultValue, List.of(), null, null, null);
    }

    /** Folders of the server; {@code deployed} is what the deployment gave, none a parameter without any. */
    public static Parameter folders(String key, String label, String description, List<Path> deployed) {
        return new Parameter(key, label, description, ParameterKind.FOLDERS, String.join("\n", deployed.stream().map(Path::toString).toList()),
                             List.of(), null, null, null);
    }

    /** One folder of the server; {@code deployed} is what the deployment gave. */
    public static Parameter folder(String key, String label, String description, Path deployed) {
        return new Parameter(key, label, description, ParameterKind.FOLDER, deployed.toString(), List.of(), null, null, null);
    }

    /** How a program of the server is started, word by word; {@code deployed} is what the deployment gave, none is off. */
    public static Parameter command(String key, String label, String description, List<String> deployed) {
        return new Parameter(key, label, description, ParameterKind.COMMAND, String.join("\n", deployed), List.of(), null, null, null);
    }

    /** The lines of a {@link ParameterKind#FOLDERS} or {@link ParameterKind#COMMAND} value, each stripped; a blank one is none. */
    static List<String> linesOf(String value) {
        return value.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
    }

    /** Why this parameter may not take the value, in words a reader is shown; empty when it may. A duration's bounds are in milliseconds. */
    public Optional<String> refusalOf(String value) {
        return refusalOf(kind, choices, least, most, value);
    }

    private static Optional<String> refusalOf(ParameterKind kind, List<Choice> choices, BigDecimal least, BigDecimal most, String value) {
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
            case FOLDER -> linesOf(value).size() != 1 ? Optional.of("it is one folder")
                    : isWholePath(value.strip()) ? Optional.empty() : Optional.of("'" + value.strip() + "' is not a whole path, from the top of a drive");
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

    private static Optional<String> outOfBounds(ParameterKind kind, BigDecimal least, BigDecimal most, BigDecimal number) {
        if (least != null && number.compareTo(least) < 0) {
            return Optional.of("it is at least " + boundOf(kind, least));
        }
        if (most != null && number.compareTo(most) > 0) {
            return Optional.of("it is at most " + boundOf(kind, most));
        }
        return Optional.empty();
    }

    private static String boundOf(ParameterKind kind, BigDecimal bound) {
        return kind == ParameterKind.DURATION ? Duration.ofMillis(bound.longValue()).toString() : bound.toPlainString();
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
