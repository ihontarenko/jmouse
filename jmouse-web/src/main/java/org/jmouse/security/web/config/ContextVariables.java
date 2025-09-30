package org.jmouse.security.web.config;

@FunctionalInterface
public interface ContextVariables<C> {
    String getValue(C context, String name);
}