package org.jmouse.beans.resolve;

import org.jmouse.beans.BeanContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link BeanCandidateProvider} backed by {@link BeanContext}. 🏗️
 *
 * <p>Extracts beans from context and wraps them into {@link BeanCandidate}.</p>
 */
public class BeanContextCandidateProvider implements BeanCandidateProvider {

    private final BeanContext beanContext;

    /**
     * Creates provider for given {@link BeanContext}.
     *
     * @param beanContext bean context
     */
    public BeanContextCandidateProvider(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    /**
     * Returns all candidates matching the given type.
     *
     * @param type target type
     * @return list of candidates (never {@code null})
     */
    @Override
    public List<BeanCandidate> getCandidates(Class<?> type) {
        List<BeanCandidate> candidates = new ArrayList<>();
        Map<String, ?>      beans      = beanContext.getBeansOfType(type);

        for (Map.Entry<String, ?> entry : beans.entrySet()) {
            Object bean = entry.getValue();

            candidates.add(new BeanCandidate(
                    entry.getKey(),
                    bean,
                    bean.getClass(),
                    isPrimary(entry.getKey())
            ));
        }

        return candidates;
    }

    /**
     * Returns a candidate by bean name.
     *
     * @param name bean name
     * @return candidate or {@code null} if not found
     */
    @Override
    public BeanCandidate getCandidate(String name) {
        if (!beanContext.containsBean(name)) {
            return null;
        }

        Object bean = beanContext.getBean(name);

        return new BeanCandidate(
                name,
                bean,
                bean.getClass(),
                isPrimary(name)
        );
    }

    /**
     * Determines whether the bean is marked as primary.
     *
     * @param name bean name
     * @return {@code true} if primary
     */
    protected boolean isPrimary(String name) {
        return beanContext.getDefinition(name).isPrimary();
    }

}