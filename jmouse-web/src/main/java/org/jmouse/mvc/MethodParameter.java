package org.jmouse.mvc;

import java.lang.reflect.*;
import java.util.List;

/**
 * 🧩 Represents a method or constructor parameter with associated metadata.
 *
 * <p>Used for binding, validation, or analysis of parameter types, annotations, and index information.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class MethodParameter {

    private final int        parameterIndex;
    private final Executable executable;
    private       Parameter  parameter;

    public MethodParameter(Executable executable, int parameterIndex) {
        this.parameterIndex = parameterIndex;
        this.executable = executable;
    }

    public static MethodParameter forMethod(int index, Class<?> type, String name, Class<?>... types) {
        try {
            return new MethodParameter(type.getMethod(name, types), index);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static MethodParameter forMethod(Method method, int index) {
        return new MethodParameter(method, index);
    }

    public static MethodParameter forParameter(Parameter parameter) {
        return new MethodParameter(parameter.getDeclaringExecutable(), parameterIndex(parameter));
    }

    public static int parameterIndex(Parameter parameter) {
        Parameter[] parameters = parameter.getDeclaringExecutable().getParameters();

        if (parameters.length > 0) {
            return List.of(parameters).indexOf(parameter);
        }

        return 0;
    }

    public Parameter getParameter() {
        Parameter parameter = this.parameter;

        if (isParameter()) {
            parameter = getExecutable().getParameters()[getParameterIndex()];
            this.parameter = parameter;
        }

        return parameter;
    }

    public Constructor<?> getConstructor() {
        Constructor<?> constructor = null;

        if (getExecutable() instanceof Constructor<?> c) {
            constructor = c;
        }

        return constructor;
    }

    public Method getMethod() {
        Method method = null;

        if (getExecutable() instanceof Method m) {
            method = m;
        }

        return method;
    }

    public Executable getExecutable() {
        return executable;
    }

    public AnnotatedElement getAnnotatedElement() {
        return executable;
    }

    public boolean isParameter() {
        return parameterIndex > -1;
    }

    public boolean isReturnType() {
        return !isParameter();
    }

    public Class<?> getType() {
        Class<?> type = getMethod().getReturnType();

        if (isParameter()) {
            type = getParameter().getType();
        }

        return type;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    public String parameterLocation() {
        return "%s#%s[%d]".formatted(executable.getDeclaringClass().getName(), executable.getName(), parameterIndex);
    }

    @Override
    public String toString() {
        return parameterLocation();
    }

}
