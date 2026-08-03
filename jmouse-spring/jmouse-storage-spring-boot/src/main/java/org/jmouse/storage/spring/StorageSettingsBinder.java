package org.jmouse.storage.spring;

import org.jmouse.storage.configuration.StorageSettings;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 🔗 Reads the library's settings out of Spring's environment, under whatever prefix the product
 * already publishes.
 *
 * <p>This exists so that adopting the library renames nobody's properties. Innoventa keeps
 * {@code innoventa.file.*}, Moneta keeps its own, and a third product picks a third — the prefix is
 * configuration, not a constant baked into the library.</p>
 *
 * <h3>Why not {@code @ConfigurationProperties}</h3>
 *
 * <p>Because the settings type lives in a module that must work without Spring at all. Binding it
 * through jMouse's own binder means one record serves a Spring application and a jMouse one; the
 * only Spring-shaped part is getting the values out of {@link Environment}, which is exactly what
 * this class is and nothing more.</p>
 *
 * <h3>Relaxed names</h3>
 *
 * <p>Spring configuration is written in kebab-case ({@code storage-directory}) and Java records are
 * declared in camelCase ({@code storageDirectory}). Spring's own binder papers over that; jMouse's
 * matches component names directly. Rather than annotate every component with an alias — which
 * would put a Spring convention into a Spring-free module — the conversion happens here, on the way
 * in, along with assembling the flat dotted keys into the nested shape the binder reads.</p>
 */
public final class StorageSettingsBinder {

    /**
     * 🏷️ Property naming the prefix everything else is read from.
     */
    public static final String PREFIX_PROPERTY = "jmouse.storage.prefix";

    /**
     * 🏷️ Prefix used when a product does not name one.
     */
    public static final String DEFAULT_PREFIX = "jmouse.storage";

    private static final String SEPARATOR      = ".";
    private static final String SEPARATOR_EXPRESSION = "\\.";
    private static final char   KEBAB_HYPHEN   = '-';
    private static final char   SNAKE_UNDERSCORE = '_';
    private static final String ROOT_KEY       = "storage";

    private StorageSettingsBinder() {
    }

    /**
     * 🏷️ The prefix this application publishes its storage settings under.
     *
     * @param environment the application environment
     * @return the configured prefix, or {@link #DEFAULT_PREFIX}
     */
    public static String prefixOf(Environment environment) {
        return environment.getProperty(PREFIX_PROPERTY, DEFAULT_PREFIX);
    }

    /**
     * 🔗 Bind the settings.
     *
     * @param environment the application environment
     * @return the settings, defaulted wherever configuration said nothing
     */
    public static StorageSettings bind(ConfigurableEnvironment environment) {
        String              prefix         = prefixOf(environment);
        Set<String>         durationPaths  = SpringDurations.durationPathsOf(StorageSettings.class);
        Map<String, Object> nested         = new HashMap<>();

        for (String propertyName : propertyNamesUnder(environment, prefix)) {
            String relative = camelCasePath(propertyName.substring(prefix.length() + SEPARATOR.length()));
            Object value    = environment.getProperty(propertyName);

            place(nested, relative, isDuration(relative, durationPaths) && value != null
                    ? SpringDurations.normalize(value.toString())
                    : value);
        }

        return StorageSettings.bind(Map.of(ROOT_KEY, nested), ROOT_KEY);
    }

    /**
     * ⏱️ Whether a property holds a duration and so needs Spring's shorthand translating.
     *
     * <p>Matched on the tail as well as the whole path, because a named backend's settings sit
     * under a key nobody can know up front — {@code backends.archive.s3.linkTimeToLive} is the same
     * setting as {@code s3.linkTimeToLive} and must be read the same way.</p>
     *
     * @param path          camelCased property path, prefix already removed
     * @param durationPaths paths the settings record declares as durations
     * @return {@code true} when the value should be normalised
     */
    private static boolean isDuration(String path, Set<String> durationPaths) {
        if (durationPaths.contains(path)) {
            return true;
        }

        return durationPaths.stream().anyMatch(candidate -> path.endsWith(SEPARATOR + candidate));
    }

    /**
     * 🐫 A whole dotted path with every segment converted to its Java component name.
     *
     * @param relative property path with the prefix removed
     * @return the camelCased path
     */
    private static String camelCasePath(String relative) {
        String[]      segments  = relative.split(SEPARATOR_EXPRESSION);
        StringBuilder converted = new StringBuilder(relative.length());

        for (String segment : segments) {
            if (!converted.isEmpty()) {
                converted.append(SEPARATOR);
            }

            converted.append(camelCase(segment));
        }

        return converted.toString();
    }

    /**
     * 🔎 Every property name the environment publishes under a prefix.
     *
     * <p>Only enumerable sources can be walked, which is a real limitation rather than an oversight:
     * a source that cannot list its keys — a system-property fallback, some remote configuration
     * clients — can still be read by name, but nothing can discover a name it was never told about.
     * In practice storage settings come from files and environment variables, both enumerable.</p>
     *
     * @param environment the application environment
     * @param prefix      prefix to collect under
     * @return the matching property names
     */
    private static Set<String> propertyNamesUnder(ConfigurableEnvironment environment, String prefix) {
        String        qualified = prefix + SEPARATOR;
        Set<String> names     = new LinkedHashSet<>();

        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String propertyName : enumerable.getPropertyNames()) {
                    if (propertyName.startsWith(qualified)) {
                        names.add(propertyName);
                    }
                }
            }
        }

        return names;
    }

    /**
     * 🪆 Place one already-camelCased dotted key into the nested map.
     *
     * <p>A segment that collides with an already-placed scalar is dropped rather than allowed to
     * overwrite a branch: {@code s3=x} alongside {@code s3.bucket=y} is a configuration mistake,
     * and silently letting either win would hide it.</p>
     *
     * @param root     map being assembled
     * @param relative camelCased key with the prefix already removed
     * @param value    the property's value
     */
    @SuppressWarnings("unchecked")
    private static void place(Map<String, Object> root, String relative, Object value) {
        String[]            segments = relative.split(SEPARATOR_EXPRESSION);
        Map<String, Object> current  = root;

        for (int index = 0; index < segments.length - 1; index++) {
            Object existing = current.computeIfAbsent(segments[index],
                                                      branch -> new HashMap<String, Object>());

            if (!(existing instanceof Map)) {
                return;
            }

            current = (Map<String, Object>) existing;
        }

        current.put(segments[segments.length - 1], value);
    }

    /**
     * 🐫 Convert a kebab-case or snake_case configuration segment to a Java component name.
     *
     * <p>A segment already in camelCase passes through unchanged, so both spellings work and a
     * product writing {@code storageDirectory} in YAML is not punished for it.</p>
     *
     * @param segment one dot-separated part of a property name
     * @return the camelCase form
     */
    private static String camelCase(String segment) {
        StringBuilder converted = new StringBuilder(segment.length());
        boolean       upperNext = false;

        for (char character : segment.toCharArray()) {
            if (character == KEBAB_HYPHEN || character == SNAKE_UNDERSCORE) {
                upperNext = true;
                continue;
            }

            converted.append(upperNext ? Character.toUpperCase(character) : character);
            upperNext = false;
        }

        return converted.toString();
    }
}
