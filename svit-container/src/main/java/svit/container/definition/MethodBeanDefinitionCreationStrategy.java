package svit.container.definition;

import svit.container.BeanContext;
import svit.container.naming.BeanNameResolver;
import svit.matcher.Matcher;
import svit.reflection.ClassMatchers;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * A strategy for creating {@link MethodBeanDefinition} instances from annotated {@link Method}s.
 * <p>
 * This strategy:
 * <ul>
 *     <li>Resolves the bean name from the method using the {@link BeanNameResolver}</li>
 *     <li>Sets the factory method and return type on the resulting {@link MethodBeanDefinition}</li>
 *     <li>Creates dependencies based on the method parameters, if any</li>
 *     <li>Updates the bean's lifecycle based on relevant annotations</li>
 *     <li>Associates the method-level annotations with the bean definition</li>
 *     <li>Links the definition to a parent definition, creating one if necessary</li>
 * </ul>
 */
public class MethodBeanDefinitionCreationStrategy extends AbstractBeanDefinitionCreationStrategy {

    /**
     * Checks whether the given element is a {@link Method}.
     *
     * @param element the annotated element to evaluate
     * @return {@code true} if {@code element} is an instance of {@link Method}, otherwise {@code false}
     */
    @Override
    public boolean supports(AnnotatedElement element) {
        return element instanceof Method;
    }

    /**
     * Creates a {@link MethodBeanDefinition} from the given {@link Method}.
     * <p>
     * Steps:
     * <ol>
     *     <li>Resolves the bean name using the {@link BeanContext}'s {@link BeanNameResolver}</li>
     *     <li>Sets the factory method and return type</li>
     *     <li>Creates dependencies for each method parameter, if present</li>
     *     <li>Updates the bean's lifecycle based on annotations</li>
     *     <li>Attaches the method-level annotations to the definition</li>
     *     <li>Updates the definition's parent, ensuring the parent is registered if needed</li>
     * </ol>
     *
     * @param element the annotated element, expected to be a {@link Method}
     * @param context the {@link BeanContext} in which the definition will be managed
     * @return a new {@link MethodBeanDefinition} representing the method-based bean
     */
    @Override
    public BeanDefinition create(AnnotatedElement element, BeanContext context) {
        Method method   = (Method) element;
        String beanName = context.getNameResolver().resolveName(method);

        // Create a new MethodBeanDefinition based on the method's return type
        MethodBeanDefinition definition = new MethodBeanDefinition(beanName, method.getReturnType());
        definition.setFactoryMethod(method);

        // If the method has parameters, create dependencies
        if (method.getParameterCount() != 0) {
            createDependencies(definition.getBeanDependencies(), method.getParameters());
        }

        // Update the definition's lifecycle based on annotations, if any
        updateBeanLifecycle(definition, method);

        // Attach method-level annotations
        definition.setAnnotations(Set.of(element.getAnnotations()));

        // Link the definition to its parent (the class that declares the method)
        updateParentDefinition(definition, context);

        return definition;
    }

    /**
     * Updates the parent definition for the given method-based definition.
     * <p>
     * Ensures that the declaring class of the method is also registered as a bean definition.
     * If it doesn't exist, a new definition is created via the {@link BeanDefinitionFactory}.
     * Then links the method-based definition as a child of that parent definition.
     *
     * @param definition the method-based bean definition
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
}
