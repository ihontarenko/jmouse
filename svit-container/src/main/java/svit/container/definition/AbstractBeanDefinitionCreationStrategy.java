package svit.container.definition;

import svit.container.annotation.Provide;
import svit.container.annotation.Qualifier;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.util.List;

abstract public class AbstractBeanDefinitionCreationStrategy implements BeanDefinitionCreationStrategy {

    protected void createDependencies(List<BeanDependency> dependencies, Parameter[] parameters) {
        for (Parameter parameter : parameters) {
            dependencies.add(createDependency(parameter));
        }
    }

    protected BeanDependency createDependency(Parameter parameter) {
        String name = null;

        if (parameter.isAnnotationPresent(Qualifier.class)) {
            name = parameter.getAnnotation(Qualifier.class).value();
        }

        return new SimpleBeanDependency(parameter.getType(), name);
    }

    protected void updateBeanLifecycle(BeanDefinition definition, AnnotatedElement element) {
        if (element.isAnnotationPresent(Provide.class)) {
            definition.setLifecycle(element.getAnnotation(Provide.class).lifecycle());
        }
    }

}
