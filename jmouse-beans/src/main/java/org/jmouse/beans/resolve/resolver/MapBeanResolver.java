package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.resolve.support.AbstractBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.core.reflection.InferredType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapBeanResolver extends AbstractBeanResolver {

    @Override
    public boolean supports(BeanResolutionRequest request) {
        InferredType type = request.beanType();

        if (!type.isMap()) {
            return false;
        }

        return type.getFirst().isString();
    }

    @Override
    public Object resolve(BeanResolutionRequest request) {
        InferredType        type       = request.beanType();
        Class<?>            valueClass = type.getLast().getClassType();
        List<BeanCandidate> candidates = candidates(request).getCandidates(valueClass);

        Map<String, Object> values = new LinkedHashMap<>();

        for (BeanCandidate candidate : candidates) {
            values.put(candidate.name(), candidate.bean());
        }

        return values;
    }
}