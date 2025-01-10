package svit.container.definition;

import svit.container.BeanContext;

import java.lang.reflect.AnnotatedElement;

/**
 * A strategy interface for creating {@link BeanDefinition} instances from annotated elements.
 * Implementations define how to interpret annotations on {@link AnnotatedElement} and produce
 * the corresponding {@link BeanDefinition}.
 */
public interface BeanDefinitionCreationStrategy {

    /**
     * Determines whether this strategy supports creating a {@link BeanDefinition} from the given element.
     *
     * @param element the annotated element, such as a class or method
     * @return {@code true} if this strategy can handle the given element, otherwise {@code false}
     */
    boolean supports(AnnotatedElement element);

    /**
     * Creates a new {@link BeanDefinition} from the given annotated element within the specified context.
     *
     * @param element the annotated element from which to create the definition
     * @param context the {@link BeanContext} in which the bean will be managed
     * @return a new {@link BeanDefinition} representing the bean described by the element
     */
    BeanDefinition create(AnnotatedElement element, BeanContext context);
}
