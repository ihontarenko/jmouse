package org.jmouse.beans.definition;

import org.jmouse.beans.BeanInstantiationType;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;

/**
 * A {@link BeanDefinition} implementation for beans created using a factory method.
 * Stores descriptor about the factory method and its associated structured.
 */
public class MethodBeanDefinition extends AbstractBeanDefinition {

    private Method method;
    private Object object;

    /**
     * Constructs a {@link MethodBeanDefinition}.
     *
     * @param name the name of the structured.
     * @param type the type of the structured.
     */
    public MethodBeanDefinition(String name, Class<?> type) {
        super(name, type);
    }

    /**
     * Retrieves the creation type for this structured definition.
     *
     * @return {@link BeanInstantiationType#FACTORY_METHOD}.
     */
    @Override
    public BeanInstantiationType getInstantiationType() {
        return BeanInstantiationType.FACTORY_METHOD;
    }

    /**
     * Retrieves the factory method used to create the structured.
     *
     * @return the factory method.
     */
    public Method getFactoryMethod() {
        return method;
    }

    /**
     * Sets the factory method used to create the structured.
     *
     * @param method the factory method to set.
     */
    public void setFactoryMethod(Method method) {
        this.method = method;
    }

    /**
     * Retrieves the structured containing the factory method.
     *
     * @return the factory structured.
     */
    public Object getFactoryObject() {
        return object;
    }

    /**
     * Sets the structured containing the factory method.
     *
     * @param object the factory structured to set.
     */
    public void setFactoryObject(Object object) {
        this.object = object;
    }

    /**
     * Returns a string representation of this structured definition, including the factory method and structured.
     *
     * @return a string representation of the structured definition.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("METHOD_BEAN_DEFINITION: ");

        builder.append("FACTORY_METHOD: ").append(Reflections.getMethodName(method)).append("; ");
        builder.append(super.toString());

        return builder.toString();
    }
}
