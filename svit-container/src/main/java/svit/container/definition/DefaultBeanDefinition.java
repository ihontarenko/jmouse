package svit.container.definition;

import svit.container.BeanCreationType;

/**
 * A default implementation of {@link BeanDefinition} for standard bean definitions.
 * Represents beans with a typical lifecycle and creation type.
 */
public class DefaultBeanDefinition extends AbstractBeanDefinition {

    /**
     * Constructs a {@link DefaultBeanDefinition}.
     *
     * @param name the name of the bean.
     * @param type the type of the bean.
     */
    public DefaultBeanDefinition(String name, Class<?> type) {
        super(name, type);
    }

    /**
     * Returns a string representation of this bean definition.
     *
     * @return a string representation including the name and type of the bean.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("DEFAULT_BEAN_DEFINITION: ");

        builder.append(super.toString());

        return builder.toString();
    }

    /**
     * Retrieves the creation type for this bean definition.
     *
     * @return {@link BeanCreationType#DEFAULT}.
     */
    @Override
    public BeanCreationType getBeanCreationType() {
        return BeanCreationType.DEFAULT;
    }
}
