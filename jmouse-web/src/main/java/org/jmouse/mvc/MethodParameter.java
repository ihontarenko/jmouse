package org.jmouse.mvc;

import org.jmouse.core.bind.descriptor.ParameterDescriptor;
import org.jmouse.core.bind.descriptor.ParameterIntrospector;
import org.jmouse.core.reflection.JavaType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
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

    private final ParameterDescriptor descriptor;
    private final int                 parameterIndex;
    private final Executable          executable;

    /**
     * Constructs a new {@code MethodParameter}.
     *
     * @param descriptor      metadata describing the parameter
     * @param parameterIndex  index of the parameter in the declaring method or constructor
     */
    public MethodParameter(ParameterDescriptor descriptor, int parameterIndex) {
        this.descriptor = descriptor;
        this.parameterIndex = parameterIndex;
        this.executable = descriptor.getExecutable().unwrap();
    }

    /**
     * Checks if the parameter has the given annotation.
     *
     * @param annotationType the annotation class to look for
     * @return {@code true} if the annotation is present, {@code false} otherwise
     */
    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
        return descriptor.unwrap().isAnnotationPresent(annotationType);
    }

    /**
     * Retrieves the annotation of the given type on the parameter.
     *
     * @param annotationType the annotation type
     * @param <A>            generic annotation type
     * @return the annotation instance or {@code null} if not present
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return descriptor.unwrap().getAnnotation(annotationType);
    }

    /**
     * Returns all annotations declared on the parameter.
     *
     * @return a list of annotations
     */
    public List<Annotation> getAnnotations() {
        return List.of(descriptor.unwrap().getAnnotations());
    }

    /**
     * Returns the resolved Java type of the parameter.
     *
     * @return the {@link JavaType} representing this parameter's type
     */
    public JavaType getJavaType() {
        return JavaType.forParameter(descriptor.unwrap());
    }

    /**
     * Returns the {@link ParameterDescriptor} backing this parameter.
     *
     * @return the descriptor
     */
    public ParameterDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Returns the index of the parameter in its declaring method or constructor.
     *
     * @return the parameter index
     */
    public int getParameterIndex() {
        return parameterIndex;
    }

    /**
     * Returns a string representation of the parameter's location.
     * Format: {@code fully.qualified.ClassName#methodName[index]}
     *
     * @return the parameter location string
     */
    public String parameterLocation() {
        return "%s#%s[%d]".formatted(executable.getDeclaringClass().getName(), executable.getName(), parameterIndex);
    }

    @Override
    public String toString() {
        return parameterLocation();
    }

    /**
     * Factory method to create a {@code MethodParameter} from a {@link Parameter}.
     *
     * @param parameter the Java reflection {@link Parameter}
     * @return a new {@code MethodParameter} instance
     */
    public static MethodParameter ofParameter(Parameter parameter) {
        ParameterIntrospector introspector = new ParameterIntrospector(parameter);
        ParameterDescriptor   descriptor   = introspector
                .introspect().executable(parameter.getDeclaringExecutable()).toDescriptor();

        return new MethodParameter(descriptor, parameterIndex(parameter));
    }

    /**
     * 🔢 Returns the index of the given {@link Parameter} in its declaring executable.
     *
     * @param parameter the {@code Parameter} whose index is to be found
     * @return the index of the parameter, or {@code 0} if no parameters are present
     *
     * ⚠️ Might return {@code -1} if the parameter is not found (defensive check recommended).
     */
    public static int parameterIndex(Parameter parameter) {
        Parameter[] parameters = parameter.getDeclaringExecutable().getParameters();

        if (parameters.length > 0) {
            return List.of(parameters).indexOf(parameter);
        }

        return 0;
    }

}
