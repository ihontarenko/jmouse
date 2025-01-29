package org.jmouse.core.env;

public interface EnvironmentConfigurer {

    default void configureEnvironment(Environment environment) {
        // no-op
    }

}
