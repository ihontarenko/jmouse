package org.jmouse.beans.definition;

import org.jmouse.beans.BeanInstantiationType;
import org.jmouse.beans.InstantiationType;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;

/**
 * 🧱 Bean definition based on constructor instantiation.
 *
 * Represents a bean created via a specific {@link Constructor}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public class ConstructorBeanDefinition extends AbstractBeanDefinition {

    private Constructor<?> constructor;

    /**
     * 🔧 Create a constructor-based bean definition.
     *
     * @param name bean name
     * @param type bean class
     */
    public ConstructorBeanDefinition(String name, Class<?> type) {
        super(name, type);
        this.strategy = null;
    }

    /**
     * 🎯 Instantiation strategy — always CONSTRUCTOR.
     */
    @Override
    public InstantiationType getInstantiationType() {
        return BeanInstantiationType.CONSTRUCTOR;
    }

    /**
     * 🛠️ Return the constructor used to create the bean.
     */
    public Constructor<?> getConstructor() {
        return constructor;
    }

    /**
     * 🧩 Set the constructor used for instantiation.
     */
    public void setConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    /**
     * 🧠 Underlying annotated element (for metadata resolution).
     */
    @Override
    public AnnotatedElement getAnnotatedElement() {
        return getBeanClass();
    }

    @Override
    public String toString() {
        return "CONSTRUCTOR_BEAN_DEFINITION: " + super.toString();
    }
}
