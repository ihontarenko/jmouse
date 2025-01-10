package svit.container.instantiation;

import svit.container.BeanContext;
import svit.container.BeanInstantiationException;
import svit.container.definition.BeanDefinition;
import svit.container.definition.BeanDependency;
import svit.container.definition.MethodBeanDefinition;
import svit.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.List;

public class MethodBeanInstantiationStrategy extends AbstractBeanInstantiationStrategy {

    @Override
    public Object create(BeanDefinition definition, BeanContext context) {
        MethodBeanDefinition beanDefinition = (MethodBeanDefinition) definition;
        Method               factoryMethod  = beanDefinition.getFactoryMethod();
        Object               factoryObject  = resolveFactoryBean(beanDefinition, context);

        List<BeanDependency> dependencies = beanDefinition.getBeanDependencies();
        Object[]             arguments    = new Object[0];

        if (!dependencies.isEmpty()) {
            try {
                arguments = resolveDependencies(dependencies, context);
            } catch (Exception exception) {
                throw new BeanInstantiationException(
                        "Failed to create bean via method strategy for bean type: " + definition.getBeanClass(), exception);
            }
        }

        return Reflections.invokeMethod(factoryObject, factoryMethod, arguments);
    }

    private Object resolveFactoryBean(MethodBeanDefinition definition, BeanContext context) {
        Object factoryBean = definition.getFactoryObject();

        if (factoryBean == null) {
            BeanDefinition parent = definition.getParentDefinition();

            factoryBean = context.getBean(parent.getBeanName());

            for (BeanDefinition childrenDefinition : parent.getChildrenDefinitions()) {
                if (childrenDefinition instanceof MethodBeanDefinition methodBeanDefinition) {
                    methodBeanDefinition.setFactoryObject(factoryBean);
                }
            }
        }

        return factoryBean;
    }

    @Override
    public boolean supports(BeanDefinition definition) {
        return MethodBeanDefinition.class.isAssignableFrom(definition.getClass());
    }
}
