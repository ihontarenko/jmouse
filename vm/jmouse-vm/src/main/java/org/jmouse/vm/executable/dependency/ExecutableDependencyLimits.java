package org.jmouse.vm.executable.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record ExecutableDependencyLimits(long maximumWork, long maximumContributions) {

    public ExecutableDependencyLimits {
        if (maximumWork <= 0 || maximumContributions <= 0) {
            throw new IllegalArgumentException("Executable dependency limits must be positive");
        }
    }

    public static ExecutableDependencyLimits defaults() {
        return Defaults.VALUE;
    }

    private static ExecutableDependencyLimits load() {
        try (InputStream stream =
                ExecutableDependencyLimits.class.getResourceAsStream(
                        ExecutableDependencyNames.LIMITS_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing executable dependency limits");
            }
            Properties properties = new Properties();
            properties.load(stream);
            return new ExecutableDependencyLimits(
                    Long.parseLong(properties.getProperty(ExecutableDependencyNames.MAXIMUM_WORK)),
                    Long.parseLong(
                            properties.getProperty(
                                    ExecutableDependencyNames.MAXIMUM_CONTRIBUTIONS)));
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Invalid executable dependency limits", exception);
        }
    }

    private static final class Defaults {
        private static final ExecutableDependencyLimits VALUE = load();
    }
}
