package org.jmouse.security.web.configuration;

public interface SecurityConfigurer<T, B extends SecurityBuilder<T>> {

    default void initialize(B builder) throws Exception {
    }

    default void configure(B builder) throws Exception {
    }

}