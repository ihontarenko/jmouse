package org.jmouse.mvc;

import java.lang.reflect.*;
import java.util.List;

/**
 * 🧩 Represents a method or constructor parameter with associated metadata.
 *
 * <p>This class encapsulates reflection details about a specific parameter within a method or constructor,
 * such as index, type, annotations, and owning executable. It's commonly used in argument resolution,
 * validation, and parameter binding mechanisms in MVC pipelines.
 *
 * <p>It supports lazy access to the {@link Parameter} and resolves its type dynamically.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
public final class MethodParameter {

    private final int        parameterIndex;
    private       Parameter  parameter;
    private final Executable executable;
    private       Class<?>   returnType;
    private       Class<?>   parameterType;

    /**
     * Creates a new {@code MethodParameter} for the given {@link Executable} (method or constructor) and index.
     *
     * @param executable      the declaring executable (method or constructor)
     * @param parameterIndex  the index of the parameter (0-based)
     */
    public MethodParameter(Executable executable, int parameterIndex) {
        this.parameterIndex = parameterIndex;
        this.executable = executable;
    }

    /**
     * Factory method to construct a {@code MethodParameter} from method name and parameter types.
     *
     * @param index the parameter index
     * @param type the class declaring the method
     * @param name the method name
     * @param types parameter types
     * @return a new {@code MethodParameter}
     */
    public static MethodParameter forMethod(int index, Class<?> type, String name, Class<?>... types) {
        try {
            return new MethodParameter(type.getMethod(name, types), index);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Factory method from a {@link Method} and index.
     *
     * @param method the method reference
     * @param index the parameter index
     * @return the corresponding {@code MethodParameter}
     */
    public static MethodParameter forMethod(Method method, int index) {
        return new MethodParameter(method, index);
    }

    /**
     * Factory method from a {@link Parameter} object.
     *
     * @param parameter the parameter to wrap
     * @return a new {@code MethodParameter}
     */
    public static MethodParameter forParameter(Parameter parameter) {
        return new MethodParameter(parameter.getDeclaringExecutable(), parameterIndex(parameter));
    }

    /**
     * Resolves the index of the given {@link Parameter}.
     *
     * @param parameter the parameter
     * @return index within its declaring executable
     */
    public static int parameterIndex(Parameter parameter) {
        Parameter[] parameters = parameter.getDeclaringExecutable().getParameters();

        if (parameters.length > 0) {
            return List.of(parameters).indexOf(parameter);
        }

        return 0;
    }

    /**
     * Returns the {@link Parameter} reflection object.
     *
     * @return the actual {@link Parameter} or {@code null} if unavailable
     */
    public Parameter getParameter() {
        Parameter parameter = this.parameter;

        if (isParameter()) {
            parameter = getExecutable().getParameters()[getParameterIndex()];
            this.parameter = parameter;
        }

        return parameter;
    }

    /**
     * Returns the underlying {@link Constructor}, if applicable.
     *
     * @return the constructor, or {@code null} if this is not a constructor
     */
    public Constructor<?> getConstructor() {
        Constructor<?> constructor = null;

        if (getExecutable() instanceof Constructor<?> c) {
            constructor = c;
        }

        return constructor;
    }

    /**
     * Returns the underlying {@link Method}, if applicable.
     *
     * @return the method, or {@code null} if this is not a method
     */
    public Method getMethod() {
        Method method = null;

        if (getExecutable() instanceof Method m) {
            method = m;
        }

        return method;
    }

    /**
     * Returns the declaring {@link Executable} (method or constructor).
     *
     * @return the executable object
     */
    public Executable getExecutable() {
        return executable;
    }

    /**
     * Returns the annotated element for this parameter (the declaring executable).
     *
     * @return the {@link AnnotatedElement}
     */
    public AnnotatedElement getAnnotatedElement() {
        return executable;
    }

    /**
     * Checks whether this instance represents a method or constructor parameter.
     *
     * @return {@code true} if it's a parameter; {@code false} if it's a return type
     */
    public boolean isParameter() {
        return parameterIndex > -1;
    }

    /**
     * Checks whether this represents the return type of a method.
     *
     * @return {@code true} if it's a return type, otherwise {@code false}
     */
    public boolean isReturnType() {
        return !isParameter();
    }

    /**
     * Returns the raw Java type of this parameter.
     *
     * @return the parameter's {@link Class} object
     */
    public Class<?> getParameterType() {
        Class<?> parameterType = this.parameterType;

        if (parameterType == null && isParameter()) {
            parameterType = getExecutable().getParameterTypes()[this.parameterIndex];
            this.parameterType = parameterType;
        }

        return parameterType;
    }

    /**
     * Returns the index of this parameter in the declaring method/constructor.
     *
     * @return the 0-based index
     */
    public int getParameterIndex() {
        return parameterIndex;
    }

    /**
     * Returns the raw Java type of method return type.
     *
     * @return the method's return type {@link Class} object
     */
    public Class<?> getReturnType() {
        Class<?> returnType = this.returnType;

        if (returnType == null && isReturnType()) {
            Method method = getMethod();
            returnType = method == null ? void.class : method.getReturnType();
            this.returnType = returnType;
        }

        return returnType;
    }

    /**
     * Returns a string representation of this parameter’s location.
     *
     * @return a string like {@code com.example.MyClass#myMethod[1]}
     */
    public String parameterLocation() {
        return "%s#%s[%d]:%s".formatted(executable.getDeclaringClass().getName(),
                executable.getName(), parameterIndex, getParameterType());
    }

    /**
     * Returns a string representation of this {@code MethodParameter}.
     *
     * @return a location-based string
     */
    @Override
    public String toString() {
        return parameterLocation();
    }

}
