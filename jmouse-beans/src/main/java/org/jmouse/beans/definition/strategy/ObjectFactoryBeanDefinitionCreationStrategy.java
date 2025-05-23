package org.jmouse.beans.definition.strategy;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextException;
import org.jmouse.beans.ObjectFactory;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.beans.definition.ObjectFactoryBeanDefinition;

/**
 * A strategy for creating {@link BeanDefinition} instances from {@link ObjectFactory} objects.
 * <p>
 * This strategy supports creating bean definitions where the bean instance is provided by a factory.
 * The default bean scope for such definitions is {@link BeanScope#PROTOTYPE}.
 */
public class ObjectFactoryBeanDefinitionCreationStrategy implements BeanDefinitionCreationStrategy<ObjectFactory<Object>> {

    /**
     * Determines if this strategy supports the provided bean.
     * <p>
     * Currently, this strategy does not support implicit bean definition creation from arbitrary objects.
     *
     * @param object the bean to check.
     * @return {@code false}, as only named factories are supported.
     */
    @Override
    public boolean supports(Object object) {
        return object instanceof ObjectFactory<?>;
    }

    /**
     * Unsupported operation for creating a bean definition without an explicit name.
     *
     * @param object  the factory bean.
     * @param context the bean context.
     * @return nothing, as this operation always throws an exception.
     * @throws BeanContextException indicating that this operation is unsupported.
     */
    @Override
    public BeanDefinition create(ObjectFactory<Object> object, BeanContext context) {
        throw new BeanContextException("Unsupported bean definition creation without explicit bean name");
    }

    /**
     * Creates a {@link BeanDefinition} from the given {@link ObjectFactory} and an explicit bean name.
     * <p>
     * The bean scope for definitions created by this strategy is set to {@link BeanScope#PROTOTYPE}.
     *
     * @param name    the name of the bean.
     * @param object  the factory bean providing the bean instance.
     * @param context the bean context.
     * @return the created {@link BeanDefinition}.
     */
    @Override
    public BeanDefinition create(String name, ObjectFactory<Object> object, BeanContext context) {
        ObjectFactoryBeanDefinition definition = new ObjectFactoryBeanDefinition(name, object.getClass(), object);

        definition.setScope(BeanScope.PROTOTYPE);

        return definition;
    }
}
