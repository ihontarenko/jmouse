package org.jmouse.core.reflection;

import org.jmouse.core.matcher.Matcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

import static org.jmouse.core.reflection.MethodMatchers.*;

/**
 * A filter class to apply various conditions for filtering methods in a class.
 *
 * The {@code MethodFilter} allows users to build custom filters for selecting methods based on multiple criteria,
 * such as method name, return type, parameter types, and annotations. The filter combines these conditions using
 * logical AND, allowing for flexible and precise method selection.
 *
 * <p>Example usage:
 * <pre>
 *     MethodFilter filter = new MethodFilter(support, matcher, SomeClass.class);
 *     filter.methodName("toString")
 *           .returnType(String.class)
 *           .parameterTypes();
 * </pre>
 */
public class MethodFilter extends AbstractFilter<Method> {

    /**
     * Constructs a new {@code MethodFilter} with the given parameters.
     *
     * @param finder the {@link MemberFinder} to be used for finding methods
     * @param matcher the {@link Matcher} to be applied to the methods
     * @param type the class type to filter methods from
     */
    public MethodFilter(MemberFinder<Method> finder, Matcher<Method> matcher, Class<?> type) {
        super(finder, matcher, type);
    }

    /**
     * Filters methods based on their parameter types.
     *
     * @param parameterTypes the parameter types to match
     * @return the updated {@link MethodFilter} with the added condition for parameter types
     * @throws NullPointerException if the provided parameterTypes is {@code null}
     */
    public MethodFilter parameterTypes(Class<?>... parameterTypes) {
        Objects.requireNonNull(parameterTypes);

        matcher = matcher.and(hasParameterCount(parameterTypes.length));
        matcher = matcher.and(hasParameterTypes(parameterTypes).or(hasSoftParameterTypes(parameterTypes)));

        return this;
    }

    /**
     * Filters methods based on their return type.
     *
     * @param returnType the return type to match
     * @return the updated {@link MethodFilter} with the added condition for return type
     * @throws NullPointerException if the provided returnType is {@code null}
     */
    public MethodFilter returnType(Class<?> returnType) {
        Objects.requireNonNull(returnType);
        matcher = matcher.and(MethodMatchers.hasReturnType(returnType));
        return this;
    }

    /**
     * Filters methods based on their name.
     *
     * @param methodName the name of the method to match
     * @return the updated {@link MethodFilter} with the added condition for method name
     * @throws NullPointerException if the provided methodName is {@code null}
     */
    public MethodFilter methodName(String methodName) {
        Objects.requireNonNull(methodName);
        matcher = matcher.and(MethodMatchers.nameEquals(Objects.requireNonNull(methodName)));
        return this;
    }

    /**
     * Filters methods that are annotated with the given annotation type.
     *
     * @param annotationType the annotation type to match
     * @return the updated {@link MethodFilter} with the added condition for the annotation
     * @throws NullPointerException if the provided annotationType is {@code null}
     */
    public MethodFilter annotated(Class<? extends Annotation> annotationType) {
        Objects.requireNonNull(annotationType);
        matcher = matcher.and(MethodMatchers.isAnnotatedWith(annotationType));
        return this;
    }
}
