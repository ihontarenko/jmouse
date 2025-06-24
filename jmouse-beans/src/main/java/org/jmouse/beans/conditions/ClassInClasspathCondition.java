package org.jmouse.beans.conditions;

import org.jmouse.beans.BeanContext;

public class ClassInClasspathCondition extends AbstractBeanCondition {

    @Override
    public boolean match(ConditionalMetadata metadata, BeanContext context) {
        return false;
    }

}
