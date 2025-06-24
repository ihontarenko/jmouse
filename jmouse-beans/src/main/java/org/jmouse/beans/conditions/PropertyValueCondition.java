package org.jmouse.beans.conditions;

import org.jmouse.beans.BeanContext;

import java.util.HashMap;
import java.util.Map;

public class PropertyValueCondition extends AbstractBeanCondition {

    @Override
    public boolean match(ConditionalMetadata definition, BeanContext context) {
        Map<String, String> values = new HashMap<>();
        OnlyIfProperty annotation = definition.getAnnotation(OnlyIfProperty.class);


        return values.get(annotation.name()).equals(annotation.value());
    }

}
