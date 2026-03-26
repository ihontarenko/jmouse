package org.jmouse.beans.resolve;

import org.jmouse.beans.BeanContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BeanContextCandidateProvider implements BeanCandidateProvider {

    private final BeanContext beanContext;

    public BeanContextCandidateProvider(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

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

    protected boolean isPrimary(String name) {
        return beanContext.getDefinition(name).isPrimary();
    }

}