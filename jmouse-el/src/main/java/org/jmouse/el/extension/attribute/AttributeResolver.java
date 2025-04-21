package org.jmouse.el.extension.attribute;

public interface AttributeResolver {
    Object resolve(Object instance, String name);
}
