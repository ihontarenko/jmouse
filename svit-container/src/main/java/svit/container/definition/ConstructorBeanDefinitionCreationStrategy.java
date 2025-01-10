package svit.container.definition;

import svit.container.BeanContext;
import svit.container.annotation.BeanConstructor;
import svit.reflection.Reflections;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.Set;

/**
 * A strategy for creating {@link ConstructorBeanDefinition} instances from classes.
 * <p>
 * This strategy:
 * <ul>
 *   <li>Resolves the bean name from the class using the {@link svit.container.naming.BeanNameResolver}.</li>
 *   <li>Searches for a constructor annotated with {@link BeanConstructor} if present; otherwise,
 *       it falls back to the first available constructor.</li>
 *   <li>Creates dependencies from the constructor parameters.</li>
 *   <li>Updates the bean lifecycle based on annotations like {@link svit.container.annotation.Provide}.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @BeanConstructor
 * public ServiceBean(@Qualifier("userObject") User user) {
 *
 * }
 * }</pre>
 */
public class ConstructorBeanDefinitionCreationStrategy extends AbstractBeanDefinitionCreationStrategy {

    /**
     * Determines if this strategy supports creating a bean definition from the given element.
     *
     * @param element the annotated element to evaluate
     * @return {@code true} if {@code element} is an instance of {@link Class}, otherwise {@code false}
     */
    @Override
    public boolean supports(AnnotatedElement element) {
        return element instanceof Class<?>;
    }

    /**
     * Creates a {@link ConstructorBeanDefinition} from the given class.
     * <p>
     * The method attempts to find a constructor annotated with {@link BeanConstructor}. If none is found,
     * it uses the first available constructor. If no constructor is found, a {@link BeanDefinitionException}
     * is thrown.
     *
     * @param element the class to create a definition for
     * @param context the {@link BeanContext} where this definition will be used
     * @return the created {@link ConstructorBeanDefinition}
     * @throws BeanDefinitionException if no valid constructor can be found for the class
     */
    @Override
    public BeanDefinition create(AnnotatedElement element, BeanContext context) {
        Class<?> klass    = (Class<?>) element;
        String   beanName = context.getNameResolver().resolveName(klass);

        // Create the definition
        ConstructorBeanDefinition definition = new ConstructorBeanDefinition(beanName, klass);

        Constructor<?> constructor;

        try {
            // Attempt to find an annotated constructor
            constructor = Reflections.findFirstAnnotatedConstructor(klass, BeanConstructor.class);
        } catch (Exception annotatedConstructorException) {
            try {
                // Fallback to first available constructor
                constructor = Reflections.findFirstConstructor(klass);
            } catch (Exception defaultConstructorException) {
                BeanDefinitionException exception = new BeanDefinitionException(
                        "No constructor was found. Please create a default constructor for (" + klass + ") at least."
                );
                defaultConstructorException.addSuppressed(annotatedConstructorException);
                exception.addSuppressed(defaultConstructorException);
                throw exception;
            }
        }

        definition.setConstructor(constructor);
        definition.setAnnotations(Set.of(klass.getAnnotations()));

        // Create dependencies from constructor parameters, if any
        if (constructor.getParameterCount() != 0) {
            createDependencies(definition.getBeanDependencies(), constructor.getParameters());
        }

        // Update the bean lifecycle (e.g., SINGLETON, PROTOTYPE) based on annotations
        updateBeanLifecycle(definition, klass);

        return definition;
    }
}
