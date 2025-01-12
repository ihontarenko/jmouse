package svit.beans.definition.strategy;

import svit.beans.BeanContext;
import svit.beans.definition.BeanDefinition;

public class SimpleObjectBeanDefinitionCreationStrategy implements BeanDefinitionCreationStrategy<Object> {

    /**
     * Determines if this strategy supports the provided object.
     *
     * @param object the object to check.
     * @return {@code true} if the strategy supports the object, {@code false} otherwise.
     */
    @Override
    public boolean supports(Object object) {
        return object != null;
    }

    /**
     * Creates a {@link BeanDefinition} from the provided object.
     *
     * @param object  the object from which the bean definition is created.
     * @param context the {@link BeanContext} used during creation.
     * @return the created {@link BeanDefinition}.
     */
    @Override
    public BeanDefinition create(Object object, BeanContext context) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }

    /**
     * Creates a {@link BeanDefinition} with a specified name from the provided object.
     *
     * @param name    the name of the bean.
     * @param object  the object from which the bean definition is created.
     * @param context the {@link BeanContext} used during creation.
     * @return the created {@link BeanDefinition}.
     */
    @Override
    public BeanDefinition create(String name, Object object, BeanContext context) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }

}
