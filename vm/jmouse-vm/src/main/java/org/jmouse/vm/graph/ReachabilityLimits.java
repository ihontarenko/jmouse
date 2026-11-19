package org.jmouse.vm.graph;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record ReachabilityLimits(long maximumRequirements, long maximumRules, long maximumWork) {

    private static final String RESOURCE = "reachability.properties";
    private static final String REQUIREMENTS = "maximum-requirements";
    private static final String RULES = "maximum-rules";
    private static final String WORK = "maximum-work";

    public ReachabilityLimits {
        if (maximumRequirements <= 0 || maximumRules <= 0 || maximumWork <= 0) {
            throw new IllegalArgumentException("Reachability limits must be positive");
        }
    }

    public static ReachabilityLimits defaults() {
        return Defaults.VALUE;
    }

    private static ReachabilityLimits load() {
        try (InputStream stream = ReachabilityLimits.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing reachability limits: " + RESOURCE);
            }
            Properties properties = new Properties();
            properties.load(stream);
            return new ReachabilityLimits(
                    Long.parseLong(properties.getProperty(REQUIREMENTS)),
                    Long.parseLong(properties.getProperty(RULES)),
                    Long.parseLong(properties.getProperty(WORK)));
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Invalid reachability limits", exception);
        }
    }

    private static final class Defaults {
        private static final ReachabilityLimits VALUE = load();
    }
}
