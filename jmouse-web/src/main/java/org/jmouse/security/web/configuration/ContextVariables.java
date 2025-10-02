package org.jmouse.security.web.configuration;

@FunctionalInterface
public interface ContextVariables<C> {
    String getValue(C context, String name);
}