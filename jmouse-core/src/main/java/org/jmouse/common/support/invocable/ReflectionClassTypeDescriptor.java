package org.jmouse.common.support.invocable;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.FieldMatchers;
import org.jmouse.core.reflection.FieldFinder;
import org.jmouse.core.reflection.MemberFinder;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.util.helper.Strings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.jmouse.util.helper.Strings.underscored;

public class ReflectionClassTypeDescriptor implements ClassTypeDescriptor {

    private final Class<?> nativeClass;

    public ReflectionClassTypeDescriptor(Class<?> nativeClass) {
        this.nativeClass = nativeClass;
    }

    public ReflectionClassTypeDescriptor(Object object) {
        this(Objects.requireNonNull(object).getClass());
    }

    @Override
    public MethodDescriptor getMethod(String name, Class<?>... parameterTypes) {
        Collection<Method> methods = MethodFinder.getMethods(getNativeClass(), name, parameterTypes);

        if (methods.size() > 1) {
            throw new AmbiguousDescriptorException(
                    "Found more than one method for the given name. Try specifying argument types for disambiguation.");
        }

        if (methods.size() == 0) {
            String parametersString = parameterTypes.length == 0 ? "" : Arrays.toString(parameterTypes);

            if (parametersString.length() > 0) {
                parametersString = parametersString.substring(1).substring(0, parametersString.length() - 2);
            }

            throw new NoSuchMethodDescriptorException(
                    "No such method exists %s#%s(%s). Please check the method name and try again."
                            .formatted(nativeClass.getCanonicalName(), name, parametersString));
        }

        return new ReflectionMethodDescriptor(methods.iterator().next(), nativeClass);
    }

    @Override
    public FieldDescriptor getField(String name) {
        Matcher<Field>      matcher = FieldMatchers.nameEquals(name);
        MemberFinder<Field> finder  = new FieldFinder();

        Optional<Field> optional = finder.findFirst(getNativeClass(), matcher);
        Field field = optional.orElseThrow(() -> new NoSuchFieldDescriptorException(
                "No such field exists '%s' in class '%s'. Please check the method name and try again."
                        .formatted(name, nativeClass.getCanonicalName())));

        return new ReflectionFieldDescriptor(field, getNativeClass());
    }

    @Override
    public Class<?> getNativeClass() {
        return nativeClass;
    }

    @Override
    public String getName() {
        return getNativeClass().getName();
    }

    @Override
    public String toString() {
        return "%s: [%s]".formatted(Strings.underscored(getClass().getSimpleName()).toUpperCase(), getNativeClass());
    }

}
