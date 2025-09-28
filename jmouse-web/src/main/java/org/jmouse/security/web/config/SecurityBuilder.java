package org.jmouse.security.web.config;

public interface SecurityBuilder<T> {
    T build() throws Exception;
}