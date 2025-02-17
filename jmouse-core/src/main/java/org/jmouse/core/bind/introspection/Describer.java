package org.jmouse.core.bind.introspection;

import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Parameter;

final public class Describer {

    private Describer() {}

    public static ParameterDescriptor forParameter(final Parameter parameter) {
        return new ParameterIntrospector(parameter).introspect().toDescriptor();
    }

    public static String className(Class<?> type) {
        return Reflections.getShortName(type);
    }

}
