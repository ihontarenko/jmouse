package org.jmouse.core.environment;

public interface EnvironmentConfigurer {

    default void configureEnvironment(Environment environment) {
        // no-op
    }

}
