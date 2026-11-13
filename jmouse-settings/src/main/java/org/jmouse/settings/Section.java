package org.jmouse.settings;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 🎚️ A section of settings, the unit an owner declares: where it stands ({@code pcb/layout}, {@code connection}), what a reader
 * calls it, whose its values are, and its shape — parameters of fixed keys, or a structured record unfolded into its keys. Its
 * values are rows, one a parameter ({@link Settings}); what each means is the declaration's, never the key's.
 *
 * @param path        words of lower-case letters, digits and dashes, joined by {@code /}
 * @param label       what a reader calls it
 * @param description what it holds, in a sentence; null when the label says it
 * @param scope       whose its values are: the installation's, or each reader's over it
 * @param shape       its parameters, fixed or structured
 */
public record Section(String path, String label, String description, SettingScope scope, Shape shape) {

    private static final Pattern PATH = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*(/[a-z0-9]+(-[a-z0-9]+)*)*");

    /** What a section's parameters are. */
    public sealed interface Shape permits Fixed, Structured {
    }

    /**
     * Parameters of fixed keys, each declared with its kind, its bounds and its default: a limit, a folder, a key to a service.
     *
     * @param parameters in the order a reader is shown them
     */
    public record Fixed(List<Parameter> parameters) implements Shape {

        public Fixed {
            parameters = List.copyOf(parameters);
            if (parameters.stream().map(Parameter::key).distinct().count() != parameters.size()) {
                throw new IllegalArgumentException("A section declares each parameter once.");
            }
        }
    }

    /**
     * A record, its keys unfolded from its seed ({@link ParameterTree}): read back into its type by the application's
     * {@link SectionReader}, which is what checks a change.
     *
     * @param type the record its folded parameters are read into — an array of one for a section that is a list
     * @param seed the parameters the installation is born with: the file its module ships, unfolded
     */
    public record Structured(Class<?> type, Map<String, String> seed) implements Shape {

        public Structured {
            Objects.requireNonNull(type, "a structured section is read into a record");
            seed = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(seed));
        }
    }

    public Section {
        if (path == null || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("A section's path is lower-case words joined by '/', not '" + path + "'.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Section '" + path + "' has no label: a reader is never shown a path.");
        }
        scope = scope == null ? SettingScope.INSTALLATION : scope;
        Objects.requireNonNull(shape, "a section has a shape");
        if (scope == SettingScope.READER && shape instanceof Fixed fixed
            && fixed.parameters().stream().anyMatch(parameter -> parameter.kind() == ParameterKind.SECRET)) {
            throw new IllegalArgumentException("Section '" + path + "' is each reader's: a key to a service is the installation's.");
        }
    }

    /** A section of fixed parameters, the installation's. */
    public static Section fixed(String path, String label, String description, List<Parameter> parameters) {
        return new Section(path, label, description, SettingScope.INSTALLATION, new Fixed(parameters));
    }

    /** A structured section, the installation's. */
    public static Section structured(String path, String label, String description, Class<?> type, Map<String, String> seed) {
        return new Section(path, label, description, SettingScope.INSTALLATION, new Structured(type, seed));
    }

    /** The same section, a value for each reader over the installation's. */
    public Section forEachReader() {
        return new Section(path, label, description, SettingScope.READER, shape);
    }

    /** What the installation is born with, key by key: a structured section's seed, a fixed one's defaults but its secrets'. */
    public Map<String, String> seed() {
        return switch (shape) {
            case Structured structured -> structured.seed();
            case Fixed fixed -> {
                Map<String, String> defaults = new java.util.LinkedHashMap<>();
                fixed.parameters().stream().filter(parameter -> parameter.kind() != ParameterKind.SECRET)
                        .forEach(parameter -> defaults.put(parameter.key(), parameter.defaultValue()));
                yield defaults;
            }
        };
    }
}
