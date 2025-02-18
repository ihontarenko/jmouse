package org.jmouse.beans.definition;

public class DuplicateBeanDefinitionException extends RuntimeException {

    public DuplicateBeanDefinitionException(BeanDefinition definition) {
        super("Duplicate structured definition: Bean '" + definition.getBeanName() + "' already present");
    }

}
