package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.support.AbstractBeanResolver;

import java.lang.reflect.Array;
import java.util.List;

public class ArrayBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionRequest request) {
        return request.classType().isArray();
    }

    @Override
    public Object resolve(BeanResolutionRequest request) {
        Class<?>            arrayType  = request.classType().getComponentType();
        List<BeanCandidate> candidates = candidates(request).getCandidates(arrayType);
        Object              array      = Array.newInstance(arrayType, candidates.size());

        for (int i = 0; i < candidates.size(); i++) {
            Array.set(array, i, candidates.get(i).bean());
        }

        return array;
    }
}