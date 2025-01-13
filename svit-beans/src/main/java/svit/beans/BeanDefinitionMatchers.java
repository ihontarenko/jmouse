package svit.beans;

import svit.beans.definition.BeanDefinition;
import svit.matcher.Matcher;
import svit.reflection.ClassMatchers;

import java.lang.annotation.Annotation;

/**
 * Utility class providing predefined {@link Matcher}s for filtering {@link BeanDefinition}s.
 * <p>
 * These matchers can be used to perform advanced queries on collections of bean definitions,
 * such as filtering by annotations or type compatibility.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * List<BeanDefinition> definitions = ...;
 * Matcher<BeanDefinition> matcher = BeanDefinitionMatchers.annotatedWith(MyAnnotation.class);
 * List<BeanDefinition> filteredDefinitions = definitions.stream()
 *     .filter(matcher::matches)
 *     .collect(Collectors.toList());
 * }</pre>
 */
public final class BeanDefinitionMatchers {

    private BeanDefinitionMatchers() {
        // Utility class: prevent instantiation
    }

    /**
     * Creates a matcher that checks if a {@link BeanDefinition} is annotated with the specified annotation.
     *
     * @param annotation the annotation type to check for.
     * @return a {@link Matcher} that matches {@link BeanDefinition}s annotated with the given annotation.
     */
    public static Matcher<BeanDefinition> annotatedWith(Class<? extends Annotation> annotation) {
        return item -> item.isAnnotatedWith(annotation);
    }

    /**
     * Creates a matcher that checks if the bean class of a {@link BeanDefinition}
     * is a supertype of the specified type.
     *
     * @param type the type to check against.
     * @return a {@link Matcher} that matches {@link BeanDefinition}s whose bean class
     *         is assignable from the specified type.
     */
    public static Matcher<BeanDefinition> isSupertype(Class<?> type) {
        return item -> ClassMatchers.isSupertype(type).matches(item.getBeanClass());
    }
}
