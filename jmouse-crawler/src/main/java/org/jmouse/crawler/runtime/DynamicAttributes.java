package org.jmouse.crawler.runtime;

import org.jmouse.core.context.mutable.MutableAttributesContext;

import java.util.Map;

public interface DynamicAttributes extends MutableAttributesContext {

    static DynamicAttributes withInitial(Map<Object, Object> attributes) {
        DynamicAttributes dynamicAttributes = new InMemoryDynamicAttributes();
        attributes.forEach(dynamicAttributes::setAttribute);
        return dynamicAttributes;
    }

}
