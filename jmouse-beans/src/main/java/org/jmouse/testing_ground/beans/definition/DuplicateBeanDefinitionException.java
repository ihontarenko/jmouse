package org.jmouse.testing_ground.beans.definition;

public class DuplicateBeanDefinitionException extends RuntimeException {

    public DuplicateBeanDefinitionException(BeanDefinition definition) {
        super("Duplicate bean definition: Bean '" + definition.getBeanName() + "' already present");
    }

}
