package svit.container.definition;

import svit.container.BeanInstantiationType;

/**
 * A simple implementation of {@link BeanDefinition} for standard bean definitions.
 * Represents beans with a typical lifecycle and creation type.
 */
public class SimpleBeanDefinition extends AbstractBeanDefinition {

    /**
     * Constructs a {@link SimpleBeanDefinition}.
     *
     * @param name the name of the bean.
     * @param type the type of the bean.
     */
    public SimpleBeanDefinition(String name, Class<?> type) {
        super(name, type);
    }

    /**
     * Returns a string representation of this bean definition.
     *
     * @return a string representation including the name and type of the bean.
     */
    @Override
    public String toString() {
        return "SIMPLE_BEAN_DEFINITION: " + super.toString();
    }

    /**
     * Retrieves the creation type for this bean definition.
     *
     * @return {@link BeanInstantiationType#SIMPLE}.
     */
    @Override
    public BeanInstantiationType getInstantiationType() {
        return BeanInstantiationType.SIMPLE;
    }
}
