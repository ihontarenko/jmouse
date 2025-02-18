package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.MethodData;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.MethodMatchers;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import static org.jmouse.core.reflection.MethodMatchers.*;

public class MethodDescriptor extends ExecutableDescriptor<Method, MethodData, MethodIntrospector> {

    private static final Matcher<Executable> SETTER = MethodMatchers.setter();
    private static final Matcher<Executable> GETTER = MethodMatchers.getter();

    protected MethodDescriptor(MethodIntrospector introspector, MethodData container) {
        super(introspector, container);
    }

    public ClassTypeDescriptor getReturnType() {
        return container.getReturnType();
    }

    public boolean isSetter() {
        return SETTER.matches(unwrap());
    }

    public boolean isGetter() {
        return GETTER.matches(unwrap());
    }

    public boolean isGetter(String prefix) {
        return nameStarts(prefix).matches(unwrap());
    }

    public String getPropertyName() {
        String prefix = isGetter() && isGetter(GETTER_IS_PREFIX) ? GETTER_IS_PREFIX : GETTER_GET_PREFIX;
        return Reflections.getPropertyName(unwrap(), prefix);
    }

    @Override
    public MethodIntrospector toIntrospector() {
        return introspector;
    }

    @Override
    public String toString() {
        return Reflections.getMethodName(unwrap()) + " : " + getReturnType().getName();
    }
}
