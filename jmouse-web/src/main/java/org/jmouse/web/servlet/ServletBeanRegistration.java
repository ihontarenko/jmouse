package org.jmouse.web.servlet;

public record ServletBeanRegistration(String name, String[] mappings) implements BeanRegistration {
}
