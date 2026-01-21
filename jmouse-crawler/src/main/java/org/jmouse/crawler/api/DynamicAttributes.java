package org.jmouse.crawler.api;

import org.jmouse.core.context.mutable.MutableAttributesContext;
import org.jmouse.crawler.runtime.state.InMemoryDynamicAttributes;

import java.util.Map;

public interface DynamicAttributes extends MutableAttributesContext {

    static DynamicAttributes withInitial(Map<Object, Object> attributes) {
        DynamicAttributes dynamicAttributes = new InMemoryDynamicAttributes();
        attributes.forEach(dynamicAttributes::setAttribute);
        return dynamicAttributes;
    }

}
