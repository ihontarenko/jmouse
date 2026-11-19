package org.jmouse.java.model.descriptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/** Canonical JVM primitive encodings are specification data. */
public final class JavaDescriptorCodes {
    private static final Properties CODES = read();

    private JavaDescriptorCodes() {}

    public static String forKeyword(String keyword) {
        return Objects.requireNonNull(
                CODES.getProperty(keyword), "Missing JVM descriptor for " + keyword);
    }

    private static Properties read() {
        Properties properties = new Properties();
        try (InputStream stream =
                JavaDescriptorCodes.class.getResourceAsStream(
                        "/org/jmouse/java/model/primitive-descriptors.properties")) {
            if (stream == null) {
                throw new IllegalStateException("Missing primitive descriptor vocabulary");
            }
            properties.load(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read primitive descriptors", exception);
        }
        return properties;
    }
}
