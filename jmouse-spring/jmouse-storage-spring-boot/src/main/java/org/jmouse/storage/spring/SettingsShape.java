package org.jmouse.storage.spring;

import java.lang.reflect.RecordComponent;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 🗺️ What shape the settings record has, so the adapter can translate Spring's conventions only
 * where they apply.
 *
 * <p>Spring configuration is written with conveniences the JDK does not share: durations as
 * {@code 15m}, lists as {@code a,b,c}, names in kebab-case. Each needs translating on the way in —
 * and each needs translating <em>only</em> at the properties that expect it, or a bucket named
 * {@code 15m} becomes a duration and an access key containing a comma becomes three access keys.</p>
 *
 * <p>The paths are derived from the record itself rather than listed by hand, so a new setting is
 * handled because it is declared, not because somebody remembered to add it here.</p>
 */
public final class SettingsShape {

    private static final String SEPARATOR = ".";

    private final Set<String> durationPaths   = new HashSet<>();
    private final Set<String> collectionPaths = new HashSet<>();

    private SettingsShape() {
    }

    /**
     * 🔎 Walk a settings record and record where each convention applies.
     *
     * @param settingsType the record to walk
     * @return its shape
     */
    public static SettingsShape of(Class<?> settingsType) {
        SettingsShape shape = new SettingsShape();
        shape.collect(settingsType, "", new HashSet<>());
        return shape;
    }

    /**
     * ⏱️ Whether a property holds an amount of time, and so needs Spring's shorthand translating.
     *
     * @param path camelCased property path, prefix already removed
     * @return {@code true} when the value is a duration
     */
    public boolean isDuration(String path) {
        return matches(path, durationPaths);
    }

    /**
     * 📃 Whether a property holds several values, and so may arrive comma-separated.
     *
     * @param path camelCased property path, prefix already removed
     * @return {@code true} when the value is a collection
     */
    public boolean isCollection(String path) {
        return matches(path, collectionPaths);
    }

    /**
     * 🎯 Match a path against a known set, by the whole path or by its tail.
     *
     * <p>The tail matters because a named backend's settings sit under a key nobody can know up
     * front: {@code backends.archive.s3.linkTimeToLive} is the same setting as
     * {@code s3.linkTimeToLive} and has to be read the same way.</p>
     *
     * @param path      the property path
     * @param candidates paths of that kind
     * @return {@code true} on a whole or tail match
     */
    private boolean matches(String path, Set<String> candidates) {
        if (candidates.contains(path)) {
            return true;
        }

        return candidates.stream().anyMatch(candidate -> path.endsWith(SEPARATOR + candidate));
    }

    /**
     * 🪆 Walk a record's components, recording paths and descending into nested records.
     *
     * @param type    record type being walked
     * @param prefix  path accumulated so far
     * @param visited types already walked, so a self-referential shape cannot loop forever
     */
    private void collect(Class<?> type, String prefix, Set<Class<?>> visited) {
        if (!type.isRecord() || !visited.add(type)) {
            return;
        }

        for (RecordComponent component : type.getRecordComponents()) {
            String   path          = prefix.isEmpty()
                    ? component.getName()
                    : prefix + SEPARATOR + component.getName();
            Class<?> componentType = component.getType();

            if (componentType == Duration.class) {
                durationPaths.add(path);
                continue;
            }

            if (Collection.class.isAssignableFrom(componentType)) {
                collectionPaths.add(path);
                continue;
            }

            // A map of nested settings: every entry is a record of its own, reached through a key
            // nobody can know up front, so its inner paths are matched by tail instead.
            if (Map.class.isAssignableFrom(componentType)) {
                continue;
            }

            collect(componentType, path, visited);
        }

        visited.remove(type);
    }
}
