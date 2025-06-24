package org.jmouse.beans.conditions;

import org.jmouse.beans.definition.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final public class ConditionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionEvaluator.class);

    public ConditionEvaluator() {

    }

    public boolean evaluate(BeanDefinition definition) {
        System.out.println(definition.getAnnotatedElement());


        return true;
    }

}
