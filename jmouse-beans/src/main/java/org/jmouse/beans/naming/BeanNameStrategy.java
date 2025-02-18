
package org.jmouse.beans.naming;

import java.lang.reflect.AnnotatedElement;

/**
 * Strategy interface for resolving structured names based on annotated elements.
 * <p>
 * Implementations of this interface provide mechanisms to determine
 * whether a given annotated element is supported and to resolve its
 * corresponding structured name.
 * </p>
 */
public interface BeanNameStrategy {

    /**
     * Determines if this strategy supports the given annotated element.
     *
     * @param element the annotated element to check.
     * @return {@code true} if this strategy supports the given element, otherwise {@code false}.
     */
    boolean supports(AnnotatedElement element);

    /**
     * Resolves the structured name for the given annotated element.
     * <p>
     * This method is called for elements that are supported by this strategy.
     * </p>
     *
     * @param element the annotated element to resolve.
     * @return the resolved structured name.
     * @throws IllegalArgumentException if the element is not supported or cannot be resolved.
     */
    String resolve(AnnotatedElement element);
}