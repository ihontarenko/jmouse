package org.jmouse.plugin.spring;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * The data a plugin ships in its jar — a profile, its numbers, its words —, read as JSON from beside the class that names
 * it. A plugin's settings are data, never constants in its code; this is the one way they are read.
 */
public final class PluginData {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private PluginData() {
    }

    /**
     * @param owner    the class the resource is shipped beside
     * @param resource its name, relative to the owner's package
     * @throws IllegalStateException when the jar ships no such resource
     */
    public static <T> T read(Class<?> owner, String resource, Class<T> type) {
        try (InputStream stream = owner.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException(owner.getName() + " ships no " + resource);
            }
            return JSON.readValue(stream, type);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }
}
