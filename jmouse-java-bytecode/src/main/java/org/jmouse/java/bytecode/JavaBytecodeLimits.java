package org.jmouse.java.bytecode;

import org.jmouse.el.parser.ParsingBudget;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record JavaBytecodeLimits(int maximumInputBytes, ParsingBudget parsing) {
    public JavaBytecodeLimits {
        if (maximumInputBytes <= 0 || parsing == null) {
            throw new IllegalArgumentException("Binary parsing requires positive limits");
        }
    }

    public static JavaBytecodeLimits standard() {
        return Defaults.INSTANCE;
    }

    private static final class Defaults {
        private static final JavaBytecodeLimits INSTANCE = read();

        private static JavaBytecodeLimits read() {
            Properties values = new Properties();
            try (InputStream input =
                    JavaBytecodeLimits.class.getResourceAsStream("bytecode.properties")) {
                if (input == null) {
                    throw new IllegalStateException("Missing bytecode defaults");
                }
                values.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read bytecode defaults", exception);
            }
            return new JavaBytecodeLimits(
                    Integer.parseInt(values.getProperty("maximum.input.bytes")),
                    new ParsingBudget(
                            Integer.parseInt(values.getProperty("maximum.depth")),
                            Long.parseLong(values.getProperty("maximum.work"))));
        }
    }
}
