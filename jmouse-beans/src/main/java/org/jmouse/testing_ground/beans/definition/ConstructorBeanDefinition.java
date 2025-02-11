package org.jmouse.testing_ground.beans.definition;

import org.jmouse.testing_ground.beans.BeanInstantiationType;

import java.lang.reflect.Constructor;

public class ConstructorBeanDefinition extends AbstractBeanDefinition {

    private Constructor<?> constructor;

    public ConstructorBeanDefinition(String name, Class<?> type) {
        super(name, type);

        this.strategy = null;
    }

    @Override
    public BeanInstantiationType getInstantiationType() {
        return BeanInstantiationType.CONSTRUCTOR;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public void setConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("CONSTRUCTOR_BEAN_DEFINITION: ");

        builder.append(super.toString());

        return builder.toString();
    }
}
