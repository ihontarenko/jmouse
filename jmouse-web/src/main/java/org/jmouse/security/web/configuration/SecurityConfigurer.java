package org.jmouse.security.web.configuration;

public interface SecurityConfigurer<B> {

    default void initialize(B builder) throws Exception {
    }

    default void configure(B builder) throws Exception {
    }

}