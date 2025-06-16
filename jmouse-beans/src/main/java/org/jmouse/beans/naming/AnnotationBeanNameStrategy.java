package org.jmouse.beans.naming;

import org.jmouse.beans.annotation.BeanName;
import org.jmouse.beans.annotation.Factories;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.util.helper.Strings;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

import static org.jmouse.core.reflection.Reflections.getAnnotationValue;

/**
 * A strategy for resolving bean names based on annotations present on a {@link Class}
 * or {@link Method}. This strategy prioritizes annotation values in the following order:
 * <ol>
 *     <li>{@link Qualifier#value()}</li>
 *     <li>{@link Provide#value()}</li>
 *     <li>{@link Factories#name()}</li>
 * </ol>
 * If none of these annotations provide a value, it falls back to:
 * <ul>
 *     <li>Class name (via {@link ClassNameStrategy}) if the element is a class.</li>
 *     <li>Method name if the element is a method.</li>
 * </ul>
 */
public class AnnotationBeanNameStrategy implements BeanNameStrategy {

    /**
     * Checks if the given {@link AnnotatedElement} is supported by this strategy.
     * This strategy supports {@link Method} and {@link Class} elements.
     *
     * @param element the element to check.
     * @return {@code true} if the element is a {@link Method} or {@link Class}, {@code false} otherwise.
     */
    @Override
    public boolean supports(AnnotatedElement element) {
        return element instanceof Method || element instanceof Class<?>;
    }

    /**
     * Resolves the name of a bean based on annotations or fallback rules.

     * @param element the element from which to resolve the name.
     * @return the resolved bean name, or {@code null} if no suitable name is found.
     */
    @Override
    public String resolve(AnnotatedElement element) {
        List<Function<AnnotatedElement, Object>> functions = List.of(
                e -> getAnnotationValue(e, BeanName.class, BeanName::value),
                e -> getAnnotationValue(e, Qualifier.class, Qualifier::value),
                e -> getAnnotationValue(e, Provide.class, Provide::value),
                e -> getAnnotationValue(e, Factories.class, Factories::name),
                e -> e instanceof Class<?> ? new ClassNameStrategy().resolve(e) : null,
                e -> e instanceof Method m ? Strings.uncapitalize(Reflections.getMethodName(m)) : null
        );

        for (Function<AnnotatedElement, Object> function : functions) {
            Object result = function.apply(element);
            if (result != null && Strings.isNotEmpty((String) result)) {
                return (String) result;
            }
        }

        return null;
    }
}
