package org.jmouse.java.bytecode.validation;

import org.jmouse.java.model.version.ClassFileVersion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Historic format boundaries are specification data, independent from the selected target release.
 */
public final class JvmValidationVersions {
    private static final String RESOURCE = "classfile-versions.properties";
    private static final Properties VERSIONS = read();

    public static final int CLASS_LITERALS = major(Boundary.CLASS_LITERALS);
    public static final int METHOD_HANDLES = major(Boundary.METHOD_HANDLES);
    public static final int INTERFACE_STATIC_SPECIAL_CALLS =
            major(Boundary.INTERFACE_STATIC_SPECIAL_CALLS);
    public static final int STATIC_CLASS_INITIALIZER = major(Boundary.STATIC_CLASS_INITIALIZER);
    public static final int MODULES = major(Boundary.MODULES);
    public static final int DYNAMIC_CONSTANTS = major(Boundary.DYNAMIC_CONSTANTS);
    public static final int STRICT_MINIMUM = major(Boundary.STRICT_MINIMUM);
    public static final int STRICT_MAXIMUM = major(Boundary.STRICT_MAXIMUM);

    private enum Boundary {
        CLASS_LITERALS,
        METHOD_HANDLES,
        INTERFACE_STATIC_SPECIAL_CALLS,
        STATIC_CLASS_INITIALIZER,
        MODULES,
        DYNAMIC_CONSTANTS,
        STRICT_MINIMUM,
        STRICT_MAXIMUM
    }

    private JvmValidationVersions() {}

    public static boolean strictSemantics(ClassFileVersion version) {
        return version.major() >= STRICT_MINIMUM && version.major() <= STRICT_MAXIMUM;
    }

    private static int major(Boundary boundary) {
        return Integer.parseInt(VERSIONS.getProperty(boundary.name()));
    }

    private static Properties read() {
        Properties properties = new Properties();
        try (InputStream input = JvmValidationVersions.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing classfile constraint versions");
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read classfile constraint versions", exception);
        }
        for (Boundary boundary : Boundary.values()) {
            String value = properties.getProperty(boundary.name());
            if (value == null || Integer.parseInt(value) <= 0) {
                throw new IllegalStateException(
                        "Invalid classfile constraint version: " + boundary);
            }
        }
        return properties;
    }
}
