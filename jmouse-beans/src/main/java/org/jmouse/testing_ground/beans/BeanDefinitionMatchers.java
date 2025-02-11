package org.jmouse.testing_ground.beans;

import org.jmouse.testing_ground.beans.definition.BeanDefinition;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ClassMatchers;

/**
 * Utility class providing predefined {@link Matcher}s for filtering {@link BeanDefinition}s.
 * <p>
 * These matchers can be used to perform advanced queries on collections of bean definitions,
 * such as filtering by annotations or type compatibility.
 * </p>
 */
public final class BeanDefinitionMatchers {

    private BeanDefinitionMatchers() {
        // Utility class: prevent instantiation
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
