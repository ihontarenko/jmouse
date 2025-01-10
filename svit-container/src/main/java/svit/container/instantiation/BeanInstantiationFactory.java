package svit.container.instantiation;

import svit.container.BeanContext;
import svit.container.definition.BeanDefinition;

public interface BeanInstantiationFactory {

    void addStrategy(BeanInstantiationStrategy strategy);

    Object createInstance(BeanDefinition definition, BeanContext context);

}
