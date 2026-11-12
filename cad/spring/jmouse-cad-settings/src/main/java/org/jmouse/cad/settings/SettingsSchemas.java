package org.jmouse.cad.settings;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Whether a section is read and its JSON Schema says what its record reads: every component a property with words describing it, nothing
 * described that is not read — nested records, lists and maps of them followed down. What a reader of the file is told and what
 * the engine reads cannot drift apart: every module's test asks this of its sections.
 */
public final class SettingsSchemas {

    private SettingsSchemas() {
    }

    /**
     * What is wrong with a section, in words: its file not read into its record, or its schema not saying what the record reads;
     * none where both hold.
     */
    public static List<String> mismatches(SettingsSection<?> section) {
        List<String> found = new ArrayList<>();
        try {
            CadSettings.read(section);
        } catch (IllegalStateException | UncheckedIOException unread) {
            found.add(unread.getMessage());
        }
        Optional<JsonNode> schema = CadSettings.schema(section);
        if (schema.isEmpty()) {
            found.add(section.path() + ": no schema at " + section.schema());
            return found;
        }
        if (!schema.get().path("description").isString()) {
            found.add(section.path() + ": the schema says nothing of what the section is");
        }
        JsonNode values = schema.get().deepCopy();
        if (values.path("properties") instanceof ObjectNode properties) {
            properties.remove(CadSettings.SCHEMA_REFERENCE);
        }
        compare(section.path(), section.type(), values, found);
        return found;
    }

    /** Every mismatch of every section given. */
    public static List<String> mismatches(Collection<? extends SettingsSection<?>> sections) {
        return sections.stream().flatMap(section -> mismatches(section).stream()).toList();
    }

    private static void compare(String where, Type type, JsonNode schema, List<String> found) {
        Class<?> raw = rawOf(type);
        if (raw.isArray()) {
            compare(where + "[]", raw.getComponentType(), schema.path("items"), found);
            return;
        }
        if (type instanceof ParameterizedType parameterized && Collection.class.isAssignableFrom(raw)) {
            compare(where + "[]", parameterized.getActualTypeArguments()[0], schema.path("items"), found);
            return;
        }
        if (type instanceof ParameterizedType parameterized && Map.class.isAssignableFrom(raw)) {
            compare(where + "{}", parameterized.getActualTypeArguments()[1], schema.path("additionalProperties"), found);
            return;
        }
        if (type instanceof ParameterizedType parameterized && Optional.class.equals(raw)) {
            compare(where, parameterized.getActualTypeArguments()[0], schema, found);
            return;
        }
        if (!raw.isRecord()) {
            return;
        }
        JsonNode properties = schema.path("properties");
        if (!properties.isObject()) {
            found.add(where + ": the schema gives no properties for " + raw.getSimpleName());
            return;
        }
        Set<String> read = new HashSet<>();
        for (RecordComponent component : raw.getRecordComponents()) {
            read.add(component.getName());
            JsonNode property = properties.path(component.getName());
            if (property.isMissingNode()) {
                found.add(where + "." + component.getName() + ": not in the schema");
                continue;
            }
            if (!property.path("description").isString() || property.path("description").asString().isBlank()) {
                found.add(where + "." + component.getName() + ": the schema does not say what it is");
            }
            compare(where + "." + component.getName(), component.getGenericType(), property, found);
        }
        properties.propertyNames().forEach(name -> {
            if (!read.contains(name)) {
                found.add(where + "." + name + ": in the schema, but " + raw.getSimpleName() + " reads no such value");
            }
        });
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
