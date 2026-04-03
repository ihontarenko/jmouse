package org.jmouse.context.definition;

import org.jmouse.beans.InstantiationType;
import org.jmouse.beans.definition.ConstructorBeanDefinition;
import org.jmouse.context.BeanContextInstantiationType;

/**
 * Bean definition for record-based {@code @BeanProperties}. 📦
 *
 * <p>Uses {@link BeanContextInstantiationType#RECORD_BEAN_PROPERTIES} instantiation strategy.</p>
 */
public class RecordPropertiesDefinition extends ConstructorBeanDefinition {

    /**
     * Creates definition for record bean properties.
     *
     * @param name bean name
     * @param type bean type (record)
     */
    public RecordPropertiesDefinition(String name, Class<?> type) {
        super(name, type);
    }

    /**
     * Returns instantiation type for record properties.
     */
    @Override
    public InstantiationType getInstantiationType() {
        return BeanContextInstantiationType.RECORD_BEAN_PROPERTIES;
    }

    /**
     * Returns debug representation.
     */
    @Override
    public String toString() {
        return "BEAN_PROPERTIES_DEFINITION: " + super.toString();
    }
}