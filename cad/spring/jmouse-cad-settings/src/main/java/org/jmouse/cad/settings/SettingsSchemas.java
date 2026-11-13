package org.jmouse.cad.settings;

import org.jmouse.descriptions.Description;
import org.jmouse.descriptions.Descriptions;
import org.jmouse.settings.Label;
import org.jmouse.settings.ParameterTree;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 🎚️ A section's JSON Schema, written from its record: every component a property, described by its {@code @param} as the record's
 * Javadoc says it (kept by {@code jmouse-descriptions} as the module compiles), nested records, lists, maps and optionals followed
 * down, an enum by the words its file says it in. Nothing of it is written by hand, so what a reader is told and what the engine
 * reads cannot drift apart; what cannot be written — a component its Javadoc does not describe, a type no schema says — is a
 * mismatch, which every module's test asks for.
 */
public final class SettingsSchemas {

    /** The draft every schema is written to. */
    public static final String DRAFT = "https://json-schema.org/draft/2020-12/schema";

    /** What a description keeps when an inline tag was not turned into words. */
    private static final Pattern UNWRITTEN_TAG = Pattern.compile("\\{@");

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private static final Map<String, Written> WRITTEN = new ConcurrentHashMap<>();

    private SettingsSchemas() {
    }

    /** A section's schema, written once a process. */
    public static ObjectNode of(SettingsSection<?> section) {
        return written(section).schema().deepCopy();
    }

    /**
     * What is wrong with a section, in words: its file not read into its record, its rows (ADR 0007) not folding back into the
     * record its file is, or its schema not able to say what the record reads; none where all three hold.
     */
    public static List<String> mismatches(SettingsSection<?> section) {
        List<String> found = new ArrayList<>();
        try {
            Object shipped = CadSettings.shipped().read(section);
            Object folded = CadSectionReader.JSON.read(ParameterTree.fold(CadSettings.declarationOf(section).seed()), section.type());
            if (!Objects.deepEquals(shipped, folded)) {
                found.add(section.path() + ": its rows fold back into another " + section.type().getSimpleName() + " than its file is");
            }
        } catch (IllegalStateException | IllegalArgumentException | UncheckedIOException unread) {
            found.add(section.path() + ": " + unread.getMessage());
        }
        found.addAll(written(section).undescribed());
        return found;
    }

    /** Every mismatch of every section given. */
    public static List<String> mismatches(Collection<? extends SettingsSection<?>> sections) {
        return sections.stream().flatMap(section -> mismatches(section).stream()).toList();
    }

    private static Written written(SettingsSection<?> section) {
        return WRITTEN.computeIfAbsent(section.path(), path -> new Writer(path).section(section));
    }

    /** A schema, and what could not be said in it. */
    private record Written(ObjectNode schema, List<String> undescribed) {
    }

    /** Writes one section's schema, noting each thing it cannot say. */
    private static final class Writer {

        private final String path;
        private final List<String> undescribed = new ArrayList<>();

        private Writer(String path) {
            this.path = path;
        }

        Written section(SettingsSection<?> section) {
            ObjectNode schema = NODES.objectNode();
            schema.put("$schema", DRAFT);
            schema.put("$id", "org/jmouse/cad/settings/" + section.path() + ".schema.json");
            schema.put("title", section.title());
            Class<?> type = section.type();
            Class<?> record = type.isArray() ? type.getComponentType() : type;
            String summary = Descriptions.of(record).map(Description::summary).orElse("");
            if (summary.isBlank()) {
                undescribed.add(path + ": " + record.getSimpleName() + "'s Javadoc says nothing of what the section is");
            } else {
                said(path, summary);
                schema.put("description", summary);
            }
            schema.setAll(typeOf(type, path));
            return new Written(schema, List.copyOf(undescribed));
        }

        /** The schema of a value of this type, `where` naming it in what is noted. */
        private ObjectNode typeOf(Type type, String where) {
            Class<?> raw = rawOf(type);
            if (raw.isArray()) {
                return array(typeOf(raw.getComponentType(), where + "[]"));
            }
            if (type instanceof ParameterizedType parameterized && Collection.class.isAssignableFrom(raw)) {
                return array(typeOf(parameterized.getActualTypeArguments()[0], where + "[]"));
            }
            if (type instanceof ParameterizedType parameterized && Map.class.isAssignableFrom(raw)) {
                ObjectNode map = NODES.objectNode().put("type", "object");
                map.set("additionalProperties", typeOf(parameterized.getActualTypeArguments()[1], where + "{}"));
                return map;
            }
            if (type instanceof ParameterizedType parameterized && Optional.class.equals(raw)) {
                ObjectNode inside = typeOf(parameterized.getActualTypeArguments()[0], where);
                JsonNode said = inside.get("type");
                if (said != null && said.isString()) {
                    inside.set("type", NODES.arrayNode().add(said.asString()).add("null"));
                }
                return inside;
            }
            Optional<String> scalar = scalarOf(raw);
            if (scalar.isPresent()) {
                return NODES.objectNode().put("type", scalar.get());
            }
            if (raw.isEnum()) {
                return enumeration(raw);
            }
            if (raw.isRecord()) {
                ObjectNode record = record(raw, where);
                // A record that is a list's item says what one is called: «Paper» → «Paper 1».
                Optional.ofNullable(raw.getAnnotation(Label.class)).ifPresent(label -> record.put("title", label.value()));
                return record;
            }
            undescribed.add(where + ": no schema says a " + raw.getSimpleName());
            return NODES.objectNode();
        }

        private ObjectNode record(Class<?> record, String where) {
            ObjectNode schema = NODES.objectNode().put("type", "object").put("additionalProperties", false);
            ArrayNode required = schema.putArray("required");
            ObjectNode properties = schema.putObject("properties");
            Optional<Description> description = Descriptions.of(record);
            for (RecordComponent component : record.getRecordComponents()) {
                String name = component.getName();
                String at = where + "." + name;
                ObjectNode property = NODES.objectNode();
                Label label = component.getAnnotation(Label.class);
                if (label == null || label.value().isBlank()) {
                    undescribed.add(at + ": " + record.getName() + "'s component " + name + " has no @Label: a reader is never shown a key");
                } else {
                    property.put("title", label.value());
                    if (!label.unit().isBlank()) {
                        property.put("unit", label.unit());
                    }
                }
                Optional<String> words = description.flatMap(said -> said.member(name)).filter(said -> !said.isBlank());
                if (words.isEmpty()) {
                    undescribed.add(at + ": " + record.getSimpleName() + "'s Javadoc has no @param " + name);
                } else {
                    said(at, words.get());
                    property.put("description", words.get());
                }
                property.setAll(typeOf(component.getGenericType(), at));
                properties.set(name, property);
                if (!Optional.class.equals(component.getType())) {
                    required.add(name);
                }
            }
            return schema;
        }

        /** An enum as its file says its constants: the words the settings' reader writes each as. */
        private static ObjectNode enumeration(Class<?> type) {
            ObjectNode schema = NODES.objectNode().put("type", "string");
            ArrayNode words = schema.putArray("enum");
            for (Object constant : type.getEnumConstants()) {
                words.add(CadSectionReader.written(constant));
            }
            return schema;
        }

        private static ObjectNode array(ObjectNode items) {
            ObjectNode array = NODES.objectNode().put("type", "array");
            array.set("items", items);
            return array;
        }

        /** Words a reader is shown must be words: an inline tag the processor left as written is noted. */
        private void said(String where, String words) {
            if (UNWRITTEN_TAG.matcher(words).find()) {
                undescribed.add(where + ": its description keeps a Javadoc tag instead of words");
            }
        }
    }

    /** The JSON type a plain value is written as; empty for one that is not plain. */
    private static Optional<String> scalarOf(Class<?> raw) {
        if (Set.of(double.class, float.class, Double.class, Float.class, BigDecimal.class).contains(raw)) {
            return Optional.of("number");
        }
        if (Set.of(int.class, long.class, short.class, byte.class, Integer.class, Long.class, Short.class, Byte.class).contains(raw)) {
            return Optional.of("integer");
        }
        if (raw == boolean.class || raw == Boolean.class) {
            return Optional.of("boolean");
        }
        if (Set.of(String.class, Path.class, Duration.class, URI.class).contains(raw)) {
            return Optional.of("string");
        }
        return Optional.empty();
    }

    private static Class<?> rawOf(Type type) {
        if (type instanceof Class<?> plain) {
            return plain;
        }
        if (type instanceof ParameterizedType parameterized) {
            return (Class<?>) parameterized.getRawType();
        }
        return Object.class;
    }
}
