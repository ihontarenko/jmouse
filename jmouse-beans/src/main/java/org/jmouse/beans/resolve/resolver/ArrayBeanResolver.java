package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.support.AbstractBeanResolver;

import java.lang.reflect.Array;
import java.util.List;

/**
 * Resolves array-based dependencies. 📦
 *
 * <p>Collects all matching beans and returns them as an array.</p>
 */
public class ArrayBeanResolver extends AbstractBeanResolver {

    /**
     * Checks whether the requested dependency is an array.
     *
     * @param request resolution request
     * @return {@code true} if supported
     */
    @Override
    public boolean supports(BeanResolutionRequest request) {
        return request.beanType().isArray();
    }

    /**
     * Resolves all matching beans and places them into a new array.
     *
     * @param request resolution request
     * @return array of resolved bean instances
     */
    @Override
    public Object resolve(BeanResolutionRequest request) {
        Class<?>            arrayType  = request.beanType().getComponentType().getClassType();
        List<BeanCandidate> candidates = getCandidateProvider(request).getCandidates(arrayType);
        Object              array      = Array.newInstance(arrayType, candidates.size());

        for (int i = 0; i < candidates.size(); i++) {
            Array.set(array, i, candidates.get(i).bean());
        }

        return array;
    }
}