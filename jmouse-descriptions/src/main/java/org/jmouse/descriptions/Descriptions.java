package org.jmouse.descriptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📝 The Javadoc of a record, an enum or a sealed type, read back at run time: the {@link org.jmouse.descriptions.processing.DescriptionsProcessor}
 * keeps it beside the class as {@code META-INF/jmouse/descriptions/<binary name>.properties} while the source compiles, so it
 * travels in the same jar. A type compiled without the processor has none.
 */
public final class Descriptions {

    /** Where a type's description is kept, before its binary name. */
    public static final String RESOURCES = "META-INF/jmouse/descriptions/";

    /** The key of a type's own words. */
    public static final String SUMMARY = "summary";

    /** The key before a member's name. */
    public static final String MEMBER = "member.";

    private static final Map<Class<?>, Optional<Description>> READ = new ConcurrentHashMap<>();

    private Descriptions() {
    }

    /** The resource a type's description is kept in. */
    public static String resourceOf(String binaryName) {
        return RESOURCES + binaryName + ".properties";
    }

    /** What a type's Javadoc says; empty where it was compiled without the processor. Read once a process. */
    /** The property a getter is known by, as Jackson writes it: {@code isUnplated} → {@code unplated}, {@code getSurface} → {@code surface}. */
    public static String propertyOf(String getter) {
        String bare = getter.startsWith("is") ? getter.substring(2) : getter.startsWith("get") ? getter.substring(3) : getter;
        return bare.isEmpty() ? getter : Character.toLowerCase(bare.charAt(0)) + bare.substring(1);
    }

    public static Optional<Description> of(Class<?> type) {
        return READ.computeIfAbsent(type, Descriptions::read);
    }

    private static Optional<Description> read(Class<?> type) {
        ClassLoader loader = type.getClassLoader() == null ? ClassLoader.getSystemClassLoader() : type.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(resourceOf(type.getName()))) {
            if (stream == null) {
                return Optional.empty();
            }
            Properties kept = new Properties();
            kept.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            Map<String, String> members = new LinkedHashMap<>();
            for (String key : kept.stringPropertyNames()) {
                if (key.startsWith(MEMBER)) {
                    members.put(key.substring(MEMBER.length()), kept.getProperty(key));
                }
            }
            return Optional.of(new Description(kept.getProperty(SUMMARY, ""), members));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }
}
