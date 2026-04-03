package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.resolve.support.AbstractBeanResolver;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.core.Priority;
import org.jmouse.core.reflection.InferredType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link AbstractBeanResolver} implementation that resolves map-based dependencies. 🗺️
 *
 * <p>
 * This resolver supports injection of {@link Map} where:
 * <ul>
 *     <li>key type is {@link String}</li>
 *     <li>value type represents bean type</li>
 * </ul>
 * </p>
 */
@Priority(Integer.MIN_VALUE + 5000)
public class MapBeanResolver extends AbstractBeanResolver {

    /**
     * Checks whether the requested dependency is a {@link Map} with {@link String} keys. 🔎
     *
     * @param request the resolution request
     * @return {@code true} if supported map type
     */
    @Override
    public boolean supports(BeanResolutionRequest request) {
        InferredType type = request.beanType();

        if (!type.isMap()) {
            return false;
        }

        return type.getFirst().isString();
    }

    /**
     * Resolves all beans matching the map value type and returns them
     * as a {@link Map} keyed by bean names. ⚙️
     *
     * @param request the resolution request
     * @return map of bean name → bean instance
     */
    @Override
    public Object resolve(BeanResolutionRequest request) {
        InferredType        type       = request.beanType();
        Class<?>            valueClass = type.getLast().getClassType();
        List<BeanCandidate> candidates = getCandidateProvider(request).getCandidates(valueClass);

        Map<String, Object> values = new LinkedHashMap<>();

        for (BeanCandidate candidate : candidates) {
            values.put(candidate.name(), candidate.bean());
        }

        return values;
    }
}