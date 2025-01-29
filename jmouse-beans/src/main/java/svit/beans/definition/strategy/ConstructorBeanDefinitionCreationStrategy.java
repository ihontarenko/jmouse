package svit.beans.definition.strategy;

import svit.beans.BeanContext;
import svit.beans.annotation.BeanConstructor;
import svit.beans.definition.BeanDefinition;
import svit.beans.definition.BeanDefinitionException;
import svit.beans.definition.ConstructorBeanDefinition;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Constructor;
import java.util.Set;

/**
 * A strategy for creating {@link ConstructorBeanDefinition} instances from classes.
 * <p>
 * This strategy:
 * <ul>
 *   <li>Resolves the bean name from the class using the {@link svit.beans.naming.BeanNameResolver}.</li>
 *   <li>Searches for a constructor annotated with {@link BeanConstructor} if present; otherwise,
 *       it falls back to the first available constructor.</li>
 *   <li>Creates dependencies from the constructor parameters.</li>
 *   <li>Updates the bean lifecycle based on annotations like {@link svit.beans.annotation.Provide}.</li>
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
public class ConstructorBeanDefinitionCreationStrategy extends AbstractBeanDefinitionCreationStrategy<Class<?>> {


    @Override
    public BeanDefinition create(String name, Class<?> klass, BeanContext context) {
        ConstructorBeanDefinition definition = new ConstructorBeanDefinition(name, klass);

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
        updateBeanDefinition(definition, klass);

        return definition;
    }

    /**
     * Determines if this strategy supports the provided object.
     *
     * @param object the object to check.
     * @return {@code true} if the strategy supports the object, {@code false} otherwise.
     */
    @Override
    public boolean supports(Object object) {
        return super.supports(object) && object instanceof Class<?>;
    }
}
