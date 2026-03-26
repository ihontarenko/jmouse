package org.jmouse.beans.resolve;

import java.lang.reflect.Parameter;

import org.jmouse.beans.annotation.Qualifier;

/**
 * Base support class for bean resolvers. 🧱
 */
public abstract class AbstractBeanResolver implements BeanResolver {

    protected BeanCandidateProvider candidates(BeanResolutionContext context) {
        return new BeanContextCandidateProvider(context.beanContext());
    }

    protected Qualifier qualifier(BeanResolutionContext context) {
        Parameter parameter = context.parameter();
        return parameter != null ? parameter.getAnnotation(Qualifier.class) : null;
    }

}