package org.jmouse.cad.plugin.gerber;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** The data a plugin ships in its jar — a profile, its numbers, its words —, read as JSON beside the class that names it. */
final class PluginResources {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private PluginResources() {
    }

    static <T> T read(Class<?> owner, String resource, Class<T> type) {
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
