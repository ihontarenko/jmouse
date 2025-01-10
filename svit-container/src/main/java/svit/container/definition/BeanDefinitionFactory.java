package svit.container.definition;

import svit.container.BeanContext;

import java.lang.reflect.AnnotatedElement;

/**
 * Factory interface for creating {@link BeanDefinition} instances.
 * Supports the addition of custom creation strategies.
 */
public interface BeanDefinitionFactory {

    /**
     * Creates a {@link BeanDefinition} for the given annotated element within the specified context.
     *
     * @param element the annotated element for which the bean definition is created.
     * @param context the {@link BeanContext} in which the bean will be managed.
     * @return a new {@link BeanDefinition} instance.
     */
    BeanDefinition createDefinition(AnnotatedElement element, BeanContext context);

    /**
     * Adds a custom {@link BeanDefinitionCreationStrategy} to this factory.
     *
     * @param strategy the strategy to add.
     */
    void addStrategy(BeanDefinitionCreationStrategy strategy);
}
