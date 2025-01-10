package svit.container.definition;

import svit.container.BeanScope;
import svit.container.annotation.Provide;
import svit.container.annotation.Qualifier;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * An abstract base class for {@link BeanDefinitionCreationStrategy} implementations.
 * Provides utility methods for processing dependencies and annotations while creating
 * {@link BeanDefinition} instances.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li><strong>Dependency Creation</strong>:
 *       <ul>
 *         <li>{@link #createDependencies(List, Parameter[])}: Iterates over the parameters, converting each into a {@link BeanDependency}.</li>
 *         <li>{@link #createDependency(Parameter)}: Creates a single {@link BeanDependency} from a parameter, handling {@link Qualifier} if present.</li>
 *       </ul>
 *   </li>
 *   <li><strong>Lifecycle Updates</strong>:
 *       <ul>
 *         <li>{@link #updateBeanLifecycle(BeanDefinition, AnnotatedElement)}: Reads the {@link Provide} annotation to set the bean's {@link BeanScope}.</li>
 *       </ul>
 *   </li>
 * </ul>
 */
public abstract class AbstractBeanDefinitionCreationStrategy implements BeanDefinitionCreationStrategy {

    /**
     * Creates bean dependencies for the given array of parameters.
     * Each parameter is converted into a {@link BeanDependency} using {@link #createDependency(Parameter)}.
     *
     * @param dependencies the list in which to store the created dependencies
     * @param parameters   the array of parameters that require dependency objects
     */
    protected void createDependencies(List<BeanDependency> dependencies, Parameter[] parameters) {
        for (Parameter parameter : parameters) {
            dependencies.add(createDependency(parameter));
        }
    }

    /**
     * Creates a single {@link BeanDependency} from the given parameter.
     * <p>
     * If the parameter is annotated with {@link Qualifier}, its value is used
     * as the dependency name.
     *
     * @param parameter the parameter to convert into a dependency
     * @return a {@link BeanDependency} representing the parameter
     */
    protected BeanDependency createDependency(Parameter parameter) {
        String name = null;

        if (parameter.isAnnotationPresent(Qualifier.class)) {
            name = parameter.getAnnotation(Qualifier.class).value();
        }

        return new SimpleBeanDependency(parameter.getType(), name);
    }

    /**
     * Updates the bean lifecycle by inspecting the {@link Provide} annotation on the provided element.
     * If present, the annotation's lifecycle value is assigned to the {@code definition}.
     *
     * @param definition the bean definition to update
     * @param element    the annotated element (class or method) that may hold the {@link Provide} annotation
     */
    protected void updateBeanLifecycle(BeanDefinition definition, AnnotatedElement element) {
        if (element.isAnnotationPresent(Provide.class)) {
            definition.setBeanScope(element.getAnnotation(Provide.class).scope());
        }
    }
}
