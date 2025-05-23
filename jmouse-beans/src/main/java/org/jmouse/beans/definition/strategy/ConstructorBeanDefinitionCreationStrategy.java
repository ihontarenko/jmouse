package org.jmouse.beans.definition.strategy;

import org.jmouse.beans.annotation.Provide;
import org.jmouse.beans.naming.BeanNameResolver;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.definition.BeanDefinitionException;
import org.jmouse.beans.definition.ConstructorBeanDefinition;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Set;

/**
 * A strategy for creating {@link ConstructorBeanDefinition} instances from classes.
 * <p>
 * This strategy:
 * <ul>
 *   <li>Resolves the bean name from the class using the {@link BeanNameResolver}.</li>
 *   <li>Searches for a constructor annotated with {@link BeanConstructor} if present; otherwise,
 *       it falls back to the first available constructor.</li>
 *   <li>Creates dependencies from the constructor parameters.</li>
 *   <li>Updates the bean lifecycle based on annotations like {@link Provide}.</li>
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
//        JavaBeanDescriptor<?>     descriptor = JavaBeanDescriptor.forBean(klass);

        Constructor<?> constructor;
        Class<?>[]     parameterTypes = new Class[0];

        if (klass.isRecord()) {
            RecordComponent[] components  = klass.getRecordComponents();
            Class<?>[]        recordTypes = new Class<?>[components.length];
            int               index       = 0;

            for (RecordComponent component : components) {
                recordTypes[index++] = component.getType();
            }

            parameterTypes = recordTypes;
        }

        try {
            // Attempt to find an annotated constructor
            constructor = Reflections.findFirstAnnotatedConstructor(klass, BeanConstructor.class);
        } catch (Exception annotatedConstructorException) {
            try {
                // Fallback to first available constructor
                constructor = Reflections.findFirstConstructor(klass, parameterTypes);
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
     * Determines if this strategy supports the provided bean.
     *
     * @param object the bean to check.
     * @return {@code true} if the strategy supports the bean, {@code false} otherwise.
     */
    @Override
    public boolean supports(Object object) {
        return super.supports(object) && object instanceof Class<?>;
    }
}
