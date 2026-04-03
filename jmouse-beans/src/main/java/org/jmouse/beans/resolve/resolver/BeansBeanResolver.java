package org.jmouse.beans.resolve.resolver;

import org.jmouse.beans.Beans;
import org.jmouse.beans.resolve.BeanCandidate;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.support.AbstractBeanResolver;
import org.jmouse.core.Priority;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link AbstractBeanResolver} implementation that resolves dependencies of type {@link Beans}. 📦
 *
 * <p>
 * This resolver performs <b>collection-style resolution</b>, retrieving all available
 * {@link BeanCandidate candidates} matching the requested generic type and wrapping
 * them into a {@link Beans} container.
 * </p>
 */
@Priority(Integer.MIN_VALUE + 7500)
public class BeansBeanResolver extends AbstractBeanResolver {

    /**
     * Checks whether the requested dependency is assignable to {@link Beans}. 🔎
     *
     * @param request the resolution request
     * @return {@code true} if {@link Beans} type is requested
     */
    @Override
    public boolean supports(BeanResolutionRequest request) {
        return Beans.class.isAssignableFrom(request.classType());
    }

    /**
     * Resolves all beans matching the requested generic type
     * and wraps them into a {@link Beans} container. ⚙️
     *
     * @param request the resolution request
     * @return a {@link Beans} instance containing resolved bean instances
     */
    @Override
    public Object resolve(BeanResolutionRequest request) {
        Class<?>            type       = request.beanType().getFirst().getClassType();
        List<BeanCandidate> candidates = getCandidateProvider(request).getCandidates(type);
        Set<Object>         values     = new LinkedHashSet<>();

        for (BeanCandidate candidate : candidates) {
            values.add(candidate.bean());
        }

        return new Beans.HashSet<>(values);
    }
}