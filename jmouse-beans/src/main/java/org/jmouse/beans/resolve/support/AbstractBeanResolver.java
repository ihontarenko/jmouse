package org.jmouse.beans.resolve.support;

import org.jmouse.beans.resolve.BeanCandidateProvider;
import org.jmouse.beans.resolve.BeanContextCandidateProvider;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolver;

/**
 * Base implementation of {@link BeanResolver}. 🧩
 *
 * <p>Provides access to {@link BeanCandidateProvider} based on request context.</p>
 */
public abstract class AbstractBeanResolver implements BeanResolver {

    /**
     * Creates a {@link BeanCandidateProvider} for the given request.
     *
     * @param request resolution request
     * @return candidate provider bound to request context
     */
    protected BeanCandidateProvider getCandidateProvider(BeanResolutionRequest request) {
        return new BeanContextCandidateProvider(request.context());
    }

}