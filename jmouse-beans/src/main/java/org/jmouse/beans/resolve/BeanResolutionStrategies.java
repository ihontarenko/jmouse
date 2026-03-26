package org.jmouse.beans.resolve;

import java.util.List;

/**
 * Factory methods for common bean resolution strategies. 🏭
 */
public final class BeanResolutionStrategies {

    private BeanResolutionStrategies() {
    }

    public static BeanResolutionStrategy defaultStrategy() {
        return new CompositeBeanResolutionStrategy(List.of(
                new OptionalBeanResolver(),
                new CollectionBeanResolver(),
                new QualifierBeanResolver(),
                new PrimaryBeanResolver(),
                new TypeBeanResolver()
        ));
    }

}