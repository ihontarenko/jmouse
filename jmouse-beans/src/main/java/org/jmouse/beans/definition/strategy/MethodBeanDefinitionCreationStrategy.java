package org.jmouse.beans.definition.strategy;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.definition.BeanDefinitionFactory;
import org.jmouse.beans.definition.MethodBeanDefinition;
import org.jmouse.beans.naming.BeanNameResolver;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.ClassMatchers;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * A strategy for creating {@link MethodBeanDefinition} instances from {@link Method} elements.
 * <p>
 * This strategy processes methods annotated with specific annotations to create structured definitions
 * and resolve dependencies required for method-based structured instantiation.
 */
public class MethodBeanDefinitionCreationStrategy extends AbstractBeanDefinitionCreationStrategy<Method> {

    /**
     * Creates a {@link BeanDefinition} with a specified name from the provided structured.
     *
     * @param name    the name of the structured.
     * @param method  the structured from which the structured definition is created.
     * @param context the {@link BeanContext} used during creation.
     * @return the created {@link BeanDefinition}.
     */
    @Override
    public BeanDefinition create(String name, Method method, BeanContext context) {
        // Create a new MethodBeanDefinition based on the method's return type
        MethodBeanDefinition definition = new MethodBeanDefinition(name, method.getReturnType());
        definition.setFactoryMethod(method);

        definition.setParametrizedTypes(Reflections.getParameterizedTypes(method.getGenericReturnType()));

        // If the method has parameters, create dependencies
        if (method.getParameterCount() != 0) {
            createDependencies(definition.getBeanDependencies(), method.getParameters());
        }

        // Update the definition's lifecycle based on annotations, if any
        updateBeanDefinition(definition, method);

        // Attach method-level annotations
        definition.setAnnotations(Set.of(method.getAnnotations()));

        // Link the definition to its parent (the class that declares the method)
        updateParentDefinition(definition, context);

        return definition;
    }

    /**
     * Updates the parent definition for the given method-based definition.
     * <p>
     * Ensures that the declaring class of the method is also registered as a structured definition.
     * If it doesn't exist, a new definition is created via the {@link BeanDefinitionFactory}.
     * Then links the method-based definition as a child of that parent definition.
     *
     * @param definition the method-based structured definition
     * @param context    the {@link BeanContext} in which the parent resides
     */
    public void updateParentDefinition(MethodBeanDefinition definition, BeanContext context) {
        BeanNameResolver  resolver         = context.getNameResolver();
        Class<?>          factoryClass     = definition.getFactoryMethod().getDeclaringClass();
        Matcher<Class<?>> matcher          = ClassMatchers.isSupertype(factoryClass);
        String            beanName         = resolver.resolveName(factoryClass);
        BeanDefinition    parentDefinition = context.getDefinition(beanName);

        // If the parent definition isn't registered or doesn't match the expected class, create it
        if (parentDefinition == null || !matcher.matches(parentDefinition.getBeanClass())) {
            BeanDefinitionFactory factory = context.getBeanDefinitionFactory();
            parentDefinition = factory.createDefinition(factoryClass, context);
            context.registerDefinition(parentDefinition);
        }

        // Link parent <-> child
        definition.setParentDefinition(parentDefinition);
        parentDefinition.addChildDefinition(definition);
    }

    /**
     * Determines if this strategy supports the provided structured.
     *
     * @param object the structured to check.
     * @return {@code true} if the strategy supports the structured, {@code false} otherwise.
     */
    @Override
    public boolean supports(Object object) {
        return super.supports(object) && object instanceof Method;
    }

}
