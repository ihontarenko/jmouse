package svit.container.definition;

import svit.container.BeanContext;
import svit.container.BeanInstantiationException;
import svit.container.annotation.BeanConstructor;
import svit.reflection.Reflections;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.Set;

public class ConstructorBeanDefinitionCreationStrategy extends AbstractBeanDefinitionCreationStrategy {

    @Override
    public boolean supports(AnnotatedElement element) {
        return element instanceof Class<?>;
    }

    @Override
    public BeanDefinition create(AnnotatedElement element, BeanContext context) {
        Class<?>                  klass      = (Class<?>) element;
        String                    beanName   = context.getNameResolver().resolveName(klass);
        ConstructorBeanDefinition definition = new ConstructorBeanDefinition(beanName, klass);

        Constructor<?> constructor;

        try {
            constructor = Reflections.findFirstAnnotatedConstructor(klass, BeanConstructor.class);
        } catch (Exception annotatedConstructorException) {
            try {
                constructor = Reflections.findFirstConstructor(klass);
            } catch (Exception defaultConstructorException) {
                RuntimeException exception = new BeanInstantiationException(
                        "No constructor was found. please create a default constructor for (" + klass + ") at least");
                defaultConstructorException.addSuppressed(annotatedConstructorException);
                exception.addSuppressed(defaultConstructorException);
                throw exception;
            }
        }

        definition.setConstructor(constructor);
        definition.setAnnotations(Set.of(klass.getAnnotations()));

        if (constructor.getParameterCount() != 0) {
            createDependencies(definition.getBeanDependencies(), constructor.getParameters());
        }

        updateBeanLifecycle(definition, klass);

        return definition;
    }

}
