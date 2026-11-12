package org.jmouse.cad.settings;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎚️ The engine's settings tree (ADR 0004): every section read once from {@code org/jmouse/cad/settings/<path>.json} into its
 * record — refusing a field the record does not have and a value missing from one it does —, and the sections a server lists,
 * the core's and every plugin's. A value is read here and nowhere else: a module that computes with a section asks for it, never
 * for its file.
 */
public final class CadSettings {

    /** The key a file names its schema by, for an editor to check it against: no value of the section. */
    public static final String SCHEMA_REFERENCE = "$schema";

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .build();

    /** Every section read so far, by path: a file is read once a process. */
    private static final Map<String, Object> READ = new ConcurrentHashMap<>();

    private final Map<String, SettingsSection<?>> sections = new LinkedHashMap<>();

    /** A tree of these sections; two of one path are one declared twice, refused. */
    public CadSettings(Collection<? extends SettingsSection<?>> declared) {
        for (SettingsSection<?> section : declared) {
            SettingsSection<?> before = sections.putIfAbsent(section.path(), section);
            if (before != null && !before.equals(section)) {
                throw new IllegalStateException("Two settings sections claim " + section.path() + ": " + before.title() + " and " + section.title());
            }
        }
    }

    /**
     * A section's values, read once: its file in whichever module declares it.
     *
     * @throws IllegalStateException when no module ships its file, or the file is not its record
     */
    public static <T> T read(SettingsSection<T> section) {
        return section.type().cast(READ.computeIfAbsent(section.path(), path -> {
            try (InputStream stream = section.type().getResourceAsStream(section.resource())) {
                if (stream == null) {
                    throw new IllegalStateException("No module ships the settings " + section.path() + " (" + section.resource() + ")");
                }
                JsonNode values = JSON.readTree(stream);
                if (values instanceof ObjectNode object) {
                    object.remove(SCHEMA_REFERENCE);
                }
                return JSON.treeToValue(values, section.type());
            } catch (IOException unreadable) {
                throw new UncheckedIOException(unreadable);
            } catch (RuntimeException refused) {
                throw new IllegalStateException("The settings " + section.path() + " are not " + section.type().getSimpleName() + ": "
                                                + refused.getMessage(), refused);
            }
        }));
    }

    /** A section's JSON Schema; empty where its module ships none. */
    public static Optional<JsonNode> schema(SettingsSection<?> section) {
        try (InputStream stream = section.type().getResourceAsStream(section.schema())) {
            return stream == null ? Optional.empty() : Optional.of(JSON.readTree(stream));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /** Every section of the tree, by path in the tree's order. */
    public List<SettingsSection<?>> sections() {
        return sections.values().stream().sorted(Comparator.comparing(SettingsSection::path)).toList();
    }

    /** The section at a path; empty for none. */
    public Optional<SettingsSection<?>> section(String path) {
        return Optional.ofNullable(sections.get(path));
    }

    /** Every section of the tree read: what a server checks before it answers anything. */
    public void readEvery() {
        sections.values().forEach(CadSettings::read);
    }
}
