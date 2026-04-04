package org.jmouse.context.feature;

import org.jmouse.beans.BeanContext;
import org.jmouse.core.reflection.Classpath;

public interface FeatureSelectorContext {

    BeanContext beanContext();

    Class<?>[] sourceClasses();

    default boolean isClassPresent(String className) {
        return Classpath.present(className);
    }

    record Default(
            BeanContext beanContext, Class<?>[] sourceClasses
    ) implements FeatureSelectorContext { }

}