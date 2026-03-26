package org.jmouse.beans.resolve;

import org.jmouse.beans.resolve.resolver.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory methods for common bean resolution strategies. 🏭
 */
public final class BeanResolutionStrategies {

    private BeanResolutionStrategies() {
    }

    public static BeanResolutionStrategy defaultStrategy() {
        List<BeanResolver>              resolvers = new ArrayList<>();
        CompositeBeanResolutionStrategy strategy  = new CompositeBeanResolutionStrategy(resolvers);

        resolvers.add(new OptionalBeanResolver());
        resolvers.add(new ProviderBeanResolver(strategy));
        resolvers.add(new BeansBeanResolver());
        resolvers.add(new CollectionBeanResolver());
        resolvers.add(new ArrayBeanResolver());
        resolvers.add(new MapBeanResolver());
        resolvers.add(new QualifierBeanResolver());
        resolvers.add(new NamedBeanResolver());
        resolvers.add(new PrimaryBeanResolver());
        resolvers.add(new LazyBeanResolver(strategy));
        resolvers.add(new TypeBeanResolver());

        return new CompositeBeanResolutionStrategy(resolvers);
    }

}