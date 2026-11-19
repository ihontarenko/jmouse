package org.jmouse.java.index.binary;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Limits of the derived API view, independent from classfile input and parser budgets. */
public record BinaryProjectionLimits(
        int maximumOutputCharacters, int maximumCachedNameCharacters, long maximumWork) {
    public BinaryProjectionLimits {
        if (maximumOutputCharacters <= 0 || maximumCachedNameCharacters <= 0 || maximumWork <= 0) {
            throw new IllegalArgumentException("Binary projection limits must be positive");
        }
    }

    public static BinaryProjectionLimits standard() {
        return Defaults.INSTANCE;
    }

    private static final class Defaults {
        private static final BinaryProjectionLimits INSTANCE = read();

        private static BinaryProjectionLimits read() {
            Properties values = new Properties();
            try (InputStream input =
                    BinaryProjectionLimits.class.getResourceAsStream(
                            BinaryProjectionNames.LIMITS_RESOURCE)) {
                if (input == null) {
                    throw new IllegalStateException("Missing binary projection limits");
                }
                values.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read binary projection limits", exception);
            }
            return new BinaryProjectionLimits(
                    Integer.parseInt(
                            values.getProperty(BinaryProjectionNames.MAXIMUM_OUTPUT_CHARACTERS)),
                    Integer.parseInt(
                            values.getProperty(
                                    BinaryProjectionNames.MAXIMUM_CACHED_NAME_CHARACTERS)),
                    Long.parseLong(values.getProperty(BinaryProjectionNames.MAXIMUM_WORK)));
        }
    }
}
