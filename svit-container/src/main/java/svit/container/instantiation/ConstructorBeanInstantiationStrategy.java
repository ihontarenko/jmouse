package svit.container.instantiation;

import svit.container.BeanContext;
import svit.container.definition.BeanDependency;
import svit.container.BeanInstantiationException;
import svit.container.definition.ConstructorBeanDefinition;
import svit.container.definition.BeanDefinition;
import svit.reflection.Reflections;

import java.util.List;

public class ConstructorBeanInstantiationStrategy extends AbstractBeanInstantiationStrategy {

    @Override
    public Object create(BeanDefinition definition, BeanContext context) {
        ConstructorBeanDefinition beanDefinition = (ConstructorBeanDefinition) definition;
        List<BeanDependency>      dependencies   = beanDefinition.getBeanDependencies();
        Object[]                  arguments      = new Object[0];

        if (!dependencies.isEmpty()) {
            try {
                arguments = resolveDependencies(dependencies, context);
            } catch (RuntimeException exception) {
                throw new BeanInstantiationException(
                        "Failed to create bean via constructor strategy for bean type: " + definition.getBeanClass(), exception);
            }
        }

        return Reflections.instantiate(beanDefinition.getConstructor(), arguments);
    }

    @Override
    public boolean supports(BeanDefinition definition) {
        return ConstructorBeanDefinition.class.isAssignableFrom(definition.getClass());
    }
}
