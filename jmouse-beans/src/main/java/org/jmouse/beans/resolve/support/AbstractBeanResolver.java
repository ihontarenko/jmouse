package org.jmouse.beans.resolve.support;

import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.beans.resolve.BeanCandidateProvider;
import org.jmouse.beans.resolve.BeanContextCandidateProvider;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolver;

public abstract class AbstractBeanResolver implements BeanResolver {

    protected BeanCandidateProvider candidates(BeanResolutionRequest request) {
        return new BeanContextCandidateProvider(request.beanContext());
    }

    protected Qualifier qualifier(BeanResolutionRequest context) {
        return context.getAnnotation(Qualifier.class);
    }

}