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
 * This strategy supports creating structured definitions where the structured instance is provided by a factory.
 * The default structured scope for such definitions is {@link BeanScope#PROTOTYPE}.
 */
public class ObjectFactoryBeanDefinitionCreationStrategy implements BeanDefinitionCreationStrategy<ObjectFactory<Object>> {

    /**
     * Determines if this strategy supports the provided structured.
     * <p>
     * Currently, this strategy does not support implicit structured definition creation from arbitrary objects.
     *
     * @param object the structured to check.
     * @return {@code false}, as only named factories are supported.
     */
    @Override
    public boolean supports(Object object) {
        return object instanceof ObjectFactory<?>;
    }

    /**
     * Unsupported operation for creating a structured definition without an explicit name.
     *
     * @param object  the factory structured.
     * @param context the structured context.
     * @return nothing, as this operation always throws an exception.
     * @throws BeanContextException indicating that this operation is unsupported.
     */
    @Override
    public BeanDefinition create(ObjectFactory<Object> object, BeanContext context) {
        throw new BeanContextException("Unsupported structured definition creation without explicit structured name");
    }

    /**
     * Creates a {@link BeanDefinition} from the given {@link ObjectFactory} and an explicit structured name.
     * <p>
     * The structured scope for definitions created by this strategy is set to {@link BeanScope#PROTOTYPE}.
     *
     * @param name    the name of the structured.
     * @param object  the factory structured providing the structured instance.
     * @param context the structured context.
     * @return the created {@link BeanDefinition}.
     */
    @Override
    public BeanDefinition create(String name, ObjectFactory<Object> object, BeanContext context) {
        ObjectFactoryBeanDefinition definition = new ObjectFactoryBeanDefinition(name, object.getClass(), object);

        definition.setScope(BeanScope.PROTOTYPE);

        return definition;
    }
}
