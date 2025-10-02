package org.jmouse.security.web.configuration;

public interface SecurityBuilder<T> {
    T build() throws Exception;
}