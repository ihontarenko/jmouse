package org.jmouse.security.web.configuration.matching;

import jakarta.servlet.DispatcherType;
import org.jmouse.security.web.configuration.Customizer;
import org.jmouse.web.match.routing.MappingMatcher;
import org.jmouse.web.match.routing.MatcherCriteria;
import org.jmouse.web.match.routing.condition.DispatcherTypeMatcher;

import java.util.ArrayList;
import java.util.List;

import static java.util.List.of;

abstract public class AbstractRequestMatcherRegistry<T> {

    abstract protected T applyMatchers(List<MappingMatcher> requestMatchers);

    public T requestPath(String pattern) {
        return matcherCriteria(c -> c.pathPattern(pattern));
    }

    public T matcherCriteria(Customizer<MatcherCriteria> customizer) {
        MatcherCriteria mappingCriteria = new MatcherCriteria();
        customizer.customize(mappingCriteria);
        return matcherCriteria(mappingCriteria);
    }

    public T matcherCriteria(MappingMatcher... criteria) {
        return applyMatchers(of(criteria));
    }

    public T anyRequest() {
        return matcherCriteria(MatcherCriteria.any());
    }

    public T noneRequest() {
        return matcherCriteria(MatcherCriteria.none());
    }

    public T dispatcherTypeMatchers(DispatcherType... dispatcherTypes) {
        List<MappingMatcher> requestMatchers = new ArrayList<>();

        for (DispatcherType dispatcherType : dispatcherTypes) {
            requestMatchers.add(new DispatcherTypeMatcher(dispatcherType));
        }

        return applyMatchers(requestMatchers);
    }

}
