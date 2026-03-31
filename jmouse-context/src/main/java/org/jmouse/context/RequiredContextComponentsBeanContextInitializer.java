package org.jmouse.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextInitializer;
import org.jmouse.beans.definition.BeanDefinitionFactory;
import org.jmouse.beans.instantiation.BeanInstantiationFactory;
import org.jmouse.context.definition.RecordPropertiesDefinitionCreationStrategy;
import org.jmouse.context.instantiation.RecordPropertiesInstantiationStrategy;
import org.jmouse.core.Priority;

@Priority(Integer.MIN_VALUE + 10)
public class RequiredContextComponentsBeanContextInitializer implements BeanContextInitializer {

    @Override
    public void initialize(BeanContext context) {
        BeanDefinitionFactory definitionFactory = context.getBeanDefinitionFactory();

        if (context.getBeanFactory() instanceof BeanInstantiationFactory instantiationFactory) {
            instantiationFactory.addStrategy(new RecordPropertiesInstantiationStrategy());
        }

        definitionFactory.addStrategy(new RecordPropertiesDefinitionCreationStrategy());
    }

}
