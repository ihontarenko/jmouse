
package svit.beans.naming;

import java.lang.reflect.AnnotatedElement;

/**
 * Interface for resolving bean names using a set of {@link BeanNameStrategy} instances.
 * <p>
 * This interface provides methods for resolving a bean name based on an annotated element
 * and for registering additional strategies to customize name resolution behavior.
 * </p>
 */
public interface BeanNameResolver {

    /**
     * Resolves the name of a bean based on the provided annotated element.
     *
     * @param beanClass the annotated element representing the bean (e.g., class, method).
     * @return the resolved bean name.
     * @throws IllegalArgumentException if no strategy supports the given element.
     */
    String resolveName(AnnotatedElement beanClass);

    /**
     * Registers a new {@link BeanNameStrategy} for resolving bean names.
     * <p>
     * Strategies are evaluated in the order they are added, and the first supporting
     * strategy is used to resolve the name.
     * </p>
     *
     * @param strategy the {@link BeanNameStrategy} to add.
     */
    void addStrategy(BeanNameStrategy strategy);
}